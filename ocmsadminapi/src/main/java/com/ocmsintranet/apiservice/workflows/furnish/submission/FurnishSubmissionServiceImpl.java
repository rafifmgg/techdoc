package com.ocmsintranet.apiservice.workflows.furnish.submission;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionResult;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishAuditService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishPersistenceService;
import com.ocmsintranet.apiservice.workflows.furnish.helpers.FurnishValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of furnish submission workflow service.
 * Orchestrates the complete furnish submission process from eService.
 *
 * Based on OCMS 41 User Story 41.4-41.7 and reduction workflow pattern.
 *
 * Workflow steps:
 * 1. Log submission received
 * 2. Initialize workflow context
 * 3. Validate basic business rules
 * 4. Check if resubmission
 * 5. Perform auto-approval checks
 * 6. Create furnish application record
 * 7. Create document attachments
 * 8. If auto-approved: create hirer/driver record and apply suspension
 * 9. Log completion and return result
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishSubmissionServiceImpl implements FurnishSubmissionService {

    private final FurnishValidator validator;
    private final FurnishPersistenceService persistenceService;
    private final FurnishAuditService auditService;

    @Override
    public FurnishSubmissionResult handleFurnishSubmission(FurnishSubmissionRequest request) {
        try {
            // Step 1: Log submission received
            auditService.logSubmissionReceived(request);

            // Step 2: Initialize workflow context
            FurnishContext context = FurnishContext.builder()
                    .request(request)
                    .build();

            // Step 3: Validate basic business rules
            auditService.logValidationStarted(request.getNoticeNo());
            try {
                validator.validateBasicBusinessRules(context);
                auditService.logValidationCompleted(request.getNoticeNo(), true);
            } catch (IllegalArgumentException e) {
                auditService.logValidationCompleted(request.getNoticeNo(), false);
                return FurnishSubmissionResult.ValidationError.builder()
                        .message(e.getMessage())
                        .build();
            }

            // Step 4: Check if resubmission
            boolean isResubmission = persistenceService.isResubmission(request.getNoticeNo());
            context.setResubmission(isResubmission);

            // Step 5: Perform auto-approval checks
            auditService.logAutoApprovalCheckStarted(request.getNoticeNo());
            validator.performAutoApprovalChecks(context);
            auditService.logAutoApprovalCheckCompleted(context);

            // Step 6: Create furnish application record
            OcmsFurnishApplication furnishApplication = persistenceService.createFurnishApplication(context);
            context.setFurnishApplication(furnishApplication);
            auditService.logFurnishApplicationCreated(
                    furnishApplication.getTxnNo(),
                    furnishApplication.getStatus(),
                    isResubmission
            );

            // Step 7: Create document attachments
            if (request.getDocumentReferences() != null && !request.getDocumentReferences().isEmpty()) {
                persistenceService.createFurnishApplicationDocuments(
                        request.getTxnNo(),
                        request.getDocumentReferences()
                );
            }

            // Step 8: If auto-approved, create hirer/driver record and apply suspension
            if (context.isAutoApprovalPassed()) {
                // Create hirer/driver record (updates current offender)
                OcmsOffenceNoticeOwnerDriver ownerDriver = persistenceService.createHirerDriverRecord(context);
                context.setNewOwnerDriver(ownerDriver);
                context.setOwnerDriverRecordCreated(true);
                auditService.logHirerDriverRecordCreated(
                        request.getNoticeNo(),
                        request.getFurnishIdNo(),
                        request.getOwnerDriverIndicator()
                );

                // Apply TS-PDP suspension (21 days)
                persistenceService.applyTsPdpSuspension(request.getNoticeNo());
                context.setSuspensionApplied(true);
                auditService.logSuspensionApplied(request.getNoticeNo(), "TS-PDP", 21);

                // Step 9: Log completion and return success
                auditService.logSubmissionCompleted(context);
                return FurnishSubmissionResult.Success.builder()
                        .furnishApplication(furnishApplication)
                        .autoApproved(true)
                        .hirerDriverRecordCreated(true)
                        .suspensionApplied(true)
                        .message("Furnish submission auto-approved successfully")
                        .build();
            } else {
                // Manual review required - TS-PDP still applied
                persistenceService.applyTsPdpSuspension(request.getNoticeNo());
                context.setSuspensionApplied(true);
                auditService.logSuspensionApplied(request.getNoticeNo(), "TS-PDP", 21);
                auditService.logManualReviewRequired(request.getNoticeNo(), context.getFailureReasonsSummary());

                // Step 9: Log completion and return business error (manual review required)
                auditService.logSubmissionCompleted(context);
                return FurnishSubmissionResult.BusinessError.builder()
                        .checkType("AUTO_APPROVAL_FAILED")
                        .message("Furnish submission requires manual review: " + context.getFailureReasonsSummary())
                        .requiresManualReview(true)
                        .furnishApplication(furnishApplication)
                        .build();
            }

        } catch (Exception e) {
            // Technical error
            auditService.logTechnicalError("furnish submission", request.getTxnNo(), e);

            Map<String, Object> details = new HashMap<>();
            details.put("txnNo", request.getTxnNo());
            details.put("noticeNo", request.getNoticeNo());
            details.put("exceptionMessage", e.getMessage());

            return FurnishSubmissionResult.TechnicalError.builder()
                    .operation("furnish submission")
                    .message("Technical error during furnish submission: " + e.getMessage())
                    .cause(e.getClass().getName())
                    .details(details)
                    .build();
        }
    }
}
