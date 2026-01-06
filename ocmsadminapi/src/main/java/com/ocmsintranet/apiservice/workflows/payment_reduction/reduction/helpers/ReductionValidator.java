package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionContext;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsible for business-level validation of reduction requests.
 *
 * Bean validation (@Valid) handles basic format checks (null, blank, etc.).
 * This service handles business logic validation like:
 * - Notice has not been paid
 * - Amount validations
 * - Date validations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReductionValidator {

    private final ReductionRuleService ruleService;

    /**
     * Validate that the notice has not been paid.
     * A paid notice cannot be reduced.
     *
     * Per spec: Check CRS reason of suspension for "FP" (Full Payment) or "PRA" (Paid).
     *
     * @param notice The notice to check
     * @return ValidationResult indicating if validation passed
     */
    public ReductionResult validateNoticeNotPaid(OcmsValidOffenceNotice notice) {
        log.debug("Validating that notice {} is not paid", notice.getNoticeNo());

        // Check CRS reason of suspension for paid status (FP or PRA)
        String crsReason = notice.getCrsReasonOfSuspension();
        if ("FP".equals(crsReason) || "PRA".equals(crsReason)) {
            String message = "Notice has been paid";
            log.warn("Notice {} has been paid (CRS reason: {}). Cannot process reduction.",
                    notice.getNoticeNo(), crsReason);
            return new ReductionResult.BusinessError("NOTICE_PAID", message);
        }

        log.debug("Notice {} is not paid, validation passed", notice.getNoticeNo());
        return null; // null means validation passed
    }

    /**
     * Validate that the reduced amount is logical compared to original composition amount.
     *
     * @param request The reduction request
     * @param notice The notice being reduced
     * @return ValidationResult indicating if validation passed
     */
    public ReductionResult validateReductionAmounts(ReductionRequest request, OcmsValidOffenceNotice notice) {
        log.debug("Validating reduction amounts for notice {}", notice.getNoticeNo());

        BigDecimal originalAmount = notice.getCompositionAmount();
        BigDecimal amountReduced = request.getAmountReduced();
        BigDecimal newAmountPayable = request.getAmountPayable();

        // Validate that amountReduced <= originalAmount
        if (amountReduced.compareTo(originalAmount) > 0) {
            String message = String.format("Amount reduced (%s) cannot be greater than original composition amount (%s)",
                    amountReduced, originalAmount);
            log.warn(message);
            return new ReductionResult.ValidationError("INVALID_REDUCTION_AMOUNT", message);
        }

        // Validate that newAmountPayable = originalAmount - amountReduced
        BigDecimal expectedPayable = originalAmount.subtract(amountReduced);
        if (newAmountPayable.compareTo(expectedPayable) != 0) {
            String message = String.format("Amount payable (%s) does not match expected value (%s = %s - %s)",
                    newAmountPayable, expectedPayable, originalAmount, amountReduced);
            log.warn(message);
            return new ReductionResult.ValidationError("INCONSISTENT_AMOUNTS", message);
        }

        // Validate that newAmountPayable is non-negative
        if (newAmountPayable.compareTo(BigDecimal.ZERO) < 0) {
            String message = String.format("Amount payable cannot be negative: %s", newAmountPayable);
            log.warn(message);
            return new ReductionResult.ValidationError("NEGATIVE_AMOUNT", message);
        }

        log.debug("Amount validation passed for notice {}", notice.getNoticeNo());
        return null; // null means validation passed
    }

    /**
     * Validate that the expiry date is after the reduction date.
     *
     * @param request The reduction request
     * @return ValidationResult indicating if validation passed
     */
    public ReductionResult validateDates(ReductionRequest request) {
        log.debug("Validating dates for notice {}", request.getNoticeNo());

        if (request.getExpiryDateOfReduction().isBefore(request.getDateOfReduction())) {
            String message = "Expiry date of reduction must be after date of reduction";
            log.warn(message);
            return new ReductionResult.ValidationError("INVALID_DATES", message);
        }

        log.debug("Date validation passed");
        return null; // null means validation passed
    }

    /**
     * Validate that the notice is eligible for reduction based on business rules.
     *
     * @param context The reduction context
     * @return ValidationResult indicating if validation passed
     */
    public ReductionResult validateEligibility(ReductionContext context) {
        log.info("Validating eligibility for notice {}", context.getNotice().getNoticeNo());

        Integer ruleCode = context.getComputerRuleCode();
        String lastStage = context.getLastProcessingStage();

        boolean isEligible = ruleService.isNoticeEligibleForReduction(ruleCode, lastStage);

        if (!isEligible) {
            String reason = ruleService.getIneligibilityReason(ruleCode, lastStage);
            String message = String.format("Notice %s is not eligible for reduction: %s",
                    context.getNotice().getNoticeNo(), reason);
            log.warn(message);
            return new ReductionResult.BusinessError("NOT_ELIGIBLE", message, reason);
        }

        log.info("Notice {} is eligible for reduction", context.getNotice().getNoticeNo());
        return null; // null means validation passed
    }

    /**
     * Validate all business rules for a reduction request.
     * This is a convenience method that runs all validations.
     *
     * @param request The reduction request
     * @param notice The loaded notice
     * @param context The reduction context
     * @return The first validation error encountered, or null if all validations pass
     */
    public ReductionResult validateAll(ReductionRequest request, OcmsValidOffenceNotice notice, ReductionContext context) {
        log.info("Running all validations for notice {}", notice.getNoticeNo());

        // Check if notice is paid
        ReductionResult paidCheck = validateNoticeNotPaid(notice);
        if (paidCheck != null) {
            return paidCheck;
        }

        // Check amounts
        ReductionResult amountCheck = validateReductionAmounts(request, notice);
        if (amountCheck != null) {
            return amountCheck;
        }

        // Check dates
        ReductionResult dateCheck = validateDates(request);
        if (dateCheck != null) {
            return dateCheck;
        }

        // Check eligibility
        ReductionResult eligibilityCheck = validateEligibility(context);
        if (eligibilityCheck != null) {
            return eligibilityCheck;
        }

        log.info("All validations passed for notice {}", notice.getNoticeNo());
        return null; // All validations passed
    }
}
