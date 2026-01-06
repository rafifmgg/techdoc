// ============================================================================
// LtaProcessingStageManager.java - Processing Stage Updates
// ============================================================================
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

@Slf4j
@Component
public class LtaProcessingStageManager {

    private final TableQueryService tableQueryService;
    private final com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient;

    public LtaProcessingStageManager(
            TableQueryService tableQueryService,
            com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.suspensionApiClient = suspensionApiClient;
    }
    
    

    /**
     * Apply TS-ROV status to a record with error code 1-4
     * This method implements the complete TS-ROV trigger logic:
     * 1. Updates VON with basic status fields
     * 2. Queries suspension reason table for noOfDaysForRevival
     * 3. Calculates revival date based on current date + noOfDaysForRevival
     * 4. Creates a record in Suspended Notices table
     * 5. Updates VON (Valid Offence Notice) table with revival date and other fields
     *
     * @param record The record containing offence notice information
     */
    @Transactional
    public void applyTsRovStatus(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            String vehicleNumber = (String) record.get("vehicleNumber");
            
            log.info("Applying TS-ROV status for vehicle {} with notice {}", vehicleNumber, offenceNoticeNumber);
            
            // Step 1: Update basic status fields in VON
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);
            if (existingRecords == null || existingRecords.isEmpty()) {
                log.warn("No existing record found for notice {} when applying TS-ROV", offenceNoticeNumber);
                return;
            }
            
            Map<String, Object> existingRecord = existingRecords.get(0);
            Object oldLastProcessingDate = existingRecord.get("lastProcessingDate");
            String existingLastProcessingStage = (String) existingRecord.get("lastProcessingStage");

            LocalDateTime currentDate = LocalDateTime.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Will update VON table after calculating revival date

            // Step 2: Query suspension reason table to get noOfDaysForRevival
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
            suspReasonFilters.put("reasonOfSuspension", SystemConstant.SuspensionReason.ROV);
            
            List<Map<String, Object>> suspReasonResults = tableQueryService.query("ocms_suspension_reason", suspReasonFilters);
            
            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("No suspension reason found for TS-ROV when applying suspension for notice {}", offenceNoticeNumber);
                return;
            }
            
            // Get the noOfDaysForRevival value
            Object noOfDaysObj = suspReasonResults.get(0).get("noOfDaysForRevival");
            if (noOfDaysObj == null) {
                log.error("noOfDaysForRevival is null for TS-ROV suspension reason - cannot apply suspension for notice {}", offenceNoticeNumber);
                return;
            }
            
            int noOfDaysForRevival;
            try {
                if (noOfDaysObj instanceof Number) {
                    noOfDaysForRevival = ((Number) noOfDaysObj).intValue();
                } else {
                    noOfDaysForRevival = Integer.parseInt(noOfDaysObj.toString().trim());
                }
            } catch (NumberFormatException e) {
                log.error("Invalid noOfDaysForRevival value: '{}' - cannot apply suspension for notice {}", noOfDaysObj, offenceNoticeNumber);
                return;
            }
            
            // Step 3: Calculate revival date
            LocalDateTime revivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = revivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            log.info("Calculated revival date {} (current date + {} days) for notice {}",
                    formattedRevivalDate, noOfDaysForRevival, offenceNoticeNumber);

            // Step 4: Apply suspension via API (replaces direct DB suspended_notice creation)
            try {
                log.debug("Applying TS-ROV suspension via API for notice {}", offenceNoticeNumber);

                // Call suspension API
                Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                    offenceNoticeNumber,
                    SystemConstant.SuspensionType.TEMPORARY, // TS
                    SystemConstant.SuspensionReason.ROV,
                    "ROV check", // suspensionRemarks
                    SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                    SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                    null, // caseNo
                    noOfDaysForRevival // daysToRevive
                );

                // Check API response
                if (suspensionApiClient.isSuccess(apiResponse)) {
                    log.info("Successfully applied TS-ROV suspension via API for notice {}", offenceNoticeNumber);
                } else {
                    String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                    log.error("API returned error when applying TS-ROV suspension for notice {}: {}",
                            offenceNoticeNumber, errorMsg);
                    // Don't throw - log and continue with VON update
                }
            } catch (Exception e) {
                log.error("Error calling suspension API for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
                // Don't throw - log and continue with VON update
            }
            
            // Step 5: Update VON (Valid Offence Notice) table with all fields
            try {
                log.debug("Updating VON for TS-ROV suspension for notice {}", offenceNoticeNumber);

                Map<String, Object> vonUpdateFields = new HashMap<>();

                // FIXED: Only update lastProcessingDate if lastProcessingStage is actually changing
                boolean isStageChanging = !SystemConstant.SuspensionReason.ROV.equals(existingLastProcessingStage);

                if (isStageChanging) {
                    log.info("Last processing stage is changing from '{}' to 'ROV' - updating lastProcessingDate", existingLastProcessingStage);
                    // Basic status fields - stage is changing
                    vonUpdateFields.put("prevProcessingStage", existingLastProcessingStage != null ? existingLastProcessingStage : SystemConstant.SuspensionReason.NPA);
                    vonUpdateFields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                    vonUpdateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                    vonUpdateFields.put("lastProcessingDate", formattedDate);
                } else {
                    log.info("Last processing stage is already 'ROV' - NOT updating lastProcessingDate (keeping existing date)");
                    // Stage is already ROV - don't update lastProcessingDate
                    vonUpdateFields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
                    vonUpdateFields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                    vonUpdateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                    // Don't add lastProcessingDate - keep existing value
                }

                vonUpdateFields.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV); // Set to ROV for revival
                vonUpdateFields.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
                vonUpdateFields.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.ROV);
                vonUpdateFields.put("eprDateOfSuspension", formattedDate);

                // Revival date fields
                vonUpdateFields.put("dueDateOfRevival", formattedRevivalDate);

                // For update operation, add update audit fields
                vonUpdateFields.put("updDate", LocalDateTime.now());
                vonUpdateFields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, vonUpdateFields);
                
                if (updatedRecords != null && !updatedRecords.isEmpty()) {
                    log.info("Successfully updated VON with all fields for notice {}", offenceNoticeNumber);
                } else {
                    log.warn("Failed to update VON for notice {}", offenceNoticeNumber);
                }
            } catch (Exception e) {
                log.error("Error updating VON for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
                throw new RuntimeException("Failed to update VON", e);
            }
            
            log.info("Successfully applied complete TS-ROV trigger logic for notice {}", offenceNoticeNumber);
            
        } catch (Exception e) {
            log.error("Error applying TS-ROV status: {}", e.getMessage(), e);
        }
    }

    /**
     * Apply TS-SYS status for DataHive system errors
     * This method implements the complete TS-SYS trigger logic:
     * 1. Updates VON with basic status fields
     * 2. Queries suspension reason table for noOfDaysForRevival
     * 3. Calculates revival date based on current date + noOfDaysForRevival
     * 4. Creates a record in Suspended Notices table
     * 5. Updates VON (Valid Offence Notice) table with revival date and other fields
     *
     * @param record The record containing offence notice information
     */
    @Transactional
    public void applyTsSysStatus(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            String vehicleNumber = (String) record.get("vehicleNumber");

            log.info("Applying TS-SYS status for vehicle {} with notice {}", vehicleNumber, offenceNoticeNumber);

            // Step 1: Update basic status fields in VON
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);

            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);
            if (existingRecords == null || existingRecords.isEmpty()) {
                log.warn("No existing record found for notice {} when applying TS-SYS", offenceNoticeNumber);
                return;
            }

            Map<String, Object> existingRecord = existingRecords.get(0);
            Object oldLastProcessingDate = existingRecord.get("lastProcessingDate");
            String existingLastProcessingStage = (String) existingRecord.get("lastProcessingStage");

            LocalDateTime currentDate = LocalDateTime.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Will update VON table after calculating revival date

            // Step 2: Query suspension reason table to get noOfDaysForRevival
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
            suspReasonFilters.put("reasonOfSuspension", SystemConstant.SuspensionReason.SYS);

            List<Map<String, Object>> suspReasonResults = tableQueryService.query("ocms_suspension_reason", suspReasonFilters);

            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("No suspension reason found for TS-SYS when applying suspension for notice {}", offenceNoticeNumber);
                return;
            }

            // Get the noOfDaysForRevival value
            Object noOfDaysObj = suspReasonResults.get(0).get("noOfDaysForRevival");
            if (noOfDaysObj == null) {
                log.error("noOfDaysForRevival is null for TS-SYS suspension reason - cannot apply suspension for notice {}", offenceNoticeNumber);
                return;
            }

            int noOfDaysForRevival;
            try {
                if (noOfDaysObj instanceof Number) {
                    noOfDaysForRevival = ((Number) noOfDaysObj).intValue();
                } else {
                    noOfDaysForRevival = Integer.parseInt(noOfDaysObj.toString().trim());
                }
            } catch (NumberFormatException e) {
                log.error("Invalid noOfDaysForRevival value: '{}' - cannot apply suspension for notice {}", noOfDaysObj, offenceNoticeNumber);
                return;
            }

            // Step 3: Calculate revival date
            LocalDateTime revivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = revivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            log.info("Calculated revival date {} (current date + {} days) for notice {}",
                    formattedRevivalDate, noOfDaysForRevival, offenceNoticeNumber);

            // Step 4: Apply suspension via API (replaces direct DB suspended_notice creation)
            try {
                log.debug("Applying TS-SYS suspension via API for notice {}", offenceNoticeNumber);

                // Call suspension API
                Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                    offenceNoticeNumber,
                    SystemConstant.SuspensionType.TEMPORARY, // TS
                    SystemConstant.SuspensionReason.SYS,
                    "DataHive system error - automatic retry pending", // suspensionRemarks
                    SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                    SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                    null, // caseNo
                    noOfDaysForRevival // daysToRevive
                );

                // Check API response
                if (suspensionApiClient.isSuccess(apiResponse)) {
                    log.info("Successfully applied TS-SYS suspension via API for notice {}", offenceNoticeNumber);
                } else {
                    String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                    log.error("API returned error when applying TS-SYS suspension for notice {}: {}",
                            offenceNoticeNumber, errorMsg);
                    // Don't throw - log and continue with VON update
                }
            } catch (Exception e) {
                log.error("Error calling suspension API for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
                // Don't throw - log and continue with VON update
            }

            // Step 5: Update VON (Valid Offence Notice) table with all fields
            try {
                log.debug("Updating VON for TS-SYS suspension for notice {}", offenceNoticeNumber);

                Map<String, Object> vonUpdateFields = new HashMap<>();

                // FIXED: Only update lastProcessingDate if lastProcessingStage is actually changing
                boolean isStageChanging = !SystemConstant.SuspensionReason.ROV.equals(existingLastProcessingStage);

                if (isStageChanging) {
                    log.info("Last processing stage is changing from '{}' to 'ROV' - updating lastProcessingDate", existingLastProcessingStage);
                    // Basic status fields - stage is changing
                    vonUpdateFields.put("prevProcessingStage", existingLastProcessingStage != null ? existingLastProcessingStage : SystemConstant.SuspensionReason.NPA);
                    vonUpdateFields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                    vonUpdateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                    vonUpdateFields.put("lastProcessingDate", formattedDate);
                } else {
                    log.info("Last processing stage is already 'ROV' - NOT updating lastProcessingDate (keeping existing date)");
                    // Stage is already ROV - don't update lastProcessingDate
                    vonUpdateFields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
                    vonUpdateFields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                    vonUpdateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                    // Don't add lastProcessingDate - keep existing value
                }

                vonUpdateFields.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV); // Set to ROV for revival
                vonUpdateFields.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
                vonUpdateFields.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.SYS);
                vonUpdateFields.put("eprDateOfSuspension", formattedDate);

                // Revival date fields
                vonUpdateFields.put("dueDateOfRevival", formattedRevivalDate);

                // For update operation, add update audit fields
                vonUpdateFields.put("updDate", LocalDateTime.now());
                vonUpdateFields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, vonUpdateFields);

                if (updatedRecords != null && !updatedRecords.isEmpty()) {
                    log.info("Successfully updated VON with all fields for notice {}", offenceNoticeNumber);
                } else {
                    log.warn("Failed to update VON for notice {}", offenceNoticeNumber);
                }
            } catch (Exception e) {
                log.error("Error updating VON for notice {}: {}", offenceNoticeNumber, e.getMessage(), e);
                throw new RuntimeException("Failed to update VON", e);
            }

            log.info("Successfully applied complete TS-SYS trigger logic for notice {}", offenceNoticeNumber);

        } catch (Exception e) {
            log.error("Error applying TS-SYS status: {}", e.getMessage(), e);
        }
    }

    /**
     * CORRECTED VERSION - Update processing stage to ENA
     * Now queries parameter table with CORRECT field mapping for ENA stage duration
     */
    public void updateProcessingStageToENA(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            
            // Query parameter table for ENA stage duration - CORRECTED FIELD MAPPING
            Map<String, Object> paramFilters = new HashMap<>();
            paramFilters.put("code", "ENA");              // ✅ CORRECT: code = 'ENA'
            paramFilters.put("parameterId", "STAGEDAYS"); // ✅ CORRECT: parameterId = 'STAGEDAYS'
            
            List<Map<String, Object>> paramResults = tableQueryService.query("ocms_parameter", paramFilters);
            
            if (paramResults == null || paramResults.isEmpty()) {
                log.error("Parameter not found: ENA STAGEDAYS - cannot update processing stage to ENA for notice {}", offenceNoticeNumber);
                return;
            }
            
            // Get the stage duration value from parameter table
            String stageDaysValue = (String) paramResults.get(0).get("value");
            if (stageDaysValue == null || stageDaysValue.trim().isEmpty()) {
                log.error("Parameter value is null or empty for ENA STAGEDAYS - cannot update processing stage to ENA for notice {}", offenceNoticeNumber);
                return;
            }
            
            int stageDays;
            try {
                stageDays = Integer.parseInt(stageDaysValue.trim());
            } catch (NumberFormatException e) {
                log.error("Invalid parameter value for ENA STAGEDAYS: '{}' - cannot update processing stage to ENA for notice {}", stageDaysValue, offenceNoticeNumber);
                return;
            }
            
            log.info("Retrieved ENA stage duration from parameter table: {} days for notice {}", stageDays, offenceNoticeNumber);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            LocalDateTime currentDate = LocalDateTime.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime nextProcessingDate = currentDate.plusDays(stageDays); // Using parameter value instead of hardcoded 7
            String nextProcessingDateFormatted = nextProcessingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_valid_offence_notice", filters);
            if (results == null || results.isEmpty()) {
                log.error("No record found for notice {} when updating to ENA", offenceNoticeNumber);
                return;
            }
            
            Map<String, Object> currentRecord = results.get(0);
            Object oldLastProcessingDate = currentRecord.get("lastProcessingDate");
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("prevProcessingStage", SystemConstant.SuspensionReason.ROV);
            fields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
            fields.put("lastProcessingStage", SystemConstant.SuspensionReason.ENA);
            fields.put("lastProcessingDate", formattedDate);
            fields.put("nextProcessingStage", SystemConstant.SuspensionReason.RD1);
            fields.put("nextProcessingDate", nextProcessingDateFormatted);

            // For update operation, add update audit fields
            fields.put("updDate", LocalDateTime.now());
            fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, fields);
            
            if (updatedRecords != null && !updatedRecords.isEmpty()) {
                log.info("Updated processing stage to ENA for notice {} with {} days duration from parameter table", offenceNoticeNumber, stageDays);
            } else {
                log.warn("Failed to update processing stage to ENA for notice {}", offenceNoticeNumber);
            }
            
        } catch (Exception e) {
            log.error("Error updating processing stage to ENA: {}", e.getMessage(), e);
        }
    }

    /**
     * FIXED VERSION - Update processing stage to RD1
     * FIXED: Only update lastProcessingDate if lastProcessingStage is actually changing
     */
    public void updateProcessingStageToRD1(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");

            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);

            LocalDateTime currentDate = LocalDateTime.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime nextDay = currentDate.plusDays(1);
            String nextDayFormatted = nextDay.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);
            if (existingRecords == null || existingRecords.isEmpty()) {
                log.warn("No existing record found for notice {} when updating to RD1", offenceNoticeNumber);
                return;
            }

            Map<String, Object> existingRecord = existingRecords.get(0);
            Object oldLastProcessingDate = existingRecord.get("lastProcessingDate");
            String existingLastProcessingStage = (String) existingRecord.get("lastProcessingStage");

            Map<String, Object> fields = new HashMap<>();

            // FIXED: Only update lastProcessingDate if lastProcessingStage is actually changing
            boolean isStageChanging = !SystemConstant.SuspensionReason.ROV.equals(existingLastProcessingStage);

            if (isStageChanging) {
                log.info("Last processing stage is changing from '{}' to 'ROV' - updating lastProcessingDate", existingLastProcessingStage);
                // Stage is changing
                fields.put("prevProcessingStage", existingLastProcessingStage != null ? existingLastProcessingStage : SystemConstant.SuspensionReason.NPA);
                fields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                fields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                fields.put("lastProcessingDate", formattedDate);
            } else {
                log.info("Last processing stage is already 'ROV' - NOT updating lastProcessingDate (keeping existing date)");
                // Stage is already ROV - don't update lastProcessingDate
                fields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
                fields.put("prevProcessingDate", oldLastProcessingDate != null ? oldLastProcessingDate : formattedDate);
                fields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);
                // Don't add lastProcessingDate - keep existing value
            }

            fields.put("nextProcessingStage", SystemConstant.SuspensionReason.RD1);
            fields.put("nextProcessingDate", nextDayFormatted);

            // For update operation, add update audit fields
            fields.put("updDate", LocalDateTime.now());
            fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, fields);

            if (updatedRecords != null && !updatedRecords.isEmpty()) {
                log.info("Updated processing stage to RD1 for notice {}", offenceNoticeNumber);
            } else {
                log.warn("Failed to update processing stage to RD1 for notice {}", offenceNoticeNumber);
            }

        } catch (Exception e) {
            log.error("Error updating processing stage to RD1: {}", e.getMessage(), e);
        }
    }
    
}
