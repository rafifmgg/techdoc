package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the ocms_offence_notice_detail table.
 * This table stores detailed information about offence notices including vehicle information,
 * coupon details, LTA data, and various timestamps.
 * 
 * Updated to match the database schema exactly as per the SQL query results.
 */
@Entity
@Table(name = "ocms_offence_notice_detail", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsOffenceNoticeDetail extends BaseEntity {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    // NOT USED NOW - Keep commented out as they don't exist in your database
    // @Column(name = "batch_date")
    // private LocalDateTime batchDate;

    // NOT USED NOW
    // @Column(name = "batch_no")
    // private Integer batchNo;

    // NOT USED NOW
    // @Column(name = "chasis_no", length = 50)
    // private String chasisNo;

    @Column(name = "color_of_vehicle", length = 15)
    private String colorOfVehicle;

    // @Column(name = "comments", length = 30)
    // private String comments;

    @Column(name = "condition_invalid_coupon_1", length = 25)
    private String conditionInvalidCoupon1;

    @Column(name = "condition_invalid_coupon_2", length = 25)
    private String conditionInvalidCoupon2;

    // Audit fields (cre_date, cre_user_id, upd_date, upd_user_id) are inherited from BaseEntity
    
    // remove from DTO
    // @Column(name = "created_by", length = 50)
    // private String createdBy;

    @Column(name = "created_dt")
    private LocalDateTime createdDt;

    @Column(name = "denom_invalid_coupon_1", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon1;

    @Column(name = "denom_invalid_coupon_2", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon2;

    @Column(name = "denom_of_valid_coupon", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomOfValidCoupon;

    // NOT USED NOW
    // @Column(name = "eht_id", length = 3)
    // private String ehtId;

    // NOT USED NOW
    // @Column(name = "end_date")
    // private LocalDateTime endDate;

    // NOT USED NOW
    // @Column(name = "end_time")
    // private LocalDateTime endTime;

    // remove from DTO
    // @Column(name = "expired_coupon_1_denom")
    // private Integer expiredCoupon1Denom;

    // remove from DTO
    // @Column(name = "expired_coupon_1_no", length = 9)
    // private String expiredCoupon1No;

    // remove from DTO
    // @Column(name = "expired_coupon_1_subtype", length = 10)
    // private String expiredCoupon1Subtype;

    // remove from DTO
    // @Column(name = "expired_coupon_2_denom")
    // private Integer expiredCoupon2Denom;

    // remove from DTO
    // @Column(name = "expired_coupon_2_no", length = 9)
    // private String expiredCoupon2No;

    // remove from DTO
    // @Column(name = "expired_coupon_2_subtype", length = 10)
    // private String expiredCoupon2Subtype;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    // remove from DTO
    // @Column(name = "first_coupon_time")
    // private LocalDateTime firstCouponTime;

    // NOT USED NOW
    // @Column(name = "handicap_first_hit_dt")
    // private LocalDateTime handicapFirstHitDt;

    @Column(name = "image_id", length = 50)
    private String imageId;

    // remove from DTO
    // @Column(name = "invalid_coupon_1_creased_tab", length = 40)
    // private String invalidCoupon1CreasedTab;

    // remove from DTO
    // @Column(name = "invalid_coupon_1_subtype", length = 10)
    // private String invalidCoupon1Subtype;

    // remove from DTO
    // @Column(name = "invalid_coupon_2_creased_tab", length = 40)
    // private String invalidCoupon2CreasedTab;

    // remove from DTO
    // @Column(name = "invalid_coupon_2_subtype", length = 10)
    // private String invalidCoupon2Subtype;

    @Column(name = "condition_invalid_coupon_3", length = 25)
    private String conditionInvalidCoupon3;

    // remove from DTO
    // @Column(name = "invalid_coupon_3_creased_tab", length = 40)
    // private String invalidCoupon3CreasedTab;

    @Column(name = "denom_invalid_coupon_3", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal denomInvalidCoupon3;

    @Column(name = "invalid_coupon_no_3", length = 9)
    private String invalidCouponNo3;

    // remove from DTO
    // @Column(name = "invalid_coupon_3_subtype", length = 10)
    // private String invalidCoupon3Subtype;

    @Column(name = "invalid_coupon_no_1", length = 9)
    private String invalidCouponNo1;

    @Column(name = "invalid_coupon_no_2", length = 9)
    private String invalidCouponNo2;

    @Column(name = "iu_no", length = 10)
    private String iuNo;

    // NOT USED NOW
    // @Column(name = "label_type_tag", length = 3)
    // private String labelTypeTag;

    // NOT USED NOW
    // @Column(name = "last_modified_by", length = 50)
    // private String lastModifiedBy;

    // NOT USED NOW
    // @Column(name = "last_modified_dt")
    // private LocalDateTime lastModifiedDt;

    // remove from DTO
    // @Column(name = "last_valid_coupon_denom")
    // private Integer lastValidCouponDenom;

    // remove from DTO
    // @Column(name = "last_valid_coupon_expired_date")
    // private LocalDateTime lastValidCouponExpiredDate;

    // remove from DTO
    // @Column(name = "last_valid_coupon_expired_time")
    // private LocalDateTime lastValidCouponExpiredTime;

    @Column(name = "last_valid_coupon_no", length = 9)
    private String lastValidCouponNo;

    // remove from DTO
    // @Column(name = "last_valid_coupon_subtype", length = 10)
    // private String lastValidCouponSubtype;

    // NOT USED NOW
    // @Column(name = "logical_delete_ind", length = 1)
    // private String logicalDeleteInd;

    // NOT USED NOW
    // @Column(name = "lots_available_ind", length = 1)
    // private String lotsAvailableInd;

    @Column(name = "lta_chassis_number", length = 25)
    private String ltaChassisNumber;

    @Column(name = "lta_deregistration_date")
    private LocalDateTime ltaDeregistrationDate;

    @Column(name = "lta_diplomatic_flag", length = 1)
    private String ltaDiplomaticFlag;

    @Column(name = "lta_eff_ownership_date")
    private LocalDateTime ltaEffOwnershipDate;

    @Column(name = "lta_make_description", length = 100)
    private String ltaMakeDescription;

    @Column(name = "lta_max_laden_weight")
    private Integer ltaMaxLadenWeight;

    @Column(name = "lta_primary_colour", length = 100)
    private String ltaPrimaryColour;

    @Column(name = "lta_road_tax_expiry_date")
    private LocalDateTime ltaRoadTaxExpiryDate;

    @Column(name = "lta_secondary_colour", length = 100)
    private String ltaSecondaryColour;

    @Column(name = "lta_unladen_weight")
    private Integer ltaUnladenWeight;

    @Column(name = "life_status")
    private LocalDateTime lifeStatus;

    // NOT USED NOW
    // @Column(name = "model_of_vehicle", length = 50)
    // private String modelOfVehicle;

    @Column(name = "rep_obu_latitude", length = 8)
    private String repObuLatitude;

    @Column(name = "rep_obu_longitude", length = 10)
    private String repObuLongitude;

    @Column(name = "rep_operator_id", length = 10)
    private String repOperatorId;

    @Column(name = "rep_parking_end_dt")
    private LocalDateTime repParkingEndDt;

    @Column(name = "rep_parking_entry_dt")
    private LocalDateTime repParkingEntryDt;

    @Column(name = "rep_parking_exit_dt")
    private LocalDateTime repParkingExitDt;

    @Column(name = "rep_parking_start_dt")
    private LocalDateTime repParkingStartDt;

    // NOT USED NOW
    // @Column(name = "photo_no")
    // private Integer photoNo;

    // NOT USED NOW
    // @Column(name = "printed_ind", length = 1)
    // private String printedInd;

    // NOT USED NOW
    // @Column(name = "review_edit_flag", length = 50)
    // private String reviewEditFlag;

    // NOT USED NOW
    // @Column(name = "review_edit_flag_remarks", length = 255)
    // private String reviewEditFlagRemarks;

    @Column(name = "road_tax_expiry_date")
    private LocalDateTime roadTaxExpiryDate;

    // remove from DTO
    // @Column(name = "rule_11_coupon_time")
    // private LocalDateTime rule11CouponTime;

    // NOT USED NOW
    @Column(name = "rule_desc", length = 255)
    private String ruleDesc;

    // NOT USED NOW
    @Column(name = "rule_no", length = 5)
    private String ruleNo;

    @Column(name = "rule_remark_1", length = 200)
    private String ruleRemark1;

    @Column(name = "rule_remark_2", length = 200)
    private String ruleRemark2;

    // NOT USED NOW
    // @Column(name = "shift_id", length = 10)
    // private String shiftId;

    // NOT USED NOW
    // @Column(name = "start_date")
    // private LocalDateTime startDate;

    // NOT USED NOW
    // @Column(name = "start_time")
    // private LocalDateTime startTime;

    @Column(name = "sys_non_printed_comment", length = 50)
    private String sysNonPrintedComment;

    @Column(name = "total_coupons_displayed")
    private Integer totalCouponsDisplayed;

    @Column(name = "transaction_id", length = 30)
    private String transactionId;

    // NOT USED NOW
    // @Column(name = "type_of_lots", length = 1)
    // private String typeOfLots;

    // NOT USED NOW
    // @Column(name = "upload_to_cams_dt")
    // private LocalDateTime uploadToCamsDt;

    // NOT USED NOW
    // @Column(name = "upload_to_eht_host_dt")
    // private LocalDateTime uploadToEhtHostDt;

    // NOT USED NOW
    @Column(name = "user_non_printed_comment", length = 255)
    private String userNonPrintedComment;

    @Column(name = "vehicle_make", length = 50)
    private String vehicleMake;

    // NOT USED NOW
    // @Column(name = "version_no")
    // private Integer versionNo;

    @Column(name = "video_id", length = 50)
    private String videoId;

    @Column(name = "rep_violation_code", length = 2)
    private String repViolationCode;

    // NOT USED NOW
    // @Column(name = "unpaid_parking_charge", precision = 19, scale = 2)
    // @JsonSerialize(using = ToStringSerializer.class)
    // private BigDecimal unpaidParkingCharge;

    // NOT USED NOW
    // @Column(name = "person_report_name", length = 66)
    // private String personReportName;

    // NOT USED NOW
    // @Column(name = "report_date")
    // private LocalDateTime reportDate;

    // NOT USED NOW
    // @Column(name = "service_provider_name", length = 66)
    // private String serviceProviderName;

    // LTA fields moved to OcmsOffenceNoticeAddress entity    
}
