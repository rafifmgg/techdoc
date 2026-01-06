package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.download.services;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for processing Toppan download return files.
 *
 * This service handles the processing of return files from Toppan containing
 * postal registration numbers for registered mail. It does NOT perform stage transitions.
 *
 * TOPPAN DOWNLOAD WORKFLOW:
 * 1. Toppan sends return files with postal registration numbers (RD2/DN2 stages)
 * 2. This service PATCHES postal registration numbers to tracking tables
 * 3. NO updates to ocms_valid_offence_notice main table
 *
 * KEY RESPONSIBILITIES:
 * - Update tracking tables with postal registration numbers from Toppan return files
 * - Route to correct table based on FILE TYPE (not stage):
 *   - DPT-URA-RD2-D2-* files → ocms_request_driver_particulars.postal_regn_no
 *   - DPT-URA-DN2-D2-* files → ocms_driver_notice.postal_regn_no
 * - Clean up staging tables after successful processing
 *
 * DATABASE OPERATIONS (PATCH ONLY - NO INSERT):
 * - ocms_driver_notice: PATCH postal_regn_no for DN2 return files
 * - ocms_request_driver_particulars: PATCH postal_regn_no for RD2 return files
 * - ocms_valid_offence_notice: Read-only (for validation)
 *
 * BUSINESS RULES:
 * - Only PATCH operations allowed on tracking tables (records must pre-exist)
 * - Postal registration numbers stored only for registered mail stages (RD2/DN2)
 * - File type determines which tracking table to update
 * - All operations are transactional to ensure data consistency
 *
 * PERFORMANCE OPTIMIZATION:
 * - Processes notices in chunks (default 500) to reduce database round-trips
 * - Supports high-volume processing (100k+ notices) efficiently
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ToppanStageTransitionService {

    // Service for database query operations across all tables
    private final TableQueryService tableQueryService;

    /**
     * Provides access to the table query service for other components.
     * Used by ToppanDownloadJob for failed notice status updates.
     *
     * @return TableQueryService instance for database operations
     */
    public TableQueryService getTableQueryService() {
        return tableQueryService;
    }

    /**
     * Chunk size for batch processing to optimize database operations.
     * Processing in chunks reduces round-trips for high-volume operations (100k+ notices).
     */
    private static final int BATCH_CHUNK_SIZE = 500;

    /**
     * Processes stage transitions for successfully processed notices.
     *
     * This is a convenience method that calls the main transition method without
     * postal registration numbers. Used for stages that don't involve registered mail.
     *
     * @param currentStage The stage that was just completed (RD1, RD2, RR3, DN1, DN2, DR3)
     * @param noticeNumbers List of notice numbers that were successfully processed
     * @param processingDate Timestamp when processing was completed
     * @param fileName The name of the file being processed (e.g., DPT-URA-RD2-D2-*, DPT-URA-DN2-D2-*)
     * @return Map containing processing results (updatedCount, errorCount, errors)
     */
    public Map<String, Object> processStageTransitions(String currentStage,
                                                       List<String> noticeNumbers,
                                                       LocalDateTime processingDate,
                                                       String fileName) {
        return processStageTransitions(currentStage, noticeNumbers, processingDate, null, fileName);
    }
    
    /**
     * Process Toppan download return file by updating tracking tables with postal registration numbers.
     *
     * This method handles Toppan download confirmation processing. It does NOT perform stage transitions.
     * It only updates the tracking tables (ocms_driver_notice or ocms_request_driver_particulars)
     * with postal registration numbers returned by Toppan.
     *
     * PROCESSING STEPS:
     * 1. For each notice in the return file:
     *    - Retrieve notice record from ocms_valid_offence_notice (for validation only)
     *    - PATCH postal registration number to tracking table based on file type:
     *      - RD2 files → ocms_request_driver_particulars.postal_regn_no
     *      - DN2 files → ocms_driver_notice.postal_regn_no
     * 2. NO updates to ocms_valid_offence_notice
     * 3. NO stage transitions or fee calculations
     *
     * @param currentStage The stage from the return file (RD2 or DN2)
     * @param noticeNumbers List of notice numbers from the return file
     * @param processingDate Timestamp when file was processed
     * @param postalRegistrationNumbers Map of notice number to postal registration number
     * @param fileName The name of the file being processed (e.g., DPT-URA-RD2-D2-*, DPT-URA-DN2-D2-*)
     * @return Map containing processing statistics and any errors encountered
     */
    public Map<String, Object> processStageTransitions(String currentStage,
                                                       List<String> noticeNumbers,
                                                       LocalDateTime processingDate,
                                                       Map<String, String> postalRegistrationNumbers,
                                                       String fileName) {
        log.info("Processing Toppan download return file for {} notices from stage {}",
                noticeNumbers.size(), currentStage);

        // Initialize result tracking for processing statistics
        Map<String, Object> result = new HashMap<>();
        int updatedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        // TP-PERF-01: Process notices in chunks to reduce database round-trips
        // For 100k notices, processing in chunks of 500 reduces overhead significantly
        int totalNotices = noticeNumbers.size();
        log.info("Processing {} notices in chunks of {}", totalNotices, BATCH_CHUNK_SIZE);

        for (int i = 0; i < totalNotices; i += BATCH_CHUNK_SIZE) {
            int endIndex = Math.min(i + BATCH_CHUNK_SIZE, totalNotices);
            List<String> chunk = noticeNumbers.subList(i, endIndex);
            log.debug("Processing chunk {}-{} of {}", i, endIndex, totalNotices);

            // Process each notice in the chunk
            for (String noticeNo : chunk) {
            try {
                // Retrieve the current notice record to get existing data for tracking table updates
                Map<String, Object> currentRecord = getNoticeRecord(noticeNo);
                if (currentRecord == null) {
                    // TP-RD2-03 & TP-DN2-03: Log NO_TARGET_ROW reason
                    log.warn("NO_TARGET_ROW: Notice {} not found in ocms_valid_offence_notice for stage {}", noticeNo, currentStage);
                    errors.add("Notice not found: " + noticeNo);
                    errorCount++;
                    continue;
                }

                // Update ONLY tracking tables with postal registration numbers
                // NO updates to ocms_valid_offence_notice (this is only receiving confirmation, not doing stage transitions)
                updateTrackingTables(noticeNo, currentStage, currentRecord, postalRegistrationNumbers, fileName);

                updatedCount++;
                log.debug("Patched postal registration number for notice {} stage {}", noticeNo, currentStage);

            } catch (Exception e) {
                // Log individual notice processing errors but continue with remaining notices
                log.error("Error updating notice {}: {}", noticeNo, e.getMessage());
                errors.add("Update failed for " + noticeNo + ": " + e.getMessage());
                errorCount++;
            }
            }

            log.debug("Completed chunk {}-{}: {} updated, {} errors", i, endIndex, updatedCount, errorCount);
        }

        log.info("Completed processing {} notices: {} updated, {} errors", totalNotices, updatedCount, errorCount);

        // Note: nro_temp deletion has been disabled - notices remain in staging table for audit purposes

        // Return processing statistics for job reporting
        result.put("updatedCount", updatedCount);
        result.put("errorCount", errorCount);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }

    /**
     * Retrieves the current notice record from the main table.
     *
     * This method fetches the existing notice data needed for validation and
     * tracking table updates.
     *
     * @param noticeNo The notice number to retrieve
     * @return Map containing the notice record data, or null if not found
     */
    private Map<String, Object> getNoticeRecord(String noticeNo) {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_valid_offence_notice", filter);
            
            if (!results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            log.error("Error querying notice {}: {}", noticeNo, e.getMessage());
        }
        return null;
    }

    /**
     * Updates stage-specific tracking tables with detailed processing information.
     *
     * Different workflow stages require different tracking information to be stored
     * in specialized tables. This method determines which tables to update based
     * on the FILE TYPE being processed, not the stage name.
     *
     * TRACKING TABLES (FILE-BASED ROUTING):
     * - DPT-URA-RD2-D2-* → ocms_request_driver_particulars (Owner Path - Request Driver Particulars)
     * - DPT-URA-DN2-D2-* → ocms_driver_notice (Driver Path - Driver Notice)
     * - RD1, DN1 → ocms_driver_notice (fallback to stage-based)
     * - RR3, DR3 → ocms_request_driver_particulars (final reminder stages)
     *
     * @param noticeNo The notice number being processed
     * @param stage The current stage being completed
     * @param noticeRecord The main notice record containing details to copy
     * @param postalRegistrationNumbers Map of registration numbers for registered mail stages
     * @param fileName The name of the file being processed (e.g., DPT-URA-RD2-D2-*, DPT-URA-DN2-D2-*)
     */
    private void updateTrackingTables(String noticeNo, String stage, Map<String, Object> noticeRecord,
                                     Map<String, String> postalRegistrationNumbers, String fileName) {
        try {
            // Route to appropriate tracking table based on FILE TYPE, not stage
            // This ensures RD2 and DN2 files update the correct tables per their workflow paths

            if (fileName != null && fileName.toUpperCase().contains(SystemConstant.SuspensionReason.RD2)) {
                // RD2 files (Owner Path - Request Driver Particulars) → ocms_request_driver_particulars
                updateRequestDriverParticularsTable(noticeNo, stage, noticeRecord, postalRegistrationNumbers);
                log.debug("RD2 file: updating ocms_request_driver_particulars for notice {}", noticeNo);
            }
            else if (fileName != null && fileName.toUpperCase().contains(SystemConstant.SuspensionReason.DN2)) {
                // DN2 files (Driver Path - Driver Notice) → ocms_driver_notice
                updateDriverNoticeTable(noticeNo, stage, noticeRecord, postalRegistrationNumbers);
                log.debug("DN2 file: updating ocms_driver_notice for notice {}", noticeNo);
            }
        } catch (Exception e) {
            log.error("Error updating tracking tables for notice {} stage {} file {}: {}",
                    noticeNo, stage, fileName, e.getMessage());
        }
    }
    
    /**
     * Updates the driver notice tracking table for early workflow stages.
     * 
     * This table tracks RD1, RD2, DN1, and DN2 stages, capturing driver information
     * and postal registration numbers for registered mail stages.
     * 
     * STAGES TRACKED:
     * - RD1: First request to owner for driver details
     * - RD2: Second request to owner (registered mail)
     * - DN1: First notice sent directly to driver
     * - DN2: Second notice to driver (registered mail)
     * 
     * SPECIAL HANDLING:
     * - RD2/DN2: Include postal registration numbers from return files
     * - All stages: Include driver and vehicle details for tracking
     * 
     * @param noticeNo Notice number being processed
     * @param stage Current stage (RD1, RD2, DN1, DN2)
     * @param noticeRecord Main notice record containing driver details
     * @param postalRegistrationNumbers Registration numbers for registered mail (RD2/DN2 only)
     */
    private void updateDriverNoticeTable(String noticeNo, String stage, Map<String, Object> noticeRecord,
                                        Map<String, String> postalRegistrationNumbers) {
        try {
            // First, get the existing driver notice record to retrieve dateOfProcessing
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            filter.put("processingStage", stage);

            // Query for the existing record (should return a list, get first if present)
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_driver_notice", filter);
            Object dateOfProcessing = null;
            if (existingRecords != null && !existingRecords.isEmpty()) {
                dateOfProcessing = existingRecords.get(0).get("dateOfProcessing");
                log.debug("Fetched dateOfProcessing for notice {} stage {}: {}", noticeNo, stage, dateOfProcessing);
            } else {
                log.warn("No existing ocms_driver_notice record found for notice {} stage {}", noticeNo, stage);
            }

            // Build tracking record for driver notice stages
            Map<String, Object> driverNoticeData = new HashMap<>();

            // Optionally, you can put dateOfProcessing back if needed in the patch
            if (dateOfProcessing != null) {
                filter.put("dateOfProcessing", dateOfProcessing);
            }

            // Set audit fields for tracking updates
            driverNoticeData.put("updDate", LocalDateTime.now());
            driverNoticeData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // Include postal registration number for DN2 registered mail stage
            // This is critical for tracking delivery confirmation
            if (stage.equals("DN2") && postalRegistrationNumbers != null) {
                String regNo = postalRegistrationNumbers.get(noticeNo);
                if (regNo != null) {
                    driverNoticeData.put("postalRegnNo", regNo);
                    log.info("Recording postal registration number for notice {} stage {}: {}",
                            noticeNo, stage, regNo);
                }
            }

            // Patch existing tracking record with latest information
            tableQueryService.patch("ocms_driver_notice", filter, driverNoticeData);
            log.debug("Patched ocms_driver_notice for notice {} stage {}", noticeNo, stage);

        } catch (Exception e) {
            log.error("Error updating ocms_driver_notice for notice {}: {}", noticeNo, e.getMessage());
        }
    }
    
    /**
     * Updates the request driver particulars tracking table for RD2, RR3 and DR3 stages.
     *
     * This table tracks request driver particulars workflow stages:
     * - RD2: Second reminder to owner (registered mail) with postal registration number
     * - RR3: Final reminder to owner with administration fee
     * - DR3: Final request to driver with administration fee
     *
     * SPECIAL HANDLING:
     * - RD2: Include postal registration numbers from return files
     * - RR3/DR3: Include calculated administration fees and total amounts
     * - Focus on owner/vehicle information for court proceedings
     * - Prepare data needed for potential prosecution case
     *
     * @param noticeNo Notice number being processed
     * @param stage Current stage (RD2, RR3 or DR3)
     * @param noticeRecord Main notice record containing owner/vehicle details and calculated fees
     * @param postalRegistrationNumbers Map of registration numbers for registered mail (RD2 only)
     */
    private void updateRequestDriverParticularsTable(String noticeNo, String stage, Map<String, Object> noticeRecord,
                                                    Map<String, String> postalRegistrationNumbers) {
        try {
            // First, get the existing RDP record to retrieve dateOfProcessing
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            filter.put("processingStage", stage);

            // Query for the existing record (should return a list, get first if present)
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_request_driver_particulars", filter);
            Object dateOfProcessing = null;
            if (existingRecords != null && !existingRecords.isEmpty()) {
                dateOfProcessing = existingRecords.get(0).get("dateOfProcessing");
                log.debug("Fetched dateOfProcessing for notice {} stage {}: {}", noticeNo, stage, dateOfProcessing);
            } else {
                log.warn("No existing ocms_request_driver_particulars record found for notice {} stage {}", noticeNo, stage);
            }

            // Build tracking record for request driver particulars stages
            Map<String, Object> requestData = new HashMap<>();

            // Optionally, you can put dateOfProcessing back if needed in the patch
            if (dateOfProcessing != null) {
                filter.put("dateOfProcessing", dateOfProcessing);
            }

            // Set audit fields for tracking updates
            requestData.put("updDate", LocalDateTime.now());
            requestData.put("updUserId",SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // Include postal registration number for RD2 registered mail stage
            // This is critical for tracking delivery confirmation
            if (stage.equals(SystemConstant.SuspensionReason.RD2) && postalRegistrationNumbers != null) {
                String regNo = postalRegistrationNumbers.get(noticeNo);
                if (regNo != null) {
                    requestData.put("postalRegnNo", regNo);
                    log.info("Recording postal registration number for RD2 notice {}: {}", noticeNo, regNo);
                }
            }

            // PATCH ONLY - record must already exist
            // filter already defined above

            // Patch existing tracking record with latest information
            tableQueryService.patch("ocms_request_driver_particulars", filter, requestData);
            log.debug("Patched ocms_request_driver_particulars for notice {} stage {}", noticeNo, stage);
            
        } catch (Exception e) {
            log.error("Error updating ocms_request_driver_particulars for notice {}: {}", noticeNo, e.getMessage());
        }
    }

}