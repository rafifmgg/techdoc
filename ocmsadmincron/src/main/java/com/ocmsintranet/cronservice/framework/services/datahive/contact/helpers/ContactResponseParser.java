package com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.ContactLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper class to parse DataHive query responses for contact information
 * Extracts mobile numbers and email addresses from JSON responses
 */
@Slf4j
@Component
public class ContactResponseParser {
    
    /**
     * Parse Singpass query response for mobile number
     * Note: Singpass table only has CONTACT column (mobile number), no separate email
     * 
     * @param resultData JSON response from DataHive
     * @param ownerId The owner ID for logging
     * @return ContactLookupResult with mobile flag
     */
    public ContactLookupResult parseSingpassResponse(JsonNode resultData, String ownerId) {
        ContactLookupResult result = ContactLookupResult.builder()
            .queryTable("SINGPASS")
            .mobileFound(false)
            .emailFound(false)
            .build();
        
        if (resultData == null || !resultData.isArray() || resultData.size() == 0) {
            log.info("No Singpass contact data found");
            return result;
        }
        
        log.info("Singpass response contains {} records", resultData.size());
        JsonNode firstRow = resultData.get(0);
        log.debug("First row data: {}", firstRow.toString());
        
        // Check for mobile number - handle both array and object formats
        String contact = null;
        
        if (firstRow.isArray() && firstRow.size() >= 2) {
            // Array format: ["ID", "CONTACT"]
            contact = firstRow.get(1).asText();
            log.debug("Parsed contact from array format");
        } else if (firstRow.has("CONTACT") && !firstRow.get("CONTACT").isNull()) {
            // Object format: {"CONTACT": "value"}
            contact = firstRow.get("CONTACT").asText();
            log.debug("Parsed contact from object format");
        }
        
        if (contact != null && !contact.trim().isEmpty()) {
            result.setMobileFound(true);
            result.setMobileNumber(contact);
            log.debug("Found mobile number");
        }
        
        log.info("Singpass contact lookup: mobile={}", 
                result.isMobileFound());
        
        return result;
    }
    
    /**
     * Parse Corppass query response for email
     * 
     * @param resultData JSON response from DataHive
     * @param ownerId The owner ID for logging
     * @return ContactLookupResult with email flag
     */
    public ContactLookupResult parseCorppassResponse(JsonNode resultData, String ownerId) {
        ContactLookupResult result = ContactLookupResult.builder()
            .queryTable("CORPPASS")
            .mobileFound(false)  // Corppass doesn't have mobile numbers
            .emailFound(false)
            .build();
        
        if (resultData == null || !resultData.isArray() || resultData.size() == 0) {
            log.info("No Corppass contact data found");
            return result;
        }
        
        log.info("Corppass response contains {} records", resultData.size());
        JsonNode firstRow = resultData.get(0);
        log.debug("First row data: {}", firstRow.toString());
        
        // Check for email address - handle both array and object formats
        String email = null;
        
        if (firstRow.isArray() && firstRow.size() >= 3) {
            // Array format: ["ENTITY_ID", "CP_ACCOUNT_EMAIL", "ACCOUNT_STATUS"]
            email = firstRow.get(1).asText();
            log.debug("Parsed email from array format");
        } else if (firstRow.has("CP_ACCOUNT_EMAIL") && !firstRow.get("CP_ACCOUNT_EMAIL").isNull()) {
            // Object format: {"CP_ACCOUNT_EMAIL": "value"}
            email = firstRow.get("CP_ACCOUNT_EMAIL").asText();
            log.debug("Parsed email from object format");
        }
        
        if (email != null && !email.trim().isEmpty()) {
            result.setEmailFound(true);
            result.setEmailAddress(email);
            log.debug("Found email address");
        }
        
        log.info("Corppass contact lookup: email={}", 
                result.isEmailFound());
        
        return result;
    }
}