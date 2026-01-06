package com.ocmsintranet.cronservice.testing.agencies.mha.controllers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaNricUploadFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaNricDownloadFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaNricUploadScenarioService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaNricNormalSuccessFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaTsNroSuspensionFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaPsRipSuspensionFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaPsRp2SuspensionFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaInvalidAddressTagFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaStreetAddressValidationFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaPostalCodeValidationFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaFutureDateAddressChangeFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaMultipleOwnerDriverFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaEmbassyDiplomatFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaExceptionReportFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaDatahiveNricFoundFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaDatahiveNricNotFoundFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaMixedPsBatchFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaLifeStatusValidationFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaStageProgressionFlowService;
import com.ocmsintranet.cronservice.testing.agencies.mha.services.MhaMultipleNoticeTypesFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for MHA NRIC test scenarios.
 * Provides API endpoints to run MHA NRIC upload and download test flows.
 */
@Slf4j
@RestController
@RequestMapping("/test/mha")
public class MhaScenarioController {

    private final MhaNricUploadFlowService mhaNricUploadFlowService;
    private final MhaNricDownloadFlowService mhaNricDownloadFlowService;
    private final MhaNricUploadScenarioService mhaNricUploadScenarioService;
    private final MhaNricNormalSuccessFlowService mhaNricNormalSuccessFlowService;
    private final MhaTsNroSuspensionFlowService mhaTsNroSuspensionFlowService;
    private final MhaPsRipSuspensionFlowService mhaPsRipSuspensionFlowService;
    private final MhaPsRp2SuspensionFlowService mhaPsRp2SuspensionFlowService;
    private final MhaInvalidAddressTagFlowService mhaInvalidAddressTagFlowService;
    private final MhaStreetAddressValidationFlowService mhaStreetAddressValidationFlowService;
    private final MhaPostalCodeValidationFlowService mhaPostalCodeValidationFlowService;
    private final MhaFutureDateAddressChangeFlowService mhaFutureDateAddressChangeFlowService;
    private final MhaMultipleOwnerDriverFlowService mhaMultipleOwnerDriverFlowService;
    private final MhaEmbassyDiplomatFlowService mhaEmbassyDiplomatFlowService;
    private final MhaExceptionReportFlowService mhaExceptionReportFlowService;
    private final MhaDatahiveNricFoundFlowService mhaDatahiveNricFoundFlowService;
    private final MhaDatahiveNricNotFoundFlowService mhaDatahiveNricNotFoundFlowService;
    private final MhaMixedPsBatchFlowService mhaMixedPsBatchFlowService;
    private final MhaLifeStatusValidationFlowService mhaLifeStatusValidationFlowService;
    private final MhaStageProgressionFlowService mhaStageProgressionFlowService;
    private final MhaMultipleNoticeTypesFlowService mhaMultipleNoticeTypesFlowService;

    @Value("${mha.test.enabled:true}")
    private boolean testEndpointsEnabled;

    /**
     * Constructor for MhaScenarioController
     *
     * @param mhaNricUploadFlowService MhaNricUploadFlowService for upload test flow
     * @param mhaNricDownloadFlowService MhaNricDownloadFlowService for download test flow
     * @param mhaNricUploadScenarioService MhaNricUploadScenarioService for upload scenario tests
     * @param mhaNricNormalSuccessFlowService MhaNricNormalSuccessFlowService for end-to-end normal success flow tests
     * @param mhaTsNroSuspensionFlowService MhaTsNroSuspensionFlowService for TS-NRO suspension flow tests
     * @param mhaPsRipSuspensionFlowService MhaPsRipSuspensionFlowService for PS-RIP suspension flow tests
     * @param mhaPsRp2SuspensionFlowService MhaPsRp2SuspensionFlowService for PS-RP2 suspension flow tests
     * @param mhaInvalidAddressTagFlowService MhaInvalidAddressTagFlowService for invalid address tag validation flow tests
     * @param mhaStreetAddressValidationFlowService MhaStreetAddressValidationFlowService for street address specific validation flow tests
     * @param mhaPostalCodeValidationFlowService MhaPostalCodeValidationFlowService for postal code specific validation flow tests
     * @param mhaFutureDateAddressChangeFlowService MhaFutureDateAddressChangeFlowService for future date address change flow tests
     * @param mhaMultipleOwnerDriverFlowService MhaMultipleOwnerDriverFlowService for multiple owner/driver processing tests
     * @param mhaEmbassyDiplomatFlowService MhaEmbassyDiplomatFlowService for embassy/diplomat processing tests
     * @param mhaExceptionReportFlowService MhaExceptionReportFlowService for exception report processing tests
     * @param mhaDatahiveNricFoundFlowService MhaDatahiveNricFoundFlowService for DataHive NRIC found tests
     * @param mhaDatahiveNricNotFoundFlowService MhaDatahiveNricNotFoundFlowService for DataHive NRIC not found tests
     * @param mhaMixedPsBatchFlowService MhaMixedPsBatchFlowService for mixed PS batch processing tests
     * @param mhaLifeStatusValidationFlowService MhaLifeStatusValidationFlowService for life status validation tests
     * @param mhaStageProgressionFlowService MhaStageProgressionFlowService for stage progression tests
     * @param mhaMultipleNoticeTypesFlowService MhaMultipleNoticeTypesFlowService for multiple notice types tests
     */
    @Autowired
    public MhaScenarioController(MhaNricUploadFlowService mhaNricUploadFlowService,
                                MhaNricDownloadFlowService mhaNricDownloadFlowService,
                                MhaNricUploadScenarioService mhaNricUploadScenarioService,
                                MhaNricNormalSuccessFlowService mhaNricNormalSuccessFlowService,
                                MhaTsNroSuspensionFlowService mhaTsNroSuspensionFlowService,
                                MhaPsRipSuspensionFlowService mhaPsRipSuspensionFlowService,
                                MhaPsRp2SuspensionFlowService mhaPsRp2SuspensionFlowService,
                                MhaInvalidAddressTagFlowService mhaInvalidAddressTagFlowService,
                                MhaStreetAddressValidationFlowService mhaStreetAddressValidationFlowService,
                                MhaPostalCodeValidationFlowService mhaPostalCodeValidationFlowService,
                                MhaFutureDateAddressChangeFlowService mhaFutureDateAddressChangeFlowService,
                                MhaMultipleOwnerDriverFlowService mhaMultipleOwnerDriverFlowService,
                                MhaEmbassyDiplomatFlowService mhaEmbassyDiplomatFlowService,
                                MhaExceptionReportFlowService mhaExceptionReportFlowService,
                                MhaDatahiveNricFoundFlowService mhaDatahiveNricFoundFlowService,
                                MhaDatahiveNricNotFoundFlowService mhaDatahiveNricNotFoundFlowService,
                                MhaMixedPsBatchFlowService mhaMixedPsBatchFlowService,
                                MhaLifeStatusValidationFlowService mhaLifeStatusValidationFlowService,
                                MhaStageProgressionFlowService mhaStageProgressionFlowService,
                                MhaMultipleNoticeTypesFlowService mhaMultipleNoticeTypesFlowService) {
        this.mhaNricUploadFlowService = mhaNricUploadFlowService;
        this.mhaNricDownloadFlowService = mhaNricDownloadFlowService;
        this.mhaNricUploadScenarioService = mhaNricUploadScenarioService;
        this.mhaNricNormalSuccessFlowService = mhaNricNormalSuccessFlowService;
        this.mhaTsNroSuspensionFlowService = mhaTsNroSuspensionFlowService;
        this.mhaPsRipSuspensionFlowService = mhaPsRipSuspensionFlowService;
        this.mhaPsRp2SuspensionFlowService = mhaPsRp2SuspensionFlowService;
        this.mhaInvalidAddressTagFlowService = mhaInvalidAddressTagFlowService;
        this.mhaStreetAddressValidationFlowService = mhaStreetAddressValidationFlowService;
        this.mhaPostalCodeValidationFlowService = mhaPostalCodeValidationFlowService;
        this.mhaFutureDateAddressChangeFlowService = mhaFutureDateAddressChangeFlowService;
        this.mhaMultipleOwnerDriverFlowService = mhaMultipleOwnerDriverFlowService;
        this.mhaEmbassyDiplomatFlowService = mhaEmbassyDiplomatFlowService;
        this.mhaExceptionReportFlowService = mhaExceptionReportFlowService;
        this.mhaDatahiveNricFoundFlowService = mhaDatahiveNricFoundFlowService;
        this.mhaDatahiveNricNotFoundFlowService = mhaDatahiveNricNotFoundFlowService;
        this.mhaMixedPsBatchFlowService = mhaMixedPsBatchFlowService;
        this.mhaLifeStatusValidationFlowService = mhaLifeStatusValidationFlowService;
        this.mhaStageProgressionFlowService = mhaStageProgressionFlowService;
        this.mhaMultipleNoticeTypesFlowService = mhaMultipleNoticeTypesFlowService;
    }

    /**
     * CONVERT FROM SH
     * Run MHA NRIC upload test flow.
     * This endpoint triggers the MHA NRIC upload test flow which prepares test data,
     * creates and uploads response files, triggers the upload API, and verifies the results.
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/nric-upload")
    public ResponseEntity<List<TestStepResult>> runNricUploadTest(@RequestParam(value = "noticePrefix", required = false) String noticePrefix) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided to prevent NULL parameter issues
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            String ts = java.time.format.DateTimeFormatter.ofPattern("HHmm").format(java.time.LocalDateTime.now());
            noticePrefix = "MHA" + ts;
            log.info("Generated notice prefix: {}", noticePrefix);
        } else {
            log.info("Using provided notice prefix: {}", noticePrefix);
        }

        log.info("Starting MHA NRIC upload test flow with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaNricUploadFlowService.runTest(noticePrefix);

        // Calculate summary statistics
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

        log.info("Completed MHA NRIC upload test flow with status: {}", allSuccess ? "SUCCESS" : "PARTIAL");
        return ResponseEntity.ok(results);
    }

    /**
     * CONVERT FROM SH
     * Run MHA NRIC download test flow.
     * This endpoint triggers the MHA NRIC download test flow which prepares test data,
     * creates and uploads response files, triggers the download API, and verifies the results.
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/nric-download")
    public ResponseEntity<List<TestStepResult>> runNricDownloadTest(@RequestParam(value = "noticePrefix", required = false) String noticePrefix) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided to prevent NULL parameter issues
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            String ts = java.time.format.DateTimeFormatter.ofPattern("HHmm").format(java.time.LocalDateTime.now());
            noticePrefix = "MHA" + ts;
            log.info("Generated notice prefix: {}", noticePrefix);
        } else {
            log.info("Using provided notice prefix: {}", noticePrefix);
        }

        log.info("Starting MHA NRIC download test flow with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaNricDownloadFlowService.runTest(noticePrefix);

        // Calculate summary statistics
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

        log.info("Completed MHA NRIC download test flow with status: {}", allSuccess ? "SUCCESS" : "PARTIAL");
        return ResponseEntity.ok(results);
    }

    /**
     * Run both MHA NRIC upload and download test flows in sequence.
     * This endpoint triggers both test flows one after the other.
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results from both flows
     */
    @PostMapping("/nric-full-flow")
    public ResponseEntity<List<TestStepResult>> runNricFullFlow(@RequestParam(value = "noticePrefix", required = false) String noticePrefix) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting MHA NRIC full flow test (upload + download)");

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            String ts = java.time.format.DateTimeFormatter.ofPattern("HHmm").format(java.time.LocalDateTime.now());
            noticePrefix = "MHA" + ts;
        }

        List<TestStepResult> combinedResults = new ArrayList<>();

        // Run upload flow
        TestStepResult uploadHeader = new TestStepResult("MHA NRIC Upload Flow", "INFO");
        uploadHeader.addDetail("Starting upload flow with notice prefix: " + noticePrefix);
        combinedResults.add(uploadHeader);

        List<TestStepResult> uploadResults = mhaNricUploadFlowService.runTest(noticePrefix);
        combinedResults.addAll(uploadResults);

        // Check if upload was successful before proceeding to download
        boolean uploadSuccess = uploadResults.stream()
            .filter(s -> !"SKIPPED".equals(s.getStatus()))
            .allMatch(s -> "SUCCESS".equals(s.getStatus()) || "INFO".equals(s.getStatus()));

        // Run download flow only if upload was successful
        if (uploadSuccess) {
            TestStepResult downloadHeader = new TestStepResult("MHA NRIC Download Flow", "INFO");
            downloadHeader.addDetail("Starting download flow with notice prefix: " + noticePrefix);
            combinedResults.add(downloadHeader);

            List<TestStepResult> downloadResults = mhaNricDownloadFlowService.runTest(noticePrefix);
            combinedResults.addAll(downloadResults);
        } else {
            TestStepResult downloadSkipped = new TestStepResult("MHA NRIC Download Flow", "SKIPPED");
            downloadSkipped.addDetail("⚠️ Download flow skipped due to upload flow failure");
            combinedResults.add(downloadSkipped);
        }

        // Calculate overall summary statistics
        boolean allSuccess = combinedResults.stream()
            .filter(s -> !"SKIPPED".equals(s.getStatus()))
            .allMatch(s -> "SUCCESS".equals(s.getStatus()) || "INFO".equals(s.getStatus()));

        TestStepResult summary = new TestStepResult("Full Test Flow Summary", allSuccess ? "SUCCESS" : "PARTIAL");
        long successCount = combinedResults.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedCount = combinedResults.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        long skippedCount = combinedResults.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count();
        summary.addDetail("📊 Test Flow Statistics:");
        summary.addDetail("✅ Successful steps: " + successCount);
        summary.addDetail("❌ Failed steps: " + failedCount);
        summary.addDetail("⚠️ Skipped steps: " + skippedCount);
        summary.addDetail("Total steps: " + (successCount + failedCount + skippedCount));
        combinedResults.add(summary);

        log.info("Completed MHA NRIC full flow test with status: {}", allSuccess ? "SUCCESS" : "PARTIAL");
        return ResponseEntity.ok(combinedResults);
    }

    /**
     * SIMPLE TEST
     * Run MHATEST001: Normal Flow - Valid NRIC Data scenario test.
     * This endpoint triggers a specific test scenario that follows 3-step pattern:
     * 1. Setup test data with valid NRIC records (id_type='N', offender_indicator='Y')
     * 2. POST to MHA upload API endpoint
     * 3. Verify file generation, upload status, and database updates
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/nric-upload-scenario-test")
    public ResponseEntity<List<TestStepResult>> runNricUploadScenarioTest(@RequestParam(value = "noticePrefix", required = false) String noticePrefix) {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "MHATEST";
            log.info("Generated notice prefix: {}", noticePrefix);
        } else {
            log.info("Using provided notice prefix: {}", noticePrefix);
        }

        log.info("Starting MHATEST001: Normal Flow - Valid NRIC Data with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaNricUploadScenarioService.runTest(noticePrefix);

        // Calculate summary statistics
        boolean allSuccess = results.stream()
            .filter(s -> !"SKIPPED".equals(s.getStatus()))
            .allMatch(s -> "SUCCESS".equals(s.getStatus()) || "INFO".equals(s.getStatus()));

        TestStepResult summary = new TestStepResult("MHATEST001 Summary", allSuccess ? "SUCCESS" : "PARTIAL");
        long successCount = results.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedCount = results.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        long skippedCount = results.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count();
        summary.addDetail("🧪 MHATEST001: Normal Flow - Valid NRIC Data");
        summary.addDetail("📊 Test Results:");
        summary.addDetail("✅ Successful steps: " + successCount);
        summary.addDetail("❌ Failed steps: " + failedCount);
        summary.addDetail("⚠️ Skipped steps: " + skippedCount);
        summary.addDetail("Total steps: " + (successCount + failedCount + skippedCount));
        results.add(summary);

        log.info("Completed MHATEST001 with status: {}", allSuccess ? "SUCCESS" : "PARTIAL");
        return ResponseEntity.ok(results);
    }

    // -----------------------------------------------------
    /**
     * Run Normal NRIC Success Flow End-to-End Test.
     * This endpoint triggers Scenario 1: Normal NRIC Success Flow that follows complete 6-step pattern:
     * 1. Setup test data (create notice with valid NRIC)
     * 2. POST /mha/upload (trigger upload process)
     * 3. Verify files in SFTP (check generated files exist)
     * 4. POST /test/mha/mha-callback (simulate MHA response)
     * 5. POST /mha/download (trigger download process)
     * 6. Verify database updates and final state
     *
     * @return List of test step results
     */
    @PostMapping("/normal-success-flow")
    public ResponseEntity<List<TestStepResult>> runNormalSuccessFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("🎯 Running Normal NRIC Success Flow End-to-End Test");
        List<TestStepResult> results = mhaNricNormalSuccessFlowService.runTest();

        // Calculate summary statistics
        boolean allSuccess = results.stream().allMatch(r -> "SUCCESS".equals(r.getStatus()));

        TestStepResult summary = new TestStepResult("Normal Success Flow Summary", allSuccess ? "SUCCESS" : "PARTIAL");
        long successCount = results.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedCount = results.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        long skippedCount = results.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count();
        summary.addDetail("🧪 Scenario 1: Normal NRIC Success Flow End-to-End Test");
        summary.addDetail("📊 Test Results:");
        summary.addDetail("✅ Successful steps: " + successCount);
        summary.addDetail("❌ Failed steps: " + failedCount);
        summary.addDetail("⚠️ Skipped steps: " + skippedCount);
        summary.addDetail("Total steps: " + (successCount + failedCount + skippedCount));
        summary.addDetail("🎯 Expected Changes Verified:");
        summary.addDetail("  - ocms_valid_offence_notice: Processing stages maintained");
        summary.addDetail("  - ocms_offence_notice_detail: Vehicle info populated");
        summary.addDetail("  - ocms_offence_notice_owner_driver: Owner details updated");
        summary.addDetail("  - ocms_offence_notice_owner_driver_addr: Address fields updated");
        summary.addDetail("  - File cleanup: NRO2URA .EXP, .TOT files removed");
        results.add(summary);

        log.info("Completed Normal NRIC Success Flow End-to-End Test with status: {}", allSuccess ? "SUCCESS" : "PARTIAL");
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA TS-NRO Suspension Flow Test.
     * Scenario 2: TS suspension with reason NRO
     * Expected: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
     *
     * @return List of test step results
     */
    @PostMapping("/ts-nro-flow")
    public ResponseEntity<List<TestStepResult>> runTsNroSuspensionFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting TS-NRO Suspension Flow Test");
        List<TestStepResult> results = mhaTsNroSuspensionFlowService.executeFlow();

        log.info("Completed TS-NRO Suspension Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }


    /**
     * Run MHA Invalid Address Tag Validation Flow Test.
     * Scenario 3: TS-NRO suspension triggered by invalid address response from MHA
     * Expected: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
     * when MHA response shows Invalid Address Tag has Value = true
     *
     * @return List of test step results
     */
    @PostMapping("/invalid-address-tag-flow")
    public ResponseEntity<List<TestStepResult>> runInvalidAddressTagFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Invalid Address Tag Validation Flow Test");
        List<TestStepResult> results = mhaInvalidAddressTagFlowService.executeFlow();

        log.info("Completed Invalid Address Tag Validation Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Street Address Specific Validation Flow Test.
     * Scenario 4: TS-NRO suspension for specific invalid address pattern
     * Expected: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
     * when MHA response shows street="NA"
     *
     * @return List of test step results
     */
    @PostMapping("/street-address-validation-flow")
    public ResponseEntity<List<TestStepResult>> runStreetAddressValidationFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Street Address Specific Validation Flow Test");
        List<TestStepResult> results = mhaStreetAddressValidationFlowService.executeFlow();

        log.info("Completed Street Address Specific Validation Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Postal Code Validation Flow Test.
     * Scenario 5: TS-NRO suspension for specific invalid address pattern
     * Expected: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
     * when MHA response shows postal_code="000000"
     *
     * @return List of test step results
     */
    @PostMapping("/postal-code-validation-flow")
    public ResponseEntity<List<TestStepResult>> runPostalCodeValidationFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Postal Code Validation Flow Test");
        List<TestStepResult> results = mhaPostalCodeValidationFlowService.executeFlow();

        log.info("Completed Postal Code Validation Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Postal Code Validation Flow Test.
     * Scenario 6: TS-NRO suspension for specific invalid address pattern
     * Expected: VON updated with suspension_type="TS", epr_reason_of_suspension="NRO"
     * when MHA response shows lastChangeAddressDate > today
     *
     * @return List of test step results
     */
    @PostMapping("/future-date-address-change-flow")
    public ResponseEntity<List<TestStepResult>> runFutureDateAddressChangeFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Future Date Address Change Flow Test");
        List<TestStepResult> results = mhaFutureDateAddressChangeFlowService.executeFlow();

        log.info("Completed Future Date Address Change Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA PS-RIP Suspension Flow Test.
     * Scenario 7: PS suspension when DoD on/after offence date
     * Expected: VON updated with suspension_type="PS", epr_reason_of_suspension="RIP"
     *
     * @return List of test step results
     */
    @PostMapping("/ps-rip-flow")
    public ResponseEntity<List<TestStepResult>> runPsRipSuspensionFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting PS-RIP Suspension Flow Test");
        List<TestStepResult> results = mhaPsRipSuspensionFlowService.executeFlow();

        log.info("Completed PS-RIP Suspension Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA PS-RP2 Suspension Flow Test.
     * Scenario 8: PS suspension when DoD before offence date
     * Expected: VON updated with suspension_type="PS", epr_reason_of_suspension="RP2"
     *
     * @return List of test step results
     */
    @PostMapping("/ps-rp2-flow")
    public ResponseEntity<List<TestStepResult>> runPsRp2SuspensionFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting PS-RP2 Suspension Flow Test");
        List<TestStepResult> results = mhaPsRp2SuspensionFlowService.executeFlow();

        log.info("Completed PS-RP2 Suspension Flow Test with status: {}",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Multiple Owner/Driver Flow Test.
     * Scenario 9: Multiple owner/driver processing with primary designation
     * Expected: Multiple owner/driver records updated with primary designation
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/multiple-owner-driver-flow")
    public ResponseEntity<List<TestStepResult>> runMultipleOwnerDriverFlow(@RequestParam(required = false) String noticePrefix) {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "MULTIOD";
            log.info("Generated notice prefix: {}", noticePrefix);
        }

        log.info("Starting Multiple Owner/Driver Flow Test with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaMultipleOwnerDriverFlowService.executeFlow(noticePrefix);

        log.info("Completed Multiple Owner/Driver Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Embassy/Diplomat Flow Test.
     * Scenario 10: Embassy/diplomatic vehicle processing
     * Expected: Diplomatic flag and vehicle registration type updates
     *
     * @return List of test step results
     */
    @PostMapping("/embassy-diplomat-flow")
    public ResponseEntity<List<TestStepResult>> runEmbassyDiplomatFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Embassy/Diplomat Flow Test");
        List<TestStepResult> results = mhaEmbassyDiplomatFlowService.executeFlow();

        log.info("Completed Embassy/Diplomat Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Exception Report Flow Test.
     * Scenario 11: Exception report processing with .EXP files
     * Expected: Suspension application and exception record processing
     *
     * @return List of test step results
     */
    @PostMapping("/exception-report-flow")
    public ResponseEntity<List<TestStepResult>> runExceptionReportFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting Exception Report Flow Test");
        List<TestStepResult> results = mhaExceptionReportFlowService.executeFlow();

        log.info("Completed Exception Report Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA DataHive NRIC Found Flow Test.
     * Scenario 12: Successful NRIC lookup with contact update from DataHive
     * Expected: Contact fields updated from DataHive lookup
     *
     * @return List of test step results
     */
    @PostMapping("/datahive-nric-found-flow")
    public ResponseEntity<List<TestStepResult>> runDatahiveNricFoundFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting DataHive NRIC Found Flow Test");
        List<TestStepResult> results = mhaDatahiveNricFoundFlowService.executeFlow();

        log.info("Completed DataHive NRIC Found Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA DataHive NRIC Not Found Flow Test.
     * Scenario 13: NRIC not found in DataHive, no contact update
     * Expected: Original contact info remains unchanged, processing continues
     *
     * @return List of test step results
     */
    @PostMapping("/datahive-nric-not-found-flow")
    public ResponseEntity<List<TestStepResult>> runDatahiveNricNotFoundFlow() {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        log.info("Starting DataHive NRIC Not Found Flow Test");
        List<TestStepResult> results = mhaDatahiveNricNotFoundFlowService.executeFlow();

        log.info("Completed DataHive NRIC Not Found Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Mixed PS Batch Flow Test.
     * Scenario 14: Mixed PS-RIP and PS-RP2 batch processing with different DoD logic
     * Expected: Some notices get PS-RIP, others get PS-RP2 based on DoD vs offence date
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/mixed-ps-batch-flow")
    public ResponseEntity<List<TestStepResult>> runMixedPsBatchFlow(@RequestParam(required = false) String noticePrefix) {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "MIXEDPS";
            log.info("Generated notice prefix: {}", noticePrefix);
        }

        log.info("Starting Mixed PS Batch Flow Test with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaMixedPsBatchFlowService.executeFlow(noticePrefix);

        log.info("Completed Mixed PS Batch Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Life Status Validation Flow Test.
     * Scenario 15: Different processing based on lifeStatus values (D vs A)
     * Expected: Suspension applied only for lifeStatus='D' (deceased)
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/life-status-validation-flow")
    public ResponseEntity<List<TestStepResult>> runLifeStatusValidationFlow(@RequestParam(required = false) String noticePrefix) {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "LIFESTS";
            log.info("Generated notice prefix: {}", noticePrefix);
        }

        log.info("Starting Life Status Validation Flow Test with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaLifeStatusValidationFlowService.executeFlow(noticePrefix);

        log.info("Completed Life Status Validation Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Stage Progression Flow Test.
     * Scenario 16: Stage progression transitions through NPA → ROV → ENA → RD1
     * Expected: Correct stage transitions and processing dates updated
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/stage-progression-flow")
    public ResponseEntity<List<TestStepResult>> runStageProgressionFlow(@RequestParam(required = false) String noticePrefix) {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "STAGEPROG";
            log.info("Generated notice prefix: {}", noticePrefix);
        }

        log.info("Starting Stage Progression Flow Test with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaStageProgressionFlowService.executeFlow(noticePrefix);

        log.info("Completed Stage Progression Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

    /**
     * Run MHA Multiple Notice Types Flow Test.
     * Scenario 17: Different offence categories with different processing logic
     * Expected: Business rules adapt to different notice types and penalty amounts
     *
     * @param noticePrefix Optional custom notice prefix, default will be generated if not provided
     * @return List of test step results
     */
    @PostMapping("/multiple-notice-types-flow")
    public ResponseEntity<List<TestStepResult>> runMultipleNoticeTypesFlow(@RequestParam(required = false) String noticePrefix) {

        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        // Generate notice prefix if not provided
        if (noticePrefix == null || noticePrefix.trim().isEmpty()) {
            noticePrefix = "MULTITYP";
            log.info("Generated notice prefix: {}", noticePrefix);
        }

        log.info("Starting Multiple Notice Types Flow Test with notice prefix: {}", noticePrefix);
        List<TestStepResult> results = mhaMultipleNoticeTypesFlowService.executeFlow(noticePrefix);

        log.info("Completed Multiple Notice Types Flow Test with status: {},",
                 results.isEmpty() ? "No steps" : results.get(results.size() - 1).getStatus());
        return ResponseEntity.ok(results);
    }

}
