package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.testing.main.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Step Execution Helper - Orchestrates the 4-step test pattern for OCMS 15
 *
 * Follows TESTING_FRAMEWORK_GUIDE.md 4-step pattern:
 * Step 1: Load Test Scenarios (from scenario.json)
 * Step 2: Trigger Change Processing Stage API (OCMS/PLUS)
 * Step 3: Fetch Verification Data (VON, ONOD, change processing, parameters)
 * Step 4: Verify Business Logic (stage, amount, audit, DH/MHA, Excel report)
 *
 * Setup: POST /v1/ocms15-test-setup (handled by Ocms15TestSetupService)
 * Cleanup: DELETE /v1/ocms15-test-cleanup (handled by Ocms15TestSetupService)
 */
@Slf4j
@Component
public class StepExecutionHelper {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EndpointHelper endpointHelper;

    @Autowired
    private VerificationHelper verificationHelper;

    @Autowired
    private TestContext testContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExcelHelper excelHelper;

    /**
     * Step 1: Load Test Scenarios from scenario.json
     *
     * @param testOcmsFlow Whether to test OCMS manual flow
     * @param testPlusFlow Whether to test PLUS integration flow
     * @return StepResult with loaded scenarios
     */
    public StepResult executeStep1_LoadScenarios(boolean testOcmsFlow, boolean testPlusFlow) {
        StepResult result = new StepResult();
        result.setStepName("Step 1: Load Test Scenarios");
        result.setDescription("Load test scenarios from scenario.json");

        try {
            long startTime = System.currentTimeMillis();

            // Load scenario.json from classpath
            ClassPathResource resource = new ClassPathResource("testing/ocms15_changeprocessing/data/scenario.json");
            InputStream inputStream = resource.getInputStream();

            List<Map<String, Object>> allScenarios = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Filter scenarios based on flow type
            List<Map<String, Object>> filteredScenarios = new ArrayList<>();
            for (Map<String, Object> scenario : allScenarios) {
                String flow = (String) scenario.get("flow");
                if ("OCMS".equals(flow) && testOcmsFlow) {
                    filteredScenarios.add(scenario);
                } else if ("PLUS".equals(flow) && testPlusFlow) {
                    filteredScenarios.add(scenario);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.setSuccess(true);
            result.setMessage("Loaded " + filteredScenarios.size() + " scenarios (OCMS: " + testOcmsFlow + ", PLUS: " + testPlusFlow + ")");
            result.setDetails(Map.of(
                "totalScenarios", allScenarios.size(),
                "filteredScenarios", filteredScenarios.size(),
                "scenarios", filteredScenarios
            ));
            result.setDuration(duration + "ms");

            log.info("Step 1 completed: {}", result.getMessage());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error in Step 1: " + e.getMessage());
            result.setDetails(Map.of("error", e.getMessage()));
            log.error("Step 1 failed: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Step 2: Trigger Change Processing Stage API
     *
     * @param scenarios List of scenarios to execute
     * @param triggerApi Whether to actually call the API (true) or skip (false)
     * @return StepResult with API call results
     */
    public StepResult executeStep2_TriggerApi(List<Map<String, Object>> scenarios, boolean triggerApi) {
        StepResult result = new StepResult();
        result.setStepName("Step 2: Trigger Change Processing Stage API");
        result.setDescription(triggerApi ? "Call change-processing-stage API" : "SKIP (dry run mode)");

        try {
            long startTime = System.currentTimeMillis();

            if (!triggerApi) {
                result.setSuccess(true);
                result.setMessage("Skipped API calls (dry run mode)");
                result.setDetails(Map.of("skipped", true));
                return result;
            }

            List<Map<String, Object>> apiResults = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");
                String newStage = (String) scenario.get("newStage");
                String flow = (String) scenario.get("flow");
                String remarks = (String) scenario.get("remarks");

                Map<String, Object> apiResult;

                if ("OCMS".equals(flow)) {
                    // Call OCMS manual API
                    apiResult = endpointHelper.callOcmsChangeProcessingStage(noticeNo, newStage, remarks);
                } else if ("PLUS".equals(flow)) {
                    // Call PLUS integration API
                    apiResult = endpointHelper.callPlusChangeProcessingStage(noticeNo, newStage);
                } else {
                    apiResult = Map.of("success", false, "error", "Unknown flow: " + flow);
                }

                if (Boolean.TRUE.equals(apiResult.get("success"))) {
                    successCount++;
                } else {
                    failCount++;
                }

                apiResults.add(Map.of(
                    "noticeNo", noticeNo,
                    "flow", flow,
                    "result", apiResult
                ));
            }

            long duration = System.currentTimeMillis() - startTime;

            result.setSuccess(failCount == 0);
            result.setMessage("API calls: " + successCount + " success, " + failCount + " failed");
            result.setDetails(Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "results", apiResults
            ));
            result.setDuration(duration + "ms");

            log.info("Step 2 completed: {}", result.getMessage());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error in Step 2: " + e.getMessage());
            result.setDetails(Map.of("error", e.getMessage()));
            log.error("Step 2 failed: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Step 3: Fetch Verification Data from database
     *
     * @return StepResult with fetched data
     */
    public StepResult executeStep3_FetchData() {
        StepResult result = new StepResult();
        result.setStepName("Step 3: Fetch Verification Data");
        result.setDescription("Fetch VON, ONOD, change processing, and parameter data");

        try {
            long startTime = System.currentTimeMillis();

            List<String> noticeNumbers = testContext.getCreatedNoticeNumbers();

            if (noticeNumbers == null || noticeNumbers.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No created notices found in context");
                result.setDetails(Map.of("error", "testContext.createdNoticeNumbers is empty"));
                return result;
            }

            // Fetch VON data
            List<Map<String, Object>> vonData = fetchVonData(noticeNumbers);
            testContext.setVonData(vonData);

            // Fetch ONOD data
            List<Map<String, Object>> onodData = fetchOnodData(noticeNumbers);
            testContext.setOnodData(onodData);

            // Fetch change processing data
            List<Map<String, Object>> changeData = fetchChangeProcessingData(noticeNumbers);
            testContext.setChangeProcessingData(changeData);

            // Fetch parameter data (for amount calculation verification)
            List<Map<String, Object>> paramData = fetchParameterData();
            testContext.setParameterData(paramData);

            long duration = System.currentTimeMillis() - startTime;

            result.setSuccess(true);
            result.setMessage("Fetched data: " + vonData.size() + " VON, " + onodData.size() + " ONOD, " + changeData.size() + " change records");
            result.setDetails(Map.of(
                "vonCount", vonData.size(),
                "onodCount", onodData.size(),
                "changeCount", changeData.size(),
                "paramCount", paramData.size()
            ));
            result.setDuration(duration + "ms");

            log.info("Step 3 completed: {}", result.getMessage());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error in Step 3: " + e.getMessage());
            result.setDetails(Map.of("error", e.getMessage()));
            log.error("Step 3 failed: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Step 4: Verify Business Logic
     *
     * @param scenarios List of scenarios to verify
     * @param showDetails Whether to show detailed verification results
     * @return StepResult with verification results
     */
    public StepResult executeStep4_VerifyLogic(List<Map<String, Object>> scenarios, boolean showDetails) {
        StepResult result = new StepResult();
        result.setStepName("Step 4: Verify Business Logic");
        result.setDescription("Verify stage progression, amount, audit trail, DH/MHA flag, Excel report");

        try {
            long startTime = System.currentTimeMillis();

            List<Map<String, Object>> verificationResults = new ArrayList<>();
            int passCount = 0;
            int failCount = 0;

            // Part A: Verify database records
            for (Map<String, Object> scenario : scenarios) {
                String noticeNo = (String) scenario.get("noticeNo");

                // Get data from context
                Map<String, Object> vonData = testContext.getVonByNoticeNo(noticeNo);
                Map<String, Object> onodData = testContext.getOnodByNoticeNo(noticeNo);
                List<Map<String, Object>> changeData = testContext.getChangeProcessingByNoticeNo(noticeNo);

                // Verify scenario
                Map<String, Object> verification = verificationHelper.verifyScenario(
                    scenario, vonData, onodData, changeData
                );

                if (Boolean.TRUE.equals(verification.get("success"))) {
                    passCount++;
                } else {
                    failCount++;
                }

                verificationResults.add(Map.of(
                    "noticeNo", noticeNo,
                    "scenario", scenario.get("scenario"),
                    "verification", showDetails ? verification : Map.of("success", verification.get("success"))
                ));
            }

            // Part B: Download and verify Excel report
            Map<String, Object> excelVerification = verifyExcelReport(showDetails);

            long duration = System.currentTimeMillis() - startTime;

            result.setSuccess(failCount == 0 && Boolean.TRUE.equals(excelVerification.get("success")));
            result.setMessage("Verification: " + passCount + " passed, " + failCount + " failed | Excel: " +
                (Boolean.TRUE.equals(excelVerification.get("success")) ? "✅ PASS" : "❌ FAIL"));
            result.setDetails(Map.of(
                "passCount", passCount,
                "failCount", failCount,
                "results", verificationResults,
                "excelReport", excelVerification
            ));
            result.setDuration(duration + "ms");

            log.info("Step 4 completed: {}", result.getMessage());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error in Step 4: " + e.getMessage());
            result.setDetails(Map.of("error", e.getMessage()));
            log.error("Step 4 failed: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Verify Excel report: Download and check 16 columns
     */
    private Map<String, Object> verifyExcelReport(boolean showDetails) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Calculate date range (today)
            String today = java.time.LocalDate.now().toString();

            // Download Excel report
            Map<String, Object> downloadResult = endpointHelper.downloadChangeProcessingReport(today, today);

            if (!Boolean.TRUE.equals(downloadResult.get("success"))) {
                result.put("success", false);
                result.put("error", "Failed to download Excel report: " + downloadResult.get("error"));
                return result;
            }

            byte[] fileData = (byte[]) downloadResult.get("fileData");
            if (fileData == null || fileData.length == 0) {
                result.put("success", false);
                result.put("error", "Excel file is empty");
                return result;
            }

            // Parse Excel file
            Map<String, Object> parseResult = excelHelper.parseExcelFile(fileData);

            if (!Boolean.TRUE.equals(parseResult.get("success"))) {
                result.put("success", false);
                result.put("error", "Failed to parse Excel: " + parseResult.get("error"));
                return result;
            }

            @SuppressWarnings("unchecked")
            List<String> headers = (List<String>) parseResult.get("headers");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) parseResult.get("rows");

            // Verify 16 columns
            List<String> expectedColumns = ExcelHelper.getExpectedChangeProcessingColumns();
            Map<String, Object> columnVerification = excelHelper.verifyColumnStructure(headers, expectedColumns);

            // Verify test notices appear in report
            List<String> createdNotices = testContext.getCreatedNoticeNumbers();
            int foundNotices = 0;
            for (Map<String, Object> row : rows) {
                String noticeNo = (String) row.get("Notice No");
                if (createdNotices.contains(noticeNo)) {
                    foundNotices++;
                }
            }

            boolean allNoticesFound = (foundNotices == createdNotices.size());

            result.put("success", Boolean.TRUE.equals(columnVerification.get("success")) && allNoticesFound);
            result.put("fileSize", fileData.length);
            result.put("fileName", downloadResult.get("fileName"));
            result.put("rowCount", rows.size());
            result.put("columnCount", headers.size());
            result.put("columnVerification", columnVerification);
            result.put("expectedNotices", createdNotices.size());
            result.put("foundNotices", foundNotices);

            if (showDetails) {
                result.put("headers", headers);
                result.put("sampleRows", rows.size() > 5 ? rows.subList(0, 5) : rows);
            }

            log.info("Excel report verified: {} rows, {} columns, {}/{} notices found",
                rows.size(), headers.size(), foundNotices, createdNotices.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error verifying Excel report: {}", e.getMessage(), e);
        }

        return result;
    }

    // ========== Data Fetching Methods ==========

    private List<Map<String, Object>> fetchVonData(List<String> noticeNumbers) {
        String sql = "SELECT * FROM VON WHERE notice_no IN (" + buildInClause(noticeNumbers) + ")";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> fetchOnodData(List<String> noticeNumbers) {
        String sql = "SELECT * FROM ONOD WHERE notice_no IN (" + buildInClause(noticeNumbers) + ")";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> fetchChangeProcessingData(List<String> noticeNumbers) {
        String sql = "SELECT * FROM OCMS_CHANGE_OF_PROCESSING WHERE notice_no IN (" + buildInClause(noticeNumbers) + ") ORDER BY date_of_change DESC";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> fetchParameterData() {
        String sql = "SELECT * FROM OCMS_PARAMETER WHERE param_name IN ('COMPOSITION_AMOUNT', 'ADMINISTRATION_FEE', 'SURCHARGE_RATE')";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Build SQL IN clause from list of notice numbers
     */
    private String buildInClause(List<String> noticeNumbers) {
        return noticeNumbers.stream()
            .map(n -> "'" + n + "'")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
}
