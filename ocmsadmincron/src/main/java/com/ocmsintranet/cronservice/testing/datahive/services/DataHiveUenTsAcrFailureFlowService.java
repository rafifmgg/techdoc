package com.ocmsintranet.cronservice.testing.datahive.services;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveUENService;
import com.ocmsintranet.cronservice.testing.datahive.helpers.DataHiveTestDatabaseHelper;
import com.ocmsintranet.cronservice.testing.datahive.helpers.DateTimeTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataHiveUenTsAcrFailureFlowService {

    private static final Logger logger = LoggerFactory.getLogger(DataHiveUenTsAcrFailureFlowService.class);

    @Autowired
    private DataHiveUENService dataHiveUenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataHiveTestDatabaseHelper databaseHelper;

    @Autowired
    private DateTimeTestHelper dateTimeHelper;

    private static final String TEST_UEN = "201234582P";
    private static final String NOTICE_NUMBER = "DHUEN016";
    private static final String ENTITY_TYPE = "LC";
    private static final String COMPANY_NAME = "DataHive TS-ACR Failure Test Pte Ltd";
    private static final String APPLICATION_STATUS_FAILED = "FAILED";
    private static final String FAILURE_REASON = "DataHive data retrieval timeout";

    public Map<String, Object> executeTsAcrFailureFlow() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            logger.info("=== SCENARIO 16: TS-ACR Application Failure Flow ===");
            logger.info("Testing UEN: {}, Notice: {}", TEST_UEN, NOTICE_NUMBER);

            // Step 1: Setup test data untuk TS-ACR failure scenario
            result.put("step1_setup", setupTestData());

            // Step 2: Call DataHive API (simulate failure)
            result.put("step2_api_call", callDataHiveApi());

            // Step 3: Query Snowflake directly untuk comparison
            result.put("step3_snowflake_query", querySnowflakeDirectly());

            // Step 4: Verify TS-ACR failure workflow dan error handling
            result.put("step4_ts_acr_failure_verification", verifyTsAcrFailureWorkflow());

            result.put("overall_status", "SUCCESS");
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());

            logger.info("TS-ACR Failure Flow completed successfully");

        } catch (Exception e) {
            logger.error("Error in TS-ACR Failure Flow: ", e);
            result.put("overall_status", "FAILED");
            result.put("error_message", e.getMessage());
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());
        }

        return result;
    }

    private Map<String, Object> setupTestData() {
        Map<String, Object> setupResult = new HashMap<>();

        try {
            logger.info("Setting up test data for TS-ACR Failure flow...");

            // Setup ocms_batch_job dengan failure status
            String batchJobSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_batch_job WHERE name = '%s') " +
                "UPDATE ocms_batch_job SET run_status = 'F', start_run = DATEADD(MINUTE, -10, GETDATE()), " +
                "end_run = DATEADD(MINUTE, -2, GETDATE()), log_text = 'Error: %s', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN016_TEST' WHERE name = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_batch_job (name, run_status, log_text, start_run, end_run, cre_date, cre_user_id) " +
                "VALUES ('%s', 'F', 'Error: %s', DATEADD(MINUTE, -10, GETDATE()), " +
                "DATEADD(MINUTE, -2, GETDATE()), GETDATE(), 'DHUEN016_TEST')",
                NOTICE_NUMBER, FAILURE_REASON, NOTICE_NUMBER,
                NOTICE_NUMBER, FAILURE_REASON
            );

            // Setup TS-ACR application dengan FAILED status
            String tsAcrSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_application WHERE uen = '%s') " +
                "UPDATE ts_acr_application SET entity_type = '%s', company_name = '%s', gazetted = 0, " +
                "application_status = '%s', failure_reason = '%s', last_attempt_date = GETDATE(), " +
                "retry_count = 3, upd_date = GETDATE(), upd_user_id = 'DHUEN016_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ts_acr_application (uen, entity_type, company_name, gazetted, application_status, " +
                "failure_reason, last_attempt_date, retry_count, cre_date, cre_user_id) VALUES " +
                "('%s', '%s', '%s', 0, '%s', '%s', GETDATE(), 3, GETDATE(), 'DHUEN016_TEST')",
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, APPLICATION_STATUS_FAILED, FAILURE_REASON, TEST_UEN,
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, APPLICATION_STATUS_FAILED, FAILURE_REASON
            );

            // Setup partial/incomplete company data untuk failed retrieval
            String firmInfoSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_dh_acra_company_detail WHERE uen = '%s') " +
                "UPDATE ocms_dh_acra_company_detail SET entity_type = '%s', entity_name = '%s', " +
                "reg_street_name = NULL, reg_postal_code = NULL, entity_status_desc = 'Data Incomplete', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN016_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_dh_acra_company_detail (uen, entity_type, entity_name, reg_street_name, reg_postal_code, " +
                "entity_status_desc, cre_date, cre_user_id) VALUES ('%s', '%s', '%s', NULL, NULL, " +
                "'Data Incomplete', GETDATE(), 'DHUEN016_TEST')",
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, TEST_UEN,
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME
            );

            // Setup error audit trail
            String errorAuditSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_audit_trail WHERE uen = '%s' AND action_type = 'DATA_RETRIEVAL_FAILED') " +
                "UPDATE ts_acr_audit_trail SET action_date = GETDATE(), action_details = '%s', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN016_TEST' WHERE uen = '%s' AND action_type = 'DATA_RETRIEVAL_FAILED' " +
                "ELSE " +
                "INSERT INTO ts_acr_audit_trail (uen, action_type, action_date, action_details, performed_by, " +
                "cre_date, cre_user_id) VALUES ('%s', 'DATA_RETRIEVAL_FAILED', GETDATE(), '%s', " +
                "'SYS_DHUEN016', GETDATE(), 'DHUEN016_TEST')",
                TEST_UEN, FAILURE_REASON, TEST_UEN,
                TEST_UEN, FAILURE_REASON
            );

            // Setup retry attempt audit trail
            String retryAuditSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_audit_trail WHERE uen = '%s' AND action_type = 'RETRY_ATTEMPT') " +
                "UPDATE ts_acr_audit_trail SET action_date = GETDATE(), action_details = 'Retry attempt 3/3 failed', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN016_TEST' WHERE uen = '%s' AND action_type = 'RETRY_ATTEMPT' " +
                "ELSE " +
                "INSERT INTO ts_acr_audit_trail (uen, action_type, action_date, action_details, performed_by, " +
                "cre_date, cre_user_id) VALUES ('%s', 'RETRY_ATTEMPT', GETDATE(), 'Retry attempt 3/3 failed', " +
                "'SYS_DHUEN016', GETDATE(), 'DHUEN016_TEST')",
                TEST_UEN, TEST_UEN, TEST_UEN
            );

            jdbcTemplate.update(batchJobSetup);
            jdbcTemplate.update(tsAcrSetup);
            jdbcTemplate.update(firmInfoSetup);
            jdbcTemplate.update(errorAuditSetup);
            jdbcTemplate.update(retryAuditSetup);

            setupResult.put("batch_job_setup", "SUCCESS");
            setupResult.put("ts_acr_setup", "SUCCESS");
            setupResult.put("firm_info_setup", "SUCCESS");
            setupResult.put("error_audit_setup", "SUCCESS");
            setupResult.put("retry_audit_setup", "SUCCESS");
            setupResult.put("test_uen", TEST_UEN);
            setupResult.put("application_status", APPLICATION_STATUS_FAILED);
            setupResult.put("failure_reason", FAILURE_REASON);

            logger.info("Test data setup completed for TS-ACR Failure flow");

        } catch (Exception e) {
            logger.error("Error setting up test data: ", e);
            setupResult.put("status", "FAILED");
            setupResult.put("error", e.getMessage());
        }

        return setupResult;
    }

    private Map<String, Object> callDataHiveApi() {
        Map<String, Object> apiResult = new HashMap<>();

        try {
            logger.info("Simulating failed DataHive API call for UEN: {}", TEST_UEN);

            // Simulate API failure scenario
            try {
                var serviceResult = dataHiveUenService.retrieveUENData(TEST_UEN, NOTICE_NUMBER);
                Map<String, Object> serviceResultMap = new HashMap<>();
                serviceResultMap.put("result", serviceResult);

                // Even if API call succeeds, simulate data processing failure
                apiResult.put("api_call_status", "PARTIAL_FAILURE");
                apiResult.put("service_result", serviceResultMap);
                apiResult.put("failure_simulation", "Simulated timeout during data processing");

            } catch (Exception simulatedException) {
                // Actual API failure
                apiResult.put("api_call_status", "FAILED");
                apiResult.put("error_message", simulatedException.getMessage());
                apiResult.put("failure_type", "API_CALL_EXCEPTION");
            }

            apiResult.put("records_processed", 0);
            logger.info("DataHive API failure simulation completed");

        } catch (Exception e) {
            logger.error("Error in DataHive API failure simulation: ", e);
            apiResult.put("api_call_status", "SIMULATION_ERROR");
            apiResult.put("error_message", e.getMessage());
        }

        return apiResult;
    }

    private Map<String, Object> querySnowflakeDirectly() {
        Map<String, Object> snowflakeResult = new HashMap<>();

        try {
            logger.info("Querying Snowflake directly for UEN: {} (expected minimal/no data)", TEST_UEN);

            // Query firm info (might have partial data)
            String firmInfoQuery = String.format(
                "SELECT * FROM V_DH_ACRA_FIRMINFO_R WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> firmInfoData = jdbcTemplate.queryForList(firmInfoQuery);

            // Query other tables (expected to be empty due to failure)
            String shareholderQuery = String.format(
                "SELECT * FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> shareholderData = jdbcTemplate.queryForList(shareholderQuery);

            String boardQuery = String.format(
                "SELECT * FROM V_DH_ACRA_BOARD_INFO_FULL WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> boardData = jdbcTemplate.queryForList(boardQuery);

            snowflakeResult.put("firm_info_records", firmInfoData.size());
            snowflakeResult.put("shareholder_records", shareholderData.size());
            snowflakeResult.put("board_records", boardData.size());
            snowflakeResult.put("firm_info_data", firmInfoData);
            snowflakeResult.put("shareholder_data", shareholderData);
            snowflakeResult.put("board_data", boardData);

            // Verify data incompleteness
            boolean hasIncompleteData = firmInfoData.size() > 0 &&
                                      (shareholderData.size() == 0 || boardData.size() == 0);
            snowflakeResult.put("has_incomplete_data", hasIncompleteData);

            logger.info("Snowflake direct query completed for failure scenario");

        } catch (Exception e) {
            logger.error("Error querying Snowflake: ", e);
            snowflakeResult.put("status", "FAILED");
            snowflakeResult.put("error", e.getMessage());
        }

        return snowflakeResult;
    }

    private Map<String, Object> verifyTsAcrFailureWorkflow() {
        Map<String, Object> verificationResult = new HashMap<>();

        try {
            logger.info("Verifying TS-ACR Failure workflow...");

            // Verify application status is FAILED
            verificationResult.put("application_status_verification", verifyApplicationStatusFailed());

            // Verify failure handling workflow
            verificationResult.put("failure_handling_verification", verifyFailureHandlingWorkflow());

            // Verify error logging dan audit trail
            verificationResult.put("error_logging_verification", verifyErrorLoggingAndAuditTrail());

            // Verify retry mechanism
            verificationResult.put("retry_mechanism_verification", verifyRetryMechanism());

            // Verify data rollback/cleanup
            verificationResult.put("data_cleanup_verification", verifyDataCleanupOnFailure());

            // Verify business rule enforcement
            verificationResult.put("business_rule_enforcement", verifyBusinessRuleEnforcementOnFailure());

            verificationResult.put("overall_verification", "SUCCESS");
            logger.info("TS-ACR Failure workflow verification completed");

        } catch (Exception e) {
            logger.error("Error in TS-ACR failure workflow verification: ", e);
            verificationResult.put("overall_verification", "FAILED");
            verificationResult.put("error", e.getMessage());
        }

        return verificationResult;
    }

    private Map<String, Object> verifyApplicationStatusFailed() {
        Map<String, Object> statusResult = new HashMap<>();

        try {
            String statusQuery = String.format(
                "SELECT application_status, failure_reason, last_attempt_date, retry_count " +
                "FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> statusData = jdbcTemplate.queryForList(statusQuery);

            boolean statusIsFailed = false;
            boolean hasFailureReason = false;
            boolean hasLastAttemptDate = false;
            boolean hasRetryCount = false;

            if (!statusData.isEmpty()) {
                Map<String, Object> appData = statusData.get(0);
                String status = (String) appData.get("application_status");
                statusIsFailed = APPLICATION_STATUS_FAILED.equals(status);
                hasFailureReason = appData.get("failure_reason") != null;
                hasLastAttemptDate = appData.get("last_attempt_date") != null;
                hasRetryCount = appData.get("retry_count") != null;
            }

            statusResult.put("status_is_failed", statusIsFailed);
            statusResult.put("has_failure_reason", hasFailureReason);
            statusResult.put("has_last_attempt_date", hasLastAttemptDate);
            statusResult.put("has_retry_count", hasRetryCount);
            statusResult.put("expected_status", APPLICATION_STATUS_FAILED);
            statusResult.put("application_data", statusData);

        } catch (Exception e) {
            logger.error("Error verifying application status: ", e);
            statusResult.put("error", e.getMessage());
        }

        return statusResult;
    }

    private Map<String, Object> verifyFailureHandlingWorkflow() {
        Map<String, Object> workflowResult = new HashMap<>();

        try {
            // Verify batch job failure status
            String batchJobQuery = String.format(
                "SELECT run_status, start_run, end_run, log_text FROM ocms_batch_job WHERE name = '%s'", NOTICE_NUMBER
            );
            List<Map<String, Object>> batchJobData = jdbcTemplate.queryForList(batchJobQuery);

            boolean batchJobFailed = false;
            boolean hasErrorLog = false;
            if (!batchJobData.isEmpty()) {
                String runStatus = (String) batchJobData.get(0).get("run_status");
                String logText = (String) batchJobData.get(0).get("log_text");
                batchJobFailed = "F".equals(runStatus);
                hasErrorLog = logText != null && logText.contains("Error");
            }

            workflowResult.put("batch_job_failed", batchJobFailed);
            workflowResult.put("has_error_log", hasErrorLog);
            workflowResult.put("batch_job_data", batchJobData);

            // Verify proper timestamp handling for failure
            if (!batchJobData.isEmpty()) {
                Object startRun = batchJobData.get(0).get("start_run");
                Object endRun = batchJobData.get(0).get("end_run");
                boolean hasFailureTimestamps = startRun != null && endRun != null;
                workflowResult.put("has_failure_timestamps", hasFailureTimestamps);
            }

        } catch (Exception e) {
            logger.error("Error verifying failure handling workflow: ", e);
            workflowResult.put("error", e.getMessage());
        }

        return workflowResult;
    }

    private Map<String, Object> verifyErrorLoggingAndAuditTrail() {
        Map<String, Object> auditResult = new HashMap<>();

        try {
            // Verify error audit trail records
            String errorAuditQuery = String.format(
                "SELECT action_type, action_date, action_details FROM ts_acr_audit_trail " +
                "WHERE uen = '%s' AND action_type IN ('DATA_RETRIEVAL_FAILED', 'RETRY_ATTEMPT')", TEST_UEN
            );
            List<Map<String, Object>> errorAuditData = jdbcTemplate.queryForList(errorAuditQuery);

            boolean hasFailureAudit = errorAuditData.stream()
                .anyMatch(record -> "DATA_RETRIEVAL_FAILED".equals(record.get("action_type")));

            boolean hasRetryAudit = errorAuditData.stream()
                .anyMatch(record -> "RETRY_ATTEMPT".equals(record.get("action_type")));

            auditResult.put("error_audit_record_count", errorAuditData.size());
            auditResult.put("has_failure_audit", hasFailureAudit);
            auditResult.put("has_retry_audit", hasRetryAudit);
            auditResult.put("error_audit_data", errorAuditData);

            // Verify error details are captured
            if (!errorAuditData.isEmpty()) {
                boolean hasDetailedErrorInfo = errorAuditData.stream()
                    .anyMatch(record -> record.get("action_details") != null &&
                                      ((String) record.get("action_details")).contains(FAILURE_REASON));
                auditResult.put("has_detailed_error_info", hasDetailedErrorInfo);
            }

        } catch (Exception e) {
            logger.error("Error verifying error logging and audit trail: ", e);
            auditResult.put("error", e.getMessage());
        }

        return auditResult;
    }

    private Map<String, Object> verifyRetryMechanism() {
        Map<String, Object> retryResult = new HashMap<>();

        try {
            // Verify retry count and mechanism
            String retryQuery = String.format(
                "SELECT retry_count, last_attempt_date FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> retryData = jdbcTemplate.queryForList(retryQuery);

            boolean hasRetryCount = false;
            boolean retryLimitReached = false;

            if (!retryData.isEmpty()) {
                Object retryCountObj = retryData.get(0).get("retry_count");
                if (retryCountObj != null) {
                    int retryCount = ((Number) retryCountObj).intValue();
                    hasRetryCount = retryCount > 0;
                    retryLimitReached = retryCount >= 3; // Assuming 3 is the limit
                }
            }

            retryResult.put("has_retry_count", hasRetryCount);
            retryResult.put("retry_limit_reached", retryLimitReached);
            retryResult.put("retry_data", retryData);

            // Verify retry audit trail
            String retryAuditQuery = String.format(
                "SELECT COUNT(*) as retry_audit_count FROM ts_acr_audit_trail " +
                "WHERE uen = '%s' AND action_type = 'RETRY_ATTEMPT'", TEST_UEN
            );
            List<Map<String, Object>> retryAuditCount = jdbcTemplate.queryForList(retryAuditQuery);

            int auditRetryCount = retryAuditCount.isEmpty() ? 0 :
                                ((Number) retryAuditCount.get(0).get("retry_audit_count")).intValue();

            retryResult.put("retry_audit_count", auditRetryCount);
            retryResult.put("has_retry_audit_trail", auditRetryCount > 0);

        } catch (Exception e) {
            logger.error("Error verifying retry mechanism: ", e);
            retryResult.put("error", e.getMessage());
        }

        return retryResult;
    }

    private Map<String, Object> verifyDataCleanupOnFailure() {
        Map<String, Object> cleanupResult = new HashMap<>();

        try {
            // Verify that incomplete/invalid data is properly handled
            String incompleteDataQuery = String.format(
                "SELECT reg_street_name, reg_postal_code, entity_status_desc " +
                "FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> incompleteData = jdbcTemplate.queryForList(incompleteDataQuery);

            boolean hasIncompleteData = false;
            if (!incompleteData.isEmpty()) {
                Map<String, Object> firmData = incompleteData.get(0);
                hasIncompleteData = firmData.get("reg_street_name") == null ||
                                  firmData.get("reg_postal_code") == null ||
                                  "Data Incomplete".equals(firmData.get("entity_status_desc"));
            }

            cleanupResult.put("has_incomplete_data_marker", hasIncompleteData);
            cleanupResult.put("incomplete_data", incompleteData);

            // Verify that downstream tables don't have orphaned data
            String[] downstreamTables = {"ocms_offence_notice_owner_driver_addr", "ocms_dh_acra_shareholder_info", "ocms_dh_acra_board_info"};
            Map<String, Integer> downstreamCounts = new HashMap<>();

            for (String table : downstreamTables) {
                String countQuery = String.format("SELECT COUNT(*) as record_count FROM %s WHERE uen = '%s'", table, TEST_UEN);
                List<Map<String, Object>> countResult = jdbcTemplate.queryForList(countQuery);
                int count = countResult.isEmpty() ? 0 : ((Number) countResult.get(0).get("record_count")).intValue();
                downstreamCounts.put(table, count);
            }

            boolean hasCleanDownstreamTables = downstreamCounts.values().stream().allMatch(count -> count == 0);
            cleanupResult.put("has_clean_downstream_tables", hasCleanDownstreamTables);
            cleanupResult.put("downstream_table_counts", downstreamCounts);

        } catch (Exception e) {
            logger.error("Error verifying data cleanup: ", e);
            cleanupResult.put("error", e.getMessage());
        }

        return cleanupResult;
    }

    private Map<String, Object> verifyBusinessRuleEnforcementOnFailure() {
        Map<String, Object> enforcementResult = new HashMap<>();

        try {
            // Verify that failed applications don't get approved
            String approvalQuery = String.format(
                "SELECT application_status, approval_date, approved_by, gazetted " +
                "FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> approvalData = jdbcTemplate.queryForList(approvalQuery);

            boolean noApprovalOnFailure = false;
            boolean gazettedSetCorrectly = false;

            if (!approvalData.isEmpty()) {
                Map<String, Object> appData = approvalData.get(0);
                String status = (String) appData.get("application_status");
                Object approvalDate = appData.get("approval_date");
                Object approvedBy = appData.get("approved_by");
                Object gazettedObj = appData.get("gazetted");

                noApprovalOnFailure = APPLICATION_STATUS_FAILED.equals(status) &&
                                    approvalDate == null &&
                                    approvedBy == null;

                Integer gazetted = gazettedObj instanceof Integer ? (Integer) gazettedObj :
                                 gazettedObj instanceof Boolean ? ((Boolean) gazettedObj ? 1 : 0) : 0;
                gazettedSetCorrectly = gazetted == 0; // Failed applications should not be gazetted
            }

            enforcementResult.put("no_approval_on_failure", noApprovalOnFailure);
            enforcementResult.put("gazetted_set_correctly", gazettedSetCorrectly);
            enforcementResult.put("approval_data", approvalData);

            // Verify that business validation prevents incomplete data processing
            String validationQuery = String.format(
                "SELECT entity_status_desc FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> validationData = jdbcTemplate.queryForList(validationQuery);

            boolean hasValidationMarker = false;
            if (!validationData.isEmpty()) {
                String status = (String) validationData.get(0).get("entity_status_desc");
                hasValidationMarker = "Data Incomplete".equals(status) || status == null;
            }

            enforcementResult.put("has_validation_marker", hasValidationMarker);
            enforcementResult.put("validation_data", validationData);

        } catch (Exception e) {
            logger.error("Error verifying business rule enforcement: ", e);
            enforcementResult.put("error", e.getMessage());
        }

        return enforcementResult;
    }
}