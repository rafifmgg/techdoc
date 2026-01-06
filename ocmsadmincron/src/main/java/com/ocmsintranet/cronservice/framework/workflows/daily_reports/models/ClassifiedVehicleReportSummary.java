package com.ocmsintranet.cronservice.framework.workflows.daily_reports.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * OCMS 14: Summary statistics for Classified Vehicle (VIP) Report
 * Used in both email body and Excel summary sheet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifiedVehicleReportSummary {

    /**
     * Report generation date
     */
    private LocalDate reportDate;

    /**
     * Total number of Type V notices issued
     */
    private int totalNoticesIssued;

    /**
     * Number of outstanding (unpaid) notices
     */
    private int outstandingNotices;

    /**
     * Number of settled (paid) notices
     */
    private int settledNotices;

    /**
     * Number of notices amended from V to S (TS-CLV revived)
     */
    private int amendedNotices;
}
