package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.helpers;

import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for audit logging and tracking of reduction operations.
 *
 * Provides structured logging at important decision points in the reduction workflow.
 * In a production system, this could also write to an audit table in the database.
 */
@Service
@Slf4j
public class ReductionAuditService {

    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Record the start of a reduction attempt.
     *
     * @param request The reduction request
     */
    public void recordReductionAttemptStart(ReductionRequest request) {
        log.info("[AUDIT] Reduction attempt started | Notice: {} | Source: {} | Officer: {} | Timestamp: {}",
                request.getNoticeNo(),
                request.getSuspensionSource(),
                request.getAuthorisedOfficer(),
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record the completion of a reduction attempt with its result.
     *
     * @param request The reduction request
     * @param result The result of the reduction
     */
    public void recordReductionAttemptComplete(ReductionRequest request, ReductionResult result) {
        String outcome = getOutcomeType(result);
        String outcomeDetails = getOutcomeDetails(result);

        log.info("[AUDIT] Reduction attempt completed | Notice: {} | Outcome: {} | Details: {} | Timestamp: {}",
                request.getNoticeNo(),
                outcome,
                outcomeDetails,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));

        // In production, you might also insert a record into an audit table here
        // Example:
        // auditRepository.save(AuditEntry.builder()
        //     .noticeNo(request.getNoticeNo())
        //     .operation("REDUCTION")
        //     .outcome(outcome)
        //     .details(outcomeDetails)
        //     .timestamp(LocalDateTime.now())
        //     .build());
    }

    /**
     * Record a validation failure.
     *
     * @param noticeNo The notice number
     * @param validationType The type of validation that failed
     * @param reason The reason for the failure
     */
    public void recordValidationFailure(String noticeNo, String validationType, String reason) {
        log.warn("[AUDIT] Validation failed | Notice: {} | Type: {} | Reason: {} | Timestamp: {}",
                noticeNo,
                validationType,
                reason,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record when a notice is not found.
     *
     * @param noticeNo The notice number that was not found
     */
    public void recordNoticeNotFound(String noticeNo) {
        log.warn("[AUDIT] Notice not found | Notice: {} | Timestamp: {}",
                noticeNo,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record an eligibility check failure.
     *
     * @param noticeNo The notice number
     * @param ruleCode The computer rule code
     * @param lastStage The last processing stage
     * @param reason The reason for ineligibility
     */
    public void recordEligibilityFailure(String noticeNo, Integer ruleCode, String lastStage, String reason) {
        log.warn("[AUDIT] Eligibility check failed | Notice: {} | RuleCode: {} | Stage: {} | Reason: {} | Timestamp: {}",
                noticeNo,
                ruleCode,
                lastStage,
                reason,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record the start of persistence operations.
     *
     * @param noticeNo The notice number
     */
    public void recordPersistenceStart(String noticeNo) {
        log.info("[AUDIT] Starting persistence operations | Notice: {} | Timestamp: {}",
                noticeNo,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record successful persistence.
     *
     * @param noticeNo The notice number
     */
    public void recordPersistenceSuccess(String noticeNo) {
        log.info("[AUDIT] Persistence operations successful | Notice: {} | Timestamp: {}",
                noticeNo,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record persistence failure.
     *
     * @param noticeNo The notice number
     * @param error The error that occurred
     */
    public void recordPersistenceFailure(String noticeNo, Throwable error) {
        log.error("[AUDIT] Persistence operations failed | Notice: {} | Error: {} | Timestamp: {}",
                noticeNo,
                error.getMessage(),
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Record idempotency check (reduction already applied).
     *
     * @param noticeNo The notice number
     */
    public void recordIdempotentRequest(String noticeNo) {
        log.info("[AUDIT] Idempotent request detected | Notice: {} already has TS-RED status | Timestamp: {}",
                noticeNo,
                LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
    }

    /**
     * Get the outcome type from a ReductionResult.
     */
    private String getOutcomeType(ReductionResult result) {
        if (result instanceof ReductionResult.Success) {
            return "SUCCESS";
        } else if (result instanceof ReductionResult.ValidationError) {
            return "VALIDATION_ERROR";
        } else if (result instanceof ReductionResult.BusinessError) {
            return "BUSINESS_ERROR";
        } else if (result instanceof ReductionResult.TechnicalError) {
            return "TECHNICAL_ERROR";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Get detailed outcome information from a ReductionResult.
     */
    private String getOutcomeDetails(ReductionResult result) {
        if (result instanceof ReductionResult.Success success) {
            return "Notice reduced successfully: " + success.noticeNo();
        } else if (result instanceof ReductionResult.ValidationError validation) {
            return validation.code() + ": " + validation.message();
        } else if (result instanceof ReductionResult.BusinessError business) {
            return business.code() + ": " + business.message();
        } else if (result instanceof ReductionResult.TechnicalError technical) {
            return technical.code() + ": " + technical.message();
        } else {
            return "Unknown result type";
        }
    }
}
