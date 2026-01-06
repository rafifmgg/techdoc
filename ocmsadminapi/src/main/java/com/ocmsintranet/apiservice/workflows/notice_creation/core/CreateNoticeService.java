package com.ocmsintranet.apiservice.workflows.notice_creation.core;

import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.RepWebHookPayloadDto;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.NoticeResponse;

public interface CreateNoticeService {
    ResponseEntity<NoticeResponse> processCreateNotice(HttpServletRequest request, String rawData);
    ResponseEntity<NoticeResponse> processStaffCreateNotice(HttpServletRequest request, String rawData);
    ResponseEntity<NoticeResponse> processRepccsCreateNotice(RepWebHookPayloadDto rawData);
    ResponseEntity<NoticeResponse> processEHTSFTP(HttpServletRequest request, String rawData);
    ResponseEntity<NoticeResponse> processEHTWebhook(HttpServletRequest request, String rawData);
    ResponseEntity<NoticeResponse> processPlusCreateNotice(HttpServletRequest request, String rawData);
}