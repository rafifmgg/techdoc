package com.ocmsintranet.apiservice.utilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling API parameters
 */
public class ParameterUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterUtils.class);
    private static final String FIELDS_PARAM = "$field";
    
    /**
     * Normalize the $field parameter from various formats
     * @param value The $field value from request (can be String, String[], or List)
     * @return Normalized String[] of field names
     */
    public static String[] normalizeFieldsParameter(Object value) {
        if (value == null) {
            return new String[0];
        }
        
        // Handle different input types
        if (value instanceof String) {
            String fieldsStr = (String)value;
            if (fieldsStr.isEmpty()) {
                return new String[0];
            } else if (fieldsStr.contains(",")) {
                // Split comma-separated string
                return Arrays.stream(fieldsStr.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .toArray(String[]::new);
            } else {
                // Single field name
                return new String[]{fieldsStr.trim()};
            }
        } else if (value instanceof String[]) {
            return (String[])value;
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> fieldsList = (List<String>)value;
            return fieldsList.stream()
                   .filter(s -> s != null && !s.isEmpty())
                   .toArray(String[]::new);
        }
        
        return new String[0];
    }
    
    /**
     * Process a request body map and normalize all parameters for service layer
     * @param requestBody The original request body
     * @return Map with normalized parameters
     */
    public static Map<String, String[]> normalizeRequestParameters(Map<String, Object> requestBody) {
        Map<String, String[]> normalizedParams = new HashMap<>();
        
        if (requestBody != null) {
            requestBody.forEach((key, value) -> {
                // Special handling for $field parameter
                if (key.equals(FIELDS_PARAM)) {
                    String[] fieldsArray = normalizeFieldsParameter(value);
                    normalizedParams.put(key, fieldsArray);
                    log.debug("Normalized $field parameter: {}", Arrays.toString(fieldsArray));
                } else if (value instanceof String) {
                    normalizedParams.put(key, new String[] { (String) value });
                } else if (value instanceof Number || value instanceof Boolean) {
                    normalizedParams.put(key, new String[] { value.toString() });
                } else if (value instanceof String[]) {
                    normalizedParams.put(key, (String[]) value);
                } else if (value instanceof List) {
                    // For other list parameters, convert to String array
                    List<?> list = (List<?>) value;
                    String[] array = list.stream()
                        .map(Object::toString)
                        .toArray(String[]::new);
                    normalizedParams.put(key, array);
                }
            });
        }
        
        return normalizedParams;
    }
}
