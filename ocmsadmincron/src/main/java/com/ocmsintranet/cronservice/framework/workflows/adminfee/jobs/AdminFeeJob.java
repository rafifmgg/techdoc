package com.ocmsintranet.cronservice.framework.workflows.adminfee.jobs;

import com.ocmsintranet.cronservice.framework.common.CronJobFramework.TrackedCronJobTemplate;
import com.ocmsintranet.cronservice.framework.workflows.adminfee.services.AdminFeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Admin Fee Job.
 *
 * This job applies administration fees to foreign vehicle notices that remain unpaid
 * beyond the FOD (Furnish Offender Details) parameter period.
 *
 * Process Flow:
 * 1. Query FOD and AFO parameters from database
 * 2. Find eligible foreign vehicle notices (PS-FOR, unpaid, past FOD period)
 * 3. Apply admin fee (AFO amount) to eligible notices
 * 4. Update amount_payable = composition_amount + administration_fee
 * 5. Send updated composition amounts to vHub
 *
 * Eligibility Criteria:
 * - vehicle_registration_type = 'F' (Foreign)
 * - suspension_type = 'PS' (Permanent Suspension)
 * - epr_reason_of_suspension = 'FOR' (Foreign)
 * - amount_paid = 0 or NULL (unpaid)
 * - administration_fee = 0 or NULL (not yet applied)
 * - offence_date + FOD days <= today
 *
 * Reference: OCMS 14 Functional Document v1.7, Section 3.7 - Foreign Vehicle Processing
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("apply_admin_fee")
public class AdminFeeJob extends TrackedCronJobTemplate {

    private final AdminFeeService adminFeeService;

    @Value("${cron.adminfee.shedlock.name:apply_admin_fee}")
    private String jobName;

    public AdminFeeJob(AdminFeeService adminFeeService) {
        this.adminFeeService = adminFeeService;
    }

    @Override
    protected String getJobName() {
        return jobName;
    }

    @Override
    protected boolean validatePreConditions() {
        log.info("Validating pre-conditions for admin fee job");

        try {
            // Check if required dependencies are initialized
            if (adminFeeService == null) {
                log.error("AdminFeeService is not initialized");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating pre-conditions: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void initialize() {
        // Call parent initialize which records job start in history
        super.initialize();
        log.info("Admin fee job initialized successfully");
    }

    @Override
    protected void cleanup() {
        // Clean up any temporary resources
        log.info("Admin fee job cleanup completed");
        super.cleanup();
    }

    @Override
    protected JobResult doExecute() {
        log.info("Starting admin fee job execution");

        try {
            // Process admin fees using the service
            AdminFeeService.ProcessingResult result = adminFeeService.processAdminFees();

            // Log the result
            log.info("Admin fee processing result: {}", result.getMessage());
            log.info("Notices processed: {}, Notices updated: {}",
                    result.getNoticesProcessed(), result.getNoticesUpdated());

            // Return job result
            return new JobResult(
                    result.isSuccess(),
                    String.format("Admin Fee Job: %s (Processed: %d, Updated: %d)",
                            result.getMessage(),
                            result.getNoticesProcessed(),
                            result.getNoticesUpdated()));

        } catch (Exception e) {
            String errorMsg = "Error executing admin fee job: " + e.getMessage();
            log.error(errorMsg, e);
            return new JobResult(false, errorMsg);
        }
    }
}
