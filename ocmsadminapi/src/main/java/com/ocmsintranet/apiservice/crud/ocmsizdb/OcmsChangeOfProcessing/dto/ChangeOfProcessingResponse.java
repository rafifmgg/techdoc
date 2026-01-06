package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Change Processing Stage batch operation
 * Based on OCMS CPS Spec §5.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeOfProcessingResponse {

    /**
     * Overall batch status: SUCCESS, PARTIAL, FAILED
     */
    private String status;

    /**
     * Summary statistics
     */
    private BatchSummary summary;

    /**
     * Per-notice results
     */
    private List<NoticeResult> results;

    /**
     * Report download information
     */
    private ReportInfo report;

    /**
     * Report URL (simple string alternative to ReportInfo)
     */
    private String reportUrl;

    /**
     * Existing record warning flag
     * Based on OCMS CPS Spec §2.5.1 Step 4
     *
     * If true, indicates that a duplicate record exists for one or more notices.
     * Frontend should prompt user to confirm before re-submitting with isConfirmation=true
     */
    private Boolean existingRecordWarning;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummary {
        private Integer requested;
        private Integer succeeded;
        private Integer failed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NoticeResult {
        private String noticeNo;

        /**
         * Outcome: UPDATED, FAILED, SKIPPED
         */
        private String outcome;

        /**
         * Previous processing stage (for successful updates)
         */
        private String previousStage;

        /**
         * New processing stage (for successful updates)
         */
        private String newStage;

        /**
         * Error code (for failures)
         */
        private String code;

        /**
         * Error or success message
         */
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportInfo {
        /**
         * Signed URL to download the report
         */
        private String url;

        /**
         * Report expiration timestamp
         */
        private LocalDateTime expiresAt;
    }
}
