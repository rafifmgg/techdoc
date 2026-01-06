package com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto;

import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.PaymentValidationResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Context object holding payment validation details.
 * Contains the validation result and related amounts for processing.
 *
 * This DTO is part of the standalone payment validation logic
 * and can be easily removed if validation is no longer needed.
 */
@Data
@Builder
public class PaymentValidationContext {

    /**
     * The validation result from payment checking
     */
    private PaymentValidationResult result;

    /**
     * Amount paid from the request
     */
    private BigDecimal amountPaid;

    /**
     * Amount payable from VON record
     */
    private BigDecimal amountPayable;

    /**
     * Current amount already paid (from VON)
     */
    private BigDecimal currentAmountPaid;

    /**
     * New total amount paid (currentAmountPaid + amountPaid)
     */
    private BigDecimal newTotalPaid;

    /**
     * Overpayment amount (if any)
     */
    private BigDecimal overpaymentAmount;
}
