package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result container for UEN data retrieval from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UENDataResult {
    
    // Company registration data
    private CompanyInfo companyInfo;
    
    // Shareholder information
    private List<ShareholderInfo> shareholderData;
    
    // Board member information  
    private List<BoardInfo> boardData;
    
    // Whether TS-ACR was applied
    private boolean tsAcrApplied;
    
    // Error information for workflow handling
    private boolean hasError;
    private String errorMessage;
    private List<String> notFoundUENs; // For email notification by workflow
    
    // Raw query results for debugging
    private Map<String, JsonNode> rawResults;
    
    /**
     * Company registration information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        private String uen;
        private String noticeNo;
        private String entityName;
        private String entityType;
        private LocalDateTime registrationDate;
        private LocalDateTime deregistrationDate;
        private String entityStatusCode;
        private String companyTypeCode;
        private boolean isFound;
        private boolean isDeregistered;
        
        // Address fields
        private String addressOne;
        private String addressOneBlockHouseNumber;
        private String addressOneLevelNumber;
        private String addressOneUnitNumber;
        private String addressOnePostalCode;
        private String addressOneStreetName;
        private String addressOneBuildingName;
    }
    
    /**
     * Shareholder information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareholderInfo {
        private String companyUen;
        private String noticeNo;
        private String category;
        private String companyProfilUen; // Note: field name without 'e' as per diagram
        private String personIdNo;
        private String shareAllotedNo;
    }
    
    /**
     * Board member information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardInfo {
        private String entityUen;
        private String noticeNo;
        private LocalDateTime positionAppointmentDate;
        private LocalDateTime positionWithdrawnDate;
        private String personIdNo;
        private String positionHeldCode;
        private LocalDateTime referencePeriod;
    }
}