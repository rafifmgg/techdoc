package com.ocmsintranet.apiservice.testing.furnish_approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestRequest;
import com.ocmsintranet.apiservice.testing.furnish_approval.dto.FurnishApprovalTestResponse;
import com.ocmsintranet.apiservice.testing.furnish_approval.helpers.EndpointHelper;
import com.ocmsintranet.apiservice.testing.furnish_approval.helpers.StepExecutionHelper;
import com.ocmsintranet.apiservice.testing.furnish_approval.helpers.TestContext;
import com.ocmsintranet.apiservice.testing.furnish_approval.helpers.VerificationHelper;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalRequest;
import com.ocmsintranet.apiservice.workflows.furnish.approval.dto.FurnishApprovalResult;
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
 * Implementation of Furnish Approval Integration Test Service
 * Follows 4-step testing pattern:
 * 1. Load test scenarios from scenario.json
 * 2. Trigger the actual furnish approval API
 * 3. Fetch verification data from database tables
 * 4. Verify business logic and data integrity
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishApprovalTestServiceImpl implements FurnishApprovalTestService {

    private final StepExecutionHelper stepExecutor;
    private final EndpointHelper endpointHelper;
    private final VerificationHelper verificationHelper;
    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FurnishApprovalTestResponse executeTest(FurnishApprovalTestRequest request) {
        log.info("Starting Furnish Approval Test - Scenario: {}", request.getScenarioName());

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

        // Step 3: Trigger furnish approval API
        stepExecutor.executeStep(context, "Trigger Furnish Approval API", () -> {
            triggerFurnishApproval(context);
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

        ClassPathResource resource = new ClassPathResource("testing/furnish_approval/data/scenario.json");
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
     * Step 2: Setup test data (create pending application, TS-PDP suspension, PS suspension if needed)
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
            app.setFurnishEmailAddr((String) setupData.get("furnishedEmail"));
            app.setFurnishTelCode("65");
            app.setFurnishTelNo((String) setupData.get("furnishedMobile"));
            app.setOwnerDriverIndicator("D");
            app.setOwnerIdNo("S1234567A");
            app.setOwnerName("Test Owner");
            app.setFurnishIdNo("S7654321B");
            app.setFurnishName("Test Furnished");
            app.setVehicleNo("TEST" + noticeNo);
            app.setCreDate(LocalDateTime.now());
            app.setCreUserId("TEST_SETUP");
            furnishApplicationRepository.save(app);
            log.info("Created pending application: {}", txnNo);
        }

        // Create TS-PDP suspension
        if (Boolean.TRUE.equals(setupData.get("createTsPdpSuspension"))) {
            SuspendedNotice tsPdp = new SuspendedNotice();
            tsPdp.setNoticeNo(noticeNo);
            tsPdp.setSuspensionType("TS");
            tsPdp.setReasonOfSuspension("PDP");
            tsPdp.setDateOfSuspension(LocalDateTime.now().minusDays(5));
            tsPdp.setSrNo(1);
            tsPdp.setSuspensionSource("TEST");
            tsPdp.setOfficerAuthorisingSupension("TEST_SETUP");
            tsPdp.setDateOfRevival(null);
            tsPdp.setCreDate(LocalDateTime.now());
            tsPdp.setCreUserId("TEST_SETUP");
            suspendedNoticeRepository.save(tsPdp);
            log.info("Created TS-PDP suspension for notice: {}", noticeNo);
        }

        // Create PS suspension (to block approval)
        if (Boolean.TRUE.equals(setupData.get("createPsSuspension"))) {
            SuspendedNotice ps = new SuspendedNotice();
            ps.setNoticeNo(noticeNo);
            ps.setSuspensionType("PS");
            ps.setReasonOfSuspension("PUR");
            ps.setDateOfSuspension(LocalDateTime.now().minusDays(5));
            ps.setSrNo(2);
            ps.setSuspensionSource("TEST");
            ps.setOfficerAuthorisingSupension("TEST_SETUP");
            ps.setDateOfRevival(null);
            ps.setCreDate(LocalDateTime.now());
            ps.setCreUserId("TEST_SETUP");
            suspendedNoticeRepository.save(ps);
            log.info("Created PS suspension for notice: {}", noticeNo);
        }
    }

    /**
     * Step 3: Trigger furnish approval API
     */
    private void triggerFurnishApproval(TestContext context) {
        log.info("Triggering furnish approval API for txnNo: {}", context.getTxnNo());

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioData = context.getScenarioData();
        @SuppressWarnings("unchecked")
        Map<String, Object> approvalRequestData = (Map<String, Object>) scenarioData.get("approvalRequest");

        FurnishApprovalRequest approvalRequest = buildApprovalRequest(approvalRequestData);

        try {
            FurnishApprovalResult result = endpointHelper.callApprovalEndpoint(approvalRequest);
            context.getRuntimeData().put("approvalResult", result);
            log.info("Furnish approval completed - result type: {}", result.getClass().getSimpleName());
        } catch (Exception e) {
            // For scenarios where approval should be blocked, this is expected
            log.info("Approval attempt failed (may be expected): {}", e.getMessage());
            context.getRuntimeData().put("approvalError", e.getMessage());
        }
    }

    private FurnishApprovalRequest buildApprovalRequest(Map<String, Object> requestData) {
        FurnishApprovalRequest request = new FurnishApprovalRequest();
        request.setTxnNo((String) requestData.get("txnNo"));
        request.setOfficerId((String) requestData.get("officerId"));
        request.setSendEmailToOwner((Boolean) requestData.get("sendEmailToOwner"));
        request.setSendEmailToFurnished((Boolean) requestData.get("sendEmailToFurnished"));
        request.setSendSmsToFurnished((Boolean) requestData.get("sendSmsToFurnished"));
        request.setCustomEmailSubject((String) requestData.get("customEmailSubject"));
        request.setCustomEmailBody((String) requestData.get("customEmailBody"));
        request.setCustomSmsBody((String) requestData.get("customSmsBody"));
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

        String txnNo = context.getTxnNo();
        String noticeNo = context.getNoticeNo();

        // Check if approval was blocked
        if (Boolean.TRUE.equals(expectedResult.get("approvalBlocked"))) {
            verificationHelper.verifyApprovalBlockedByPs(context, txnNo);
            return; // No further verifications needed
        }

        // Verify application approved
        verificationHelper.verifyApplicationApproved(context, txnNo);

        // Verify email notifications
        if (Boolean.TRUE.equals(expectedResult.get("emailToOwnerSent"))) {
            String ownerEmail = (String) setupData.get("ownerEmail");
            verificationHelper.verifyEmailToOwner(context, noticeNo, ownerEmail);
        }

        if (Boolean.TRUE.equals(expectedResult.get("emailToFurnishedSent"))) {
            String furnishedEmail = (String) setupData.get("furnishedEmail");
            verificationHelper.verifyEmailToFurnished(context, noticeNo, furnishedEmail);
        }

        // Verify SMS notification
        if (Boolean.TRUE.equals(expectedResult.get("smsToFurnishedSent"))) {
            String furnishedMobile = (String) setupData.get("furnishedMobile");
            verificationHelper.verifySmsToFurnished(context, noticeNo, furnishedMobile);
        }

        // Verify TS-PDP suspension revival
        if (Boolean.TRUE.equals(expectedResult.get("tsPdpSuspensionRevived"))) {
            verificationHelper.verifyTsPdpSuspensionRevived(context, noticeNo);
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
