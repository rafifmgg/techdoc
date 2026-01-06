package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite ID class for OcmsSuspendedNotice entity.
 * This class represents the composite primary key consisting of noticeNo, dateOfSuspension, and srNo.
 */
public class OcmsSuspendedNoticeId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String noticeNo;
    private LocalDateTime dateOfSuspension;
    private Integer srNo;
    
    public OcmsSuspendedNoticeId() {
    }
    
    public OcmsSuspendedNoticeId(String noticeNo, LocalDateTime dateOfSuspension, Integer srNo) {
        this.noticeNo = noticeNo;
        this.dateOfSuspension = dateOfSuspension;
        this.srNo = srNo;
    }
    
    public String getNoticeNo() {
        return noticeNo;
    }
    
    public void setNoticeNo(String noticeNo) {
        this.noticeNo = noticeNo;
    }
    
    public LocalDateTime getDateOfSuspension() {
        return dateOfSuspension;
    }
    
    public void setDateOfSuspension(LocalDateTime dateOfSuspension) {
        this.dateOfSuspension = dateOfSuspension;
    }
    
    public Integer getSrNo() {
        return srNo;
    }
    
    public void setSrNo(Integer srNo) {
        this.srNo = srNo;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OcmsSuspendedNoticeId that = (OcmsSuspendedNoticeId) o;
        return Objects.equals(noticeNo, that.noticeNo) &&
               Objects.equals(dateOfSuspension, that.dateOfSuspension) &&
               Objects.equals(srNo, that.srNo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(noticeNo, dateOfSuspension, srNo);
    }
}
