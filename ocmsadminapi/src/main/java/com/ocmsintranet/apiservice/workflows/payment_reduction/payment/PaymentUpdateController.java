package com.ocmsintranet.apiservice.workflows.payment_reduction.payment;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto.PaymentUpdateRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for payment update operations
 * Exposes the /updatePayment endpoint for processing payment transactions
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class PaymentUpdateController {

    private final PaymentUpdateService paymentUpdateService;

    public PaymentUpdateController(PaymentUpdateService paymentUpdateService) {
        this.paymentUpdateService = paymentUpdateService;
        log.info("PaymentUpdateController initialized");
    }

    /**
     * POST endpoint for updating payment information
     * This endpoint:
     * 1. Updates VON and eVON with payment and suspension details
     * 2. Creates suspended notice record
     * 3. Creates web transaction audit record
     *
     * @param request Payment update request containing notice number, amount, and transaction details
     * @return Standard CrudResponse with success or error message
     */
    @PostMapping("/updatePayment")
    public ResponseEntity<CrudResponse<?>> updatePayment(@Valid @RequestBody PaymentUpdateRequest request) {
        try {
            log.info("Received payment update request for notice: {}", request.getNoticeNo());

            // Process the payment update
            paymentUpdateService.processPaymentUpdate(request);

            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Error processing payment update for notice: {}", request.getNoticeNo(), e);

            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                    CrudResponse.AppCodes.INTERNAL_SERVER_ERROR,
                    "Error processing payment update: " + e.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Unexpected error processing payment update for notice: {}", request.getNoticeNo(), e);

            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                    CrudResponse.AppCodes.INTERNAL_SERVER_ERROR,
                    CrudResponse.Messages.DATABASE_ERROR
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
