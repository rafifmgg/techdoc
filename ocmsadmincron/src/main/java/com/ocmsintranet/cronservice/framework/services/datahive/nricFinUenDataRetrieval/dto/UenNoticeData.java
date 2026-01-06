package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for UEN and notice number pairing
 * Used for batch processing to map results back to specific notices
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UenNoticeData {

    /**
     * UEN (Unique Entity Number)
     */
    private String uen;

    /**
     * Offence notice number
     */
    private String noticeNo;

    /**
     * Get cache key for result mapping
     * Format: "uen|noticeNo"
     */
    public String getCacheKey() {
        return uen + "|" + noticeNo;
    }
}
