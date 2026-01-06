package com.ocmsintranet.cronservice.crud.ocmsizdb.offencerulecode;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OffenceRuleCode entity
 * Represents offence rules with their codes and fine amounts
 */
@Entity
@Table(name = "ocms_offence_rule_code", schema = "ocmsizmgr")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@IdClass(OffenceRuleCodeId.class)
public class OffenceRuleCode extends BaseEntity {
 
    @Id
    @Column(name = "computer_rule_code", nullable = false)
    private Integer computerRuleCode;
    
    @Id
    @Column(name = "effective_start_date", nullable = false)
    private LocalDateTime effectiveStartDate;
    
    @Id
    @Column(name = "vehicle_category", nullable = false, length = 1)
    private String vehicleCategory;
    
    @Column(name = "complaint_code", nullable = false, length = 8)
    private String complaintCode;
    
    @Column(name = "composition_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal compositionAmount;
    
    @Column(name = "description", nullable = false, length = 255)
    private String description;
    
    @Column(name = "effective_end_date", nullable = false)
    private LocalDateTime effectiveEndDate;
    
    @Column(name = "offence_type", nullable = false, length = 1)
    private String offenceType;
    
    @Column(name = "rule_no", nullable = false, length = 5)
    private String ruleNo;
    
    @Column(name = "secondary_fine_amount", precision = 19, scale = 2)
    private BigDecimal secondaryFineAmount;
}