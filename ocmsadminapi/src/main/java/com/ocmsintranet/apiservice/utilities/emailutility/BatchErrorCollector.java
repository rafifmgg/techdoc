package com.ocmsintranet.apiservice.utilities.emailutility;

import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for collecting errors during batch processing and sending a consolidated email
 */
@Component
@Slf4j
public class BatchErrorCollector {

    @Autowired
    private ApiErrorEmailHelper emailHelper;

    private final List<ErrorEntry> errorEntries = new ArrayList<>();
    private String batchId;
    private String endpoint;

    /**
     * Initialize a new batch error collection
     * @param endpoint The API endpoint being processed
     */
    public void initBatch(String endpoint) {
        this.errorEntries.clear();
        this.batchId = UUID.randomUUID().toString();
        this.endpoint = endpoint;
        log.info("Initialized batch error collection with ID: {}", batchId);
    }

    /**
     * Add an error to the batch collection
     * @param dto The DTO that caused the error
     * @param errorType The type of error
     * @param errorMessage The error message
     * @param statusCode The HTTP status code
     */
    public void addError(OffenceNoticeDto dto, String errorType, String errorMessage, int statusCode) {
        // Skip duplicate notice errors for EHTSFTP subsystem
        if ("EHTSFTP".equals(dto.getSubsystemLabel()) && 
            errorType.equals("DUPLICATE_NOTICE")) {
            log.info("Skipping email notification for duplicate notice in EHTSFTP subsystem: {}", 
                     dto.getNoticeNo());
            return;
        }
        
        ErrorEntry entry = new ErrorEntry();
        entry.dto = dto;
        entry.errorType = errorType;
        entry.errorMessage = errorMessage;
        entry.statusCode = statusCode;
        
        errorEntries.add(entry);
        log.info("Added error to batch collection: {} - {}", errorType, errorMessage);
    }

    /**
     * Send a consolidated email with all collected errors if any exist
     */
    public void sendConsolidatedErrorEmail() {
        if (errorEntries.isEmpty()) {
            log.info("No errors to send in batch {}", batchId);
            return;
        }

        log.info("Sending consolidated error email for batch {} with {} errors", batchId, errorEntries.size());
        
        StringBuilder errorDetails = new StringBuilder();
        errorDetails.append("<h3>Batch Processing Errors</h3>");
        errorDetails.append("<p>The following errors occurred during batch processing:</p>");
        errorDetails.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        errorDetails.append("<tr style='background-color: #f2f2f2;'>");
        errorDetails.append("<th>Notice Number</th>");
        errorDetails.append("<th>Vehicle Number</th>");
        errorDetails.append("<th>Subsystem</th>");
        errorDetails.append("<th>Error Type</th>");
        errorDetails.append("<th>Error Message</th>");
        errorDetails.append("</tr>");

        for (ErrorEntry entry : errorEntries) {
            errorDetails.append("<tr>");
            errorDetails.append("<td>").append(entry.dto.getNoticeNo() != null ? entry.dto.getNoticeNo() : "N/A").append("</td>");
            errorDetails.append("<td>").append(entry.dto.getVehicleNo() != null ? entry.dto.getVehicleNo() : "N/A").append("</td>");
            errorDetails.append("<td>").append(entry.dto.getSubsystemLabel() != null ? entry.dto.getSubsystemLabel() : "N/A").append("</td>");
            errorDetails.append("<td>").append(entry.errorType).append("</td>");
            errorDetails.append("<td>").append(entry.errorMessage).append("</td>");
            errorDetails.append("</tr>");
        }

        errorDetails.append("</table>");
        
        // Send the consolidated email
        emailHelper.sendErrorNotificationEmailAsync(
            batchId,
            "CREATE_NOTICE_BATCH",
            endpoint,
            HttpStatus.MULTI_STATUS.value(),
            errorDetails.toString()
        );
        
        // Clear the collection after sending
        errorEntries.clear();
    }

    /**
     * Inner class to store error information
     */
    private static class ErrorEntry {
        private OffenceNoticeDto dto;
        private String errorType;
        private String errorMessage;
        private int statusCode;
    }
}
