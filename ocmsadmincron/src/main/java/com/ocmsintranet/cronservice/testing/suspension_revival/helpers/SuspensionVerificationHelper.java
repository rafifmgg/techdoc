package com.ocmsintranet.cronservice.testing.suspension_revival.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Verification Helper for Suspension/Revival Tests
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Contains business logic verification methods used in Step 4
 * Uses emojis for visual feedback: ✅ success, ❌ failed
 */
@Slf4j
@Component
public class SuspensionVerificationHelper {

    /**
     * Verify successful suspension creation
     *
     * @param noticeNo     Notice number to verify
     * @param expectedType Expected suspension type (TS/PS)
     * @param expectedReason Expected reason of suspension
     * @param showDetails  Show detailed verification results
     * @param context      Test context with data from Step 3
     * @return Verification result map
     */
    public Map<String, Object> verifySuspensionCreated(
            String noticeNo,
            String expectedType,
            String expectedReason,
            boolean showDetails,
            SuspensionTestContext context
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("noticeNo", noticeNo);
        result.put("expectedType", expectedType);
        result.put("expectedReason", expectedReason);

        List<String> checks = new ArrayList<>();
        List<String> failedChecks = new ArrayList<>();

        // Get suspension data from context
        List<Map<String, Object>> suspensions = context.getSuspensionData();

        if (suspensions == null || suspensions.isEmpty()) {
            result.put("status", "❌ failed");
            result.put("message", "No suspension data found");
            failedChecks.add("Suspension not created");
            result.put("failed_checks", failedChecks);
            return result;
        }

        // Find suspension for this notice
        Map<String, Object> suspension = findSuspension(suspensions, noticeNo);

        if (suspension == null) {
            result.put("status", "❌ failed");
            result.put("message", "Suspension not found for notice " + noticeNo);
            failedChecks.add("Suspension not found");
            result.put("failed_checks", failedChecks);
            return result;
        }

        // Check suspension type
        String actualType = (String) suspension.get("suspension_type");
        if (expectedType.equals(actualType)) {
            checks.add("✅ Suspension type = " + expectedType);
        } else {
            failedChecks.add("❌ Suspension type: expected " + expectedType + ", got " + actualType);
        }

        // Check reason of suspension
        String actualReason = (String) suspension.get("reason_of_suspension");
        if (expectedReason.equals(actualReason)) {
            checks.add("✅ Reason of suspension = " + expectedReason);
        } else {
            failedChecks.add("❌ Reason: expected " + expectedReason + ", got " + actualReason);
        }

        // Check suspension source
        String actualSource = (String) suspension.get("suspension_source");
        if ("004".equals(actualSource)) {
            checks.add("✅ Suspension source = 004 (OCMS)");
        } else {
            failedChecks.add("❌ Suspension source: expected 004, got " + actualSource);
        }

        // Check officer field
        Object officer = suspension.get("officer_authorising_suspension");
        if (officer != null && !officer.toString().isEmpty()) {
            checks.add("✅ Officer authorising suspension is set");
        } else {
            failedChecks.add("❌ Officer authorising suspension is missing");
        }

        // Check remarks field
        Object remarks = suspension.get("suspension_remarks");
        if (remarks != null && !remarks.toString().isEmpty()) {
            checks.add("✅ Suspension remarks are set");
        } else {
            failedChecks.add("❌ Suspension remarks are missing");
        }

        // Determine overall status
        boolean allPassed = failedChecks.isEmpty();
        result.put("status", allPassed ? "✅ success" : "❌ failed");
        result.put("message", allPassed ?
                "All verifications passed" :
                failedChecks.size() + " verification(s) failed");

        if (showDetails) {
            result.put("passed_checks", checks);
            result.put("failed_checks", failedChecks);
            result.put("suspension_details", suspension);
        }

        result.put("total_checks", checks.size() + failedChecks.size());
        result.put("passed_count", checks.size());
        result.put("failed_count", failedChecks.size());

        return result;
    }

    /**
     * Verify suspension NOT created (rejection flow)
     *
     * @param noticeNo       Notice number to verify
     * @param expectedReason Expected rejection reason
     * @param showDetails    Show detailed verification results
     * @param context        Test context with data from Step 3
     * @return Verification result map
     */
    public Map<String, Object> verifySuspensionNotCreated(
            String noticeNo,
            String expectedReason,
            boolean showDetails,
            SuspensionTestContext context
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("noticeNo", noticeNo);
        result.put("expectedReason", expectedReason);

        List<String> checks = new ArrayList<>();

        // Get suspension data from context
        List<Map<String, Object>> suspensions = context.getSuspensionData();

        // Check that suspension was NOT created
        Map<String, Object> suspension = findSuspension(suspensions, noticeNo);

        if (suspension == null) {
            checks.add("✅ Suspension correctly NOT created");
            result.put("status", "✅ success");
            result.put("message", "Rejection verified successfully");
        } else {
            checks.add("❌ Suspension was created but should have been rejected");
            result.put("status", "❌ failed");
            result.put("message", "Suspension incorrectly created");
        }

        if (showDetails) {
            result.put("checks", checks);
        }

        return result;
    }

    /**
     * Verify revival processed correctly
     *
     * @param noticeNo    Notice number to verify
     * @param shouldBeRevived True if suspension should be revived
     * @param showDetails Show detailed verification results
     * @param context     Test context with data from Step 3
     * @return Verification result map
     */
    public Map<String, Object> verifyRevival(
            String noticeNo,
            boolean shouldBeRevived,
            boolean showDetails,
            SuspensionTestContext context
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("noticeNo", noticeNo);
        result.put("shouldBeRevived", shouldBeRevived);

        List<String> checks = new ArrayList<>();
        List<String> failedChecks = new ArrayList<>();

        // Get suspension data from context
        List<Map<String, Object>> suspensions = context.getSuspensionData();
        Map<String, Object> suspension = findSuspension(suspensions, noticeNo);

        if (suspension == null) {
            result.put("status", "❌ failed");
            result.put("message", "Suspension not found");
            return result;
        }

        // Check date_of_revival field
        Object dateOfRevival = suspension.get("date_of_revival");
        boolean isRevived = (dateOfRevival != null);

        if (shouldBeRevived) {
            if (isRevived) {
                checks.add("✅ Suspension correctly revived");
                result.put("status", "✅ success");
            } else {
                failedChecks.add("❌ Suspension should be revived but date_of_revival is NULL");
                result.put("status", "❌ failed");
            }
        } else {
            if (!isRevived) {
                checks.add("✅ Suspension correctly NOT revived");
                result.put("status", "✅ success");
            } else {
                failedChecks.add("❌ Suspension should NOT be revived but date_of_revival is set");
                result.put("status", "❌ failed");
            }
        }

        result.put("message", failedChecks.isEmpty() ?
                "Revival verification passed" :
                "Revival verification failed");

        if (showDetails) {
            result.put("passed_checks", checks);
            result.put("failed_checks", failedChecks);
            result.put("date_of_revival", dateOfRevival);
        }

        return result;
    }

    /**
     * Find suspension record for given notice number
     *
     * @param suspensions List of suspension records
     * @param noticeNo    Notice number to find
     * @return Suspension record or null if not found
     */
    private Map<String, Object> findSuspension(List<Map<String, Object>> suspensions, String noticeNo) {
        if (suspensions == null) {
            return null;
        }

        return suspensions.stream()
                .filter(s -> noticeNo.equals(s.get("noticeNo")) || noticeNo.equals(s.get("notice_no")))
                .findFirst()
                .orElse(null);
    }
}
