package com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for officer dashboard listing and search request.
 * Based on OCMS 41 User Stories 41.9-41.12.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApplicationListRequest {

    // Filter by status
    private List<String> statuses; // P, Resubmission (null = all)

    // Search parameters (OCMS41.10)
    private String noticeNo;
    private String vehicleNo;
    private String furnishIdNo; // Hirer or Driver ID

    // Date range filters
    private LocalDateTime submissionDateFrom;
    private LocalDateTime submissionDateTo;

    // Pagination
    private Integer page;
    private Integer pageSize;

    // Sorting
    private String sortBy; // noticeNo, submissionDate, vehicleNo, status
    private String sortDirection; // ASC, DESC
}
