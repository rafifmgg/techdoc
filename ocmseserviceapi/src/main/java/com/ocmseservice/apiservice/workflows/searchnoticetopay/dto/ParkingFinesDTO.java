package com.ocmseservice.apiservice.workflows.searchnoticetopay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ParkingFinesDTO {
    //private String appCode;
    private String noticeNo;
    private String vehicleNo;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime noticeDateTime;
    private BigDecimal amountPayable;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String ppCode;
    private LocalDateTime transactionDateAndTime;
    private String errorMessage;
    private String show;
    private String noticePaymentFlag;

    //private String paymentAcceptanceAllowed;
    //private String allowSelect;

}

//notice_no
//vehicle_no
//notice_date_and_time
//amount_payable
//pp_code
//date_transaction
//error_message
//show
//notice_payment_flag
