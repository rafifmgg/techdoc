package com.ocmseservice.apiservice.workflows.searchnoticetopay.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice.EocmsValidOffenceNotice;

@Data
public class SearchParkingNoticeDTO {

    private String appCode;
    private String noticeNo;
    private String vehicleNo;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime noticeDateAndTime;
    private BigDecimal amountPayable;
    private String ppCode;
    private String userMessage;
    private String dateTransaction;
    private String errorMessage;
    private String errorCode;
    private String show;
    private String noticePaymentFlag;
    private String allowSelect;
    private String paymentAcceptanceAllowed;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDateAndTime;

    // Field tambahan untuk optimasi proses di StateValidations
    private String anFlag;
    private String lastProcessingStage;
    private String nextProcessingStage;
    private String vehicleRegistrationType;
    private String offenceNoticeType;
    private String ruleCode;
    private String seachBy;
    private String carparkName;
    private transient EocmsValidOffenceNotice offenceNoticeEntity;


}
