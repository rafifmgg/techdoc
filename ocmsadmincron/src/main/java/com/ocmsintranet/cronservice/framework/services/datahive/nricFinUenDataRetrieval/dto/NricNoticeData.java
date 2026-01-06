package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for NRIC and Notice Number pairing
 * Used for batch DataHive NRIC data retrieval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NricNoticeData {
    /**
     * NRIC/UIN number
     */
    private String nric;

    /**
     * Offence notice number
     */
    private String noticeNo;

    /**
     * Generate cache key for mapping results
     * Format: "nric|noticeNo"
     */
    public String getCacheKey() {
        return nric + "|" + noticeNo;
    }
}
