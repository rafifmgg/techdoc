package com.ocmsintranet.cronservice.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for calling Suspension/Revival API endpoints
 * Provides methods to apply suspension and revival via the internal API instead of direct DB access
 *
 * Uses direct HTTP calls to localhost for same-App-Service communication (no APIM overhead)
 * Includes built-in retry logic:
 * - Exponential backoff with jitter
 * - Configurable retry attempts (default: 3)
 */
@Slf4j
@Component
public class SuspensionApiClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ocms.api.internal.baseurl:http://localhost:8085}")
    private String internalApiBaseUrl;

    @Value("${ocms.api.suspension.endpoint:/ocms/v1/suspension/apply-suspension}")
    private String suspensionEndpoint;

    @Value("${ocms.api.revival.endpoint:/ocms/v1/suspension/apply-revival}")
    private String revivalEndpoint;

    @Value("${ocms.api.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${ocms.api.retry.initialDelayMs:2000}")
    private long initialDelayMs;

    @Value("${ocms.api.retry.maxDelayMs:30000}")
    private long maxDelayMs;

    /**
     * Apply suspension to notice(s) via API
     * Calls POST /apply-suspension endpoint via localhost
     *
     * @param noticeNos List of notice numbers to suspend
     * @param suspensionType Suspension type (TS or PS)
     * @param reasonOfSuspension Reason code (ROV, SYS, RIP, RP2, NRO, OLD, DBB, ANS, FOR)
     * @param suspensionRemarks Remarks for the suspension
     * @param officerAuthorisingSuspension Officer ID authorizing the suspension
     * @param suspensionSource Source code (004=OCMS, 005=PLUS)
     * @param caseNo Case number (optional)
     * @param daysToRevive Days until revival (optional, for TS types)
     * @return Response from API containing results for each notice
     */
    public List<Map<String, Object>> applySuspension(
            List<String> noticeNos,
            String suspensionType,
            String reasonOfSuspension,
            String suspensionRemarks,
            String officerAuthorisingSuspension,
            String suspensionSource,
            String caseNo,
            Integer daysToRevive) {

        // Build request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("noticeNo", noticeNos);
        requestBody.put("suspensionType", suspensionType);
        requestBody.put("reasonOfSuspension", reasonOfSuspension);
        requestBody.put("suspensionRemarks", suspensionRemarks);
        requestBody.put("officerAuthorisingSuspension", officerAuthorisingSuspension);
        requestBody.put("suspensionSource", suspensionSource);

        if (caseNo != null && !caseNo.trim().isEmpty()) {
            requestBody.put("caseNo", caseNo);
        }

        if (daysToRevive != null && daysToRevive > 0) {
            requestBody.put("daysToRevive", daysToRevive);
        }

        log.info("Calling internal suspension API for {} notice(s) with type={}, reason={}",
                noticeNos.size(), suspensionType, reasonOfSuspension);
        log.debug("Suspension API request: {}", requestBody);

        // Call with retry logic
        return callApiWithRetry(
                internalApiBaseUrl + suspensionEndpoint,
                requestBody,
                noticeNos
        );
    }

    /**
     * Apply suspension to a single notice via API
     * Convenience method that wraps single notice into a list
     *
     * @param noticeNo Notice number to suspend
     * @param suspensionType Suspension type (TS or PS)
     * @param reasonOfSuspension Reason code
     * @param suspensionRemarks Remarks for the suspension
     * @param officerAuthorisingSuspension Officer ID authorizing the suspension
     * @param suspensionSource Source code (004=OCMS, 005=PLUS)
     * @param caseNo Case number (optional)
     * @param daysToRevive Days until revival (optional, for TS types)
     * @return Response for the single notice
     */
    public Map<String, Object> applySuspensionSingle(
            String noticeNo,
            String suspensionType,
            String reasonOfSuspension,
            String suspensionRemarks,
            String officerAuthorisingSuspension,
            String suspensionSource,
            String caseNo,
            Integer daysToRevive) {

        List<String> noticeNos = new ArrayList<>();
        noticeNos.add(noticeNo);

        List<Map<String, Object>> results = applySuspension(
                noticeNos, suspensionType, reasonOfSuspension, suspensionRemarks,
                officerAuthorisingSuspension, suspensionSource, caseNo, daysToRevive);

        // Return first result
        return results.isEmpty() ? createErrorResponse(noticeNo, "No response from API") : results.get(0);
    }

    /**
     * Apply revival to notice(s) via API
     * Calls POST /apply-revival endpoint via localhost
     *
     * @param noticeNos List of notice numbers to revive
     * @param revivalRemarks Remarks for the revival
     * @param officerAuthorisingRevival Officer ID authorizing the revival
     * @param revivalSource Source code (004=OCMS, 005=PLUS)
     * @return Response from API containing results for each notice
     */
    public List<Map<String, Object>> applyRevival(
            List<String> noticeNos,
            String revivalRemarks,
            String officerAuthorisingRevival,
            String revivalSource) {

        // Build request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("noticeNo", noticeNos);
        requestBody.put("revivalRemarks", revivalRemarks);
        requestBody.put("officerAuthorisingRevival", officerAuthorisingRevival);
        requestBody.put("revivalSource", revivalSource);

        log.info("Calling internal revival API for {} notice(s)", noticeNos.size());
        log.debug("Revival API request: {}", requestBody);

        // Call with retry logic
        return callApiWithRetry(
                internalApiBaseUrl + revivalEndpoint,
                requestBody,
                noticeNos
        );
    }

    /**
     * Apply revival to a single notice via API
     * Convenience method that wraps single notice into a list
     *
     * @param noticeNo Notice number to revive
     * @param revivalRemarks Remarks for the revival
     * @param officerAuthorisingRevival Officer ID authorizing the revival
     * @param revivalSource Source code (004=OCMS, 005=PLUS)
     * @return Response for the single notice
     */
    public Map<String, Object> applyRevivalSingle(
            String noticeNo,
            String revivalRemarks,
            String officerAuthorisingRevival,
            String revivalSource) {

        List<String> noticeNos = new ArrayList<>();
        noticeNos.add(noticeNo);

        List<Map<String, Object>> results = applyRevival(
                noticeNos, revivalRemarks, officerAuthorisingRevival, revivalSource);

        // Return first result
        return results.isEmpty() ? createErrorResponse(noticeNo, "No response from API") : results.get(0);
    }

    /**
     * Create an error response map for a failed API call
     *
     * @param noticeNo Notice number
     * @param errorMessage Error message
     * @return Error response map
     */
    private Map<String, Object> createErrorResponse(String noticeNo, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("noticeNo", noticeNo);
        error.put("appCode", "OCMS-9999");
        error.put("message", errorMessage);
        return error;
    }

    /**
     * Check if API response indicates success
     * Success is determined by absence of appCode or appCode = "OCMS-0000"
     *
     * @param response Response map from API
     * @return true if successful, false if error
     */
    public boolean isSuccess(Map<String, Object> response) {
        if (response == null) {
            return false;
        }

        String appCode = (String) response.get("appCode");

        // No appCode = success
        if (appCode == null) {
            return true;
        }

        // OCMS-0000 = success
        return "OCMS-0000".equals(appCode);
    }

    /**
     * Extract error message from API response
     *
     * @param response Response map from API
     * @return Error message or null if no error
     */
    public String getErrorMessage(Map<String, Object> response) {
        if (response == null) {
            return "No response from API";
        }

        return (String) response.get("message");
    }

    /**
     * Call API with exponential backoff retry logic
     *
     * @param url Full API URL
     * @param requestBody Request payload
     * @param noticeNos List of notice numbers (for error response)
     * @return API response
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callApiWithRetry(
            String url,
            Map<String, Object> requestBody,
            List<String> noticeNos) {

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetryAttempts) {
            try {
                if (retryCount > 0) {
                    log.info("Retry attempt {} of {} for API call to {}", retryCount, maxRetryAttempts, url);
                }

                // Setup headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Create request entity
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                // Make REST call
                ResponseEntity<List> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        List.class
                );

                log.info("API call completed successfully to {}", url);
                return (List<Map<String, Object>>) response.getBody();

            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount <= maxRetryAttempts) {
                    // Calculate exponential backoff delay with jitter
                    long delayMs = calculateBackoffDelay(retryCount);

                    log.warn("API call failed, retrying in {} ms. Retry {}/{}. Error: {}",
                            delayMs, retryCount, maxRetryAttempts, e.getMessage());

                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted", ie);
                        break;
                    }
                } else {
                    log.error("API call to {} failed after {} retries: {}", url, maxRetryAttempts, e.getMessage());
                }
            }
        }

        // All retries failed - return error response for all notices
        log.error("Failed to call API after {} attempts: {}", maxRetryAttempts, lastException.getMessage());
        List<Map<String, Object>> errorResponse = new ArrayList<>();
        for (String noticeNo : noticeNos) {
            Map<String, Object> error = new HashMap<>();
            error.put("noticeNo", noticeNo);
            error.put("appCode", "OCMS-9999");
            error.put("message", "API call failed: " + lastException.getMessage());
            errorResponse.add(error);
        }
        return errorResponse;
    }

    /**
     * Calculate exponential backoff delay with jitter
     *
     * @param retryCount Current retry attempt (starting from 1)
     * @return Delay in milliseconds
     */
    private long calculateBackoffDelay(int retryCount) {
        // Calculate exponential backoff
        long expBackoff = initialDelayMs * (long) Math.pow(2, (double) (retryCount - 1));

        // Add jitter (between 0% and 15% of the calculated delay)
        double jitterPct = Math.random() * 0.15;
        long jitter = (long) (expBackoff * jitterPct);

        // Calculate total delay with jitter
        long delay = expBackoff + jitter;

        // Ensure delay doesn't exceed maximum
        return Math.min(delay, maxDelayMs);
    }
}
