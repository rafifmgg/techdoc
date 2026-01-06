package com.ocmsintranet.cronservice.framework.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Global Excel Generation Helper
 *
 * A reusable utility for generating Excel reports across different workflows.
 * This helper provides standardized Excel generation capabilities including:
 * - Cell style creation (header, data, date, time, title styles)
 * - Dynamic sheet creation with flexible column configurations
 * - Data type handling (dates, numbers, strings)
 * - Local file saving for debugging
 * - Consistent formatting across all reports
 *
 * Key Features:
 * - Builder pattern for easy configuration
 * - Support for multiple sheets per workbook
 * - Automatic column width adjustment
 * - Standard OCMS styling (Calibri font, consistent formatting)
 * - Type-safe data handling with proper date/time formatting
 *
 * Usage Example:
 * ```java
 * ExcelGenerationHelper helper = new ExcelGenerationHelper();
 * ExcelWorkbookBuilder builder = helper.createWorkbook("Report Title");
 *
 * builder.addSheet("Summary")
 *        .addHeaders(Arrays.asList("Date", "Count", "Status"))
 *        .addDataRows(summaryData)
 *        .setColumnWidths(Arrays.asList(4000, 3000, 3000));
 *
 * byte[] excelData = builder.build();
 * ```
 */
@Slf4j
@Component
public class ExcelGenerationHelper {

    // Standard column widths (in Excel units, 1 unit = 1/256 character width)
    public static final int DEFAULT_COLUMN_WIDTH = 3000;
    public static final int WIDE_COLUMN_WIDTH = 6000;
    public static final int NARROW_COLUMN_WIDTH = 2000;

    // Standard font sizes (in points - multiply by 20 for POI)
    public static final short DEFAULT_FONT_SIZE = 11;
    public static final short TITLE_FONT_SIZE = 14;

    /**
     * Create a new Excel workbook builder
     *
     * @param title Optional title for the workbook
     * @return ExcelWorkbookBuilder instance
     */
    public ExcelWorkbookBuilder createWorkbook(String title) {
        return new ExcelWorkbookBuilder(title);
    }

    /**
     * Create a new Excel workbook builder without title
     *
     * @return ExcelWorkbookBuilder instance
     */
    public ExcelWorkbookBuilder createWorkbook() {
        return new ExcelWorkbookBuilder(null);
    }

    /**
     * Builder class for creating Excel workbooks with multiple sheets
     */
    public static class ExcelWorkbookBuilder {
        private final Workbook workbook;
        private final CellStyleSet styles;

        public ExcelWorkbookBuilder(String title) {
            this.workbook = new XSSFWorkbook();
            this.styles = new CellStyleSet(workbook);
            // Title is handled at sheet level if needed
        }

        /**
         * Add a new sheet to the workbook
         *
         * @param sheetName Name of the sheet
         * @return ExcelSheetBuilder instance
         */
        public ExcelSheetBuilder addSheet(String sheetName) {
            return new ExcelSheetBuilder(this, workbook.createSheet(sheetName), styles);
        }

        /**
         * Build the final Excel file as byte array
         *
         * @return Excel file as byte array
         * @throws RuntimeException if Excel generation fails
         */
        public byte[] build() {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                workbook.close();

                log.info("Excel workbook generated successfully with {} sheets", workbook.getNumberOfSheets());
                return outputStream.toByteArray();

            } catch (IOException e) {
                log.error("Error generating Excel workbook", e);
                throw new RuntimeException("Failed to generate Excel workbook", e);
            }
        }

        /**
         * Build and save Excel file locally for debugging
         *
         * @param filePath Local file path to save
         * @param fileName File name (without extension)
         * @return Excel file as byte array
         */
        public byte[] buildAndSave(String filePath, String fileName) {
            byte[] excelData = build();
            saveLocally(excelData, filePath, fileName);
            return excelData;
        }
    }

    /**
     * Builder class for creating individual Excel sheets
     */
    public static class ExcelSheetBuilder {
        private final ExcelWorkbookBuilder workbookBuilder;
        private final Sheet sheet;
        private final CellStyleSet styles;
        private int currentRow = 0;

        public ExcelSheetBuilder(ExcelWorkbookBuilder workbookBuilder, Sheet sheet, CellStyleSet styles) {
            this.workbookBuilder = workbookBuilder;
            this.sheet = sheet;
            this.styles = styles;
        }

        /**
         * Add a title row to the sheet
         *
         * @param title Title text
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addTitle(String title) {
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(styles.getTitleStyle());

            // Add empty row after title
            currentRow++;
            return this;
        }

        /**
         * Add header row to the sheet
         *
         * @param headers List of header values
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addHeaders(List<String> headers) {
            Row headerRow = sheet.createRow(currentRow++);

            for (int i = 0; i < headers.size(); i++) {
                createCell(headerRow, i, headers.get(i), styles.getHeaderStyle());
            }

            return this;
        }

        /**
         * Add multiple data rows to the sheet
         *
         * @param dataRows List of data rows (each row is a list of values)
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addDataRows(List<List<Object>> dataRows) {
            for (List<Object> rowData : dataRows) {
                addDataRow(rowData);
            }
            return this;
        }

        /**
         * Add multiple data rows from Map data
         *
         * @param mapRows List of data rows as maps
         * @param columnKeys Ordered list of keys to extract from each map
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addDataRowsFromMaps(List<Map<String, Object>> mapRows, List<String> columnKeys) {
            for (Map<String, Object> rowData : mapRows) {
                List<Object> values = columnKeys.stream()
                        .map(rowData::get)
                        .toList();
                addDataRow(values);
            }
            return this;
        }

        /**
         * Add a single data row to the sheet
         *
         * @param rowData List of cell values
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addDataRow(List<Object> rowData) {
            Row row = sheet.createRow(currentRow++);

            for (int i = 0; i < rowData.size(); i++) {
                Object value = rowData.get(i);
                CellStyle style = determineCellStyle(value);
                createCell(row, i, value, style);
            }

            return this;
        }

        /**
         * Add key-value pairs as label-value rows
         *
         * @param keyValuePairs Map of label-value pairs
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addKeyValueRows(Map<String, Object> keyValuePairs) {
            for (Map.Entry<String, Object> entry : keyValuePairs.entrySet()) {
                Row row = sheet.createRow(currentRow++);
                createCell(row, 0, entry.getKey(), styles.getHeaderStyle());
                createCell(row, 1, entry.getValue(), determineCellStyle(entry.getValue()));
            }
            return this;
        }

        /**
         * Add an empty row
         *
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder addEmptyRow() {
            currentRow++;
            return this;
        }

        /**
         * Set column widths for the sheet
         *
         * @param widths List of column widths (in Excel units)
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder setColumnWidths(List<Integer> widths) {
            for (int i = 0; i < widths.size(); i++) {
                sheet.setColumnWidth(i, widths.get(i));
            }
            return this;
        }

        /**
         * Set all columns to the same width
         *
         * @param columnCount Number of columns
         * @param width Width for all columns
         * @return ExcelSheetBuilder for chaining
         */
        public ExcelSheetBuilder setUniformColumnWidth(int columnCount, int width) {
            for (int i = 0; i < columnCount; i++) {
                sheet.setColumnWidth(i, width);
            }
            return this;
        }

        /**
         * Finish building this sheet and return to workbook builder
         *
         * @return ExcelWorkbookBuilder for adding more sheets
         */
        public ExcelWorkbookBuilder finishSheet() {
            return workbookBuilder;
        }

        /**
         * Determine appropriate cell style based on value type
         */
        private CellStyle determineCellStyle(Object value) {
            if (value instanceof LocalDate || (value instanceof String && ((String) value).matches("\\d{4}-\\d{2}-\\d{2}"))) {
                return styles.getDateStyle();
            } else if (value instanceof LocalDateTime || (value instanceof String && ((String) value).matches("\\d{2}:\\d{2}:\\d{2}"))) {
                return styles.getTimeStyle();
            } else {
                return styles.getDataStyle();
            }
        }
    }

    /**
     * Container for all cell styles used in Excel generation
     */
    private static class CellStyleSet {
        private final CellStyle headerStyle;
        private final CellStyle dataStyle;
        private final CellStyle dateStyle;
        private final CellStyle timeStyle;
        private final CellStyle titleStyle;

        public CellStyleSet(Workbook workbook) {
            this.headerStyle = createHeaderStyle(workbook);
            this.dataStyle = createDataStyle(workbook);
            this.dateStyle = createDateStyle(workbook);
            this.timeStyle = createTimeStyle(workbook);
            this.titleStyle = createTitleStyle(workbook);
        }

        public CellStyle getHeaderStyle() { return headerStyle; }
        public CellStyle getDataStyle() { return dataStyle; }
        public CellStyle getDateStyle() { return dateStyle; }
        public CellStyle getTimeStyle() { return timeStyle; }
        public CellStyle getTitleStyle() { return titleStyle; }
    }

    /**
     * Create a cell with value and style
     */
    private static void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof java.util.Date) {
                cell.setCellValue((java.util.Date) value);
            } else if (value instanceof LocalDate) {
                LocalDate localDate = (LocalDate) value;
                java.util.Date date = java.sql.Date.valueOf(localDate);
                cell.setCellValue(date);
            } else if (value instanceof LocalDateTime) {
                LocalDateTime localDateTime = (LocalDateTime) value;
                java.util.Date date = java.sql.Timestamp.valueOf(localDateTime);
                cell.setCellValue(date);
            } else {
                String strValue = value.toString();
                // Try to parse date strings in format yyyy-MM-dd
                if (strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    try {
                        LocalDate localDate = LocalDate.parse(strValue);
                        java.util.Date date = java.sql.Date.valueOf(localDate);
                        cell.setCellValue(date);
                    } catch (Exception e) {
                        cell.setCellValue(strValue);
                    }
                } else {
                    cell.setCellValue(strValue);
                }
            }
        } else {
            cell.setCellValue("");
        }

        cell.setCellStyle(style);
    }

    /**
     * Create header style for Excel cells
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(DEFAULT_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * Create data style for Excel cells
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(DEFAULT_FONT_SIZE);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * Create date style for Excel cells (mm/dd/yyyy format)
     */
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(DEFAULT_FONT_SIZE);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        CreationHelper createHelper = workbook.getCreationHelper();
        short dateFormat = createHelper.createDataFormat().getFormat("mm/dd/yyyy");
        style.setDataFormat(dateFormat);

        return style;
    }

    /**
     * Create time style for Excel cells (h:mm:ss AM/PM format)
     */
    private static CellStyle createTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(DEFAULT_FONT_SIZE);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        CreationHelper createHelper = workbook.getCreationHelper();
        short timeFormat = createHelper.createDataFormat().getFormat("h:mm:ss AM/PM");
        style.setDataFormat(timeFormat);

        return style;
    }

    /**
     * Create title style for Excel cells
     */
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * Save Excel file locally for debugging
     *
     * @param excelData Excel file as byte array
     * @param filePath Directory path
     * @param fileName File name (without extension)
     */
    private static void saveLocally(byte[] excelData, String filePath, String fileName) {
        try {
            File directory = new File(filePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fullFileName = fileName.endsWith(".xlsx") ? fileName : fileName + ".xlsx";
            File file = new File(directory, fullFileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(excelData);
            }

            log.info("Excel file saved locally at: {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error saving Excel file locally: {}", e.getMessage(), e);
        }
    }
}