package com.ocmsintranet.cronservice.crud.ocmsizdb.offencerulecode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Composite ID class for OffenceRuleCode entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffenceRuleCodeId implements Serializable {
    
    private Integer computerRuleCode;
    private LocalDateTime effectiveStartDate;
    private String vehicleCategory;
    
    // Equals and hashCode methods are provided by Lombok's @Data
}