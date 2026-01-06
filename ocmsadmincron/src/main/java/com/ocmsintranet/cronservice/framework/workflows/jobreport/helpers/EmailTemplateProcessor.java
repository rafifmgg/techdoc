package com.ocmsintranet.cronservice.framework.workflows.jobreport.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Helper class for processing email templates
 */
@Component
@Slf4j
public class EmailTemplateProcessor {

    /**
     * Load an email template from the resources folder
     * 
     * @param templatePath Path to the template in the resources folder
     * @return The email template as a string
     * @throws IOException If the template cannot be loaded
     */
    public String loadEmailTemplate(String templatePath) throws IOException {
        log.debug("Loading email template: {}", templatePath);
        ClassPathResource resource = new ClassPathResource(templatePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
    
    /**
     * Process the template by replacing mustache-style variables with actual values
     * 
     * @param template The template string
     * @param data The data to replace variables with
     * @return The processed template
     */
    public String processTemplate(String template, Map<String, Object> data) {
        log.debug("Processing email template with {} data items", data.size());
        String result = template;
        
        // Replace simple variables
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!(entry.getValue() instanceof List)) {
                result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        
        // Process list sections (for repeating content)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof List) {
                String listKey = entry.getKey();
                String startTag = "{{#" + listKey + "}}";
                String endTag = "{{/" + listKey + "}}";
                
                int startIndex = result.indexOf(startTag);
                int endIndex = result.indexOf(endTag) + endTag.length();
                
                if (startIndex >= 0 && endIndex > startIndex) {
                    String beforeSection = result.substring(0, startIndex);
                    String section = result.substring(startIndex + startTag.length(), endIndex - endTag.length());
                    String afterSection = result.substring(endIndex);
                    
                    StringBuilder repeatedSections = new StringBuilder();
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> items = (List<Map<String, String>>) entry.getValue();
                    
                    for (Map<String, String> item : items) {
                        String processedSection = section;
                        for (Map.Entry<String, String> itemEntry : item.entrySet()) {
                            processedSection = processedSection.replace("{{" + itemEntry.getKey() + "}}", 
                                itemEntry.getValue() != null ? itemEntry.getValue() : "");
                        }
                        repeatedSections.append(processedSection);
                    }
                    
                    result = beforeSection + repeatedSections + afterSection;
                }
            }
        }
        
        return result;
    }
}
