package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import com.ocmsintranet.apiservice.crud.BaseEntity;
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

    @Id
    @Column(name = "processing_stage", length = 3, nullable = false)
    private String processingStage;

    @Column(name = "email_addr", length = 320, nullable = false)
    private String emailAddr;

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
}