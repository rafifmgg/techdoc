package com.ocmsintranet.apiservice.testing.furnish_manual.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_manual.helpers.EndpointHelper;
import com.ocmsintranet.apiservice.testing.furnish_manual.helpers.StepExecutionHelper;
import com.ocmsintranet.apiservice.testing.furnish_manual.helpers.TestContext;
import com.ocmsintranet.apiservice.testing.furnish_manual.helpers.VerificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishTestServiceImpl implements ManualFurnishTestService {

    private final ManualFurnishTestSetupService setupService;
    private final StepExecutionHelper stepExecutionHelper;
    private final EndpointHelper endpointHelper;
    private final VerificationHelper verificationHelper;
    private final ObjectMapper objectMapper;

    @Override
    public ManualFurnishTestResponse executeTest(ManualFurnishTestRequest request) {
        long startTime = System.currentTimeMillis();
        String scenarioName = request.getScenarioName();

        log.info("=== Starting Manual Furnish Test: {} ===", scenarioName);

        ManualFurnishTestResponse response = ManualFurnishTestResponse.builder()
                .scenarioName(scenarioName)
                .overallResult("PENDING")
                .build();

        try {
            // === STEP 1: Load Scenario ===
            TestContext context = stepExecutionHelper.executeStep(
                response,
                "Load Scenario",
                () -> loadScenario(scenarioName)
            );

            if (context == null) {
                response.setOverallResult("FAILED");
                response.setErrorMessage("Failed to load scenario");
                return response;
            }

            // === STEP 2: Setup Test Data ===
            stepExecutionHelper.executeStep(
                response,
                "Setup Test Data",
                () -> {
                    if (request.isSetupData()) {
                        setupTestData(context);
                    }
                    return null;
                }
            );

            // === STEP 3: Trigger Manual Furnish Endpoint ===
            stepExecutionHelper.executeStep(
                response,
                "Trigger Manual Furnish",
                () -> {
                    triggerManualFurnish(context);
                    return null;
                }
            );

            // === STEP 4: Verify Business Logic ===
            stepExecutionHelper.executeStep(
                response,
                "Verify Business Logic",
                () -> {
                    verifyBusinessLogic(context, response);
                    return null;
                }
            );

            // === STEP 5: Cleanup ===
            if (request.isCleanupAfterTest()) {
                stepExecutionHelper.executeStep(
                    response,
                    "Cleanup Test Data",
                    () -> {
                        setupService.cleanupTestData();
                        return null;
                    }
                );
            }

            response.setOverallResult("SUCCESS");
            response.setNoticeNo(context.getNoticeNo());

        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage(), e);
            response.setOverallResult("FAILED");
            response.setErrorMessage(e.getMessage());
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionTimeMs(executionTime);
            log.info("=== Test Completed: {} in {}ms ===", response.getOverallResult(), executionTime);
        }

        return response;
    }

    private TestContext loadScenario(String scenarioName) {
        try {
            log.info("Loading scenario: {}", scenarioName);
            ClassPathResource resource = new ClassPathResource("testing/furnish_manual/data/scenario.json");
            List<Map<String, Object>> scenarios = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, Object> scenarioData = scenarios.stream()
                    .filter(s -> scenarioName.equals(s.get("scenario")))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioName));

            TestContext context = new TestContext();
            context.setScenarioName(scenarioName);
            context.setScenarioData(scenarioData);
            context.setNoticeNo((String) scenarioData.get("noticeNo"));

            log.info("Loaded scenario: {} - {}", scenarioName, scenarioData.get("description"));
            return context;

        } catch (Exception e) {
            log.error("Failed to load scenario: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load scenario: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupTestData(TestContext context) {
        log.info("Setting up test data for scenario: {}", context.getScenarioName());

        Map<String, Object> scenarioData = context.getScenarioData();
        Map<String, Object> setupData = (Map<String, Object>) scenarioData.get("setupData");

        if (setupData == null || setupData.isEmpty()) {
            log.info("No setup data required for this scenario");
            return;
        }

        // Setup is handled by ManualFurnishTestSetupService
        // which creates test notices MF001-MF006
        Map<String, Object> result = setupService.setupTestData();
        log.info("Setup completed: {}", result);
    }

    @SuppressWarnings("unchecked")
    private void triggerManualFurnish(TestContext context) {
        log.info("Triggering manual furnish endpoint");

        Map<String, Object> scenarioData = context.getScenarioData();

        // Check if this is a bulk furnish scenario
        if (scenarioData.containsKey("furnishRequests")) {
            List<Map<String, Object>> furnishRequests =
                (List<Map<String, Object>>) scenarioData.get("furnishRequests");
            endpointHelper.triggerBulkFurnish(context, furnishRequests);
        } else {
            Map<String, Object> furnishRequest =
                (Map<String, Object>) scenarioData.get("furnishRequest");
            endpointHelper.triggerManualFurnish(context, furnishRequest);
        }
    }

    @SuppressWarnings("unchecked")
    private void verifyBusinessLogic(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying business logic");

        Map<String, Object> scenarioData = context.getScenarioData();
        Map<String, Object> expectedResult = (Map<String, Object>) scenarioData.get("expectedResult");

        if (expectedResult == null) {
            log.warn("No expected results defined for scenario: {}", context.getScenarioName());
            return;
        }

        // Verify owner/driver record created or updated
        if (Boolean.TRUE.equals(expectedResult.get("ownerDriverCreated"))) {
            verificationHelper.verifyOwnerDriverCreated(context, response);
        }

        if (Boolean.TRUE.equals(expectedResult.get("ownerDriverUpdated"))) {
            verificationHelper.verifyOwnerDriverUpdated(context, response);
        }

        // Verify current offender indicator
        if (expectedResult.containsKey("currentOffenderIndicator")) {
            String expectedIndicator = (String) expectedResult.get("currentOffenderIndicator");
            verificationHelper.verifyCurrentOffenderIndicator(context, response, expectedIndicator);
        }

        // Verify address created or updated
        if (Boolean.TRUE.equals(expectedResult.get("addressCreated"))) {
            verificationHelper.verifyAddressCreated(context, response);
        }

        if (Boolean.TRUE.equals(expectedResult.get("addressUpdated"))) {
            verificationHelper.verifyAddressUpdated(context, response);
        }

        // Verify bulk furnish results
        if (expectedResult.containsKey("recordsCreated")) {
            Integer expectedCount = (Integer) expectedResult.get("recordsCreated");
            verificationHelper.verifyBulkFurnishCount(context, response, expectedCount);
        }

        // Verify furnishable stage check
        if (Boolean.TRUE.equals(expectedResult.get("furnishableStage"))) {
            verificationHelper.verifyFurnishableStage(context, response);
        }

        log.info("Business logic verification completed");
    }
}
