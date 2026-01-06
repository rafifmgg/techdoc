package com.ocmsintranet.apiservice.testing.furnish_submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_submission.dto.FurnishSubmissionTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_submission.helpers.EndpointHelper;
import com.ocmsintranet.apiservice.testing.furnish_submission.helpers.StepExecutionHelper;
import com.ocmsintranet.apiservice.testing.furnish_submission.helpers.TestContext;
import com.ocmsintranet.apiservice.testing.furnish_submission.helpers.VerificationHelper;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of Furnish Submission Integration Test Service
 * Follows 4-step testing pattern:
 * 1. Load test scenarios from scenario.json
 * 2. Trigger the actual furnish submission API
 * 3. Fetch verification data from database tables
 * 4. Verify business logic and data integrity
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishSubmissionTestServiceImpl implements FurnishSubmissionTestService {

    private final StepExecutionHelper stepExecutor;
    private final EndpointHelper endpointHelper;
    private final VerificationHelper verificationHelper;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FurnishSubmissionTestResponse executeTest(FurnishSubmissionTestRequest request) {
        log.info("Starting Furnish Submission Test - Scenario: {}", request.getScenarioName());

        // Initialize test context
        TestContext context = new TestContext();
        context.setScenarioName(request.getScenarioName());
        context.setStartTimeMs(System.currentTimeMillis());

        // Step 1: Load test scenario
        stepExecutor.executeStep(context, "Load Test Scenario", () -> {
            loadTestScenario(context, request.getScenarioName());
        });

        // Step 2: Setup test data (if needed)
        if (request.isSetupData()) {
            stepExecutor.executeStep(context, "Setup Test Data", () -> {
                setupTestData(context);
            });
        }

        // Step 3: Trigger furnish submission API
        stepExecutor.executeStep(context, "Trigger Furnish Submission API", () -> {
            triggerFurnishSubmission(context);
        });

        // Step 4: Fetch verification data
        stepExecutor.executeStep(context, "Fetch Verification Data", () -> {
            fetchVerificationData(context);
        });

        // Step 5: Verify business logic
        stepExecutor.executeVerification(context, "Verify Business Logic", () -> {
            verifyBusinessLogic(context);
        });

        // Step 6: Cleanup (if requested)
        if (request.isCleanupAfterTest()) {
            stepExecutor.executeStep(context, "Cleanup Test Data", () -> {
                cleanupTestData(context);
            });
        }

        // Build and return response
        return context.buildResponse();
    }

    /**
     * Step 1: Load test scenario from scenario.json
     */
    private void loadTestScenario(TestContext context, String scenarioName) throws IOException {
        log.info("Loading test scenario: {}", scenarioName);

        ClassPathResource resource = new ClassPathResource("testing/furnish_submission/data/scenario.json");
        JsonNode scenarios = objectMapper.readTree(resource.getInputStream());

        Optional<JsonNode> scenarioOpt = findScenario(scenarios, scenarioName);
        if (scenarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioName);
        }

        JsonNode scenario = scenarioOpt.get();

        // Load scenario data into context
        Map<String, Object> scenarioData = objectMapper.convertValue(scenario, Map.class);
        context.setScenarioData(scenarioData);

        // Extract notice number
        String noticeNo = scenario.get("noticeNo").asText();
        context.setNoticeNo(noticeNo);

        log.info("Scenario loaded: {} - noticeNo: {}", scenarioName, noticeNo);
    }

    /**
     * Find scenario by name in scenarios array
     */
    private Optional<JsonNode> findScenario(JsonNode scenarios, String scenarioName) {
        if (!scenarios.isArray()) {
            return Optional.empty();
        }

        for (JsonNode scenario : scenarios) {
            if (scenario.has("scenario") && scenarioName.equals(scenario.get("scenario").asText())) {
                return Optional.of(scenario);
            }
        }
        return Optional.empty();
    }

    /**
     * Step 2: Setup test data (create test notice, setup suspensions if needed)
     */
    private void setupTestData(TestContext context) {
        log.info("Setting up test data for notice: {}", context.getNoticeNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> fullTestData = (Map<String, Object>) scenarioData.get("fullTestData");

        String noticeNo = context.getNoticeNo();
        String vehicleNo = (String) scenarioData.get("vehicleNo");

        // Create or update valid offence notice
        OcmsValidOffenceNotice notice = validOffenceNoticeRepository.findById(noticeNo)
                .orElse(new OcmsValidOffenceNotice());

        notice.setNoticeNo(noticeNo);
        notice.setVehicleNo(vehicleNo);
        notice.setCurrentProcessingStage("PRE"); // Pre-payment stage (furnishable)
        notice.setNoticeDateAndTime(LocalDateTime.now().minusDays(5));
        notice.setCreDate(LocalDateTime.now());

        validOffenceNoticeRepository.save(notice);
        log.info("Created test notice: {}", noticeNo);

        // Setup PS suspension if required
        if (Boolean.TRUE.equals(scenarioData.get("setupPsSuspension"))) {
            setupPsSuspension(context, noticeNo);
        }

        // Setup invalid stage if required
        if (Boolean.TRUE.equals(scenarioData.get("setupInvalidStage"))) {
            notice.setCurrentProcessingStage("CPC"); // Closed stage (not furnishable)
            validOffenceNoticeRepository.save(notice);
            log.info("Set notice to non-furnishable stage: CPC");
        }
    }

    /**
     * Setup PS suspension for testing
     */
    private void setupPsSuspension(TestContext context, String noticeNo) {
        log.info("Setting up PS suspension for notice: {}", noticeNo);

        SuspendedNotice psSuspension = new SuspendedNotice();
        psSuspension.setNoticeNo(noticeNo);
        psSuspension.setSuspensionType("PS");
        psSuspension.setReasonOfSuspension("Payment Under Review");
        psSuspension.setDateOfSuspension(LocalDateTime.now().minusDays(5));
        psSuspension.setSrNo(1);
        psSuspension.setSuspensionSource("TEST");
        psSuspension.setOfficerAuthorisingSupension("TEST_SETUP");
        psSuspension.setDateOfRevival(null); // Still suspended
        psSuspension.setCreDate(LocalDateTime.now());
        psSuspension.setCreUserId("TEST_SETUP");

        suspendedNoticeRepository.save(psSuspension);
        log.info("Created PS suspension for notice: {}", noticeNo);
    }

    /**
     * Step 3: Trigger furnish submission API
     */
    private void triggerFurnishSubmission(TestContext context) {
        log.info("Triggering furnish submission API for notice: {}", context.getNoticeNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> fullTestData = (Map<String, Object>) scenarioData.get("fullTestData");

        // Build furnish submission request from test data
        FurnishSubmissionRequest submissionRequest = buildSubmissionRequest(fullTestData);

        // Call the actual furnish submission endpoint
        FurnishSubmissionResult result = endpointHelper.callSubmitEndpoint(submissionRequest);

        // Store result in runtime data
        context.getRuntimeData().put("submissionResult", result);

        // Extract txnNo from result if it's a Success
        if (result instanceof FurnishSubmissionResult.Success success) {
            context.setTxnNo(success.furnishApplication().getTxnNo());
            log.info("Furnish submission succeeded - txnNo: {}", success.furnishApplication().getTxnNo());
        } else {
            log.info("Furnish submission completed - result type: {}", result.getClass().getSimpleName());
        }
    }

    /**
     * Build furnish submission request from test data
     */
    private FurnishSubmissionRequest buildSubmissionRequest(Map<String, Object> testData) {
        FurnishSubmissionRequest request = new FurnishSubmissionRequest();

        request.setNoticeNo((String) testData.get("noticeNo"));
        request.setVehicleNo((String) testData.get("vehicleNo"));
        request.setOwnerIdNo((String) testData.get("ownerIdNo"));
        request.setOwnerName((String) testData.get("ownerName"));
        request.setOwnerEmailAddr((String) testData.get("ownerEmailAddr"));
        request.setOwnerTelCode((String) testData.get("ownerTelCode"));
        request.setOwnerTelNo((String) testData.get("ownerTelNo"));

        request.setOwnerDriverIndicator((String) testData.get("furnishType"));
        request.setFurnishIdNo((String) testData.get("furnishIdNo"));
        request.setFurnishName((String) testData.get("furnishName"));
        request.setFurnishEmailAddr((String) testData.get("furnishEmailAddr"));
        request.setFurnishTelCode((String) testData.get("furnishTelCode"));
        request.setFurnishTelNo((String) testData.get("furnishTelNo"));
        request.setFurnishMailBlkNo((String) testData.get("furnishAddrLine1"));
        request.setFurnishMailStreetName((String) testData.get("furnishAddrLine2"));
        request.setFurnishMailPostalCode((String) testData.get("furnishPostalCode"));

        // Documents (if any) - use documentReferences instead of DocumentUpload inner class
        @SuppressWarnings("unchecked")
        List<Map<String, String>> documents = (List<Map<String, String>>) testData.get("documents");
        if (documents != null && !documents.isEmpty()) {
            List<String> docRefs = new ArrayList<>();
            for (Map<String, String> doc : documents) {
                docRefs.add(doc.get("blobUrl"));
            }
            request.setDocumentReferences(docRefs);
        }

        return request;
    }

    /**
     * Step 4: Fetch verification data from database
     */
    private void fetchVerificationData(TestContext context) {
        log.info("Fetching verification data for txnNo: {}, noticeNo: {}",
                context.getTxnNo(), context.getNoticeNo());

        // Data fetching is done in verification step via VerificationHelper
        // This step is a placeholder for consistency with testing framework
    }

    /**
     * Step 5: Verify business logic
     */
    private void verifyBusinessLogic(TestContext context) {
        log.info("Verifying business logic for scenario: {}", context.getScenarioName());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> expectedResult = (Map<String, Object>) scenarioData.get("expectedResult");

        String txnNo = context.getTxnNo();
        String noticeNo = context.getNoticeNo();

        // Verify furnish application record
        String expectedStatus = (String) expectedResult.get("applicationStatus");
        verificationHelper.verifyFurnishApplication(context, txnNo, expectedStatus);

        // Verify documents
        Integer expectedDocCount = (Integer) expectedResult.get("documentCount");
        if (expectedDocCount != null) {
            verificationHelper.verifyFurnishDocuments(context, txnNo, expectedDocCount);
        }

        // For auto-approved cases, verify additional records
        if (Boolean.TRUE.equals(expectedResult.get("autoApproved"))) {
            // Verify owner/driver record
            String expectedOffenderType = (String) expectedResult.get("ownerDriverType");
            verificationHelper.verifyOwnerDriverRecord(context, noticeNo, expectedOffenderType);

            // Get owner/driver indicator from runtime data
            @SuppressWarnings("unchecked")
            Map<String, Object> runtimeData = context.getRuntimeData();
            if (runtimeData.containsKey("ownerDriverRecord")) {
                com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver record =
                        (com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver)
                                runtimeData.get("ownerDriverRecord");
                verificationHelper.verifyOwnerDriverAddress(context, noticeNo, record.getOwnerDriverIndicator());
            }

            // Verify TS-PDP suspension
            if (Boolean.TRUE.equals(expectedResult.get("tsPdpSuspension"))) {
                verificationHelper.verifyTsPdpSuspension(context, noticeNo);
            }
        } else {
            // For manual review cases, verify NO TS-PDP suspension
            verificationHelper.verifyNoTsPdpSuspension(context, noticeNo);
        }
    }

    /**
     * Step 6: Cleanup test data
     */
    private void cleanupTestData(TestContext context) {
        log.info("Cleaning up test data for notice: {}", context.getNoticeNo());
        // Cleanup logic here (delete test records)
        // Note: Usually we keep test data for manual verification
    }
}
