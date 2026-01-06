package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhSpsIncarceration;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_sps_incarceration table.
 * Stores incarceration details of individuals including inmate number, admission date, 
 * programme category, and tentative release date. The table enforces uniqueness on inmate 
 * number and links each record to an offender via id_no.
 * 
 * Primary Key: inmate_number
 */
@Entity
@Table(name = "ocms_dh_sps_incarceration", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsDhSpsIncarceration extends BaseEntity {

    @Column(name = "id_no", length = 9, nullable = false)
    private String idNo;

    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Id
    @Column(name = "inmate_number", length = 11, nullable = false)
    private String inmateNumber;

    @Column(name = "inmate_programme_category", length = 3)
    private String inmateProgrammeCategory;

    @Column(name = "adm_date")
    private LocalDateTime admDate;

    @Column(name = "tentative_release_date")
    private LocalDateTime tentativeReleaseDate;

    @Column(name = "reference_period_offence_info", nullable = false)
    private LocalDateTime referencePeriodOffenceInfo;

    @Column(name = "reference_period_release", nullable = false)
    private LocalDateTime referencePeriodRelease;
}