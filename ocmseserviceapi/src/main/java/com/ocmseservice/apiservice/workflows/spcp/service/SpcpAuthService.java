package com.ocmseservice.apiservice.workflows.spcp.service;

import com.ocmseservice.apiservice.workflows.spcp.model.*;
import com.ocmseservice.apiservice.workflows.spcp.util.AppCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for SingPass/CorpPass authentication
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpcpAuthService {
    
    private final RestTemplate restTemplate;
    
    @Value("${spcp.service.url:https://singpasscorppass.azurewebsites.net}")
    private String spcpBaseUrl;
    
    @Value("${spcp.api.key:default-api-key}")
    private String spcpApiKey;

    /**
     * Service creates Transaction ID by calling SPCP web-service
     * Calls the SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/createAppTxnId
     *
     * @param request AuthAppTxnIdRequest containing sessionId and appId
     * @return AuthAppTxnIdResponse
     */
    public AuthAppTxnIdResponse createAppTxnId(AuthAppTxnIdRequest request) {
        log.info("Creating application transaction ID for session: {}", request.getSessionId());
        
        AuthAppTxnIdResponse response = new AuthAppTxnIdResponse();
        
        try {
            // Create a new RestTemplate instance
            RestTemplate restTemplate = new RestTemplate();
            
            // Setting Header with APIM key
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            String apimKey = AppCacheUtil.getValue(AppCacheUtil.APIM_SPMS_KEY);
            if (apimKey == null) {
                log.warn("APIM key not found in cache, using default or empty value");
                // You might want to set a default key or handle this case appropriately
                // apimKey = "your-default-key-for-testing";
            } else {
                httpHeaders.set(AppCacheUtil.APIM_HEADER, apimKey);
                log.info("Using APIM key for authentication request");
            }
            
            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("sessionId", request.getSessionId());
            requestBody.put("appId", request.getAppId());

            // Setting Entity object with header values and body
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

            // Make the request to SIT endpoint
            String url = spcpBaseUrl + "/spcpDS/spcp/createAppTxnId";
            log.info("Calling external API: {}", url);
            
            ResponseEntity<AuthAppTxnIdResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AuthAppTxnIdResponse.class);
            
            response = responseEntity.getBody();
            
            // Ensure response is not null
            if (response == null) {
                response = new AuthAppTxnIdResponse();
                response.setResponseCode("500");
                response.setResponseMsg("Null response received from API");
                log.error("Null response received from createAppTxnId API");
            } else {
                log.info("Response from createAppTxnId API: {}", response);
            }
        } catch (Exception e) {
            String message = "Failed to create application transaction ID. Please try again.";
            log.error(message, e);
            // Ensure response is not null before setting properties
            if (response == null) {
                response = new AuthAppTxnIdResponse();
            }
            response.setResponseCode("500");
            response.setResponseMsg(message);
        }
        
        return response;
    }

    /**
     * Get authentication response from SPCP service
     * Calls the SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/getAuthResponse
     *
     * @param request SpcpAuthRequest containing appId and authTxnId
     * @return SpcpAuthResponse
     */
    public SpcpAuthResponse getAuthResponse(SpcpAuthRequest request) {
        log.info("Getting authentication response for authTxnId: {}", request.getAuthTxnId());
        
        SpcpAuthResponse response = new SpcpAuthResponse();
        
        try {
            // Create a new RestTemplate instance
            RestTemplate restTemplate = new RestTemplate();
            
            // Setting Header with APIM key
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            String apimKey = AppCacheUtil.getValue(AppCacheUtil.APIM_SPMS_KEY);
            if (apimKey == null) {
                log.warn("APIM key not found in cache, using default or empty value");
                // You might want to set a default key or handle this case appropriately
                // apimKey = "your-default-key-for-testing";
            } else {
                httpHeaders.set(AppCacheUtil.APIM_HEADER, apimKey);
                log.info("Using APIM key for authentication response request");
            }
            
            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("appId", request.getAppId());
            requestBody.put("authTxnId", request.getAuthTxnId());

            // Setting Entity object with header values and body
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            
            // Make the request to SIT endpoint
            String url = spcpBaseUrl + "/spcpDS/spcp/getAuthResponse";
            log.info("Calling external API: {}", url);
            
            ResponseEntity<SpcpAuthResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    SpcpAuthResponse.class);
            
            response = responseEntity.getBody();
            
            // Ensure response is not null
            if (response == null) {
                response = new SpcpAuthResponse();
                response.setResponseCode("500");
                response.setResponseMsg("Null response received from API");
                log.error("Null response received from getAuthResponse API");
            } else {
                log.info("Response from getAuthResponse API: {}", response);
            }
        } catch (Exception e) {
            String message = "Failed to get authentication response. Please try again.";
            log.error(message, e);
            // Ensure response is not null before setting properties
            if (response == null) {
                response = new SpcpAuthResponse();
            }
            response.setResponseCode("500");
            response.setResponseMsg(message);
        }
        
        return response;
    }
    
    /**
     * Get MyInfo data from SPCP service
     * Calls the SIT endpoint: https://singpasscorppass.azurewebsites.net/spcpDS/myinfo/getMyInfoData
     * 
     * @param request MyInfoRequest containing appId, nric, and txnNo
     * @return MyInfoResponse with user's personal information
     */
    public MyInfoResponse getMyInfoData(MyInfoRequest request) {
        MyInfoResponse response = null;
        
        try {
            // Create headers
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            // Add APIM subscription key if available
            String apimKey = AppCacheUtil.getValue(AppCacheUtil.APIM_SPMS_KEY);
            if (apimKey != null && !apimKey.isEmpty()) {
                httpHeaders.set(AppCacheUtil.APIM_HEADER, apimKey);
            } else {
                log.warn("APIM subscription key not found in cache. Using default key.");
                httpHeaders.set(AppCacheUtil.APIM_HEADER, spcpApiKey);
            }
            
            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("appId", request.getAppId());
            requestBody.put("nric", request.getNric());
            requestBody.put("txnNo", request.getTxnNo());

            // Setting Entity object with header values and body
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            
            // Make the request to SIT endpoint
            String url = spcpBaseUrl + "/spcpDS/myinfo/getMyInfoData";
            log.info("Calling external API: {}", url);
            
            ResponseEntity<MyInfoResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    MyInfoResponse.class);
            
            response = responseEntity.getBody();
            
            // Ensure response is not null
            if (response == null) {
                response = new MyInfoResponse();
                response.setResponseCode("500");
                response.setResponseMsg("Null response received from API");
                log.error("Null response received from getMyInfoData API");
            } else {
                log.info("Response from getMyInfoData API: {}", response);
            }
        } catch (Exception e) {
            String message = "Failed to get MyInfo data. Please try again.";
            log.error(message, e);
            // Ensure response is not null before setting properties
            if (response == null) {
                response = new MyInfoResponse();
            }
            response.setResponseCode("500");
            response.setResponseMsg(message);
        }
        
        return response;
    }
}
