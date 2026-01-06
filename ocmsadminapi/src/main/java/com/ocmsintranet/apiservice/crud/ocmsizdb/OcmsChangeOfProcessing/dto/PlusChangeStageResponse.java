package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for PLUS Manual Stage Change API
 * Based on OCMS × PLUS Interface Spec v0.7 §3.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlusChangeStageResponse {

    /**
     * Request reference echoed from request (if provided)
     */
    private String requestRef;

    /**
     * Summary statistics
     */
    private StageSummary summary;

    /**
     * List of successfully changed notices
     */
    private List<SuccessItem> successList;

    /**
     * List of failed notices with error details
     */
    private List<FailedItem> failedList;

    /**
     * Optional URL to download detailed report
     */
    private String reportUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageSummary {
        private Integer total;
        private Integer success;
        private Integer failed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SuccessItem {
        private String noticeNo;
        private String prevStage;
        private String newStage;

        /**
         * Timestamp when change was made (ISO 8601 format)
         * Example: "2025-10-28T10:11:12+08:00"
         */
        private String changedAt;

        private String authorisedOfficer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String noticeNo;
        private String errorCode;
        private String errorMessage;
    }
}
