package com.ocmsintranet.cronservice.utilities.repccs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generator for NOPOARC format .dat files with specific format requirements
 * Used for NOPO archival processing
 * 
 * File Format:
 * - Header Record: H + YYYYMMDD + HHMM + 37 spaces (total 50 chars)
 * - Detail Records: D + NOPO number + settlement status + filler (total 50 chars each)
 * - Trailer Record: T + record count + 42 spaces (total 50 chars)
 */
@Component
public class RepcssNopoarcDatFileGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(RepcssNopoarcDatFileGenerator.class);
    
    // File format constants
    private static final int TOTAL_LINE_LENGTH = 50;
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    
    // Field lengths for NOPOARC format
    private static final int NOPO_NUMBER_LENGTH = 10;
    private static final int SETTLEMENT_STATUS_LENGTH = 1;
    private static final int DETAIL_FILLER_LENGTH = 38;
    private static final int TRAILER_COUNT_LENGTH = 7;
    
    // Date/time formatters
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    
    /**
     * Data class for NOPOARC information
     */
    public static class NopoarcData {
        private final String nopoNumber;
        private final String settlementStatus;
        
        public NopoarcData(String nopoNumber, String settlementStatus) {
            this.nopoNumber = nopoNumber;
            this.settlementStatus = settlementStatus;
        }
        
        // Getters
        public String getNopoNumber() { return nopoNumber; }
        public String getSettlementStatus() { return settlementStatus; }
    }
    
    /**
     * Generate NOPOARC format .dat file content
     * 
     * @param nopoarcDataList List of NOPO data containing NOPO number and settlement status
     * @return byte array containing the NOPOARC format .dat file content
     */
    public byte[] generateNopoarcDatFileContent(List<NopoarcData> nopoarcDataList) {
        logger.info("Generating NOPOARC .dat file content for {} NOPO records", nopoarcDataList.size());
        
        StringBuilder content = new StringBuilder();
        
        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");
        
        // 2. Generate Detail Records
        for (NopoarcData nopoarcData : nopoarcDataList) {
            content.append(generateDetailRecord(nopoarcData)).append("\n");
        }
        
        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(nopoarcDataList.size()));
        
        logger.info("Generated NOPOARC .dat file with {} detail records", nopoarcDataList.size());
        return content.toString().getBytes();
    }
    
    /**
     * Generate header record
     * Format: H + YYYYMMDD + HHMM + 37 spaces
     */
    private String generateHeaderRecord() {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FORMATTER);
        String time = now.format(TIME_FORMATTER);
        
        StringBuilder header = new StringBuilder();
        header.append(HEADER_RECORD_TYPE);  // 1 char
        header.append(date);                // 8 chars
        header.append(time);                // 4 chars
        
        // Add filler spaces (37 spaces)
        int fillerLength = TOTAL_LINE_LENGTH - header.length();
        header.append(" ".repeat(fillerLength));
        
        return header.toString();
    }
    
    /**
     * Generate detail record for NOPO data
     * Format: D + NOPO_number + settlement_status + filler
     */
    private String generateDetailRecord(NopoarcData nopoarcData) {
        StringBuilder detail = new StringBuilder();
        
        // Record Type (1 char)
        detail.append(DETAIL_RECORD_TYPE);
        
        // NOPO Number (10 chars, left-aligned with trailing spaces)
        detail.append(padRight(nopoarcData.getNopoNumber(), NOPO_NUMBER_LENGTH));
        
        // Settlement Status (1 char)
        //String status = mapSettlementStatus(nopoarcData.getSettlementStatus());
        String status = "S";
        detail.append(padRight(status, SETTLEMENT_STATUS_LENGTH));
        
        // Filler (38 spaces)
        detail.append(" ".repeat(DETAIL_FILLER_LENGTH));
        
        return detail.toString();
    }
    
    /**
     * Generate trailer record
     * Format: T + total_count + 42 spaces
     */
    private String generateTrailerRecord(int totalRecords) {
        StringBuilder trailer = new StringBuilder();
        trailer.append(TRAILER_RECORD_TYPE);  // 1 char
        
        // Total Detail Record count (7 digits, zero-padded, right-aligned)
        trailer.append(String.format("%07d", totalRecords));
        
        // Add filler spaces (42 spaces)
        int fillerLength = TOTAL_LINE_LENGTH - trailer.length();
        trailer.append(" ".repeat(fillerLength));
        
        return trailer.toString();
    }
    
    /**
     * Map settlement status to appropriate code
     * S = Settled (Terselesaikan)
     * P = Pending (Tertunda)
     */
    private String mapSettlementStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "P"; // Default to Pending
        }

        // Map payment status to settlement status
        return switch (paymentStatus.toUpperCase()) {
            case "PAID", "SETTLED", "COMPLETED" -> "S";
            case "PENDING", "UNPAID", "OUTSTANDING" -> "P";
            default -> "P"; // Default to Pending
        };
    }
    
    /**
     * Pad string to right with spaces (left-aligned)
     */
    private String padRight(String str, int length) {
        if (str == null) {
            str = "";
        }
        
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        
        return str + " ".repeat(length - str.length());
    }
    
    /**
     * Generate file name for NOPOARC .dat file
     * Format: REPCCS-CAS-IN-NOPOARC-YYYYMMDDHHMMSS.dat
     */
    public String generateNopoarcFileName() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(TIMESTAMP_FORMATTER);
        String fileName = String.format("REPCCS_NOPOARCH_%s.DAT", timestamp);
        logger.debug("Generated NOPOARC filename: {}", fileName);
        return fileName;
    }
}
