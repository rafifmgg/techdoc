package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

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
 * Entity representing the ocms_sms_notification_records table.
 * This table stores information related to SMS notifications sent for notices,
 * including offence details and recipient information.
 */
@Entity
@Table(name = "ocms_sms_notification_records", schema = "ocmsizmgr")
@IdClass(OcmsSmsNotificationRecordsId.class)
@Getter
@Setter
public class OcmsSmsNotificationRecords extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Id
    @Column(name = "processing_stage", length = 3)
    private String processingStage;

    @Column(name = "content", columnDefinition = "varbinary(max)")
    private byte[] content;

    @Column(name = "date_sent")
    private LocalDateTime dateSent;

    @Column(name = "mobile_code", length = 3)
    private String mobileCode;

    @Column(name = "mobile_no", length = 12, nullable = false)
    private String mobileNo;

    @Column(name = "msg_error", length = 250)
    private String msgError;

    @Column(name = "status", length = 1, nullable = false)
    private String status;
}