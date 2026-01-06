package com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating payment information
 * This DTO handles the payment update operation which:
 * 1. Updates VON and eVON with payment and suspension details
 * 2. Creates suspended notice record
 * 3. Creates web transaction audit record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUpdateRequest {

    @NotBlank(message = "Notice number is required")
    @JsonProperty("noticeNo")
    private String noticeNo;

    @NotBlank(message = "Payment mode is required")
    @JsonProperty("paymentMode")
    private String paymentMode;

    @NotNull(message = "Transaction date and time is required")
    @JsonProperty("transactionDateAndTime")
    private LocalDateTime transactionDateAndTime;

    @JsonProperty("remarks")
    private String remarks;

    @NotNull(message = "Amount paid is required")
    @JsonProperty("amountPaid")
    private BigDecimal amountPaid;
}
