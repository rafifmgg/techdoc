package com.ocmsintranet.apiservice.workflows.reports.system.notification.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReportRequestDto {

    private String noticeNo;
    private String processingStage;
    private String status;
    private LocalDateTime dateFromCreated;
    private LocalDateTime dateToCreated;
    private LocalDateTime dateFromSent;
    private LocalDateTime dateToSent;

    // Email specific filters
    private String emailAddr;
    private String subject;

    // SMS specific filters
    private String mobileCode;
    private String mobileNo;

    // Error message filter
    private String msgError;

    // Pagination parameters
    @JsonProperty("$skip")
    private Integer skip = 0;
    @JsonProperty("$limit")
    private Integer limit = 10;
}