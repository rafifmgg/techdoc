package com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for officer dashboard listing response.
 * Based on OCMS 41 User Stories 41.9, 41.11, 41.12.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApplicationListResponse {

    private List<FurnishApplicationSummary> applications;
    private long totalRecords;
    private int currentPage;
    private int totalPages;

    /**
     * Summary information for each furnish application in the list.
     * Key details to display (OCMS41.9):
     * - Notice No.
     * - Vehicle No.
     * - Offence Date & Time
     * - Current Processing Stage
     * - Status (Pending, Resubmission)
     * - Submission Date & Time
     * - No. of Working Days Pending Approval
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FurnishApplicationSummary {
        private String txnNo;
        private String noticeNo;
        private String vehicleNo;
        private LocalDateTime offenceDate;
        private String ppCode;
        private String ppName;
        private String currentProcessingStage;
        private String status; // P, Resubmission, A, R
        private LocalDateTime submissionDate;
        private Integer workingDaysPending;
        private String furnishName;
        private String furnishIdNo;
        private String ownerDriverIndicator; // H or D
    }
}
