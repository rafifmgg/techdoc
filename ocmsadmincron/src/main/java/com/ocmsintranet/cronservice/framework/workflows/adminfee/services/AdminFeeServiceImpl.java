package com.ocmsintranet.cronservice.framework.workflows.adminfee.services;

import com.ocmsintranet.cronservice.framework.workflows.adminfee.helpers.AdminFeeHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for Admin Fee processing operations.
 *
 * This service orchestrates the complete workflow for applying administration fees
 * to foreign vehicle notices that remain unpaid beyond the FOD parameter period.
 *
 * Process Flow:
 * 1. Retrieve FOD (days) and AFO (admin fee amount) parameters
 * 2. Query eligible notices (foreign vehicles, unpaid, past FOD period)
 * 3. Batch update notices with admin fee
 * 4. Prepare and return notices for vHub notification
 *
 * Reference: OCMS 14 Functional Document v1.7, Section 3.7 - Foreign Vehicle Processing
 */
@Slf4j
@Service
public class AdminFeeServiceImpl implements AdminFeeService {

    private final AdminFeeHelper adminFeeHelper;

    public AdminFeeServiceImpl(AdminFeeHelper adminFeeHelper) {
        this.adminFeeHelper = adminFeeHelper;
    }

    /**
     * Execute the admin fee job asynchronously.
     * This method is called by the scheduler and runs the job asynchronously.
     *
     * @return CompletableFuture with processing result
     */
    @Async
    @Override
    public CompletableFuture<ProcessingResult> executeJob() {
        log.info("Starting async admin fee job execution");

        try {
            ProcessingResult result = processAdminFees();
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async admin fee job execution: {}", e.getMessage(), e);
            ProcessingResult errorResult = new ProcessingResult(
                    false,
                    "Error executing admin fee job: " + e.getMessage(),
                    0,
                    0);
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Process admin fee application for eligible foreign vehicle notices.
     *
     * This method orchestrates the complete workflow:
     * 1. Retrieve FOD and AFO parameters from database
     * 2. Query eligible notices using FOD period
     * 3. Batch update notices with AFO admin fee amount
     * 4. Prepare updated notices for vHub notification
     *
     * @return Processing result with success status and message
     */
    @Override
    public ProcessingResult processAdminFees() {
        log.info("Starting admin fee processing workflow");

        List<String> errors = new ArrayList<>();

        try {
            // Step 1: Retrieve FOD parameter (number of days after offence date)
            Integer fodDays = adminFeeHelper.getFodDays();
            if (fodDays == null) {
                String errorMsg = "FOD parameter not found or invalid in database";
                log.error(errorMsg);
                errors.add(errorMsg);
                return new ProcessingResult(false, errorMsg, 0, 0);
            }

            log.info("FOD parameter: {} days", fodDays);

            // Step 2: Retrieve AFO parameter (admin fee amount)
            BigDecimal afoAmount = adminFeeHelper.getAfoAmount();
            if (afoAmount == null) {
                String errorMsg = "AFO parameter not found or invalid in database";
                log.error(errorMsg);
                errors.add(errorMsg);
                return new ProcessingResult(false, errorMsg, 0, 0);
            }

            log.info("AFO parameter: ${}", afoAmount);

            // Step 3: Query eligible notices
            List<Map<String, Object>> eligibleNotices = adminFeeHelper.queryEligibleNotices(fodDays);

            if (eligibleNotices.isEmpty()) {
                String msg = "No eligible notices found for admin fee application";
                log.info(msg);
                return new ProcessingResult(true, msg, 0, 0);
            }

            log.info("Found {} eligible notices for admin fee application", eligibleNotices.size());

            // Step 4: Batch update notices with admin fee
            int updateCount = adminFeeHelper.batchUpdateAdminFee(eligibleNotices, afoAmount);

            log.info("Successfully updated {} of {} notices", updateCount, eligibleNotices.size());

            // Step 5: Prepare notices for vHub notification (if needed)
            List<Map<String, Object>> vhubNotices = adminFeeHelper.prepareVhubNotification(eligibleNotices);

            log.info("Prepared {} notices for vHub notification", vhubNotices.size());

            // TODO: Send to vHub API when vHub integration is ready
            // For now, just log the notices
            for (Map<String, Object> notice : vhubNotices) {
                log.debug("vHub notice: {}", notice);
            }

            // Step 6: Send error notification if there were any errors
            if (updateCount < eligibleNotices.size()) {
                errors.add(String.format("Failed to update %d of %d notices",
                        eligibleNotices.size() - updateCount, eligibleNotices.size()));
            }

            if (!errors.isEmpty()) {
                adminFeeHelper.sendErrorNotification(errors);
            }

            // Return success if at least one notice was updated
            boolean success = updateCount > 0;
            String message = String.format(
                    "Admin fee processing completed. Processed %d notices: %d updated successfully",
                    eligibleNotices.size(), updateCount);

            return new ProcessingResult(success, message, eligibleNotices.size(), updateCount);

        } catch (Exception e) {
            String errorMsg = "Error processing admin fees: " + e.getMessage();
            log.error(errorMsg, e);
            errors.add(errorMsg);
            adminFeeHelper.sendErrorNotification(errors);
            return new ProcessingResult(false, errorMsg, 0, 0);
        }
    }
}
