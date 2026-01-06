package com.ocmsintranet.apiservice.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Client for calling the Cron Service's SMS endpoints.
 * Delegates SMS sending to the cron service which has SmsUtil.
 */
@Component
@Slf4j
public class SmsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ocms.cron.baseUrl:http://localhost:8083}")
    private String cronBaseUrl;

    @Value("${ocms.cron.sms.enabled:true}")
    private boolean smsEnabled;

    public SmsClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Send SMS by calling cron service's SMS endpoint
     *
     * @param phoneNumber Phone number with country code (e.g., "+6591234567")
     * @param message SMS message body
     * @param language Language (default: "english")
     * @return true if SMS sent successfully
     */
    public boolean sendSms(String phoneNumber, String message, String language) {
        if (!smsEnabled) {
            log.warn("SMS is disabled. SMS to {} was not sent", phoneNumber);
            return false;
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.error("Phone number is required");
            return false;
        }

        if (message == null || message.isEmpty()) {
            log.error("SMS message is required");
            return false;
        }

        try {
            // Build endpoint URL
            String smsEndpoint = cronBaseUrl + "/v1/sms/send";
            log.info("Sending SMS via cron service: {}", smsEndpoint);

            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("phoneNumber", phoneNumber);
            requestBody.put("message", message);
            requestBody.put("language", language != null ? language : "english");

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Call cron SMS endpoint
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    smsEndpoint,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<JsonNode>() {}
            );

            // Check response
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                if (body != null && body.has("status")) {
                    String status = body.get("status").asText();
                    if ("success".equals(status)) {
                        log.info("SMS sent successfully to: {}", phoneNumber);
                        return true;
                    } else if ("disabled".equals(status)) {
                        log.warn("SMS is disabled in cron service");
                        return false;
                    } else {
                        log.error("SMS sending failed: {}", body.has("message") ? body.get("message").asText() : "Unknown error");
                        return false;
                    }
                }
                log.info("SMS request accepted by cron service");
                return true;
            } else {
                log.error("SMS API returned non-success status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error calling cron SMS service: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send SMS with mobile code and number separately
     *
     * @param mobileCode Country code (e.g., "65")
     * @param mobileNo Mobile number without country code
     * @param message SMS message body
     * @param language Language (default: "english")
     * @return true if SMS sent successfully
     */
    public boolean sendSms(String mobileCode, String mobileNo, String message, String language) {
        // Build phone number with country code
        String phoneNumber = "+" + (mobileCode != null ? mobileCode : "65") + mobileNo;
        return sendSms(phoneNumber, message, language);
    }
}
