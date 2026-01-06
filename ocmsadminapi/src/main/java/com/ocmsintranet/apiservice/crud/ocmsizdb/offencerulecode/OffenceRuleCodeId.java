package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Composite ID class for OffenceRuleCode entity
 */
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OffenceRuleCodeId implements Serializable {

   private Integer computerRuleCode;
   private LocalDateTime effectiveStartDate;
   private String vehicleCategory;
   private BigDecimal compositionAmount;
   
    // Equals and hashCode methods are provided by Lombok's @Data
}