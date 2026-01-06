package com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MhaReportRequestDto {

    private String noticeNo;
    private String processingStage;
    private String status;
    private LocalDateTime dateFromCreated;
    private LocalDateTime dateToCreated;

    // Pagination parameters
    @JsonProperty("$skip")
    private Integer skip = 0;
    @JsonProperty("$limit")
    private Integer limit = 10;

    // Sorting parameter - single field only
    // Format: "$sort[fieldName]": 1 (ASC) or -1 (DESC)
    // Valid fields: notice_no, blk_hse_no, street_name, floor_no, unit_no, bldg_name, postal_code
    // This will be populated by Spring from query parameters like $sort[notice_no]=1
    private String sortField;
    private Integer sortDirection;

    // Helper method to set sort from map (for backward compatibility if needed)
    public void setSortFromMap(java.util.Map<String, String[]> params) {
        if (params != null) {
            for (java.util.Map.Entry<String, String[]> entry : params.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("$sort[") && key.endsWith("]")) {
                    // Extract field name from $sort[fieldName]
                    String field = key.substring(6, key.length() - 1);
                    this.sortField = field;
                    if (entry.getValue() != null && entry.getValue().length > 0) {
                        try {
                            this.sortDirection = Integer.parseInt(entry.getValue()[0]);
                        } catch (NumberFormatException e) {
                            this.sortDirection = 1; // Default to ASC
                        }
                    }
                    break; // Only support single field
                }
            }
        }
    }
}