package com.ocmsintranet.cronservice.framework.workflows.ocms41_sync_furnished.services;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplication.EocmsFurnishApplication;
import com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplication.EocmsFurnishApplicationService;
import com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc.EocmsFurnishApplicationDoc;
import com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc.EocmsFurnishApplicationDocService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDoc;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocId;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocService;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OCMS 41: Sync Furnished Submissions (Internet DB → Intranet DB)
 *
 * Synchronizes furnish hirer/driver submissions from Internet database to Intranet database
 * where is_sync = false
 *
 * Flow:
 * 1. Query eocms_furnish_application where is_sync = 'N'
 * 2. For each record:
 *    - Decrypt PII fields (Internet DB has encrypted PII)
 *    - Check if record exists in ocms_furnish_application → UPDATE
 *    - If not exists → INSERT
 *    - Set is_sync = 'Y' in eocms_furnish_application
 *
 * Note: PII decryption happens automatically via JPA entity listeners
 * (Internet DB entities have @Convert annotation for encrypted fields)
 */
@Slf4j
@Service
public class Ocms41SyncFurnishedService {

    private final EocmsFurnishApplicationService eFurnishService;
    private final OcmsFurnishApplicationService furnishService;
    private final EocmsFurnishApplicationDocService eDocService;
    private final OcmsFurnishApplicationDocService docService;
    private final AzureBlobStorageUtil blobStorageUtil;

    @Value("${ocms41.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${azure.blob.internet-container-name:internetblob}")
    private String internetContainerName;

    @Value("${azure.blob.intranet-container-name:intranetblob}")
    private String intranetContainerName;

    @Autowired
    public Ocms41SyncFurnishedService(
            EocmsFurnishApplicationService eFurnishService,
            OcmsFurnishApplicationService furnishService,
            EocmsFurnishApplicationDocService eDocService,
            OcmsFurnishApplicationDocService docService,
            AzureBlobStorageUtil blobStorageUtil) {
        this.eFurnishService = eFurnishService;
        this.furnishService = furnishService;
        this.eDocService = eDocService;
        this.docService = docService;
        this.blobStorageUtil = blobStorageUtil;
    }

    /**
     * Main sync method - syncs furnished submissions from Internet → Intranet
     */
    public void syncFurnishedSubmissions() {
        log.info("=== Starting OCMS 41: Sync Furnished Submissions (Internet → Intranet) at {} ===", LocalDateTime.now());

        // Check if sync feature is enabled
        if (!syncEnabled) {
            log.info("OCMS 41 furnished sync is disabled. Skipping sync.");
            return;
        }

        try {
            // Sync furnished application records
            int applicationsSynced = syncFurnishedApplications();
            log.info("Synced {} furnished application records", applicationsSynced);

            // Sync supporting documents and blobs
            int documentsSynced = syncSupportingDocuments();
            log.info("Synced {} supporting document records and blobs", documentsSynced);

            log.info("=== OCMS 41 Furnished Sync completed. Applications: {}, Documents: {} ===",
                    applicationsSynced, documentsSynced);

        } catch (Exception e) {
            log.error("Error during OCMS 41 furnished sync", e);
        }
    }

    /**
     * Sync furnished application records where is_sync = 'N'
     * @return Number of records synced
     */
    @Transactional
    public int syncFurnishedApplications() {
        log.info("Starting furnished application sync (is_sync = N)");

        // Query eocms_furnish_application where is_sync = 'N'
        List<EocmsFurnishApplication> unsyncedRecords = eFurnishService.findByIsSync("N");

        if (unsyncedRecords.isEmpty()) {
            log.info("No unsynced furnished application records found");
            return 0;
        }

        log.info("Found {} unsynced furnished application records to process", unsyncedRecords.size());

        int successCount = 0;
        int failureCount = 0;

        for (EocmsFurnishApplication eFurnish : unsyncedRecords) {
            try {
                String txnNo = eFurnish.getTxnNo();

                // Check if record exists in Intranet DB
                Optional<OcmsFurnishApplication> furnishOpt = furnishService.getById(txnNo);

                OcmsFurnishApplication furnish;
                boolean isUpdate;
                if (furnishOpt.isPresent()) {
                    // UPDATE existing record
                    furnish = furnishOpt.get();
                    isUpdate = true;
                    copyEFurnishToFurnish(eFurnish, furnish, isUpdate);
                    furnishService.save(furnish);
                    log.debug("Updated ocms_furnish_application for txn_no {}", txnNo);
                } else {
                    // INSERT new record
                    furnish = new OcmsFurnishApplication();
                    isUpdate = false;
                    copyEFurnishToFurnish(eFurnish, furnish, isUpdate);
                    furnishService.save(furnish);
                    log.debug("Inserted new ocms_furnish_application for txn_no {}", txnNo);
                }

                // Set is_sync = 'Y' in Internet DB
                eFurnish.setIsSync("Y");
                eFurnish.setUpdDate(LocalDateTime.now());
                eFurnish.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                eFurnishService.save(eFurnish);

                successCount++;
                log.debug("Successfully synced furnished application for txn_no {}", txnNo);

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to sync furnished application for txn_no {}: {}", eFurnish.getTxnNo(), e.getMessage(), e);
                // Continue with next record
            }
        }

        log.info("Furnished application sync completed: {} success, {} failed", successCount, failureCount);
        return successCount;
    }

    /**
     * Copy fields from eocms_furnish_application (Internet DB) to ocms_furnish_application (Intranet DB)
     * PII fields are automatically decrypted by JPA when reading from Internet DB
     *
     * @param eFurnish Source record from Internet DB
     * @param furnish Target record in Intranet DB
     * @param isUpdate true if updating existing record, false if inserting new record
     */
    private void copyEFurnishToFurnish(EocmsFurnishApplication eFurnish, OcmsFurnishApplication furnish, boolean isUpdate) {
        // Primary key
        furnish.setTxnNo(eFurnish.getTxnNo());

        // Notice information
        furnish.setNoticeNo(eFurnish.getNoticeNo());
        furnish.setVehicleNo(eFurnish.getVehicleNo());
        furnish.setOffenceDate(eFurnish.getOffenceDate());
        furnish.setPpCode(eFurnish.getPpCode());
        furnish.setPpName(eFurnish.getPpName());
        furnish.setLastProcessingStage(eFurnish.getLastProcessingStage());

        // Furnished person details (PII - auto-decrypted)
        furnish.setFurnishName(eFurnish.getFurnishName());
        furnish.setFurnishIdType(eFurnish.getFurnishIdType());
        furnish.setFurnishIdNo(eFurnish.getFurnishIdNo());

        // Furnished person address
        furnish.setFurnishMailBlkNo(eFurnish.getFurnishMailBlkNo());
        furnish.setFurnishMailFloor(eFurnish.getFurnishMailFloor());
        furnish.setFurnishMailStreetName(eFurnish.getFurnishMailStreetName());
        furnish.setFurnishMailUnitNo(eFurnish.getFurnishMailUnitNo());
        furnish.setFurnishMailBldgName(eFurnish.getFurnishMailBldgName());
        furnish.setFurnishMailPostalCode(eFurnish.getFurnishMailPostalCode());

        // Furnished person contact
        furnish.setFurnishTelCode(eFurnish.getFurnishTelCode());
        furnish.setFurnishTelNo(eFurnish.getFurnishTelNo());
        furnish.setFurnishEmailAddr(eFurnish.getFurnishEmailAddr());

        // Relationship and questionnaire
        furnish.setOwnerDriverIndicator(eFurnish.getOwnerDriverIndicator());
        furnish.setHirerOwnerRelationship(eFurnish.getHirerOwnerRelationship());
        furnish.setOthersRelationshipDesc(eFurnish.getOthersRelationshipDesc());
        furnish.setQuesOneAns(eFurnish.getQuesOneAns());
        furnish.setQuesTwoAns(eFurnish.getQuesTwoAns());
        furnish.setQuesThreeAns(eFurnish.getQuesThreeAns());

        // Rental period
        furnish.setRentalPeriodTo(eFurnish.getRentalPeriodTo());
        furnish.setRentalPeriodFrom(eFurnish.getRentalPeriodFrom());

        // Status
        furnish.setStatus(eFurnish.getStatus());

        // Owner details (PII - auto-decrypted)
        furnish.setOwnerName(eFurnish.getOwnerName());
        furnish.setOwnerIdNo(eFurnish.getOwnerIdNo());
        furnish.setOwnerTelCode(eFurnish.getOwnerTelCode());
        furnish.setOwnerTelNo(eFurnish.getOwnerTelNo());
        furnish.setOwnerEmailAddr(eFurnish.getOwnerEmailAddr());

        // CorpPass and review
        furnish.setCorppassStaffName(eFurnish.getCorppassStaffName());
        furnish.setReasonForReview(eFurnish.getReasonForReview());
        furnish.setRemarks(eFurnish.getRemarks());

        // Set audit fields based on operation type
        if (isUpdate) {
            // UPDATE: Only set updDate and updUserId
            furnish.setUpdDate(LocalDateTime.now());
            furnish.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        } else {
            // INSERT: Set creDate to current time, creUserId to SYSTEM, explicitly null update fields
            furnish.setCreDate(LocalDateTime.now());
            furnish.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            furnish.setUpdDate(null);
            furnish.setUpdUserId(null);
        }
    }

    /**
     * Sync supporting documents where is_sync = 'N'
     * Also copies blob files from Internet storage to Intranet storage
     * @return Number of documents synced
     */
    @Transactional
    public int syncSupportingDocuments() {
        log.info("Starting supporting documents sync (is_sync = N)");

        // Query eocms_furnish_application_doc where is_sync = 'N'
        List<EocmsFurnishApplicationDoc> unsyncedDocs = eDocService.findByIsSync("N");

        if (unsyncedDocs.isEmpty()) {
            log.info("No unsynced supporting document records found");
            return 0;
        }

        log.info("Found {} unsynced supporting document records to process", unsyncedDocs.size());

        int successCount = 0;
        int failureCount = 0;

        for (EocmsFurnishApplicationDoc eDoc : unsyncedDocs) {
            try {
                String txnNo = eDoc.getTxnNo();
                Integer attachmentId = eDoc.getAttachmentId();

                // Check if record exists in Intranet DB
                OcmsFurnishApplicationDocId docId = new OcmsFurnishApplicationDocId(txnNo, attachmentId);
                Optional<OcmsFurnishApplicationDoc> docOpt = docService.getById(docId);

                OcmsFurnishApplicationDoc doc;
                boolean isUpdate;
                if (docOpt.isPresent()) {
                    // UPDATE existing record
                    doc = docOpt.get();
                    isUpdate = true;
                    copyEDocToDoc(eDoc, doc, isUpdate);
                    log.debug("Updated ocms_furnish_application_doc for txn_no {} attachment_id {}", txnNo, attachmentId);
                } else {
                    // INSERT new record
                    doc = new OcmsFurnishApplicationDoc();
                    isUpdate = false;
                    copyEDocToDoc(eDoc, doc, isUpdate);
                    log.debug("Inserted new ocms_furnish_application_doc for txn_no {} attachment_id {}", txnNo, attachmentId);
                }

                // Sync blob file from Internet storage to Intranet storage
                boolean blobSynced = syncBlobFile(eDoc);
                if (!blobSynced) {
                    log.warn("Failed to sync blob file for txn_no {} attachment_id {}, but continuing with metadata sync",
                            txnNo, attachmentId);
                }

                // Save document metadata to Intranet DB
                docService.save(doc);

                // Set is_sync = 'Y' in Internet DB
                eDoc.setIsSync("Y");
                eDoc.setUpdDate(LocalDateTime.now());
                eDoc.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                eDocService.save(eDoc);

                successCount++;
                log.debug("Successfully synced supporting document for txn_no {} attachment_id {}", txnNo, attachmentId);

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to sync supporting document for txn_no {} attachment_id {}: {}",
                        eDoc.getTxnNo(), eDoc.getAttachmentId(), e.getMessage(), e);
                // Continue with next record
            }
        }

        log.info("Supporting documents sync completed: {} success, {} failed", successCount, failureCount);
        return successCount;
    }

    /**
     * Copy fields from eocms_furnish_application_doc (Internet DB) to ocms_furnish_application_doc (Intranet DB)
     *
     * @param eDoc Source record from Internet DB
     * @param doc Target record in Intranet DB
     * @param isUpdate true if updating existing record, false if inserting new record
     */
    private void copyEDocToDoc(EocmsFurnishApplicationDoc eDoc, OcmsFurnishApplicationDoc doc, boolean isUpdate) {
        // Primary key
        doc.setTxnNo(eDoc.getTxnNo());
        doc.setAttachmentId(eDoc.getAttachmentId());

        // Document metadata
        doc.setDocName(eDoc.getDocName());
        doc.setMime(eDoc.getMime());
        doc.setSize(eDoc.getSize());

        // Set audit fields based on operation type
        if (isUpdate) {
            // UPDATE: Only set updDate and updUserId
            doc.setUpdDate(LocalDateTime.now());
            doc.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        } else {
            // INSERT: Set creDate to current time, creUserId to SYSTEM, explicitly null update fields
            doc.setCreDate(LocalDateTime.now());
            doc.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            doc.setUpdDate(null);
            doc.setUpdUserId(null);
        }
    }

    /**
     * Sync blob file from Internet storage to Intranet storage
     * Blob path format: {folder}/{txn_no}/{doc_name}
     *
     * @param eDoc Document record from Internet DB
     * @return true if blob sync successful, false otherwise
     */
    private boolean syncBlobFile(EocmsFurnishApplicationDoc eDoc) {
        try {
            String txnNo = eDoc.getTxnNo();
            String docName = eDoc.getDocName();

            // Construct blob paths
            // Internet blob path: furnish-applications/{txn_no}/{doc_name}
            String internetBlobPath = "furnish-applications/" + txnNo + "/" + docName;

            // Intranet blob path: furnish-applications/{txn_no}/{doc_name}
            String intranetBlobPath = "furnish-applications/" + txnNo + "/" + docName;

            log.debug("Syncing blob file from Internet storage to Intranet storage");
            log.debug("Source: {} (container: {})", internetBlobPath, internetContainerName);
            log.debug("Target: {} (container: {})", intranetBlobPath, intranetContainerName);

            // Download from Internet blob storage
            byte[] fileContent = blobStorageUtil.downloadFromBlob(internetBlobPath);

            if (fileContent == null || fileContent.length == 0) {
                log.error("Failed to download blob from Internet storage: {}", internetBlobPath);
                return false;
            }

            log.debug("Downloaded {} bytes from Internet storage", fileContent.length);

            // Upload to Intranet blob storage
            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                    blobStorageUtil.uploadBytesToBlob(fileContent, intranetBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload blob to Intranet storage: {}", uploadResponse.getErrorMessage());
                return false;
            }

            log.debug("Successfully synced blob file to Intranet storage: {}", intranetBlobPath);
            return true;

        } catch (Exception e) {
            log.error("Error syncing blob file for txn_no {} doc_name {}: {}",
                    eDoc.getTxnNo(), eDoc.getDocName(), e.getMessage(), e);
            return false;
        }
    }
}
