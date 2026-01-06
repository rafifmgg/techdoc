package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecodehistory;

import com.ocmsintranet.apiservice.crud.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ocmsintranet.apiservice.crud.annotations.NumberAble;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_offence_rule_code_history")
@Setter
@Getter
@NoArgsConstructor
public class OffenceRuleCodeHistory extends BaseEntity {
    @NotBlank
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NumberAble
    @Column(name = "offence_rule_code_history_id", nullable = false)
    private Integer offenceRuleCodeHistoryId;

    @Column(name = "action", length = 8)
    private String action;

    @Column(name = "computer_rule_code")
    private Integer computerRuleCode;

    @Column(name = "effective_start_date")
    private LocalDateTime effectiveStartDate;

    @Column(name = "vehicle_category", length = 1)
    private String vehicleCategory;

    @Column(name = "complaint_code", length = 8)
    private String complaintCode;

    @Column(name = "composition_amount", precision = 19, scale = 2)
    private BigDecimal compositionAmount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "effective_end_date")
    private LocalDateTime effectiveEndDate;

    @Column(name = "offence_type", length = 1)
    private String offenceType;

    @Column(name = "rule_no", length = 5)
    private String ruleNo;

    @Column(name = "secondary_fine_amount", precision = 19, scale = 2)
    private BigDecimal secondaryFineAmount;
}
