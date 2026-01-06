package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for LTA Upload data containing all required fields from multiple tables.
 * This combines data from ocms_valid_offence_notice, ocms_offence_notice_detail, and ocms_offence_notice_owner_driver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LtaUploadData {
    // From ocms_valid_offence_notice
    private String noticeNo;
    private String vehicleNo;
    private String vehicleCategory;
    private String nextProcessingStage;
    private LocalDateTime nextProcessingDate;
    private String offenceNoticeType;
    private String suspensionType;
    private String eprReasonOfSuspension;
    private String ppCode;
    private String parkingLotNo;
    private LocalDateTime noticeDateAndTime;
    private BigDecimal compositionAmount;
    private String subsystemLabel;
    
    // From ocms_offence_notice_detail
    private String ltaChassisNumber;
    private String ltaDiplomaticFlag;
    private String ltaMakeDescription;
    private String ltaPrimaryColour;
    private String ltaSecondaryColour;
    private LocalDateTime ltaRoadTaxExpiryDate;
    private Integer ltaUnladenWeight;
    private Integer ltaMaxLadenWeight;
    private LocalDateTime ltaEffOwnershipDate;
    private LocalDateTime ltaDeregistrationDate;
    private String vehicleMake;
    private String colorOfVehicle;
    private String iuNo;
    private String repViolationCode;
    private LocalDateTime repParkingStartDt;
    private String ltaErrorCode;
    
    // From ocms_offence_notice_owner_driver
    private String ownerName;
    private String idType;
    private String nricNo;
    private String passportPlaceOfIssue;
    private String regBlkHseNo;
    private String regStreet;
    private String regFloor;
    private String regUnit;
    private String regBldg;
    private String regPostalCode;
    private LocalDateTime ltaRegAddressEffectiveDate;
    private String ltaMailingBlockHouseNumber;
    private String ltaMailingStreetName;
    private String ltaMailingFloorNumber;
    private String ltaMailingUnitNumber;
    private String ltaMailingBuildingName;
    private String ltaMailingPostalCode;
}
