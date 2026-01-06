package com.ocmsintranet.apiservice.workflows.notice_management.suspension.suspendednotice.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspendedNoticePlusDto {
    private String noticeNo;
    private Integer srNo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfSuspension;
    
    private String suspensionType;
    private String reasonOfSuspension;
    private String suspensionSource;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDateOfRevival;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfRevival;
    
    private String officerAuthorisingSuspension;
    private String officerAuthorisingRevival;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime creDate;
    
    private String suspensionRemarks;
    private String caseNo;
    private String creUserId;
    private String revivalRemarks;
    
    // Constructor from SuspendedNotice entity
    public SuspendedNoticePlusDto(SuspendedNotice suspendedNotice) {
        this.noticeNo = suspendedNotice.getNoticeNo();
        this.srNo = suspendedNotice.getSrNo();
        this.dateOfSuspension = suspendedNotice.getDateOfSuspension();
        this.suspensionType = suspendedNotice.getSuspensionType();
        this.reasonOfSuspension = suspendedNotice.getReasonOfSuspension();
        this.suspensionSource = suspendedNotice.getSuspensionSource();
        this.dueDateOfRevival = suspendedNotice.getDueDateOfRevival();
        this.dateOfRevival = suspendedNotice.getDateOfRevival();
        this.officerAuthorisingSuspension = suspendedNotice.getOfficerAuthorisingSupension();
        this.officerAuthorisingRevival = suspendedNotice.getOfficerAuthorisingRevival();
        this.creDate = suspendedNotice.getCreDate();
        this.suspensionRemarks = suspendedNotice.getSuspensionRemarks();
        this.caseNo = suspendedNotice.getCaseNo();
        this.creUserId = suspendedNotice.getCreUserId();
        this.revivalRemarks = suspendedNotice.getRevivalRemarks();
    }
}
