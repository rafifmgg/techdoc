package com.ocmsintranet.apiservice.workflows.furnish.approval;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.workflows.furnish.domain.EmailTemplateType;
import com.ocmsintranet.apiservice.workflows.furnish.domain.FurnishStatus;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalRequest;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalResult;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishNotificationService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishPersistenceService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishTemplateService;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.FurnishDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of officer approval workflow.
 * Based on OCMS 41 User Stories 41.15-41.23.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishApprovalServiceImpl implements FurnishApprovalService {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final FurnishPersistenceService persistenceService;
    private final FurnishTemplateService templateService;
    private final FurnishDashboardService dashboardService;
    private final FurnishNotificationService notificationService;

    @Override
    @Transactional
    public FurnishApprovalResult handleApproval(FurnishApprovalRequest request) {
        try {
            log.info("Processing approval request for txnNo: {}, officer: {}", request.getTxnNo(), request.getOfficerId());

            // Step 1: Validate and retrieve application
            Optional<OcmsFurnishApplication> applicationOpt = furnishApplicationRepository.findById(request.getTxnNo());
            if (applicationOpt.isEmpty()) {
                return FurnishApprovalResult.ValidationError.builder()
                        .field("txnNo")
                        .message("Furnish application not found: " + request.getTxnNo())
                        .build();
            }

            OcmsFurnishApplication application = applicationOpt.get();

            // Check application status
            if (FurnishStatus.APPROVED.getCode().equals(application.getStatus())) {
                return FurnishApprovalResult.BusinessError.builder()
                        .reason("ALREADY_APPROVED")
                        .message("This application has already been approved")
                        .build();
            }

            if (FurnishStatus.REJECTED.getCode().equals(application.getStatus())) {
                return FurnishApprovalResult.BusinessError.builder()
                        .reason("ALREADY_REJECTED")
                        .message("This application has already been rejected. Cannot approve rejected applications.")
                        .build();
            }

            // Step 2: Check if notice has PS suspension (OCMS41.20)
            if (hasActivePsSuspension(application.getNoticeNo())) {
                return FurnishApprovalResult.BusinessError.builder()
                        .reason("PERMANENT_SUSPENSION")
                        .message("Notice has active permanent suspension (PS). Cannot approve furnish application.")
                        .build();
            }

            // Step 3: Update hirer/driver records and address
            log.debug("Creating hirer/driver record for approval");
            OcmsOffenceNoticeOwnerDriver ownerDriver = persistenceService.createHirerDriverRecord(
                    buildContextFromApplication(application)
            );

            // Step 4: Revive from TS-PDP suspension
            log.debug("Reviving TS-PDP suspension for notice: {}", application.getNoticeNo());
            reviveTsPdpSuspension(application.getNoticeNo(), request.getOfficerId());

            // Step 5: Send notifications (if requested)
            boolean emailSentToOwner = false;
            boolean emailSentToFurnished = false;
            boolean smsSentToFurnished = false;

            FurnishApplicationDetailResponse detail = dashboardService.getApplicationDetail(request.getTxnNo());

            if (request.isSendEmailToOwner()) {
                emailSentToOwner = sendEmailToOwner(detail, request);
            }

            if (request.isSendEmailToFurnished()) {
                emailSentToFurnished = sendEmailToFurnished(detail, request);
            }

            if (request.isSendSmsToFurnished()) {
                smsSentToFurnished = sendSmsToFurnished(detail, request);
            }

            // Step 6: Update status to Approved (OCMS41.22)
            application.setStatus(FurnishStatus.APPROVED.getCode());
            application.setRemarks(buildApprovalRemarks(request.getOfficerId(), request.getRemarks()));
            furnishApplicationRepository.save(application);

            log.info("Successfully approved furnish application: {}, notice: {}",
                    request.getTxnNo(), application.getNoticeNo());

            return FurnishApprovalResult.Success.builder()
                    .txnNo(application.getTxnNo())
                    .noticeNo(application.getNoticeNo())
                    .hirerDriverRecordUpdated(true)
                    .suspensionRevived(true)
                    .emailSentToOwner(emailSentToOwner)
                    .emailSentToFurnished(emailSentToFurnished)
                    .smsSentToFurnished(smsSentToFurnished)
                    .message("Furnish application approved successfully")
                    .build();

        } catch (Exception e) {
            log.error("Technical error during approval: txnNo={}", request.getTxnNo(), e);

            Map<String, Object> details = new HashMap<>();
            details.put("txnNo", request.getTxnNo());
            details.put("officerId", request.getOfficerId());
            details.put("exceptionMessage", e.getMessage());

            return FurnishApprovalResult.TechnicalError.builder()
                    .operation("furnish approval")
                    .message("Technical error during approval: " + e.getMessage())
                    .cause(e.getClass().getName())
                    .details(details)
                    .build();
        }
    }

    /**
     * Check if notice has active permanent suspension (PS)
     */
    private boolean hasActivePsSuspension(String noticeNo) {
        List<SuspendedNotice> suspensions = suspendedNoticeRepository.findByNoticeNo(noticeNo);
        return suspensions.stream()
                .anyMatch(s -> "PS".equals(s.getSuspensionType()) && s.getDateOfRevival() == null);
    }

    /**
     * Revive TS-PDP suspension by setting revival date
     */
    private void reviveTsPdpSuspension(String noticeNo, String officerId) {
        List<SuspendedNotice> suspensions = suspendedNoticeRepository.findByNoticeNo(noticeNo);

        for (SuspendedNotice suspension : suspensions) {
            if ("TS".equals(suspension.getSuspensionType()) &&
                "PDP".equals(suspension.getReasonOfSuspension()) &&
                suspension.getDateOfRevival() == null) {

                suspension.setDateOfRevival(LocalDateTime.now());
                suspension.setRevivalReason("APR"); // Approved
                suspension.setOfficerAuthorisingRevival(officerId);
                suspension.setRevivalRemarks("Furnish application approved");

                suspendedNoticeRepository.save(suspension);
                log.info("Revived TS-PDP suspension for notice: {}", noticeNo);
            }
        }
    }

    /**
     * Send email to owner (confirmation)
     */
    private boolean sendEmailToOwner(FurnishApplicationDetailResponse detail, FurnishApprovalRequest request) {
        try {
            if (detail.getOwnerEmailAddr() == null || detail.getOwnerEmailAddr().isBlank()) {
                log.warn("No owner email address available for notice: {}", detail.getNoticeNo());
                return false;
            }

            log.info("Sending approval confirmation email to owner: {}", detail.getOwnerEmailAddr());

            String subject = "Furnish Application Approved - Notice " + detail.getNoticeNo();
            String body = templateService.generateOwnerApprovalConfirmationEmail(detail);

            return notificationService.sendAndRecordEmail(
                    detail.getNoticeNo(),
                    detail.getCurrentProcessingStage(),
                    detail.getOwnerEmailAddr(),
                    subject,
                    body
            );
        } catch (Exception e) {
            log.error("Failed to send email to owner", e);
            return false;
        }
    }

    /**
     * Send email to furnished person (driver or hirer)
     */
    private boolean sendEmailToFurnished(FurnishApplicationDetailResponse detail, FurnishApprovalRequest request) {
        try {
            if (detail.getFurnishEmailAddr() == null || detail.getFurnishEmailAddr().isBlank()) {
                log.warn("No furnished person email address available for notice: {}", detail.getNoticeNo());
                return false;
            }

            log.info("Sending email to furnished person: {}", detail.getFurnishEmailAddr());

            EmailTemplateType templateType = "D".equals(detail.getOwnerDriverIndicator())
                    ? EmailTemplateType.DRIVER_FURNISHED
                    : EmailTemplateType.HIRER_FURNISHED;

            String subject = request.getCustomEmailSubject() != null
                    ? request.getCustomEmailSubject()
                    : templateService.generateApprovalEmailSubject(detail.getNoticeNo());

            String body = request.getCustomEmailBody() != null
                    ? request.getCustomEmailBody()
                    : templateService.generateEmailContent(templateType, detail, request.getOfficerId());

            return notificationService.sendAndRecordEmail(
                    detail.getNoticeNo(),
                    detail.getCurrentProcessingStage(),
                    detail.getFurnishEmailAddr(),
                    subject,
                    body
            );
        } catch (Exception e) {
            log.error("Failed to send email to furnished person", e);
            return false;
        }
    }

    /**
     * Send SMS to furnished person (driver or hirer)
     */
    private boolean sendSmsToFurnished(FurnishApplicationDetailResponse detail, FurnishApprovalRequest request) {
        try {
            if (detail.getFurnishTelNo() == null || detail.getFurnishTelNo().isBlank()) {
                log.warn("No furnished person mobile number available for notice: {}", detail.getNoticeNo());
                return false;
            }

            log.info("Sending SMS to furnished person: {}", detail.getFurnishTelNo());

            String smsBody = request.getCustomSmsBody() != null
                    ? request.getCustomSmsBody()
                    : templateService.generateDriverFurnishedSms(detail);

            String mobileCode = detail.getFurnishTelCode() != null ? detail.getFurnishTelCode() : "65";

            return notificationService.sendAndRecordSms(
                    detail.getNoticeNo(),
                    detail.getCurrentProcessingStage(),
                    mobileCode,
                    detail.getFurnishTelNo(),
                    smsBody
            );
        } catch (Exception e) {
            log.error("Failed to send SMS to furnished person", e);
            return false;
        }
    }

    /**
     * Build approval remarks
     */
    private String buildApprovalRemarks(String officerId, String customRemarks) {
        String timestamp = LocalDateTime.now().toString();
        String baseRemarks = String.format("Approved by %s on %s", officerId, timestamp);

        if (customRemarks != null && !customRemarks.isBlank()) {
            return baseRemarks + " - " + customRemarks;
        }

        return baseRemarks;
    }

    /**
     * Build context from application for persistence service
     */
    private com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext buildContextFromApplication(
            OcmsFurnishApplication application) {

        // Build request from application
        com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest request =
            com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest.builder()
                .txnNo(application.getTxnNo())
                .noticeNo(application.getNoticeNo())
                .vehicleNo(application.getVehicleNo())
                .offenceDate(application.getOffenceDate())
                .ppCode(application.getPpCode())
                .ppName(application.getPpName())
                .furnishName(application.getFurnishName())
                .furnishIdType(application.getFurnishIdType())
                .furnishIdNo(application.getFurnishIdNo())
                .furnishMailBlkNo(application.getFurnishMailBlkNo())
                .furnishMailFloor(application.getFurnishMailFloor())
                .furnishMailStreetName(application.getFurnishMailStreetName())
                .furnishMailUnitNo(application.getFurnishMailUnitNo())
                .furnishMailBldgName(application.getFurnishMailBldgName())
                .furnishMailPostalCode(application.getFurnishMailPostalCode())
                .furnishTelCode(application.getFurnishTelCode())
                .furnishTelNo(application.getFurnishTelNo())
                .furnishEmailAddr(application.getFurnishEmailAddr())
                .ownerDriverIndicator(application.getOwnerDriverIndicator())
                .build();

        return com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext.builder()
                .request(request)
                .furnishApplication(application)
                .build();
    }
}
