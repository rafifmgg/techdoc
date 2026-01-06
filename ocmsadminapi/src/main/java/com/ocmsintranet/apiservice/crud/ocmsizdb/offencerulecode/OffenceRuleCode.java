package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    
    @Id
    @Column(name = "composition_amount", nullable = false, precision = 19, scale = 2)
    @JsonSerialize(using = ToStringSerializer.class)
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
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal secondaryFineAmount;
}