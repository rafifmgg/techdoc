package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing common dataset retrieved from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHiveCommonData {

    /**
     * Dataset identifier
     */
    private String datasetId;

    /**
     * Dataset name
     */
    private String datasetName;

    /**
     * Dataset category
     */
    private String category;

    /**
     * Dataset description
     */
    private String description;

    /**
     * Reference code
     */
    private String referenceCode;

    /**
     * Reference value
     */
    private String referenceValue;

    /**
     * Effective date
     */
    private java.sql.Date effectiveDate;

    /**
     * Expiry date
     */
    private java.sql.Date expiryDate;

    /**
     * Status (Active, Inactive, etc.)
     */
    private String status;

    /**
     * Data version
     */
    private String version;

    /**
     * DataHive retrieval timestamp
     */
    private java.sql.Timestamp retrievalTimestamp;

    /**
     * Data source identifier
     */
    private String dataSource;

    /**
     * Last updated timestamp
     */
    private java.sql.Timestamp lastUpdated;
}