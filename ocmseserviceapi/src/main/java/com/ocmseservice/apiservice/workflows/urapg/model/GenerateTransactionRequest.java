package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model class for URA Payment Gateway transaction generation request
 */
@Data
@NoArgsConstructor
public class GenerateTransactionRequest {
    
    /**
     * List of notice numbers for the parking fines
     */
    private List<String> notices;
    
    /**
     * Payment method: "JX", "ENETC", or "PYNOW"
     */
    private String paymentMethod;
}
