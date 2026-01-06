package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for handling PLUS API notice-related requests
 */
public interface GetNoticeService {
    
    /**
     * Process request for PLUS offence notice endpoint
     * 
     * @param request HTTP request
     * @param body Request body containing filters and pagination parameters
     * @return Response with offence notice data
     */
    ResponseEntity<?> processPlusOffenceNotice(
            HttpServletRequest request, Map<String, Object> body);
}
