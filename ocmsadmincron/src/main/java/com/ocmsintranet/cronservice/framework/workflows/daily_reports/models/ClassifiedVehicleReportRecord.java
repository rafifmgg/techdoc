package com.ocmsintranet.cronservice.framework.workflows.daily_reports.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCMS 14: Model for Classified Vehicle (VIP) Report record
 * Daily report for notices issued to Type V (VIP) vehicles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifiedVehicleReportRecord {

    /**
     * Notice number
     */
    private String noticeNo;

    /**
     * Vehicle number
     */
    private String vehicleNo;

    /**
     * Vehicle registration type ('V' = VIP)
     */
    private String vehicleRegistrationType;

    /**
     * Date notice was issued
     */
    private LocalDateTime noticeDate;

    /**
     * Offence rule code
     */
    private String offenceRuleCode;

    /**
     * Original composition amount
     */
    private BigDecimal compositionAmount;

    /**
     * Current amount payable
     */
    private BigDecimal amountPayable;

    /**
     * Suspension type (if suspended)
     */
    private String suspensionType;

    /**
     * Suspension reason (if suspended)
     */
    private String suspensionReason;

    /**
     * Suspension date (if suspended)
     */
    private LocalDateTime suspensionDate;

    /**
     * Place of offence (car park name)
     */
    private String ppName;

    /**
     * Place of offence code
     */
    private String ppCode;

    /**
     * Payment status (Outstanding/Settled)
     */
    private String paymentStatus;

    /**
     * For amended notices: Date TS-CLV was applied
     */
    private LocalDateTime tsClvSuspensionDate;

    /**
     * For amended notices: Date TS-CLV was revived (V→S amendment)
     */
    private LocalDateTime tsClvRevivalDate;

    /**
     * For amended notices: Current type after amendment
     */
    private String currentType;
}
