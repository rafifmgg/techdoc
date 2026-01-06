package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for email notification operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationResult {
    private boolean success;
    private String message;
    private int totalPendingApplications;
    private int emailsSent;
    private int emailsFailed;
}
