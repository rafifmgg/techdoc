package com.ocmsintranet.cronservice.framework.workflows.adminfee.services;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Admin Fee processing operations.
 *
 * This service handles the business logic for applying administration fees
 * to foreign vehicle notices that remain unpaid beyond the FOD parameter period.
 */
public interface AdminFeeService {

    /**
     * Execute the admin fee job asynchronously.
     * This method is called by the scheduler.
     *
     * @return CompletableFuture with processing result
     */
    CompletableFuture<ProcessingResult> executeJob();

    /**
     * Process admin fee application for eligible foreign vehicle notices.
     *
     * This method orchestrates the complete workflow:
     * 1. Retrieve FOD and AFO parameters
     * 2. Query eligible notices
     * 3. Apply admin fees
     * 4. Send updates to vHub
     *
     * @return Processing result with success status and message
     */
    ProcessingResult processAdminFees();

    /**
     * Result object for admin fee processing.
     */
    class ProcessingResult {
        private final boolean success;
        private final String message;
        private final int noticesProcessed;
        private final int noticesUpdated;

        public ProcessingResult(boolean success, String message, int noticesProcessed, int noticesUpdated) {
            this.success = success;
            this.message = message;
            this.noticesProcessed = noticesProcessed;
            this.noticesUpdated = noticesUpdated;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getNoticesProcessed() {
            return noticesProcessed;
        }

        public int getNoticesUpdated() {
            return noticesUpdated;
        }
    }
}
