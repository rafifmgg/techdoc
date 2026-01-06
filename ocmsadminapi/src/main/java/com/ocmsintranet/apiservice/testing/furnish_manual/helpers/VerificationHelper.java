package com.ocmsintranet.apiservice.testing.furnish_manual.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.testing.furnish_manual.dto.ManualFurnishTestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationHelper {

    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository addressRepository;

    /**
     * Verify that owner/driver record was created
     */
    public void verifyOwnerDriverCreated(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying owner/driver record created");

        try {
            String noticeNo = context.getNoticeNo();
            var records = ownerDriverRepository.findByNoticeNo(noticeNo);

            boolean passed = !records.isEmpty();
            String actual = passed ? "Record created" : "No record found";

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Owner/Driver Record Created")
                            .passed(passed)
                            .expected("Record should exist for notice " + noticeNo)
                            .actual(actual)
                            .message(passed ? "✓ Record created successfully" : "✗ No record found")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Owner/Driver Record Created", e.getMessage());
        }
    }

    /**
     * Verify that owner/driver record was updated
     */
    public void verifyOwnerDriverUpdated(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying owner/driver record updated");

        try {
            String noticeNo = context.getNoticeNo();
            var records = ownerDriverRepository.findByNoticeNo(noticeNo);

            boolean passed = !records.isEmpty();
            String actual = passed ? "Record updated" : "No record found";

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Owner/Driver Record Updated")
                            .passed(passed)
                            .expected("Record should be updated for notice " + noticeNo)
                            .actual(actual)
                            .message(passed ? "✓ Record updated successfully" : "✗ No record found")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Owner/Driver Record Updated", e.getMessage());
        }
    }

    /**
     * Verify current offender indicator
     */
    public void verifyCurrentOffenderIndicator(TestContext context, ManualFurnishTestResponse response,
                                               String expectedIndicator) {
        log.info("Verifying current offender indicator: {}", expectedIndicator);

        try {
            String noticeNo = context.getNoticeNo();
            var records = ownerDriverRepository.findByNoticeNo(noticeNo);

            if (records.isEmpty()) {
                addFailedVerification(response, "Current Offender Indicator", "No record found");
                return;
            }

            var record = records.get(0);
            String actualIndicator = record.getOffenderIndicator();
            boolean passed = expectedIndicator.equals(actualIndicator);

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Current Offender Indicator")
                            .passed(passed)
                            .expected(expectedIndicator)
                            .actual(actualIndicator)
                            .message(passed ? "✓ Indicator matches" : "✗ Indicator mismatch")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Current Offender Indicator", e.getMessage());
        }
    }

    /**
     * Verify that address record was created
     */
    public void verifyAddressCreated(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying address record created");

        try {
            String noticeNo = context.getNoticeNo();
            var ownerDriverRecords = ownerDriverRepository.findByNoticeNo(noticeNo);

            if (ownerDriverRecords.isEmpty()) {
                addFailedVerification(response, "Address Record Created", "No owner/driver record found");
                return;
            }

            var ownerDriver = ownerDriverRecords.get(0);
            var addressRecords = addressRepository.findByNoticeNoAndIdNo(
                    noticeNo,
                    ownerDriver.getIdNo()
            );

            boolean passed = !addressRecords.isEmpty();
            String actual = passed ? "Address created" : "No address found";

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Address Record Created")
                            .passed(passed)
                            .expected("Address should exist for notice " + noticeNo)
                            .actual(actual)
                            .message(passed ? "✓ Address created successfully" : "✗ No address found")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Address Record Created", e.getMessage());
        }
    }

    /**
     * Verify that address record was updated
     */
    public void verifyAddressUpdated(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying address record updated");

        try {
            String noticeNo = context.getNoticeNo();
            var ownerDriverRecords = ownerDriverRepository.findByNoticeNo(noticeNo);

            if (ownerDriverRecords.isEmpty()) {
                addFailedVerification(response, "Address Record Updated", "No owner/driver record found");
                return;
            }

            var ownerDriver = ownerDriverRecords.get(0);
            var addressRecords = addressRepository.findByNoticeNoAndIdNo(
                    noticeNo,
                    ownerDriver.getIdNo()
            );

            boolean passed = !addressRecords.isEmpty();
            String actual = passed ? "Address updated" : "No address found";

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Address Record Updated")
                            .passed(passed)
                            .expected("Address should be updated for notice " + noticeNo)
                            .actual(actual)
                            .message(passed ? "✓ Address updated successfully" : "✗ No address found")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Address Record Updated", e.getMessage());
        }
    }

    /**
     * Verify bulk furnish count
     */
    public void verifyBulkFurnishCount(TestContext context, ManualFurnishTestResponse response,
                                       Integer expectedCount) {
        log.info("Verifying bulk furnish count: {}", expectedCount);

        try {
            Integer actualCount = (Integer) context.getVerificationData().get("noticeCount");

            boolean passed = expectedCount.equals(actualCount);

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Bulk Furnish Count")
                            .passed(passed)
                            .expected(expectedCount + " records")
                            .actual(actualCount + " records")
                            .message(passed ? "✓ Count matches" : "✗ Count mismatch")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Bulk Furnish Count", e.getMessage());
        }
    }

    /**
     * Verify furnishable stage check
     */
    public void verifyFurnishableStage(TestContext context, ManualFurnishTestResponse response) {
        log.info("Verifying furnishable stage");

        try {
            var result = context.getVerificationData().get("manualFurnishResult");
            boolean passed = result != null;

            ManualFurnishTestResponse.VerificationResult verification =
                    ManualFurnishTestResponse.VerificationResult.builder()
                            .checkName("Furnishable Stage Check")
                            .passed(passed)
                            .expected("Notice should be furnishable")
                            .actual(passed ? "Notice is furnishable" : "Notice is not furnishable")
                            .message(passed ? "✓ Stage check passed" : "✗ Stage check failed")
                            .build();

            response.getVerifications().add(verification);

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            addFailedVerification(response, "Furnishable Stage Check", e.getMessage());
        }
    }

    private void addFailedVerification(ManualFurnishTestResponse response, String checkName, String errorMessage) {
        ManualFurnishTestResponse.VerificationResult verification =
                ManualFurnishTestResponse.VerificationResult.builder()
                        .checkName(checkName)
                        .passed(false)
                        .expected("Verification should pass")
                        .actual("Error: " + errorMessage)
                        .message("✗ Verification failed")
                        .build();

        response.getVerifications().add(verification);
    }
}
