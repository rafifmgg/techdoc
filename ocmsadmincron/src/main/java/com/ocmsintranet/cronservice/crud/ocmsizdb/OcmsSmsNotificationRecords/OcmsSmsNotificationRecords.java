package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSmsNotificationRecords;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsId;

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

    @Id
    @Column(name = "batch_id", length = 50, nullable = false)
    private String batchId;

    @Column(name = "content", columnDefinition = "varbinary(max)")
    private byte[] content;

    @Column(name = "date_sent")
    private LocalDateTime dateSent;

    @Column(name = "mobile_code", length = 3)
    private String mobileCode;

    @Column(name = "mobile_no", length = 12, nullable = false)
    private String mobileNo;

    @Column(name = "msg_status", length = 250)
    private String msgStatus;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    // @Column(name = "cre_date", nullable = false)
    // private LocalDateTime creDate;

    // @Column(name = "cre_user_id", length = 50, nullable = false)
    // private String creUserId;

    // @Column(name = "upd_date")
    // private LocalDateTime updDate;

    // @Column(name = "upd_user_id", length = 50)
    // private String updUserId;
}
