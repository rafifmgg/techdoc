package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for transaction information
 */
@Data
@NoArgsConstructor
public class TransactionDTO {
    private String txnId;
    private String txnDate; // Format: dd/MM/yy HH:mm:ss
    private double amountPayable;
    private String paymentMethod; // "JX", "ENETC", or "PYNOW"
    private String email;
    private List<TxnDetail> txnDetails;

    /**
     * Transaction detail item
     */
    @Data
    @NoArgsConstructor
    public static class TxnDetail {
        private String txnId;
        private String productId;
        private double unitPrice;
        private double unitGst;
        private int quantity;
        private double totalProductPrice;
    }
}
