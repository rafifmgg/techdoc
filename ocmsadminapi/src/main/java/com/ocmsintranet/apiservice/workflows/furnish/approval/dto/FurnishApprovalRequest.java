package com.ocmsintranet.apiservice.workflows.furnish.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for officer approval request.
 * Based on OCMS 41 User Stories 41.15-41.19.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApprovalRequest {

    @NotBlank(message = "Transaction number is required")
    private String txnNo;

    @NotBlank(message = "Officer ID is required")
    private String officerId;

    // Notification preferences (OCMS41.15, 41.16)
    private boolean sendEmailToOwner;
    private boolean sendEmailToFurnished;
    private boolean sendSmsToFurnished;

    // Email template selection (if custom)
    private String emailTemplateId; // null = use default template

    // Custom email content (if officer customizes)
    private String customEmailSubject;
    private String customEmailBody;

    // Custom SMS content (if officer customizes)
    private String customSmsBody;

    // Remarks (optional)
    private String remarks;
}
