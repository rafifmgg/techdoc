package com.ocmsintranet.cronservice.testing.suspension_revival.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint Helper for Suspension/Revival Tests
 *
 * Follows OCMS Testing Framework pattern from TESTING_FRAMEWORK_GUIDE.md
 *
 * Provides methods to call internal APIs for fetching verification data
 * Similar to testing/toppan/helpers/EndpointHelper.java
 */
@Slf4j
@Component
public class SuspensionEndpointHelper {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ocms.api.internal.baseurl:http://localhost:8085}")
    private String apiBaseUrl;

    @Value("${ocms.api.version:v1}")
    private String apiVersion;

    /**
     * Post to an API endpoint
     *
     * @param endpoint API endpoint path (e.g., "/suspendednoticelist")
     * @param payload  Request payload
     * @return Response map with {success, total, data, error}
     */
    public Map<String, Object> postToEndpoint(String endpoint, Map<String, Object> payload) {
        try {
            String url = buildUrl(endpoint);
            log.debug("POST to {}: {}", url, payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getStatusCode().is2xxSuccessful());
            result.put("statusCode", response.getStatusCode().value());

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                result.put("data", body.get("data"));
                result.put("total", body.get("total"));
            }

            return result;

        } catch (Exception e) {
            log.error("Error calling endpoint {}: {}", endpoint, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Fetch suspension records by notice numbers
     *
     * @param noticeNumbers List of notice numbers
     * @return API response map
     */
    public Map<String, Object> fetchSuspensions(List<String> noticeNumbers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);
        payload.put("noticeNo[$in]", String.join(",", noticeNumbers));

        return postToEndpoint("/ocms/" + apiVersion + "/suspendednoticelist", payload);
    }

    /**
     * Fetch VON records by notice numbers
     *
     * @param noticeNumbers List of notice numbers
     * @return API response map
     */
    public Map<String, Object> fetchVonRecords(List<String> noticeNumbers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);
        payload.put("noticeNo[$in]", String.join(",", noticeNumbers));

        return postToEndpoint("/ocms/" + apiVersion + "/validoffencenoticelist", payload);
    }

    /**
     * Fetch LTA file records by vehicle numbers
     *
     * @param vehicleNumbers List of vehicle numbers
     * @return API response map
     */
    public Map<String, Object> fetchLtaFiles(List<String> vehicleNumbers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);
        payload.put("vehicleNo[$in]", String.join(",", vehicleNumbers));

        return postToEndpoint("/ocms/" + apiVersion + "/ltafilelist", payload);
    }

    /**
     * Fetch MHA file records by NRICs
     *
     * @param nrics List of NRICs
     * @return API response map
     */
    public Map<String, Object> fetchMhaFiles(List<String> nrics) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("$limit", 1000);
        payload.put("nric[$in]", String.join(",", nrics));

        return postToEndpoint("/ocms/" + apiVersion + "/mhafilelist", payload);
    }

    /**
     * Build full URL from endpoint path
     *
     * @param endpoint Endpoint path
     * @return Full URL
     */
    private String buildUrl(String endpoint) {
        String url = apiBaseUrl;

        // Remove trailing slash from base URL
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Add leading slash to endpoint if missing
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }

        return url + endpoint;
    }

    /**
     * Get API base URL
     *
     * @return API base URL
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
}
