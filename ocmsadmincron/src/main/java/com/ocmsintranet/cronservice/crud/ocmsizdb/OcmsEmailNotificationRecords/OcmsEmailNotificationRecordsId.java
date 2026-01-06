package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEmailNotificationRecords;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        OcmsEmailNotificationRecordsId that = (OcmsEmailNotificationRecordsId) o;
        
        if (noticeNo != null ? !noticeNo.equals(that.noticeNo) : that.noticeNo != null) return false;
        return processingStage != null ? processingStage.equals(that.processingStage) : that.processingStage == null;
    }

    @Override
    public int hashCode() {
        int result = noticeNo != null ? noticeNo.hashCode() : 0;
        result = 31 * result + (processingStage != null ? processingStage.hashCode() : 0);
        return result;
    }
}