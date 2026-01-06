package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.NonEditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_suspended_notice table.
 * This table is used to record and manage suspensions related to compliance notices within an organization.
 * It tracks details such as the reason for suspension, the officer responsible, and the dates of suspension
 * and potential revival. This table is essential for maintaining accountability and ensuring that all
 * suspensions are documented and managed systematically.
 */
@Entity
@Table(name = "ocms_suspended_notice", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SuspendedNoticeId.class)
public class SuspendedNotice extends BaseEntity {

    @Column(name = "notice_no", length = 10, nullable = false)
    @Id
    @NotBlank
    @NonEditable
    private String noticeNo;

    @Column(name = "date_of_suspension", nullable = false)
    @Id
    @NonEditable
    private LocalDateTime dateOfSuspension;

    @Column(name = "sr_no", nullable = false)
    @Id
    @NonEditable
    private Integer srNo;

    @Column(name = "suspension_source", length = 4, nullable = false)
    @NotBlank
    private String suspensionSource;

    @Column(name = "case_no", length = 50)
    private String caseNo;

    @Column(name = "suspension_type", length = 2, nullable = false)
    @NotBlank
    private String suspensionType;

    @Column(name = "reason_of_suspension", length = 3, nullable = false)
    @NotBlank
    private String reasonOfSuspension;

    @Column(name = "officer_authorising_suspension", length = 50, nullable = false)
    @NotBlank
    private String officerAuthorisingSupension;

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
