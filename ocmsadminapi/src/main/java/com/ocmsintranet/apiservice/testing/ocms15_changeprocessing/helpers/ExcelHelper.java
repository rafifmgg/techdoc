package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Excel Helper - Parses Excel reports for verification
 *
 * Responsibilities:
 * - Parse Excel file from byte array
 * - Extract rows as Map<String, Object>
 * - Verify column structure (16 columns for change processing report)
 */
@Slf4j
@Component
public class ExcelHelper {

    /**
     * Parse Excel file and extract all rows
     *
     * @param fileData Excel file as byte array
     * @return List of rows, each row is a Map of column name → value
     */
    public Map<String, Object> parseExcelFile(byte[] fileData) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);  // First sheet
            Iterator<Row> rowIterator = sheet.iterator();

            // Read header row
            Row headerRow = rowIterator.hasNext() ? rowIterator.next() : null;
            if (headerRow == null) {
                result.put("success", false);
                result.put("error", "Excel file is empty");
                return result;
            }

            List<String> headers = extractHeaders(headerRow);

            // Read data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, Object> rowData = extractRowData(row, headers);
                if (!rowData.isEmpty()) {
                    rows.add(rowData);
                }
            }

            result.put("success", true);
            result.put("headers", headers);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("columnCount", headers.size());

            log.info("Parsed Excel file: {} rows, {} columns", rows.size(), headers.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error parsing Excel file: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Extract headers from header row
     */
    private List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        Iterator<Cell> cellIterator = headerRow.cellIterator();

        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String header = getCellValueAsString(cell);
            headers.add(header);
        }

        return headers;
    }

    /**
     * Extract data from a row
     */
    private Map<String, Object> extractRowData(Row row, List<String> headers) {
        Map<String, Object> rowData = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            String header = headers.get(i);
            Object value = getCellValue(cell);
            rowData.put(header, value);
        }

        return rowData;
    }

    /**
     * Get cell value as appropriate Java type
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                } else {
                    return cell.getNumericCellValue();
                }

            case BOOLEAN:
                return cell.getBooleanCellValue();

            case FORMULA:
                // Evaluate formula
                return cell.getNumericCellValue();

            case BLANK:
                return null;

            default:
                return cell.toString();
        }
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Format number to avoid scientific notation
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return String.valueOf(cell.getNumericCellValue());

            case BLANK:
                return "";

            default:
                return cell.toString();
        }
    }

    /**
     * Verify Excel report has expected column structure
     *
     * @param headers List of header names from Excel
     * @param expectedColumns List of expected column names
     * @return Verification result
     */
    public Map<String, Object> verifyColumnStructure(List<String> headers, List<String> expectedColumns) {
        Map<String, Object> result = new HashMap<>();
        List<String> missingColumns = new ArrayList<>();
        List<String> extraColumns = new ArrayList<>();

        // Check for missing columns
        for (String expected : expectedColumns) {
            if (!headers.contains(expected)) {
                missingColumns.add(expected);
            }
        }

        // Check for extra columns
        for (String actual : headers) {
            if (!expectedColumns.contains(actual)) {
                extraColumns.add(actual);
            }
        }

        boolean success = missingColumns.isEmpty() && extraColumns.isEmpty();

        result.put("success", success);
        result.put("expectedCount", expectedColumns.size());
        result.put("actualCount", headers.size());
        result.put("missingColumns", missingColumns);
        result.put("extraColumns", extraColumns);

        if (success) {
            result.put("message", "Column structure matches (16 columns)");
        } else {
            result.put("message", "Column structure mismatch: " +
                missingColumns.size() + " missing, " +
                extraColumns.size() + " extra");
        }

        return result;
    }

    /**
     * Get expected columns for Change Processing Stage report (16 columns per spec §4.5.3)
     */
    public static List<String> getExpectedChangeProcessingColumns() {
        return Arrays.asList(
            "Notice No",
            "Date of Change",
            "Previous Processing Stage",
            "Last Processing Stage",
            "Next Processing Stage",
            "Amount Payable",
            "Source",
            "Remarks",
            "Vehicle No",
            "Offender ID",
            "PP Code",
            "PP Name",
            "Notice Date Time",
            "Updated Date",
            "Updated User ID",
            "Created Date"
        );
    }
}
