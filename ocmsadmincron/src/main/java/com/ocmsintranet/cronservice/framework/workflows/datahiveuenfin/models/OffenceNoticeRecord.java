package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing an offence notice record for DataHive processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffenceNoticeRecord {

    /**
     * Identification number (UEN or FIN)
     */
    private String idNo;

    /**
     * Type of identification ('B' for UEN, 'F' for FIN)
     */
    private String idType;

    /**
     * Notice number
     */
    private String noticeNo;

    /**
     * Next processing stage (RD1, RD2, RR3, DN2, DR3)
     */
    private String nextProcessingStage;

    /**
     * Next processing date
     */
    private java.sql.Date nextProcessingDate;

    /**
     * Suspension type (should be NULL for processing)
     */
    private String suspensionType;

    /**
     * Offender indicator ('Y' for eligible records)
     */
    private String offenderIndicator;

    /**
     * Owner/Driver/Hirer indicator ('O', 'D', 'H')
     */
    private String ownerDriverIndicator;

    /**
     * Additional metadata for tracking
     */
    private java.sql.Timestamp createdDate;
    private java.sql.Timestamp updatedDate;
    private String processStatus;
    private String errorMessage;
}