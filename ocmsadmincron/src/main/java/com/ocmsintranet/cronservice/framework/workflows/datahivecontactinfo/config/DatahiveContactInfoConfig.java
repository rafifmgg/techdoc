package com.ocmsintranet.cronservice.framework.workflows.datahivecontactinfo.config;

import com.ocmsintranet.cronservice.framework.common.ExcelReportConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration for DataHive Contact Information Excel Report Generation
 *
 * This class defines the structure and formatting for DataHive contact information reports.
 * It implements ExcelReportConfig to provide standardized configuration for the
 * global Excel generation helper.
 *
 * Report Structure:
 * - Summary Sheet: Key-value pairs with DataHive contact retrieval statistics
 * - Error Sheet: 13 columns showing failed DataHive lookups with error details
 * - Success Sheet: 12 columns showing successful DataHive contact retrievals
 */
@Component
public class DatahiveContactInfoConfig implements ExcelReportConfig {

    @Override
    public String getReportTitle() {
        return "OCMS SUMMARY REPORT - DATAHIVE CONTACT INFORMATION LISTING";
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
        return "Report Datahive Contact Information_{date}.xlsx";
    }

    @Override
    public String getLocalSaveDirectory() {
        return System.getProperty("user.home") + "/DataHive_Reports/";
    }

    /**
     * Configuration for Summary sheet
     * Uses key-value format with 7 rows of DataHive statistics
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
     * 13 columns showing failed DataHive lookups with detailed error information
     */
    private SheetConfig createErrorSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                      // Serial Number
            "NOTICE NO",                // Notice identifier
            "DATE/TIME OF OFFENCE",     // When the offence occurred
            "OFFENCE TYPE",             // Type of offence (O/E)
            "PROCESSING STAGE",         // Current stage (ENA, RD1, etc.)
            "ID NO",                    // NRIC/FIN/Business registration
            "ID TYPE",                  // Type of ID (N=NRIC, B=Business, F=FIN)
            "NAME",                     // Recipient full name
            "MOBILE NO./EMAIL",         // Contact information
            "DATE/TIME SENT",           // When request sent to DataHive
            "DATASET",                  // Source system (Singpass/Corppass)
            "DATE/TIME RECEIVED",       // When response received from DataHive
            "ERROR MESSAGE"             // Error description
        );

        List<Integer> widths = Arrays.asList(
            1500, // S/N
            3500, // NOTICE NO
            4500, // DATE/TIME OF OFFENCE
            2500, // OFFENCE TYPE
            3000, // PROCESSING STAGE
            3000, // ID NO
            2000, // ID TYPE
            4000, // NAME
            4000, // MOBILE NO./EMAIL
            4500, // DATE/TIME SENT
            3000, // DATASET
            4500, // DATE/TIME RECEIVED
            5000  // ERROR MESSAGE
        );

        return new SheetConfig("Error", headers, widths, getErrorColumnKeys(), Map.of());
    }

    /**
     * Configuration for Success sheet
     * 12 columns showing successful DataHive contact retrievals
     */
    private SheetConfig createSuccessSheetConfig() {
        List<String> headers = Arrays.asList(
            "S/N",                      // Serial Number
            "NOTICE NO",                // Notice identifier
            "DATE/TIME OF OFFENCE",     // When the offence occurred
            "OFFENCE TYPE",             // Type of offence (O/E)
            "PROCESSING STAGE",         // Current stage (ENA, RD1, etc.)
            "ID NO",                    // NRIC/FIN/Business registration
            "ID TYPE",                  // Type of ID (N=NRIC, B=Business, F=FIN)
            "NAME",                     // Recipient full name
            "MOBILE NO./EMAIL",         // Contact information
            "DATE/TIME SENT",           // When request sent to DataHive
            "DATASET",                  // Source system (Singpass/Corppass)
            "DATE/TIME RECEIVED"        // When response received from DataHive
        );

        List<Integer> widths = Arrays.asList(
            1500, // S/N
            3500, // NOTICE NO
            4500, // DATE/TIME OF OFFENCE
            2500, // OFFENCE TYPE
            3000, // PROCESSING STAGE
            3000, // ID NO
            2000, // ID TYPE
            4000, // NAME
            4000, // MOBILE NO./EMAIL
            4500, // DATE/TIME SENT
            3000, // DATASET
            4500  // DATE/TIME RECEIVED
        );

        return new SheetConfig("Success", headers, widths, getSuccessColumnKeys(), Map.of());
    }

    /**
     * Get column keys mapping for Error sheet data extraction
     */
    public List<String> getErrorColumnKeys() {
        return Arrays.asList(
            "serial_number",         // S/N
            "notice_no",             // NOTICE NO
            "offence_date_time",     // DATE/TIME OF OFFENCE
            "offence_type",          // OFFENCE TYPE
            "processing_stage",      // PROCESSING STAGE
            "id_no",                 // ID NO
            "id_type",               // ID TYPE
            "name",                  // NAME
            "mobile_email",          // MOBILE NO./EMAIL
            "date_time_sent",        // DATE/TIME SENT
            "dataset",               // DATASET
            "date_time_received",    // DATE/TIME RECEIVED
            "error_message"          // ERROR MESSAGE
        );
    }

    /**
     * Get column keys mapping for Success sheet data extraction
     */
    public List<String> getSuccessColumnKeys() {
        return Arrays.asList(
            "serial_number",         // S/N
            "notice_no",             // NOTICE NO
            "offence_date_time",     // DATE/TIME OF OFFENCE
            "offence_type",          // OFFENCE TYPE
            "processing_stage",      // PROCESSING STAGE
            "id_no",                 // ID NO
            "id_type",               // ID TYPE
            "name",                  // NAME
            "mobile_email",          // MOBILE NO./EMAIL
            "date_time_sent",        // DATE/TIME SENT
            "dataset",               // DATASET
            "date_time_received"     // DATE/TIME RECEIVED
        );
    }
}