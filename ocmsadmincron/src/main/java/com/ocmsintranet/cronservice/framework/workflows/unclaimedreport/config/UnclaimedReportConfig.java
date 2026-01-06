package com.ocmsintranet.cronservice.framework.workflows.unclaimedreport.config;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Unclaimed Batch Data Excel Report Generation
 * Based on OCMS 20 Specification
 *
 * This class defines the structure and formatting for Unclaimed Batch Data reports.
 * It implements ExcelReportConfig to provide standardized configuration for the
 * global Excel generation helper.
 *
 * Report Structure (as per OCMS 20 spec):
 * - Single sheet with 11 columns showing unclaimed reminder details
 */
@Component
public class UnclaimedReportConfig implements ExcelReportConfig {

    @Override
    public String getReportTitle() {
        return "OCMS Unclaimed Batch Data Report";
    }

    @Override
    public List<SheetConfig> getSheetConfigurations() {
        return Arrays.asList(createUnclaimedDataSheetConfig());
    }

    @Override
    public String getFileNamePattern() {
        return "UnclaimedBatchData_{date}.xlsx";
    }

    @Override
    public String getLocalSaveDirectory() {
        return System.getProperty("user.home") + "/Unclaimed_Reports/";
    }

    /**
     * Configuration for Unclaimed Data sheet
     * 11 columns showing unclaimed reminder details
     */
    private SheetConfig createUnclaimedDataSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                          // Serial Number
            "Notice Number",                // Notice identifier
            "Offence Type",                 // Type of offence
            "Offence Date & Time",          // When offence occurred
            "Vehicle No",                   // Vehicle registration
            "Offender ID",                  // NRIC/FIN number
            "Offender Name",                // Full name
            "Reminder Letter Date",         // Date reminder was sent
            "Days Since Letter Sent",       // Number of days elapsed
            "Current Processing Stage",     // Current stage (e.g., RD1, RD2)
            "Suspension Status"             // TS-UNC status
        );

        List<Integer> widths = Arrays.asList(
            1500,  // S/N
            3000,  // Notice Number
            3000,  // Offence Type
            4000,  // Offence Date & Time
            3000,  // Vehicle No
            3000,  // Offender ID
            4000,  // Offender Name
            3000,  // Reminder Letter Date
            2500,  // Days Since Letter Sent
            3500,  // Current Processing Stage
            3000   // Suspension Status
        );

        Map<String, Object> additionalConfig = Map.of(
            "includeTitle", true,
            "sheetTitle", getReportTitle()
        );

        return new SheetConfig("Unclaimed Data", headers, widths, getColumnKeys(), additionalConfig);
    }

    /**
     * Get column keys mapping for data extraction
     */
    public List<String> getColumnKeys() {
        return Arrays.asList(
            "serial_number",            // S/N
            "notice_no",                // Notice Number
            "offence_type",             // Offence Type
            "offence_date_time",        // Offence Date & Time
            "vehicle_no",               // Vehicle No
            "offender_id",              // Offender ID
            "offender_name",            // Offender Name
            "reminder_letter_date",     // Reminder Letter Date
            "days_since_letter",        // Days Since Letter Sent
            "processing_stage",         // Current Processing Stage
            "suspension_status"         // Suspension Status
        );
    }
}
