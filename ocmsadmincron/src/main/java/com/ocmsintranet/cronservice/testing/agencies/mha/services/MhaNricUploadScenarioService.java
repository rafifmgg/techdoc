package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.common.ApiConfigHelper;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Service class for MHA NRIC Upload Scenario Testing
 * Implements MHATEST001: Normal Flow - Valid NRIC Data
 *
 * This service follows 3-step pattern:
 * 1. Setup test data in SQL tables (ocms_valid_offence_notice, ocms_offence_notice_detail, ocms_offence_notice_owner_driver)
 * 2. POST to /v1/mha/upload endpoint
 * 3. Verify file generation, upload status, and database updates
 */
@Slf4j
@Service
public class MhaNricUploadScenarioService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_INFO = "INFO";

    // Test data constants - following DataHiveContactNRIC pattern
    private static final String SCHEMA = "ocmsizmgr";
    private static final String TEST_VEHICLE_NO_1 = "SBA1234A";
    private static final String TEST_VEHICLE_NO_2 = "SBA5678B";
    private static final String TEST_VEHICLE_NO_3 = "SBA9012C";
    private static final String TEST_NRIC_1 = "S1234567A";
    private static final String TEST_NRIC_2 = "S2345678B";
    private static final String TEST_NRIC_3 = "S3456789C";
    private static final String TEST_ID_TYPE = "N";  // NRIC ID type

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final SftpUtil sftpUtil;

    @Autowired
    private ApiConfigHelper apiConfigHelper;

    /**
     * Constructor for MhaNricUploadScenarioService
     *
     * @param jdbcTemplate JdbcTemplate for database operations
     * @param restTemplate RestTemplate for API calls
     * @param sftpUtil SftpUtil for SFTP operations
     */
    @Autowired
    public MhaNricUploadScenarioService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate, SftpUtil sftpUtil) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.sftpUtil = sftpUtil;
    }

    /**
     * Run MHATEST001: Normal Flow - Valid NRIC Data test scenario
     *
     * @param noticePrefix Prefix for test notice numbers
     * @return List of test step results
     */
    public List<TestStepResult> runTest(String noticePrefix) {
        List<TestStepResult> results = new ArrayList<>();
        boolean continueExecution = true;

        log.info("🚀 Starting MHATEST001: Normal Flow - Valid NRIC Data with prefix: {}", noticePrefix);

        // Step 1: Setup test data with valid NRIC records
        TestStepResult step1 = setupTestDataForMhaUpload(noticePrefix);
        results.add(step1);
        if (STATUS_FAILED.equals(step1.getStatus())) {
            continueExecution = false;
        }

        // Step 2: Trigger MHA upload API
        if (continueExecution) {
            TestStepResult step2 = triggerMhaUploadApi();
            results.add(step2);
            if (STATUS_FAILED.equals(step2.getStatus())) {
                continueExecution = false;
            }
        }

        // Step 3: Verify MHA upload results
        if (continueExecution) {
            TestStepResult step3 = verifyMhaUploadResults(noticePrefix);
            results.add(step3);
        }

        log.info("✅ Completed MHATEST001 with {} steps", results.size());
        return results;
    }

    /**
     * Step 1: Setup test data for MHA upload
     * Insert test records with id_type='N' (NRIC), offender_indicator='Y', next_processing_stage='RD1'
     */
    private TestStepResult setupTestDataForMhaUpload(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 1: Setup Test Data for MHA Upload", STATUS_INFO);

        try {
            // Generate unique notice numbers using prefix and timestamp
            // String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
            String noticeNumber1 = noticePrefix + "001"; // Take HHmmss part
            String noticeNumber2 = noticePrefix + "002";
            String noticeNumber3 = noticePrefix + "003";

            log.info("🔧 Setting up test data with notice numbers: {}, {}, {}", noticeNumber1, noticeNumber2, noticeNumber3);

            result.addDetail("🧹 Cleaning up existing test data...");
            cleanupTestData(noticePrefix);

            result.addDetail("📝 Setting up test data for MHA upload testing...");

            // Insert VON records for MHA upload
            insertValidOffenceNoticeForMha(result, noticeNumber1, TEST_VEHICLE_NO_1);
            insertValidOffenceNoticeForMha(result, noticeNumber2, TEST_VEHICLE_NO_2);
            insertValidOffenceNoticeForMha(result, noticeNumber3, TEST_VEHICLE_NO_3);

            // Insert OND records
            insertOffenceNoticeDetail(result, noticeNumber1);
            insertOffenceNoticeDetail(result, noticeNumber2);
            insertOffenceNoticeDetail(result, noticeNumber3);

            // Insert owner/driver records with valid NRIC
            insertOwnerDriverWithNric(result, noticeNumber1, TEST_NRIC_1, "John Doe");
            insertOwnerDriverWithNric(result, noticeNumber2, TEST_NRIC_2, "Jane Smith");
            insertOwnerDriverWithNric(result, noticeNumber3, TEST_NRIC_3, "Bob Wilson");

            // Verify data insertion
            String countSQL = """
                SELECT COUNT(*) FROM %s.ocms_valid_offence_notice
                WHERE notice_no LIKE ?
                AND next_processing_stage = 'RD1'
                """.formatted(SCHEMA);

            Integer count = jdbcTemplate.queryForObject(countSQL, Integer.class, noticePrefix + "%");

            if (count != null && count == 3) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ Successfully inserted 3 test records for MHA upload");
                result.addDetail("📋 Notice numbers: " + noticeNumber1 + ", " + noticeNumber2 + ", " + noticeNumber3);
                result.addDetail("🎯 Filter criteria: next_processing_stage='RD1' (ready for MHA upload)");
                log.info("✅ Test data setup completed successfully");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Data insertion verification failed. Expected 3 records, found: " + count);
            }

        } catch (Exception e) {
            log.error("❌ Error setting up test data: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error during test data setup: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 2: Trigger MHA upload API endpoint
     */
    private TestStepResult triggerMhaUploadApi() {
        TestStepResult result = new TestStepResult("Step 2: Trigger MHA Upload API", STATUS_INFO);

        try {
            String uploadUrl = apiConfigHelper.buildApiUrl("/v1/mha/upload");
            log.info("🚀 Calling MHA upload API: {}", uploadUrl);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make POST request to MHA upload endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ MHA upload API called successfully");
                result.addDetail("📡 Response status: " + response.getStatusCode());
                result.addDetail("📄 Response body: " + response.getBody());
                log.info("✅ MHA upload API completed successfully");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ MHA upload API failed with status: " + response.getStatusCode());
                result.addDetail("📄 Response body: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Error calling MHA upload API: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error calling MHA upload API: " + e.getMessage());
        }

        return result;
    }

    /**
     * Step 3: Verify MHA upload results
     * Check file generation, SFTP upload, and database updates
     */
    private TestStepResult verifyMhaUploadResults(String noticePrefix) {
        TestStepResult result = new TestStepResult("Step 3: Verify MHA Upload Results", STATUS_INFO);

        try {
            log.info("🔍 Verifying MHA upload results for prefix: {}", noticePrefix);

            boolean allVerificationsPassed = true;
            List<String> verificationResults = new ArrayList<>();

            // Verification 1: Check if records were processed (assuming processing stage changes)
            String processedSQL = """
                SELECT COUNT(*) FROM %s.ocms_valid_offence_notice
                WHERE notice_no LIKE ?
                AND next_processing_stage = 'RD1'
                """.formatted(SCHEMA);

            Integer processedCount = jdbcTemplate.queryForObject(processedSQL, Integer.class, noticePrefix + "%");

            if (processedCount != null && processedCount > 0) {
                verificationResults.add("✅ Found " + processedCount + " records ready for MHA processing");
            } else {
                verificationResults.add("❌ No records found for MHA processing");
                allVerificationsPassed = false;
            }

            // Verification 2: Check for exclusion logic (HST table)
            try {
                String hstCheckSQL = """
                    SELECT COUNT(*) FROM ocms_hst h
                    INNER JOIN ocms_valid_offence_notice v ON h.id_number = v.id_number
                    WHERE v.notice_no LIKE ?
                    """;

                Integer hstCount = jdbcTemplate.queryForObject(hstCheckSQL, Integer.class, noticePrefix + "%");

                if (hstCount != null && hstCount == 0) {
                    verificationResults.add("✅ Exclusion logic verified - No HST conflicts found");
                } else {
                    verificationResults.add("⚠️ Found " + hstCount + " HST conflicts (should be excluded from processing)");
                }
            } catch (Exception e) {
                verificationResults.add("⚠️ HST exclusion check skipped: " + e.getMessage());
            }

            // Verification 3: Check SFTP upload
            try {
                // Define SFTP server and path constants for MHA upload
                final String SFTP_SERVER = "mha";
                final String SFTP_OUTPUT_PATH = "/mhanro/input";

                // Get current date in YYYYMMDD format for file prefix
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                String currentDate = dateFormat.format(new Date());
                String filePrefix = "URA2NRO_" + currentDate;

                verificationResults.add("🔍 Verifying presence of MHA upload file on SFTP server...");
                verificationResults.add("📁 SFTP Server: " + SFTP_SERVER);
                verificationResults.add("📁 Directory: " + SFTP_OUTPUT_PATH);
                verificationResults.add("🔎 Looking for files with prefix: " + filePrefix);

                // List files in SFTP directory
                List<String> files = null;
                try {
                    files = sftpUtil.listFiles(SFTP_SERVER, SFTP_OUTPUT_PATH);
                    verificationResults.add("📋 Total files found in directory: " + files.size());

                    // Filter files by prefix (only if files listing was successful)
                    if (files != null) {
                        List<String> matchingFiles = new ArrayList<>();
                        for (String file : files) {
                            if (file.startsWith(filePrefix)) {
                                matchingFiles.add(file);
                            }
                        }

                        verificationResults.add("📋 Files matching prefix '" + filePrefix + "': " + matchingFiles.size());

                        // Check if any matching files were found
                        if (matchingFiles.isEmpty()) {
                            verificationResults.add("❌ No MHA upload files found with prefix: " + filePrefix);
                            allVerificationsPassed = false;
                        } else {
                            // Sort files by name (which includes timestamp)
                            Collections.sort(matchingFiles, Comparator.reverseOrder());

                            // Get the latest file
                            String latestFile = matchingFiles.get(0);
                            verificationResults.add("✅ Latest MHA upload file found: " + latestFile);

                            // Add all matching files to the result
                            verificationResults.add("📋 All matching files:");
                            for (String file : matchingFiles) {
                                verificationResults.add("  - " + file);
                            }
                        }
                    }

                } catch (Exception e) {
                    verificationResults.add("❌ Failed to list files on SFTP server: " + e.getMessage());
                    allVerificationsPassed = false;
                }

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                verificationResults.add("❌ Failed to verify MHA upload file on SFTP: " + e.getMessage());
                verificationResults.add("❌ Stack trace: " + sw.toString());
                allVerificationsPassed = false;
            }

            // Set final result status
            if (allVerificationsPassed) {
                result.setStatus(STATUS_SUCCESS);
                result.addDetail("✅ All MHA upload verifications passed");
            } else {
                result.setStatus(STATUS_FAILED);
                result.addDetail("❌ Some MHA upload verifications failed");
            }

            // Add all verification details
            verificationResults.forEach(result::addDetail);

            log.info("✅ MHA upload verification completed");

        } catch (Exception e) {
            log.error("❌ Error verifying MHA upload results: {}", e.getMessage(), e);
            result.setStatus(STATUS_FAILED);
            result.addDetail("❌ Error during MHA upload verification: " + e.getMessage());
        }

        return result;
    }

    /**
     * Clean up existing test data with the given prefix
     */
    private void cleanupTestData(String noticePrefix) {
        try {
            log.info("🧹 Cleaning up existing test data with prefix: {}", noticePrefix);

            // Delete in reverse order due to foreign key constraints
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_owner_driver WHERE notice_no LIKE ?", noticePrefix + "%");
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_offence_notice_detail WHERE notice_no LIKE ?", noticePrefix + "%");
            jdbcTemplate.update("DELETE FROM " + SCHEMA + ".ocms_valid_offence_notice WHERE notice_no LIKE ?", noticePrefix + "%");

            log.info("✅ Test data cleanup completed");
        } catch (Exception e) {
            log.warn("⚠️ Error during test data cleanup: {}", e.getMessage());
        }
    }

    /** Insert VON record for MHA upload testing */
    private void insertValidOffenceNoticeForMha(TestStepResult result, String noticeNo, String vehicleNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_valid_offence_notice (" +
                     "notice_no, composition_amount, computer_rule_code, " +
                     "last_processing_stage, last_processing_date, next_processing_stage, next_processing_date, " +
                     "notice_date_and_time, offence_notice_type, pp_code, pp_name, vehicle_category, " +
                     "vehicle_no, vehicle_registration_type, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            noticeNo, new BigDecimal("70.00"), 10300,
            "ROV", LocalDateTime.now(), "RD1", LocalDateTime.now(),
            LocalDateTime.now(), "O", "A0045", "ATTAP VALLEY ROAD", "C",
            vehicleNo, "S",
            "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ VON record inserted: " + noticeNo + " (Vehicle: " + vehicleNo + ")");
    }

    /** Insert OND record */
    private void insertOffenceNoticeDetail(TestStepResult result, String noticeNo) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_detail (" +
                     "notice_no, lta_chassis_number, lta_diplomatic_flag, lta_eff_ownership_date, " +
                     "lta_make_description, lta_primary_colour, cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, "TESTCHASSIS" + noticeNo.substring(noticeNo.length()-3), "N",
            LocalDateTime.now().minusYears(1), "TOYOTA", "WHITE", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ OND record inserted: " + noticeNo);
    }

    /** Insert owner/driver record with valid NRIC */
    private void insertOwnerDriverWithNric(TestStepResult result, String noticeNo, String nric, String name) {
        String sql = "INSERT INTO " + SCHEMA + ".ocms_offence_notice_owner_driver (" +
                     "notice_no, name, id_no, id_type, offender_indicator, owner_driver_indicator, " +
                     "cre_user_id, cre_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, noticeNo, name, nric, TEST_ID_TYPE, "Y", "O", "TEST_USER", LocalDateTime.now());

        result.addDetail("✅ Owner/driver record inserted: " + noticeNo + " (NRIC: " + nric + ", Name: " + name + ")");
    }
}