package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Unclaimed Batch Data Report
 * Contains MHA/DataHive address lookup results
 */
@Data
public class UnclaimedBatchDataDto {

    private String noticeNo;
    private String idNo;
    private String offenderName;
    private String previousAddress;
    private String newAddress;
    private String addressSource;  // "MHA", "DataHive", "PR Grant"
    private LocalDateTime dateRetrieved;
}
