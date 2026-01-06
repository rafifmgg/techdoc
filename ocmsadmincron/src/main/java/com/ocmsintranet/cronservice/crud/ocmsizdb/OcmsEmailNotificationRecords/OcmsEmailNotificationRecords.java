package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

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
 * Entity representing the ocms_email_notification_records table.
 * Stores information related to email notifications sent for notices, 
 * including offence details and recipient information.
 */
@Entity
@Table(name = "ocms_email_notification_records", schema = "ocmsizmgr")
@IdClass(OcmsEmailNotificationRecordsId.class)
@Getter
@Setter
public class OcmsEmailNotificationRecords extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "email_addr", length = 320, nullable = false)
    private String emailAddr;

    @Id
    @Column(name = "processing_stage", length = 3, nullable = false)
    private String processingStage;

    @Column(name = "content", columnDefinition = "varbinary(max)")
    private byte[] content;

    @Column(name = "date_sent", nullable = false)
    private LocalDateTime dateSent;

    @Column(name = "msg_error", length = 250)
    private String msgError;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "subject", length = 100, nullable = false)
    private String subject;

    // @Column(name = "cre_date", nullable = false)
    // private LocalDateTime creDate;

    // @Column(name = "cre_user_id", length = 50, nullable = false)
    // private String creUserId;

    // @Column(name = "upd_date")
    // private LocalDateTime updDate;

    // @Column(name = "upd_user_id", length = 50)
    // private String updUserId;
}