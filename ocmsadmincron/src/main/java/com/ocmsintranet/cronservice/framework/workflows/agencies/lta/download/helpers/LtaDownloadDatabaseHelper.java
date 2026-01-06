package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.utilities.SuspensionApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for LTA Download database operations
 */
@Slf4j
@Component
public class LtaDownloadDatabaseHelper {

    private final TableQueryService tableQueryService;

    @Autowired
    private SuspensionApiClient suspensionApiClient;

    // Patterns for vehicle registration number types
    private static final Pattern SG_PATTERN = Pattern.compile("^[A-Z]{1,3}[0-9]{1,4}[A-Z]?$");
    private static final Pattern MY_PATTERN = Pattern.compile("^[A-Z]{1,3}[0-9]{1,4}$");

    @Value("${notification.email.lta.error:ocms-support@ura.gov.sg}")
    private String errorNotificationEmail;

    public LtaDownloadDatabaseHelper(TableQueryService tableQueryService) {
        this.tableQueryService = tableQueryService;
    }

    /**
     * Apply TS-ROV for error codes 1-4 using SuspensionApiClient
     * OCMS 17 - Refactored to use centralized suspension API
     *
     * @param vehicleNumber The vehicle registration number
     * @param errorCode The error code (1-4)
     * @return true if the operation was successful
     */
    public boolean applyTsRov(String vehicleNumber, int errorCode) {
        log.info("Applying TS-ROV for vehicle {} with error code {}", vehicleNumber, errorCode);

        try {
            // Find the offence notice record at ROV stage
            Map<String, Object> filters = new HashMap<>();
            filters.put("vehicleNumber", vehicleNumber);
            filters.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV);

            log.info("Searching for offence notice with vehicle: {} and nextProcessingStage: ROV", vehicleNumber);
            List<Map<String, Object>> records = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (records.isEmpty()) {
                log.error("No offence notice record found for vehicle {} at ROV stage", vehicleNumber);
                return false;
            }

            // Extract notice details
            Map<String, Object> record = records.get(0);
            String noticeNo = (String) record.get("noticeNo");
            String currentProcessingStage = (String) record.get("lastProcessingStage");

            log.info("Found notice {} - Vehicle: {}, Current Stage: {}",
                    noticeNo, vehicleNumber, currentProcessingStage);

            // Build suspension remarks with LTA error code and message
            String errorMessage = getErrorMessage(errorCode);
            String suspensionRemarks = String.format("Auto-suspended: LTA error code %d - %s", errorCode, errorMessage);

            // Call suspension API via SuspensionApiClient
            Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                noticeNo,
                SystemConstant.SuspensionType.TEMPORARY, // TS
                SystemConstant.SuspensionReason.ROV,     // ROV
                suspensionRemarks,
                SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                SystemConstant.Subsystem.OCMS_CODE,
                null,  // caseNo
                null   // daysToRevive - NULL for TS-ROV (no auto-revival)
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("Successfully applied TS-ROV via API for notice {} (vehicle: {}, error code: {})",
                        noticeNo, vehicleNumber, errorCode);

                // Update LTA error code in VON (additional field not handled by suspension API)
                updateLtaErrorCode(noticeNo, errorCode);

                // Send notification email about the error
                sendErrorNotification(vehicleNumber, errorCode, noticeNo);

                return true;
            } else {
                String appCode = (String) apiResponse.get("appCode");
                String message = (String) apiResponse.get("message");
                log.error("Failed to apply TS-ROV via API for notice {}: {} ({})",
                        noticeNo, message, appCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error applying TS-ROV for vehicle {}: {}", vehicleNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update LTA error code in valid_offence_notice table
     * This is a supplementary update since ltaErrorCode is not part of standard suspension fields
     */
    private void updateLtaErrorCode(String noticeNo, int errorCode) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);

            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("ltaErrorCode", errorCode);

            tableQueryService.patch("ocms_valid_offence_notice", filters, updateFields);

            log.debug("Updated LTA error code {} for notice {}", errorCode, noticeNo);
        } catch (Exception e) {
            log.warn("Failed to update LTA error code for notice {}: {}", noticeNo, e.getMessage());
            // Non-critical - don't fail the suspension
        }
    }
    
    /**
     * Check if a vehicle is in the suspension list
     *
     * @param vehicleNumber The vehicle registration number
     * @return true if the vehicle is suspended
     */
    public boolean checkVehicleSuspension(String vehicleNumber) {
        log.debug("Checking suspension for vehicle {}", vehicleNumber);
        
        try {
            // Check if the table exists in the database schema
            try {
                Map<String, Object> filters = new HashMap<>();
                filters.put("vehicleNumber", vehicleNumber);
                filters.put("status", "ACTIVE");
                
                List<Map<String, Object>> records = tableQueryService.query("ocms_vehicle_suspension", filters);
                
                return !records.isEmpty();
            } catch (Exception tableException) {
                // If the table doesn't exist, log a warning and return false
                if (tableException.getMessage() != null && 
                    tableException.getMessage().contains("No entity found for table: ocms_vehicle_suspension")) {
                    log.warn("Table ocms_vehicle_suspension does not exist in the current environment. Skipping suspension check.");
                    return false;
                } else {
                    // Re-throw if it's a different exception
                    throw tableException;
                }
            }
        } catch (Exception e) {
            log.error("Error checking vehicle suspension for {}", vehicleNumber, e);
            return false;
        }
    }
    
    /**
     * Check if a vehicle is in the exclusion list
     *
     * @param vehicleNumber The vehicle registration number
     * @return true if the vehicle is excluded
     */
    public boolean checkVehicleExclusion(String vehicleNumber) {
        log.debug("Checking exclusion for vehicle {}", vehicleNumber);
        
        try {
            // Check if the table exists in the database schema
            try {
                Map<String, Object> filters = new HashMap<>();
                filters.put("vehicleNumber", vehicleNumber);
                filters.put("status", "ACTIVE");
                
                List<Map<String, Object>> records = tableQueryService.query("ocms_vehicle_exclusion", filters);
                
                return !records.isEmpty();
            } catch (Exception tableException) {
                // If the table doesn't exist, log a warning and return false
                if (tableException.getMessage() != null && 
                    tableException.getMessage().contains("No entity found for table: ocms_vehicle_exclusion")) {
                    log.warn("Table ocms_vehicle_exclusion does not exist in the current environment. Skipping exclusion check.");
                    return false;
                } else {
                    // Re-throw if it's a different exception
                    throw tableException;
                }
            }
        } catch (Exception e) {
            log.error("Error checking vehicle exclusion for {}", vehicleNumber, e);
            return false;
        }
    }
    
    /**
     * Determine the vehicle type based on the vehicle number
     *
     * @param vehicleNumber The vehicle registration number
     * @return The vehicle type code
     */
    public String determineVehicleType(String vehicleNumber) {
        log.debug("Determining vehicle type for {}", vehicleNumber);
        
        try {
            if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
                return "UK"; // Unknown
            }
            
            String cleanVehicleNumber = vehicleNumber.trim().toUpperCase();
            
            // Check for Singapore vehicle pattern
            if (SG_PATTERN.matcher(cleanVehicleNumber).matches()) {
                return "SG";
            }
            
            // Check for Malaysia vehicle pattern
            if (MY_PATTERN.matcher(cleanVehicleNumber).matches()) {
                return "MY";
            }
            
            // If it doesn't match SG or MY patterns, consider it as Other
            return "OT";
        } catch (Exception e) {
            log.error("Error determining vehicle type for {}", vehicleNumber, e);
            return "UK";
        }
    }
    
    /**
     * Process snowflake data for a vehicle
     *
     * @param vehicleNumber The vehicle registration number
     * @return true if snowflake data was found and processed
     */
    public boolean processSnowflakeData(String vehicleNumber) {
        log.debug("Processing snowflake data for vehicle {}", vehicleNumber);
        
        try {
            // Query for snowflake data
            Map<String, Object> filters = new HashMap<>();
            filters.put("vehicleNumber", vehicleNumber);
            
            List<Map<String, Object>> records = tableQueryService.query("ocms_snowflake_data", filters);
            
            if (records.isEmpty()) {
                log.debug("No snowflake data found for vehicle {}", vehicleNumber);
                return false;
            }
            
            // Process the snowflake data
            Map<String, Object> snowflakeData = records.get(0);
            
            // Update the offence notice with snowflake data
            Map<String, Object> offenceFilters = new HashMap<>();
            offenceFilters.put("vehicleNumber", vehicleNumber);
            offenceFilters.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV);
            
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("snowflakeProcessed", true);
            updateFields.put("snowflakeData", snowflakeData.get("data"));
            updateFields.put("snowflakeProcessedDate", LocalDateTime.now());
            
            tableQueryService.patch("ocms_offence_notice", offenceFilters, updateFields);
            
            return true;
        } catch (Exception e) {
            log.error("Error processing snowflake data for vehicle {}", vehicleNumber, e);
            return false;
        }
    }
    
    /**
     * Update the status of a record
     *
     * @param vehicleNumber The vehicle registration number
     * @param statusCode The status code (S for suspended, E for excluded)
     * @return true if the update was successful
     */
    public boolean updateRecordStatus(String vehicleNumber, String statusCode) {
        log.info("Updating record status for vehicle {} to {}", vehicleNumber, statusCode);
        
        try {
            // Find the offence notice record
            Map<String, Object> filters = new HashMap<>();
            filters.put("vehicleNumber", vehicleNumber);
            filters.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV);
            
            // Update the record with the status
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
            updateFields.put("prevProcessingDate", LocalDateTime.now());
            updateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
            updateFields.put("lastProcessingDate", LocalDateTime.now());
            updateFields.put("nextProcessingStage", statusCode.equals("S") ? "SUS" : "EXC");
            updateFields.put("nextProcessingDate", LocalDateTime.now().plusDays(30)); // Skip for 30 days
            updateFields.put("statusCode", statusCode);
            
            tableQueryService.patch("ocms_offence_notice", filters, updateFields);
            
            return true;
        } catch (Exception e) {
            log.error("Error updating record status for vehicle {}", vehicleNumber, e);
            return false;
        }
    }
    
    /**
     * Update database records with LTA response data
     *
     * @param record The parsed record data
     * @param vehicleType The determined vehicle type
     * @param hasSnowflakeData Whether snowflake data was processed
     * @return true if the update was successful
     */
    public boolean updateDatabaseRecords(Map<String, Object> record, String vehicleType, boolean hasSnowflakeData) {
        String vehicleNumber = (String) record.get("vehicleNumber");
        log.info("Updating database records for vehicle {}", vehicleNumber);
        
        try {
            // Find the offence notice record
            Map<String, Object> filters = new HashMap<>();
            filters.put("vehicleNumber", vehicleNumber);
            filters.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV);
            
            List<Map<String, Object>> offenceRecords = tableQueryService.query("ocms_offence_notice", filters);
            
            if (offenceRecords.isEmpty()) {
                log.error("No offence notice record found for vehicle {}", vehicleNumber);
                return false;
            }
            
            Map<String, Object> offenceRecord = offenceRecords.get(0);
            String offenceNoticeId = (String) offenceRecord.get("offenceNoticeId");
            
            // Update offence notice details
            updateOffenceNoticeDetails(offenceNoticeId, record, vehicleType);
            
            // Update owner/driver details
            updateOwnerDriverDetails(offenceNoticeId, record);
            
            // Update processing stages
            updateProcessingStages(offenceNoticeId);
            
            return true;
        } catch (Exception e) {
            log.error("Error updating database records for vehicle {}", vehicleNumber, e);
            return false;
        }
    }
    
    /**
     * Update offence notice details
     *
     * @param offenceNoticeId The offence notice ID
     * @param record The parsed record data
     * @param vehicleType The determined vehicle type
     */
    private void updateOffenceNoticeDetails(String offenceNoticeId, Map<String, Object> record, String vehicleType) {
        log.debug("Updating offence notice details for ID {}", offenceNoticeId);
        
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("offenceNoticeId", offenceNoticeId);
            
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("vehicleType", vehicleType);
            updateFields.put("chassisNumber", record.get("chassisNumber"));
            updateFields.put("vehicleMake", record.get("vehicleMake"));
            updateFields.put("vehicleColor", record.get("vehicleColor"));
            updateFields.put("roadTaxExpiryDate", parseDate((String) record.get("roadTaxExpiryDate")));
            updateFields.put("registrationDate", parseDate((String) record.get("registrationDate")));
            updateFields.put("deregistrationDate", parseDate((String) record.get("deregistrationDate")));
            updateFields.put("iuNumber", record.get("iuNumber"));
            updateFields.put("ltaResponseDate", LocalDateTime.now());
            
            tableQueryService.patch("ocms_offence_notice_detail", filters, updateFields);
        } catch (Exception e) {
            log.error("Error updating offence notice details for ID {}", offenceNoticeId, e);
            throw e;
        }
    }
    
    /**
     * Update owner/driver details
     *
     * @param offenceNoticeId The offence notice ID
     * @param record The parsed record data
     */
    private void updateOwnerDriverDetails(String offenceNoticeId, Map<String, Object> record) {
        log.debug("Updating owner/driver details for ID {}", offenceNoticeId);
        
        try {
            // Check if owner/driver record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("offenceNoticeId", offenceNoticeId);
            
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver", filters);
            
            Map<String, Object> ownerData = new HashMap<>();
            ownerData.put("offenceNoticeId", offenceNoticeId);
            ownerData.put("ownerIdType", record.get("ownerIdType"));
            ownerData.put("ownerId", record.get("ownerId"));
            ownerData.put("ownerName", record.get("ownerName"));
            ownerData.put("addressType", record.get("addressType"));
            ownerData.put("blockHouseNo", record.get("blockHouseNo"));
            ownerData.put("streetName", record.get("streetName"));
            ownerData.put("buildingName", record.get("buildingName"));
            ownerData.put("postalCode", record.get("postalCode"));
            ownerData.put("effectiveOwnershipDate", parseDate((String) record.get("effectiveOwnershipDate")));
            ownerData.put("updatedAt", LocalDateTime.now());
            
            if (existingRecords.isEmpty()) {
                // Insert new record
                ownerData.put("createdAt", LocalDateTime.now());
                tableQueryService.post("ocms_offence_notice_owner_driver", ownerData);
            } else {
                // Update existing record
                // Add update date to satisfy NOT NULL constraint
                ownerData.put("updDate", LocalDateTime.now());
                ownerData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                tableQueryService.patch("ocms_offence_notice_owner_driver", filters, ownerData);
            }
        } catch (Exception e) {
            log.error("Error updating owner/driver details for ID {}", offenceNoticeId, e);
            throw e;
        }
    }
    
    /**
     * Update processing stages for an offence notice
     *
     * @param offenceNoticeId The offence notice ID
     */
    private void updateProcessingStages(String offenceNoticeId) {
        log.debug("Updating processing stages for ID {}", offenceNoticeId);
        
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("offenceNoticeId", offenceNoticeId);
            
            // Get current record to preserve last processing date
            List<Map<String, Object>> records = tableQueryService.query("ocms_offence_notice", filters);
            if (records.isEmpty()) {
                log.error("No offence notice record found for ID {}", offenceNoticeId);
                return;
            }
            
            Map<String, Object> record = records.get(0);
            
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
            updateFields.put("prevProcessingDate", record.get("lastProcessingDate"));
            updateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
            updateFields.put("lastProcessingDate", LocalDateTime.now());
            updateFields.put("nextProcessingStage", SystemConstant.SuspensionReason.ENA);
            updateFields.put("nextProcessingDate", LocalDateTime.now());
            
            tableQueryService.patch("ocms_offence_notice", filters, updateFields);
        } catch (Exception e) {
            log.error("Error updating processing stages for ID {}", offenceNoticeId, e);
            throw e;
        }
    }
    
    /**
     * Send error notification email
     *
     * @param vehicleNumber The vehicle registration number
     * @param errorCode The error code
     * @param noticeNo The notice number
     */
    private void sendErrorNotification(String vehicleNumber, int errorCode, String noticeNo) {
        log.info("Sending error notification for vehicle {} with error code {}", vehicleNumber, errorCode);

        try {
            // In a real implementation, this would send an email
            // For now, just log the notification
            String errorMessage = getErrorMessage(errorCode);
            log.info("Would send email to {} with subject 'LTA Error: {} - {}' and message '{}'",
                    errorNotificationEmail, vehicleNumber, errorCode, errorMessage);

            // Record the notification in the database
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("noticeNo", noticeNo);
            notificationData.put("notificationType", "LTA_ERROR");
            notificationData.put("notificationStatus", "SENT");
            notificationData.put("recipientEmail", errorNotificationEmail);
            notificationData.put("subject", "LTA Error: " + vehicleNumber + " - " + errorCode);
            notificationData.put("message", errorMessage);
            notificationData.put("createdAt", LocalDateTime.now());

            tableQueryService.post("ocms_notification", notificationData);
        } catch (Exception e) {
            log.error("Error sending notification for vehicle {}", vehicleNumber, e);
        }
    }
    
    /**
     * Get error message for an error code
     *
     * @param errorCode The error code
     * @return The error message
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case 1:
                return "Vehicle not found in LTA records";
            case 2:
                return "Vehicle has been deregistered";
            case 3:
                return "Vehicle ownership has changed";
            case 4:
                return "Invalid vehicle registration number format";
            default:
                return "Unknown error";
        }
    }
    
    /**
     * Parse date string to LocalDate
     *
     * @param dateString The date string in format YYYYMMDD
     * @return The parsed LocalDate or null if invalid
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateString);
            return null;
        }
    }
}
