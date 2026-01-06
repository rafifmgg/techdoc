package com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * OCMS 18 - PS Report Request DTO
 *
 * Request DTO for Permanent Suspension Reports
 * Section 6: Permanent Suspension Report
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PsReportRequestDto {

    // Filter parameters
    private LocalDateTime dateFromSuspension;
    private LocalDateTime dateToSuspension;
    private String suspensionSource; // OCMS, PLUS, CERTIS, EEPS, REPCCS, or null for ALL
    private String officerAuthorisingSupension; // For PS by Officer report only
    private String creUserId; // PSing Officer - For PS by Officer report only

    // Pagination parameters
    @JsonProperty("$skip")
    private Integer skip = 0;
    @JsonProperty("$limit")
    private Integer limit = 10;

    // Sorting parameter - single field only
    // Valid fields: notice_no, vehicle_no, date_of_suspension, etc.
    private String sortField;
    private Integer sortDirection; // 1 for ASC, -1 for DESC
}
