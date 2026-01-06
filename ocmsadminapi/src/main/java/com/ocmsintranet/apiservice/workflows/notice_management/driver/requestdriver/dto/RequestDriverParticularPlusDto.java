package com.ocmsintranet.apiservice.workflows.notice_management.driver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
// import com.ocmsintranet.apiservice.crud.ocmsizdb.requestdriverparticular.RequestDriverParticular;

/**
 * DTO for PLUS Request Driver Particular API
 * Contains only fields specified in PLUS Interface Specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDriverParticularPlusDto {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfProcessing;
    
    private String noticeNo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfRdp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfReturn;
    
    private String ownerBldg;
    private String ownerBlkHseNo;
    private String ownerFloor;
    private String ownerIdType;
    private String ownerName;
    private String ownerNricNo;
    private String ownerPostalCode;
    private String ownerStreet;
    private String ownerUnit;
    private String postalRegnNo;
    private String processingStage;
    private String reminderFlag;
    private String unclaimedReason;
    
    // // Constructor from RequestDriverParticular entity
    // public RequestDriverParticularPlusDto(RequestDriverParticular entity) {
    //     this.dateOfProcessing = entity.getDateOfProcessing();
    //     this.noticeNo = entity.getNoticeNo();
    //     this.dateOfRdp = entity.getDateOfRdp();
    //     this.dateOfReturn = entity.getDateOfReturn();
    //     this.ownerBldg = entity.getOwnerBldg();
    //     this.ownerBlkHseNo = entity.getOwnerBlkHseNo();
    //     this.ownerFloor = entity.getOwnerFloor();
    //     this.ownerIdType = entity.getOwnerIdType();
    //     this.ownerName = entity.getOwnerName();
    //     this.ownerNricNo = entity.getOwnerNricNo();
    //     this.ownerPostalCode = entity.getOwnerPostalCode();
    //     this.ownerStreet = entity.getOwnerStreet();
    //     this.ownerUnit = entity.getOwnerUnit();
    //     this.postalRegnNo = entity.getPostalRegnNo();
    //     this.processingStage = entity.getProcessingStage();
    //     this.reminderFlag = entity.getReminderFlag();
    //     this.unclaimedReason = entity.getUnclaimedReason();
    // }
}