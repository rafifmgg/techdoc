package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsCustody;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_sps_custody table.
 * This table stores custody records for individuals (e.g., ex-SCs, ex-PRs, or other identified persons) 
 * based on their incarceration status. It includes key identifiers, admission institution mappings, 
 * custody, offense timelines, and metadata tracking.
 * 
 * Primary Key: (id_no, notice_no, adm_date)
 */
@Entity
@Table(name = "ocms_dh_sps_custody", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsDhSpsCustodyId.class)
public class OcmsDhSpsCustody extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 9, nullable = false)
    private String idNo;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "instit_code", length = 7, nullable = false)
    private String institCode;

    @Id
    @Column(name = "adm_date", nullable = false)
    private LocalDateTime admDate;

    @Column(name = "reference_period", nullable = false)
    private LocalDateTime referencePeriod;

    @Column(name = "current_custody_status", length = 1)
    private String currentCustodyStatus;
}