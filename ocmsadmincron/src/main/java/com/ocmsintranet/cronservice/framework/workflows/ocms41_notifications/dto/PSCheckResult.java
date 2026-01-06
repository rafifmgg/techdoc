package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for Permanent Suspension check operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PSCheckResult {
    private boolean success;
    private String message;
    private int totalChecked;
    private int suspendedNotices;
    private int rejectedApplications;
}
