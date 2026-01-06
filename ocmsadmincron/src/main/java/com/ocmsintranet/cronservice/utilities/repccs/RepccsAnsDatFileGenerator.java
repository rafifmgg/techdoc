package com.ocmsintranet.cronservice.utilities.repccs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generator for Repccs ans format .dat files with specific format requirements
 * Used for Unqualified Advisory Notices (ANS) processing
 *
 * File Format:
 * - Header Record: H + YYYYMMDD + HHMM + 37 spaces (total 50 chars)
 * - Detail Records: D + licence plate + filler (total 50 chars each)
 * - Trailer Record: T + record count + 42 spaces (total 50 chars)
 *
 * Each record has a fixed length of 50 characters
 */
@Component
public class RepccsAnsDatFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(RepccsAnsDatFileGenerator.class);

    // File format constants
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    private static final int LICENCE_PLATE_LENGTH = 12;
    private static final int HEADER_FILLER_LENGTH = 37;
    private static final int DETAIL_FILLER_LENGTH = 37;
    private static final int TRAILER_FILLER_LENGTH = 42;

    // Date/time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    public static class AnsvehVehicleData {
        private final String licencePlate;

        public AnsvehVehicleData(String licencePlate) {
            this.licencePlate = licencePlate;
        }

        public String getLicencePlate() {
            return licencePlate;
        }
    }

    public String generateDatFileContent(List<AnsvehVehicleData> vehicleDataList) {
        logger.info("Generating Repccs ans .dat file content for {} vehicles", vehicleDataList.size());

        if (vehicleDataList == null || vehicleDataList.isEmpty()) {
            throw new IllegalArgumentException("Vehicle data list cannot be null or empty");
        }

        StringBuilder content = new StringBuilder();

        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");

        // 2. Generate Detail Records
        for (AnsvehVehicleData vehicleData : vehicleDataList) {
            content.append(generateDetailRecord(vehicleData)).append("\n");
        }

        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(vehicleDataList.size()));

        logger.info("Successfully generated Repccs ans .dat file content with {} records", vehicleDataList.size());
        return content.toString();
    }

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

    private String generateDetailRecord(AnsvehVehicleData vehicleData) {
        StringBuilder detail = new StringBuilder();
        detail.append(DETAIL_RECORD_TYPE);

        // Licence plate (14 chars, left-aligned)
        String licencePlate = vehicleData.getLicencePlate() != null ?
                vehicleData.getLicencePlate() : "";
        if (licencePlate.length() > LICENCE_PLATE_LENGTH) {
            licencePlate = licencePlate.substring(0, LICENCE_PLATE_LENGTH);
        } else {
            licencePlate = String.format("%-" + LICENCE_PLATE_LENGTH + "s", licencePlate);
        }
        detail.append(licencePlate);

        // Filler
        detail.append(" ".repeat(DETAIL_FILLER_LENGTH));

        return detail.toString();
    }

    private String generateTrailerRecord(int totalRecords) {
        StringBuilder trailer = new StringBuilder();
        trailer.append(TRAILER_RECORD_TYPE);
        trailer.append(String.format("%07d", totalRecords));
        //trailer.append(String.format("%-"+TOTAL_RECORDS+"s",totalRecords));
        trailer.append(" ".repeat(TRAILER_FILLER_LENGTH));

        return trailer.toString();
    }

    public String generateFileName() {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        return "REPCCS_ANSVEH_" + timestamp + ".DAT";
    }

}
