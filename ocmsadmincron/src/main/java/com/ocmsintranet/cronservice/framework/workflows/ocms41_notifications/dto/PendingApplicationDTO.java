package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for pending furnish applications requiring officer approval
 * Used in daily notification emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApplicationDTO {
    private String noticeNo;
    private String vehicleNo;
    private LocalDateTime offenceDate;
    private String currentProcessingStage;
    private String status; // P=Pending, S=Resubmission
    private LocalDateTime submissionReceivedDate;
    private Integer workingDaysPending;
    private String furnishName;
    private String furnishIdNo;
    private String ownerName;
    private String ownerIdNo;
}
