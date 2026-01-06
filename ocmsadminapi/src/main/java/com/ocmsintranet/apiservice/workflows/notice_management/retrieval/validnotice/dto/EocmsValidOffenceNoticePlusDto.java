package com.ocmsintranet.apiservice.workflows.plus.evalidnotice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;

/**
 * DTO for PLUS Internet Valid Offence Notice API
 * Contains only fields specified in PLUS Interface Specification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsValidOffenceNoticePlusDto {
    
    private String noticeNo;
    private String vehicleNo;
    private String offenceNoticeType;
    private String eprReasonOfSuspension;
    private String lastProcessingStage;
    private String anFlag;
    private String crsReasonOfSuspension;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime crsDateOfSuspension;

    private String nextProcessingStage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime eprDateOfSuspension;

    /*
    // Constructor from EocmsValidOffenceNotice entity - commented out until entity is ready
    public EocmsValidOffenceNoticePlusDto(EocmsValidOffenceNotice entity) {
        this.noticeNo = entity.getNoticeNo();
        this.vehicleNo = entity.getVehicleNo();
        this.offenceNoticeType = entity.getOffenceNoticeType();
        this.eprReasonOfSuspension = entity.getEprReasonOfSuspension();
        this.lastProcessingStage = entity.getLastProcessingStage();
    }
    */
}