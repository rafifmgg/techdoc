package com.ocmsintranet.apiservice.workflows.reports.system.notification.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReportResponseDto {

    private List<NotificationRecord> data;
    private int total;
    private int skip;
    private int limit;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationRecord {
        private String noticeNo;
        private String processingStage;
        private String content;
        private String status;
        private String msgError;
        private LocalDateTime creDate;
        private String contact;
        private LocalDateTime dateSent;
    }
}