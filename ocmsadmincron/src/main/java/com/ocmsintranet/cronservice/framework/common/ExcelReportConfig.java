package com.ocmsintranet.cronservice.framework.common;

import java.util.List;
import java.util.Map;

/**
 * Configuration interface for Excel report generation
 *
 * This interface defines the structure for configuring Excel reports in a standardized way.
 * Implementing classes can define their specific report configurations, making it easy
 * to generate consistent Excel reports across different workflows.
 *
 * Usage Example:
 * ```java
 * public class MhaReportConfig implements ExcelReportConfig {
 *     @Override
 *     public String getReportTitle() {
 *         return "OCMS SUMMARY REPORT - MHA Particulars Check Result";
 *     }
 *
 *     @Override
 *     public List<SheetConfig> getSheetConfigurations() {
 *         return Arrays.asList(
 *             new SheetConfig("Summary", getSummaryColumns(), getSummaryColumnWidths()),
 *             new SheetConfig("Success", getSuccessColumns(), getSuccessColumnWidths()),
 *             new SheetConfig("Error", getErrorColumns(), getErrorColumnWidths())
 *         );
 *     }
 * }
 * ```
 */
public interface ExcelReportConfig {

    /**
     * Get the main title for the Excel report
     *
     * @return Report title
     */
    String getReportTitle();

    /**
     * Get the configuration for all sheets in the workbook
     *
     * @return List of sheet configurations
     */
    List<SheetConfig> getSheetConfigurations();

    /**
     * Get the file name pattern for the Excel report
     * Should include placeholder for date if needed
     *
     * @return File name pattern (e.g., "MHA_Report_{date}.xlsx")
     */
    default String getFileNamePattern() {
        return "{reportType}_Report_{date}.xlsx";
    }

    /**
     * Get the local save directory for debugging
     *
     * @return Local directory path
     */
    default String getLocalSaveDirectory() {
        return System.getProperty("user.home") + "/OCMS_Reports/";
    }

    /**
     * Configuration for individual Excel sheets
     */
    class SheetConfig {
        private final String sheetName;
        private final List<String> columnHeaders;
        private final List<Integer> columnWidths;
        private final List<String> columnKeys;  // Data field keys for MAP_LIST sheets
        private final Map<String, Object> additionalConfig;

        public SheetConfig(String sheetName, List<String> columnHeaders, List<Integer> columnWidths) {
            this.sheetName = sheetName;
            this.columnHeaders = columnHeaders;
            this.columnWidths = columnWidths;
            this.columnKeys = List.of(); // Empty list means auto-generate from headers
            this.additionalConfig = Map.of();
        }

        public SheetConfig(String sheetName, List<String> columnHeaders, List<Integer> columnWidths,
                          Map<String, Object> additionalConfig) {
            this.sheetName = sheetName;
            this.columnHeaders = columnHeaders;
            this.columnWidths = columnWidths;
            this.columnKeys = List.of(); // Empty list means auto-generate from headers
            this.additionalConfig = additionalConfig != null ? additionalConfig : Map.of();
        }

        public SheetConfig(String sheetName, List<String> columnHeaders, List<Integer> columnWidths,
                          List<String> columnKeys, Map<String, Object> additionalConfig) {
            this.sheetName = sheetName;
            this.columnHeaders = columnHeaders;
            this.columnWidths = columnWidths;
            this.columnKeys = columnKeys != null ? columnKeys : List.of();
            this.additionalConfig = additionalConfig != null ? additionalConfig : Map.of();
        }

        public String getSheetName() { return sheetName; }
        public List<String> getColumnHeaders() { return columnHeaders; }
        public List<Integer> getColumnWidths() { return columnWidths; }
        public List<String> getColumnKeys() { return columnKeys; }
        public Map<String, Object> getAdditionalConfig() { return additionalConfig; }

        /**
         * Check if this sheet should include a title row
         */
        public boolean includeTitle() {
            return (Boolean) additionalConfig.getOrDefault("includeTitle", false);
        }

        /**
         * Get the title for this sheet (if includeTitle is true)
         */
        public String getSheetTitle() {
            return (String) additionalConfig.getOrDefault("sheetTitle", "");
        }

        /**
         * Check if this sheet should use key-value format instead of tabular
         */
        public boolean isKeyValueFormat() {
            return (Boolean) additionalConfig.getOrDefault("keyValueFormat", false);
        }
    }

    /**
     * Data container for sheet content
     */
    class SheetData {
        private final String sheetName;
        private final Object data;
        private final SheetDataType dataType;

        public SheetData(String sheetName, Object data, SheetDataType dataType) {
            this.sheetName = sheetName;
            this.data = data;
            this.dataType = dataType;
        }

        public String getSheetName() { return sheetName; }
        public Object getData() { return data; }
        public SheetDataType getDataType() { return dataType; }

        /**
         * Get data as list of maps (for tabular data)
         */
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> getAsMapList() {
            if (dataType == SheetDataType.MAP_LIST) {
                return (List<Map<String, Object>>) data;
            }
            throw new IllegalStateException("Data is not in MAP_LIST format");
        }

        /**
         * Get data as key-value map (for summary data)
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getAsKeyValueMap() {
            if (dataType == SheetDataType.KEY_VALUE_MAP) {
                return (Map<String, Object>) data;
            }
            throw new IllegalStateException("Data is not in KEY_VALUE_MAP format");
        }

        /**
         * Get data as list of lists (for custom tabular data)
         */
        @SuppressWarnings("unchecked")
        public List<List<Object>> getAsListOfLists() {
            if (dataType == SheetDataType.LIST_OF_LISTS) {
                return (List<List<Object>>) data;
            }
            throw new IllegalStateException("Data is not in LIST_OF_LISTS format");
        }
    }

    /**
     * Enum for different types of sheet data
     */
    enum SheetDataType {
        MAP_LIST,        // List<Map<String, Object>> - for database query results
        KEY_VALUE_MAP,   // Map<String, Object> - for summary/statistics data
        LIST_OF_LISTS    // List<List<Object>> - for custom tabular data
    }
}