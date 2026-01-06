package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Model representing the result of DataHive process execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessExecutionResult {

    /**
     * Overall process success status
     */
    private boolean success;

    /**
     * Process execution message
     */
    private String message;

    /**
     * Number of records processed
     */
    private int recordsProcessed;

    /**
     * Number of UEN records processed successfully
     */
    private int uenRecordsProcessed;

    /**
     * Number of FIN records processed successfully
     */
    private int finRecordsProcessed;

    /**
     * Number of records with errors
     */
    private int recordsWithErrors;

    /**
     * List of error messages encountered during processing
     */
    private List<String> errorMessages;

    /**
     * Processing statistics
     */
    private Map<String, Object> processingStats;

    /**
     * Process start timestamp
     */
    private java.sql.Timestamp processStartTime;

    /**
     * Process end timestamp
     */
    private java.sql.Timestamp processEndTime;

    /**
     * Total processing duration in milliseconds
     */
    private long processingDurationMs;

    /**
     * Interface error status
     */
    private boolean hasInterfaceErrors;

    /**
     * DataHive connection status
     */
    private boolean dataHiveConnectionSuccess;

    /**
     * Common dataset processing status
     */
    private boolean commonDatasetProcessed;
}