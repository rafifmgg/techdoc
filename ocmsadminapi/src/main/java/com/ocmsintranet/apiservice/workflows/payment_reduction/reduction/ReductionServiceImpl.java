package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionContext;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionResult;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.helpers.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementation of ReductionService that orchestrates the entire reduction workflow.
 *
 * This service coordinates all the helper services to perform:
 * - Request validation
 * - Notice loading and checking
 * - Eligibility determination
 * - Transactional database updates
 * - Audit logging
 *
 * The workflow follows the business process defined in the requirements document.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReductionServiceImpl implements ReductionService {

    private final OcmsValidOffenceNoticeRepository noticeRepository;
    private final ReductionValidator validator;
    private final ReductionPersistenceService persistenceService;
    private final ReductionAuditService auditService;

    @Override
    public ReductionResult handleReductionRequest(ReductionRequest request) {
        log.info("================================================");
        log.info("Starting reduction request processing for notice: {}", request.getNoticeNo());
        log.info("================================================");

        // Record audit log for attempt start
        auditService.recordReductionAttemptStart(request);

        try {
            // Step 1-2: Format and mandatory validation already handled by @Valid in controller

            // Step 3: Load notice by notice number
            log.info("Step 3: Loading notice by notice number: {}", request.getNoticeNo());
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeRepository.findByNoticeNo(request.getNoticeNo());

            if (noticeOpt.isEmpty()) {
                String message = String.format("Notice %s not found in the system", request.getNoticeNo());
                log.warn(message);
                auditService.recordNoticeNotFound(request.getNoticeNo());
                ReductionResult result = new ReductionResult.BusinessError("NOTICE_NOT_FOUND", message);
                auditService.recordReductionAttemptComplete(request, result);
                return result;
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();
            log.info("Notice found: {} for vehicle: {}", notice.getNoticeNo(), notice.getVehicleNo());

            // Step 4: Check idempotency - if reduction already applied, return success
            if (persistenceService.isReductionAlreadyApplied(notice)) {
                log.info("Reduction already applied to notice {} - treating as idempotent request",
                        notice.getNoticeNo());
                auditService.recordIdempotentRequest(notice.getNoticeNo());
                ReductionResult result = new ReductionResult.Success(
                        notice.getNoticeNo(),
                        "Reduction already applied - request processed successfully (idempotent)");
                auditService.recordReductionAttemptComplete(request, result);
                return result;
            }

            // Step 5: Check if notice has been paid
            log.info("Step 5: Checking if notice has been paid");
            ReductionResult paidCheck = validator.validateNoticeNotPaid(notice);
            if (paidCheck != null) {
                auditService.recordValidationFailure(notice.getNoticeNo(), "NOTICE_PAID",
                        "Notice has already been paid");
                auditService.recordReductionAttemptComplete(request, paidCheck);
                return paidCheck;
            }

            // Step 6: Build reduction context
            log.info("Step 6: Building reduction context");
            ReductionContext context = buildReductionContext(request, notice);

            // Step 7: Validate amounts and dates
            log.info("Step 7: Validating amounts and dates");
            ReductionResult amountCheck = validator.validateReductionAmounts(request, notice);
            if (amountCheck != null) {
                auditService.recordValidationFailure(notice.getNoticeNo(), "INVALID_AMOUNTS",
                        amountCheck instanceof ReductionResult.ValidationError ve ? ve.message() : "Amount validation failed");
                auditService.recordReductionAttemptComplete(request, amountCheck);
                return amountCheck;
            }

            ReductionResult dateCheck = validator.validateDates(request);
            if (dateCheck != null) {
                auditService.recordValidationFailure(notice.getNoticeNo(), "INVALID_DATES",
                        dateCheck instanceof ReductionResult.ValidationError ve ? ve.message() : "Date validation failed");
                auditService.recordReductionAttemptComplete(request, dateCheck);
                return dateCheck;
            }

            // Step 8: Check eligibility (computer rule code + processing stage)
            log.info("Step 8: Checking eligibility - RuleCode: {}, Stage: {}",
                    context.getComputerRuleCode(), context.getLastProcessingStage());
            ReductionResult eligibilityCheck = validator.validateEligibility(context);
            if (eligibilityCheck != null) {
                if (eligibilityCheck instanceof ReductionResult.BusinessError be) {
                    auditService.recordEligibilityFailure(notice.getNoticeNo(),
                            context.getComputerRuleCode(),
                            context.getLastProcessingStage(),
                            be.reason());
                }
                auditService.recordReductionAttemptComplete(request, eligibilityCheck);
                return eligibilityCheck;
            }

            // Step 9: Perform transactional database updates
            log.info("Step 9: Performing transactional database updates");
            auditService.recordPersistenceStart(notice.getNoticeNo());

            try {
                persistenceService.applyReduction(context);
                auditService.recordPersistenceSuccess(notice.getNoticeNo());

                log.info("================================================");
                log.info("Reduction processing completed successfully for notice: {}", notice.getNoticeNo());
                log.info("================================================");

                ReductionResult successResult = new ReductionResult.Success(
                        notice.getNoticeNo(),
                        "Reduction applied successfully");
                auditService.recordReductionAttemptComplete(request, successResult);
                return successResult;

            } catch (OptimisticLockingFailureException e) {
                // Handle concurrent modification
                String message = String.format("Concurrent modification detected for notice %s. Please retry.",
                        notice.getNoticeNo());
                log.warn(message, e);
                auditService.recordPersistenceFailure(notice.getNoticeNo(), e);
                ReductionResult result = new ReductionResult.TechnicalError(
                        "OPTIMISTIC_LOCK_FAILURE",
                        message,
                        e);
                auditService.recordReductionAttemptComplete(request, result);
                return result;

            } catch (Exception e) {
                // Handle any other persistence errors
                String message = String.format("Failed to persist reduction for notice %s: %s",
                        notice.getNoticeNo(), e.getMessage());
                log.error(message, e);
                auditService.recordPersistenceFailure(notice.getNoticeNo(), e);
                ReductionResult result = new ReductionResult.TechnicalError(
                        "REDUCTION_FAIL",
                        "Database operation failed during reduction processing",
                        e);
                auditService.recordReductionAttemptComplete(request, result);
                return result;
            }

        } catch (Exception e) {
            // Catch-all for unexpected errors
            String message = String.format("Unexpected error processing reduction for notice %s: %s",
                    request.getNoticeNo(), e.getMessage());
            log.error(message, e);
            ReductionResult result = new ReductionResult.TechnicalError(
                    "UNEXPECTED_ERROR",
                    "An unexpected error occurred during reduction processing",
                    e);
            auditService.recordReductionAttemptComplete(request, result);
            return result;
        }
    }

    /**
     * Build a ReductionContext from the request and loaded notice.
     *
     * This context contains all data needed for the reduction workflow.
     */
    private ReductionContext buildReductionContext(ReductionRequest request, OcmsValidOffenceNotice notice) {
        log.debug("Building reduction context for notice: {}", notice.getNoticeNo());

        // Get next serial number - same sr_no is used for both suspended_notice and reduced_offence_amount
        Integer srNo = persistenceService.getNextSrNo(notice.getNoticeNo());

        // Create audit context
        String auditContext = String.format("Reduction requested by %s from source %s at %s",
                request.getAuthorisedOfficer(),
                request.getSuspensionSource(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return ReductionContext.builder()
                .request(request)
                .notice(notice)
                .computerRuleCode(notice.getComputerRuleCode())
                .lastProcessingStage(notice.getLastProcessingStage())
                .srNo(srNo)
                .processingStartTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .auditContext(auditContext)
                .build();
    }
}
