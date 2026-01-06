package com.ocmsintranet.cronservice.utilities.repccs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generator for CASREP format .dat files with specific format requirements
 * Used for Listed Vehicle CAS to REPCCS processing
 *
 * File Format:
 * - Header Record: H + YYYYMMDD + HHMM + 137 spaces (total 150 chars)
 * - Detail Records: D + licence plate + reason description + filler (total 150 chars each)
 * - Trailer Record: T + record count + 142 spaces (total 150 chars)
 *
 * Note: OBU Label field is omitted as it's not mandatory for CASREP format
 */
@Component
public class RepcssListedDatFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(RepcssListedDatFileGenerator.class);

    // File format constants
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    private static final int LICENCE_PLATE_LENGTH = 14;
    private static final int OBU_LABEL_LENGTH = 10;
    private static final int REASON_DESCRIPTION_LENGTH = 100;
    private static final int HEADER_FILLER_LENGTH = 137;
    private static final int DETAIL_FILLER_LENGTH = 27;
    private static final int TRAILER_FILLER_LENGTH = 142;

    // Date/time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Data class for CASREP vehicle information (without OBU Label)
     */
    public static class ListedVehicleData {
        private final String licencePlate;
        private final String reasonDescription;

        public ListedVehicleData(String licencePlate, String reasonDescription) {
            this.licencePlate = licencePlate;
            this.reasonDescription = reasonDescription;
        }

        // Getters
        public String getLicencePlate() { return licencePlate; }
        public String getReasonDescription() { return reasonDescription; }
    }

    /**
     * Generate CASREP format .dat file content
     *
     * @param vehicleDataList List of vehicle data containing licence plate and reason description
     * @return String containing the CASREP format .dat file content
     */
    public String generateDatFileContent(List<ListedVehicleData> vehicleDataList) {
        logger.info("Generating CASREP .dat file content for {} vehicle records", vehicleDataList.size());

        if (vehicleDataList == null || vehicleDataList.isEmpty()) {
            throw new IllegalArgumentException("Vehicle data list cannot be null or empty");
        }

        StringBuilder content = new StringBuilder();

        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");

        // 2. Generate Detail Records
        for (ListedVehicleData vehicleData : vehicleDataList) {
            content.append(generateDetailRecord(vehicleData)).append("\n");
        }

        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(vehicleDataList.size()));

        logger.info("Successfully generated CASREP .dat file with {} detail records", vehicleDataList.size());
        return content.toString();
    }

    /**
     * Generate header record
     * Format: H + YYYYMMDD + HHMM + 137 spaces
     */
    private String generateHeaderRecord() {
        LocalDateTime now = LocalDateTime.now();
        String creationDate = now.format(DATE_FORMATTER);
        String creationTime = now.format(TIME_FORMATTER);

        StringBuilder header = new StringBuilder();
        header.append(HEADER_RECORD_TYPE);
        header.append(creationDate);
        header.append(creationTime);
        header.append(" ".repeat(HEADER_FILLER_LENGTH));

        return header.toString();
    }

    /**
     * Generate detail record for vehicle data
     * Format: D + licence_plate + obu_label + reason_description + filler
     */
    private String generateDetailRecord(ListedVehicleData vehicleData) {
        StringBuilder detail = new StringBuilder();
        detail.append(DETAIL_RECORD_TYPE);

        // Vehicle number (licence plate) (14 chars, left-aligned)
        String licencePlate = vehicleData.getLicencePlate() != null ?
                vehicleData.getLicencePlate() : "";
        if (licencePlate.length() > LICENCE_PLATE_LENGTH) {
            licencePlate = licencePlate.substring(0, LICENCE_PLATE_LENGTH);
        } else {
            licencePlate = String.format("%-" + LICENCE_PLATE_LENGTH + "s", licencePlate);
        }
        detail.append(licencePlate);

        //add obu label (empty field but required for format)
        String obuLabel = "";
        obuLabel = truncateAndPad(obuLabel, OBU_LABEL_LENGTH); // 10-character field
        detail.append(obuLabel);

        // Reason Description (100 chars, left-aligned)
        String reasonDesc = vehicleData.getReasonDescription() != null ?
                vehicleData.getReasonDescription() : "";
        if (reasonDesc.length() > REASON_DESCRIPTION_LENGTH) {
            reasonDesc = reasonDesc.substring(0, REASON_DESCRIPTION_LENGTH);
        } else {
            reasonDesc = String.format("%-" + REASON_DESCRIPTION_LENGTH + "s", reasonDesc);
        }
        detail.append(reasonDesc);

        // Filler
        detail.append(" ".repeat(DETAIL_FILLER_LENGTH));

        return detail.toString();
    }

    /**
     * Generate trailer record
     * Format: T + total_count + 142 spaces
     */
    private String generateTrailerRecord(int totalRecords) {
        StringBuilder trailer = new StringBuilder();
        trailer.append(TRAILER_RECORD_TYPE);
        trailer.append(String.format("%07d", totalRecords));
        trailer.append(" ".repeat(TRAILER_FILLER_LENGTH));

        return trailer.toString();
    }

    /**
     * Generate file name for CASREP .dat file
     * Format: CES_WANTED_VEHICLES_YYYYMMDDHHMMSS.DAT
     */
    public String generateFileName() {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        return "REPCCS_LISTEDVEH_" + timestamp + ".DAT";
    }

    /**
     * Helper method to truncate or pad a string to specified length
     */
    private String truncateAndPad(String input, int length) {
        if (input == null) {
            input = "";
        }
        if (input.length() > length) {
            return input.substring(0, length);
        }
        return String.format("%-" + length + "s", input);
    }
}
