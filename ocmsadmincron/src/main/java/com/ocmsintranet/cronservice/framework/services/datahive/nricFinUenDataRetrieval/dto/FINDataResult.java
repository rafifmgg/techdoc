package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Result container for FIN data retrieval from DataHive
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FINDataResult {
    
    // Death status data
    private DeathStatus deathStatus;
    
    // PR/SC conversion status
    private PRStatus prStatus;
    
    // Work permit information (EP/WP)
    private WorkPermitInfo workPermitInfo;
    
    // Pass information (LTVP/STP)
    private PassInfo passInfo;
    
    // Common prison/custody data
    private CommonDataResult commonData;
    
    // Transaction codes applied
    private List<String> appliedTransactionCodes;
    
    // Work items generated (placeholder)
    private List<String> generatedWorkItems;
    
    // Raw query results for debugging
    private Map<String, JsonNode> rawResults;
    
    /**
     * Death status information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeathStatus {
        private String fin;
        private Date dateOfDeath;
        private boolean hasDeathDate;
        private String appliedTransactionCode; // PS-RIP or PS-RP2
    }
    
    /**
     * PR/SC conversion status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PRStatus {
        private String uin; // New NRIC
        private String previousFin;
        private Date datePrGranted;
        private Date scGrantDate;
        private boolean isConverted;
        private String conversionType; // "PR" or "SC"
        private String appliedTransactionCode; // TS-OLD
        private String remark; // "FIN has no NRIC" if not found
    }
    
    /**
     * Work permit information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkPermitInfo {
        private String idNo;
        private String noticeNo;
        private String workPermitNo;
        private String passType;
        private Date expiryDate;
        private Date cancelledDate;
        private String employerUen;
        private Date issuanceDate;
        private Date applicationDate;
        private Date ipaExpiryDate;
        private String workPassStatus;
        private Date withdrawnDate;
        private boolean isActive;
        
        // Address information
        private String blockHouseNo;
        private String floorNo;
        private String unitNo;
        private String streetName;
        private String postalCode;
    }
    
    /**
     * Pass information (LTVP/STP)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassInfo {
        private String idNo;
        private String noticeNo;
        private String passType; // NON_WORK_PASS_TYPE
        private Date dateOfIssue;
        private Date dateOfExpiry;
        private String principalName;
        private String sex;
        private Date dateOfBirth;
        private String referencePeriod;
        
        // Address information
        private String block;
        private String floor;
        private String unit;
        private String streetName;
        private String buildingName;
        private String postalCode;
    }
}