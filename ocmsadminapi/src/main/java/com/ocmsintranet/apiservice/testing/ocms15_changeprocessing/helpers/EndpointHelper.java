package com.ocmsintranet.apiservice.testing.ocms15_changeprocessing.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint Helper - Handles all HTTP API calls for OCMS 15 testing
 *
 * Responsibilities:
 * - Call /staff-create-notice (Step 0)
 * - Call /change-processing-stage (Step 2 - OCMS manual flow)
 * - Call /external/plus/change-processing-stage (Step 2 - PLUS integration)
 * - Call /generate-report-change-processing-stage (Step 4 - Excel report)
 * - Download and verify Excel report
 */
@Slf4j
@Component
public class EndpointHelper {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "http://localhost:8080/v1";

    /**
     * Step 0: Create a notice via /staff-create-notice
     *
     * @param noticeData Raw JSON string of OffenceNoticeDto
     * @return Map with success flag and response data
     */
    public Map<String, Object> createNotice(String noticeData) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = BASE_URL + "/staff-create-notice";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(noticeData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("statusCode", response.getStatusCode().value());
            result.put("response", response.getBody());

            log.info("Created notice via /staff-create-notice: {}", response.getStatusCode());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error creating notice: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * Step 2: Call OCMS manual change processing stage API
     *
     * @param noticeNo Notice number
     * @param newStage New processing stage
     * @param remarks Optional remarks
     * @return Map with success flag and response data
     */
    public Map<String, Object> callOcmsChangeProcessingStage(String noticeNo, String newStage, String remarks) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = BASE_URL + "/change-processing-stage";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("noticeNo", noticeNo);
            requestBody.put("newProcessingStage", newStage);
            if (remarks != null && !remarks.isEmpty()) {
                requestBody.put("remarks", remarks);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("statusCode", response.getStatusCode().value());
            result.put("response", response.getBody());

            log.info("Called OCMS change-processing-stage for {}: {}", noticeNo, response.getStatusCode());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error calling OCMS API for {}: {}", noticeNo, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Step 2: Call PLUS integration change processing stage API
     *
     * @param noticeNo Notice number
     * @param newStage New processing stage
     * @return Map with success flag and response data
     */
    public Map<String, Object> callPlusChangeProcessingStage(String noticeNo, String newStage) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = BASE_URL + "/external/plus/change-processing-stage";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("noticeNo", noticeNo);
            requestBody.put("newProcessingStage", newStage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("statusCode", response.getStatusCode().value());
            result.put("response", response.getBody());

            log.info("Called PLUS change-processing-stage for {}: {}", noticeNo, response.getStatusCode());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error calling PLUS API for {}: {}", noticeNo, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Step 4: Generate and download Excel report
     *
     * @param startDate Start date (yyyy-MM-dd)
     * @param endDate End date (yyyy-MM-dd)
     * @return Map with success flag and file data
     */
    public Map<String, Object> downloadChangeProcessingReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = BASE_URL + "/generate-report-change-processing-stage?startDate=" + startDate + "&endDate=" + endDate;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                byte[].class
            );

            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("statusCode", response.getStatusCode().value());
            result.put("fileData", response.getBody());
            result.put("contentType", response.getHeaders().getContentType());
            result.put("fileName", extractFileName(response.getHeaders()));

            log.info("Downloaded change processing report: {} bytes", response.getBody() != null ? response.getBody().length : 0);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Error downloading report: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * Extract filename from Content-Disposition header
     */
    private String extractFileName(HttpHeaders headers) {
        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            return contentDisposition.substring(contentDisposition.indexOf("filename=") + 9)
                .replace("\"", "");
        }
        return "change_processing_report.xlsx";
    }

    /**
     * Parse JSON response to Map
     */
    public Map<String, Object> parseResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse response as JSON: {}", e.getMessage());
            return Map.of("raw", jsonResponse);
        }
    }
}
