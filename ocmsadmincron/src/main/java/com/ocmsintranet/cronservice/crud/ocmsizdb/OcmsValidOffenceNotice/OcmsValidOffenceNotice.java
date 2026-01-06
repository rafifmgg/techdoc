package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_valid_offence_notice table.
 * This table is a core table in the OCMS system that stores information about valid offence notices.
 * It maintains detailed records of parking offences, including vehicle information, processing stages,
 * payment details, and suspension information.
 */
@Entity
@Table(name = "ocms_valid_offence_notice", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsValidOffenceNotice extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "administration_fee", precision = 19, scale = 2)
    private BigDecimal administrationFee;

    // NOT USED NOW
    // @Column(name = "advice_surcharge", precision = 19, scale = 2)
    // private BigDecimal adviceSurcharge;


     @Column(name = "amount_paid", precision = 19, scale = 2)
     private BigDecimal amountPaid;

    @Column(name = "amount_payable", precision = 19, scale = 2)
    private BigDecimal amountPayable;

    @Column(name = "an_flag", length = 1)
    private String anFlag;

    // NOT USED NOW
    // @Column(name = "assign_date")
    // private LocalDateTime assignDate;

    // NOT USED NOW
    // @Column(name = "assign_officer", length = 50)
    // private String assignOfficer;

    @Column(name = "composition_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal compositionAmount;

    @Column(name = "computer_rule_code", nullable = false)
    private Integer computerRuleCode;

    @Column(name = "crs_date_of_suspension")
    private LocalDateTime crsDateOfSuspension;

    @Column(name = "crs_reason_of_suspension", length = 3)
    private String crsReasonOfSuspension;

    // NOT USED NOW
    // @Column(name = "decision", length = 1)
    // private String decision;

    // NOT USED NOW
    // @Column(name = "decision_date")
    // private LocalDateTime decisionDate;

    // NOT USED NOW
    // @Column(name = "decision_officer", length = 50)
    // private String decisionOfficer;

    // NOT USED NOW
    // @Column(name = "decision_remark", length = 50)
    // private String decisionRemark;

    @Column(name = "due_date_of_revival")
    private LocalDateTime dueDateOfRevival;

    @Column(name = "epr_date_of_suspension")
    private LocalDateTime eprDateOfSuspension;

    @Column(name = "epr_reason_of_suspension", length = 3)
    private String eprReasonOfSuspension;

    // NOT USED NOW
    // @Column(name = "incentive_payt_amt", precision = 19, scale = 2)
    // private BigDecimal incentivePaytAmt;

    // NOT USED NOW
    // @Column(name = "incentive_payt_date")
    // private LocalDateTime incentivePaytDate;

    @Column(name = "last_processing_date", nullable = false)
    private LocalDateTime lastProcessingDate;

    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    // NOT USED NOW
    // @Column(name = "last_rdp_dn_date")
    // private LocalDateTime lastRdpDnDate;

    // NOT USED NOW
    // @Column(name = "last_rdp_dn_stage", length = 3)
    // private String lastRdpDnStage;

    @Column(name = "next_processing_date")
    private LocalDateTime nextProcessingDate;

    @Column(name = "next_processing_stage", length = 3)
    private String nextProcessingStage;

    @Column(name = "notice_date_and_time", nullable = false)
    private LocalDateTime noticeDateAndTime;

    @Column(name = "offence_notice_type", length = 1, nullable = false)
    private String offenceNoticeType;

    @Column(name = "other_remark", length = 200)
    private String otherRemark;

    // NOT USED NOW
    // @Column(name = "parking_fee", precision = 19, scale = 2)
    // private BigDecimal parkingFee;

    @Column(name = "parking_lot_no", length = 5)
    private String parkingLotNo;

    @Column(name = "payment_acceptance_allowed", length = 1)
    private String paymentAcceptanceAllowed;

    @Column(name = "payment_due_date")
    private LocalDateTime paymentDueDate;

    @Column(name = "payment_status", length = 2)
    private String paymentStatus;

    @Column(name = "is_sync", length = 1)
    private String isSync = "N";

    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;

    @Column(name = "pp_name", length = 100, nullable = false)
    private String ppName;

    @Column(name = "prev_processing_date")
    private LocalDateTime prevProcessingDate;

    @Column(name = "prev_processing_stage", length = 3)
    private String prevProcessingStage;

    // NOT USED NOW
    // @Column(name = "rule_remarks", length = 65)
    // private String ruleRemarks;

    // NOT USED NOW
    // @Column(name = "surcharge", precision = 19, scale = 2)
    // private BigDecimal surcharge;

    // NOT USED NOW
    // @Column(name = "susp_type_update_allowed", length = 1)
    // private String suspTypeUpdateAllowed;

    @Column(name = "suspension_type", length = 2)
    private String suspensionType;

    @Column(name = "vehicle_category", length = 1, nullable = false)
    private String vehicleCategory;

    @Column(name = "vehicle_no", length = 14, nullable = false)
    private String vehicleNo;

    // NOT USED NOW
    // @Column(name = "vehicle_owner_type", length = 5)
    // private String vehicleOwnerType;

    @Column(name = "vehicle_registration_type", length = 1)
    private String vehicleRegistrationType;

    // NOT USED NOW
    // @Column(name = "sent_to_rep_date")
    // private LocalDateTime sentToRepDate;

    @Column(name = "subsystem_label", length = 50)
    private String subsystemLabel;

    @Column(name = "warden_no", length = 5)
    private String wardenNo;

    @Column(name = "rep_charge_amount", precision = 19, scale = 2)
    private BigDecimal repChargeAmount;

    @Column(name = "eservice_message_code", length = 3)
    private String eserviceMessageCode;
}