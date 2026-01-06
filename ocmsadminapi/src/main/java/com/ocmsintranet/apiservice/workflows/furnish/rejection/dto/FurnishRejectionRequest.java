package com.ocmsintranet.apiservice.workflows.furnish.rejection.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for officer rejection request.
 * Based on OCMS 41 User Stories 41.24-41.28.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishRejectionRequest {

    @NotBlank(message = "Transaction number is required")
    private String txnNo;

    @NotBlank(message = "Officer ID is required")
    private String officerId;

    // Notification preferences (OCMS41.24, 41.25)
    private boolean sendEmailToOwner;

    // Email template selection (OCMS41.25)
    private String emailTemplateId; // null = use default template
    // Options:
    // - REJECTED_DOCS_REQUIRED (Template 4)
    // - REJECTED_MULTIPLE_HIRERS (Template 5)
    // - REJECTED_RENTAL_DISCREPANCY (Template 6)
    // - REJECTED_GENERAL (Template 7)

    // Custom email content (if officer customizes)
    private String customEmailSubject;
    private String customEmailBody;

    // Rejection reason (OCMS41.27)
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    // Remarks (optional)
    private String remarks;
}
