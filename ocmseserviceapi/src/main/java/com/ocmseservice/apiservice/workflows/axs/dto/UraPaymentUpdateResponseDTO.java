package com.ocmseservice.apiservice.workflows.axs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class UraPaymentUpdateResponseDTO {
    private String sender;
    private String targetReceiver;
    private String dateSent;
    private String timeSent;
    private String signature;
    private String transactionID;
    private String recordCounter;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMsg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;
    private int totalAmt;
    private List<UraPaymentTransactionDTO> txnList;
}

//private String sender;
//private String targetReceiver;
//private String dateSent;
//private String timeSent;
//private String transactionID;
//private int recordCounter;
//private String signature;
//private String errorMsg;
//private String status;
//private List<UraToAxsTransactionDTO> txnList;