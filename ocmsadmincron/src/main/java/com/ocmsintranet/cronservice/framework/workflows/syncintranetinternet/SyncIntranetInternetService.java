package com.ocmsintranet.cronservice.framework.workflows.syncintranetinternet;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice.EocmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice.EocmsValidOffenceNoticeService;
import com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver.EocmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver.EocmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Process 7: Batch Cron Sync (Push Intranet to Internet)
 *
 * Synchronizes records from Intranet database to Internet database where is_sync = false
 *
 * Flow:
 * 1. Query VON where is_sync = false
 * 2. For each VON:
 *    - Check if eVON exists → UPDATE eVON
 *    - If not exists → INSERT eVON
 *    - Set is_sync = true in VON
 * 3. Query ONOD where is_sync = false
 * 4. For each ONOD:
 *    - Check if eONOD exists → UPDATE eONOD (PII DB)
 *    - If not exists → INSERT eONOD (PII DB)
 *    - Set is_sync = true in ONOD
 */
@Slf4j
@Service
public class SyncIntranetInternetService {

    private final OcmsValidOffenceNoticeService vonService;
    private final EocmsValidOffenceNoticeService eVonService;
    private final OcmsOffenceNoticeOwnerDriverService onodService;
    private final EocmsOffenceNoticeOwnerDriverService eOnodService;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    @Autowired
    public SyncIntranetInternetService(
            OcmsValidOffenceNoticeService vonService,
            EocmsValidOffenceNoticeService eVonService,
            OcmsOffenceNoticeOwnerDriverService onodService,
            EocmsOffenceNoticeOwnerDriverService eOnodService) {
        this.vonService = vonService;
        this.eVonService = eVonService;
        this.onodService = onodService;
        this.eOnodService = eOnodService;
    }

    /**
     * Main sync method - syncs both VON and ONOD records
     */
    public void syncIntranetToInternet() {
        log.info("=== Starting Batch Cron Sync: Push Intranet to Internet at {} ===", LocalDateTime.now());

        // Check if payment sync feature is enabled
        if (!paymentSyncEnabled) {
            log.info("Payment sync feature is disabled. Skipping batch sync.");
            return;
        }

        try {
            // Sync VON records
            int vonSynced = syncValidOffenceNotices();
            log.info("Synced {} VON records", vonSynced);

            // Sync ONOD records
            int onodSynced = syncOffenceNoticeOwnerDriver();
            log.info("Synced {} ONOD records", onodSynced);

            log.info("=== Batch Cron Sync completed. Total synced: {} VON + {} ONOD ===", vonSynced, onodSynced);

        } catch (Exception e) {
            log.error("Error during batch cron sync", e);
        }
    }

    /**
     * Sync VON records where is_sync = false
     * @return Number of records synced
     */
    @Transactional
    public int syncValidOffenceNotices() {
        log.info("Starting VON sync (is_sync = false)");

        // Query VON where is_sync = false
        List<OcmsValidOffenceNotice> unsyncedRecords = vonService.findByIsSync("N");

        if (unsyncedRecords.isEmpty()) {
            log.info("No unsynced VON records found");
            return 0;
        }

        log.info("Found {} unsynced VON records to process", unsyncedRecords.size());

        int successCount = 0;
        int failureCount = 0;

        for (OcmsValidOffenceNotice von : unsyncedRecords) {
            try {
                String noticeNo = von.getNoticeNo();

                // Check if eVON exists
                Optional<EocmsValidOffenceNotice> eVonOpt = eVonService.getById(noticeNo);

                EocmsValidOffenceNotice eVon;
                boolean isUpdate;
                if (eVonOpt.isPresent()) {
                    // UPDATE existing eVON
                    eVon = eVonOpt.get();
                    isUpdate = true;
                    copyVonToEVon(von, eVon, isUpdate);
                    eVonService.save(eVon);
                    log.debug("Updated eVON for notice {}", noticeNo);
                } else {
                    // INSERT new eVON
                    eVon = new EocmsValidOffenceNotice();
                    isUpdate = false;
                    copyVonToEVon(von, eVon, isUpdate);
                    eVonService.save(eVon);
                    log.debug("Inserted new eVON for notice {}", noticeNo);
                }

                // Set is_sync = Y in VON
                von.setIsSync("Y");
                von.setUpdDate(LocalDateTime.now());
                von.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                vonService.save(von);

                successCount++;
                log.debug("Successfully synced VON for notice {}", noticeNo);

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to sync VON for notice {}: {}", von.getNoticeNo(), e.getMessage(), e);
                // Continue with next record
            }
        }

        log.info("VON sync completed: {} success, {} failed", successCount, failureCount);
        return successCount;
    }

    /**
     * Sync ONOD records where is_sync = false
     * @return Number of records synced
     */
    @Transactional
    public int syncOffenceNoticeOwnerDriver() {
        log.info("Starting ONOD sync (is_sync = false)");

        // Query ONOD where is_sync = false
        List<OcmsOffenceNoticeOwnerDriver> unsyncedRecords = onodService.findByIsSync("N");

        if (unsyncedRecords.isEmpty()) {
            log.info("No unsynced ONOD records found");
            return 0;
        }

        log.info("Found {} unsynced ONOD records to process", unsyncedRecords.size());

        int successCount = 0;
        int failureCount = 0;

        for (OcmsOffenceNoticeOwnerDriver onod : unsyncedRecords) {
            try {
                String noticeNo = onod.getNoticeNo();
                String ownerDriverIndicator = onod.getOwnerDriverIndicator();

                // Check if eONOD exists (PII DB) - use composite key lookup
                Optional<EocmsOffenceNoticeOwnerDriver> eOnodOpt =
                    eOnodService.findByNoticeNoAndOwnerDriverIndicator(noticeNo, ownerDriverIndicator);

                EocmsOffenceNoticeOwnerDriver eOnod;
                boolean isUpdate;
                if (eOnodOpt.isPresent()) {
                    // UPDATE existing eONOD
                    eOnod = eOnodOpt.get();
                    isUpdate = true;
                    copyOnodToEOnod(onod, eOnod, isUpdate);
                    eOnodService.save(eOnod);
                    log.debug("Updated eONOD (PII) for notice {} ({})", noticeNo, ownerDriverIndicator);
                } else {
                    // INSERT new eONOD
                    eOnod = new EocmsOffenceNoticeOwnerDriver();
                    isUpdate = false;
                    copyOnodToEOnod(onod, eOnod, isUpdate);
                    eOnodService.save(eOnod);
                    log.debug("Inserted new eONOD (PII) for notice {} ({})", noticeNo, ownerDriverIndicator);
                }

                // Set is_sync = Y in ONOD
                onod.setIsSync("Y");
                onod.setUpdDate(LocalDateTime.now());
                onod.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                onodService.save(onod);

                successCount++;
                log.debug("Successfully synced ONOD for notice {}", noticeNo);

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to sync ONOD for notice {}: {}", onod.getNoticeNo(), e.getMessage(), e);
                // Continue with next record
            }
        }

        log.info("ONOD sync completed: {} success, {} failed", successCount, failureCount);
        return successCount;
    }

    /**
     * Copy fields from VON (intranet) to eVON (internet)
     * Only copies public-facing fields (excludes sensitive internal data)
     *
     * @param von Source VON record from intranet
     * @param eVon Target eVON record in internet DB
     * @param isUpdate true if updating existing record, false if inserting new record
     */
    private void copyVonToEVon(OcmsValidOffenceNotice von, EocmsValidOffenceNotice eVon, boolean isUpdate) {
        eVon.setNoticeNo(von.getNoticeNo());
        eVon.setVehicleNo(von.getVehicleNo());
        eVon.setAnFlag(von.getAnFlag());
        eVon.setNoticeDateAndTime(von.getNoticeDateAndTime());
        eVon.setAmountPayable(von.getAmountPayable());
        eVon.setAmountPaid(von.getAmountPaid());
        eVon.setPpCode(von.getPpCode());
        eVon.setPpName(von.getPpName());
        eVon.setPaymentAcceptanceAllowed(von.getPaymentAcceptanceAllowed());
        eVon.setPaymentStatus(von.getPaymentStatus());
        eVon.setLastProcessingStage(von.getLastProcessingStage());
        eVon.setNextProcessingStage(von.getNextProcessingStage());
        eVon.setVehicleRegistrationType(von.getVehicleRegistrationType());
        eVon.setSuspensionType(von.getSuspensionType());
        eVon.setCrsReasonOfSuspension(von.getCrsReasonOfSuspension());
        eVon.setCrsDateOfSuspension(von.getCrsDateOfSuspension());
        eVon.setEprReasonOfSuspension(von.getEprReasonOfSuspension());
        eVon.setEprDateOfSuspension(von.getEprDateOfSuspension());
        eVon.setOffenceNoticeType(von.getOffenceNoticeType());
        eVon.setEserviceMessageCode(von.getEserviceMessageCode());

        // Set audit fields based on operation type
        if (isUpdate) {
            // UPDATE: Only set updDate and updUserId
            eVon.setUpdDate(LocalDateTime.now());
            eVon.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        } else {
            // INSERT: Set creDate to current time, creUserId to SYSTEM, explicitly null update fields
            eVon.setCreDate(LocalDateTime.now());
            eVon.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            eVon.setUpdDate(null);
            eVon.setUpdUserId(null);
        }
    }

    /**
     * Copy fields from ONOD (intranet) to eONOD (PII internet)
     * Copies owner/driver personal information
     *
     * @param onod Source ONOD record from intranet
     * @param eOnod Target eONOD record in PII DB
     * @param isUpdate true if updating existing record, false if inserting new record
     */
    private void copyOnodToEOnod(OcmsOffenceNoticeOwnerDriver onod, EocmsOffenceNoticeOwnerDriver eOnod, boolean isUpdate) {
        eOnod.setNoticeNo(onod.getNoticeNo());
        eOnod.setOwnerDriverIndicator(onod.getOwnerDriverIndicator());
        eOnod.setIdType(onod.getIdType());
        eOnod.setIdNo(onod.getIdNo());
        eOnod.setName(onod.getName());
        eOnod.setOffenderIndicator(onod.getOffenderIndicator());

        // Set audit fields based on operation type
        if (isUpdate) {
            // UPDATE: Only set updDate and updUserId
            eOnod.setUpdDate(LocalDateTime.now());
            eOnod.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        } else {
            // INSERT: Set creDate to current time, creUserId to SYSTEM, explicitly null update fields
            eOnod.setCreDate(LocalDateTime.now());
            eOnod.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            eOnod.setUpdDate(null);
            eOnod.setUpdUserId(null);
        }
    }
}
