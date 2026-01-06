package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.services.DataHiveUenFinService;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models.OffenceNoticeRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class containing main flow stage functions for DataHive UEN & FIN process
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataHiveUenFinHelper {

    private final DataHiveUenFinService dataHiveUenFinService;

    // Store records for common dataset processing
    private List<OffenceNoticeRecord> currentRecords;

    // Store UEN status mapping (noticeNo -> "REGISTERED" or "DEREGISTERED") for statistics
    private Map<String, String> uenStatusMap = new java.util.HashMap<>();

    /**
     * Execute the complete DataHive UEN & FIN synchronization flow
     *
     * @return true if the process completed successfully, false otherwise
     */
    public boolean executeMainFlow() {
        log.info("Starting DataHive UEN & FIN main flow execution");

        try {
            // Step 1: Query DataHive for FIN & UEN
            log.info("Step 1: Querying DataHive for FIN & UEN records");
            List<OffenceNoticeRecord> records = queryDataHiveForNotices();

            // Step 2: Check if any records exist
            if (records == null || records.isEmpty()) {
                log.info("No records found for processing. Stopping cron.");
                return stopCronProcess();
            }

            log.info("Found {} records for processing", records.size());

            // Store records for common dataset processing
            this.currentRecords = records;

            // Step 3: Process each record based on ID type
            boolean overallSuccess = true;
            for (OffenceNoticeRecord record : records) {
                boolean recordSuccess = processRecordByIdType(record);
                if (!recordSuccess) {
                    overallSuccess = false;
                }
            }

            // Step 4: Process common dataset (pass all records)
            boolean commonDatasetSuccess = processCommonDataset(records);
            if (!commonDatasetSuccess) {
                overallSuccess = false;
            }

            // Step 5: Check for interface errors and finalize
            boolean finalizeSuccess = finalizeProcess(overallSuccess);

            log.info("DataHive UEN & FIN main flow completed with status: {}", finalizeSuccess);
            return finalizeSuccess;

        } catch (Exception e) {
            log.error("Error during DataHive UEN & FIN main flow execution", e);
            logError("Main flow execution failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Step 1: Query DataHive for notices that need FIN & UEN processing
     *
     * @return List of offence notice records requiring processing
     */
    public List<OffenceNoticeRecord> queryDataHiveForNotices() {
        log.info("Executing query to get list of notices needing DataHive FIN & UEN processing");
        try {
            return dataHiveUenFinService.getNoticesForDataHiveProcessing();
        } catch (Exception e) {
            log.error("Failed to query notices for DataHive processing", e);
            return null;
        }
    }

    /**
     * Step 2: Stop cron process when no records are found
     *
     * @return true indicating successful stop
     */
    public boolean stopCronProcess() {
        log.info("Stopping cron process - no records to process");
        return true;
    }

    /**
     * Step 3: Process record based on ID type (UEN or FIN)
     *
     * @param record the offence notice record to process
     * @return true if processing was successful
     */
    public boolean processRecordByIdType(OffenceNoticeRecord record) {
        log.info("Processing record with ID: {} and type: {}", record.getIdNo(), record.getIdType());

        if ("B".equals(record.getIdType())) {
            // UEN processing
            return processUenData(record);
        } else if ("F".equals(record.getIdType())) {
            // FIN processing
            return processFinData(record);
        } else {
            log.warn("Unknown ID type: {} for record: {}", record.getIdType(), record.getIdNo());
            return false;
        }
    }

    /**
     * Process UEN data for a specific record
     *
     * @param record the record to process
     * @return true if successful
     */
    public boolean processUenData(OffenceNoticeRecord record) {
        log.info("Processing UEN data for record: {}", record.getIdNo());
        try {
            return dataHiveUenFinService.processUenRecord(record);
        } catch (Exception e) {
            log.error("Error processing UEN data for record: " + record.getIdNo(), e);
            return false;
        }
    }

    /**
     * Process FIN data for a specific record
     *
     * @param record the record to process
     * @return true if successful
     */
    public boolean processFinData(OffenceNoticeRecord record) {
        log.info("Processing FIN data for record: {}", record.getIdNo());
        try {
            return dataHiveUenFinService.processFinRecord(record);
        } catch (Exception e) {
            log.error("Error processing FIN data for record: " + record.getIdNo(), e);
            return false;
        }
    }

    /**
     * Step 6: Process common dataset from DataHive
     * Retrieves prison/custody data for all FIN and NRIC records
     *
     * @param records List of all records to process
     * @return true if common dataset processing was successful
     */
    public boolean processCommonDataset(List<OffenceNoticeRecord> records) {
        log.info("Processing common dataset from DataHive for {} records", records.size());
        try {
            // Retrieve common dataset (prison/custody data) for all records
            // DataHiveCommonService automatically handles storage
            boolean dataRetrieved = dataHiveUenFinService.retrieveCommonDatasetFromDataHive(records);

            if (!dataRetrieved) {
                logError("Failed to retrieve common dataset from DataHive");
                return false;
            }

            log.info("Common dataset processing completed successfully");
            return true;
        } catch (Exception e) {
            log.error("Error processing common dataset", e);
            logError("Common dataset processing failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Step 7: Finalize the process and handle any interface errors
     *
     * @param hasErrors true if there were errors during processing
     * @return true if finalization was successful
     */
    public boolean finalizeProcess(boolean hasErrors) {
        log.info("Finalizing DataHive process. Has errors: {}", hasErrors);

        try {
            if (hasErrors) {
                logError("Process completed with interface errors");
                return false;
            } else {
                // Add records to ocms_nro_temp table
                boolean recordsAdded = addRecordsToNroTemp();
                if (recordsAdded) {
                    log.info("Process completed successfully");
                    return true;
                } else {
                    logError("Failed to add records to ocms_nro_temp");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error during process finalization", e);
            logError("Process finalization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Step 7: Finalize the process and handle any interface errors
     * Always adds successful records to ocms_nro_temp, regardless of errors in other records
     *
     * @param overallSuccess true if all records processed successfully (no errors)
     * @param records list of successful records to add to ocms_nro_temp
     * @return true if finalization was successful
     */
    public boolean finalizeProcess(boolean overallSuccess, List<OffenceNoticeRecord> records) {
        log.info("Finalizing DataHive process. Overall success: {}, Successful records to add: {}",
                 overallSuccess, records != null ? records.size() : 0);

        try {
            // Always add successful records to ocms_nro_temp, even if some records had errors
            if (records != null && !records.isEmpty()) {
                boolean recordsAdded = dataHiveUenFinService.addRecordsToNroTemp(records);
                if (recordsAdded) {
                    log.info("Successfully added {} records to ocms_nro_temp", records.size());
                } else {
                    logError("Failed to add records to ocms_nro_temp");
                    return false;
                }
            } else {
                log.warn("No successful records to add to ocms_nro_temp");
            }

            // Return success status based on overall processing result
            if (!overallSuccess) {
                log.warn("Process completed with partial success - {} records added, but some had errors",
                         records != null ? records.size() : 0);
                return false;
            } else {
                log.info("Process completed successfully - all records processed");
                return true;
            }
        } catch (Exception e) {
            log.error("Error during process finalization", e);
            logError("Process finalization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Process common dataset from DataHive (uses stored records)
     * This is a convenience method that uses the internally stored currentRecords.
     * Prefer using processCommonDataset(List<OffenceNoticeRecord> records) directly.
     *
     * @return true if common dataset processing was successful
     */
    public boolean processCommonDataset() {
        if (currentRecords == null || currentRecords.isEmpty()) {
            log.warn("No records available for common dataset processing");
            return true; // Return true to not block the flow
        }
        return processCommonDataset(currentRecords);
    }

    // Private helper methods for DataHive operations

    private boolean addRecordsToNroTemp() {
        if (currentRecords == null || currentRecords.isEmpty()) {
            log.warn("No records available to add to ocms_nro_temp");
            return true; // Return true as there's nothing to add
        }
        return dataHiveUenFinService.addRecordsToNroTemp(currentRecords);
    }

    /**
     * Log error messages
     *
     * @param errorMessage the error message to log
     */
    private void logError(String errorMessage) {
        log.error(errorMessage);
        // Additional error logging logic can be added here
    }

    /**
     * BATCH PROCESSING: Process all records using batch mode for improved performance
     * Groups records by ID type and processes them in batches
     *
     * @param records List of all records to process
     * @return Map of noticeNo -> success status
     */
    public Map<String, Boolean> batchProcessAllRecords(List<OffenceNoticeRecord> records) {
        log.info("Starting batch processing for {} total records", records.size());

        // Group records by ID type
        Map<String, List<OffenceNoticeRecord>> recordsByType = records.stream()
                .collect(Collectors.groupingBy(OffenceNoticeRecord::getIdType));

        Map<String, Boolean> allResults = new java.util.HashMap<>();

        // Process UEN records in batch
        List<OffenceNoticeRecord> uenRecords = recordsByType.get("B");
        if (uenRecords != null && !uenRecords.isEmpty()) {
            log.info("Batch processing {} UEN records", uenRecords.size());
            Map<String, Boolean> uenResults = dataHiveUenFinService.batchProcessUenRecords(uenRecords);
            allResults.putAll(uenResults);

            // Populate UEN status map from service after batch processing
            Map<String, String> serviceStatusMap = dataHiveUenFinService.getUenStatusMap();
            uenStatusMap.putAll(serviceStatusMap);
        }

        // Process FIN records in batch
        List<OffenceNoticeRecord> finRecords = recordsByType.get("F");
        if (finRecords != null && !finRecords.isEmpty()) {
            log.info("Batch processing {} FIN records", finRecords.size());
            Map<String, Boolean> finResults = dataHiveUenFinService.batchProcessFinRecords(finRecords);
            allResults.putAll(finResults);
        }

        log.info("Batch processing completed: {} total results", allResults.size());
        return allResults;
    }

    /**
     * BATCH PROCESSING: Process common dataset for all records in batch mode
     * Much faster than individual processing
     *
     * @param records List of all records
     * @return true if successful
     */
    public boolean batchProcessCommonDatasetForAll(List<OffenceNoticeRecord> records) {
        log.info("Batch processing common dataset for {} records", records.size());
        return dataHiveUenFinService.batchProcessCommonDataset(records);
    }

    /**
     * Get the UEN status map populated during batch processing
     * Maps noticeNo to "REGISTERED" or "DEREGISTERED" status
     *
     * @return Map of noticeNo -> UEN status
     */
    public Map<String, String> getUenStatusMap() {
        return uenStatusMap;
    }
}
