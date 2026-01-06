package com.ocmsintranet.apiservice.workflows.reports.system.notification;

import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportResponseDto;
import org.springframework.http.ResponseEntity;

public interface NotificationReportService {

    ResponseEntity<NotificationReportResponseDto> getNotificationReport(NotificationReportRequestDto request);
}