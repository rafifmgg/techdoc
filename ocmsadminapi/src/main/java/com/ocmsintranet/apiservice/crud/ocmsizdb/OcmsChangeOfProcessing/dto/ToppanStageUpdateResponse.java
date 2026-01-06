package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Toppan stage update operation
 *
 * Returns statistics about how many notices were updated as manual vs automatic changes,
 * and any errors encountered during processing.
 *
 * Based on OCMS 15 Spec §2.5.2 - Handling for Manual Stage Change Notice in Toppan Cron
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToppanStageUpdateResponse {

    /**
     * Total number of notices in the request
     */
    private int totalNotices;

    /**
     * Number of notices updated as AUTOMATIC stage changes
     * (VON stage updated + amount_payable calculated)
     */
    private int automaticUpdates;

    /**
     * Number of notices updated as MANUAL stage changes
     * (VON stage updated, amount_payable NOT touched)
     */
    private int manualUpdates;

    /**
     * Number of notices skipped (e.g., not found, stage mismatch, already processed)
     */
    private int skipped;

    /**
     * List of error messages for notices that failed to update
     * Format: "noticeNo: error message"
     */
    private List<String> errors;

    /**
     * Overall success flag
     */
    @Builder.Default
    private boolean success = true;
}
