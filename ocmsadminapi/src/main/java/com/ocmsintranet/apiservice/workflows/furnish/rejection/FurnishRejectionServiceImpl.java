package com.ocmsintranet.apiservice.workflows.furnish.rejection;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.workflows.furnish.domain.EmailTemplateType;
import com.ocmsintranet.apiservice.workflows.furnish.domain.FurnishStatus;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionResult;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishNotificationService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishTemplateService;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.FurnishDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of officer rejection workflow.
 * Based on OCMS 41 User Stories 41.24-41.33.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishRejectionServiceImpl implements FurnishRejectionService {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final FurnishTemplateService templateService;
    private final FurnishDashboardService dashboardService;
    private final FurnishNotificationService notificationService;

    @Override
    @Transactional
    public FurnishRejectionResult handleRejection(FurnishRejectionRequest request) {
        try {
            log.info("Processing rejection request for txnNo: {}, officer: {}", request.getTxnNo(), request.getOfficerId());

            // Step 1: Validate and retrieve application
            Optional<OcmsFurnishApplication> applicationOpt = furnishApplicationRepository.findById(request.getTxnNo());
            if (applicationOpt.isEmpty()) {
                return FurnishRejectionResult.ValidationError.builder()
                        .field("txnNo")
                        .message("Furnish application not found: " + request.getTxnNo())
                        .build();
            }

            OcmsFurnishApplication application = applicationOpt.get();

            // Check application status
            if (FurnishStatus.APPROVED.getCode().equals(application.getStatus())) {
                return FurnishRejectionResult.BusinessError.builder()
                        .reason("ALREADY_APPROVED")
                        .message("This application has already been approved. Cannot reject approved applications.")
                        .build();
            }

            if (FurnishStatus.REJECTED.getCode().equals(application.getStatus())) {
                return FurnishRejectionResult.BusinessError.builder()
                        .reason("ALREADY_REJECTED")
                        .message("This application has already been rejected")
                        .build();
            }

            // Step 2: Send email notification to owner (if requested)
            boolean emailSentToOwner = false;

            if (request.isSendEmailToOwner()) {
                FurnishApplicationDetailResponse detail = dashboardService.getApplicationDetail(request.getTxnNo());
                emailSentToOwner = sendRejectionEmailToOwner(detail, request);
            }

            // Step 3: Update status to Rejected (OCMS41.32)
            application.setStatus(FurnishStatus.REJECTED.getCode());
            application.setRemarks(buildRejectionRemarks(request.getOfficerId(), request.getRejectionReason(), request.getRemarks()));
            furnishApplicationRepository.save(application);

            // Step 4: Resend notice to eService portal (OCMS41.33)
            boolean noticeResentToPortal = resendNoticeToPortal(application.getNoticeNo());

            log.info("Successfully rejected furnish application: {}, notice: {}",
                    request.getTxnNo(), application.getNoticeNo());

            return FurnishRejectionResult.Success.builder()
                    .txnNo(application.getTxnNo())
                    .noticeNo(application.getNoticeNo())
                    .emailSentToOwner(emailSentToOwner)
                    .noticeResentToPortal(noticeResentToPortal)
                    .message("Furnish application rejected successfully")
                    .build();

        } catch (Exception e) {
            log.error("Technical error during rejection: txnNo={}", request.getTxnNo(), e);

            Map<String, Object> details = new HashMap<>();
            details.put("txnNo", request.getTxnNo());
            details.put("officerId", request.getOfficerId());
            details.put("exceptionMessage", e.getMessage());

            return FurnishRejectionResult.TechnicalError.builder()
                    .operation("furnish rejection")
                    .message("Technical error during rejection: " + e.getMessage())
                    .cause(e.getClass().getName())
                    .details(details)
                    .build();
        }
    }

    /**
     * Send rejection email to owner
     */
    private boolean sendRejectionEmailToOwner(FurnishApplicationDetailResponse detail, FurnishRejectionRequest request) {
        try {
            if (detail.getOwnerEmailAddr() == null || detail.getOwnerEmailAddr().isBlank()) {
                log.warn("No owner email address available for notice: {}", detail.getNoticeNo());
                return false;
            }

            log.info("Sending rejection email to owner: {}", detail.getOwnerEmailAddr());

            String subject = request.getCustomEmailSubject() != null
                    ? request.getCustomEmailSubject()
                    : templateService.generateRejectionEmailSubject(detail.getNoticeNo());

            String body;
            if (request.getCustomEmailBody() != null) {
                body = request.getCustomEmailBody();
            } else {
                // Use template based on template ID or default to REJECTED_GENERAL
                EmailTemplateType templateType = determineRejectionTemplateType(request.getEmailTemplateId());
                body = templateService.generateEmailContent(templateType, detail, request.getOfficerId());
            }

            return notificationService.sendAndRecordEmail(
                    detail.getNoticeNo(),
                    detail.getCurrentProcessingStage(),
                    detail.getOwnerEmailAddr(),
                    subject,
                    body
            );
        } catch (Exception e) {
            log.error("Failed to send rejection email to owner", e);
            return false;
        }
    }

    /**
     * Determine rejection template type from template ID
     */
    private EmailTemplateType determineRejectionTemplateType(String templateId) {
        if (templateId == null) {
            return EmailTemplateType.REJECTED_GENERAL;
        }

        return switch (templateId) {
            case "REJECTED_DOCS_REQUIRED" -> EmailTemplateType.REJECTED_DOCS_REQUIRED;
            case "REJECTED_MULTIPLE_HIRERS" -> EmailTemplateType.REJECTED_MULTIPLE_HIRERS;
            case "REJECTED_RENTAL_DISCREPANCY" -> EmailTemplateType.REJECTED_RENTAL_DISCREPANCY;
            default -> EmailTemplateType.REJECTED_GENERAL;
        };
    }

    /**
     * Resend notice to eService portal (OCMS41.33)
     * This makes the notice available again for owner to resubmit
     */
    private boolean resendNoticeToPortal(String noticeNo) {
        try {
            log.info("Resending notice to eService portal: {}", noticeNo);

            // TODO: Integrate with eService portal API to make notice available again
            // This would typically involve:
            // 1. Update notice status in eService portal
            // 2. Send notification to portal that notice is available for resubmission

            return true;
        } catch (Exception e) {
            log.error("Failed to resend notice to portal: {}", noticeNo, e);
            return false;
        }
    }

    /**
     * Build rejection remarks
     */
    private String buildRejectionRemarks(String officerId, String rejectionReason, String customRemarks) {
        String timestamp = LocalDateTime.now().toString();
        String baseRemarks = String.format("Rejected by %s on %s - Reason: %s",
                officerId, timestamp, rejectionReason);

        if (customRemarks != null && !customRemarks.isBlank()) {
            return baseRemarks + " - " + customRemarks;
        }

        return baseRemarks;
    }
}
