package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for OcmsEmailNotificationRecords entity.
 * Primary key consists of notice_no and email_addr.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsEmailNotificationRecordsId implements Serializable {

    private String noticeNo;
    private String processingStage;
    
}