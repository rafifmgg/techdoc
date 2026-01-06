package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.drivernotice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
// import com.ocmsintranet.apiservice.crud.ocmsizdb.drivernotice.DriverNotice;

/**
 * DTO for PLUS Driver Notice API
 * Contains only fields specified in PLUS Interface Specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverNoticePlusDto {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfProcessing;
    
    private String noticeNo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfDn;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfReturn;
    
    private String driverBldg;
    private String driverBlkHseNo;
    private String driverFloor;
    private String driverIdType;
    private String driverName;
    private String driverNricNo;
    private String driverPostalCode;
    private String driverStreet;
    private String driverUnit;
    private String postalRegnNo;
    private String processingStage;
    private String reasonForUnclaim;
    private String reminderFlag;
    
    /*
    // Constructor from DriverNotice entity - commented out until entity is ready
    public DriverNoticePlusDto(DriverNotice entity) {
        this.dateOfProcessing = entity.getDateOfProcessing();
        this.noticeNo = entity.getNoticeNo();
        this.dateOfDn = entity.getDateOfDn();
        this.dateOfReturn = entity.getDateOfReturn();
        this.driverBldg = entity.getDriverBldg();
        this.driverBlkHseNo = entity.getDriverBlkHseNo();
        this.driverFloor = entity.getDriverFloor();
        this.driverIdType = entity.getDriverIdType();
        this.driverName = entity.getDriverName();
        this.driverNricNo = entity.getDriverNricNo();
        this.driverPostalCode = entity.getDriverPostalCode();
        this.driverStreet = entity.getDriverStreet();
        this.driverUnit = entity.getDriverUnit();
        this.postalRegnNo = entity.getPostalRegnNo();
        this.processingStage = entity.getProcessingStage();
        this.reasonForUnclaim = entity.getReasonForUnclaim();
        this.reminderFlag = entity.getReminderFlag();
    }
    */
}