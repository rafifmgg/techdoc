package com.ocmsintranet.apiservice.testing.furnish_manual.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.BulkFurnishRequest;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.ManualFurnishRequest;
import com.ocmsintranet.apiservice.workflows.furnish.manual.ManualFurnishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class EndpointHelper {

    private final ManualFurnishService manualFurnishService;

    /**
     * Trigger manual furnish endpoint for single notice
     */
    @SuppressWarnings("unchecked")
    public void triggerManualFurnish(TestContext context, Map<String, Object> furnishRequestData) {
        log.info("Triggering manual furnish for notice: {}", furnishRequestData.get("noticeNo"));

        try {
            // Build ManualFurnishRequest from scenario data
            ManualFurnishRequest request = ManualFurnishRequest.builder()
                    .noticeNo((String) furnishRequestData.get("noticeNo"))
                    .officerId((String) furnishRequestData.getOrDefault("officerId", "TEST_OFFICER"))
                    .ownerDriverIndicator((String) furnishRequestData.getOrDefault("ownerDriverIndicator", "H"))
                    .idType((String) furnishRequestData.getOrDefault("idType", "N"))
                    .idNo((String) furnishRequestData.get("idNo"))
                    .name((String) furnishRequestData.get("name"))
                    .blkNo((String) furnishRequestData.getOrDefault("blkNo", "1"))
                    .floor((String) furnishRequestData.get("floor"))
                    .streetName((String) furnishRequestData.getOrDefault("streetName", "Test Street"))
                    .unitNo((String) furnishRequestData.get("unitNo"))
                    .bldgName((String) furnishRequestData.get("bldgName"))
                    .postalCode((String) furnishRequestData.get("postalCode"))
                    .telCode((String) furnishRequestData.get("telCode"))
                    .telNo((String) furnishRequestData.get("telNo"))
                    .emailAddr((String) furnishRequestData.get("emailAddr"))
                    .overwriteExisting(Boolean.TRUE.equals(furnishRequestData.get("overwrite")))
                    .remarks((String) furnishRequestData.get("remarks"))
                    .build();

            // Call actual service
            var result = manualFurnishService.handleManualFurnish(request);

            log.info("Manual furnish result: {}", result);

            // Store result in context for verification
            context.getVerificationData().put("manualFurnishResult", result);

        } catch (Exception e) {
            log.error("Failed to trigger manual furnish: {}", e.getMessage(), e);
            throw new RuntimeException("Manual furnish failed: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger bulk furnish endpoint for multiple notices
     */
    @SuppressWarnings("unchecked")
    public void triggerBulkFurnish(TestContext context, List<Map<String, Object>> furnishRequests) {
        log.info("Triggering bulk furnish for {} notices", furnishRequests.size());

        try {
            // Extract notice numbers
            List<String> noticeNumbers = new ArrayList<>();
            for (Map<String, Object> req : furnishRequests) {
                noticeNumbers.add((String) req.get("noticeNo"));
            }

            // Use first request for common person details
            Map<String, Object> firstRequest = furnishRequests.get(0);

            // Build BulkFurnishRequest
            BulkFurnishRequest request = BulkFurnishRequest.builder()
                    .noticeNos(noticeNumbers)
                    .officerId((String) firstRequest.getOrDefault("officerId", "TEST_OFFICER"))
                    .ownerDriverIndicator((String) firstRequest.getOrDefault("ownerDriverIndicator", "H"))
                    .idType((String) firstRequest.getOrDefault("idType", "N"))
                    .idNo((String) firstRequest.get("idNo"))
                    .name((String) firstRequest.get("name"))
                    .blkNo((String) firstRequest.getOrDefault("blkNo", "1"))
                    .floor((String) firstRequest.get("floor"))
                    .streetName((String) firstRequest.getOrDefault("streetName", "Test Street"))
                    .unitNo((String) firstRequest.get("unitNo"))
                    .bldgName((String) firstRequest.get("bldgName"))
                    .postalCode((String) firstRequest.get("postalCode"))
                    .telCode((String) firstRequest.get("telCode"))
                    .telNo((String) firstRequest.get("telNo"))
                    .emailAddr((String) firstRequest.get("emailAddr"))
                    .overwriteExisting(Boolean.TRUE.equals(firstRequest.get("overwrite")))
                    .remarks((String) firstRequest.get("remarks"))
                    .build();

            // Call actual service
            var result = manualFurnishService.handleBulkFurnish(request);

            log.info("Bulk furnish result: {}", result);

            // Store result in context for verification
            context.getVerificationData().put("bulkFurnishResult", result);
            context.getVerificationData().put("noticeCount", noticeNumbers.size());

        } catch (Exception e) {
            log.error("Failed to trigger bulk furnish: {}", e.getMessage(), e);
            throw new RuntimeException("Bulk furnish failed: " + e.getMessage(), e);
        }
    }
}
