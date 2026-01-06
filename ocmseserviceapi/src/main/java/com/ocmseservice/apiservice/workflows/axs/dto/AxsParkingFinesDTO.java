package com.ocmseservice.apiservice.workflows.axs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AxsParkingFinesDTO {

    private String noticeNo;
    private String vehicleNo;
    private String placeOfOffence;
    private String offenceDate;
    private String offenceTime;
    private BigDecimal amountPayable;
    private String processingStage;
    private String showPON;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String paymentAllowedFlag;
    private String displayMsg;
    private String atomsFlag;

}
