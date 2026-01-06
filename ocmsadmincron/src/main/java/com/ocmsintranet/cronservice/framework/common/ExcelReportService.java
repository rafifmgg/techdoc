package com.ocmsintranet.cronservice.framework.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel Report Service
 *
 * A high-level service that combines ExcelGenerationHelper and ExcelReportConfig
 * to provide easy, standardized Excel report generation for any workflow.
 *
 * This service handles:
 * - Configuration-driven report generation
 * - Automatic file naming with date patterns
 * - Local file saving for debugging
 * - Standardized sheet creation based on data types
 * - Error handling and logging
 *
 * Usage Example:
 * ```java
 * @Autowired
 * private ExcelReportService excelReportService;
 *
 * ExcelReportConfig config = new MhaReportConfig();
 * List<ExcelReportConfig.SheetData> sheetDataList = Arrays.asList(
 *     new ExcelReportConfig.SheetData("Summary", summaryData, SheetDataType.KEY_VALUE_MAP),
 *     new ExcelReportConfig.SheetData("Success", successRecords, SheetDataType.MAP_LIST)
 * );
 *
 * byte[] excelBytes = excelReportService.generateReport(config, sheetDataList, "2024-01-15");
 * ```
 */
@Slf4j
@Service
public class ExcelReportService {

    private final ExcelGenerationHelper excelHelper;

    public ExcelReportService(ExcelGenerationHelper excelHelper) {
        this.excelHelper = excelHelper;
    }

    /**
     * Generate an Excel report based on configuration and data
     *
     * @param config Report configuration defining structure
     * @param sheetDataList Data for each sheet
     * @param reportDate Date for the report (used in filename)
     * @return Excel file as byte array
     */
    public byte[] generateReport(ExcelReportConfig config, List<ExcelReportConfig.SheetData> sheetDataList, String reportDate) {
        try {
            log.info("Generating Excel report: {}", config.getReportTitle());

            ExcelGenerationHelper.ExcelWorkbookBuilder workbookBuilder = excelHelper.createWorkbook(config.getReportTitle());

            // Create each sheet based on configuration
            for (ExcelReportConfig.SheetConfig sheetConfig : config.getSheetConfigurations()) {
                ExcelReportConfig.SheetData sheetData = findSheetData(sheetDataList, sheetConfig.getSheetName());

                if (sheetData != null) {
                    createSheet(workbookBuilder, sheetConfig, sheetData);
                } else {
                    log.warn("No data found for sheet: {}", sheetConfig.getSheetName());
                    createEmptySheet(workbookBuilder, sheetConfig);
                }
            }

            // Build and optionally save locally
            String fileName = generateFileName(config.getFileNamePattern(), reportDate);
            byte[] excelBytes = workbookBuilder.buildAndSave(config.getLocalSaveDirectory(), fileName);

            log.info("Excel report generated successfully: {} bytes", excelBytes.length);
            return excelBytes;

        } catch (Exception e) {
            log.error("Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    /**
     * Generate an Excel report with automatic date (yesterday)
     *
     * @param config Report configuration
     * @param sheetDataList Data for sheets
     * @return Excel file as byte array
     */
    public byte[] generateReport(ExcelReportConfig config, List<ExcelReportConfig.SheetData> sheetDataList) {
        String reportDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return generateReport(config, sheetDataList, reportDate);
    }

    /**
     * Create a sheet based on configuration and data
     */
    private void createSheet(ExcelGenerationHelper.ExcelWorkbookBuilder workbookBuilder,
                           ExcelReportConfig.SheetConfig sheetConfig,
                           ExcelReportConfig.SheetData sheetData) {

        ExcelGenerationHelper.ExcelSheetBuilder sheetBuilder = workbookBuilder.addSheet(sheetConfig.getSheetName());

        // Add title if configured
        if (sheetConfig.includeTitle()) {
            sheetBuilder.addTitle(sheetConfig.getSheetTitle());
        }

        // Set column widths
        if (!sheetConfig.getColumnWidths().isEmpty()) {
            sheetBuilder.setColumnWidths(sheetConfig.getColumnWidths());
        }

        // Handle different data types
        switch (sheetData.getDataType()) {
            case KEY_VALUE_MAP:
                if (sheetConfig.isKeyValueFormat()) {
                    sheetBuilder.addKeyValueRows(sheetData.getAsKeyValueMap());
                } else {
                    // Convert key-value to tabular format
                    sheetBuilder.addHeaders(List.of("Field", "Value"));
                    Map<String, Object> kvData = sheetData.getAsKeyValueMap();
                    for (Map.Entry<String, Object> entry : kvData.entrySet()) {
                        sheetBuilder.addDataRow(List.of(entry.getKey(), entry.getValue()));
                    }
                }
                break;

            case MAP_LIST:
                // Add headers
                sheetBuilder.addHeaders(sheetConfig.getColumnHeaders());
                // Add data rows
                sheetBuilder.addDataRowsFromMaps(sheetData.getAsMapList(), extractColumnKeys(sheetConfig));
                break;

            case LIST_OF_LISTS:
                // Add headers
                sheetBuilder.addHeaders(sheetConfig.getColumnHeaders());
                // Add data rows
                sheetBuilder.addDataRows(sheetData.getAsListOfLists());
                break;
        }

        sheetBuilder.finishSheet();
    }

    /**
     * Create an empty sheet with just headers
     */
    private void createEmptySheet(ExcelGenerationHelper.ExcelWorkbookBuilder workbookBuilder,
                                ExcelReportConfig.SheetConfig sheetConfig) {

        ExcelGenerationHelper.ExcelSheetBuilder sheetBuilder = workbookBuilder.addSheet(sheetConfig.getSheetName());

        if (sheetConfig.includeTitle()) {
            sheetBuilder.addTitle(sheetConfig.getSheetTitle());
        }

        if (!sheetConfig.getColumnWidths().isEmpty()) {
            sheetBuilder.setColumnWidths(sheetConfig.getColumnWidths());
        }

        // Add headers only
        sheetBuilder.addHeaders(sheetConfig.getColumnHeaders());
        sheetBuilder.finishSheet();
    }

    /**
     * Find sheet data by name
     */
    private ExcelReportConfig.SheetData findSheetData(List<ExcelReportConfig.SheetData> sheetDataList, String sheetName) {
        return sheetDataList.stream()
                .filter(data -> data.getSheetName().equals(sheetName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract column keys from sheet configuration
     * This assumes the column headers map to data keys in some way
     */
    private List<String> extractColumnKeys(ExcelReportConfig.SheetConfig sheetConfig) {
        // Use explicit column keys if provided, otherwise auto-generate from headers
        if (sheetConfig.getColumnKeys() != null && !sheetConfig.getColumnKeys().isEmpty()) {
            return sheetConfig.getColumnKeys();
        }
        return sheetConfig.getColumnHeaders().stream()
                .map(header -> convertHeaderToKey(header))
                .toList();
    }

    /**
     * Convert header name to likely database column key
     * This is a simple implementation - real implementations might have mapping logic
     */
    private String convertHeaderToKey(String header) {
        return header.toLowerCase()
                .replace(" ", "_")
                .replace("number", "no")
                .replace("current_processing_stage", "last_processing_stage")
                .replace("processing_date", "processing_date_time")
                .replace("processing_time", "processing_date_time");
    }

    /**
     * Generate filename based on pattern and date
     */
    private String generateFileName(String pattern, String reportDate) {
        String dateFormatted = reportDate.replace("-", "");
        return pattern
                .replace("{date}", dateFormatted)
                .replace("{reportType}", "Excel");
    }

    /**
     * Utility method to create sheet data from maps
     */
    public static ExcelReportConfig.SheetData createMapListSheetData(String sheetName, List<Map<String, Object>> data) {
        return new ExcelReportConfig.SheetData(sheetName, data, ExcelReportConfig.SheetDataType.MAP_LIST);
    }

    /**
     * Utility method to create sheet data from key-value pairs
     */
    public static ExcelReportConfig.SheetData createKeyValueSheetData(String sheetName, Map<String, Object> data) {
        return new ExcelReportConfig.SheetData(sheetName, data, ExcelReportConfig.SheetDataType.KEY_VALUE_MAP);
    }

    /**
     * Utility method to create sheet data from list of lists
     */
    public static ExcelReportConfig.SheetData createListSheetData(String sheetName, List<List<Object>> data) {
        return new ExcelReportConfig.SheetData(sheetName, data, ExcelReportConfig.SheetDataType.LIST_OF_LISTS);
    }
}