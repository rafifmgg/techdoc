package com.ocmsintranet.cronservice.framework.workflows.daily_reports.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCMS 14: Model for RIP Hirer/Driver Furnished Report record
 * Daily report for notices suspended under PS-RP2 where offender is Hirer/Driver
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RipHirerDriverReportRecord {

    /**
     * Notice number
     */
    private String noticeNo;

    /**
     * Suspension date
     */
    private LocalDateTime suspensionDate;

    /**
     * Suspension record number
     */
    private Integer srNo;

    /**
     * Offender name (Hirer or Driver)
     */
    private String offenderName;

    /**
     * ID type (NRIC, FIN, Passport, etc.)
     */
    private String idType;

    /**
     * ID number
     */
    private String idNo;

    /**
     * Owner/Driver/Hirer indicator ('H' = Hirer, 'D' = Driver)
     */
    private String ownerDriverIndicator;

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
     * Vehicle number
     */
    private String vehicleNo;

    /**
     * Place of offence (car park name)
     */
    private String ppName;
}
