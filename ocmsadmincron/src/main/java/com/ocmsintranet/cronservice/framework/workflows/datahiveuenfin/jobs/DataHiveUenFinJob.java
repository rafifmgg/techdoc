package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.helpers.DataHiveUenFinHelper;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.services.DataHiveUenFinService;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models.OffenceNoticeRecord;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models.ProcessExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * DataHive UEN & FIN Synchronization Job - Retrieves and processes FIN/UEN data from DataHive
 *
 * This job orchestrates the complete DataHive synchronization workflow to retrieve
 * UEN (Unique Entity Number) and FIN (Foreign Identification Number) data for
 * offence notice processing.
 *
 * WORKFLOW OVERVIEW:
 * 1. Query database for notices requiring DataHive FIN & UEN processing
 * 2. Process records in BATCH MODE based on ID type (UEN = 'B', FIN = 'F')
 *    - Groups records by type automatically
 *    - Reduces API calls by ~99% (from N to N/100)
 * 3. Retrieve data from DataHive using Snowflake connections in batches
 * 4. Store retrieved data in appropriate database tables
 * 5. Process common dataset from DataHive in BATCH MODE
 * 6. Handle interface errors and update tracking tables
 *
 * PROCESSING CRITERIA:
 * - Processing stages: RD1, RD2, RR3, DN2, DR3
 * - Next processing date <= current date
 * - Suspension type is NULL
 * - ID type is 'F' (FIN) or 'B' (UEN)
 * - Notice not in ocms_nro_temp (already processed)
 * - ID not in ocms_hst (historical exclusions)
 * - Offender indicator = 'Y'
 *
 * DATA SOURCES:
 * - DataHive UEN data: Entity information from business registrations
 * - DataHive FIN data: Foreign worker and resident information
 * - Common dataset: Reference data and lookup tables
 *
 * ERROR HANDLING:
 * - Individual record processing errors do not stop the entire job
 * - Failed DataHive connections trigger appropriate error logging
 * - Interface errors are tracked and reported
 * - Critical errors trigger framework notifications and job failure
 *
 * DEPENDENCIES:
 * - DataHiveUenFinService: For DataHive operations and database updates
 * - DataHiveUenFinHelper: For workflow orchestration and business logic
 * - Snowflake: For DataHive connectivity and data retrieval
 *
 * SCHEDULING:
 * - Configurable cron schedule (typically daily or multiple times per day)
 * - Uses ShedLock for distributed locking in clustered environments
 * - Manual trigger available via REST API endpoint
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataHiveUenFinJob extends TrackedCronJobTemplate {

    // Service for DataHive operations and database interactions
    private final DataHiveUenFinService dataHiveUenFinService;

    // Helper for workflow orchestration and business logic
    private final DataHiveUenFinHelper dataHiveUenFinHelper;

    // Job name for ShedLock and tracking purposes
    @Value("${cron.datahive.uenfin.shedlock.name:fetch_datahive_uen_fin}")
    private String jobName;

    // Maximum number of records to process in a single job run
    @Value("${datahive.uenfin.max.records.per.run:1000}")
    private int maxRecordsPerRun;

    // DataHive connection timeout in seconds
    @Value("${datahive.connection.timeout.seconds:300}")
    private int dataHiveConnectionTimeout;

    @Override
    protected String getJobName() {
        return jobName;
    }

    /**
     * Validates that all required dependencies are properly initialized before job execution.
     * This ensures the job fails fast if any critical components are missing.
     *
     * @return true if all pre-conditions are met, false otherwise
     */
    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for DataHive UEN & FIN synchronization job");

        // Verify DataHive service is available
        if (dataHiveUenFinService == null) {
            log.error("DataHiveUenFinService is not initialized");
            return false;
        }

        // Verify workflow helper is available
        if (dataHiveUenFinHelper == null) {
            log.error("DataHiveUenFinHelper is not initialized");
            return false;
        }

        // Verify configuration values are reasonable
        if (maxRecordsPerRun <= 0) {
            log.error("Invalid maxRecordsPerRun configuration: {}", maxRecordsPerRun);
            return false;
        }

        if (dataHiveConnectionTimeout <= 0) {
            log.error("Invalid dataHiveConnectionTimeout configuration: {}", dataHiveConnectionTimeout);
            return false;
        }

        log.info("All pre-conditions validated successfully");
        log.info("Configuration - Max records per run: {}, Connection timeout: {}s",
                maxRecordsPerRun, dataHiveConnectionTimeout);
        return true;
    }

    /**
     * Initialize the job with any required setup.
     * Called by the framework before doExecute().
     */
    @Override
    protected void initialize() {
        super.initialize();
        log.info("Initializing DataHive UEN & FIN synchronization job at {}", LocalDateTime.now());
    }

    /**
     * Cleanup resources after job execution (success or failure).
     * Ensures any connections or temporary resources are properly released.
     */
    @Override
    protected void cleanup() {
        log.info("Cleaning up DataHive UEN & FIN synchronization job resources");
        // Any specific cleanup logic can be added here
        super.cleanup();
    }

    /**
     * Main execution method that orchestrates the complete DataHive synchronization workflow.
     *
     * EXECUTION FLOW (BATCH MODE):
     * 1. Query database for notices requiring DataHive FIN & UEN processing
     * 2. Validate that records exist for processing
     * 3. Process records in BATCH MODE - groups by ID type automatically
     *    - UEN records: Batch retrieve from V_DH_ACRA_* tables
     *    - FIN records: Batch retrieve from V_DH_MHA_* and V_DH_MOM_* tables
     * 4. Process common dataset in BATCH MODE
     *    - Batch retrieve prison/custody data for FIN records
     * 5. Handle interface errors and update tracking tables
     * 6. Generate comprehensive execution summary
     *
     * PERFORMANCE: Batch processing reduces API calls by ~99% (from N to N/100)
     *
     * @return JobResult containing execution status and summary message
     */
    @Override
    protected JobResult doExecute() {
        log.info("Starting DataHive UEN & FIN synchronization job execution");
        LocalDateTime executionStartTime = LocalDateTime.now();

        try {
            // Step 1: Query for notices requiring DataHive processing
            log.info("Querying for notices requiring DataHive FIN & UEN processing");
            List<OffenceNoticeRecord> records = dataHiveUenFinHelper.queryDataHiveForNotices();

            // Step 2: Validate that records exist for processing
            if (records == null || records.isEmpty()) {
                log.info("No records found requiring DataHive processing");
                return new JobResult(true, "No records found requiring DataHive processing");
            }

            // Limit records processed in single run to prevent performance issues
            if (records.size() > maxRecordsPerRun) {
                log.info("Found {} records, limiting to {} per run", records.size(), maxRecordsPerRun);
                records = records.subList(0, maxRecordsPerRun);
            }

            log.info("Processing {} records for DataHive synchronization", records.size());

            // Step 3: Initialize processing statistics
            ProcessExecutionResult.ProcessExecutionResultBuilder resultBuilder = ProcessExecutionResult.builder()
                .processStartTime(java.sql.Timestamp.valueOf(executionStartTime))
                .recordsProcessed(0)
                .uenRecordsProcessed(0)
                .finRecordsProcessed(0)
                .recordsWithErrors(0)
                .errorMessages(new ArrayList<>())
                .hasInterfaceErrors(false)
                .dataHiveConnectionSuccess(true)
                .commonDatasetProcessed(false);

            // Step 4: Process records by ID type using BATCH PROCESSING
            log.info("Using BATCH PROCESSING mode for improved performance");
            int totalProcessed = 0;
            int totalErrors = 0;
            int uenProcessed = 0;
            int uenRegistered = 0;
            int uenDeregistered = 0;
            int uenFailed = 0;
            int finProcessed = 0;
            int finFailed = 0;
            List<String> errorMessages = new ArrayList<>();
            List<OffenceNoticeRecord> successfulRecords = new ArrayList<>();

            try {
                // Process all records in batch mode (groups by type automatically)
                Map<String, Boolean> batchResults = dataHiveUenFinHelper.batchProcessAllRecords(records);
                Map<String, String> uenStatusMap = dataHiveUenFinHelper.getUenStatusMap(); // Get registered/deregistered info

                // Update record statuses based on batch results
                for (OffenceNoticeRecord record : records) {
                    Boolean success = batchResults.get(record.getNoticeNo());

                    if (success != null && success) {
                        totalProcessed++;
                        successfulRecords.add(record);
                        record.setProcessStatus("SUCCESS");

                        // Track by ID type
                        if ("B".equals(record.getIdType())) {
                            uenProcessed++;
                            // Determine if registered or de-registered
                            String status = uenStatusMap.get(record.getNoticeNo());
                            if ("DEREGISTERED".equals(status)) {
                                uenDeregistered++;
                            } else if ("REGISTERED".equals(status)) {
                                uenRegistered++;
                            }
                        } else if ("F".equals(record.getIdType())) {
                            finProcessed++;
                        }
                        log.debug("Successfully processed record: {}", record.getIdNo());
                    } else {
                        totalErrors++;
                        record.setProcessStatus("FAILED");

                        // Track failures by type
                        if ("B".equals(record.getIdType())) {
                            uenFailed++;
                        } else if ("F".equals(record.getIdType())) {
                            finFailed++;
                        }

                        String errorMsg = String.format("Failed to process record %s (Type: %s)",
                                record.getIdNo(), record.getIdType());
                        errorMessages.add(errorMsg);
                        log.warn(errorMsg);
                    }
                }

                log.info("Batch processing completed: {} successful ({} UEN: {} registered, {} de-registered; {} FIN), {} failed ({} UEN, {} FIN)",
                        totalProcessed, uenProcessed, uenRegistered, uenDeregistered, finProcessed,
                        totalErrors, uenFailed, finFailed);

            } catch (Exception e) {
                // If batch processing fails entirely, mark all as errors
                totalErrors = records.size();
                String errorMsg = "Batch processing failed: " + e.getMessage();
                errorMessages.add(errorMsg);
                log.error("Critical error in batch processing: {}", e.getMessage(), e);
            }

            // Step 5: Process common dataset from DataHive using BATCH PROCESSING
            log.info("Processing common dataset from DataHive in BATCH mode");
            boolean commonDatasetSuccess = false;
            try {
                commonDatasetSuccess = dataHiveUenFinHelper.batchProcessCommonDatasetForAll(records);
                if (commonDatasetSuccess) {
                    log.info("Successfully processed common dataset from DataHive in batch");
                } else {
                    log.warn("Failed to process common dataset from DataHive in batch");
                    errorMessages.add("Failed to process common dataset from DataHive");
                }
            } catch (Exception e) {
                log.error("Error processing common dataset in batch: {}", e.getMessage(), e);
                errorMessages.add("Error processing common dataset: " + e.getMessage());
            }

            // Step 6: Check for interface errors and finalize
            boolean hasInterfaceErrors = !errorMessages.isEmpty() || totalErrors > 0;
            boolean finalizationSuccess = false;

            try {
                // Only add successful records to nro_temp
                finalizationSuccess = dataHiveUenFinHelper.finalizeProcess(!hasInterfaceErrors, successfulRecords);
                if (finalizationSuccess && !hasInterfaceErrors) {
                    log.info("Process finalization completed successfully");
                } else {
                    log.warn("Process finalization completed with errors - {} successful records added to nro_temp", successfulRecords.size());
                }
            } catch (Exception e) {
                log.error("Error during process finalization: {}", e.getMessage(), e);
                errorMessages.add("Error during process finalization: " + e.getMessage());
                hasInterfaceErrors = true;
            }

            // Step 7: Calculate execution duration and build result
            LocalDateTime executionEndTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(executionStartTime, executionEndTime).toMillis();

            ProcessExecutionResult result = resultBuilder
                .processEndTime(java.sql.Timestamp.valueOf(executionEndTime))
                .processingDurationMs(durationMs)
                .recordsProcessed(totalProcessed)
                .uenRecordsProcessed(uenProcessed)
                .finRecordsProcessed(finProcessed)
                .recordsWithErrors(totalErrors)
                .errorMessages(errorMessages)
                .hasInterfaceErrors(hasInterfaceErrors)
                .commonDatasetProcessed(commonDatasetSuccess)
                .success(!hasInterfaceErrors && finalizationSuccess)
                .build();

            // Step 8: Generate comprehensive result message with detailed breakdown
            String resultMessage = String.format(
                "DataHive sync: %d success (%d UEN [%d reg, %d dereg], %d FIN), %d fail (%d UEN, %d FIN), %dms",
                totalProcessed, uenProcessed, uenRegistered, uenDeregistered, finProcessed,
                totalErrors, uenFailed, finFailed, durationMs
            );

            if (commonDatasetSuccess) {
                resultMessage += ", common-dataset: OK";
            }

            // Return success even with partial errors to allow continued processing
            if (hasInterfaceErrors || totalErrors > 0) {
                log.warn(resultMessage + " (with errors)");
                return new JobResult(true, resultMessage + " (with errors)");
            } else {
                log.info(resultMessage);
                return new JobResult(true, resultMessage);
            }

        } catch (Exception e) {
            log.error("Critical error in DataHive UEN & FIN synchronization job: {}", e.getMessage(), e);
            // Throw exception to trigger framework error handling and notifications
            throw new RuntimeException("DataHive UEN & FIN synchronization job failed: " + e.getMessage(), e);
        }
    }
}