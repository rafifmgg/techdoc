package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.EmailNotificationResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PSCheckResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PendingApplicationDTO;

import java.util.List;

/**
 * Service interface for OCMS 41 notification workflows
 */
public interface Ocms41NotificationService {

    /**
     * Send daily email notification to officers about pending applications
     * Based on OCMS 41 User Story 41.8 and 41.35
     *
     * @return EmailNotificationResult with details of the notification sent
     */
    EmailNotificationResult sendDailyOfficerNotification();

    /**
     * Check for permanent suspensions (PS) on notices with pending applications
     * Auto-reject applications if notice is permanently suspended
     *
     * @return PSCheckResult with details of the check performed
     */
    PSCheckResult checkPermanentSuspensions();

    /**
     * Get list of pending applications requiring manual approval
     * Status: P (Pending) or S (Resubmission)
     *
     * @return List of PendingApplicationDTO
     */
    List<PendingApplicationDTO> getPendingApplications();

    /**
     * Calculate working days between two dates (excluding weekends and public holidays)
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Number of working days
     */
    int calculateWorkingDays(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}
