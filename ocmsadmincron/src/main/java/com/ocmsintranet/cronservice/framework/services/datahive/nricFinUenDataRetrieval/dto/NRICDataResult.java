package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.ContactLookupResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result container for NRIC data retrieval from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NRICDataResult {
    
    // Comcare data from MSF tables
    private List<ComcareData> comcareData;
    
    // Common prison/custody data
    private CommonDataResult commonData;
    
    // Contact information (mobile)
    private ContactLookupResult contactInfo;
    
    // Raw query results for debugging
    private Map<String, JsonNode> rawResults;
    
    /**
     * Comcare fund data structure
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComcareData {
        private String idNo;
        private String noticeNo;
        private String assistanceStart;
        private String assistanceEnd;
        private String beneficiaryName;
        private String dataDate;
        private String paymentDate;
        private String referencePeriod;
        private String source; // FSC or CCC
    }
}