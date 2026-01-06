package com.ocmsintranet.apiservice.testing.furnish_rejection.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.testing.furnish_rejection.dto.FurnishRejectionTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_rejection.dto.FurnishRejectionTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_rejection.helpers.EndpointHelper;
import com.ocmsintranet.apiservice.testing.furnish_rejection.helpers.StepExecutionHelper;
import com.ocmsintranet.apiservice.testing.furnish_rejection.helpers.TestContext;
import com.ocmsintranet.apiservice.testing.furnish_rejection.helpers.VerificationHelper;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionRequest;
import com.ocmsintranet.apiservice.workflows.furnish.rejection.dto.FurnishRejectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of Furnish Rejection Integration Test Service
 * Follows 4-step testing pattern
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishRejectionTestServiceImpl implements FurnishRejectionTestService {

    private final StepExecutionHelper stepExecutor;
    private final EndpointHelper endpointHelper;
    private final VerificationHelper verificationHelper;
    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FurnishRejectionTestResponse executeTest(FurnishRejectionTestRequest request) {
        log.info("Starting Furnish Rejection Test - Scenario: {}", request.getScenarioName());

        TestContext context = new TestContext();
        context.setScenarioName(request.getScenarioName());
        context.setStartTimeMs(System.currentTimeMillis());

        // Step 1: Load test scenario
        stepExecutor.executeStep(context, "Load Test Scenario", () -> {
            loadTestScenario(context, request.getScenarioName());
        });

        // Step 2: Setup test data
        if (request.isSetupData()) {
            stepExecutor.executeStep(context, "Setup Test Data", () -> {
                setupTestData(context);
            });
        }

        // Step 3: Trigger furnish rejection API
        stepExecutor.executeStep(context, "Trigger Furnish Rejection API", () -> {
            triggerFurnishRejection(context);
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

        return context.buildResponse();
    }

    /**
     * Step 1: Load test scenario from scenario.json
     */
    private void loadTestScenario(TestContext context, String scenarioName) throws IOException {
        log.info("Loading test scenario: {}", scenarioName);

        ClassPathResource resource = new ClassPathResource("testing/furnish_rejection/data/scenario.json");
        JsonNode scenarios = objectMapper.readTree(resource.getInputStream());

        Optional<JsonNode> scenarioOpt = findScenario(scenarios, scenarioName);
        if (scenarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioName);
        }

        JsonNode scenario = scenarioOpt.get();
        Map<String, Object> scenarioData = objectMapper.convertValue(scenario, Map.class);
        context.setScenarioData(scenarioData);

        String noticeNo = scenario.get("noticeNo").asText();
        String txnNo = scenario.get("txnNo").asText();
        context.setNoticeNo(noticeNo);
        context.setTxnNo(txnNo);

        log.info("Scenario loaded: {} - noticeNo: {}, txnNo: {}", scenarioName, noticeNo, txnNo);
    }

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
     * Step 2: Setup test data (create pending application)
     */
    private void setupTestData(TestContext context) {
        log.info("Setting up test data for txnNo: {}, noticeNo: {}", context.getTxnNo(), context.getNoticeNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> setupData = (Map<String, Object>) scenarioData.get("setupData");

        String noticeNo = context.getNoticeNo();
        String txnNo = context.getTxnNo();

        // Create valid offence notice
        OcmsValidOffenceNotice notice = validOffenceNoticeRepository.findById(noticeNo)
                .orElse(new OcmsValidOffenceNotice());
        notice.setNoticeNo(noticeNo);
        notice.setVehicleNo("TEST" + noticeNo);
        notice.setCurrentProcessingStage("PRE");
        notice.setNoticeDateAndTime(LocalDateTime.now().minusDays(10));
        notice.setOffenceTime(LocalDateTime.now().minusDays(10));
        notice.setCreDate(LocalDateTime.now());
        validOffenceNoticeRepository.save(notice);

        // Create pending furnish application
        if (Boolean.TRUE.equals(setupData.get("createPendingApplication"))) {
            OcmsFurnishApplication app = new OcmsFurnishApplication();
            app.setTxnNo(txnNo);
            app.setNoticeNo(noticeNo);
            app.setApplicationStatus((String) setupData.get("applicationStatus"));
            app.setOwnerEmailAddr((String) setupData.get("ownerEmail"));
            app.setFurnishEmailAddr("furnished@test.com");
            app.setFurnishTelCode("65");
            app.setFurnishTelNo("91234567");
            app.setOwnerDriverIndicator("H");
            app.setOwnerIdNo("202345678L");
            app.setOwnerName("Test Owner");
            app.setFurnishIdNo("203456789N");
            app.setFurnishName("Test Furnished");
            app.setVehicleNo("TEST" + noticeNo);
            app.setCreDate(LocalDateTime.now());
            app.setCreUserId("TEST_SETUP");
            furnishApplicationRepository.save(app);
            log.info("Created pending application: {}", txnNo);
        }
    }

    /**
     * Step 3: Trigger furnish rejection API
     */
    private void triggerFurnishRejection(TestContext context) {
        log.info("Triggering furnish rejection API for txnNo: {}", context.getTxnNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> rejectionRequestData = (Map<String, Object>) scenarioData.get("rejectionRequest");

        FurnishRejectionRequest rejectionRequest = buildRejectionRequest(rejectionRequestData);

        try {
            FurnishRejectionResult result = endpointHelper.callRejectionEndpoint(rejectionRequest);
            context.getRuntimeData().put("rejectionResult", result);
            log.info("Furnish rejection completed - result type: {}", result.getClass().getSimpleName());
        } catch (Exception e) {
            log.info("Rejection attempt failed (may be expected): {}", e.getMessage());
            context.getRuntimeData().put("rejectionError", e.getMessage());
        }
    }

    private FurnishRejectionRequest buildRejectionRequest(Map<String, Object> requestData) {
        FurnishRejectionRequest request = new FurnishRejectionRequest();
        request.setTxnNo((String) requestData.get("txnNo"));
        request.setOfficerId((String) requestData.get("officerId"));
        request.setEmailTemplateId((String) requestData.get("emailTemplateId"));
        request.setSendEmailToOwner((Boolean) requestData.get("sendEmailToOwner"));
        request.setCustomEmailSubject((String) requestData.get("customEmailSubject"));
        request.setCustomEmailBody((String) requestData.get("customEmailBody"));
        request.setRemarks((String) requestData.get("remarks"));
        return request;
    }

    /**
     * Step 4: Fetch verification data from database
     */
    private void fetchVerificationData(TestContext context) {
        log.info("Fetching verification data for txnNo: {}", context.getTxnNo());
        // Data fetching is done in verification step via VerificationHelper
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
        @SuppressWarnings("unchecked")
        Map<String, Object> setupData = (Map<String, Object>) scenarioData.get("setupData");
        @SuppressWarnings("unchecked")
        Map<String, Object> rejectionRequest = (Map<String, Object>) scenarioData.get("rejectionRequest");

        String txnNo = context.getTxnNo();
        String noticeNo = context.getNoticeNo();

        // Verify application rejected
        verificationHelper.verifyApplicationRejected(context, txnNo);

        // Verify email notifications
        if (Boolean.TRUE.equals(expectedResult.get("emailToOwnerSent"))) {
            String ownerEmail = (String) setupData.get("ownerEmail");
            verificationHelper.verifyEmailToOwner(context, noticeNo, ownerEmail);
        } else {
            verificationHelper.verifyNoEmailSent(context, noticeNo);
        }

        // Verify rejection reason
        String remarks = (String) rejectionRequest.get("remarks");
        if (remarks != null) {
            verificationHelper.verifyRejectionReason(context, txnNo, remarks);
        }
    }

    /**
     * Step 6: Cleanup test data
     */
    private void cleanupTestData(TestContext context) {
        log.info("Cleaning up test data for txnNo: {}", context.getTxnNo());
        // Cleanup logic here
    }
}
