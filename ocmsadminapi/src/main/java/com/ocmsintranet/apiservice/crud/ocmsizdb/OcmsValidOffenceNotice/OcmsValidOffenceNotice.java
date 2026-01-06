package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ocms_valid_offence_notice", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsValidOffenceNotice extends BaseEntity {

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "composition_amount", precision = 19, scale = 2, nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal compositionAmount;

    @Column(name = "computer_rule_code", nullable = false)
    private Integer computerRuleCode;

    @Column(name = "crs_date_of_suspension")
    private LocalDateTime crsDateOfSuspension;

    @Column(name = "crs_reason_of_suspension", length = 3)
    private String crsReasonOfSuspension;

    @Column(name = "due_date_of_revival")
    private LocalDateTime dueDateOfRevival;

    @Column(name = "epr_date_of_suspension")
    private LocalDateTime eprDateOfSuspension;

    @Column(name = "epr_reason_of_suspension", length = 3)
    private String eprReasonOfSuspension;

    @Column(name = "last_processing_date", nullable = false)
    private LocalDateTime lastProcessingDate;

    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    @Column(name = "next_processing_date")
    private LocalDateTime nextProcessingDate;

    @Column(name = "next_processing_stage", length = 3)
    private String nextProcessingStage;

    @Column(name = "notice_date_and_time", nullable = false)
    private LocalDateTime noticeDateAndTime;

    @Column(name = "offence_notice_type", length = 1, nullable = false)
    private String offenceNoticeType;

    @Column(name = "offence_type", length = 5)
    private String offenceType;

    @Column(name = "offence_time")
    private LocalDateTime offenceTime;

    @Column(name = "suspension_status", length = 1)
    private String suspensionStatus;

    @Column(name = "parking_lot_no", length = 5)
    private String parkingLotNo;

    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;

    @Column(name = "pp_name", length = 100, nullable = false)
    private String ppName;

    @Column(name = "prev_processing_date")
    private LocalDateTime prevProcessingDate;

    @Column(name = "prev_processing_stage", length = 3)
    private String prevProcessingStage;

    @Column(name = "suspension_type", length = 2)
    private String suspensionType;

    @Column(name = "vehicle_category", length = 1, nullable = false)
    private String vehicleCategory;

    @Column(name = "vehicle_no", length = 14, nullable = false)
    private String vehicleNo;

    @Column(name = "vehicle_registration_type", length = 1)
    private String vehicleRegistrationType;

    @Column(name = "subsystem_label", length = 8)
    private String subsystemLabel;

    @Column(name = "warden_no", length = 5)
    private String wardenNo;

    @Column(name = "rep_charge_amount", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal repChargeAmount;
 
    @Column(name = "administration_fee", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal administrationFee;

// COMMENTED OUT: Fields present in Java class but NOT available in SQL table
//    @Column(name = "advice_surcharge", precision = 19, scale = 2)
//    @JsonSerialize(using = ToStringSerializer.class)
//    private BigDecimal adviceSurcharge;
//
   @Column(name = "amount_paid", precision = 19, scale = 2)
   @JsonSerialize(using = ToStringSerializer.class)
   private BigDecimal amountPaid;

    @Column(name = "amount_payable", precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amountPayable;

    @Column(name = "an_flag", length = 1)
    private String anFlag;
//
//    @Column(name = "assign_date")
//    private LocalDateTime assignDate;
//
//    @Column(name = "assign_officer", length = 50)
//    private String assignOfficer;
//
//    @Column(name = "decision", length = 1)
//    private String decision;
//
//    @Column(name = "decision_date")
//    private LocalDateTime decisionDate;
//
//    @Column(name = "decision_officer", length = 50)
//    private String decisionOfficer;
//
//    @Column(name = "decision_remark", length = 50)
//    private String decisionRemark;
//
//    @Column(name = "incentive_payt_amt", precision = 19, scale = 2)
//    @JsonSerialize(using = ToStringSerializer.class)
//    private BigDecimal incentivePaytAmt;
//
//    @Column(name = "incentive_payt_date")
//    private LocalDateTime incentivePaytDate;
//
//    @Column(name = "last_rdp_dn_date")
//    private LocalDateTime lastRdpDnDate;
//
//    @Column(name = "last_rdp_dn_stage", length = 3)
//    private String lastRdpDnStage;
//
    @Column(name = "other_remark", length = 200)
    private String otherRemark;

    @Column(name = "is_sync", length = 1)
    private String isSync = "N";

    @Column(name = "eservice_message_code", length = 3)
    private String eserviceMessageCode;
//
//    @Column(name = "parking_fee", precision = 19, scale = 2)
//    @JsonSerialize(using = ToStringSerializer.class)
//    private BigDecimal parkingFee;

    @Column(name = "payment_acceptance_allowed", length = 1)
    private String paymentAcceptanceAllowed;

    @Column(name = "payment_due_date")
    private LocalDateTime paymentDueDate;

    @Column(name = "payment_status", length = 2)
    private String paymentStatus;
//
//    @Column(name = "rule_remarks", length = 65)
//    private String ruleRemarks;
//
//    @Column(name = "surcharge", precision = 19, scale = 2)
//    @JsonSerialize(using = ToStringSerializer.class)
//    private BigDecimal surcharge;
//
//    @Column(name = "susp_type_update_allowed", length = 1)
//    private String suspTypeUpdateAllowed;
//
//    @Column(name = "vehicle_owner_type", length = 5)
//    private String vehicleOwnerType;
//
//    @Column(name = "sent_to_rep_date")
//    private LocalDateTime sentToRepDate;
//

    // ============================================================================
    // HELPER METHODS - Semantic Aliases
    // ============================================================================

    /**
     * Get the current processing stage of this notice.
     *
     * <p><strong>IMPORTANT:</strong> This is a semantic alias for {@link #getLastProcessingStage()}.
     * The database field is named "last_processing_stage" for historical/legacy reasons,
     * but it actually represents the CURRENT processing stage of the notice.</p>
     *
     * <p><strong>Naming Rationale:</strong></p>
     * <ul>
     *   <li><strong>Database:</strong> {@code last_processing_stage} (OCMS legacy convention)</li>
     *   <li><strong>Business Logic:</strong> The "last" stage we recorded IS the "current" state</li>
     *   <li><strong>API/DTO:</strong> {@code currentProcessingStage} (clearer for API consumers)</li>
     * </ul>
     *
     * <p>This method provides a more intuitive name when working with service/controller layers
     * and DTOs, avoiding confusion about whether this represents historical or current state.</p>
     *
     * @return the current processing stage code (e.g., "ISS", "CRE", "NRO")
     * @see #getLastProcessingStage()
     * @see #setCurrentProcessingStage(String)
     */
    public String getCurrentProcessingStage() {
        return this.lastProcessingStage;
    }

    /**
     * Set the current processing stage of this notice.
     *
     * <p><strong>IMPORTANT:</strong> This is a semantic alias for {@link #setLastProcessingStage(String)}.
     * See {@link #getCurrentProcessingStage()} for detailed explanation of the naming.</p>
     *
     * @param stage the new processing stage code (e.g., "ISS", "CRE", "NRO")
     * @see #setLastProcessingStage(String)
     * @see #getCurrentProcessingStage()
     */
    public void setCurrentProcessingStage(String stage) {
        this.lastProcessingStage = stage;
    }

    /**
     * Get the processing stage (generic alias).
     *
     * <p><strong>IMPORTANT:</strong> This is another alias for {@link #getLastProcessingStage()}.
     * Use {@link #getCurrentProcessingStage()} for clearer semantics in new code.</p>
     *
     * @return the processing stage code
     * @see #getCurrentProcessingStage()
     * @see #getLastProcessingStage()
     */
    public String getProcessingStage() {
        return this.lastProcessingStage;
    }

    /**
     * Set the processing stage (generic alias).
     *
     * <p><strong>IMPORTANT:</strong> This is another alias for {@link #setLastProcessingStage(String)}.
     * Use {@link #setCurrentProcessingStage(String)} for clearer semantics in new code.</p>
     *
     * @param stage the new processing stage code
     * @see #setCurrentProcessingStage(String)
     * @see #setLastProcessingStage(String)
     */
    public void setProcessingStage(String stage) {
        this.lastProcessingStage = stage;
    }
}
