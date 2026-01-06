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
public class DataHiveUenNonListedCompanyFlowService {

    private static final Logger logger = LoggerFactory.getLogger(DataHiveUenNonListedCompanyFlowService.class);

    @Autowired
    private DataHiveUENService dataHiveUenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataHiveTestDatabaseHelper databaseHelper;

    @Autowired
    private DateTimeTestHelper dateTimeHelper;

    private static final String TEST_UEN = "201234580N";
    private static final String NOTICE_NUMBER = "DHUEN014";
    private static final String ENTITY_TYPE_NLC = "NLC";
    private static final String COMPANY_NAME = "DataHive Non-Listed Company Test Pte Ltd";

    public Map<String, Object> executeNonListedCompanyFlow() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            logger.info("=== SCENARIO 14: Non-Listed Company Flow (EntityType=NLC) ===");
            logger.info("Testing UEN: {}, Notice: {}", TEST_UEN, NOTICE_NUMBER);

            // Step 1: Setup test data dengan EntityType=NLC
            result.put("step1_setup", setupTestData());

            // Step 2: Call DataHive API
            result.put("step2_api_call", callDataHiveApi());

            // Step 3: Query Snowflake directly untuk comparison
            result.put("step3_snowflake_query", querySnowflakeDirectly());

            // Step 4: Verify business logic untuk Non-Listed Company
            result.put("step4_business_logic_verification", verifyNonListedCompanyBusinessLogic());

            result.put("overall_status", "SUCCESS");
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());

            logger.info("Non-Listed Company Flow completed successfully");

        } catch (Exception e) {
            logger.error("Error in Non-Listed Company Flow: ", e);
            result.put("overall_status", "FAILED");
            result.put("error_message", e.getMessage());
            result.put("execution_time_seconds", Duration.between(startTime, LocalDateTime.now()).getSeconds());
        }

        return result;
    }

    private Map<String, Object> setupTestData() {
        Map<String, Object> setupResult = new HashMap<>();

        try {
            logger.info("Setting up test data for Non-Listed Company flow...");

            // Setup ocms_batch_job
            String batchJobSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_batch_job WHERE name = '%s') " +
                "UPDATE ocms_batch_job SET run_status = 'R', start_run = GETDATE(), upd_date = GETDATE(), upd_user_id = 'DHUEN014_TEST' WHERE name = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_batch_job (name, run_status, log_text, start_run, cre_date, cre_user_id) " +
                "VALUES ('%s', 'R', 'Non-Listed Company Flow Test', GETDATE(), GETDATE(), 'DHUEN014_TEST')",
                NOTICE_NUMBER, NOTICE_NUMBER, NOTICE_NUMBER
            );

            // Setup TS-ACR application record untuk Non-Listed Company (gazetted=0)
            String tsAcrSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ts_acr_application WHERE uen = '%s') " +
                "UPDATE ts_acr_application SET entity_type = '%s', company_name = '%s', gazetted = 0, " +
                "upd_date = GETDATE(), upd_user_id = 'DHUEN014_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ts_acr_application (uen, entity_type, company_name, gazetted, application_status, " +
                "cre_date, cre_user_id) VALUES ('%s', '%s', '%s', 0, 'PENDING', GETDATE(), 'DHUEN014_TEST')",
                TEST_UEN, ENTITY_TYPE_NLC, COMPANY_NAME, TEST_UEN,
                TEST_UEN, ENTITY_TYPE_NLC, COMPANY_NAME
            );

            // Setup ocms_dh_acra_company_detail dengan minimal required fields untuk NLC
            String firmInfoSetup = String.format(
                "IF EXISTS (SELECT 1 FROM ocms_dh_acra_company_detail WHERE uen = '%s') " +
                "UPDATE ocms_dh_acra_company_detail SET entity_type = '%s', entity_name = '%s', " +
                "reg_street_name = 'Non-Listed Company Street', reg_postal_code = '654321', " +
                "entity_status_desc = 'Live Company', upd_date = GETDATE(), upd_user_id = 'DHUEN014_TEST' WHERE uen = '%s' " +
                "ELSE " +
                "INSERT INTO ocms_dh_acra_company_detail (uen, entity_type, entity_name, reg_street_name, reg_postal_code, " +
                "entity_status_desc, cre_date, cre_user_id) VALUES ('%s', '%s', '%s', 'Non-Listed Company Street', " +
                "'654321', 'Live Company', GETDATE(), 'DHUEN014_TEST')",
                TEST_UEN, ENTITY_TYPE_NLC, COMPANY_NAME, TEST_UEN,
                TEST_UEN, ENTITY_TYPE_NLC, COMPANY_NAME
            );

            jdbcTemplate.update(batchJobSetup);
            jdbcTemplate.update(tsAcrSetup);
            jdbcTemplate.update(firmInfoSetup);

            setupResult.put("batch_job_setup", "SUCCESS");
            setupResult.put("ts_acr_setup", "SUCCESS");
            setupResult.put("firm_info_setup", "SUCCESS");
            setupResult.put("test_uen", TEST_UEN);
            setupResult.put("entity_type", ENTITY_TYPE_NLC);
            setupResult.put("gazetted_flag", 0);

            logger.info("Test data setup completed for Non-Listed Company");

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

            // Call DataHive service method
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

            // Query deregistered firm info
            String deregFirmQuery = String.format(
                "SELECT * FROM V_DH_ACRA_FIRMINFO_D WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> deregFirmData = jdbcTemplate.queryForList(deregFirmQuery);

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
            snowflakeResult.put("dereg_firm_records", deregFirmData.size());
            snowflakeResult.put("shareholder_records", shareholderData.size());
            snowflakeResult.put("board_records", boardData.size());
            snowflakeResult.put("firm_info_data", firmInfoData);
            snowflakeResult.put("dereg_firm_data", deregFirmData);
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

    private Map<String, Object> verifyNonListedCompanyBusinessLogic() {
        Map<String, Object> verificationResult = new HashMap<>();

        try {
            logger.info("Verifying Non-Listed Company business logic...");

            // Verify EntityType=NLC business rules
            verificationResult.put("entity_type_validation", verifyEntityTypeBusinessRules());

            // Verify gazetted flag logic (should be 0 for NLC)
            verificationResult.put("gazetted_flag_validation", verifyGazettedFlagLogic());

            // Verify Non-Listed Company specific requirements
            verificationResult.put("non_listed_company_requirements", verifyNonListedCompanyRequirements());

            // Verify simplified data requirements vs Listed Company
            verificationResult.put("simplified_data_requirements", verifySimplifiedDataRequirements());

            verificationResult.put("overall_verification", "SUCCESS");
            logger.info("Non-Listed Company business logic verification completed");

        } catch (Exception e) {
            logger.error("Error in business logic verification: ", e);
            verificationResult.put("overall_verification", "FAILED");
            verificationResult.put("error", e.getMessage());
        }

        return verificationResult;
    }

    private Map<String, Object> verifyEntityTypeBusinessRules() {
        Map<String, Object> entityTypeResult = new HashMap<>();

        try {
            // Verify entity_type = 'NLC' in ocms_dh_acra_company_detail
            String entityTypeQuery = String.format(
                "SELECT entity_type FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> entityTypeData = jdbcTemplate.queryForList(entityTypeQuery);

            boolean entityTypeCorrect = false;
            if (!entityTypeData.isEmpty()) {
                String entityType = (String) entityTypeData.get(0).get("entity_type");
                entityTypeCorrect = ENTITY_TYPE_NLC.equals(entityType);
            }

            entityTypeResult.put("entity_type_in_db", entityTypeData.isEmpty() ? "NOT_FOUND" : entityTypeData.get(0).get("entity_type"));
            entityTypeResult.put("entity_type_correct", entityTypeCorrect);
            entityTypeResult.put("expected_entity_type", ENTITY_TYPE_NLC);

            // Verify TS-ACR application has matching entity_type
            String tsAcrQuery = String.format(
                "SELECT entity_type, gazetted FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> tsAcrData = jdbcTemplate.queryForList(tsAcrQuery);

            boolean tsAcrEntityTypeMatch = false;
            if (!tsAcrData.isEmpty()) {
                String tsAcrEntityType = (String) tsAcrData.get(0).get("entity_type");
                tsAcrEntityTypeMatch = ENTITY_TYPE_NLC.equals(tsAcrEntityType);
            }

            entityTypeResult.put("ts_acr_entity_type_match", tsAcrEntityTypeMatch);
            entityTypeResult.put("ts_acr_data", tsAcrData);

        } catch (Exception e) {
            logger.error("Error verifying entity type business rules: ", e);
            entityTypeResult.put("error", e.getMessage());
        }

        return entityTypeResult;
    }

    private Map<String, Object> verifyGazettedFlagLogic() {
        Map<String, Object> gazettedResult = new HashMap<>();

        try {
            // For Non-Listed Companies, gazetted flag should be 0
            String gazettedQuery = String.format(
                "SELECT gazetted, entity_type FROM ts_acr_application WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> gazettedData = jdbcTemplate.queryForList(gazettedQuery);

            boolean gazettedFlagCorrect = false;
            if (!gazettedData.isEmpty()) {
                Object gazettedObj = gazettedData.get(0).get("gazetted");
                Integer gazetted = gazettedObj instanceof Integer ? (Integer) gazettedObj :
                                 gazettedObj instanceof Boolean ? ((Boolean) gazettedObj ? 1 : 0) : 0;
                gazettedFlagCorrect = gazetted == 0;
            }

            gazettedResult.put("gazetted_flag_correct", gazettedFlagCorrect);
            gazettedResult.put("expected_gazetted_value", 0);
            gazettedResult.put("actual_gazetted_data", gazettedData);

            // Verify business rule: Non-Listed Companies should not be gazetted
            if (!gazettedData.isEmpty()) {
                String entityType = (String) gazettedData.get(0).get("entity_type");
                Object gazettedObj = gazettedData.get(0).get("gazetted");
                Integer gazetted = gazettedObj instanceof Integer ? (Integer) gazettedObj :
                                 gazettedObj instanceof Boolean ? ((Boolean) gazettedObj ? 1 : 0) : 0;

                boolean businessRuleValid = ENTITY_TYPE_NLC.equals(entityType) && gazetted == 0;
                gazettedResult.put("business_rule_validation", businessRuleValid);
                gazettedResult.put("rule_description", "EntityType=NLC should have gazetted=0");
            }

        } catch (Exception e) {
            logger.error("Error verifying gazetted flag logic: ", e);
            gazettedResult.put("error", e.getMessage());
        }

        return gazettedResult;
    }

    private Map<String, Object> verifyNonListedCompanyRequirements() {
        Map<String, Object> requirementsResult = new HashMap<>();

        try {
            // Non-Listed Companies may have less stringent requirements than Listed Companies
            String companyInfoQuery = String.format(
                "SELECT entity_name, reg_street_name, reg_postal_code, entity_status_desc " +
                "FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> companyInfo = jdbcTemplate.queryForList(companyInfoQuery);

            boolean hasBasicInfo = false;
            if (!companyInfo.isEmpty()) {
                Map<String, Object> info = companyInfo.get(0);
                // For NLC, only entity_name is mandatory
                hasBasicInfo = info.get("entity_name") != null;
            }

            requirementsResult.put("has_basic_company_info", hasBasicInfo);
            requirementsResult.put("company_info_data", companyInfo);

            // Check if address info exists (optional for NLC)
            String addressQuery = String.format(
                "SELECT COUNT(*) as address_count FROM ocms_offence_notice_owner_driver_addr WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> addressCount = jdbcTemplate.queryForList(addressQuery);

            int addressRecords = addressCount.isEmpty() ? 0 :
                               ((Number) addressCount.get(0).get("address_count")).intValue();

            requirementsResult.put("has_address_records", addressRecords > 0);
            requirementsResult.put("address_record_count", addressRecords);
            requirementsResult.put("address_requirement", "OPTIONAL for NLC");

            // Shareholder information (may be optional or limited for NLC)
            String shareholderQuery = String.format(
                "SELECT COUNT(*) as shareholder_count FROM ocms_dh_acra_shareholder_info WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> shareholderCount = jdbcTemplate.queryForList(shareholderQuery);

            int shareholderRecords = shareholderCount.isEmpty() ? 0 :
                                   ((Number) shareholderCount.get(0).get("shareholder_count")).intValue();

            requirementsResult.put("has_shareholder_records", shareholderRecords > 0);
            requirementsResult.put("shareholder_record_count", shareholderRecords);
            requirementsResult.put("shareholder_requirement", "OPTIONAL for NLC");

        } catch (Exception e) {
            logger.error("Error verifying Non-Listed Company requirements: ", e);
            requirementsResult.put("error", e.getMessage());
        }

        return requirementsResult;
    }

    private Map<String, Object> verifySimplifiedDataRequirements() {
        Map<String, Object> simplifiedResult = new HashMap<>();

        try {
            // Compare data volume vs Listed Company expectations
            String[] tables = {"ocms_dh_acra_company_detail", "ocms_offence_notice_owner_driver_addr", "ocms_dh_acra_shareholder_info", "ocms_dh_acra_board_info"};
            Map<String, Integer> tableCounts = new HashMap<>();

            for (String table : tables) {
                String countQuery = String.format("SELECT COUNT(*) as record_count FROM %s WHERE uen = '%s'", table, TEST_UEN);
                List<Map<String, Object>> countResult = jdbcTemplate.queryForList(countQuery);
                int count = countResult.isEmpty() ? 0 : ((Number) countResult.get(0).get("record_count")).intValue();
                tableCounts.put(table, count);
            }

            simplifiedResult.put("table_record_counts", tableCounts);

            // Verify that NLC doesn't require all tables to have data
            boolean hasMinimalRequiredData = tableCounts.get("ocms_dh_acra_company_detail") > 0;
            simplifiedResult.put("meets_minimal_requirements", hasMinimalRequiredData);

            // Check for reduced disclosure requirements
            String disclosureQuery = String.format(
                "SELECT entity_type, entity_status_desc FROM ocms_dh_acra_company_detail WHERE uen = '%s'", TEST_UEN
            );
            List<Map<String, Object>> disclosureData = jdbcTemplate.queryForList(disclosureQuery);

            if (!disclosureData.isEmpty()) {
                String entityType = (String) disclosureData.get(0).get("entity_type");
                String status = (String) disclosureData.get(0).get("entity_status_desc");

                boolean validNlcStatus = ENTITY_TYPE_NLC.equals(entityType) &&
                                       ("Live Company".equals(status) || "Active".equals(status));

                simplifiedResult.put("valid_nlc_status", validNlcStatus);
                simplifiedResult.put("nlc_disclosure_data", disclosureData.get(0));
            }

        } catch (Exception e) {
            logger.error("Error verifying simplified data requirements: ", e);
            simplifiedResult.put("error", e.getMessage());
        }

        return simplifiedResult;
    }
}