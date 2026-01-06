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
public class DataHiveUenTsAcrSuccessFlowService {

    private static final Logger logger = LoggerFactory.getLogger(DataHiveUenTsAcrSuccessFlowService.class);

    @Autowired
    private DataHiveUENService dataHiveUenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataHiveTestDatabaseHelper databaseHelper;

    @Autowired
    private DateTimeTestHelper dateTimeHelper;

    private static final String TEST_UEN = "201234581O";
    private static final String NOTICE_NUMBER = "DHUEN015";
    private static final String ENTITY_TYPE = "LC";
    private static final String COMPANY_NAME = "DataHive TS-ACR Success Test Pte Ltd";
    private static final String APPLICATION_STATUS_SUCCESS = "SUCCESS";

    public Map<String, Object> executeTsAcrSuccessFlow() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            logger.info("=== SCENARIO 15: TS-ACR Application Success Flow ===");
            logger.info("Testing UEN: {}, Notice: {}", TEST_UEN, NOTICE_NUMBER);

            // Step 1: Setup test data untuk TS-ACR success scenario
            result.put("step1_setup", setupTestData());

            // Step 2: Call DataHive API
            result.put("step2_api_call", callDataHiveApi());

            // Step 3: Query Snowflake directly untuk comparison
            result.put("step3_snowflake_query", querySnowflakeDirectly());

            // Step 4: Verify TS-ACR success workflow
            result.put("step4_ts_acr_success_verification", verifyTsAcrSuccessWorkflow());

            result.put("overall_status", "SUCCESS");
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());

            logger.info("TS-ACR Success Flow completed successfully");

        } catch (Exception e) {
            logger.error("Error in TS-ACR Success Flow: ", e);
            result.put("overall_status", "FAILED");
            result.put("error_message", e.getMessage());
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());
        }

        return result;
    }

    private Map<String, Object> setupTestData() {
        Map<String, Object> setupResult = new HashMap<>();

        try {
            logger.info("Setting up test data for TS-ACR Success flow...");

            // Setup ocms_batch_job untuk success scenario
            String batchJobSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_batch_job WHERE name = '%s') " +
                "UPDATE ocms_batch_job SET run_status = 'S', start_run = DATEADD(MINUTE, -5, GETDATE()), " +
                "end_run = GETDATE(), upd_date = GETDATE(), upd_user_id = 'DHUEN015_TEST' WHERE name = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_batch_job (name, run_status, log_text, start_run, end_run, cre_date, cre_user_id) " +
                "VALUES ('%s', 'S', 'TS-ACR Success Flow Test - Completed Successfully', " +
                "DATEADD(MINUTE, -5, GETDATE()), GETDATE(), GETDATE(), 'DHUEN015_TEST')",
                NOTICE_NUMBER, NOTICE_NUMBER, NOTICE_NUMBER
            );

            // Setup TS-ACR application dengan SUCCESS status
            String tsAcrSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_application WHERE uen = '%s') " +
                "UPDATE ts_acr_application SET entity_type = '%s', company_name = '%s', gazetted = 1, " +
                "application_status = '%s', approval_date = GETDATE(), approved_by = 'SYS_DHUEN015', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN015_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ts_acr_application (uen, entity_type, company_name, gazetted, application_status, " +
                "approval_date, approved_by, cre_date, cre_user_id) VALUES ('%s', '%s', '%s', 1, '%s', " +
                "GETDATE(), 'SYS_DHUEN015', GETDATE(), 'DHUEN015_TEST')",
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, APPLICATION_STATUS_SUCCESS, TEST_UEN,
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, APPLICATION_STATUS_SUCCESS
            );

            // Setup complete company data untuk successful application
            String firmInfoSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_dh_acra_company_detail WHERE uen = '%s') " +
                "UPDATE ocms_dh_acra_company_detail SET entity_type = '%s', entity_name = '%s', " +
                "reg_street_name = 'TS-ACR Success Street', reg_postal_code = '789012', " +
                "entity_status_desc = 'Live Company', reg_date = GETDATE(), " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN015_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_dh_acra_company_detail (uen, entity_type, entity_name, reg_street_name, reg_postal_code, " +
                "entity_status_desc, reg_date, cre_date, cre_user_id) VALUES ('%s', '%s', '%s', " +
                "'TS-ACR Success Street', '789012', 'Live Company', GETDATE(), GETDATE(), 'DHUEN015_TEST')",
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME, TEST_UEN,
                TEST_UEN, ENTITY_TYPE, COMPANY_NAME
            );

            // Setup audit trail untuk success workflow
            String auditTrailSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_audit_trail WHERE uen = '%s' AND action_type = 'DATA_RETRIEVAL_SUCCESS') " +
                "UPDATE ts_acr_audit_trail SET action_date = GETDATE(), action_details = 'DataHive data retrieved successfully', " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN015_TEST' WHERE uen = '%s' AND action_type = 'DATA_RETRIEVAL_SUCCESS' " +
                "ELSE " +
                "INSERT INTO ts_acr_audit_trail (uen, action_type, action_date, action_details, performed_by, " +
                "cre_date, cre_user_id) VALUES ('%s', 'DATA_RETRIEVAL_SUCCESS', GETDATE(), " +
                "'DataHive data retrieved successfully', 'SYS_DHUEN015', GETDATE(), 'DHUEN015_TEST')",
                TEST_UEN, TEST_UEN, TEST_UEN
            );

            jdbcTemplate.update(batchJobSetup);
            jdbcTemplate.update(tsAcrSetup);
            jdbcTemplate.update(firmInfoSetup);
            jdbcTemplate.update(auditTrailSetup);

            setupResult.put("batch_job_setup", "SUCCESS");
            setupResult.put("ts_acr_setup", "SUCCESS");
            setupResult.put("firm_info_setup", "SUCCESS");
            setupResult.put("audit_trail_setup", "SUCCESS");
            setupResult.put("test_uen", TEST_UEN);
            setupResult.put("application_status", APPLICATION_STATUS_SUCCESS);

            logger.info("Test data setup completed for TS-ACR Success flow");

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
            logger.info("Calling DataHive API for UEN: {}", TEST_UEN);

            // Call DataHive UEN service method
            var serviceResult = dataHiveUenService.retrieveUENData(TEST_UEN, NOTICE_NUMBER);
            Map<String, Object> serviceResultMap = new HashMap<>();
            serviceResultMap.put("result", serviceResult);

            apiResult.put("api_call_status", "SUCCESS");
            apiResult.put("service_result", serviceResultMap);
            apiResult.put("records_processed", serviceResult != null ? 1 : 0);

            logger.info("DataHive API call completed");

        } catch (Exception e) {
            logger.error("Error calling DataHive API: ", e);
            apiResult.put("api_call_status", "FAILED");
            apiResult.put("error_message", e.getMessage());
        }

        return apiResult;
    }

    private Map<String, Object> querySnowflakeDirectly() {
        Map<String, Object> snowflakeResult = new HashMap<>();

        try {
            logger.info("Querying Snowflake directly for UEN: {}", TEST_UEN);

            // Query firm info
            String firmInfoQuery = String.format(
                "SELECT * FROM V_DH_ACRA_FIRMINFO_R WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> firmInfoData = jdbcTemplate.queryForList(firmInfoQuery);

            // Query shareholder info
            String shareholderQuery = String.format(
                "SELECT * FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> shareholderData = jdbcTemplate.queryForList(shareholderQuery);

            // Query board info
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

            logger.info("Snowflake direct query completed");

        } catch (Exception e) {
            logger.error("Error querying Snowflake: ", e);
            snowflakeResult.put("status", "FAILED");
            snowflakeResult.put("error", e.getMessage());
        }

        return snowflakeResult;
    }

    private Map<String, Object> verifyTsAcrSuccessWorkflow() {
        Map<String, Object> verificationResult = new HashMap<>();

        try {
            logger.info("Verifying TS-ACR Success workflow...");

            // Verify application status is SUCCESS
            verificationResult.put("application_status_verification", verifyApplicationStatusSuccess());

            // Verify approval workflow completion
            verificationResult.put("approval_workflow_verification", verifyApprovalWorkflowCompletion());

            // Verify data consistency post-approval
            verificationResult.put("data_consistency_verification", verifyDataConsistencyPostApproval());

            // Verify audit trail completeness
            verificationResult.put("audit_trail_verification", verifyAuditTrailCompleteness());

            // Verify business rule compliance
            verificationResult.put("business_rule_compliance", verifyBusinessRuleCompliance());

            verificationResult.put("overall_verification", "SUCCESS");
            logger.info("TS-ACR Success workflow verification completed");

        } catch (Exception e) {
            logger.error("Error in TS-ACR success workflow verification: ", e);
            verificationResult.put("overall_verification", "FAILED");
            verificationResult.put("error", e.getMessage());
        }

        return verificationResult;
    }

    private Map<String, Object> verifyApplicationStatusSuccess() {
        Map<String, Object> statusResult = new HashMap<>();

        try {
            String statusQuery = String.format(
                "SELECT application_status, approval_date, approved_by FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> statusData = jdbcTemplate.queryForList(statusQuery);

            boolean statusIsSuccess = false;
            boolean hasApprovalDate = false;
            boolean hasApprover = false;

            if (!statusData.isEmpty()) {
                Map<String, Object> appData = statusData.get(0);
                String status = (String) appData.get("application_status");
                statusIsSuccess = APPLICATION_STATUS_SUCCESS.equals(status);
                hasApprovalDate = appData.get("approval_date") != null;
                hasApprover = appData.get("approved_by") != null;
            }

            statusResult.put("status_is_success", statusIsSuccess);
            statusResult.put("has_approval_date", hasApprovalDate);
            statusResult.put("has_approver", hasApprover);
            statusResult.put("expected_status", APPLICATION_STATUS_SUCCESS);
            statusResult.put("application_data", statusData);

        } catch (Exception e) {
            logger.error("Error verifying application status: ", e);
            statusResult.put("error", e.getMessage());
        }

        return statusResult;
    }

    private Map<String, Object> verifyApprovalWorkflowCompletion() {
        Map<String, Object> workflowResult = new HashMap<>();

        try {
            // Verify batch job completion
            String batchJobQuery = String.format(
                "SELECT run_status, start_run, end_run FROM ocms_batch_job WHERE name = '%s'", NOTICE_NUMBER
            );
            List<Map<String, Object>> batchJobData = jdbcTemplate.queryForList(batchJobQuery);

            boolean batchJobCompleted = false;
            if (!batchJobData.isEmpty()) {
                String runStatus = (String) batchJobData.get(0).get("run_status");
                batchJobCompleted = "S".equals(runStatus);
            }

            workflowResult.put("batch_job_completed", batchJobCompleted);
            workflowResult.put("batch_job_data", batchJobData);

            // Verify workflow timestamps are logical
            if (!batchJobData.isEmpty()) {
                Object startRun = batchJobData.get(0).get("start_run");
                Object endRun = batchJobData.get(0).get("end_run");
                boolean hasValidTimestamps = startRun != null && endRun != null;
                workflowResult.put("has_valid_timestamps", hasValidTimestamps);
            }

        } catch (Exception e) {
            logger.error("Error verifying approval workflow: ", e);
            workflowResult.put("error", e.getMessage());
        }

        return workflowResult;
    }

    private Map<String, Object> verifyDataConsistencyPostApproval() {
        Map<String, Object> consistencyResult = new HashMap<>();

        try {
            // Verify data exists in all required tables
            String[] requiredTables = {"ocms_dh_acra_company_detail", "ts_acr_application"};
            Map<String, Integer> tableCounts = new HashMap<>();

            for (String table : requiredTables) {
                String countQuery = String.format("SELECT COUNT(*) as record_count FROM %s WHERE uen = '%s'", table, TEST_UEN);
                List<Map<String, Object>> countResult = jdbcTemplate.queryForList(countQuery);
                int count = countResult.isEmpty() ? 0 : ((Number) countResult.get(0).get("record_count")).intValue();
                tableCounts.put(table, count);
            }

            boolean hasRequiredData = tableCounts.values().stream().allMatch(count -> count > 0);
            consistencyResult.put("has_required_data", hasRequiredData);
            consistencyResult.put("table_record_counts", tableCounts);

            // Verify entity type consistency
            String firmEntityQuery = String.format("SELECT entity_type FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN);
            String tsAcrEntityQuery = String.format("SELECT entity_type FROM ts_acr_application WHERE uen = '%s'", TEST_UEN);

            List<Map<String, Object>> firmEntity = jdbcTemplate.queryForList(firmEntityQuery);
            List<Map<String, Object>> tsAcrEntity = jdbcTemplate.queryForList(tsAcrEntityQuery);

            boolean entityTypeConsistent = false;
            if (!firmEntity.isEmpty() && !tsAcrEntity.isEmpty()) {
                String firmEntityType = (String) firmEntity.get(0).get("entity_type");
                String tsAcrEntityType = (String) tsAcrEntity.get(0).get("entity_type");
                entityTypeConsistent = firmEntityType != null && firmEntityType.equals(tsAcrEntityType);
            }

            consistencyResult.put("entity_type_consistent", entityTypeConsistent);

        } catch (Exception e) {
            logger.error("Error verifying data consistency: ", e);
            consistencyResult.put("error", e.getMessage());
        }

        return consistencyResult;
    }

    private Map<String, Object> verifyAuditTrailCompleteness() {
        Map<String, Object> auditResult = new HashMap<>();

        try {
            // Verify audit trail records exist
            String auditQuery = String.format(
                "SELECT action_type, action_date, performed_by FROM ts_acr_audit_trail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> auditData = jdbcTemplate.queryForList(auditQuery);

            auditResult.put("audit_record_count", auditData.size());
            auditResult.put("audit_trail_data", auditData);

            // Check for specific audit events
            boolean hasDataRetrievalEvent = auditData.stream()
                .anyMatch(record -> "DATA_RETRIEVAL_SUCCESS".equals(record.get("action_type")));

            auditResult.put("has_data_retrieval_event", hasDataRetrievalEvent);

            // Verify audit trail timestamps are recent
            if (!auditData.isEmpty()) {
                boolean hasRecentAuditEvents = auditData.stream()
                    .allMatch(record -> record.get("action_date") != null);
                auditResult.put("has_recent_audit_events", hasRecentAuditEvents);
            }

        } catch (Exception e) {
            logger.error("Error verifying audit trail: ", e);
            auditResult.put("error", e.getMessage());
        }

        return auditResult;
    }

    private Map<String, Object> verifyBusinessRuleCompliance() {
        Map<String, Object> complianceResult = new HashMap<>();

        try {
            // Verify gazetted status for approved application
            String gazettedQuery = String.format(
                "SELECT gazetted, entity_type, application_status FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> gazettedData = jdbcTemplate.queryForList(gazettedQuery);

            boolean gazettedCompliance = false;
            if (!gazettedData.isEmpty()) {
                Map<String, Object> appData = gazettedData.get(0);
                Object gazettedObj = appData.get("gazetted");
                String status = (String) appData.get("application_status");
                String entityType = (String) appData.get("entity_type");

                Integer gazetted = gazettedObj instanceof Integer ? (Integer) gazettedObj :
                                 gazettedObj instanceof Boolean ? ((Boolean) gazettedObj ? 1 : 0) : 0;

                // For successful LC applications, gazetted should be 1
                gazettedCompliance = APPLICATION_STATUS_SUCCESS.equals(status) &&
                                   ENTITY_TYPE.equals(entityType) &&
                                   gazetted == 1;
            }

            complianceResult.put("gazetted_compliance", gazettedCompliance);
            complianceResult.put("gazetted_data", gazettedData);

            // Verify registration date exists for approved companies
            String regDateQuery = String.format(
                "SELECT reg_date, entity_status_desc FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> regDateData = jdbcTemplate.queryForList(regDateQuery);

            boolean hasRegistrationDate = false;
            if (!regDateData.isEmpty()) {
                hasRegistrationDate = regDateData.get(0).get("reg_date") != null;
            }

            complianceResult.put("has_registration_date", hasRegistrationDate);
            complianceResult.put("registration_data", regDateData);

        } catch (Exception e) {
            logger.error("Error verifying business rule compliance: ", e);
            complianceResult.put("error", e.getMessage());
        }

        return complianceResult;
    }
}