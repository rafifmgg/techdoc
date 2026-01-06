package com.ocmsintranet.cronservice.framework.workflows.hstreport.config;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Monthly HST Excel Report Generation
 * Based on OCMS 20 Specification
 *
 * This class defines the structure and formatting for Monthly HST reports.
 * It implements ExcelReportConfig to provide standardized configuration for the
 * global Excel generation helper.
 *
 * Report Structure (as per OCMS 20 spec):
 * - Single sheet with 10 columns showing HST records and associated suspended notices
 */
@Component
public class HstReportConfig implements ExcelReportConfig {

    @Override
    public String getReportTitle() {
        return "OCMS Monthly HST Report";
    }

    @Override
    public List<SheetConfig> getSheetConfigurations() {
        return Arrays.asList(createHstDataSheetConfig());
    }

    @Override
    public String getFileNamePattern() {
        return "MonthlyHSTReport_{date}.xlsx";
    }

    @Override
    public String getLocalSaveDirectory() {
        return System.getProperty("user.home") + "/HST_Reports/";
    }

    /**
     * Configuration for HST Data sheet
     * 10 columns showing HST records and suspended notices
     */
    private SheetConfig createHstDataSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                          // Serial Number
            "Offender ID",                  // NRIC/FIN number from ocms_hst
            "Offender Name",                // Name from ocms_hst
            "Address",                      // Full address from ocms_hst
            "Notice Numbers",               // Comma-separated list of suspended notice numbers
            "Total Suspended Notices",      // Count of suspended notices
            "HST Created Date",             // When HST record was created
            "First Suspension Date",        // Earliest TS-HST suspension date
            "Latest Suspension Date",       // Most recent TS-HST suspension date
            "Status"                        // Active/Inactive
        );

        List<Integer> widths = Arrays.asList(
            1500,  // S/N
            3000,  // Offender ID
            4000,  // Offender Name
            6000,  // Address
            5000,  // Notice Numbers
            2500,  // Total Suspended Notices
            3000,  // HST Created Date
            3000,  // First Suspension Date
            3000,  // Latest Suspension Date
            2500   // Status
        );

        Map<String, Object> additionalConfig = Map.of(
            "includeTitle", true,
            "sheetTitle", getReportTitle()
        );

        return new SheetConfig("HST Data", headers, widths, getColumnKeys(), additionalConfig);
    }

    /**
     * Get column keys mapping for data extraction
     */
    public List<String> getColumnKeys() {
        return Arrays.asList(
            "serial_number",            // S/N
            "id_no",                    // Offender ID
            "name",                     // Offender Name
            "address",                  // Address
            "notice_numbers",           // Notice Numbers
            "total_suspended_notices",  // Total Suspended Notices
            "hst_created_date",         // HST Created Date
            "first_suspension_date",    // First Suspension Date
            "latest_suspension_date",   // Latest Suspension Date
            "status"                    // Status
        );
    }
}
