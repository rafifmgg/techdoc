package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for reduction request from external systems (e.g. PLUS).
 * Contains JSR-380 bean validation annotations for basic format validation.
 * Business validation is performed separately in ReductionValidator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReductionRequest {

    /**
     * Notice number to be reduced (mandatory)
     */
    @NotBlank(message = "Notice number is required")
    private String noticeNo;

    /**
     * Amount reduced from the original composition amount (mandatory)
     */
    @NotNull(message = "Amount reduced is required")
    @Positive(message = "Amount reduced must be positive")
    private BigDecimal amountReduced;

    /**
     * New amount payable after reduction (mandatory)
     */
    @NotNull(message = "Amount payable is required")
    private BigDecimal amountPayable;

    /**
     * Date of reduction (mandatory)
     */
    @NotNull(message = "Date of reduction is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateOfReduction;

    /**
     * Expiry date of the reduction offer (mandatory)
     */
    @NotNull(message = "Expiry date of reduction is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiryDateOfReduction;

    /**
     * Reason for the reduction (mandatory)
     */
    @NotBlank(message = "Reason of reduction is required")
    private String reasonOfReduction;

    /**
     * Officer authorizing the reduction (mandatory)
     */
    @NotBlank(message = "Authorised officer is required")
    private String authorisedOfficer;

    /**
     * Source system code for suspension (mandatory)
     * Example: "005" for PLUS system
     */
    @NotBlank(message = "Suspension source is required")
    private String suspensionSource;

    /**
     * Optional remarks for the reduction
     */
    private String remarks;
}
