package com.ocmsintranet.cronservice.utilities.ces;

import com.ocmsintranet.cronservice.framework.dto.ces.CesOffenceRuleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CesOffenceRuleDatGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CesOffenceRuleDatGenerator.class);

    // File format constants
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    private static final int OFFENCE_CODE_LENGTH = 8;
    private static final int OFFENCE_DESC_LENGTH = 255;
    private static final int MIN_FINE_AMOUNT_LENGTH = 7;
    private static final int MAX_FINE_AMOUNT_LENGTH = 7;
    private static final int EFFECTIVE_DATE_LENGTH = 17;
    private static final int EXPIRY_DATE_LENGTH = 17;
    private static final int HEADER_FILLER_LENGTH = 337;
    private static final int DETAIL_FILLER_LENGTH = 28;
    private static final int TRAILER_FILLER_LENGTH = 342;

    // Date/time formatters
    private static final DateTimeFormatter HEADER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HEADER_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DATETIME_WITH_MILLIS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generateDatFileContent(List<CesOffenceRuleData> offenceRules) {
        logger.info("Generating CES Offence Rule .dat file content for {} records", offenceRules.size());

        if (offenceRules == null || offenceRules.isEmpty()) {
            throw new IllegalArgumentException("Offence rule data list cannot be null or empty");
        }

        StringBuilder content = new StringBuilder();

        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");

        // 2. Generate Detail Records
        for (CesOffenceRuleData rule : offenceRules) {
            content.append(generateDetailRecord(rule)).append("\n");
        }

        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(offenceRules.size()));

        logger.info("Successfully generated CES Offence Rule .dat file content with {} records", offenceRules.size());
        return content.toString();
    }

    private String generateHeaderRecord() {
        LocalDateTime now = LocalDateTime.now();

        StringBuilder header = new StringBuilder();
        header.append(HEADER_RECORD_TYPE);
        header.append(now.format(HEADER_DATE_FORMAT));
        header.append(now.format(HEADER_TIME_FORMAT));
        header.append(" ".repeat(HEADER_FILLER_LENGTH));

        return header.toString();
    }

    private String generateDetailRecord(CesOffenceRuleData rule) {
        StringBuilder detail = new StringBuilder();
        detail.append(DETAIL_RECORD_TYPE);

        // Offence Code (8 chars)
        String offenceCode = rule.getOffenceCode() != null ?
                rule.getOffenceCode() : "";
        if (offenceCode.length() > OFFENCE_CODE_LENGTH) {
            offenceCode = offenceCode.substring(0, OFFENCE_CODE_LENGTH);
        } else {
            offenceCode = String.format("%-8s", offenceCode);
        }
        detail.append(offenceCode);

        // Offence Rule (8 chars) - konversi dari int ke String
        String offenceRule = rule.getOffenceRule();
        //String offenceRule = String.valueOf(offenceRuleValue);
        if (offenceRule.length() > OFFENCE_CODE_LENGTH) {
            offenceRule = offenceRule.substring(0, OFFENCE_CODE_LENGTH);
        } else {
            offenceRule = String.format("%-8s", offenceRule);
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
            detail.append(String.format("%7s", "")); // 7 spasi kosong, agar panjang tetap 7
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

    private String generateTrailerRecord(int totalRecords) {
        StringBuilder trailer = new StringBuilder();
        trailer.append(TRAILER_RECORD_TYPE);
        trailer.append(String.format("%07d", totalRecords));
        trailer.append(" ".repeat(TRAILER_FILLER_LENGTH));

        return trailer.toString();
    }

    public String generateFileName() {
        String timestamp = LocalDateTime.now().format(DATETIME_WITH_MILLIS_FORMAT);
        return "CES_OFFENCE_RULE_" + timestamp + ".DAT";
    }
}
