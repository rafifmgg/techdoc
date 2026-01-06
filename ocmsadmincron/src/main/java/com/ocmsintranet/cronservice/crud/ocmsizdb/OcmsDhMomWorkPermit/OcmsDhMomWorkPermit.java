package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMomWorkPermit;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_mom_work_permit table.
 * Stores work permit details and status for individuals, identified by notice_no and id_no. 
 * Includes permit dates, status codes, and metadata for record creation and updates.
 * 
 * Primary Key: (id_no, notice_no)
 */
@Entity
@Table(name = "ocms_dh_mom_work_permit", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsDhMomWorkPermitId.class)
public class OcmsDhMomWorkPermit extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 9, nullable = false)
    private String idNo;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "name", length = 66)
    private String name;

    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "work_permit_no", length = 9)
    private String workPermitNo;

    @Column(name = "employer_nric_fin", length = 9)
    private String employerNricFin;

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "ipa_expiry_date")
    private LocalDateTime ipaExpiryDate;

    @Column(name = "withdrawn_date")
    private LocalDateTime withdrawnDate;

    @Column(name = "application_date")
    private LocalDateTime applicationDate;

    @Column(name = "passtype_cd", length = 9)
    private String passtypeCd;

    @Column(name = "issuance_date")
    private LocalDateTime issuanceDate;

    @Column(name = "work_pass_status_cd", length = 9)
    private String workPassStatusCd;

    @Column(name = "revoked_cancelled_date")
    private LocalDateTime revokedCancelledDate;

    @Column(name = "uen", length = 10)
    private String uen;
}