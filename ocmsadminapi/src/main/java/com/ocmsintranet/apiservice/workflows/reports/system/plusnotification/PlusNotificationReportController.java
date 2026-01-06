package com.ocmsintranet.apiservice.workflows.reports.system.plusnotification;

import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportResponseDto;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.NotificationReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/${api.version}/plus-notification-report")
@Slf4j
public class PlusNotificationReportController {

    @Autowired
    private final NotificationReportService notificationReportService;

    public PlusNotificationReportController(NotificationReportService notificationReportService) {
        this.notificationReportService = notificationReportService;
    }

    @PostMapping
    public ResponseEntity<NotificationReportResponseDto> getPlusNotificationReport(
            @RequestBody NotificationReportRequestDto request) {
        log.info("Received PLUS notification report request: {}", request);
        return notificationReportService.getNotificationReport(request);
    }
}
