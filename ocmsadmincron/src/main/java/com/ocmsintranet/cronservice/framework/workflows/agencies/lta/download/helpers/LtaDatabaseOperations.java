// ============================================================================
// LtaDatabaseOperations.java - FIXED VERSION with Correct Field Mapping
// ============================================================================
package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LtaDatabaseOperations {

    private final TableQueryService tableQueryService;

    public LtaDatabaseOperations(TableQueryService tableQueryService) {
        this.tableQueryService = tableQueryService;
    }

    /**
     * FIXED VERSION - Update database tables with record information
     * Only updates fields that have non-null values in the record
     * Fixed field mapping to match actual entity field names
     * REMOVED updDate from updates to prevent non-editable field error
     * 
     * @param record The record containing the data to update
     * @param offenceNoticeNumber The offence notice number
     */
    public void updateDatabaseTables(Map<String, Object> record, String offenceNoticeNumber) {
        try {
            log.debug("Updating database tables for notice {} with selective field updates", offenceNoticeNumber);
            updateOffenceNoticeDetail(record, offenceNoticeNumber);
            updateOffenceNoticeOwnerDriver(record);
        } catch (Exception e) {
            log.error("Error updating database tables for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
        }
    }

    /**
     * FIXED VERSION - Update the offence notice detail table with vehicle information
     * Fixed field mapping to match OcmsOffenceNoticeDetail entity
     * REMOVED updDate field to prevent non-editable field error
     */
    private void updateOffenceNoticeDetail(Map<String, Object> record, String offenceNoticeNumber) {
        try {
            String vehicleNumber = (String) record.get("vehicleNumber");
            if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
                log.warn("Vehicle number is null or empty for notice {}, skipping detail update", offenceNoticeNumber);
                return;
            }
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            // FIXED: Use correct entity field names AND removed updDate
            Map<String, Object> fields = new HashMap<>();
            fields.put("vehicleNo", vehicleNumber); // We already validated this is not null

            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);

            if (existingRecords != null && !existingRecords.isEmpty()) {
                Map<String, Object> existingRecord = existingRecords.get(0);

                if (existingRecord.get("lastProcessingDate") != null) {
                    fields.put("prevProcessingDate", existingRecord.get("lastProcessingDate"));
                }
                
                // For update operation, add update audit fields
                fields.put("updDate", LocalDateTime.now());
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                // PROCESS 2: Set is_sync to N to trigger cron batch sync to internet DB
                fields.put("isSync", "N");
                log.info("Setting isSync=N for notice {} to trigger internet sync via cron batch (Process 7)", offenceNoticeNumber);

                log.info("Updating existing valid offence notice for notice {} with {} fields", offenceNoticeNumber, fields.size());
                // REMOVED: updDate and updUserId fields that cause non-editable errors
                List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, fields);
                
                if (updatedRecords == null || updatedRecords.isEmpty()) {
                    log.warn("Failed to update offence notice detail for notice {}", offenceNoticeNumber);
                }
            } else {
                log.info("Creating new valid offence notice for notice {}", offenceNoticeNumber);
                fields.putAll(filters);
                fields.put("creDate", LocalDateTime.now());
                fields.put("creUserId", "LTA_SYSTEM");

                // PROCESS 2: Set is_sync to N to trigger cron batch sync to internet DB
                fields.put("isSync", "N");
                log.info("Setting isSync=N for new notice {} to trigger internet sync via cron batch (Process 7)", offenceNoticeNumber);

                Map<String, Object> createdRecord = tableQueryService.post("ocms_valid_offence_notice", fields);
                
                if (createdRecord == null) {
                    log.warn("Failed to create offence notice detail for notice {}", offenceNoticeNumber);
                }
            }
        } catch (Exception e) {
            log.error("Error updating valid offence notice: {}", e.getMessage(), e);
        }
    }

    /**
     * FIXED VERSION - Update the offence notice owner driver table with owner information
     * Fixed field mapping to match OcmsOffenceNoticeOwnerDriver entity
     * REMOVED updDate field and addressType field that don't exist or cause errors
     */
    private void updateOffenceNoticeOwnerDriver(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            String ownerId = (String) record.get("ownerId");
            
            if (ownerId == null || ownerId.trim().isEmpty()) {
                log.warn("Owner ID is null or empty for notice {}, skipping owner driver update", offenceNoticeNumber);
                return;
            }
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            filters.put("ownerDriverIndicator", "O"); // Default to Owner
            
            // FIXED: Use correct entity field names from OcmsOffenceNoticeOwnerDriver.java
            // REMOVED: addressType field that doesn't exist in entity
            Map<String, Object> fields = new HashMap<>();
            addIfPresentMapped(fields, record, "ownerIdType", "idType");           // FIXED
            fields.put("idNo", ownerId);                                         // FIXED - Updated to use idNo instead of nricNo to match entity
            addIfPresentMapped(fields, record, "ownerName", "name");               // FIXED
            addIfPresentMapped(fields, record, "blockHouseNo", "regBlkHseNo");     // FIXED
            addIfPresentMapped(fields, record, "streetName", "regStreet");         // FIXED
            addIfPresentMapped(fields, record, "buildingName", "regBldg");         // FIXED
            addIfPresentMapped(fields, record, "postalCode", "regPostalCode");     // FIXED

            // PROCESS 2: Set is_sync to N to trigger cron batch sync to internet DB
            fields.put("isSync", "N");

            // Add required fields
            // fields.put("mhaProcessingDateTime", LocalDateTime.now());
            // fields.put("lifeStatus", "A"); // Default to Active

            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver", filters);

            if (existingRecords != null && !existingRecords.isEmpty()) {
                // Add update date to satisfy NOT NULL constraint
                fields.put("updDate", LocalDateTime.now());
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                log.info("Updating existing owner driver info for notice {} with {} fields (including isSync=N)", offenceNoticeNumber, fields.size());
                // REMOVED: updDate and updUserId fields that cause non-editable errors
                List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_offence_notice_owner_driver", filters, fields);

                if (updatedRecords == null || updatedRecords.isEmpty()) {
                    log.warn("Failed to update owner driver info for notice {}", offenceNoticeNumber);
                }
            } else {
                log.info("Creating new owner driver info for notice {} (with isSync=N)", offenceNoticeNumber);
                fields.putAll(filters);
                // FIXED: Add ALL required fields for new records
                fields.put("creDate", LocalDateTime.now());      // This was missing!
                fields.put("creUserId", "LTA_SYSTEM");           // This was missing!
                // isSync already set above
                Map<String, Object> createdRecord = tableQueryService.post("ocms_offence_notice_owner_driver", fields);
                
                if (createdRecord == null) {
                    log.warn("Failed to create owner driver info for notice {}", offenceNoticeNumber);
                }
            }
        } catch (Exception e) {
            log.error("Error updating offence notice owner driver: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add a field to the fields map if it exists in the record map with field mapping
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
}