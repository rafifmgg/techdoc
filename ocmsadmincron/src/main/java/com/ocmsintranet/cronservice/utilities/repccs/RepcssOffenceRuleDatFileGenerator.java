package com.ocmsintranet.cronservice.utilities.repccs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generator for Offence Rule .dat files with specific format requirements
 *
 * File Format:
 * - Header Record: H + YYYYMMDD + HHMM + 337 spaces (total 350 chars)
 * - Detail Records: D + offence data fields (total 350 chars each)
 * - Trailer Record: T + record count + 342 spaces (total 350 chars)
 */
@Component
public class RepcssOffenceRuleDatFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(RepcssOffenceRuleDatFileGenerator.class);

    // File format constants
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    private static final int OFFENCE_CODE_LENGTH = 8;
    private static final int OFFENCE_DESC_LENGTH = 255;
    private static final int EFFECTIVE_DATE_LENGTH = 17;
    private static final int EXPIRY_DATE_LENGTH = 17;
    private static final int HEADER_FILLER_LENGTH = 337;
    private static final int DETAIL_FILLER_LENGTH = 28;
    private static final int TRAILER_FILLER_LENGTH = 342;

    // Date/time formatters
    private static final DateTimeFormatter HEADER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HEADER_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DATETIME_WITH_MILLIS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * Data class for offence rule information
     */
    public static class OffenceRuleData {
        private final Integer offenceCode;
        private final String offenceRule;
        private final String offenceDescription;
        private final LocalDateTime effectiveStartDate;
        private final LocalDateTime effectiveEndDate;
        private final BigDecimal defaultFineAmount;
        private final BigDecimal secondaryFineAmount;
        private final String vehicleType;

        public OffenceRuleData(Integer offenceCode, String offenceRule, String offenceDescription,
                               LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate,
                               BigDecimal defaultFineAmount, BigDecimal secondaryFineAmount, String vehicleType) {
            this.offenceCode = offenceCode;
            this.offenceRule = offenceRule;
            this.offenceDescription = offenceDescription;
            this.effectiveStartDate = effectiveStartDate;
            this.effectiveEndDate = effectiveEndDate;
            this.defaultFineAmount = defaultFineAmount;
            this.secondaryFineAmount = secondaryFineAmount;
            this.vehicleType = vehicleType;
        }

        // Getters
        public Integer getOffenceCode() { return offenceCode; }
        public String getOffenceRule() { return offenceRule; }
        public String getOffenceDescription() { return offenceDescription; }
        public LocalDateTime getEffectiveStartDate() { return effectiveStartDate; }
        public LocalDateTime getEffectiveEndDate() { return effectiveEndDate; }
        public BigDecimal getDefaultFineAmount() { return defaultFineAmount; }
        public BigDecimal getSecondaryFineAmount() { return secondaryFineAmount; }
        public String getVehicleType() { return vehicleType; }
    }

    /**
     * Generate offence rule .dat file content
     *
     * @param offenceRuleList List of offence rule data
     * @return String containing the complete .dat file content
     */
    public String generateDatFileContent(List<OffenceRuleData> offenceRuleList) {
        logger.info("Generating offence rule .dat file content for {} records", offenceRuleList.size());

        if (offenceRuleList == null || offenceRuleList.isEmpty()) {
            throw new IllegalArgumentException("Offence rule data list cannot be null or empty");
        }

        StringBuilder content = new StringBuilder();

        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");

        // 2. Generate Detail Records
        for (OffenceRuleData rule : offenceRuleList) {
            content.append(generateDetailRecord(rule)).append("\n");
        }

        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(offenceRuleList.size()));

        logger.info("Successfully generated offence rule .dat file content with {} records", offenceRuleList.size());
        return content.toString();
    }

    /**
     * Generate header record
     * Format: H + YYYYMMDD + HHMM + 337 spaces
     */
    private String generateHeaderRecord() {
        LocalDateTime now = LocalDateTime.now();

        StringBuilder header = new StringBuilder();
        header.append(HEADER_RECORD_TYPE);
        header.append(now.format(HEADER_DATE_FORMAT));
        header.append(now.format(HEADER_TIME_FORMAT));
        header.append(" ".repeat(HEADER_FILLER_LENGTH));

        return header.toString();
    }

    /**
     * Generate detail record for offence rule
     * Format: D + offence_code + offence_rule + description + start_date + end_date + default_fine + secondary_fine + vehicle_type + filler
     */
    private String generateDetailRecord(OffenceRuleData rule) {
        StringBuilder detail = new StringBuilder();
        detail.append(DETAIL_RECORD_TYPE);

        // Offence Code (8 chars)
        String offenceCode = rule.getOffenceCode() != null ?
                rule.getOffenceCode().toString() : "";
        if (offenceCode.length() > OFFENCE_CODE_LENGTH) {
            offenceCode = offenceCode.substring(0, OFFENCE_CODE_LENGTH);
        } else {
            offenceCode = String.format("%-8s", offenceCode);
        }
        detail.append(offenceCode);

        // Offence Rule (8 chars)
        String offenceRule = rule.getOffenceRule();
        if (offenceRule != null) {
            if (offenceRule.length() > OFFENCE_CODE_LENGTH) {
                offenceRule = offenceRule.substring(0, OFFENCE_CODE_LENGTH);
            } else {
                offenceRule = String.format("%-8s", offenceRule);
            }
        } else {
            offenceRule = String.format("%-8s", "");
        }
        detail.append(offenceRule);

        // Offence Description (255 chars)
        String offenceDesc = rule.getOffenceDescription() != null ?
                rule.getOffenceDescription() : "";
        if (offenceDesc.length() > OFFENCE_DESC_LENGTH) {
            offenceDesc = offenceDesc.substring(0, OFFENCE_DESC_LENGTH);
        } else {
            offenceDesc = String.format("%-255s", offenceDesc);
        }
        detail.append(offenceDesc);

        // Effective Date (17 chars)
        String effectiveDate = rule.getEffectiveStartDate() != null ?
                rule.getEffectiveStartDate().format(DATETIME_WITH_MILLIS_FORMAT) : "99991231235959999";
        if (effectiveDate.length() > EFFECTIVE_DATE_LENGTH) {
            effectiveDate = effectiveDate.substring(0, EFFECTIVE_DATE_LENGTH);
        }
        detail.append(effectiveDate);

        // Expiry Date (17 chars)
        String expiryDate = rule.getEffectiveEndDate() != null ?
                rule.getEffectiveEndDate().format(DATETIME_WITH_MILLIS_FORMAT) : "99991231235959999";
        if (expiryDate.length() > EXPIRY_DATE_LENGTH) {
            expiryDate = expiryDate.substring(0, EXPIRY_DATE_LENGTH);
        }
        detail.append(expiryDate);

        // Default Fine Amount (7 chars)
        BigDecimal defaultFine = rule.getDefaultFineAmount() != null ?
                rule.getDefaultFineAmount() : BigDecimal.ZERO;
        String defaultFineStr = String.format("%07.2f", defaultFine);
        detail.append(defaultFineStr);

        // Secondary Fine Amount (7 chars)
        BigDecimal secondaryFine = rule.getSecondaryFineAmount() != null ?
                rule.getSecondaryFineAmount() : BigDecimal.ZERO;
        if (secondaryFine == null || secondaryFine.compareTo(BigDecimal.ONE) < 0) {
            detail.append(String.format("%7s", "")); // 7 spasi kosong
        } else {
            String secFineStr = String.format("%07.2f", secondaryFine);
            detail.append(secFineStr);
        }

        // Vehicle Type (2 chars)
        String vehicleType = rule.getVehicleType() != null ?
                rule.getVehicleType() : "";
        if (vehicleType.length() > 2) {
            vehicleType = vehicleType.substring(0, 2);
        } else {
            vehicleType = String.format("%-2s", vehicleType);
        }
        detail.append(vehicleType);

        // Filler
        detail.append(" ".repeat(DETAIL_FILLER_LENGTH));

        return detail.toString();
    }

    /**
     * Generate trailer record
     * Format: T + total_count + 342 spaces
     */
    private String generateTrailerRecord(int totalRecords) {
        StringBuilder trailer = new StringBuilder();
        trailer.append(TRAILER_RECORD_TYPE);
        trailer.append(String.format("%07d", totalRecords));
        trailer.append(" ".repeat(TRAILER_FILLER_LENGTH));

        return trailer.toString();
    }

    /**
     * Generate file name for offence rule .dat file
     */
    public String generateFileName() {
        String timestamp = LocalDateTime.now().format(DATETIME_WITH_MILLIS_FORMAT);
        return "REPCCS_OFFENCECODE_" + timestamp + ".DAT";
    }
}
