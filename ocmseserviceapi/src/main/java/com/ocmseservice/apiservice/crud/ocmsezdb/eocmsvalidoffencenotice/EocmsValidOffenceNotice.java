package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eocms_valid_offence_notice", schema = "ocmsezmgr")
@Getter
@Setter
public class EocmsValidOffenceNotice extends BaseEntity {
    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;
    
    @Column(name = "amount_paid", precision = 19, scale = 2)
    private BigDecimal amountPaid;
    
    @Column(name = "amount_payable", precision = 19, scale = 2)
    private BigDecimal amountPayable;
    
    @Column(name = "crs_date_of_suspension")
    private LocalDateTime crsDateOfSuspension;
    
    @Column(name = "crs_reason_of_suspension", length = 3)
    private String crsReasonOfSuspension;
    
    @Column(name = "epr_date_of_suspension")
    private LocalDateTime eprDateOfSuspension;
    
    @Column(name = "epr_reason_of_suspension", length = 3)
    private String eprReasonOfSuspension;
    
    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;
    
    @Column(name = "next_processing_stage", length = 3)
    private String nextProcessingStage;
    
    @Column(name = "an_flag", length = 1)
    private String anFlag;
    
    @Column(name = "carpark_name", length = 100)
    private String carparkName;
    
    @Column(name = "notice_date_and_time", nullable = false)
    private LocalDateTime noticeDateAndTime;
    
    @Column(name = "offence_notice_type", length = 1, nullable = false)
    private String offenceNoticeType;
    
    @Column(name = "payment_acceptance_allowed", length = 1)
    private String paymentAcceptanceAllowed;
    
    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;
    
    @Column(name = "suspension_type", length = 2)
    private String suspensionType;
    
    @Column(name = "vehicle_no", length = 12, nullable = false)
    private String vehicleNo;
    
    @Column(name = "vehicle_registration_type", length = 1)
    private String vehicleRegistrationType;
    
    @Column(name = "refund_identified_date")
    private LocalDateTime refundIdentifiedDate;
    
    @Column(name = "is_sync", nullable = false)
    private Boolean isSync;
}

