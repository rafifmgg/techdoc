package com.ocmsintranet.cronservice.testing.notification_sms_email.services;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.notification_sms_email.helpers.NotificationTestDataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for testing notification SMS/Email workflow with HST checking.
 * This service implements the test flow logic converted from test-notification-query.sh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSmsEmailTestService {

    private final NotificationTestDataHelper notificationTestDataHelper;
    private final RestTemplate restTemplate;

    @Value("${ocms.api.base-url:http://localhost:8083}")
    private String apiBaseUrl;

    private static final String TEST_USER = "TEST_USER";
    private static final int PROCESSING_WAIT_TIME = 20; // seconds

    /**
     * Run the complete notification SMS/Email test flow.
     * This method executes all test steps sequentially and collects results.
     *
     * @return List of test step results
     */
    public List<TestStepResult> runTest() {
        List<TestStepResult> results = new ArrayList<>();

        try {
            // Step 1: Clean up test data
            results.add(cleanupTestData());

            // Step 2: Insert test data
            results.add(insertTestData());

            // Step 3: Verify data insertion
            results.add(verifyDataInsertion());

            // Step 4: Test repository query
            results.add(testRepositoryQuery());

            // Step 5: Test HST checking logic
            results.add(testHstChecking());

            // Step 6: Trigger notification job
            results.add(triggerNotificationJob());

            // Step 7: Wait for processing
            results.add(waitForProcessing());

            // Step 8: Verify notification records
            results.add(verifyNotificationRecords());

            // Step 9: Verify TS-HST suspension updates
            results.add(verifyTsHstSuspension());

        } catch (Exception e) {
            log.error("❌ Unexpected error during test execution", e);
            TestStepResult errorResult = new TestStepResult("Unexpected Error", "FAILED");
            errorResult.addDetail("Test flow interrupted: " + e.getMessage());
            results.add(errorResult);
        }

        return results;
    }

    /**
     * Step 1: Clean up test data from previous test runs.
     * Deletes records from multiple tables where notice_no matches test patterns.
     *
     * @return Test step result
     */
    private TestStepResult cleanupTestData() {
        log.info("🧹 Step 1: Cleaning up test data");
        try {
            int deletedCount = notificationTestDataHelper.cleanupTestData();
            TestStepResult result = new TestStepResult("Cleanup Test Data", "SUCCESS");
            result.addDetail(String.format("Successfully cleaned up test data. Deleted %d records.", deletedCount));
            return result;
        } catch (Exception e) {
            log.error("❌ Error cleaning up test data", e);
            TestStepResult result = new TestStepResult("Cleanup Test Data", "FAILED");
            result.addDetail("Failed to clean up test data: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 2: Insert test data for all test cases.
     * Inserts records into VON, owner/driver, HST, and exclusion list tables.
     *
     * @return Test step result
     */
    private TestStepResult insertTestData() {
        log.info("📝 Step 2: Inserting test data");
        try {
            Map<String, Integer> insertCounts = notificationTestDataHelper.insertTestData(TEST_USER);
            TestStepResult result = new TestStepResult("Insert Test Data", "SUCCESS");
            result.addDetail(String.format("Successfully inserted test data: %d VON records, %d owner/driver records, " +
                    "%d HST records, %d exclusion list records",
                    insertCounts.getOrDefault("von", 0),
                    insertCounts.getOrDefault("ownerDriver", 0),
                    insertCounts.getOrDefault("hst", 0),
                    insertCounts.getOrDefault("exclusionList", 0)));
            return result;
        } catch (Exception e) {
            log.error("❌ Error inserting test data", e);
            TestStepResult result = new TestStepResult("Insert Test Data", "FAILED");
            result.addDetail("Failed to insert test data: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 3: Verify that test data was inserted correctly.
     * Checks counts and key fields in inserted records.
     *
     * @return Test step result
     */
    private TestStepResult verifyDataInsertion() {
        log.info("🔍 Step 3: Verifying data insertion");
        try {
            Map<String, Boolean> verificationResults = notificationTestDataHelper.verifyDataInsertion();

            boolean allVerified = verificationResults.values().stream().allMatch(v -> v);

            if (allVerified) {
                TestStepResult result = new TestStepResult("Verify Data Insertion", "SUCCESS");
                result.addDetail("All test data verified successfully");
                return result;
            } else {
                String failedVerifications = verificationResults.entrySet().stream()
                        .filter(entry -> !entry.getValue())
                        .map(Map.Entry::getKey)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                TestStepResult result = new TestStepResult("Verify Data Insertion", "FAILED");
                result.addDetail("Data verification failed for: " + failedVerifications);
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Error verifying data insertion", e);
            TestStepResult result = new TestStepResult("Verify Data Insertion", "FAILED");
            result.addDetail("Failed to verify data insertion: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 4: Test the repository query used to select records for processing.
     * Verifies that the query returns the expected records.
     *
     * @return Test step result
     */
    private TestStepResult testRepositoryQuery() {
        log.info("🔍 Step 4: Testing repository query");
        try {
            List<String> selectedNotices = notificationTestDataHelper.testRepositoryQuery();

            boolean hasExpectedNotices = selectedNotices.contains("TEST001") &&
                    selectedNotices.contains("TEST002") &&
                    selectedNotices.contains("TEST003") &&
                    selectedNotices.contains("TEST004") &&
                    !selectedNotices.contains("TEST005") &&
                    !selectedNotices.contains("TEST006");

            if (hasExpectedNotices) {
                TestStepResult result = new TestStepResult("Test Repository Query", "SUCCESS");
                result.addDetail(String.format("Repository query returned %d records as expected", selectedNotices.size()));
                return result;
            } else {
                TestStepResult result = new TestStepResult("Test Repository Query", "FAILED");
                result.addDetail(String.format("Repository query returned unexpected notices: %s",
                        String.join(", ", selectedNotices)));
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Error testing repository query", e);
            TestStepResult result = new TestStepResult("Test Repository Query", "FAILED");
            result.addDetail("Failed to test repository query: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 5: Test HST checking logic.
     * Verifies that HST records are correctly identified for suspension.
     *
     * @return Test step result
     */
    private TestStepResult testHstChecking() {
        log.info("🔍 Step 5: Testing HST checking logic");
        try {
            Map<String, Boolean> hstCheckResults = notificationTestDataHelper.testHstChecking();

            boolean allChecksCorrect = hstCheckResults.get("TEST001") == false &&
                    hstCheckResults.get("TEST002") == true &&
                    hstCheckResults.get("TEST003") == true &&
                    hstCheckResults.get("TEST004") == true;

            if (allChecksCorrect) {
                TestStepResult result = new TestStepResult("Test HST Checking", "SUCCESS");
                result.addDetail("HST checking logic verified successfully");
                return result;
            } else {
                TestStepResult result = new TestStepResult("Test HST Checking", "FAILED");
                result.addDetail("HST checking logic verification failed");
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Error testing HST checking", e);
            TestStepResult result = new TestStepResult("Test HST Checking", "FAILED");
            result.addDetail("Failed to test HST checking: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 6: Trigger the notification job via API.
     * Calls the notification job endpoint to start processing.
     *
     * @return Test step result
     */
    private TestStepResult triggerNotificationJob() {
        log.info("🚀 Step 6: Triggering notification job");
        try {
            String url = apiBaseUrl + "/ocms/v1/notification-sms-email/execute";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            log.info("Calling API: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                TestStepResult result = new TestStepResult("Trigger Notification Job", "SUCCESS");
                result.addDetail(String.format("Successfully triggered notification job. Response: %s",
                        response.getBody()));
                return result;
            } else {
                TestStepResult result = new TestStepResult("Trigger Notification Job", "FAILED");
                result.addDetail(String.format("Failed to trigger notification job. Status: %s, Response: %s",
                        response.getStatusCode(), response.getBody()));
                return result;
            }
        } catch (RestClientException e) {
            log.error("❌ Error triggering notification job", e);
            TestStepResult result = new TestStepResult("Trigger Notification Job", "FAILED");
            result.addDetail("Failed to trigger notification job: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 7: Wait for notification processing to complete.
     * Pauses execution for a fixed duration.
     *
     * @return Test step result
     */
    private TestStepResult waitForProcessing() {
        log.info("⏳ Step 7: Waiting for processing to complete ({} seconds)", PROCESSING_WAIT_TIME);
        try {
            TimeUnit.SECONDS.sleep(PROCESSING_WAIT_TIME);
            TestStepResult result = new TestStepResult("Wait For Processing", "SUCCESS");
            result.addDetail(String.format("Waited %d seconds for processing to complete", PROCESSING_WAIT_TIME));
            return result;
        } catch (InterruptedException e) {
            log.error("❌ Error waiting for processing", e);
            Thread.currentThread().interrupt();
            TestStepResult result = new TestStepResult("Wait For Processing", "FAILED");
            result.addDetail("Wait interrupted: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 8: Verify notification records after processing.
     * Checks SMS and email notification records for each test case.
     *
     * @return Test step result
     */
    private TestStepResult verifyNotificationRecords() {
        log.info("🔍 Step 8: Verifying notification records");
        try {
            Map<String, Map<String, Integer>> notificationCounts =
                    notificationTestDataHelper.verifyNotificationRecords();

            Map<String, Integer> smsCounts = notificationCounts.get("sms");
            Map<String, Integer> emailCounts = notificationCounts.get("email");

            boolean allVerified =
                    smsCounts.getOrDefault("TEST001", 0) == 1 &&
                    smsCounts.getOrDefault("TEST002", 0) == 1 &&
                    smsCounts.getOrDefault("TEST003", 0) == 1 &&
                    smsCounts.getOrDefault("TEST004", 0) == 1 &&
                    smsCounts.getOrDefault("TEST005", 0) == 0 &&
                    smsCounts.getOrDefault("TEST006", 0) == 0 &&
                    emailCounts.getOrDefault("TEST001", 0) == 1 &&
                    emailCounts.getOrDefault("TEST002", 0) == 1 &&
                    emailCounts.getOrDefault("TEST003", 0) == 1 &&
                    emailCounts.getOrDefault("TEST004", 0) == 1 &&
                    emailCounts.getOrDefault("TEST005", 0) == 0 &&
                    emailCounts.getOrDefault("TEST006", 0) == 0;

            if (allVerified) {
                TestStepResult result = new TestStepResult("Verify Notification Records", "SUCCESS");
                result.addDetail("All notification records verified successfully");
                return result;
            } else {
                TestStepResult result = new TestStepResult("Verify Notification Records", "FAILED");
                result.addDetail("Notification records verification failed. Unexpected notification counts.");
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Error verifying notification records", e);
            TestStepResult result = new TestStepResult("Verify Notification Records", "FAILED");
            result.addDetail("Failed to verify notification records: " + e.getMessage());
            return result;
        }
    }

    /**
     * Step 9: Verify TS-HST suspension updates.
     * Checks VON and suspended notice records for HST-related suspensions.
     *
     * @return Test step result
     */
    private TestStepResult verifyTsHstSuspension() {
        log.info("🔍 Step 9: Verifying TS-HST suspension updates");
        try {
            Map<String, Map<String, String>> suspensionResults =
                    notificationTestDataHelper.verifyTsHstSuspension();

            Map<String, String> vonSuspensions = suspensionResults.get("von");
            Map<String, String> suspendedNotices = suspensionResults.get("suspendedNotice");

            boolean allVerified =
                    "NULL".equals(vonSuspensions.get("TEST001")) &&
                    "TS".equals(vonSuspensions.get("TEST002")) &&
                    "TS".equals(vonSuspensions.get("TEST003")) &&
                    "TS".equals(vonSuspensions.get("TEST004")) &&
                    !suspendedNotices.containsKey("TEST001") &&
                    suspendedNotices.containsKey("TEST002") &&
                    suspendedNotices.containsKey("TEST003") &&
                    suspendedNotices.containsKey("TEST004");

            if (allVerified) {
                TestStepResult result = new TestStepResult("Verify TS-HST Suspension", "SUCCESS");
                result.addDetail("All TS-HST suspension updates verified successfully");
                return result;
            } else {
                TestStepResult result = new TestStepResult("Verify TS-HST Suspension", "FAILED");
                result.addDetail("TS-HST suspension verification failed. Unexpected suspension states.");
                return result;
            }
        } catch (Exception e) {
            log.error("❌ Error verifying TS-HST suspension updates", e);
            TestStepResult result = new TestStepResult("Verify TS-HST Suspension", "FAILED");
            result.addDetail("Failed to verify TS-HST suspension updates: " + e.getMessage());
            return result;
        }
    }
}
