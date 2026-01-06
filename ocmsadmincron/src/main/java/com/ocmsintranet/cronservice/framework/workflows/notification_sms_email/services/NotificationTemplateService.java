package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.services;

import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.models.OcmsTemplateStore;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.repositories.OcmsTemplateStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch and process notification templates from the database.
 * Handles template retrieval, caching, and placeholder replacement.
 */
@Service
@Slf4j
public class NotificationTemplateService {

    @Autowired
    private OcmsTemplateStoreRepository templateRepository;

    // In-memory cache for templates to reduce database queries
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    // Pattern to match placeholders in format <placeholder_name>
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("<([^>]+)>");

    // Email template delimiters
    private static final String SUBJECT_DELIMITER = "---SUBJECT---";
    private static final String BODY_DELIMITER = "---BODY---";

    // List of all expected templates (from Excel)
    private static final String[] REQUIRED_TEMPLATES = {
        "EAN_SMS_INDIVIDUAL",
        "EAN_EMAIL_COMPANY",
        "ENOPO_SMS_INDIVIDUAL",
        "ENOPO_EMAIL_COMPANY",
        "ERD1_SMS_INDIVIDUAL",
        "ERD2_SMS_INDIVIDUAL",
        "ERR3_SMS_INDIVIDUAL",
        "EDN_SMS_INDIVIDUAL",
        "ECTS_SMS_INDIVIDUAL",
        "EATOMS_SMS_INDIVIDUAL",
        "EFM_SMS_INDIVIDUAL",
        "EFM_EMAIL_COMPANY",
        "ELA_SMS_INDIVIDUAL",
        "ELV_SMS_INDIVIDUAL"
    };

    @PostConstruct
    public void init() {
        log.info("NotificationTemplateService initialized");
        validateRequiredTemplates();
    }

    /**
     * Validate that all required templates exist in the database.
     * Logs errors for missing templates but does not prevent application startup.
     * Per README requirement: "Missing templates do not stop the cron"
     */
    public void validateRequiredTemplates() {
        log.info("Validating required templates...");
        int missingCount = 0;

        for (String templateName : REQUIRED_TEMPLATES) {
            try {
                Optional<OcmsTemplateStore> template = templateRepository.findLatestActiveByTemplateName(templateName);
                if (!template.isPresent()) {
                    log.error("CRITICAL: Required template '{}' is MISSING from database!", templateName);
                    missingCount++;
                } else if (template.get().getTemplateContent() == null || template.get().getTemplateContent().isEmpty()) {
                    log.error("CRITICAL: Required template '{}' exists but has NULL or EMPTY content!", templateName);
                    missingCount++;
                } else {
                    log.debug("Template '{}' (version {}) validated successfully",
                             templateName, template.get().getVersion());
                }
            } catch (Exception e) {
                log.error("Error validating template '{}': {}", templateName, e.getMessage(), e);
                missingCount++;
            }
        }

        if (missingCount == 0) {
            log.info("Template validation complete: All {} required templates are present", REQUIRED_TEMPLATES.length);
        } else {
            log.error("Template validation complete: {} out of {} templates are MISSING or INVALID!",
                     missingCount, REQUIRED_TEMPLATES.length);
            log.error("PLEASE RUN THE TEMPLATE MIGRATION SQL SCRIPT TO INSERT MISSING TEMPLATES!");
        }
    }

    /**
     * Get template content by template name.
     * Fetches the latest active version of the template.
     * Uses cache for performance, fetches from database if not cached.
     *
     * @param templateName The name of the template to retrieve
     * @return Template content, or null if not found
     */
    public String getTemplateContent(String templateName) {
        if (templateName == null || templateName.isEmpty()) {
            log.warn("Template name is null or empty");
            return null;
        }

        // Check cache first
        if (templateCache.containsKey(templateName)) {
            log.debug("Template {} found in cache", templateName);
            return templateCache.get(templateName);
        }

        // Fetch latest active version from database
        try {
            Optional<OcmsTemplateStore> template = templateRepository.findLatestActiveByTemplateName(templateName);
            if (template.isPresent() && template.get().getTemplateContent() != null) {
                String content = template.get().getTemplateContent();
                Integer version = template.get().getVersion();
                templateCache.put(templateName, content); // Cache it
                log.info("Template {} (version {}) loaded from database and cached", templateName, version);
                return content;
            } else {
                log.warn("Template {} not found in database or has null content", templateName);
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching template {}: {}", templateName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Process a template by replacing placeholders with actual values.
     * Placeholders are in the format <placeholder_name> (e.g., <Name>, <noticeno>)
     *
     * @param templateContent The template content with placeholders
     * @param data Map containing placeholder values
     * @return Processed template with placeholders replaced
     */
    public String processTemplate(String templateContent, Map<String, Object> data) {
        if (templateContent == null || templateContent.isEmpty()) {
            log.warn("Template content is null or empty");
            return "";
        }

        if (data == null) {
            log.warn("Data map is null, returning template as-is");
            return templateContent;
        }

        String result = templateContent;

        // Replace all placeholders found in the template
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String placeholderName = matcher.group(1); // Extract placeholder name without < >
            String placeholder = "<" + placeholderName + ">"; // Full placeholder with brackets

            // Try to find matching value in data map (case-insensitive)
            Object value = findValueInMap(data, placeholderName);

            if (value != null) {
                String replacement = value.toString().trim();
                result = result.replace(placeholder, replacement);
                log.debug("Replaced placeholder {} with value: {}", placeholder, replacement);
            } else {
                log.warn("No value found for placeholder: {}", placeholder);
                // Keep placeholder as-is if no value found
            }
        }

        return result;
    }

    /**
     * Find value in map by key, case-insensitive.
     * Also handles common mapping variations (e.g., "vehicle no." → "vehicleNo")
     *
     * @param data Map containing data
     * @param key Key to search for
     * @return Value if found, null otherwise
     */
    private Object findValueInMap(Map<String, Object> data, String key) {
        if (key == null) return null;

        // Try exact match first
        if (data.containsKey(key)) {
            return data.get(key);
        }

        // Try case-insensitive match
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        // Try common variations (e.g., "vehicle no." → "vehicleNo")
        String normalizedKey = normalizeKey(key);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() != null && normalizeKey(entry.getKey()).equals(normalizedKey)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Normalize key by removing spaces, dots, and converting to lowercase.
     *
     * @param key Key to normalize
     * @return Normalized key
     */
    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase()
                  .replace(" ", "")
                  .replace(".", "")
                  .replace("_", "");
    }

    /**
     * Split email template into subject and body.
     * Expected format:
     * ---SUBJECT---
     * Subject text here
     * ---BODY---
     * Body HTML here
     *
     * @param emailTemplate The email template content
     * @return Map with "subject" and "body" keys
     */
    public Map<String, String> splitEmailTemplate(String emailTemplate) {
        Map<String, String> result = new HashMap<>();

        if (emailTemplate == null || emailTemplate.isEmpty()) {
            log.warn("Email template is null or empty");
            result.put("subject", "");
            result.put("body", "");
            return result;
        }

        try {
            // Find subject section
            int subjectStart = emailTemplate.indexOf(SUBJECT_DELIMITER);
            int bodyStart = emailTemplate.indexOf(BODY_DELIMITER);

            if (subjectStart == -1 || bodyStart == -1) {
                log.warn("Email template does not contain required delimiters. Using whole content as body.");
                result.put("subject", "Notification from URA");
                result.put("body", emailTemplate);
                return result;
            }

            // Extract subject (between ---SUBJECT--- and ---BODY---)
            String subject = emailTemplate.substring(
                subjectStart + SUBJECT_DELIMITER.length(),
                bodyStart
            ).trim();

            // Extract body (after ---BODY---)
            String body = emailTemplate.substring(bodyStart + BODY_DELIMITER.length()).trim();

            result.put("subject", subject);
            result.put("body", body);

            log.debug("Email template split - Subject: {}, Body length: {}", subject, body.length());
            return result;

        } catch (Exception e) {
            log.error("Error splitting email template: {}", e.getMessage(), e);
            result.put("subject", "Notification from URA");
            result.put("body", emailTemplate);
            return result;
        }
    }

    /**
     * Get and process a template in one call.
     * Convenience method that combines getTemplateContent and processTemplate.
     *
     * @param templateName Name of the template to fetch
     * @param data Map containing placeholder values
     * @return Processed template content
     */
    public String getAndProcessTemplate(String templateName, Map<String, Object> data) {
        String templateContent = getTemplateContent(templateName);
        if (templateContent == null) {
            log.error("Template {} not found, cannot process", templateName);
            return "";
        }
        return processTemplate(templateContent, data);
    }

    /**
     * Clear the template cache.
     * Useful for forcing a refresh of templates from the database.
     */
    public void clearCache() {
        templateCache.clear();
        log.info("Template cache cleared");
    }

    /**
     * Refresh a specific template in the cache.
     *
     * @param templateName Name of the template to refresh
     */
    public void refreshTemplate(String templateName) {
        templateCache.remove(templateName);
        log.info("Template {} removed from cache, will be reloaded on next access", templateName);
    }
}
