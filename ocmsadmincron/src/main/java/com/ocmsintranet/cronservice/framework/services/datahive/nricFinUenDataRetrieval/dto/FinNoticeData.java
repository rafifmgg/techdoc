package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for FIN and notice number pairing
 * Used for batch processing to map results back to specific notices
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinNoticeData {

    /**
     * FIN (Foreign Identification Number)
     */
    private String fin;

    /**
     * Offence notice number
     */
    private String noticeNo;

    /**
     * Owner/driver indicator ('O', 'D', 'H')
     */
    private String ownerDriverIndicator;

    /**
     * Offence date (used for death date comparison)
     */
    private Date offenceDate;

    /**
     * Get cache key for result mapping
     * Format: "fin|noticeNo"
     */
    public String getCacheKey() {
        return fin + "|" + noticeNo;
    }
}
