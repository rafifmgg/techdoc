package com.ocmseservice.apiservice.workflows.axs.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaymentUpdateDTO {

    private String sender;
    private String targetReceiver;
    private String dateSent;
    private String timeSent;
    private String signature;
    private String transactionID;
    private String recordCounter;
    private int totalAmt;
    private List<TransactionDTO> txnList;

}

