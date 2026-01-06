package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching notices for Change Processing Stage
 * Based on OCMS CPS Spec §2.3.1
 *
 * Search Criteria (at least one required):
 * - Notice Number
 * - ID Number
 * - Vehicle Number
 * - Current Processing Stage
 * - Date of Current Processing Stage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchChangeProcessingStageRequest {

    /**
     * Notice number (optional, max 10 characters)
     * Example: "N-001"
     */
    private String noticeNo;

    /**
     * ID number (optional, max 20 characters)
     * Example: "S1234567A", "T1234567B", "198801011234"
     *
     * When searching by ID Number:
     * - Backend queries ONOD table to find all notices for this ID
     * - Then retrieves notice details from VON table
     */
    private String idNo;

    /**
     * Vehicle number (optional, max 14 characters)
     * Example: "SBA1234A", "ABC123"
     */
    private String vehicleNo;

    /**
     * Current processing stage (optional)
     * Example: "DN1", "RD2", "ROV"
     */
    private String currentProcessingStage;

    /**
     * Date of current processing stage (optional)
     * Format: yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfCurrentProcessingStage;

    /**
     * Check if at least one search criterion is provided
     * @return true if valid, false otherwise
     */
    public boolean hasValidCriteria() {
        return (noticeNo != null && !noticeNo.trim().isEmpty()) ||
               (idNo != null && !idNo.trim().isEmpty()) ||
               (vehicleNo != null && !vehicleNo.trim().isEmpty()) ||
               (currentProcessingStage != null && !currentProcessingStage.trim().isEmpty()) ||
               (dateOfCurrentProcessingStage != null);
    }

    /**
     * Check if searching by ID Number
     * @return true if ID number is the primary search criterion
     */
    public boolean isSearchingByIdNo() {
        return idNo != null && !idNo.trim().isEmpty();
    }

    /**
     * Alias method for compatibility
     * @return current processing stage
     */
    public String getLastProcessingStage() {
        return this.currentProcessingStage;
    }

    /**
     * Alias method for compatibility
     * @param stage processing stage
     */
    public void setLastProcessingStage(String stage) {
        this.currentProcessingStage = stage;
    }
}
