package com.ocmsintranet.apiservice.workflows.furnish.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDoc;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.workflows.furnish.domain.FurnishStatus;
import com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence service for furnish submission workflow.
 * Handles all database operations in transactional manner.
 *
 * Based on OCMS 41 requirements and reduction workflow pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishPersistenceService {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsFurnishApplicationDocRepository furnishApplicationDocRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * Check if this is a resubmission (previous furnish application exists for this notice)
     */
    public boolean isResubmission(String noticeNo) {
        List<OcmsFurnishApplication> existingApplications =
                furnishApplicationRepository.findByNoticeNo(noticeNo);
        return !existingApplications.isEmpty();
    }

    /**
     * Create furnish application record (pending or auto-approved)
     */
    @Transactional
    public OcmsFurnishApplication createFurnishApplication(FurnishContext context) {
        FurnishSubmissionRequest request = context.getRequest();
        boolean isResubmission = context.isResubmission();
        boolean autoApproved = context.isAutoApprovalPassed();

        log.debug("Creating furnish application for notice: {}, resubmission: {}, auto-approved: {}",
                request.getNoticeNo(), isResubmission, autoApproved);

        OcmsFurnishApplication application = new OcmsFurnishApplication();

        // Primary key and notice details
        application.setTxnNo(request.getTxnNo());
        application.setNoticeNo(request.getNoticeNo());
        application.setVehicleNo(request.getVehicleNo());
        application.setOffenceDate(request.getOffenceDate());
        application.setPpCode(request.getPpCode());
        application.setPpName(request.getPpName());

        // Furnished person details
        application.setFurnishName(request.getFurnishName());
        application.setFurnishIdType(request.getFurnishIdType());
        application.setFurnishIdNo(request.getFurnishIdNo());

        // Furnished person address
        application.setFurnishMailBlkNo(request.getFurnishMailBlkNo());
        application.setFurnishMailFloor(request.getFurnishMailFloor());
        application.setFurnishMailStreetName(request.getFurnishMailStreetName());
        application.setFurnishMailUnitNo(request.getFurnishMailUnitNo());
        application.setFurnishMailBldgName(request.getFurnishMailBldgName());
        application.setFurnishMailPostalCode(request.getFurnishMailPostalCode());

        // Furnished person contact
        application.setFurnishTelCode(request.getFurnishTelCode());
        application.setFurnishTelNo(request.getFurnishTelNo());
        application.setFurnishEmailAddr(request.getFurnishEmailAddr());

        // Furnish details
        application.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
        application.setHirerOwnerRelationship(request.getHirerOwnerRelationship());
        application.setOthersRelationshipDesc(request.getOthersRelationshipDesc());

        // Questionnaire answers
        application.setQuesOneAns(request.getQuesOneAns());
        application.setQuesTwoAns(request.getQuesTwoAns());
        application.setQuesThreeAns(request.getQuesThreeAns());

        // Rental period
        application.setRentalPeriodFrom(request.getRentalPeriodFrom());
        application.setRentalPeriodTo(request.getRentalPeriodTo());

        // Submitter (owner/furnisher) details
        application.setOwnerName(request.getOwnerName());
        application.setOwnerIdNo(request.getOwnerIdNo());
        application.setOwnerTelCode(request.getOwnerTelCode());
        application.setOwnerTelNo(request.getOwnerTelNo());
        application.setOwnerEmailAddr(request.getOwnerEmailAddr());

        // CorpPass business rep
        application.setCorppassStaffName(request.getCorppassStaffName());

        // Status determination
        if (isResubmission) {
            application.setStatus(FurnishStatus.RESUBMISSION.getCode());
        } else if (autoApproved) {
            application.setStatus(FurnishStatus.APPROVED.getCode());
        } else {
            application.setStatus(FurnishStatus.PENDING.getCode());
        }

        // Reason for review (if manual review required)
        if (!autoApproved && context.hasAutoApprovalFailures()) {
            application.setReasonForReview(context.getFailureReasonsSummary());
        }

        // Save application
        OcmsFurnishApplication saved = furnishApplicationRepository.save(application);
        log.info("Created furnish application: {}, status: {}", saved.getTxnNo(), saved.getStatus());

        return saved;
    }

    /**
     * Create document attachments for furnish application
     */
    @Transactional
    public void createFurnishApplicationDocuments(String txnNo, List<String> documentReferences) {
        if (documentReferences == null || documentReferences.isEmpty()) {
            log.debug("No documents to attach for txn: {}", txnNo);
            return;
        }

        log.debug("Creating {} document records for txn: {}", documentReferences.size(), txnNo);

        for (int i = 0; i < documentReferences.size(); i++) {
            OcmsFurnishApplicationDoc doc = new OcmsFurnishApplicationDoc();
            doc.setTxnNo(txnNo);
            doc.setAttachmentId(i + 1);
            doc.setDocName(documentReferences.get(i)); // Azure Blob path

            furnishApplicationDocRepository.save(doc);
        }

        log.info("Created {} document records for txn: {}", documentReferences.size(), txnNo);
    }

    /**
     * Create hirer/driver record (auto-approval only)
     * This updates current offender and creates furnished person record
     */
    @Transactional
    public OcmsOffenceNoticeOwnerDriver createHirerDriverRecord(FurnishContext context) {
        FurnishSubmissionRequest request = context.getRequest();

        log.debug("Creating hirer/driver record for notice: {}, type: {}",
                request.getNoticeNo(), request.getOwnerDriverIndicator());

        // Step 1: Update existing owner/driver to NOT current offender
        List<OcmsOffenceNoticeOwnerDriver> existingRecords =
                ownerDriverRepository.findByNoticeNo(request.getNoticeNo());

        for (OcmsOffenceNoticeOwnerDriver existing : existingRecords) {
            if ("Y".equals(existing.getOffenderIndicator())) {
                existing.setOffenderIndicator("N");
                ownerDriverRepository.save(existing);
                log.debug("Updated owner/driver {} to NOT current offender", existing.getIdNo());
            }
        }

        // Step 2: Create new hirer/driver record for furnished person
        OcmsOffenceNoticeOwnerDriver newRecord = new OcmsOffenceNoticeOwnerDriver();
        newRecord.setNoticeNo(request.getNoticeNo());
        newRecord.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
        newRecord.setIdNo(request.getFurnishIdNo());
        newRecord.setIdType(request.getFurnishIdType());
        newRecord.setName(request.getFurnishName());
        newRecord.setOffenderIndicator("Y");

        // Contact details
        newRecord.setOffenderTelCode(request.getFurnishTelCode());
        newRecord.setOffenderTelNo(request.getFurnishTelNo());
        newRecord.setEmailAddr(request.getFurnishEmailAddr());

        OcmsOffenceNoticeOwnerDriver saved = ownerDriverRepository.save(newRecord);
        log.info("Created hirer/driver record for notice: {}, ID: {}", request.getNoticeNo(), request.getFurnishIdNo());

        // Step 3: Create furnished mail address
        createFurnishedMailAddress(request);

        return saved;
    }

    /**
     * Create furnished mail address (type='furnished_mail')
     */
    @Transactional
    public void createFurnishedMailAddress(FurnishSubmissionRequest request) {
        log.debug("Creating furnished mail address for notice: {}", request.getNoticeNo());

        OcmsOffenceNoticeOwnerDriverAddr address = new OcmsOffenceNoticeOwnerDriverAddr();
        address.setNoticeNo(request.getNoticeNo());
        address.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
        address.setTypeOfAddress("furnished_mail");

        address.setBlkHseNo(request.getFurnishMailBlkNo());
        address.setFloorNo(request.getFurnishMailFloor());
        address.setStreetName(request.getFurnishMailStreetName());
        address.setUnitNo(request.getFurnishMailUnitNo());
        address.setBldgName(request.getFurnishMailBldgName());
        address.setPostalCode(request.getFurnishMailPostalCode());

        ownerDriverAddrRepository.save(address);
        log.info("Created furnished mail address for notice: {}", request.getNoticeNo());
    }

    /**
     * Apply TS-PDP suspension (21 days, auto-approval only)
     */
    @Transactional
    public void applyTsPdpSuspension(String noticeNo) {
        log.debug("Applying TS-PDP suspension for notice: {}", noticeNo);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(21);

        // Get next srNo for this notice
        Integer maxSrNo = suspendedNoticeRepository.findMaxSrNoByNoticeNo(noticeNo);
        Integer nextSrNo = (maxSrNo == null) ? 1 : maxSrNo + 1;

        SuspendedNotice suspension = new SuspendedNotice();
        // Composite key fields
        suspension.setNoticeNo(noticeNo);
        suspension.setDateOfSuspension(now);
        suspension.setSrNo(nextSrNo);

        // Required fields
        suspension.setSuspensionSource("FURN"); // Furnish source
        suspension.setSuspensionType("TS"); // TS-PDP type
        suspension.setReasonOfSuspension("PDP"); // Pending Driver Particulars
        suspension.setOfficerAuthorisingSupension("SYSTEM"); // Auto-approval system
        suspension.setDueDateOfRevival(dueDate);

        // Optional fields
        suspension.setSuspensionRemarks("Auto-suspended due to furnish submission");

        suspendedNoticeRepository.save(suspension);
        log.info("Applied TS-PDP suspension for notice: {} (21 days, srNo: {})", noticeNo, nextSrNo);
    }
}
