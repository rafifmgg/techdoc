package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import java.util.List;

/**
 * Service interface for Unclaimed Reminders processing
 */
public interface UnclaimedService {

    /**
     * Check and validate notice numbers, retrieve notice and reminder details
     * @param noticeNumbers List of notice numbers to check
     * @return List of UnclaimedReminderDto with notice details
     */
    List<UnclaimedReminderDto> checkUnclaimedNotices(List<String> noticeNumbers);

    /**
     * Process unclaimed reminder submissions
     * - Apply TS-UNC suspension to notices
     * - Update notice processing stages
     * @param unclaimedRecords List of unclaimed reminder records
     * @return List of processing results
     */
    List<UnclaimedProcessingResult> processUnclaimedReminders(List<UnclaimedReminderDto> unclaimedRecords);
}
