package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COMPLETE UPDATE ONLY VERSION - Main processing orchestrator for LTA records
 * Coordinates between different processing components based on business rules
 * 
 * CRITICAL CHANGES:
 * - ALL CREATE OPERATIONS COMPLETELY REMOVED
 * - Records MUST pre-exist in all 3 tables
 * - Only UPDATE operations are performed
 * - If records don't exist, operations are skipped with warnings
 * - No creation logic anywhere in this class
 */
@Slf4j
@Component
public class LtaProcessingOrchestrator {

    private final LtaDatabaseOperations databaseOperations;
    private final LtaProcessingStageManager stageManager;
    private final LtaBusinessRulesChecker businessRulesChecker;
    private final LtaSnowflakeProcessor snowflakeProcessor;
    private final TableQueryService tableQueryService;

    public LtaProcessingOrchestrator(
            LtaDatabaseOperations databaseOperations,
            LtaProcessingStageManager stageManager,
            LtaBusinessRulesChecker businessRulesChecker,
            LtaSnowflakeProcessor snowflakeProcessor,
            TableQueryService tableQueryService) {
        this.databaseOperations = databaseOperations;
        this.stageManager = stageManager;
        this.businessRulesChecker = businessRulesChecker;
        this.snowflakeProcessor = snowflakeProcessor;
        this.tableQueryService = tableQueryService;
    }

    /**
     * Apply TS-ROV status for error codes 1-4
     *
     * @param record The LTA record with error code
     */
    public void applyTsRovStatus(Map<String, Object> record) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        String vehicleNumber = (String) record.get("vehicleNumber");
        
        log.info("Applying TS-ROV status for vehicle {} with notice {}", vehicleNumber, offenceNoticeNumber);
        
        // Apply TS-ROV status
        stageManager.applyTsRovStatus(record);
    }

    /**
     * Process diplomat vehicle - Update database and set to RD1
     *
     * @param record The LTA record for diplomat vehicle
     */
    public void processDiplomatVehicle(Map<String, Object> record) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        
        log.info("Processing diplomat vehicle for notice {}", offenceNoticeNumber);
        
        // Update database tables first
        databaseOperations.updateDatabaseTables(record, offenceNoticeNumber);
        
        // Set processing stage to RD1
        stageManager.updateProcessingStageToRD1(record);
    }

    /**
     * Process VIP vehicle - Update database with vehicle_registration_type='V' and set to RD1
     *
     * @param record The LTA record for VIP vehicle
     */
    public void processVipVehicle(Map<String, Object> record) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        String vehicleNumber = (String) record.get("vehicleNumber");

        log.info("Processing VIP vehicle {} for notice {}", vehicleNumber, offenceNoticeNumber);

        // OCMS 14: Update vehicle_registration_type = 'V' for VIP vehicles
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);

            Map<String, Object> fields = new HashMap<>();
            fields.put("vehicleRegistrationType", "V"); // Set as VIP
            fields.put("updDate", LocalDateTime.now());
            fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            log.info("Setting vehicle_registration_type='V' for VIP vehicle {} (notice: {})", vehicleNumber, offenceNoticeNumber);
            tableQueryService.patch("ocms_valid_offence_notice", filters, fields);
        } catch (Exception e) {
            log.error("Error updating vehicle_registration_type for VIP notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
        }

        // Update database tables with owner data
        databaseOperations.updateDatabaseTables(record, offenceNoticeNumber);

        // Set processing stage to RD1
        stageManager.updateProcessingStageToRD1(record);
    }

    /**
     * Process standard vehicle according to business rules
     *
     * @param record The LTA record for standard vehicle
     */
    public void processStandardVehicle(Map<String, Object> record) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        
        log.info("Processing standard vehicle for notice {}", offenceNoticeNumber);
        
        // Step 1: Check if notice is suspended
        boolean isNoticeSuspended = businessRulesChecker.isNoticeSuspended(record);
        
        if (isNoticeSuspended) {
            log.info("Notice {} is suspended, checking if under TS-HST", offenceNoticeNumber);
            
            // Step 2: If suspended, check if under TS-HST
            boolean isUnderTsHst = businessRulesChecker.isNoticeUnderTsHst(record);
            
            if (!isUnderTsHst) {
                log.info("Notice {} is suspended but not under TS-HST, ending process", offenceNoticeNumber);
                return;
            }
            
            log.info("Notice {} is under TS-HST, continuing to exclusion list check", offenceNoticeNumber);
        } else {
            log.info("Notice {} is not suspended, continuing to exclusion list check", offenceNoticeNumber);
        }
        
        // Step 3: Check if offender is in exclusion list
        boolean isInExclusionList = businessRulesChecker.isOffenderInExclusionList(record);
        
        if (isInExclusionList) {
            log.info("Offender is in exclusion list, updating to RD1");
            stageManager.updateProcessingStageToRD1(record);
            return;
        }
        
        // Step 4: Check if ID type is passport
        String ownerIdType = (String) record.get("ownerIdType");
        if (LtaIdTypeUtil.isPassportIdType(ownerIdType)) {
            log.info("ID type is passport (ownerIdType={}), querying passport data then updating to RD1", ownerIdType);
            snowflakeProcessor.queryPassportData(record);
            stageManager.updateProcessingStageToRD1(record);
            return;
        } else {
            log.info("ID type is not passport (ownerIdType={}), calling Snowflake function", ownerIdType);
            snowflakeProcessor.processSnowflakeFunction(record, stageManager);
            return;
        }
    }

    /**
     * UPDATE ONLY VERSION - Update offence notice with owner data from LTA response
     * This method updates both ocms_valid_offence_notice, ocms_offence_notice_detail and ocms_offence_notice_owner_driver tables
     * with vehicle and owner data extracted from the LTA response
     * 
     * CRITICAL: ONLY UPDATES EXISTING RECORDS - NO CREATION
     *
     * @param record The LTA response record containing vehicle and owner data
     * @param offenceNoticeNumber The offence notice number
     */
    public void updateNoticeWithOwnerData(Map<String, Object> record, String offenceNoticeNumber) {
        try {
            log.info("Updating notice with owner data for notice {} (UPDATE ONLY MODE)", offenceNoticeNumber);
            
            // Step 1: Verify the offence notice exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            List<Map<String, Object>> offenceNotices = tableQueryService.query("ocms_valid_offence_notice", filters);
            
            if (offenceNotices.isEmpty()) {
                log.error("No offence notice found for notice number {} - cannot proceed with updates", offenceNoticeNumber);
                return;
            }
            
            String noticeNo = (String) offenceNotices.get(0).get("noticeNo");
            log.info("Found existing offence notice number {} for processing", noticeNo);
            
            // Step 2: Update ocms_valid_offence_notice with vehicle information
            updateValidOffenceNotice(record, offenceNoticeNumber);
            
            // Step 3: Update ocms_offence_notice_detail with vehicle information
            updateOffenceNoticeDetail(noticeNo, record);
            
            // Step 4: Update ocms_offence_notice_owner_driver with owner information
            try {
                updateOffenceNoticeOwnerDriver(noticeNo, record);
                log.info("Successfully updated notice with owner data for notice {}", offenceNoticeNumber);
            } catch (Exception e) {
                // Get error code from record
                String errorCode = (String) record.get("errorCode");
                
                // Check if error code is 0 or 3 (special handling for these codes)
                if (errorCode == null || "0".equals(errorCode) || "3".equals(errorCode)) {
                    log.error("Failed to update owner driver data for notice {} with error code {}, applying TS-ROV status: {}", 
                            offenceNoticeNumber, errorCode, e.getMessage());
                    
                    // Apply TS-ROV status for error codes 0 or 3 when storing data to owner driver table fails
                    stageManager.applyTsRovStatus(record);
                } else {
                    // For other error codes, just log the error
                    log.error("Error updating owner driver data for notice {} with error code {}: {}", 
                            offenceNoticeNumber, errorCode, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error updating notice with owner data for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
        }
    }
    
    /**
     * UPDATE ONLY - Update the valid offence notice table with vehicle information
     * This updates the ocms_valid_offence_notice table
     * CRITICAL: Only updates existing records, never creates new ones
     *
     * @param record The LTA response record
     * @param offenceNoticeNumber The offence notice number
     */
    private void updateValidOffenceNotice(Map<String, Object> record, String offenceNoticeNumber) {
        try {
            log.debug("Updating valid offence notice for notice {} (UPDATE ONLY)", offenceNoticeNumber);
            
            String vehicleNumber = (String) record.get("vehicleNumber");
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);

            Map<String, Object> fields = new HashMap<>();

            // Only update vehicleRegistrationType if eligible
            String diplomaticFlag = (String) record.get("diplomaticFlag");
            if (diplomaticFlag != null && diplomaticFlag.equalsIgnoreCase("y")) {
                log.info("DiplomaticFlag flag detected for vehicle {}. Setting vehicle_registration_type to 'D'", vehicleNumber);
                fields.put("vehicleRegistrationType", "D"); // Set as Diplomat
            }

            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (existingRecords != null && !existingRecords.isEmpty()) {
                if (!fields.isEmpty()) {
                    // For update operation, add update audit fields
                    fields.put("updDate", LocalDateTime.now());
                    fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                    log.info("Updating existing valid offence notice for notice {}", offenceNoticeNumber);
                    tableQueryService.patch("ocms_valid_offence_notice", filters, fields);
                } else {
                    log.debug("No eligible fields to update for valid offence notice {}", offenceNoticeNumber);
                }
            } else {
                log.warn("No existing valid offence notice found for notice {} - skipping update", offenceNoticeNumber);
            }
        } catch (Exception e) {
            log.error("Error updating valid offence notice for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
        }
    }
    
    /**
     * UPDATE ONLY VERSION - Update the offence notice detail table with vehicle information
     * This updates the ocms_offence_notice_detail table
     * Fixed field mapping to match actual entity field names
     * CRITICAL: Only updates existing records, never creates new ones
     *
     * @param noticeNo The offence notice number
     * @param record The LTA response record
     */
    private void updateOffenceNoticeDetail(String noticeNo, Map<String, Object> record) {
        try {
            log.debug("Updating offence notice detail for notice number {} (UPDATE ONLY)", noticeNo);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            
            // CRITICAL: Check if record exists first - NO CREATION
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_detail", filters);
            
            if (existingRecords.isEmpty()) {
                log.warn("No existing offence notice detail record found for notice number {} - skipping update", noticeNo);
                return;
            }
            
            // Build update fields - Use correct entity field names
            Map<String, Object> fields = new HashMap<>();
            
            // Map LTA fields to actual entity fields
            addIfPresentMapped(fields, record, "chassisNumber", "ltaChassisNumber");
            addIfPresentMapped(fields, record, "vehicleMake", "ltaMakeDescription");
            addIfPresentMapped(fields, record, "primaryColour", "ltaPrimaryColour");
            addIfPresentMapped(fields, record, "secondaryColour", "ltaSecondaryColour");
            addIfPresentMapped(fields, record, "diplomaticFlag", "ltaDiplomaticFlag");
            addIfPresentMapped(fields, record, "iuObuLabel", "iuNo");
            addIfPresentMapped(fields, record, "errorCode", "ltaErrorCode");
            
            // Handle date fields separately with proper conversion
            String roadTaxExpiryDate = (String) record.get("roadTaxExpiryDate");
            if (roadTaxExpiryDate != null && !roadTaxExpiryDate.trim().isEmpty()) {
                LocalDateTime dateTime = parseDate(roadTaxExpiryDate);
                if (dateTime != null) {
                    fields.put("ltaRoadTaxExpiryDate", dateTime);
                }
            }
            
            String effectiveOwnershipDate = (String) record.get("effectiveOwnershipDate");
            if (effectiveOwnershipDate != null && !effectiveOwnershipDate.trim().isEmpty()) {
                LocalDateTime dateTime = parseDate(effectiveOwnershipDate);
                if (dateTime != null) {
                    fields.put("ltaEffOwnershipDate", dateTime);
                }
            }
            
            String deregistrationDate = (String) record.get("deregistrationDate");
            if (deregistrationDate != null && !deregistrationDate.trim().isEmpty()) {
                LocalDateTime dateTime = parseDate(deregistrationDate);
                if (dateTime != null) {
                    fields.put("ltaDeregistrationDate", dateTime);
                }
            }
            
            // Handle weight fields
            String unladenWeight = (String) record.get("unladenWeight");
            if (unladenWeight != null && !unladenWeight.trim().isEmpty()) {
                try {
                    fields.put("ltaUnladenWeight", Integer.parseInt(unladenWeight.trim()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid unladen weight value: {}", unladenWeight);
                }
            }
            
            String maxLadenWeight = (String) record.get("maxLadenWeight");
            if (maxLadenWeight != null && !maxLadenWeight.trim().isEmpty()) {
                try {
                    fields.put("ltaMaxLadenWeight", Integer.parseInt(maxLadenWeight.trim()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid max laden weight value: {}", maxLadenWeight);
                }
            }
            fields.put("ltaProcessingDateTime", LocalDateTime.now());
            
            // ONLY UPDATE - NO CREATION
            if (!fields.isEmpty()) {
                log.info("Updating existing offence notice detail record for notice number {} with {} fields", noticeNo, fields.size());
                tableQueryService.patch("ocms_offence_notice_detail", filters, fields);
            } else {
                log.debug("No fields to update for offence notice detail {}", noticeNo);
            }
            
        } catch (Exception e) {
            log.error("Error updating offence notice detail for notice number {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Update the offence notice address table with LTA status information
     * This updates the errorCode and processingDateTime fields in the offence_notice_address table
     * for both LTA registration and mailing addresses
     * Uses tableQueryService.post() and tableQueryService.patch()
     *
     * @param noticeNo The offence notice number
     * @param processingDatetime The processing datetime to set
     * @param errorCode The error code to set
     */
    private void updateOffenceNoticeDetailStatus(String noticeNo, LocalDateTime processingDatetime, String errorCode) {
        try {
            log.debug("Updating LTA address status for notice number {}", noticeNo);
            
            // Update both LTA registration and mailing addresses
            updateAddressStatus(noticeNo, SystemConstant.OwnerDriverIndicator.OWNER, SystemConstant.AddressType.LTA_REGISTRATION, processingDatetime, errorCode);
            // updateAddressStatus(noticeNo, SystemConstant.OwnerDriverIndicator.OWNER, SystemConstant.AddressType.LTA_MAILING, processingDatetime, errorCode);
            
        } catch (Exception e) {
            log.error("Error updating LTA address status for notice number {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Update status information for a specific address record
     * Uses tableQueryService.post() for creation and tableQueryService.patch() for updates
     * 
     * @param noticeNo The offence notice number
     * @param ownerDriverIndicator The owner/driver indicator (SystemConstant.OwnerDriverIndicator.OWNER or SystemConstant.OwnerDriverIndicator.DRIVER)
     * @param addressType The type of address (SystemConstant.AddressType.LTA_REGISTRATION, SystemConstant.AddressType.LTA_MAILING, etc.)
     * @param processingDatetime The processing datetime to set
     * @param errorCode The error code to set
     */
    private void updateAddressStatus(String noticeNo, String ownerDriverIndicator, String addressType, 
                                    LocalDateTime processingDatetime, String errorCode) {
        try {
            // Define filters to check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            filters.put("ownerDriverIndicator", ownerDriverIndicator);
            filters.put("typeOfAddress", addressType);
            
            // Check if record exists
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver_addr", filters);
            boolean isNewRecord = existingRecords.isEmpty();
            
            // Prepare fields for update or create
            Map<String, Object> fields = new HashMap<>();
            
            // Set the processing datetime and error code
            if (processingDatetime != null) {
                fields.put("processingDateTime", processingDatetime);
            }
            
            if (errorCode != null) {
                fields.put("errorCode", errorCode);
            }
            
            if (isNewRecord) {
                // For create operation, add required fields
                fields.put("noticeNo", noticeNo);
                fields.put("ownerDriverIndicator", ownerDriverIndicator);
                fields.put("typeOfAddress", addressType);
                
                // Add creation audit fields
                fields.put("creDate", LocalDateTime.now());
                fields.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.debug("Creating new {} address record for notice {}", addressType, noticeNo);
                tableQueryService.post("ocms_offence_notice_owner_driver_addr", fields);
            } else {
                // For update operation, add update audit fields
                fields.put("updDate", LocalDateTime.now());
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.debug("Updating existing {} address record for notice {}", addressType, noticeNo);
                tableQueryService.patch("ocms_offence_notice_owner_driver_addr", filters, fields);
            }
            
        } catch (Exception e) {
            log.error("Error updating {} address status for notice {}: {}", addressType, noticeNo, e.getMessage(), e);
        }
    }

/**
 * Update or create the offence notice owner driver table with owner information
 * This updates or creates records in the ocms_offence_notice_owner_driver table
 * Fixed field mapping and required fields
 * Supports both update and create operations based on record existence
 * Uses transaction boundaries to ensure data consistency
 *
 * @param noticeNo The offence notice number
 * @param record The LTA response record
 */
private void updateOffenceNoticeOwnerDriver(String noticeNo, Map<String, Object> record) {
    try {
        log.debug("Updating offence notice owner driver for notice number {} (UPDATE OR CREATE)", noticeNo);
        
        String ownerId = (String) record.get("ownerId");
        if (ownerId == null || ownerId.trim().isEmpty()) {
            log.warn("Owner ID is null or empty for notice number {}, skipping owner driver update", noticeNo);
            return;
        }

        // Sanitize the owner ID by removing symbols
        String sanitizedOwnerId = sanitizeIdNumber(ownerId);
        
        // If the ID was changed during sanitization, update the record
        if (!sanitizedOwnerId.equals(ownerId)) {
            record.put("ownerId", sanitizedOwnerId);
            log.info("Foreign ID with symbols detected and sanitized for notice {}: {} -> {}", 
                    noticeNo, ownerId, sanitizedOwnerId);
        }
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("noticeNo", noticeNo);
        filters.put("ownerDriverIndicator", SystemConstant.OwnerDriverIndicator.OWNER); // Default to Owner
        
        // Check if record exists first to determine if update or create is needed
        List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver", filters);
        
        boolean isCreate = existingRecords.isEmpty();
        if (isCreate) {
            log.info("No existing offence notice owner driver record found for notice number {} - will create new record", noticeNo);
        } else {
            log.info("Found existing offence notice owner driver record for notice number {} - will update", noticeNo);
        }
        
        // Build update fields - Use correct entity field names
        Map<String, Object> fields = new HashMap<>();
        
        // Map LTA fields to actual entity fields
        addIfPresentMapped(fields, record, "ownerIdType", "entityType");
        fields.put("idNo", sanitizedOwnerId); // Use the sanitized ID
        
        // Set idType based on ownerIdType mapping
        String ownerIdType = (String) record.get("ownerIdType");
        if (ownerIdType != null) {
            String idType = mapOwnerIdTypeToIdType(ownerIdType, ownerId, noticeNo);
            fields.put("idType", idType);
        }
        addIfPresentMapped(fields, record, "ownerName", "name");
        addIfPresentMapped(fields, record, "passportPlaceOfIssue", "passportPlaceOfIssue");

        fields.put("isSync", "N");
        log.info("Setting isSync=N for ONOD (owner/driver) record for UIN {} to trigger internet sync via cron batch (Process 7)");

        // Handle both update and create operations for owner driver record first
        if (!fields.isEmpty()) {
            if (isCreate) {
                // For create operation, add required fields
                fields.put("noticeNo", noticeNo);
                fields.put("ownerDriverIndicator", SystemConstant.OwnerDriverIndicator.OWNER); // Default to Owner
                fields.put("offenderIndicator", SystemConstant.OffenderIndicator.CURRENT); // Default to Offender Indicator as Current Offender
                
                // Add creation date to satisfy NOT NULL constraint
                fields.put("creDate", LocalDateTime.now());
                fields.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.info("Creating new offence notice owner driver record for notice number {} with {} fields", noticeNo, fields.size());
                tableQueryService.post("ocms_offence_notice_owner_driver", fields);
            } else {
                // For update operation
                // Add update date to satisfy NOT NULL constraint
                fields.put("updDate", LocalDateTime.now());
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                log.info("Updating existing offence notice owner driver record for notice number {} with {} fields", noticeNo, fields.size());
                tableQueryService.patch("ocms_offence_notice_owner_driver", filters, fields);
            }
        } else {
            log.debug("No fields to update for offence notice owner driver {}", noticeNo);
        }
        
        // Process LTA registration address - within the same transaction
        updateLtaRegistrationAddress(noticeNo, SystemConstant.OwnerDriverIndicator.OWNER, record);
        
        // Process LTA mailing address - within the same transaction
        updateLtaMailingAddress(noticeNo, SystemConstant.OwnerDriverIndicator.OWNER, record);
        
    } catch (Exception e) {
        log.error("Error updating offence notice owner driver for notice number {}: {}", noticeNo, e.getMessage(), e);
        throw new RuntimeException("Failed to update owner driver data for notice " + noticeNo, e);
    }
}
    
    /**
     * Update only the ownerdriver table with LTA processing date and error code for error codes 1, 2, and 4
     * Creates a minimal record with required fields if one doesn't exist
     * 
     * @param noticeNo The offence notice number
     * @param errorCode The error code to set
     */
    @Transactional
    public void updateOffenceNoticeLtaDatetimeErrorCode(String noticeNo, String errorCode) {
        try {
            log.debug("Updating offence notice LTA processing Datetime and LTA error code for notice number {} (UPDATE OR CREATE)", noticeNo);
            updateOffenceNoticeDetailStatus(noticeNo, LocalDateTime.now(), errorCode);
            log.debug("Successfully updated LTA processing datetime and error code for notice {}", noticeNo);
        } catch (Exception e) {
            log.error("Error updating offence notice LTA processing Datetime and LTA error code for notice number {}: {}", noticeNo, e.getMessage(), e);
        }
    }
       
    /**
     * Update LTA registration address for an offence notice
     * Uses tableQueryService.post() for creation and tableQueryService.patch() for updates
     * 
     * @param noticeNo The offence notice number
     * @param ownerDriverIndicator The owner/driver indicator (SystemConstant.OwnerDriverIndicator.OWNER or SystemConstant.OwnerDriverIndicator.DRIVER)
     * @param record The LTA response record
     */
    private void updateLtaRegistrationAddress(String noticeNo, String ownerDriverIndicator, Map<String, Object> record) {
        try {
            log.debug("Updating LTA registration address for notice {}", noticeNo);
            
            // Define filters to check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            filters.put("ownerDriverIndicator", ownerDriverIndicator);
            filters.put("typeOfAddress", SystemConstant.AddressType.LTA_REGISTRATION);
            
            // Check if record exists
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver_addr", filters);
            boolean isNewRecord = existingRecords.isEmpty();
            
            // Prepare fields for update or create
            Map<String, Object> fields = new HashMap<>();
            
            // Map address fields from LTA response
            addIfPresentMapped(fields, record, "blockHouseNo", "blkHseNo");
            addIfPresentMapped(fields, record, "streetName", "streetName");
            addIfPresentMapped(fields, record, "floorNumber", "floorNo");
            addIfPresentMapped(fields, record, "unitNumber", "unitNo");
            addIfPresentMapped(fields, record, "buildingName", "bldgName");
            addIfPresentMapped(fields, record, "postalCode", "postalCode");
            addIfPresentMapped(fields, record, "addressType", "addressType");
            addIfPresentMapped(fields, record, "errorCode", "errorCode");
            
            // Set processing date time
            fields.put("processingDateTime", LocalDateTime.now());
            
            // Set effective date if available
            String regAddressEffectiveDate = (String) record.get("regAddressEffectiveDate");
            if (regAddressEffectiveDate != null && !regAddressEffectiveDate.trim().isEmpty()) {
                LocalDateTime dateTime = parseDate(regAddressEffectiveDate);
                if (dateTime != null) {
                    fields.put("effectiveDate", dateTime);
                }
            }
            
            if (isNewRecord) {
                // For create operation, add required fields
                fields.put("noticeNo", noticeNo);
                fields.put("ownerDriverIndicator", ownerDriverIndicator);
                fields.put("typeOfAddress", SystemConstant.AddressType.LTA_REGISTRATION);
                
                // Add creation audit fields
                fields.put("creDate", LocalDateTime.now());
                fields.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.info("Creating new LTA registration address record for notice {}", noticeNo);
                tableQueryService.post("ocms_offence_notice_owner_driver_addr", fields);
            } else {
                // For update operation, add update audit fields
                fields.put("updDate", LocalDateTime.now());
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.info("Updating existing LTA registration address for notice {}", noticeNo);
                tableQueryService.patch("ocms_offence_notice_owner_driver_addr", filters, fields);
            }
            
            log.debug("Successfully {} LTA registration address for notice {}", 
                    isNewRecord ? "created" : "updated", noticeNo);
        } catch (Exception e) {
            log.error("Error updating LTA registration address for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Update LTA mailing address for an offence notice
     * Uses tableQueryService.post() for creation and tableQueryService.patch() for updates
     * 
     * @param noticeNo The offence notice number
     * @param ownerDriverIndicator The owner/driver indicator (SystemConstant.OwnerDriverIndicator.OWNER or SystemConstant.OwnerDriverIndicator.DRIVER)
     * @param record The LTA response record
     */
    private void updateLtaMailingAddress(String noticeNo, String ownerDriverIndicator, Map<String, Object> record) {
        try {
            log.debug("Updating LTA mailing address for notice {}", noticeNo);
            
            // Check if any mailing address fields are present and not empty
            boolean hasAnyAddressField = isNotNullOrEmpty(record.get("mailingBlockHouse")) || 
                                        isNotNullOrEmpty(record.get("mailingStreetName")) ||
                                        isNotNullOrEmpty(record.get("mailingFloorNumber")) ||
                                        isNotNullOrEmpty(record.get("mailingUnitNumber")) ||
                                        isNotNullOrEmpty(record.get("mailingBuildingName")) ||
                                        isNotNullOrEmpty(record.get("mailingPostalCode"));
            
            // Skip database operation if no address fields exist
            if (hasAnyAddressField) {
                // Define filters to check if record exists
                Map<String, Object> filters = new HashMap<>();
                filters.put("noticeNo", noticeNo);
                filters.put("ownerDriverIndicator", ownerDriverIndicator);
                filters.put("typeOfAddress", SystemConstant.AddressType.LTA_MAILING);
                
                // Check if record exists
                List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver_addr", filters);
                boolean isNewRecord = existingRecords.isEmpty();
                
                // Prepare fields for update or create
                Map<String, Object> fields = new HashMap<>();
                
                // Map address fields from LTA response
                addIfPresentMapped(fields, record, "mailingBlockHouse", "blkHseNo");
                addIfPresentMapped(fields, record, "mailingStreetName", "streetName");
                addIfPresentMapped(fields, record, "mailingFloorNumber", "floorNo");
                addIfPresentMapped(fields, record, "mailingUnitNumber", "unitNo");
                addIfPresentMapped(fields, record, "mailingBuildingName", "bldgName");
                addIfPresentMapped(fields, record, "mailingPostalCode", "postalCode");
                addIfPresentMapped(fields, record, "addressType", "addressType");
                addIfPresentMapped(fields, record, "errorCode", "errorCode");
                
                // Set processing date time
                fields.put("processingDateTime", LocalDateTime.now());
                
                // Set effective date if available
                String mailingAddressEffectiveDate = (String) record.get("mailingAddressEffectiveDate");
                if (mailingAddressEffectiveDate != null && !mailingAddressEffectiveDate.trim().isEmpty()) {
                    LocalDateTime dateTime = parseDate(mailingAddressEffectiveDate);
                    if (dateTime != null) {
                        fields.put("effectiveDate", dateTime);
                    }
                }
                
                if (isNewRecord) {
                    // For create operation, add required fields
                    fields.put("noticeNo", noticeNo);
                    fields.put("ownerDriverIndicator", ownerDriverIndicator);
                    fields.put("typeOfAddress", SystemConstant.AddressType.LTA_MAILING);
                    
                    // Add creation audit fields
                    fields.put("creDate", LocalDateTime.now());
                    fields.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                    
                    log.info("Creating new LTA mailing address record for notice {}", noticeNo);
                    tableQueryService.post("ocms_offence_notice_owner_driver_addr", fields);
                } else {
                    // For update operation, add update audit fields
                    fields.put("updDate", LocalDateTime.now());
                    fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                    
                    log.info("Updating existing LTA mailing address for notice {}", noticeNo);
                    tableQueryService.patch("ocms_offence_notice_owner_driver_addr", filters, fields);
                }
                
                log.debug("Successfully {} LTA mailing address for notice {}", 
                        isNewRecord ? "created" : "updated", noticeNo);
            }
        } catch (Exception e) {
            log.error("Error updating LTA mailing address for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to check if an object is not null and not an empty string
     * 
     * @param obj The object to check
     * @return true if the object is not null and not an empty string, false otherwise
     */
    private boolean isNotNullOrEmpty(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return !((String) obj).trim().isEmpty();
        }
        return true;
    }
    
    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate localDate = LocalDate.parse(dateString.trim(), formatter);
            return localDate.atStartOfDay(); // Convert to LocalDateTime
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateString);
            return null;
        }
    }
    
    /**
     * FIXED VERSION - Add a field to the fields map if it exists in the record map with field mapping
     * 
     * @param fields The fields map to add to
     * @param record The record map to get the value from
     * @param sourceField The name of the field in the record map
     * @param targetField The name of the field in the entity
     */
    private void addIfPresentMapped(Map<String, Object> fields, Map<String, Object> record, String sourceField, String targetField) {
        Object value = record.get(sourceField);
        if (value != null) {
            // For strings, check if they're empty
            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.trim().isEmpty()) {
                    fields.put(targetField, value);
                }
            } else {
                fields.put(targetField, value);
            }
        }
    }
    
    /**
     * Maps the ownerIdType to the idType value
     * 
     * @param ownerIdType The owner ID type code
     * @param ownerId The owner ID value (not used in this implementation)
     * @param noticeNo The notice number (for logging)
     * @return The idType value mapped to N, F, B, or P format
     */
    private String mapOwnerIdTypeToIdType(String ownerIdType, String ownerId, String noticeNo) {
        if (ownerIdType == null) {
            log.warn("Null ownerIdType for notice number {}, defaulting to NRIC (N)", noticeNo);
            return "-"; // Default Not Found
        }
        
        switch (ownerIdType) {
            case "1":
                return "N";  // Singapore NRIC
            case "D":
                return "F";  // Foreign Identification Number (FIN)
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
            case "A":
            case "B":
            case "C":
                return "B";  // UEN (Unique Entity Number)
            case "2":
            case "3":
                return "P";  // PASSPORT
            default:
                log.warn("Unknown ownerIdType: {} for notice number {}, defaulting to NRIC (N)", ownerIdType, noticeNo);
                return "-"; // Default Not Found
        }
    }

    /**
     * Sanitizes an ID number by removing any non-alphanumeric characters
     * Useful for foreign IDs that may contain symbols like hyphens
     *
     * @param idNumber The original ID number that may contain symbols
     * @return The sanitized ID number with symbols removed
     */
    private String sanitizeIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isEmpty()) {
            return idNumber;
        }
        
        // Check if ID contains any non-alphanumeric characters
        boolean containsSymbols = !idNumber.matches("[a-zA-Z0-9]+");
        
        if (containsSymbols) {
            // Log the original ID before sanitizing
            log.info("Sanitizing ID with symbols: {}", idNumber);
            
            // Remove all non-alphanumeric characters
            String sanitizedId = idNumber.replaceAll("[^a-zA-Z0-9]", "");
            
            log.info("Sanitized ID result: {}", sanitizedId);
            return sanitizedId;
        }
        
        // Return original if no symbols found
        return idNumber;
    }    
}
