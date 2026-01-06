package com.ocmsintranet.apiservice.workflows.payment_reduction.payment;

import lombok.Getter;

/**
 * Enum representing the result of payment validation.
 * Used to determine the appropriate action after checking payment logic.
 *
 * This is a standalone validation result that can be easily removed
 * if payment validation logic is no longer needed.
 */
@Getter
public enum PaymentValidationResult {

    /**
     * Payment already exists for this notice (duplicate payment)
     * Action: Create refund notice, skip payment processing
     */
    DOUBLE_PAYMENT("Double payment detected - notice already paid"),

    /**
     * Payment amount equals the amount payable (exact payment)
     * Action: Apply PS-FP suspension
     */
    FULL_PAYMENT("Full payment - amount matches payable"),

    /**
     * Payment amount is less than amount payable (partial payment)
     * Action: Apply PS-PRA suspension
     */
    PARTIAL_PAYMENT("Partial payment - amount less than payable"),

    /**
     * Payment amount exceeds amount payable (overpayment)
     * Action: Apply PS-FP suspension + create refund notice
     */
    OVER_PAYMENT("Over payment - amount exceeds payable");

    private final String description;

    PaymentValidationResult(String description) {
        this.description = description;
    }
}
