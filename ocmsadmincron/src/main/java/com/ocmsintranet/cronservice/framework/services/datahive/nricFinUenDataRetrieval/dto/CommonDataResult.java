package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Common prison/custody data result used by both NRIC and FIN services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonDataResult {
    
    // Custody status data
    private List<CustodyInfo> custodyData;
    
    // Incarceration data (release date info)
    private List<IncarcerationInfo> incarcerationData;
    
    // Raw query results for debugging
    private Map<String, JsonNode> rawResults;
    
    /**
     * Custody status information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustodyInfo {
        private String currentCustodyStatus;
        private String noticeNo;
        private String institCode;
        private String idNo;
        private String admDate;
        private String referencePeriod;
        private boolean exists;
    }
    
    /**
     * Incarceration information (release date info)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncarcerationInfo {
        private String inmateNumber;
        private String noticeNo;
        private String idNo;
        private String tentativeReleaseDate;
        private String referencePeriodRelease;
        private boolean exists;
    }
}