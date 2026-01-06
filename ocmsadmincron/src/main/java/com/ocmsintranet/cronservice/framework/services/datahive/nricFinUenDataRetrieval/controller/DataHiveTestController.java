package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.controller;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FINDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NRICDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UENDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveFINService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveNRICService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveUENService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Temporary test controller for DataHive services
 * Remove this controller after testing is complete
 */
@Slf4j
@RestController
@RequestMapping("/api/datahive/test")
@RequiredArgsConstructor
public class DataHiveTestController {
    
    private final DataHiveNRICService nricService;
    private final DataHiveFINService finService;
    private final DataHiveUENService uenService;
    
    /**
     * Test NRIC data retrieval
     * Example: GET /api/datahive/test/nric/S1234567A?noticeNumber=ON123456
     */
    @GetMapping("/nric/{nric}")
    // Test NRIC data retrieval
    // Example: GET /api/datahive/test/nric/S1234567A?noticeNumber=ON123456
    public ResponseEntity<?> testNRICRetrieval(
            @PathVariable String nric,
            @RequestParam String noticeNumber) {
        
        log.info("Testing NRIC retrieval for: {}, notice: {}", nric, noticeNumber);
        
        try {
            NRICDataResult result = nricService.retrieveNRICData(nric, noticeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("summary", generateNRICSummary(result));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in NRIC test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test FIN data retrieval
     * Example: GET /api/datahive/test/fin/G1234567X?noticeNumber=ON123456&ownerDriverIndicator=O&offenceDate=2024-01-01
     */
    @GetMapping("/fin/{fin}")
    // Test FIN data retrieval
    // Retrieves death status, PR/SC conversion, work permits, passes, and prison/custody data for a FIN
    public ResponseEntity<?> testFINRetrieval(
            @PathVariable String fin,
            @RequestParam String noticeNumber,
            @RequestParam String ownerDriverIndicator, // Owner/Driver indicator (O/D/H)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date offenceDate) {
        
        log.info("Testing FIN retrieval for: {}, notice: {}, ownerDriver: {}, offenceDate: {}", 
            fin, noticeNumber, ownerDriverIndicator, offenceDate);
        
        try {
            FINDataResult result = finService.retrieveFINData(fin, noticeNumber, ownerDriverIndicator, offenceDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("summary", generateFINSummary(result));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in FIN test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test UEN data retrieval
     * Example: GET /api/datahive/test/uen/201234567A?noticeNumber=ON123456
     */
    @GetMapping("/uen/{uen}")
    // Test UEN data retrieval
    // Retrieves company registration, shareholder, and board member data for a UEN
    public ResponseEntity<?> testUENRetrieval(
            @PathVariable String uen,
            @RequestParam String noticeNumber) {
        
        log.info("Testing UEN retrieval for: {}, notice: {}", uen, noticeNumber);
        
        try {
            UENDataResult result = uenService.retrieveUENData(uen, noticeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("summary", generateUENSummary(result));
            
            // Highlight errors for workflow handling
            if (result.isHasError()) {
                response.put("workflowAction", "Email notification required");
                response.put("notFoundUENs", result.getNotFoundUENs());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in UEN test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test NRIC common dataset only (custody + incarceration)
     * Example: GET /api/datahive/test/common/nric/S1234567A?noticeNumber=ON123456
     */
    @GetMapping("/common/nric/{nric}")
    public ResponseEntity<?> testNRICCommonDataset(
            @PathVariable String nric,
            @RequestParam String noticeNumber) {
        
        log.info("Testing NRIC common dataset retrieval for: {}, notice: {}", nric, noticeNumber);
        
        try {
            // Get only common data (custody + incarceration)
            CommonDataResult commonData = nricService.getCommonService().getPrisonCustodyData("NRIC", nric, noticeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commonData", commonData);
            response.put("custodyRecords", commonData.getCustodyData() != null ? commonData.getCustodyData().size() : 0);
            response.put("incarcerationRecords", commonData.getIncarcerationData() != null ? commonData.getIncarcerationData().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in NRIC common dataset test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test FIN common dataset only (custody + incarceration)
     * Example: GET /api/datahive/test/common/fin/G1234567X?noticeNumber=ON123456
     */
    @GetMapping("/common/fin/{fin}")
    public ResponseEntity<?> testFINCommonDataset(
            @PathVariable String fin,
            @RequestParam String noticeNumber) {
        
        log.info("Testing FIN common dataset retrieval for: {}, notice: {}", fin, noticeNumber);
        
        try {
            // Get only common data (custody + incarceration)
            CommonDataResult commonData = finService.getCommonService().getPrisonCustodyData("FIN", fin, noticeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commonData", commonData);
            response.put("custodyRecords", commonData.getCustodyData() != null ? commonData.getCustodyData().size() : 0);
            response.put("incarcerationRecords", commonData.getIncarcerationData() != null ? commonData.getIncarcerationData().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in FIN common dataset test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test NRIC-specific data only (MSF ComCare + Contact)
     * Example: GET /api/datahive/test/specific/nric/S1234567A?noticeNumber=ON123456
     */
    @GetMapping("/specific/nric/{nric}")
    public ResponseEntity<?> testNRICSpecificData(
            @PathVariable String nric,
            @RequestParam String noticeNumber) {
        
        log.info("Testing NRIC-specific data retrieval for: {}, notice: {}", nric, noticeNumber);
        
        try {
            // Create a custom result for NRIC-specific data only
            NRICDataResult fullResult = nricService.retrieveNRICData(nric, noticeNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comcareData", fullResult.getComcareData());
            response.put("comcareRecords", fullResult.getComcareData() != null ? fullResult.getComcareData().size() : 0);
            response.put("contactInfo", fullResult.getContactInfo());
            response.put("hasContactInfo", fullResult.getContactInfo() != null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in NRIC-specific data test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test FIN-specific data only (Death, PR/SC, Work Permit, Pass)
     * Example: GET /api/datahive/test/specific/fin/G1234567X?noticeNumber=ON123456&ownerDriverIndicator=O&offenceDate=2024-01-01
     */
    @GetMapping("/specific/fin/{fin}")
    public ResponseEntity<?> testFINSpecificData(
            @PathVariable String fin,
            @RequestParam String noticeNumber,
            @RequestParam String ownerDriverIndicator,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date offenceDate) {
        
        log.info("Testing FIN-specific data retrieval for: {}, notice: {}", fin, noticeNumber);
        
        try {
            // Create a custom result for FIN-specific data only
            FINDataResult fullResult = finService.retrieveFINData(fin, noticeNumber, ownerDriverIndicator, offenceDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deathStatus", fullResult.getDeathStatus());
            response.put("hasDeathStatus", fullResult.getDeathStatus() != null && fullResult.getDeathStatus().isHasDeathDate());
            response.put("prStatus", fullResult.getPrStatus());
            response.put("isPRSCConverted", fullResult.getPrStatus() != null && fullResult.getPrStatus().isConverted());
            response.put("workPermitInfo", fullResult.getWorkPermitInfo());
            response.put("hasWorkPermit", fullResult.getWorkPermitInfo() != null);
            response.put("passInfo", fullResult.getPassInfo());
            response.put("hasPass", fullResult.getPassInfo() != null);
            response.put("appliedTransactionCodes", fullResult.getAppliedTransactionCodes());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in FIN-specific data test: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    // Health check - Check if DataHive test controller is running
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "DataHive Test Controller");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> generateNRICSummary(NRICDataResult result) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("comcareRecordsFound", result.getComcareData() != null ? result.getComcareData().size() : 0);
        summary.put("hasContactInfo", result.getContactInfo() != null);
        
        if (result.getCommonData() != null) {
            summary.put("custodyRecords", result.getCommonData().getCustodyData() != null ? 
                result.getCommonData().getCustodyData().size() : 0);
            summary.put("incarcerationRecords", result.getCommonData().getIncarcerationData() != null ? 
                result.getCommonData().getIncarcerationData().size() : 0);
        }
        
        return summary;
    }
    
    private Map<String, Object> generateFINSummary(FINDataResult result) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("hasDeathStatus", result.getDeathStatus() != null && result.getDeathStatus().isHasDeathDate());
        summary.put("isPRSCConverted", result.getPrStatus() != null && result.getPrStatus().isConverted());
        summary.put("hasWorkPermit", result.getWorkPermitInfo() != null);
        summary.put("hasPass", result.getPassInfo() != null);
        summary.put("appliedTransactionCodes", result.getAppliedTransactionCodes());
        summary.put("generatedWorkItems", result.getGeneratedWorkItems());
        
        if (result.getCommonData() != null) {
            summary.put("custodyRecords", result.getCommonData().getCustodyData() != null ? 
                result.getCommonData().getCustodyData().size() : 0);
            summary.put("incarcerationRecords", result.getCommonData().getIncarcerationData() != null ? 
                result.getCommonData().getIncarcerationData().size() : 0);
        }
        
        return summary;
    }
    
    private Map<String, Object> generateUENSummary(UENDataResult result) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("companyFound", result.getCompanyInfo() != null && result.getCompanyInfo().isFound());
        summary.put("isDeregistered", result.getCompanyInfo() != null && result.getCompanyInfo().isDeregistered());
        summary.put("shareholderRecords", result.getShareholderData() != null ? result.getShareholderData().size() : 0);
        summary.put("boardMemberRecords", result.getBoardData() != null ? result.getBoardData().size() : 0);
        summary.put("tsAcrApplied", result.isTsAcrApplied());
        summary.put("hasError", result.isHasError());
        
        if (result.isHasError()) {
            summary.put("errorMessage", result.getErrorMessage());
        }
        
        return summary;
    }
}