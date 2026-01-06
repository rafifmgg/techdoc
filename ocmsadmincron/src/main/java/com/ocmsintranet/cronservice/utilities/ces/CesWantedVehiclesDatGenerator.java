package com.ocmsintranet.cronservice.utilities.ces;

import com.ocmsintranet.cronservice.framework.dto.ces.CesWantedVehicleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CesWantedVehiclesDatGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CesWantedVehiclesDatGenerator.class);

    // File format constants
    private static final String HEADER_RECORD_TYPE = "H";
    private static final String DETAIL_RECORD_TYPE = "D";
    private static final String TRAILER_RECORD_TYPE = "T";
    private static final int VEHICLE_NUMBER_LENGTH = 14;
    private static final int OBU_LABEL_LENGTH = 10;
    private static final int MESSAGE_LENGTH = 100;
    private static final int HEADER_FILLER_LENGTH = 137;
    private static final int DETAIL_FILLER_LENGTH = 27;
    private static final int TRAILER_FILLER_LENGTH = 142;

    // Date/time formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generateDatFileContent(List<CesWantedVehicleData> wantedVehicles) {
        logger.info("Generating CES Wanted Vehicles .dat file content for {} records", wantedVehicles.size());

        if (wantedVehicles == null || wantedVehicles.isEmpty()) {
            throw new IllegalArgumentException("Wanted vehicle data list cannot be null or empty");
        }

        StringBuilder content = new StringBuilder();

        // 1. Generate Header Record
        content.append(generateHeaderRecord()).append("\n");

        // 2. Generate Detail Records
        for (CesWantedVehicleData vehicle : wantedVehicles) {
            content.append(generateDetailRecord(vehicle)).append("\n");
        }

        // 3. Generate Trailer Record
        content.append(generateTrailerRecord(wantedVehicles.size()));

        logger.info("Successfully generated CES Wanted Vehicles .dat file content with {} records", wantedVehicles.size());
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
        //header.append(String.format("%08d", 0));
        header.append(" ".repeat(HEADER_FILLER_LENGTH));

        return header.toString();
    }

    private String generateDetailRecord(CesWantedVehicleData vehicle) {
        StringBuilder detail = new StringBuilder();
        detail.append(DETAIL_RECORD_TYPE);

        // Vehicle number (12 chars, left-aligned)
        String vehicleNumber = vehicle.getVehicleNo() != null ?
                vehicle.getVehicleNo() : "";
        if (vehicleNumber.length() > VEHICLE_NUMBER_LENGTH) {
            vehicleNumber = vehicleNumber.substring(0, VEHICLE_NUMBER_LENGTH);
        } else {
            vehicleNumber = String.format("%-14s", vehicleNumber);
        }
        detail.append(vehicleNumber);
        //add obu label
        String obuLabel = "";
        obuLabel = truncateAndPad(obuLabel, 10); // 10-character field
        detail.append(obuLabel);
        // Message (50 chars, left-aligned)
        String message = vehicle.getMessage() != null ?
                vehicle.getMessage() : "";
        if (message.length() > MESSAGE_LENGTH) {
            message = message.substring(0, MESSAGE_LENGTH);
        } else {
            message = String.format("%-" + MESSAGE_LENGTH + "s", message);
            // message = String.format("%-100s", message);
        }
        detail.append(message);

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
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        return "CES_WANTED_VEHICLES_" + timestamp + ".DAT";
    }
    private String truncateAndPad(String input, int length) {
        if (input.length() > length) {
            return input.substring(0, length);
        }
        return String.format("%-" + length + "s", input);
    }
}
