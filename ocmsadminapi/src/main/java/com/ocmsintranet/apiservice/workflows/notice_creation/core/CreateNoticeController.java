package com.ocmsintranet.apiservice.workflows.notice_creation.core;

import com.ocmsintranet.apiservice.crud.exception.ErrorCodes;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.RepWebHookPayloadDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.NoticeResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1")
@Slf4j
public class CreateNoticeController {

    @Autowired
    private final CreateNoticeService createNoticeService;

    public CreateNoticeController(CreateNoticeService createNoticeService) {
        this.createNoticeService = createNoticeService;
    }

    @PostMapping("/create-notice")
    public ResponseEntity<NoticeResponse> handleNotice(HttpServletRequest request, @RequestBody String body) {
        log.info("Received on /create-notice: " + body);
        return createNoticeService.processCreateNotice(request, body);
    }

    @PostMapping("/staff-create-notice")
    public ResponseEntity<NoticeResponse> handleStaffNotice(HttpServletRequest request, @RequestBody String body) {
        log.info("Received on /staff-create-notice: " + body);
        return createNoticeService.processStaffCreateNotice(request, body);
    }

    @PostMapping("/plus-create-notice")
    public ResponseEntity<NoticeResponse> handlePlusNotice(HttpServletRequest request, @RequestBody String body) {
        log.info("Received on /plus-create-notice: " + body);
        return createNoticeService.processPlusCreateNotice(request, body);
    }

    @PostMapping("/repccsWebhook")
    public ResponseEntity<NoticeResponse> handleRepccsNotice(@RequestBody RepWebHookPayloadDto rep) {
        //log.info("Received on /repccs-create-notice: " + rep);

        ResponseEntity<NoticeResponse> response = createNoticeService.processRepccsCreateNotice(rep);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            NoticeResponse noticeResponse = response.getBody();
            Object data = noticeResponse.getData();
            if (data instanceof NoticeResponse.SingleNoticeResponseData) {
                NoticeResponse.SingleNoticeResponseData singleData = (NoticeResponse.SingleNoticeResponseData) data;
                if ("OCMS-2000".equals(singleData.getAppCode()) &&
                        (singleData.getMessage() != null && singleData.getMessage().contains("successfully created"))) {

                    singleData.setMessage("OK");
                    singleData.setNoticeNo(null);
                    log.info("Modified success response: message='OK', noticeNo removed");
                }
            }
        }

        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<NoticeResponse> handleJsonParseError(HttpMessageNotReadableException ex) {
        log.error("JSON deserialization error: {}", ex.getMessage());

        NoticeResponse errorResponse = new NoticeResponse();
        errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Invalid input format or failed validation");

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/ehtsftp-create-notice")
    public ResponseEntity<NoticeResponse> handleEhtsftpNotice(HttpServletRequest request, @RequestBody String body) {
        log.info("Received on /ehtsftp-create-notice");
        return createNoticeService.processEHTSFTP(request, body);
    }

    @PostMapping("/cesWebhook-create-notice")
    public ResponseEntity<NoticeResponse> handleEhtWebhookNotice(HttpServletRequest request, @RequestBody String body) {
        log.info("Received on /cesWebhook-create-notice");
        log.info("check payload certis ini controller ++" + body);

        return createNoticeService.processEHTWebhook(request, body);
    }
}