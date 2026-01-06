package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.cronservice.crud.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_nro_temp table.
 * Temporary table to queue notice and offender ID data for MHA and DataHive queries.
 * Used for both Unclaimed reminders (UNC) and HST monthly address checks.
 *
 * Updated for OCMS 20 to support:
 * - UNC (Unclaimed) queries - query_reason = 'UNC'
 * - HST (House Tenant) queries - query_reason = 'HST'
 * - Regular MHA NRIC verification - query_reason = NULL or 'NRIC'
 *
 * Primary Key: Auto-generated ID
 */
@Entity
@Table(name = "ocms_nro_temp", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsNroTemp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "notice_no", length = 20, nullable = false)
    private String noticeNo;

    @Column(name = "id_no", length = 20)
    private String idNo;

    @Column(name = "id_type", length = 10)
    private String idType;

    /**
     * Query reason to distinguish different types of queries:
     * - 'NRIC' or NULL: Regular MHA NRIC verification (existing flow)
     * - 'UNC': Unclaimed reminder address query
     * - 'HST': House tenant address query
     */
    @Column(name = "query_reason", length = 10)
    private String queryReason;

    /**
     * Processing status flag
     * - false: Not yet processed by MHA/DataHive
     * - true: Response received and processed
     */
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
}