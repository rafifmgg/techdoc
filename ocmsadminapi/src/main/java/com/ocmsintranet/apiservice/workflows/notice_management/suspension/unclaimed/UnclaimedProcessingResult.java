package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of processing an unclaimed reminder record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnclaimedProcessingResult {

    private String noticeNo;
    private boolean success;
    private String message;
    private String errorMessage;

    public UnclaimedProcessingResult(String noticeNo, boolean success, String message) {
        this.noticeNo = noticeNo;
        this.success = success;
        this.message = message;
    }
}
