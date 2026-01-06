package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of HST processing per notice
 * Returned in array format by HST endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HstProcessingResult {

    private String noticeNo;
    private String appCode;
    private String message;

    public static HstProcessingResult success(String noticeNo) {
        return new HstProcessingResult(noticeNo, "OCMS-2000", "OK");
    }

    public static HstProcessingResult error(String noticeNo, String message) {
        return new HstProcessingResult(noticeNo, "OCMS-4002", message);
    }
}
