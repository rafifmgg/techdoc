package com.ocmsintranet.cronservice.framework.dto.ces;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CesOffenceRuleData {
    private final String offenceCode;
    private final String offenceRule;
    private final String offenceDescription;
    private final LocalDateTime effectiveStartDate;
    private final LocalDateTime effectiveEndDate;
    private final BigDecimal defaultFineAmount;
    private final BigDecimal secondaryFineAmount;
    private final String vehicleType;
}
