package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_suspended_notice table.
 * This table stores information about notices that have been suspended,
 * including the notice number, date of suspension, suspension type, and reasons.
 */
@Entity
@Table(name = "ocms_suspended_notice", schema = "ocmsizmgr")
@IdClass(OcmsSuspendedNoticeId.class)
@Getter
@Setter
public class OcmsSuspendedNotice extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Id
    @Column(name = "date_of_suspension", nullable = false)
    private LocalDateTime dateOfSuspension;

    @Id
    @Column(name = "sr_no", nullable = false)
    private Integer srNo;

    @Column(name = "suspension_source", length = 4, nullable = false)
    private String suspensionSource;

    @Column(name = "case_no", length = 50)
    private String caseNo;

    @Column(name = "suspension_type", length = 2, nullable = false)
    private String suspensionType;

    @Column(name = "reason_of_suspension", length = 3, nullable = false)
    private String reasonOfSuspension;

    @Column(name = "officer_authorising_suspension", length = 50, nullable = false)
    private String officerAuthorisingSuspension;

    @Column(name = "due_date_of_revival")
    private LocalDateTime dueDateOfRevival;

    @Column(name = "suspension_remarks", length = 50)
    private String suspensionRemarks;

    @Column(name = "date_of_revival")
    private LocalDateTime dateOfRevival;

    @Column(name = "revival_reason", length = 3)
    private String revivalReason;

    @Column(name = "officer_authorising_revival", length = 50)
    private String officerAuthorisingRevival;

    @Column(name = "revival_remarks", length = 50)
    private String revivalRemarks;
}
