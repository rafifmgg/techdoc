package com.ocmseservice.apiservice.workflows.axs.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AxsUraPaymentRequestDTO {

    private String sender;
    private String targetReceiver;
    private String dateSent;
    private String timeSent;
    private String signature;
    private String transactionID;
    private String recordCounter;
    private int totalAmt;
    private List<Transaction> txnList;

    @Getter
    @Setter
    public static class Transaction {
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
}
