package com.ocmseservice.apiservice.workflows.axs.dto;

import lombok.Data;

@Data
public class UraPaymentTransactionDTO {
    private String receiptNo;
    private String typeOfReceipt;
    private String transactionDate;
    private String transactionTime;
    private String noticeNo;
    private String vehicleNo;
    private String atomsFlag;
    private String paymentMode;
    private String paymentAmount;
    private String remarks;
}

