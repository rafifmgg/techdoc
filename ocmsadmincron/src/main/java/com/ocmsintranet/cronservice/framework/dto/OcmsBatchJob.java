package com.ocmsintranet.cronservice.framework.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OcmsBatchJob {
    private Integer batchJobId;         // Primary key

    private String name;                // varchar(64)
    private String runStatus;           // varchar(1)
    private String logText;             // text

    private LocalDateTime startRun;     // datetime2(7)
    private LocalDateTime endRun;       // datetime2(7)

    private LocalDateTime creDate;      // datetime2(7)
    private String creUserId;           // varchar(50)

    private LocalDateTime updDate;      // datetime2(7)
    private String updUserId;           // varchar(50)
}