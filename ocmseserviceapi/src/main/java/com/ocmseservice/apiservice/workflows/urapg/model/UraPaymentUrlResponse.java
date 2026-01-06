package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from URA Payment Gateway API for payment URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UraPaymentUrlResponse {
    private String responseCode;
    private String paymentUrl;
}
