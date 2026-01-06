package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling PLUS API notice-related endpoints
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class GetNoticeController {

    private final GetNoticeService getNoticeService;

    /**
     * Endpoint for retrieving valid offence notice records
     * 
     * @param request HTTP request
     * @param body Request body containing filters and pagination parameters
     * @return Response with offence notice data
     */
    @PostMapping("/plus-offence-notice")
    public ResponseEntity<?> getPlusOffenceNotice(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        log.info("Received request on /plus-offence-notice: {}", body);
        return getNoticeService.processPlusOffenceNotice(request, body);
    }
}
