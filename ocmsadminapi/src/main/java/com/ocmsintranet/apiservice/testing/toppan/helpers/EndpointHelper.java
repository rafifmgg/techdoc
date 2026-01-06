package com.ocmsintranet.apiservice.testing.toppan.helpers;

import com.ocmsintranet.apiservice.testing.main.ConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for HTTP endpoint operations
 */
@Component
@Slf4j
public class EndpointHelper {

    private final RestTemplate restTemplate;
    private final ConfigHelper configHelper;
    private final String apiVersion;

    public EndpointHelper(RestTemplate restTemplate,
                          ConfigHelper configHelper,
                          @Value("${api.version}") String apiVersion) {
        this.restTemplate = restTemplate;
        this.configHelper = configHelper;
        this.apiVersion = apiVersion;
    }

    /**
     * POST to an endpoint and extract data
     *
     * @param endpoint Endpoint path (e.g., "/validoffencenoticelist")
     * @param payload Request payload
     * @return Map with keys: success (boolean), total (Object), data (Object), error (String)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> postToEndpoint(String endpoint, Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("total", 0);
        result.put("data", null);

        try {
            // Build endpoint URL using ConfigHelper
            String url = configHelper.buildApiUrl("/" + apiVersion + endpoint);
            log.info("POST request to: {}", url);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Make POST request using exchange method with ParameterizedTypeReference
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Extract total and data from response body
            Map<String, Object> responseBody = response.getBody();
            log.debug("Endpoint {}: Response body keys: {}", endpoint, responseBody != null ? responseBody.keySet() : "null");

            if (responseBody != null) {
                // Extract total
                if (responseBody.containsKey("total")) {
                    result.put("total", responseBody.get("total"));
                }

                // Extract data with nested structure handling
                if (responseBody.containsKey("data")) {
                    Object dataFromResponse = responseBody.get("data");

                    // Handle nested structure: if data is a Map with "data" key, extract the inner array
                    if (dataFromResponse instanceof Map) {
                        Map<?, ?> dataMap = (Map<?, ?>) dataFromResponse;
                        if (dataMap.containsKey("data")) {
                            result.put("data", dataMap.get("data"));
                        } else {
                            result.put("data", dataFromResponse);
                        }
                    } else {
                        // Data is already a List (no nesting)
                        result.put("data", dataFromResponse);
                    }
                }
            }

            result.put("success", true);
            log.info("Endpoint {} completed successfully. Total: {}", endpoint, result.get("total"));

        } catch (Exception e) {
            log.error("Endpoint {} failed with exception: {}", endpoint, e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
