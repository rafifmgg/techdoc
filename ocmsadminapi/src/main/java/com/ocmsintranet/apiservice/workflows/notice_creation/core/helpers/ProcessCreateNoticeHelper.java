package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;

/**
 * Helper class for the processCreateNotice method in CreateNoticeServiceImpl
 */
@Component
@Slf4j
public class ProcessCreateNoticeHelper {

    /**
     * Creates an error response map for HTTP responses
     * 
     * @param statusCode The HTTP status code
     * @param message The error message
     * @return Map containing the error details
     */
    public Map<String, Object> createErrorResponse(String statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("HTTPStatusCode", statusCode);
        response.put("HTTPStatusDescription", message);
        return response;
    }
    
    /**
     * Creates a success response with notice number
     * 
     * @param noticeNumber The notice number to include in the response
     * @return Map containing the success details
     */
    public Map<String, String> createSuccessResponse(String noticeNumber) {
        Map<String, String> response = new HashMap<>();
        response.put("HTTPStatusCode", "200");
        response.put("HTTPStatusDescription", "Success");
        response.put("NoticeNumber", noticeNumber);
        return response;
    }
}