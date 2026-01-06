package com.ocmsintranet.apiservice.workflows.plus.evalidnotice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling PLUS API valid notice endpoint
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class EvalidNoticeController {

    private final EvalidNoticeService validNoticeService;

    /**
     * Get list of internet valid offence notices (PLUS Interface Specification)
     * Currently returns dummy data only
     */
    @PostMapping("/plus-evalid-offence-notice")
    public ResponseEntity<?> getPlusInternetValidOffenceNotice(@RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Received request on /plus-evalid-offence-notice: {}", requestBody);
        return validNoticeService.processPlusEValidOffenceNotice(requestBody);
    }
}
