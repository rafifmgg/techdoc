package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing UEN data retrieved from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHiveUenData {

    /**
     * UEN (Unique Entity Number)
     */
    private String uen;

    /**
     * Entity name
     */
    private String entityName;

    /**
     * Entity type
     */
    private String entityType;

    /**
     * Registration date
     */
    private java.sql.Date registrationDate;

    /**
     * Entity status (Active, Inactive, etc.)
     */
    private String entityStatus;

    /**
     * Business address
     */
    private String businessAddress;

    /**
     * Postal code
     */
    private String postalCode;

    /**
     * Entity incorporation date
     */
    private java.sql.Date incorporationDate;

    /**
     * Primary activity description
     */
    private String primaryActivity;

    /**
     * Secondary activity description
     */
    private String secondaryActivity;

    /**
     * DataHive retrieval timestamp
     */
    private java.sql.Timestamp retrievalTimestamp;

    /**
     * Data source identifier
     */
    private String dataSource;

    /**
     * Validation status
     */
    private String validationStatus;
}