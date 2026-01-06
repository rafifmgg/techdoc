package com.ocmsintranet.apiservice.workflows.reports.system.notification;

import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/${api.version}/notification-report")
@Slf4j
public class NotificationReportController {

    @Autowired
    private final NotificationReportService notificationReportService;

    public NotificationReportController(NotificationReportService notificationReportService) {
        this.notificationReportService = notificationReportService;
    }

    @PostMapping("/search")
    public ResponseEntity<NotificationReportResponseDto> getNotificationReport(
            @RequestBody NotificationReportRequestDto request) {
        log.info("Received notification report request: {}", request);
        return notificationReportService.getNotificationReport(request);
    }
}