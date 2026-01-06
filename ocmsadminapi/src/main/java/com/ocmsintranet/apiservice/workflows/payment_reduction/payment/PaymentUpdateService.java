package com.ocmsintranet.apiservice.workflows.payment_reduction.payment;

import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto.PaymentUpdateRequest;

/**
 * Service interface for payment update operations
 * Handles the complex payment update workflow that involves:
 * 1. Updating VON (Intranet DB)
 * 2. Updating eVON (Internet DB)
 * 3. Creating suspended notice record
 * 4. Creating web transaction audit record
 */
public interface PaymentUpdateService {

    /**
     * Process payment update for a notice
     * @param request Payment update request containing notice number, amount, and transaction details
     * @throws RuntimeException if any step of the update process fails
     */
    void processPaymentUpdate(PaymentUpdateRequest request);
}
