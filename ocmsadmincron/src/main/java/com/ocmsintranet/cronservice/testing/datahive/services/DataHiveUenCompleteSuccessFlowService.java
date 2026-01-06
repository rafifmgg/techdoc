package com.ocmsintranet.cronservice.testing.datahive.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.datahive.helpers.DataHiveTestDatabaseHelper;
import com.ocmsintranet.cronservice.testing.datahive.helpers.DateTimeTestHelper;
import com.ocmsintranet.cronservice.testing.datahive.models.CompanyComparisonResult;
import com.ocmsintranet.cronservice.testing.datahive.models.ShareholderComparisonResult;
import com.ocmsintranet.cronservice.testing.datahive.models.BoardComparisonResult;
import com.ocmsintranet.cronservice.testing.datahive.models.GazettedFlagComparisonResult;
import com.ocmsintranet.cronservice.testing.datahive.models.TsAcrValidationComparisonResult;
import com.ocmsintranet.cronservice.testing.datahive.models.SuspensionRecordComparisonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing DataHive Complete UEN Success Flow.
 * Uses DataHiveTestDatabaseHelper to eliminate code duplication.
 *
 * Business scenario: Process comprehensive company profile data from DataHive
 * Expected operations:
 * - ocms_dh_acra_company_detail: Create/update company information
 * - ocms_dh_acra_shareholder_info: Create shareholder records
 * - ocms_dh_acra_board_info: Create board member records
 * - ocms_offence_notice_owner_driver: Update gazetted_flag for LC entities
 * - ocms_offence_notice_owner_driver_addr: Create registered address records
 */
@Service
@Slf4j
public class DataHiveUenCompleteSuccessFlowService {

    // Business processing constants
    private static final String PROCESSING_SUCCESS = "SUCCESS";
    private static final String PROCESSING_FAILED = "FAILED";
    private static final String PROCESSING_INFO = "INFO";

    // Verification constants for consistent error messages
    private static final String ERROR_PREFIX = "❌ VERIFICATION ERROR";
    private static final String VERIFICATION_START_PREFIX = "🔍 Verifying";

    // Business data constants - UEN for comprehensive company profile processing
    private static final String TARGET_UEN = "T14UF7629E";  // UEN with complete ACRA records
    private static final String UEN_ID_TYPE = "B";  // UEN ID type
    private static final String NOTICE_PREFIX = "DHUEN";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "001";

    // Database schema
    private static final String SCHEMA = "ocmsizmgr";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    @Autowired
    private DataHiveUtil dataHiveUtil;

    @Autowired
    private DataHiveTestDatabaseHelper databaseHelper;

    @Autowired
    private DateTimeTestHelper dateTimeHelper;

    /**
     * Execute complete DataHive UEN processing flow with comprehensive data handling.
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting DataHive UEN Complete Success Flow Processing for UEN: {}, Notice: {}", TARGET_UEN, NOTICE_NUMBER);

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Initialize data structures
            steps.add(setupTestData());

            // Step 2: Retrieve DataHive data
            steps.add(callDataHiveApi());

            // Step 3: Query Snowflake directly
            steps.add(querySnowflakeDirectly());

            // Step 4: Verify data processing
            steps.add(verifyExactDataMatch());

            log.info("✅ DataHive UEN Complete Success Flow Processing completed successfully");
            return steps;

        } catch (Exception e) {
            log.error("❌ DataHive UEN Complete Success Flow Processing failed: {}", e.getMessage(), e);

            // Add failure step with preserved details from previous successful steps
            TestStepResult failureStep = new TestStepResult("Flow Execution", PROCESSING_FAILED);
            failureStep.addDetail("❌ Processing flow failed: " + e.getMessage());

            // Add summary of completed steps for debugging context
            failureStep.addDetail("🔍 Completed Steps Before Failure:");
            for (int i = 0; i < steps.size(); i++) {
                TestStepResult step = steps.get(i);
                failureStep.addDetail("  Step " + (i+1) + ": " + step.getTitle() + " - " + step.getStatus());
            }

            steps.add(failureStep);
            return steps;
        }
    }

    /**
     * Step 1: Initialize data structures for UEN processing using helper.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Initialize Data Structures", PROCESSING_INFO);

        try {
            log.info("🔧 Step 1: Initializing data structures for UEN processing");

            // Initialize core OCMS tables using helper - all centralized!
            databaseHelper.resetOrInsertValidOffenceNotice(NOTICE_NUMBER, "DHTEST002", "PP002",
                "COMPANY OFFENCE", new java.math.BigDecimal("100.00"), 10002, result);

            databaseHelper.resetOrInsertOffenceNoticeDetail(NOTICE_NUMBER, "DHTEST987654321",
                "MERCEDES", "SILVER", result);

            databaseHelper.resetOrInsertOwnerDriver(NOTICE_NUMBER, UEN_ID_TYPE, TARGET_UEN,
                "DATAHIVE TEST COMPANY PTE LTD", result);

            databaseHelper.resetOrInsertOwnerDriverAddress(NOTICE_NUMBER, "456", "DATAHIVE COMPANY STREET",
                "05", "12", "DATAHIVE TOWER", "567890", result);

            // Initialize DataHive company detail using helper
            databaseHelper.resetOrInsertCompanyDetail(TARGET_UEN, NOTICE_NUMBER,
                "DATAHIVE TEST COMPANY PTE LTD", "NA", dateTimeHelper.getCurrentTimestamp(), "L", result);

            // Initialize shareholder data using bulk helper
            Object[][] shareholderData = {
                {"1", "F5627565R", 1000},
                {"2", "S6239817E", 2000},
                {"1", "S3333333A", 3000}
            };
            databaseHelper.resetOrInsertShareholderInfo(TARGET_UEN, NOTICE_NUMBER, shareholderData, result);

            // Initialize board data using bulk helper
            Object[][] boardData = {
                {"S9876543B", "", dateTimeHelper.createTestTimestamp(2020, 3, 15, 0, 0), dateTimeHelper.createTestTimestamp(2025, 12, 1, 0, 0)},
                {"S1111222C", "", dateTimeHelper.createTestTimestamp(2020, 4, 1, 0, 0), dateTimeHelper.createTestTimestamp(2025, 12, 1, 0, 0)}
            };
            databaseHelper.resetOrInsertBoardInfo(TARGET_UEN, NOTICE_NUMBER, boardData, result);

            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ Data structures initialized successfully using helper");

        } catch (Exception e) {
            log.error("❌ Error during data structure initialization: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Data structure initialization failed: " + e.getMessage());
            throw new RuntimeException("Data structure initialization failed", e);
        }

        return result;
    }

    /**
     * Step 2: Retrieve comprehensive company data from DataHive.
     */
    private TestStepResult callDataHiveApi() {
        TestStepResult result = new TestStepResult("Retrieve DataHive Data", PROCESSING_INFO);

        try {
            log.info("🌐 Step 2: Retrieving DataHive data for UEN: {}", TARGET_UEN);

            String apiUrl = apiConfigHelper.buildApiUrl("/api/datahive/test/uen/" + TARGET_UEN + "?noticeNumber=" + NOTICE_NUMBER);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                result.setStatus(PROCESSING_SUCCESS);
                result.addDetail("✅ DataHive data retrieval successful");
                result.addDetail("API URL: " + apiUrl);
                result.addDetail("Response Status: " + response.getStatusCode());

                // Log API response summary
                JsonNode apiResult = response.getBody();
                result.setJsonData(apiResult);

                // Parse the actual response structure for UEN
                JsonNode dataNode = apiResult.get("data");
                JsonNode summaryNode = apiResult.get("summary");

                if (summaryNode != null) {
                    result.addDetail("API Summary - TS ACRA Applied: " + summaryNode.get("tsAcrApplied").asBoolean());
                    result.addDetail("API Summary - Is Deregistered: " + summaryNode.get("isDeregistered").asBoolean());
                    result.addDetail("API Summary - Has Error: " + summaryNode.get("hasError").asBoolean());
                    result.addDetail("API Summary - Board Member Records: " + summaryNode.get("boardMemberRecords").asInt());
                    result.addDetail("API Summary - Company Found: " + summaryNode.get("companyFound").asBoolean());
                    result.addDetail("API Summary - Shareholder Records: " + summaryNode.get("shareholderRecords").asInt());
                }

                if (dataNode != null) {
                    // Count company data
                    JsonNode companyData = dataNode.get("companyData");
                    result.addDetail("Company Data Count: " + (companyData != null ? 1 : 0));

                    // Count shareholder and board member data
                    JsonNode shareholderArray = dataNode.get("shareholderData");
                    JsonNode boardMemberArray = dataNode.get("boardData");

                    result.addDetail("Shareholder Data Count: " + (shareholderArray != null ? shareholderArray.size() : 0));
                    result.addDetail("Board Member Data Count: " + (boardMemberArray != null ? boardMemberArray.size() : 0));
                }

            } else {
                result.setStatus(PROCESSING_FAILED);
                result.addDetail("❌ DataHive API call failed with status: " + (response.getStatusCode()));
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error during DataHive data retrieval: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ DataHive data retrieval failed: " + e.getMessage());
            throw new RuntimeException("DataHive data retrieval failed", e);
        }

        return result;
    }

    /**
     * Step 3: Query Snowflake directly for data verification.
     */
    private TestStepResult querySnowflakeDirectly() {
        TestStepResult result = new TestStepResult("Query Snowflake Directly", PROCESSING_INFO);

        try {
            log.info("❄️ Step 3: Querying Snowflake directly for UEN: {}", TARGET_UEN);

            // Direct queries to all DataHive views for ACRA
            log.info("Starting parallel DataHive queries for UEN: {}", TARGET_UEN);

            // Query 1: Registered company info
            String firmInfoRQueryStr = "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, " +
                    "COMPANY_TYPE_CODE, ADDRESS_ONE, ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                    "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, ADDRESS_ONE_STREET_NAME, " +
                    "ADDRESS_ONE_BUILDING_NAME, UEN " +
                    "FROM V_DH_ACRA_FIRMINFO_R WHERE UEN = '" + TARGET_UEN + "'";

            // Query 2: De-registered company info
            String firmInfoDQueryStr = "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, " +
                    "COMPANY_TYPE_CODE, ADDRESS_ONE, ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                    "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, ADDRESS_ONE_STREET_NAME, " +
                    "ADDRESS_ONE_BUILDING_NAME, UEN " +
                    "FROM V_DH_ACRA_FIRMINFO_D WHERE UEN = '" + TARGET_UEN + "'";

            // Query 3: Shareholder info
            String shareholderQueryStr = "SELECT COMPANY_UEN, SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN, SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO " +
                    "FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE COMPANY_UEN = '" + TARGET_UEN + "'";

            // Query 4: Board info
            String boardQueryStr = "SELECT POSITION_APPOINTMENT_DATE, POSITION_WITHDRAWN_WITHDRAWAL_DATE, PERSON_IDENTIFICATION_NUMBER, ENTITY_UEN, POSITION_HELD_CODE, REFERENCE_PERIOD " +
                    "FROM V_DH_ACRA_BOARD_INFO_FULL WHERE ENTITY_UEN = '" + TARGET_UEN + "'";

            // Execute all queries in parallel
            CompletableFuture<JsonNode> firmInfoRQuery = dataHiveUtil.executeQueryAsyncDataOnly(firmInfoRQueryStr);
            CompletableFuture<JsonNode> firmInfoDQuery = dataHiveUtil.executeQueryAsyncDataOnly(firmInfoDQueryStr);
            CompletableFuture<JsonNode> shareholderQuery = dataHiveUtil.executeQueryAsyncDataOnly(shareholderQueryStr);
            CompletableFuture<JsonNode> boardQuery = dataHiveUtil.executeQueryAsyncDataOnly(boardQueryStr);

            // Wait for all queries to complete
            CompletableFuture.allOf(firmInfoRQuery, firmInfoDQuery, shareholderQuery, boardQuery).join();

            // Store results
            result.setJsonData(Map.of(
                    "firmInfoR", firmInfoRQuery.get(),
                    "firmInfoD", firmInfoDQuery.get(),
                    "shareholder", shareholderQuery.get(),
                    "board", boardQuery.get()
            ));

            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ Snowflake queries completed successfully");
            result.addDetail("Registered Company Records: " + (firmInfoRQuery.get().get("data") != null ? firmInfoRQuery.get().get("data").size() : 0));
            result.addDetail("De-registered Company Records: " + (firmInfoDQuery.get().get("data") != null ? firmInfoDQuery.get().get("data").size() : 0));
            result.addDetail("Shareholder Records: " + (shareholderQuery.get().get("data") != null ? shareholderQuery.get().get("data").size() : 0));
            result.addDetail("Board Records: " + (boardQuery.get().get("data") != null ? boardQuery.get().get("data").size() : 0));

        } catch (Exception e) {
            log.error("❌ Error during Snowflake queries: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Snowflake queries failed: " + e.getMessage());
            throw new RuntimeException("Snowflake queries failed", e);
        }

        return result;
    }

    /**
     * Step 4: Verify data processing and storage.
     */
    private TestStepResult verifyExactDataMatch() {
        TestStepResult result = new TestStepResult("Verify Data Processing", PROCESSING_INFO);

        try {
            log.info("🔍 Step 4: Verifying data processing and storage");

            // Verify company detail data (custom logic specific to complete success)
            verifyCompanyDetailData(result);

            // Verify shareholder info data (custom logic specific to complete success)
            verifyShareholderInfoData(result);

            // Verify board info data (custom logic specific to complete success)
            verifyBoardInfoData(result);

            // Verify gazetted flag updates (business logic verification)
            verifyGazettedFlagUpdates(result);

            // Verify TS-ACR validation updates (critical business logic)
            verifyTsAcrValidation(result);

            // Verify suspension record creation (TS-ACR workflow)
            verifySuspensionRecordCreation(result);

            result.addDetail("✅ Data processing verification completed including business logic tables");

        } catch (Exception e) {
            log.error("❌ Error during data processing verification: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Data processing verification failed: " + e.getMessage());
            throw new RuntimeException("Data processing verification failed", e);
        }

        if (result.getDetails().stream().anyMatch(detail -> detail.contains("❌"))) {
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Data processing verification found mismatches");
        } else {
            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ All data processing verifications passed");
        }

        return result;
    }

    // ===============================
    // Custom Verification Methods
    // (These contain specific business logic for complete success flow)
    // ===============================

    private void verifyCompanyDetailData(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " company detail data for complete success flow");

            // Query Snowflake for registered company info
            String snowflakeQuery = "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, UEN " +
                    "FROM V_DH_ACRA_FIRMINFO_R WHERE UEN = '" + TARGET_UEN + "'";

            // Query database for company detail
            String dbQuery = "SELECT entity_name, entity_type, registration_date, deregistration_date, entity_status_code, company_type_code, uen " +
                    "FROM " + SCHEMA + ".ocms_dh_acra_company_detail WHERE uen = ? AND notice_no = ?";

            CompletableFuture<JsonNode> snowflakeResult = dataHiveUtil.executeQueryAsyncDataOnly(snowflakeQuery);
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(dbQuery, TARGET_UEN, NOTICE_NUMBER);

            // Wait for Snowflake result
            JsonNode snowflakeData = snowflakeResult.get();

            // Create comparison result
            CompanyComparisonResult.CompanyComparisonResultBuilder comparisonBuilder = CompanyComparisonResult.builder()
                .uen(TARGET_UEN)
                .noticeNumber(NOTICE_NUMBER);

            if (snowflakeData != null && snowflakeData.get("data") != null && snowflakeData.get("data").size() > 0) {
                JsonNode snowflakeRecord = snowflakeData.get("data").get(0);

                // Build DataHive data
                CompanyComparisonResult.CompanyData dataHiveData = CompanyComparisonResult.CompanyData.builder()
                    .entityName(getStringFromJsonArray(snowflakeRecord, 0))
                    .entityType(getStringFromJsonArray(snowflakeRecord, 1))
                    .registrationDate(parseTimestamp(getStringFromJsonArray(snowflakeRecord, 2)))
                    .deregistrationDate(parseTimestamp(getStringFromJsonArray(snowflakeRecord, 3)))
                    .entityStatusCode(getStringFromJsonArray(snowflakeRecord, 4))
                    .companyTypeCode(getStringFromJsonArray(snowflakeRecord, 5))
                    .uen(getStringFromJsonArray(snowflakeRecord, 6))
                    .build();

                comparisonBuilder.dataHiveData(dataHiveData);

                if (!dbResult.isEmpty()) {
                    Map<String, Object> dbRecord = dbResult.get(0);

                    // Build database data
                    CompanyComparisonResult.CompanyData databaseData = CompanyComparisonResult.CompanyData.builder()
                        .entityName((String) dbRecord.get("entity_name"))
                        .entityType((String) dbRecord.get("entity_type"))
                        .registrationDate(convertToLocalDateTime(dbRecord.get("registration_date")))
                        .deregistrationDate(convertToLocalDateTime(dbRecord.get("deregistration_date")))
                        .entityStatusCode((String) dbRecord.get("entity_status_code"))
                        .companyTypeCode((String) dbRecord.get("company_type_code"))
                        .uen((String) dbRecord.get("uen"))
                        .build();

                    comparisonBuilder.databaseData(databaseData);

                    // Perform detailed field comparison
                    CompanyComparisonResult comparison = comparisonBuilder.build();
                    performDetailedCompanyComparison(comparison, dataHiveData, databaseData);

                    // Add comprehensive comparison results to test step
                    result.addDetail(comparison.generateComparisonTable());
                    result.addDetail(String.format("📊 Company Comparison Summary: %s (%.1f%% match)",
                        comparison.getOverallStatus().getSymbol(),
                        comparison.getMatchPercentage()));

                    // Store structured comparison result
                    addStructuredDataToResult(result, "companyComparison", comparison);

                } else {
                    result.addDetail(ERROR_PREFIX + " [COMPANY]: No database record found for UEN: " + TARGET_UEN);
                    comparisonBuilder.overallStatus(CompanyComparisonResult.ComparisonStatus.MISSING_DATABASE);
                }
            } else {
                result.addDetail("⚠️ No Snowflake data found for V_DH_ACRA_FIRMINFO_R table UEN: " + TARGET_UEN);
                comparisonBuilder.overallStatus(CompanyComparisonResult.ComparisonStatus.MISSING_DATAHIVE);
            }

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [COMPANY]: " + e.getMessage());
            log.error("Error in company detail verification", e);
        }
    }

    private void verifyShareholderInfoData(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " shareholder info data for complete success flow");

            // Query Snowflake for shareholder info
            String snowflakeQuery = "SELECT COMPANY_UEN, SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN, SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO " +
                    "FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE COMPANY_UEN = '" + TARGET_UEN + "'";

            // Query database for shareholder info
            String dbQuery = "SELECT category, company_profile_uen, person_id_no, share_allotted_no, company_uen " +
                    "FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info WHERE company_uen = ? AND notice_no = ?";

            CompletableFuture<JsonNode> snowflakeResult = dataHiveUtil.executeQueryAsyncDataOnly(snowflakeQuery);
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(dbQuery, TARGET_UEN, NOTICE_NUMBER);

            // Wait for Snowflake result
            JsonNode snowflakeData = snowflakeResult.get();

            // Create comparison result
            ShareholderComparisonResult comparison = ShareholderComparisonResult.builder()
                .companyUen(TARGET_UEN)
                .noticeNumber(NOTICE_NUMBER)
                .build();

            if (snowflakeData != null && snowflakeData.get("data") != null && snowflakeData.get("data").size() > 0) {
                for (JsonNode snowflakeRecord : snowflakeData.get("data")) {
                    performShareholderRecordComparison(snowflakeRecord, dbResult, comparison);
                }

                // Add comprehensive comparison results to test step
                result.addDetail(comparison.generateComparisonTable());
                result.addDetail(String.format("📊 Shareholder Comparison Summary: %s (%.1f%% match)",
                    comparison.getOverallStatus().getSymbol(),
                    comparison.getMatchPercentage()));

                // Store structured comparison result
                addStructuredDataToResult(result, "shareholderComparison", comparison);

            } else {
                result.addDetail("⚠️ No Snowflake shareholder data found for V_DH_ACRA_SHAREHOLDER_GZ table UEN: " + TARGET_UEN);
                comparison.getSummaryDetails().add("No DataHive shareholder data found");
            }

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [SHAREHOLDER]: " + e.getMessage());
            log.error("Error in shareholder info verification", e);
        }
    }

    private void verifyBoardInfoData(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " board info data for complete success flow");

            // Query Snowflake for board info - enhanced query with all fields
            String snowflakeQuery = "SELECT PERSON_IDENTIFICATION_NUMBER, POSITION_HELD_CODE, POSITION_APPOINTMENT_DATE, " +
                    "POSITION_WITHDRAWN_WITHDRAWAL_DATE, REFERENCE_PERIOD, ENTITY_UEN " +
                    "FROM V_DH_ACRA_BOARD_INFO_FULL WHERE ENTITY_UEN = '" + TARGET_UEN + "'";

            // Query database for board info - enhanced query with all fields
            String dbQuery = "SELECT person_id_no, position_held_code, position_appointment_date, " +
                    "position_withdrawn_date, reference_period, entity_uen " +
                    "FROM " + SCHEMA + ".ocms_dh_acra_board_info WHERE entity_uen = ? AND notice_no = ?";

            CompletableFuture<JsonNode> snowflakeResult = dataHiveUtil.executeQueryAsyncDataOnly(snowflakeQuery);
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(dbQuery, TARGET_UEN, NOTICE_NUMBER);

            // Wait for Snowflake result
            JsonNode snowflakeData = snowflakeResult.get();

            // Create comparison result
            BoardComparisonResult comparison = BoardComparisonResult.builder()
                .entityUen(TARGET_UEN)
                .noticeNumber(NOTICE_NUMBER)
                .build();

            if (snowflakeData != null && snowflakeData.get("data") != null && snowflakeData.get("data").size() > 0) {
                for (JsonNode snowflakeRecord : snowflakeData.get("data")) {
                    performBoardRecordComparison(snowflakeRecord, dbResult, comparison);
                }

                // Add comprehensive comparison results to test step
                result.addDetail(comparison.generateComparisonTable());
                result.addDetail(String.format("📊 Board Member Comparison Summary: %s (%.1f%% match)",
                    comparison.getOverallStatus().getSymbol(),
                    comparison.getMatchPercentage()));

                // Store structured comparison result
                addStructuredDataToResult(result, "boardComparison", comparison);

            } else {
                result.addDetail("⚠️ No Snowflake board data found for V_DH_ACRA_BOARD_INFO_FULL table UEN: " + TARGET_UEN);
                comparison.getSummaryDetails().add("No DataHive board data found");
            }

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [BOARD]: " + e.getMessage());
            log.error("Error in board info verification", e);
        }
    }

    // ===============================
    // Helper Methods for Enhanced Comparison
    // ===============================

    /**
     * Helper method to add structured data to TestStepResult with consistent null checking.
     */
    private void addStructuredDataToResult(TestStepResult result, String key, Object data) {
        if (result.getJsonData() == null) {
            result.setJsonData(new java.util.HashMap<>());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonData = (Map<String, Object>) result.getJsonData();
        jsonData.put(key, data);
    }

    private String getStringFromJsonArray(JsonNode arrayNode, int index) {
        if (arrayNode.isArray() && index < arrayNode.size()) {
            JsonNode valueNode = arrayNode.get(index);
            return valueNode.isNull() ? null : valueNode.asText();
        }
        return null;
    }

    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return null;
        }
        try {
            return dateTimeHelper.convertEpochToLocalDateTime(timestampStr);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timestampStr);
            return null;
        }
    }

    private LocalDateTime convertToLocalDateTime(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        try {
            return dateTimeHelper.convertTimestampToLocalDateTime(dbValue);
        } catch (Exception e) {
            log.warn("Failed to convert database timestamp: {}", dbValue);
            return null;
        }
    }

    private void performDetailedCompanyComparison(CompanyComparisonResult comparison,
                                                  CompanyComparisonResult.CompanyData dataHiveData,
                                                  CompanyComparisonResult.CompanyData databaseData) {
        // Compare entity name
        comparison.addFieldComparison("entityName", dataHiveData.getEntityName(), databaseData.getEntityName());

        // Compare entity type
        comparison.addFieldComparison("entityType", dataHiveData.getEntityType(), databaseData.getEntityType());

        // Compare registration date
        comparison.addFieldComparison("registrationDate",
            dataHiveData.getRegistrationDate(), databaseData.getRegistrationDate());

        // Compare deregistration date
        comparison.addFieldComparison("deregistrationDate",
            dataHiveData.getDeregistrationDate(), databaseData.getDeregistrationDate());

        // Compare entity status code
        comparison.addFieldComparison("entityStatusCode",
            dataHiveData.getEntityStatusCode(), databaseData.getEntityStatusCode());

        // Compare company type code
        comparison.addFieldComparison("companyTypeCode",
            dataHiveData.getCompanyTypeCode(), databaseData.getCompanyTypeCode());

        // Compare UEN
        comparison.addFieldComparison("uen", dataHiveData.getUen(), databaseData.getUen());

        // Add summary details
        comparison.getSummaryDetails().add(String.format("Total fields compared: %d", comparison.getTotalFields()));
        comparison.getSummaryDetails().add(String.format("Fields matched: %d", comparison.getMatchedFields()));
        comparison.getSummaryDetails().add(String.format("Match percentage: %.1f%%", comparison.getMatchPercentage()));
    }

    private void performShareholderRecordComparison(JsonNode snowflakeRecord,
                                                   List<Map<String, Object>> dbResults,
                                                   ShareholderComparisonResult comparison) {
        // Extract DataHive data - array format: [COMPANY_UEN, CATEGORY, PROFILE_UEN, PERSON_ID, SHARE_ALLOTTED]
        String companyUen = getStringFromJsonArray(snowflakeRecord, 0);
        String category = getStringFromJsonArray(snowflakeRecord, 1);
        String companyProfileUen = getStringFromJsonArray(snowflakeRecord, 2);
        String personIdNo = getStringFromJsonArray(snowflakeRecord, 3);
        Integer shareAllottedNo = parseInteger(getStringFromJsonArray(snowflakeRecord, 4));

        // Build DataHive data
        ShareholderComparisonResult.ShareholderData dataHiveData = ShareholderComparisonResult.ShareholderData.builder()
            .companyUen(companyUen)
            .category(category)
            .companyProfileUen(companyProfileUen)
            .personIdNo(personIdNo)
            .shareAllottedNo(shareAllottedNo)
            .build();

        // Find matching database record
        Map<String, Object> matchingDbRecord = findMatchingShareholderRecord(category, personIdNo, companyProfileUen, dbResults);

        ShareholderComparisonResult.ShareholderData databaseData = null;
        ShareholderComparisonResult.ComparisonStatus recordStatus;

        if (matchingDbRecord != null) {
            // Build database data
            databaseData = ShareholderComparisonResult.ShareholderData.builder()
                .companyUen((String) matchingDbRecord.get("company_uen"))
                .category((String) matchingDbRecord.get("category"))
                .companyProfileUen((String) matchingDbRecord.get("company_profile_uen"))
                .personIdNo((String) matchingDbRecord.get("person_id_no"))
                .shareAllottedNo((Integer) matchingDbRecord.get("share_allotted_no"))
                .build();

            // Determine record status based on field comparison
            recordStatus = compareShareholderFields(dataHiveData, databaseData);
        } else {
            recordStatus = ShareholderComparisonResult.ComparisonStatus.MISSING_DATABASE;
        }

        // Create record comparison
        ShareholderComparisonResult.ShareholderRecordComparison recordComparison =
            ShareholderComparisonResult.ShareholderRecordComparison.builder()
                .category(category)
                .personIdNo(personIdNo)
                .companyProfileUen(companyProfileUen)
                .shareAllottedNo(shareAllottedNo)
                .dataHiveData(dataHiveData)
                .databaseData(databaseData)
                .status(recordStatus)
                .build();

        comparison.addRecordComparison(recordComparison);
    }

    private Map<String, Object> findMatchingShareholderRecord(String category, String personIdNo, String companyProfileUen,
                                                             List<Map<String, Object>> dbResults) {
        return dbResults.stream()
            .filter(record -> {
                boolean categoryMatch = category != null && category.equals(record.get("category"));
                boolean personIdMatch = personIdNo != null && personIdNo.equals(record.get("person_id_no"));
                boolean companyUenMatch = companyProfileUen != null && companyProfileUen.equals(record.get("company_profile_uen"));

                // Match by person ID first, then company UEN if person ID is null
                return categoryMatch && (personIdMatch || (personIdNo == null && companyUenMatch));
            })
            .findFirst()
            .orElse(null);
    }

    private ShareholderComparisonResult.ComparisonStatus compareShareholderFields(
            ShareholderComparisonResult.ShareholderData dataHiveData,
            ShareholderComparisonResult.ShareholderData databaseData) {

        boolean allMatch = true;

        // Compare category
        if (!objectsEqual(dataHiveData.getCategory(), databaseData.getCategory())) {
            allMatch = false;
        }

        // Compare person ID
        if (!objectsEqual(dataHiveData.getPersonIdNo(), databaseData.getPersonIdNo())) {
            allMatch = false;
        }

        // Compare company profile UEN
        if (!objectsEqual(dataHiveData.getCompanyProfileUen(), databaseData.getCompanyProfileUen())) {
            allMatch = false;
        }

        // Compare share allotted number
        if (!objectsEqual(dataHiveData.getShareAllottedNo(), databaseData.getShareAllottedNo())) {
            allMatch = false;
        }

        return allMatch ? ShareholderComparisonResult.ComparisonStatus.MATCH :
               ShareholderComparisonResult.ComparisonStatus.MISMATCH;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", value);
            return null;
        }
    }

    private boolean objectsEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    private void performBoardRecordComparison(JsonNode snowflakeRecord,
                                             List<Map<String, Object>> dbResults,
                                             BoardComparisonResult comparison) {
        // Extract DataHive data - array format: [PERSON_ID, POSITION_CODE, APPOINTMENT_DATE, WITHDRAWN_DATE, REFERENCE_PERIOD, ENTITY_UEN]
        String personIdNo = getStringFromJsonArray(snowflakeRecord, 0);
        String positionHeldCode = getStringFromJsonArray(snowflakeRecord, 1);
        LocalDateTime positionAppointmentDate = parseTimestamp(getStringFromJsonArray(snowflakeRecord, 2));
        LocalDateTime positionWithdrawnDate = parseTimestamp(getStringFromJsonArray(snowflakeRecord, 3));
        LocalDateTime referencePeriod = parseTimestamp(getStringFromJsonArray(snowflakeRecord, 4));
        String entityUen = getStringFromJsonArray(snowflakeRecord, 5);

        // Build DataHive data
        BoardComparisonResult.BoardData dataHiveData = BoardComparisonResult.BoardData.builder()
            .personIdNo(personIdNo)
            .positionHeldCode(positionHeldCode)
            .positionAppointmentDate(positionAppointmentDate)
            .positionWithdrawnDate(positionWithdrawnDate)
            .referencePeriod(referencePeriod)
            .entityUen(entityUen)
            .build();

        // Find matching database record
        Map<String, Object> matchingDbRecord = findMatchingBoardRecord(personIdNo, positionHeldCode, dbResults);

        BoardComparisonResult.BoardData databaseData = null;
        BoardComparisonResult.ComparisonStatus recordStatus;

        if (matchingDbRecord != null) {
            // Build database data
            databaseData = BoardComparisonResult.BoardData.builder()
                .personIdNo((String) matchingDbRecord.get("person_id_no"))
                .positionHeldCode((String) matchingDbRecord.get("position_held_code"))
                .positionAppointmentDate(convertToLocalDateTime(matchingDbRecord.get("position_appointment_date")))
                .positionWithdrawnDate(convertToLocalDateTime(matchingDbRecord.get("position_withdrawn_date")))
                .referencePeriod(convertToLocalDateTime(matchingDbRecord.get("reference_period")))
                .entityUen((String) matchingDbRecord.get("entity_uen"))
                .build();

            // Determine record status based on field comparison
            recordStatus = compareBoardFields(dataHiveData, databaseData);
        } else {
            recordStatus = BoardComparisonResult.ComparisonStatus.MISSING_DATABASE;
        }

        // Create record comparison
        BoardComparisonResult.BoardRecordComparison recordComparison =
            BoardComparisonResult.BoardRecordComparison.builder()
                .personIdNo(personIdNo)
                .positionHeldCode(positionHeldCode)
                .positionAppointmentDate(positionAppointmentDate)
                .positionWithdrawnDate(positionWithdrawnDate)
                .referencePeriod(referencePeriod)
                .dataHiveData(dataHiveData)
                .databaseData(databaseData)
                .status(recordStatus)
                .build();

        comparison.addRecordComparison(recordComparison);
    }

    private Map<String, Object> findMatchingBoardRecord(String personIdNo, String positionHeldCode,
                                                       List<Map<String, Object>> dbResults) {
        return dbResults.stream()
            .filter(record -> {
                boolean personIdMatch = personIdNo != null && personIdNo.equals(record.get("person_id_no"));
                boolean positionMatch = positionHeldCode != null && positionHeldCode.equals(record.get("position_held_code"));
                return personIdMatch && positionMatch;
            })
            .findFirst()
            .orElse(null);
    }

    private BoardComparisonResult.ComparisonStatus compareBoardFields(
            BoardComparisonResult.BoardData dataHiveData,
            BoardComparisonResult.BoardData databaseData) {

        boolean allMatch = true;

        // Compare person ID
        if (!objectsEqual(dataHiveData.getPersonIdNo(), databaseData.getPersonIdNo())) {
            allMatch = false;
        }

        // Compare position held code
        if (!objectsEqual(dataHiveData.getPositionHeldCode(), databaseData.getPositionHeldCode())) {
            allMatch = false;
        }

        // Compare appointment date
        if (!dateTimeHelper.isTimestampEqual(dataHiveData.getPositionAppointmentDate(), databaseData.getPositionAppointmentDate())) {
            allMatch = false;
        }

        // Compare withdrawn date
        if (!dateTimeHelper.isTimestampEqual(dataHiveData.getPositionWithdrawnDate(), databaseData.getPositionWithdrawnDate())) {
            allMatch = false;
        }

        // Compare reference period
        if (!dateTimeHelper.isTimestampEqual(dataHiveData.getReferencePeriod(), databaseData.getReferencePeriod())) {
            allMatch = false;
        }

        // Compare entity UEN
        if (!objectsEqual(dataHiveData.getEntityUen(), databaseData.getEntityUen())) {
            allMatch = false;
        }

        return allMatch ? BoardComparisonResult.ComparisonStatus.MATCH :
               BoardComparisonResult.ComparisonStatus.MISMATCH;
    }

    // ===============================
    // Business Logic Table Verification Methods
    // ===============================

    private void verifyGazettedFlagUpdates(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " gazetted flag updates for complete success flow");

            // First, get the company entity type from our company data
            String entityType = null;
            if (result.getJsonData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonData = (Map<String, Object>) result.getJsonData();
                Object companyComparison = jsonData.get("companyComparison");
                if (companyComparison instanceof CompanyComparisonResult) {
                    CompanyComparisonResult compCompare = (CompanyComparisonResult) companyComparison;
                    if (compCompare.getDatabaseData() != null) {
                        entityType = compCompare.getDatabaseData().getEntityType();
                    }
                }
            }

            // Create gazetted flag comparison based on entity type
            GazettedFlagComparisonResult gazettedComparison;
            if ("LC".equals(entityType)) {
                gazettedComparison = GazettedFlagComparisonResult.createForListedCompany(NOTICE_NUMBER, entityType);
            } else {
                gazettedComparison = GazettedFlagComparisonResult.createForNonListedCompany(NOTICE_NUMBER, entityType);
            }

            // Query actual gazetted flag value
            String gazettedQuery = "SELECT gazetted_flag FROM " + SCHEMA + ".ocms_offence_notice_owner_driver " +
                    "WHERE notice_no = ? AND owner_driver_indicator = 'O'";
            List<Map<String, Object>> gazettedResult = jdbcTemplate.queryForList(gazettedQuery, NOTICE_NUMBER);

            if (!gazettedResult.isEmpty()) {
                String actualGazettedFlag = (String) gazettedResult.get(0).get("gazetted_flag");
                gazettedComparison.setActualGazettedFlag(actualGazettedFlag);
            } else {
                gazettedComparison.setActualGazettedFlag(null);
                gazettedComparison.addVerificationDetail("No owner record found for gazetted flag check");
            }

            // Evaluate the comparison
            gazettedComparison.evaluateComparison();

            // Add results to test step
            result.addDetail(gazettedComparison.generateComparisonTable());
            result.addDetail(String.format("📊 Gazetted Flag Verification: %s",
                gazettedComparison.getOverallStatus().getSymbol()));

            // Store structured result
            addStructuredDataToResult(result, "gazettedFlagComparison", gazettedComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [GAZETTED_FLAG]: " + e.getMessage());
            log.error("Error in gazetted flag verification", e);
        }
    }

    private void verifyTsAcrValidation(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " TS-ACR validation updates for complete success flow");

            // Create expected TS-ACR validation result
            TsAcrValidationComparisonResult tsAcrComparison = TsAcrValidationComparisonResult.createExpected(NOTICE_NUMBER);

            // Query actual TS-ACR validation data
            String tsAcrQuery = "SELECT suspension_type, epr_reason_of_suspension FROM " + SCHEMA + ".ocms_valid_offence_notice " +
                    "WHERE notice_no = ?";
            List<Map<String, Object>> tsAcrResult = jdbcTemplate.queryForList(tsAcrQuery, NOTICE_NUMBER);

            if (!tsAcrResult.isEmpty()) {
                Map<String, Object> record = tsAcrResult.get(0);
                TsAcrValidationComparisonResult.TsAcrValidationData actualData =
                    TsAcrValidationComparisonResult.TsAcrValidationData.builder()
                        .noticeNo(NOTICE_NUMBER)
                        .suspensionType((String) record.get("suspension_type"))
                        .eprReasonOfSuspension((String) record.get("epr_reason_of_suspension"))
                        .build();

                tsAcrComparison.setActualData(actualData);
            } else {
                tsAcrComparison.setActualData(null);
                tsAcrComparison.addVerificationDetail("No record found in ocms_valid_offence_notice");
            }

            // Perform standard TS-ACR comparison
            tsAcrComparison.performStandardTsAcrComparison();

            // Add results to test step
            result.addDetail(tsAcrComparison.generateComparisonTable());
            result.addDetail(String.format("📊 TS-ACR Validation Summary: %s (%.1f%% match)",
                tsAcrComparison.getOverallStatus().getSymbol(),
                tsAcrComparison.getMatchPercentage()));

            // Store structured result
            addStructuredDataToResult(result, "tsAcrValidationComparison", tsAcrComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [TS_ACR]: " + e.getMessage());
            log.error("Error in TS-ACR validation verification", e);
        }
    }

    private void verifySuspensionRecordCreation(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " suspension record creation for complete success flow");

            // Query the latest suspension record for this notice
            String suspensionQuery = "SELECT TOP 1 notice_no, date_of_suspension, sr_no, suspension_source, " +
                    "suspension_type, reason_of_suspension, due_date_of_revival, suspension_remarks, " +
                    "officer_authorising_suspension, cre_user_id FROM " + SCHEMA + ".ocms_suspended_notice " +
                    "WHERE notice_no = ? ORDER BY sr_no DESC";
            List<Map<String, Object>> suspensionResult = jdbcTemplate.queryForList(suspensionQuery, NOTICE_NUMBER);

            // Get expected sr_no (would be auto-generated)
            Integer expectedSrNo = null;
            if (!suspensionResult.isEmpty()) {
                expectedSrNo = (Integer) suspensionResult.get(0).get("sr_no");
            }

            // Create expected suspension record result
            SuspensionRecordComparisonResult suspensionComparison =
                SuspensionRecordComparisonResult.createExpected(NOTICE_NUMBER, expectedSrNo);

            if (!suspensionResult.isEmpty()) {
                Map<String, Object> record = suspensionResult.get(0);

                // Parse dates carefully
                LocalDateTime dateOfSuspension = null;
                Object suspensionDateObj = record.get("date_of_suspension");
                if (suspensionDateObj != null) {
                    dateOfSuspension = convertToLocalDateTime(suspensionDateObj);
                }

                LocalDate dueDateOfRevival = null;
                Object revivalDateObj = record.get("due_date_of_revival");
                if (revivalDateObj != null && revivalDateObj instanceof java.sql.Date) {
                    dueDateOfRevival = ((java.sql.Date) revivalDateObj).toLocalDate();
                }

                SuspensionRecordComparisonResult.SuspensionRecordData actualData =
                    SuspensionRecordComparisonResult.SuspensionRecordData.builder()
                        .noticeNo((String) record.get("notice_no"))
                        .dateOfSuspension(dateOfSuspension)
                        .srNo((Integer) record.get("sr_no"))
                        .suspensionSource((String) record.get("suspension_source"))
                        .suspensionType((String) record.get("suspension_type"))
                        .reasonOfSuspension((String) record.get("reason_of_suspension"))
                        .dueDateOfRevival(dueDateOfRevival)
                        .suspensionRemarks((String) record.get("suspension_remarks"))
                        .officerAuthorisingSuspension((String) record.get("officer_authorising_suspension"))
                        .creUserId((String) record.get("cre_user_id"))
                        .build();

                suspensionComparison.setActualData(actualData);
            } else {
                suspensionComparison.setActualData(null);
                suspensionComparison.addVerificationDetail("No suspension record found in ocms_suspended_notice");
            }

            // Perform standard suspension record comparison
            suspensionComparison.performStandardSuspensionComparison();

            // Add results to test step
            result.addDetail(suspensionComparison.generateComparisonTable());
            result.addDetail(String.format("📊 Suspension Record Summary: %s (%.1f%% match)",
                suspensionComparison.getOverallStatus().getSymbol(),
                suspensionComparison.getMatchPercentage()));

            // Store structured result
            addStructuredDataToResult(result, "suspensionRecordComparison", suspensionComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [SUSPENSION]: " + e.getMessage());
            log.error("Error in suspension record verification", e);
        }
    }

}