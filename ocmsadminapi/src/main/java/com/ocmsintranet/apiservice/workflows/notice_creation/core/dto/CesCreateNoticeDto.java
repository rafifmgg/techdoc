package com.ocmsintranet.apiservice.workflows.notice_creation.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CesCreateNoticeDto {

    private String transactionId;
    private String subsystemLabel;
    private String anFlag;
    private String iuNo;
    private String repObuLatitude;
    private String repObuLongitude;
    private String repOperatorId;

    private String repParkingEntryDt;
    private String repParkingStartDt;
    private String repParkingEndDt;

    private BigDecimal repChargeAmount;
    private String otherRemark;

    private String repViolationCode;
    private BigDecimal compositionAmount;
    private Integer computerRuleCode;
    private String creUserId;
    private String noticeDateAndTime;
    private String offenceNoticeType;
    private String ppCode;
    private String ppName;
    //change
    //private String comments;
    //
    private String ruleRemark1;
    private String ruleRemark2;
    private String sysNonPrintedComment;
    private String createdDt;

    private String vehicleCategory;
    private String parkingLotNo;
    private String wardenNo;
    private String vehicleRegistrationType;
    private String vehicleNo;
    private String vehicleMake;
    private String colorOfVehicle;
    private String creDate;
    private String noticeNo;
    private String userNonPrintedComment;


}
