package com.ocmsintranet.apiservice.workflows.reports.system.hst;

import java.util.Map;

/**
 * Service interface for HST ad-hoc report generation
 * OCMS 20: Section 3.6 - Ad-hoc Reports
 */
public interface HstAdHocReportService {

    /**
     * Generate Hirer/Driver Furnished Report
     * Shows HST records where address was furnished by Hirer (H) or Driver (D)
     *
     * @param startDate Optional start date filter (yyyy-MM-dd)
     * @param endDate Optional end date filter (yyyy-MM-dd)
     * @return Map with reportUrl and recordCount
     */
    Map<String, Object> generateHirerDriverFurnishedReport(String startDate, String endDate);

    /**
     * Generate Alternative Address Furnished Report
     * Shows HST records where address was furnished by eService (ES) or LTA
     *
     * @param startDate Optional start date filter (yyyy-MM-dd)
     * @param endDate Optional end date filter (yyyy-MM-dd)
     * @return Map with reportUrl and recordCount
     */
    Map<String, Object> generateAlternativeAddressFurnishedReport(String startDate, String endDate);
}
