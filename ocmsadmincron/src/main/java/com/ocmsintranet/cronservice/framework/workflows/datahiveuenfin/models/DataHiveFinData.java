package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing FIN data retrieved from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHiveFinData {

    /**
     * FIN (Foreign Identification Number)
     */
    private String fin;

    /**
     * Person's full name
     */
    private String fullName;

    /**
     * Date of birth
     */
    private java.sql.Date dateOfBirth;

    /**
     * Nationality
     */
    private String nationality;

    /**
     * Gender
     */
    private String gender;

    /**
     * Residential address
     */
    private String residentialAddress;

    /**
     * Postal code
     */
    private String postalCode;

    /**
     * Pass type (Work permit, Student pass, etc.)
     */
    private String passType;

    /**
     * Pass status (Valid, Expired, Cancelled, etc.)
     */
    private String passStatus;

    /**
     * Pass expiry date
     */
    private java.sql.Date passExpiryDate;

    /**
     * Employer name (if applicable)
     */
    private String employerName;

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