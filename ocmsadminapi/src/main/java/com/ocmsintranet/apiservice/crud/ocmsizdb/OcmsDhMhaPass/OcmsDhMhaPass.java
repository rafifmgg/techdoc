package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMhaPass;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_mha_pass table.
 * Stores MHA pass information for individuals identified by id_no and notice_no.
 * 
 * Primary Key: composite key (id_no, notice_no)
 */
@Entity
@Table(name = "ocms_dh_mha_pass", schema = "ocmsizmgr")
@IdClass(OcmsDhMhaPassId.class)
@Getter
@Setter
public class OcmsDhMhaPass extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 20, nullable = false)
    private String idNo;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "address_indicator", length = 1)
    private String addressIndicator;

    @Column(name = "date_of_expiry")
    private LocalDateTime dateOfExpiry;

    @Column(name = "reference_period_ltvp", nullable = false)
    private LocalDateTime referencePeriodLtvp;

    @Column(name = "reference_period_stp", nullable = false)
    private LocalDateTime referencePeriodStp;

    @Column(name = "reference_period_finddeath", nullable = false)
    private LocalDateTime referencePeriodFinddeath;

    @Column(name = "reference_period_alive_scpr", nullable = false)
    private LocalDateTime referencePeriodAliveScpr;

    @Column(name = "reference_period_scgrant", nullable = false)
    private LocalDateTime referencePeriodScgrant;

    @Column(name = "principal_name", length = 66)
    private String principalName;

    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "non_work_pass_type", length = 9)
    private String nonWorkPassType;

    @Column(name = "previous_fin", length = 9)
    private String previousFin;

    @Column(name = "date_pr_granted")
    private LocalDateTime datePrGranted;

    @Column(name = "sc_grant_date")
    private LocalDateTime scGrantDate;

    @Column(name = "date_of_issue")
    private LocalDateTime dateOfIssue;
}