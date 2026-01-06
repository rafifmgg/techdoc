package com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Ad-Hoc ANS Report (OCMS 10 Section 9.2)
 *
 * Contains Excel file download URL with 2 tabs:
 * - Tab 1: ANS (eNotifications sent via SMS/Email)
 * - Tab 2: AN Letter (Physical letters sent to Toppan)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnsReportResponseDto {

    /**
     * Download URL for the Excel file
     */
    private String downloadUrl;

    /**
     * File name of the generated report
     */
    private String fileName;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Number of ANS records (SMS/Email)
     */
    private Integer ansCount;

    /**
     * Number of AN Letter records
     */
    private Integer anLetterCount;

    /**
     * Total records across both tabs
     */
    private Integer totalRecords;

    /**
     * Timestamp when report was generated
     */
    private LocalDateTime generatedAt;

    /**
     * Search criteria used for the report
     */
    private AnsReportRequestDto searchCriteria;
}
