package com.ocmsintranet.cronservice.framework.workflows.notificationreport.config;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration for eNotification Excel Report Generation
 *
 * This class defines the structure and formatting for eNotification reports.
 * It implements ExcelReportConfig to provide standardized configuration for the
 * global Excel generation helper.
 *
 * Report Structure:
 * - Summary Sheet: Key-value pairs with notification statistics by processing stage
 * - Error Sheet: 12 columns showing failed notifications with error details
 * - Success Sheet: 11 columns showing successful notifications
 */
@Component
public class NotificationReportConfig implements ExcelReportConfig {

    @Override
    public String getReportTitle() {
        return "OCMS SUMMARY REPORT - Daily Sent eNotification List";
    }

    @Override
    public List<SheetConfig> getSheetConfigurations() {
        return Arrays.asList(
            createSummarySheetConfig(),
            createErrorSheetConfig(),
            createSuccessSheetConfig()
        );
    }

    @Override
    public String getFileNamePattern() {
        return "ReportENotificationSent_{date}.xlsx";
    }

    @Override
    public String getLocalSaveDirectory() {
        return System.getProperty("user.home") + "/eNotification_Reports/";
    }

    /**
     * Configuration for Summary sheet
     * Uses key-value format with 39 rows of statistics
     */
    private SheetConfig createSummarySheetConfig() {
        List<String> headers = Arrays.asList("Description", "Value");
        List<Integer> widths = Arrays.asList(15000, 6000);

        Map<String, Object> additionalConfig = Map.of(
            "includeTitle", true,
            "sheetTitle", getReportTitle(),
            "keyValueFormat", true
        );

        return new SheetConfig("Summary", headers, widths, additionalConfig);
    }

    /**
     * Configuration for Error sheet
     * 12 columns showing failed notifications with detailed error information
     */
    private SheetConfig createErrorSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                  // Serial Number
            "Notice No.",           // Notice identifier
            "Mobile No / Email",    // Contact information
            "Recipient Name",       // Full name
            "ID No",               // NRIC/FIN number
            "ID Type",             // Type of ID (N=NRIC)
            "Processing Stage",     // Current stage (ENA, RD1, etc.)
            "Message Type",         // Type of message
            "Error Message",        // Detailed error description
            "Send Date",           // Date notification sent
            "Send Time",           // Time notification sent
            "Status"               // Status (Error)
        );

        List<Integer> widths = Arrays.asList(
            1500, // S/N
            3000, // Notice No.
            4000, // Mobile No / Email
            4000, // Recipient Name
            3000, // ID No
            2000, // ID Type
            3000, // Processing Stage
            3000, // Message Type
            5000, // Error Message
            3000, // Send Date
            3000, // Send Time
            2000  // Status
        );

        return new SheetConfig("Error", headers, widths, getErrorColumnKeys(), Map.of());
    }

    /**
     * Configuration for Success sheet
     * 11 columns showing successful notifications (same as Error minus Error Message)
     */
    private SheetConfig createSuccessSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                  // Serial Number
            "Notice No.",           // Notice identifier
            "Mobile No / Email",    // Contact information
            "Recipient Name",       // Full name
            "ID No",               // NRIC/FIN number
            "ID Type",             // Type of ID (N=NRIC)
            "Processing Stage",     // Current stage (ENA, RD1, etc.)
            "Message Type",         // Type of message
            "Send Date",           // Date notification sent
            "Send Time",           // Time notification sent
            "Status"               // Status (Success)
        );

        List<Integer> widths = Arrays.asList(
            1500, // S/N
            3000, // Notice No.
            4000, // Mobile No / Email
            4000, // Recipient Name
            3000, // ID No
            2000, // ID Type
            3000, // Processing Stage
            3000, // Message Type
            3000, // Send Date
            3000, // Send Time
            2000  // Status
        );

        return new SheetConfig("Success", headers, widths, getSuccessColumnKeys(), Map.of());
    }

    /**
     * Get column keys mapping for Error sheet data extraction
     */
    public List<String> getErrorColumnKeys() {
        return Arrays.asList(
            "serial_number",      // S/N
            "notice_no",          // Notice No.
            "contact",            // Mobile No / Email
            "name",               // Recipient Name
            "id_no",              // ID No
            "id_type",            // ID Type
            "processing_stage",   // Processing Stage
            "message_type",       // Message Type
            "error_message",      // Error Message
            "send_date",          // Send Date
            "send_time",          // Send Time
            "status"              // Status
        );
    }

    /**
     * Get column keys mapping for Success sheet data extraction
     */
    public List<String> getSuccessColumnKeys() {
        return Arrays.asList(
            "serial_number",      // S/N
            "notice_no",          // Notice No.
            "contact",            // Mobile No / Email
            "name",               // Recipient Name
            "id_no",              // ID No
            "id_type",            // ID Type
            "processing_stage",   // Processing Stage
            "message_type",       // Message Type
            "send_date",          // Send Date
            "send_time",          // Send Time
            "status"              // Status
        );
    }
}