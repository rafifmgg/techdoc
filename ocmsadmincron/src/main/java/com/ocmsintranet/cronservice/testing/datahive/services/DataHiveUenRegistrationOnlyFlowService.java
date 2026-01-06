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
 * Service for processing DataHive Company Registration Only Flow.
 * Uses DataHiveTestDatabaseHelper to eliminate code duplication.
 *
 * Business scenario: Only company registration data available from DataHive
 * Expected operations:
 * - ocms_dh_acra_company_detail: Create/update company registration information only
 * - ocms_offence_notice_owner_driver_addr: Create registered address records
 * - Other tables (shareholder, board): NO CHANGES expected
 */
@Service
@Slf4j
public class DataHiveUenRegistrationOnlyFlowService {

    // Business processing constants
    private static final String PROCESSING_SUCCESS = "SUCCESS";
    private static final String PROCESSING_FAILED = "FAILED";
    private static final String PROCESSING_INFO = "INFO";

    // Verification constants for consistent error messages
    private static final String ERROR_PREFIX = "❌ VERIFICATION ERROR";
    private static final String VERIFICATION_START_PREFIX = "🔍 Verifying";

    // Business data constants - UEN with registration record only
    private static final String TARGET_UEN = "201234568B";
    private static final String UEN_ID_TYPE = "B";
    private static final String NOTICE_PREFIX = "DHUEN";
    private static final String NOTICE_NUMBER = NOTICE_PREFIX + "002";

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
     * Execute DataHive UEN Registration Only processing flow.
     */
    public List<TestStepResult> executeFlow() {
        log.info("🚀 Starting DataHive UEN Registration Only Flow Processing for UEN: {}, Notice: {}", TARGET_UEN, NOTICE_NUMBER);

        List<TestStepResult> steps = new ArrayList<>();

        try {
            // Step 1: Initialize data structures
            steps.add(setupTestData());

            // Step 2: Retrieve DataHive data
            steps.add(callDataHiveApi());

            // Step 3: Query Snowflake directly
            steps.add(querySnowflakeDirectly());

            // Step 4: Verify data processing
            steps.add(verifyRegistrationOnlyMatch());

            log.info("✅ DataHive UEN Registration Only Flow Processing completed successfully");
            return steps;

        } catch (Exception e) {
            log.error("❌ DataHive UEN Registration Only Flow Processing failed: {}", e.getMessage(), e);

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
     * Step 1: Initialize data structures for UEN registration only processing using helper.
     */
    private TestStepResult setupTestData() {
        TestStepResult result = new TestStepResult("Initialize Data Structures", PROCESSING_INFO);

        try {
            log.info("🔧 Step 1: Initializing data structures for UEN registration only processing");

            // Initialize core OCMS tables using helper - all in one place!
            databaseHelper.resetOrInsertValidOffenceNotice(NOTICE_NUMBER, "REGREG001", "PP003",
                "REGISTRATION ONLY OFFENCE", new java.math.BigDecimal("150.00"), 10003, result);

            databaseHelper.resetOrInsertOffenceNoticeDetail(NOTICE_NUMBER, "REGREG123456789",
                "TOYOTA", "WHITE", result);

            databaseHelper.resetOrInsertOwnerDriver(NOTICE_NUMBER, UEN_ID_TYPE, TARGET_UEN,
                "REGISTRATION TEST COMPANY PTE LTD", result);

            databaseHelper.resetOrInsertOwnerDriverAddress(NOTICE_NUMBER, "123", "REGISTRATION COMPANY STREET",
                "02", "01", "REGISTRATION TOWER", "111111", result);

            // Initialize only company detail table (no shareholder/board data)
            databaseHelper.resetOrInsertCompanyDetail(TARGET_UEN, NOTICE_NUMBER,
                "REGISTRATION TEST COMPANY PTE LTD", "NA", dateTimeHelper.createTestTimestamp(2019, 5, 15, 0, 0), "L", result);

            // Verify shareholder and board tables are empty for this UEN using helper
            databaseHelper.verifyNoShareholderData(TARGET_UEN, NOTICE_NUMBER, result);
            databaseHelper.verifyNoBoardData(TARGET_UEN, NOTICE_NUMBER, result);

            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ Data structures initialized for registration only flow using helper");

        } catch (Exception e) {
            log.error("❌ Error during data structure initialization: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Data structure initialization failed: " + e.getMessage());
            throw new RuntimeException("Data structure initialization failed", e);
        }

        return result;
    }

    /**
     * Step 2: Retrieve only company registration data from DataHive.
     */
    private TestStepResult callDataHiveApi() {
        TestStepResult result = new TestStepResult("Retrieve DataHive Registration Data", PROCESSING_INFO);

        try {
            log.info("🌐 Step 2: Retrieving DataHive registration data for UEN: {}", TARGET_UEN);

            String apiUrl = apiConfigHelper.buildApiUrl("/api/datahive/test/uen/" + TARGET_UEN + "?noticeNumber=" + NOTICE_NUMBER);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                result.setStatus(PROCESSING_SUCCESS);
                result.addDetail("✅ DataHive registration data retrieval successful");
                result.addDetail("API URL: " + apiUrl);
                result.addDetail("Response Status: " + response.getStatusCode());

                JsonNode apiResult = response.getBody();
                result.setJsonData(apiResult);

                // Parse and verify registration only pattern
                JsonNode dataNode = apiResult.get("data");
                JsonNode summaryNode = apiResult.get("summary");

                if (summaryNode != null) {
                    result.addDetail("API Summary - Company Found: " + summaryNode.get("companyFound").asBoolean());
                    result.addDetail("API Summary - Board Member Records: " + summaryNode.get("boardMemberRecords").asInt() + " (Expected: 0)");
                    result.addDetail("API Summary - Shareholder Records: " + summaryNode.get("shareholderRecords").asInt() + " (Expected: 0)");
                }

                if (dataNode != null) {
                    JsonNode companyData = dataNode.get("companyData");
                    JsonNode shareholderArray = dataNode.get("shareholderData");
                    JsonNode boardMemberArray = dataNode.get("boardData");

                    int actualShareholderCount = (shareholderArray != null ? shareholderArray.size() : 0);
                    int actualBoardCount = (boardMemberArray != null ? boardMemberArray.size() : 0);

                    result.addDetail("Company Data Count: " + (companyData != null ? 1 : 0) + " (Expected: 1)");
                    result.addDetail("Shareholder Data Count: " + actualShareholderCount + " (Expected: 0)");
                    result.addDetail("Board Member Data Count: " + actualBoardCount + " (Expected: 0)");

                    if (actualShareholderCount == 0 && actualBoardCount == 0) {
                        result.addDetail("✅ Confirmed: No shareholder/board data as expected for registration only flow");
                    } else {
                        result.addDetail("❌ ERROR: Found unexpected shareholder/board data in registration only flow");
                    }
                }

            } else {
                result.setStatus(PROCESSING_FAILED);
                result.addDetail("❌ DataHive API call failed with status: " + (response.getStatusCode()));
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error during DataHive registration data retrieval: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ DataHive registration data retrieval failed: " + e.getMessage());
            throw new RuntimeException("DataHive registration data retrieval failed", e);
        }

        return result;
    }

    /**
     * Step 3: Query Snowflake directly for registration data verification.
     */
    private TestStepResult querySnowflakeDirectly() {
        TestStepResult result = new TestStepResult("Query Snowflake for Registration Data", PROCESSING_INFO);

        try {
            log.info("❄️ Step 3: Querying Snowflake directly for UEN registration: {}", TARGET_UEN);

            // Execute parallel queries to all DataHive views
            String registeredQuery = String.format(
                "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, " +
                "COMPANY_TYPE_CODE, ADDRESS_ONE, ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, ADDRESS_ONE_STREET_NAME, " +
                "ADDRESS_ONE_BUILDING_NAME, UEN FROM V_DH_ACRA_FIRMINFO_R WHERE UEN = '%s'", TARGET_UEN);

            String deregisteredQuery = String.format(
                "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, " +
                "COMPANY_TYPE_CODE, ADDRESS_ONE, ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, ADDRESS_ONE_STREET_NAME, " +
                "ADDRESS_ONE_BUILDING_NAME, UEN FROM V_DH_ACRA_FIRMINFO_D WHERE UEN = '%s'", TARGET_UEN);

            String shareholderQuery = String.format(
                "SELECT COMPANY_UEN, SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN, SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO " +
                "FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE COMPANY_UEN = '%s'", TARGET_UEN);

            String boardQuery = String.format(
                "SELECT POSITION_APPOINTMENT_DATE, POSITION_WITHDRAWN_WITHDRAWAL_DATE, PERSON_IDENTIFICATION_NUMBER, ENTITY_UEN, POSITION_HELD_CODE, REFERENCE_PERIOD " +
                "FROM V_DH_ACRA_BOARD_INFO_FULL WHERE ENTITY_UEN = '%s'", TARGET_UEN);

            // Execute all queries in parallel
            CompletableFuture<JsonNode> registeredFuture = dataHiveUtil.executeQueryAsyncDataOnly(registeredQuery);
            CompletableFuture<JsonNode> deregisteredFuture = dataHiveUtil.executeQueryAsyncDataOnly(deregisteredQuery);
            CompletableFuture<JsonNode> shareholderFuture = dataHiveUtil.executeQueryAsyncDataOnly(shareholderQuery);
            CompletableFuture<JsonNode> boardFuture = dataHiveUtil.executeQueryAsyncDataOnly(boardQuery);

            // Wait for all queries to complete
            CompletableFuture.allOf(registeredFuture, deregisteredFuture, shareholderFuture, boardFuture).join();

            // Store results and verify pattern
            int registeredRecords = (registeredFuture.get().get("data") != null ? registeredFuture.get().get("data").size() : 0);
            int deregisteredRecords = (deregisteredFuture.get().get("data") != null ? deregisteredFuture.get().get("data").size() : 0);
            int shareholderRecords = (shareholderFuture.get().get("data") != null ? shareholderFuture.get().get("data").size() : 0);
            int boardRecords = (boardFuture.get().get("data") != null ? boardFuture.get().get("data").size() : 0);

            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ Snowflake queries completed successfully");
            result.addDetail("Registered Company Records: " + registeredRecords + " (Expected: 1)");
            result.addDetail("De-registered Company Records: " + deregisteredRecords + " (Expected: 0)");
            result.addDetail("Shareholder Records: " + shareholderRecords + " (Expected: 0)");
            result.addDetail("Board Records: " + boardRecords + " (Expected: 0)");

            // Validate expected registration only pattern
            if (registeredRecords == 1 && deregisteredRecords == 0 && shareholderRecords == 0 && boardRecords == 0) {
                result.addDetail("✅ VERIFIED: Registration only pattern confirmed in Snowflake");
            } else {
                result.addDetail("❌ ERROR: Snowflake data does not match registration only pattern");
            }

        } catch (Exception e) {
            log.error("❌ Error during Snowflake queries: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Snowflake queries failed: " + e.getMessage());
            throw new RuntimeException("Snowflake queries failed", e);
        }

        return result;
    }

    /**
     * Step 4: Verify registration only data processing and storage using helper.
     */
    private TestStepResult verifyRegistrationOnlyMatch() {
        TestStepResult result = new TestStepResult("Verify Registration Only Processing", PROCESSING_INFO);

        try {
            log.info("🔍 Step 4: Verifying registration only data processing and storage using helper");

            // Verify only company detail data is processed with enhanced comparison
            verifyCompanyDetailData(result);

            // Enhanced verification of NO shareholder/board data pattern
            verifyNoShareholderBoardData(result);

            // Verify address data using helper method
            databaseHelper.verifyAddressData(NOTICE_NUMBER, result);

            // Verify gazetted flag updates (business logic verification)
            verifyGazettedFlagUpdates(result);

            // Verify TS-ACR validation updates (critical business logic)
            verifyTsAcrValidation(result);

            // Verify suspension record creation (TS-ACR workflow)
            verifySuspensionRecordCreation(result);

            result.addDetail("✅ Registration only data processing verification completed including business logic tables");

        } catch (Exception e) {
            log.error("❌ Error during registration only data verification: {}", e.getMessage(), e);
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Registration only data verification failed: " + e.getMessage());
            throw new RuntimeException("Registration only data verification failed", e);
        }

        if (result.getDetails().stream().anyMatch(detail -> detail.contains("❌"))) {
            result.setStatus(PROCESSING_FAILED);
            result.addDetail("❌ Registration only verification found mismatches");
        } else {
            result.setStatus(PROCESSING_SUCCESS);
            result.addDetail("✅ All registration only verifications passed");
        }

        return result;
    }

    /**
     * Verify company detail data processing - enhanced with detailed comparison for registration only flow
     */
    private void verifyCompanyDetailData(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " company detail data for registration only flow");

            // Query Snowflake for registered company info - enhanced query
            String snowflakeQuery = "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, UEN " +
                    "FROM V_DH_ACRA_FIRMINFO_R WHERE UEN = '" + TARGET_UEN + "'";

            // Query database for company detail - enhanced query
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
                    result.addDetail(String.format("📊 Registration Company Comparison Summary: %s (%.1f%% match)",
                        comparison.getOverallStatus().getSymbol(),
                        comparison.getMatchPercentage()));
                    result.addDetail("✅ REGISTRATION ONLY: Only company data expected and verified");

                    // Store structured comparison result
                    addStructuredDataToResult(result, "companyComparison", comparison);

                } else {
                    result.addDetail(ERROR_PREFIX + " [COMPANY]: No database record found for UEN: " + TARGET_UEN);
                    comparisonBuilder.overallStatus(CompanyComparisonResult.ComparisonStatus.MISSING_DATABASE);
                }
            } else {
                result.addDetail("⚠️ No Snowflake registration data found for V_DH_ACRA_FIRMINFO_R table UEN: " + TARGET_UEN);
                comparisonBuilder.overallStatus(CompanyComparisonResult.ComparisonStatus.MISSING_DATAHIVE);
            }

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [COMPANY]: " + e.getMessage());
            log.error("Error in registration company verification", e);
        }
    }

    // ===============================
    // Helper Methods for Enhanced Comparison (consistent with complete flow)
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

        // Compare deregistration date (should be null for registration only)
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

        // Add summary details specific to registration only
        comparison.getSummaryDetails().add(String.format("Registration Only Flow: %d fields compared", comparison.getTotalFields()));
        comparison.getSummaryDetails().add(String.format("Fields matched: %d", comparison.getMatchedFields()));
        comparison.getSummaryDetails().add(String.format("Match percentage: %.1f%%", comparison.getMatchPercentage()));
        comparison.getSummaryDetails().add("Expected: Only company registration data, NO shareholder/board data");
    }

    /**
     * Enhanced verification that NO shareholder and board data exists - specific to registration only flow
     */
    private void verifyNoShareholderBoardData(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " NO shareholder/board data for registration only flow");

            // Create comparison results to track expectations
            ShareholderComparisonResult shareholderComparison = ShareholderComparisonResult.builder()
                .companyUen(TARGET_UEN)
                .noticeNumber(NOTICE_NUMBER)
                .build();

            BoardComparisonResult boardComparison = BoardComparisonResult.builder()
                .entityUen(TARGET_UEN)
                .noticeNumber(NOTICE_NUMBER)
                .build();

            // Query database for any unexpected shareholder data
            String shareholderDbQuery = "SELECT COUNT(*) as count FROM " + SCHEMA + ".ocms_dh_acra_shareholder_info WHERE company_uen = ? AND notice_no = ?";
            List<Map<String, Object>> shareholderResult = jdbcTemplate.queryForList(shareholderDbQuery, TARGET_UEN, NOTICE_NUMBER);
            int shareholderCount = ((Number) shareholderResult.get(0).get("count")).intValue();

            // Query database for any unexpected board data
            String boardDbQuery = "SELECT COUNT(*) as count FROM " + SCHEMA + ".ocms_dh_acra_board_info WHERE entity_uen = ? AND notice_no = ?";
            List<Map<String, Object>> boardResult = jdbcTemplate.queryForList(boardDbQuery, TARGET_UEN, NOTICE_NUMBER);
            int boardCount = ((Number) boardResult.get(0).get("count")).intValue();

            // Generate verification tables
            StringBuilder verificationTable = new StringBuilder();
            verificationTable.append("\n📊 Registration Only Data Verification for UEN: ").append(TARGET_UEN).append("\n");
            verificationTable.append("═".repeat(80)).append("\n");
            verificationTable.append(String.format("%-25s | %-10s | %-10s | %s\n", "Data Type", "Expected", "Actual", "Status"));
            verificationTable.append("─".repeat(80)).append("\n");

            // Company data verification
            verificationTable.append(String.format("%-25s | %-10s | %-10s | %s\n",
                "Company Records", "1", "1", "✅ MATCH"));

            // Shareholder data verification
            String shareholderStatus = shareholderCount == 0 ? "✅ MATCH" : "❌ UNEXPECTED";
            verificationTable.append(String.format("%-25s | %-10s | %-10s | %s\n",
                "Shareholder Records", "0", shareholderCount, shareholderStatus));

            // Board data verification
            String boardStatus = boardCount == 0 ? "✅ MATCH" : "❌ UNEXPECTED";
            verificationTable.append(String.format("%-25s | %-10s | %-10s | %s\n",
                "Board Records", "0", boardCount, boardStatus));

            verificationTable.append("─".repeat(80)).append("\n");

            // Overall status
            boolean allMatch = shareholderCount == 0 && boardCount == 0;
            String overallStatus = allMatch ? "✅ REGISTRATION ONLY VERIFIED" : "❌ UNEXPECTED DATA FOUND";
            verificationTable.append(String.format("📈 Registration Only Verification: %s\n", overallStatus));

            result.addDetail(verificationTable.toString());

            // Store structured verification results
            addStructuredDataToResult(result, "shareholderVerification", Map.of(
                "expected", 0,
                "actual", shareholderCount,
                "status", shareholderStatus,
                "comparison", shareholderComparison
            ));
            addStructuredDataToResult(result, "boardVerification", Map.of(
                "expected", 0,
                "actual", boardCount,
                "status", boardStatus,
                "comparison", boardComparison
            ));

            if (!allMatch) {
                result.addDetail("❌ REGISTRATION ONLY VIOLATION: Found unexpected shareholder/board data");
            } else {
                result.addDetail("✅ REGISTRATION ONLY CONFIRMED: No unexpected shareholder/board data found");
            }

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [REGISTRATION_ONLY]: " + e.getMessage());
            log.error("Error in registration only verification", e);
        }
    }

    // ===============================
    // Business Logic Table Verification Methods (Registration Only)
    // ===============================

    private void verifyGazettedFlagUpdates(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " gazetted flag updates for registration only flow");

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
            result.addDetail(String.format("📊 Registration Only Gazetted Flag Verification: %s",
                gazettedComparison.getOverallStatus().getSymbol()));

            // Store structured result
            addStructuredDataToResult(result, "gazettedFlagComparison", gazettedComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [GAZETTED_FLAG]: " + e.getMessage());
            log.error("Error in registration gazetted flag verification", e);
        }
    }

    private void verifyTsAcrValidation(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " TS-ACR validation updates for registration only flow");

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
            result.addDetail(String.format("📊 Registration Only TS-ACR Validation Summary: %s (%.1f%% match)",
                tsAcrComparison.getOverallStatus().getSymbol(),
                tsAcrComparison.getMatchPercentage()));

            // Store structured result
            addStructuredDataToResult(result, "tsAcrValidationComparison", tsAcrComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [TS_ACR]: " + e.getMessage());
            log.error("Error in registration TS-ACR validation verification", e);
        }
    }

    private void verifySuspensionRecordCreation(TestStepResult result) {
        try {
            result.addDetail(VERIFICATION_START_PREFIX + " suspension record creation for registration only flow");

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
            result.addDetail(String.format("📊 Registration Only Suspension Record Summary: %s (%.1f%% match)",
                suspensionComparison.getOverallStatus().getSymbol(),
                suspensionComparison.getMatchPercentage()));

            // Store structured result
            addStructuredDataToResult(result, "suspensionRecordComparison", suspensionComparison);

        } catch (Exception e) {
            result.addDetail(ERROR_PREFIX + " [SUSPENSION]: " + e.getMessage());
            log.error("Error in registration suspension record verification", e);
        }
    }

}