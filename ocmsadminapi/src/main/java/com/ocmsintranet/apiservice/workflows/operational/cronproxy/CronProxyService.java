package com.ocmsintranet.apiservice.workflows.operational.cronproxy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ocmsintranet.apiservice.utilities.HttpUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for proxying requests to various cron jobs
 * Acts as a bridge between adminui and cron endpoints
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CronProxyService {

    private final RestTemplate restTemplate;
    
    // Type reference for Map<String, Object> responses
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = 
            new ParameterizedTypeReference<Map<String, Object>>() {};
    
    @Value("${cron.base.url}")
    private String cronBaseUrl;
    
    /**
     * Trigger the LTA enquiry process in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerLtaEnquiry() {
        String url = cronBaseUrl + "/lta/upload";
        log.info("Forwarding LTA enquiry trigger request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering LTA enquiry: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger LTA enquiry: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the LTA result process in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerLtaResult() {
        String url = cronBaseUrl + "/lta/download/manual";
        log.info("Forwarding LTA result trigger request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering LTA result: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger LTA result: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the MHA enquiry process in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerMhaEnquiry() {
        String url = cronBaseUrl + "/mha/upload";
        log.info("Forwarding MHA enquiry trigger request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering MHA enquiry: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger MHA enquiry: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the MHA result process in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerMhaResult() {
        String url = cronBaseUrl + "/mha/download/execute";
        log.info("Forwarding MHA result trigger request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering MHA result: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger MHA result: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Generate daily job execution report in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> generateDailyJobReport() {
        String url = cronBaseUrl + "/job-execution-report/generate-daily-report";
        log.info("Forwarding job report generation request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error generating job report: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate job report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Execute ENA reminder in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> executeEnaReminder() {
        String url = cronBaseUrl + "/notification-sms-email/execute";
        log.info("Forwarding ENA reminder execution request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error executing ENA reminder: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to execute ENA reminder: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Retry ENA reminder in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> retryEnaReminder() {
        String url = cronBaseUrl + "/notification-sms-email/retry";
        log.info("Forwarding ENA reminder retry request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error retrying ENA reminder: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retry ENA reminder: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the LTA daily report generation in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerLtaReportDaily() {
        String url = cronBaseUrl + "/lta/report/generate";
        log.info("Forwarding LTA daily report generation request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering LTA daily report: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger LTA daily report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the MHA daily report generation in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerMhaReportDaily() {
        String url = cronBaseUrl + "/mha/report/execute";
        log.info("Forwarding MHA daily report generation request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering MHA daily report: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger MHA daily report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the eNotification report generation in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerEnotifReportDaily() {
        String url = cronBaseUrl + "/enotif/report/execute";
        log.info("Forwarding eNotification report generation request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering eNotification report: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger eNotification report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the Datahive contact info report generation in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerDhContactInfoReportDaily() {
        String url = cronBaseUrl + "/dhcontactinfo/report/execute";
        log.info("Forwarding Datahive contact info report generation request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering Datahive contact info report: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger Datahive contact info report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Trigger the Toppan download process in the cron tier
     * Downloads and processes response files from Toppan
     *
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerToppanDownload() {
        String url = cronBaseUrl + "/toppan/download-responses";
        log.info("Forwarding Toppan download trigger request to: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    MAP_TYPE_REF);

            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering Toppan download: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger Toppan download: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Trigger the Toppan letters generation process in the cron tier
     * Processes all 6 stages (RD1, RD2, RR3, DN1, DN2, DR3) in sequence
     *
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerToppanUpload() {
        String url = cronBaseUrl + "/toppan/generate-letters";
        log.info("Forwarding Toppan upload trigger request to: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    MAP_TYPE_REF);

            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering Toppan upload: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger Toppan upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Trigger the DataHive UEN/FIN sync process in the cron tier
     * 
     * @return Response from the cron tier
     */
    public ResponseEntity<Map<String, Object>> triggerDataHiveUenFin() {
        String url = cronBaseUrl + "/datahive-uen-fin/execute-sync";
        log.info("Forwarding DataHive UEN/FIN sync trigger request to: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(new HashMap<>(), headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    MAP_TYPE_REF);
            
            log.info("Received response from cron tier: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error triggering DataHive UEN/FIN sync: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger DataHive UEN/FIN sync: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
