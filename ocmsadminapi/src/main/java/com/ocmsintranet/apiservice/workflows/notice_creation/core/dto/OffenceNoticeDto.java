package com.ocmsintranet.apiservice.workflows.notice_creation.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for mapping incoming data to OcmsValidOffenceNotice and OffenceNoticeDetail entities.
 * Fields marked with @jakarta.validation.constraints.NotNull are mandatory based on the entity constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OffenceNoticeDto {
    
    // Common primary key for both entities - mandatory
    // @NotNull
    private String noticeNo;
    
    // --- OcmsValidOffenceNotice fields ---
    private BigDecimal administrationFee;
    private BigDecimal adviceSurcharge;
    private BigDecimal amountPaid;
    private BigDecimal amountPayable;
    private String anFlag;
    private LocalDateTime assignDate;
    private String assignOfficer;
    
    // @NotNull
    private BigDecimal compositionAmount; 
    
    // @NotNull
    private Integer computerRuleCode;
    
    // @NotNull
    private LocalDateTime creDate;
    
    // @NotNull
    private String creUserId;
    
    private LocalDateTime crsDateOfSuspension;
    private String crsReasonOfSuspension;
    private String decision;
    private LocalDateTime decisionDate;
    private String decisionOfficer;
    private String decisionRemark;
    private LocalDateTime dueDateOfRevival;
    private LocalDateTime eprDateOfSuspension;
    private String eprReasonOfSuspension;
    private BigDecimal incentivePaytAmt;
    private LocalDateTime incentivePaytDate;
    
    // @NotNull
    private LocalDateTime lastProcessingDate;
    
    // @NotNull
    private String lastProcessingStage;
    
    private LocalDateTime lastRdpDnDate;
    private String lastRdpDnStage;
    private LocalDateTime nextProcessingDate;
    private String nextProcessingStage;
    
    // @NotNull
    private LocalDateTime noticeDateAndTime;
    
    // @NotNull
    private String offenceNoticeType;
    private String eserviceMessageCode;

    private String otherRemark;
    private BigDecimal parkingFee;
    private String parkingLotNo;
    private String paymentAcceptanceAllowed;
    private LocalDateTime paymentDueDate;
    private String paymentStatus;
    
    // @NotNull
    private String ppCode;
    
    // @NotNull
    private String ppName;
    
    private LocalDateTime prevProcessingDate;
    private String prevProcessingStage;
    private String ruleRemarks;
    private BigDecimal surcharge;
    private String suspTypeUpdateAllowed;
    private String suspensionType;
    private LocalDateTime updDate;
    private String updUserId;
    
    // @NotNull
    private String vehicleCategory;
    
    // @NotNull
    private String vehicleNo;
    
    private String vehicleOwnerType;
    private String vehicleRegistrationType;
    // private LocalDateTime sentToRepDate;
    private String subsystemLabel;
    private LocalDateTime eprReasonSuspensionDate;
    private LocalDateTime crsReasonSuspensionDate;
    private String wardenNo;
    private BigDecimal repChargeAmount;

    // --- OffenceNoticeDetail fields ---
    //private LocalDateTime batchDate;
    //private Integer batchNo;
    //private String chasisNo;
    private String colorOfVehicle;
    private String comments;
    private String conditionInvalidCoupon1;
    private String conditionInvalidCoupon2;
    private String createdBy;
    private LocalDateTime createdDt;
    private BigDecimal denomInvalidCoupon1;
    private BigDecimal denomInvalidCoupon2;
    private BigDecimal denomOfValidCoupon;
  //  private String ehtId;
  //  private LocalDateTime endDate;
  //  private LocalDateTime endTime;
   private Integer expiredCoupon1Denom;
   private String expiredCoupon1No;
   private String expiredCoupon1Subtype;
   private Integer expiredCoupon2Denom;
   private String expiredCoupon2No;
   private String expiredCoupon2Subtype;
    private LocalDateTime expiryTime;
   private LocalDateTime firstCouponTime;
 //   private LocalDateTime handicapFirstHitDt;
    private String imageId;
    private String invalidCoupon1CreasedTab;
    private String invalidCoupon1Subtype;
    private String invalidCoupon2CreasedTab;
    private String invalidCoupon2Subtype;
    private String conditionInvalidCoupon3;
    private String invalidCoupon3CreasedTab;
    private BigDecimal denomInvalidCoupon3;
    private String invalidCouponNo3;
    private String invalidCoupon3Subtype;
    private String invalidCouponNo1;
    private String invalidCouponNo2;
    private String iuNo;
  //  private String labelTypeTag;
  //  private String lastModifiedBy;
 //   private LocalDateTime lastModifiedDt;
   private Integer lastValidCouponDenom;
   private LocalDateTime lastValidCouponExpiredDate;
   private LocalDateTime lastValidCouponExpiredTime;

    private String lastValidCouponNo;
    private String lastValidCouponSubtype;
//    private String logicalDeleteInd;
//    private String lotsAvailableInd;
    private String ltaChassisNumber;
    private LocalDateTime ltaDeregistrationDate;
    private String ltaDiplomaticFlag;
    private LocalDateTime ltaEffOwnershipDate;
    private String ltaMakeDescription;
    private Integer ltaMaxLadenWeight;
    private String ltaPrimaryColour;
    private LocalDateTime ltaRoadTaxExpiryDate;
    private String ltaSecondaryColour;
    private Integer ltaUnladenWeight;
    private String modelOfVehicle;
    private String repObuLatitude;
    private String repObuLongitude;
    private String repOperatorId;
    private LocalDateTime repParkingEndDt;
    private LocalDateTime repParkingEntryDt;
    private LocalDateTime repParkingExitDt;
    private LocalDateTime repParkingStartDt;
    private Integer photoNo;
    private String printedInd;
    private String reviewEditFlag;
    private String reviewEditFlagRemarks;
    private LocalDateTime roadTaxExpiryDate;
    private LocalDateTime rule11CouponTime;
    private String ruleDesc;
    private String ruleNo;
    private String ruleRemark1;
    private String ruleRemark2;
    private String shiftId;
    private LocalDateTime startDate;
    private LocalDateTime startTime;
    private String sysNonPrintedComment;
    private Integer totalCouponsDisplayed;
    private String transactionId;
    private String typeOfLots;
    private LocalDateTime uploadToCamsDt;
    private LocalDateTime uploadToEhtHostDt;
    private String userNonPrintedComment;
    private String vehicleMake;
    private Integer versionNo;
    private String videoId;
    private String repViolationCode;
    private BigDecimal unpaidParkingCharge;
    private String personReportName;
    private LocalDateTime reportDate;
    private String serviceProviderName;
    
    // --- File attachment fields ---
    private List<String> photos;
    private List<String> videos;
}
