package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for URA Payment Gateway response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UraPaymentResponse {
    
    /**
     * Status code from payment gateway
     */
    private String statusCode;
    
    /**
     * Status message from payment gateway
     */
    private String statusMessage;
    
    /**
     * Transaction ID from payment gateway
     */
    private String transactionId;
    
    /**
     * Receipt number (same as in request)
     */
    private String receiptNo;
    
    /**
     * URL to redirect user to payment gateway
     */
    private String redirectUrl;
    
    /**
     * Error code if any
     */
    private String errorCode;
    
    /**
     * Error message if any
     */
    private String errorMessage;
}
