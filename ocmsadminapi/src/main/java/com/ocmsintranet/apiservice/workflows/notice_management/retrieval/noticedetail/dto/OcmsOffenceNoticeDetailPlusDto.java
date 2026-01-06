package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetail;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for PLUS API /plus-offence-notice-detail endpoint
 * Contains only the fields required by PLUS interface specification
 */
@Data
@NoArgsConstructor
public class OcmsOffenceNoticeDetailPlusDto {

    private String noticeNo;
    private String colorOfVehicle;
    // private String comments;
    private String conditionInvalidCoupon1;
    private String conditionInvalidCoupon2;
    private LocalDateTime creDate;
    private String creUserId;
    private LocalDateTime createdDt;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon1;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon2;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomOfValidCoupon;

    private LocalDateTime expiryTime;
    private String imageId;
    private String conditionInvalidCoupon3;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon3;

    private String invalidCouponNo3;
    private String invalidCouponNo1;
    private String invalidCouponNo2;
    private String iuNo;
    private String lastValidCouponNo;
    private String repObuLatitude;
    private String repObuLongitude;
    private String repOperatorId;
    private LocalDateTime repParkingEndDt;
    private LocalDateTime repParkingEntryDt;
    private LocalDateTime repParkingExitDt;
    private LocalDateTime repParkingStartDt;
    private String ruleDesc;
    private String ruleNo;
    private String ruleRemark1;
    private String ruleRemark2;
    private Integer totalCouponsDisplayed;
    private String vehicleMake;

    /**
     * Constructor to map from OcmsOffenceNoticeDetail entity
     */
    public OcmsOffenceNoticeDetailPlusDto(OcmsOffenceNoticeDetail entity) {
        if (entity != null) {
            this.noticeNo = entity.getNoticeNo();
            this.colorOfVehicle = entity.getColorOfVehicle();
            // this.comments = entity.getComments();
            this.conditionInvalidCoupon1 = entity.getConditionInvalidCoupon1();
            this.conditionInvalidCoupon2 = entity.getConditionInvalidCoupon2();
            this.creDate = entity.getCreDate();
            this.creUserId = entity.getCreUserId();
            this.createdDt = entity.getCreatedDt();
            this.denomInvalidCoupon1 = entity.getDenomInvalidCoupon1();
            this.denomInvalidCoupon2 = entity.getDenomInvalidCoupon2();
            this.denomOfValidCoupon = entity.getDenomOfValidCoupon();
            this.expiryTime = entity.getExpiryTime();
            this.imageId = entity.getImageId();
            this.conditionInvalidCoupon3 = entity.getConditionInvalidCoupon3();
            this.denomInvalidCoupon3 = entity.getDenomInvalidCoupon3();
            this.invalidCouponNo3 = entity.getInvalidCouponNo3();
            this.invalidCouponNo1 = entity.getInvalidCouponNo1();
            this.invalidCouponNo2 = entity.getInvalidCouponNo2();
            this.iuNo = entity.getIuNo();
            this.lastValidCouponNo = entity.getLastValidCouponNo();
            this.repObuLatitude = entity.getRepObuLatitude();
            this.repObuLongitude = entity.getRepObuLongitude();
            this.repOperatorId = entity.getRepOperatorId();
            this.repParkingEndDt = entity.getRepParkingEndDt();
            this.repParkingEntryDt = entity.getRepParkingEntryDt();
            this.repParkingExitDt = entity.getRepParkingExitDt();
            this.repParkingStartDt = entity.getRepParkingStartDt();
            this.ruleDesc = entity.getRuleDesc();
            this.ruleNo = entity.getRuleNo();
            this.ruleRemark1 = entity.getRuleRemark1();
            this.ruleRemark2 = entity.getRuleRemark2();
            this.totalCouponsDisplayed = entity.getTotalCouponsDisplayed();
            this.vehicleMake =  entity.getVehicleMake();
        }
    }
}
