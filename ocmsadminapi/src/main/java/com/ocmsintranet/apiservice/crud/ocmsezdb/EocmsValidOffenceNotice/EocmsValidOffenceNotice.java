package com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the eocms_valid_offence_notice table (eVON).
 * This is the mirror table in the EOCMS (Internet) database.
 * It maintains the same structure as ocms_valid_offence_notice but in the external database.
 */
@Entity
@Table(name = "eocms_valid_offence_notice", schema = "ocmsezmgr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EocmsValidOffenceNotice extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "vehicle_no", length = 12, nullable = false)
    private String vehicleNo;

    @Column(name = "an_flag", length = 1)
    private String anFlag;

    @Column(name = "notice_date_and_time", nullable = false)
    private LocalDateTime noticeDateAndTime;

    @Column(name = "amount_payable", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amountPayable;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amountPaid;

    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;

    @Column(name = "pp_name", length = 100)
    private String ppName;

    @Column(name = "payment_acceptance_allowed", length = 1)
    private String paymentAcceptanceAllowed;

    @Column(name = "payment_status", length = 2)
    private String paymentStatus;

    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    @Column(name = "next_processing_stage", length = 3, nullable = false)
    private String nextProcessingStage;

    @Column(name = "vehicle_registration_type", length = 1)
    private String vehicleRegistrationType;

    @Column(name = "suspension_type", length = 2)
    private String suspensionType;

    @Column(name = "crs_reason_of_suspension", length = 3)
    private String crsReasonOfSuspension;

    @Column(name = "crs_date_of_suspension")
    private LocalDateTime crsDateOfSuspension;

    @Column(name = "epr_reason_of_suspension", length = 3)
    private String eprReasonOfSuspension;

    @Column(name = "epr_date_of_suspension")
    private LocalDateTime eprDateOfSuspension;

    @Column(name = "offence_notice_type", length = 1, nullable = false)
    private String offenceNoticeType;

    @Column(name = "eservice_message_code", length = 3)
    private String eserviceMessageCode;

    @Builder.Default
    @Column(name = "is_sync", length = 1)
    private String isSync = "N";
}
