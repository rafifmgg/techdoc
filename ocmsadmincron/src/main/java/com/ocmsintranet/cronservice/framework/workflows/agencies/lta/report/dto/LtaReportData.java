package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for LTA VRLS Vehicle Ownership Check Report.
 * Based on the LTA VRLS Vehicle Ownership Check Result specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LtaReportData {

    // Core notice information
    private String noticeNo;
    private String vehicleNo;
    private String vehicleCategory;
    private String offenceNoticeType;
    private LocalDateTime noticeDateAndTime;
    private BigDecimal compositionAmount;

    // Processing fields (from actual query)
    private LocalDateTime processingDateTime; // from onoda.processing_date_time
    private String errorCode; // from onoda.error_code (LTA response code)

    // LTA Response specific fields
    private LocalDate ltaDeregistrationDate; // from ond.lta_deregistration_date

    // Car Park information (from query)
    private String ppCode; // from von.pp_code (car park code)
    private String ppName; // from von.pp_name (car park name)

    // Processing type information
    private String suspensionType; // from von.suspension_type
    private String eprReasonOfSuspension; // from von.epr_reason_of_suspension

    // Owner information (from query)
    private String ownerName; // from onod.name
    private String idType;
    private String nricNo;
    private String passportPlaceOfIssue;

    // Address information
    private String regBlkHseNo;
    private String regStreet;
    private String regFloor;
    private String regUnit;
    private String regBldg;
    private String regPostalCode;
    private LocalDateTime ltaRegAddressEffectiveDate;

    // Additional vehicle details (if needed)
    private String ltaChassisNumber;
    private String ltaMakeDescription;
    private String ltaPrimaryColour;
    private String ltaSecondaryColour;
    private LocalDateTime ltaRoadTaxExpiryDate;
    private Integer ltaUnladenWeight;
    private Integer ltaMaxLadenWeight;
    private LocalDateTime ltaEffOwnershipDate;
    private String vehicleMake;
    private String colorOfVehicle;
    private String subsystemLabel;

    // Report metadata
    private String reportType;
    private String reportPeriod;
    private LocalDateTime reportGeneratedAt;
    private String reportStatus;
    private String errorMessage;

    // Processing information
    private String nextProcessingStage;
    private LocalDateTime nextProcessingDate;
    private String currentStatus;
    private LocalDateTime statusUpdatedAt;
}