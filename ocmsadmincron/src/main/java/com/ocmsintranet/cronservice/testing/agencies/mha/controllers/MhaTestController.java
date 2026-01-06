package com.ocmsintranet.cronservice.testing.agencies.mha.controllers;

import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaTestService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaCallbackService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaFileContentReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;

/**
 * REST controller for MHA test endpoints.
 * Provides API endpoints to run MHA NRIC upload tests.
 */
@RestController
@RequestMapping("/test/mha")
public class MhaTestController {

    private final MhaTestService mhaTestService;
    private final MhaCallbackService mhaCallbackService;
    private final MhaFileContentReaderService mhaFileContentReaderService;
    private final RestTemplate restTemplate;

    @Value("${mha.test.enabled:true}")
    private boolean testEndpointsEnabled;

    @Value("${api.version}")
    private String apiVersion;

    @Value("${ocms.APIM.baseurl:http://localhost:8083}")
    private String apiBaseUrl;

    /**
     * Constructor for MhaTestController
     *
     * @param mhaTestService MhaTestService for test operations
     * @param mhaCallbackService MhaCallbackService for callback operations
     * @param mhaFileContentReaderService MhaFileContentReaderService for reading file content
     * @param restTemplate RestTemplate for internal API calls
     */
    @Autowired
    public MhaTestController(MhaTestService mhaTestService, MhaCallbackService mhaCallbackService, 
                           MhaFileContentReaderService mhaFileContentReaderService, RestTemplate restTemplate) {
        this.mhaTestService = mhaTestService;
        this.mhaCallbackService = mhaCallbackService;
        this.mhaFileContentReaderService = mhaFileContentReaderService;
        this.restTemplate = restTemplate;
    }

    /**
     * Process result test flow.
     * This endpoint triggers the process result test flow which lists files in
     * SFTP container, downloads the latest file, and uploads it to the output
     * directory with a new name.
     *
     * @return List of test step results
     */
    @PostMapping("/mha-callback")
    public ResponseEntity<?> processCallbackTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = mhaCallbackService.processCallbackTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Read content from the latest MHA input file.
     * This endpoint finds and displays the content of the latest input file
     * from the SFTP upload directory.
     *
     * @return List of test step results containing file content
     */
    @PostMapping("/mha-input-file-content")
    public ResponseEntity<?> readFileContent() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = mhaFileContentReaderService.readLatestFileContent();
        return ResponseEntity.ok(results);
    }

    /**
     * Run full MHA NRIC End-to-End flow.
     * Steps: DB prepare -> Upload -> Callback (generate NRO2URA files) -> Download/Execute -> Post-download DB snapshot
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return Aggregated list of step results
     */
    @PostMapping("/e2e-nric")
    public ResponseEntity<?> runE2eNricFlow(@RequestParam(value = "noticePrefix", required = false) String noticePrefix) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }

        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        // Defaults
        final String scenarioName = "Successful Upload with SLIFT Encryption";
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now());
            noticePrefix = "MHATEST" + ts;
        }

        try {
            // Step 1: Prepare DB test data
            TestStepResult step1 = mhaTestService.createTestRecords(scenarioName, noticePrefix);
            results.add(step1);
            continueExecution = "SUCCESS".equals(step1.getStatus());

            // Step 2: Pre-upload DB snapshot (joined tables)
            TestStepResult step2;
            if (continueExecution) {
                step2 = mhaTestService.displayJoinedTableData(noticePrefix);
            } else {
                step2 = new TestStepResult("Step 2: Pre-upload DB snapshot (joined tables)", "SKIPPED");
                step2.addDetail("⚠️ Skipped due to previous step failure");
            }
            results.add(step2);
            continueExecution = continueExecution && !"FAILED".equals(step2.getStatus());

            // Step 3: Trigger Upload API
            TestStepResult step3;
            if (continueExecution) {
                step3 = mhaTestService.triggerMhaNricUpload(scenarioName, noticePrefix);
            } else {
                step3 = new TestStepResult("Step 3: Triggering MHA NRIC upload workflow via API...", "SKIPPED");
                step3.addDetail("⚠️ Skipped due to previous step failure");
            }
            results.add(step3);
            continueExecution = continueExecution && !"FAILED".equals(step3.getStatus());

            // Step 4: Simulate Callback (generate NRO2URA .TOT/.EXP)
            if (continueExecution) {
                TestStepResult step4Info = new TestStepResult("Step 4: Simulating MHA callback to generate output files", "INFO");
                step4Info.addDetail("Delegating to callback test sub-steps below");
                results.add(step4Info);
                List<TestStepResult> cbResults = mhaCallbackService.processCallbackTest();
                results.addAll(cbResults);

                // If any callback sub-step failed, mark continuation false
                boolean callbackFailed = cbResults.stream().anyMatch(s -> "FAILED".equals(s.getStatus()));
                if (callbackFailed) {
                    continueExecution = false;
                }
            } else {
                TestStepResult skipped = new TestStepResult("Step 4: Simulating MHA callback to generate output files", "SKIPPED");
                skipped.addDetail("⚠️ Skipped due to previous step failure");
                results.add(skipped);
            }

            // Step 5: Trigger Download/Execute
            TestStepResult step5;
            if (continueExecution) {
                step5 = new TestStepResult("Step 5: Triggering MHA NRIC download/execute", "RUNNING");
                String baseUrl = mhaTestService.getCronBaseUrl();
                String url = baseUrl + "/" + apiVersion + "/mha/download/execute";
                step5.addDetail("📡 Calling API: " + url);
                try {
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("{}", headers);
                    org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                        url,
                        org.springframework.http.HttpMethod.POST,
                        entity,
                        String.class
                    );
                    String body = resp.getBody();
                    step5.setStatus("SUCCESS");
                    step5.addDetail("📥 API Response: " + body);
                    step5.setJsonData(body);
                } catch (Exception e) {
                    step5.setStatus("FAILED");
                    step5.addDetail("❌ Error calling download/execute: " + e.getMessage());
                }
            } else {
                step5 = new TestStepResult("Step 5: Triggering MHA NRIC download/execute", "SKIPPED");
                step5.addDetail("⚠️ Skipped due to previous step failure");
            }
            results.add(step5);
            continueExecution = continueExecution && "SUCCESS".equals(step5.getStatus());

            // Step 6: Post-download DB snapshot (joined tables)
            TestStepResult step6;
            if (continueExecution) {
                step6 = mhaTestService.displayJoinedTableData(noticePrefix);
                step6.setStepName("Step 6: Post-download DB snapshot (joined tables)");
            } else {
                step6 = new TestStepResult("Step 6: Post-download DB snapshot (joined tables)", "SKIPPED");
                step6.addDetail("⚠️ Skipped due to previous step failure");
            }
            results.add(step6);

            // Summary
            boolean allSuccess = results.stream()
                .filter(s -> !"SKIPPED".equals(s.getStatus()))
                .allMatch(s -> "SUCCESS".equals(s.getStatus()) || "INFO".equals(s.getStatus()));

            TestStepResult summary = new TestStepResult("Test Flow Summary", allSuccess ? "SUCCESS" : "PARTIAL");
            long successCount = results.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
            long failedCount = results.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
            long skippedCount = results.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count();
            summary.addDetail("📊 Test Flow Statistics:");
            summary.addDetail("✅ Successful steps: " + successCount);
            summary.addDetail("❌ Failed steps: " + failedCount);
            summary.addDetail("⚠️ Skipped steps: " + skippedCount);
            summary.addDetail("Total steps: " + (successCount + failedCount + skippedCount));
            results.add(summary);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            TestStepResult error = new TestStepResult("Unexpected error", "FAILED");
            error.addDetail("❌ Unhandled exception: " + e.getMessage());
            results.add(error);
            return ResponseEntity.status(500).body(results);
        }
    }
}
