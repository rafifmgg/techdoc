package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_nro_temp table.
 * Temporary table to queue notice and offender ID data for MHA and DataHive queries.
 * Used for both Unclaimed reminders (UNC) and HST monthly address checks.
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

    @Column(name = "id_no", length = 20, nullable = false)
    private String idNo;

    @Column(name = "id_type", length = 10, nullable = false)
    private String idType;

    @Column(name = "query_reason", length = 10, nullable = false)
    private String queryReason; // 'UNC' or 'HST'

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
}
