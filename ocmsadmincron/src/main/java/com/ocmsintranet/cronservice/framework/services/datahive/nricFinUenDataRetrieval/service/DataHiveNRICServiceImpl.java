package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NRICDataResult;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DataHiveNRICService for retrieving NRIC holder data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataHiveNRICServiceImpl implements DataHiveNRICService {
    
    private final DataHiveUtil dataHiveUtil;
    private final DataHiveCommonService commonService;
    private final TableQueryService tableQueryService;
    
    private static final String COMCARE_TABLE = "ocms_dh_msf_comcare_fund";
    
    @Override
    public NRICDataResult retrieveNRICData(String nric, String noticeNumber) {
        log.info("Retrieving NRIC data for notice: {}", noticeNumber);
        
        NRICDataResult result = NRICDataResult.builder()
            .comcareData(new ArrayList<>())
            .rawResults(new HashMap<>())
            .build();
        
        try {
            // Step 1: Get Comcare data (FSC and CCC)
            getComcareData(nric, noticeNumber, result);
            
            // Step 2: Get common prison/custody data
            CommonDataResult commonData = commonService.getPrisonCustodyData("NRIC", nric, noticeNumber);
            result.setCommonData(commonData);
        } catch (Exception e) {
            log.error("Error retrieving NRIC data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve NRIC data", e);
        }
        
        return result;
    }
    
    private void getComcareData(String nric, String noticeNumber, NRICDataResult result) {
        try {
            // Query FSC and CCC in parallel
            CompletableFuture<JsonNode> fscFuture = queryFSCComcare(nric);
            CompletableFuture<JsonNode> cccFuture = queryCCCComcare(nric);
            
            // Wait for both queries to complete
            CompletableFuture.allOf(fscFuture, cccFuture).join();
            
            // Process FSC data
            JsonNode fscData = fscFuture.get();
            result.getRawResults().put("fscComcare", fscData);
            
            if (fscData != null && fscData.isArray() && fscData.size() > 0) {
                processFSCData(fscData, nric, noticeNumber, result);
            }
            
            // Process CCC data
            JsonNode cccData = cccFuture.get();
            result.getRawResults().put("cccComcare", cccData);
            
            if (cccData != null && cccData.isArray() && cccData.size() > 0) {
                processCCCData(cccData, nric, noticeNumber, result);
            }
            
        } catch (Exception e) {
            log.error("Error getting Comcare data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get Comcare data", e);
        }
    }
    
    private CompletableFuture<JsonNode> queryFSCComcare(String nric) {
        String query = String.format(
            "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, " +
            "BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
            "FROM V_DH_MSF_I2_FCF WHERE BENEFICIARY_ID_NO = '%s'",
            nric
        );
        
        log.debug("Executing FSC Comcare query");
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private CompletableFuture<JsonNode> queryCCCComcare(String nric) {
        String query = String.format(
            "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, " +
            "BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
            "FROM V_DH_MSF_I3_CCF WHERE BENEFICIARY_ID_NO = '%s'",
            nric
        );
        
        log.debug("Executing CCC Comcare query");
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private void processFSCData(JsonNode fscData, String nric, String noticeNumber, NRICDataResult result) {
        // DataHive returns data in array format based on SELECT field order:
        // SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD
        // Index:         0,                1,                2,               3,           4,          5,             6
        for (JsonNode record : fscData) {
            if (record.isArray() && record.size() >= 7) {
                NRICDataResult.ComcareData comcare = NRICDataResult.ComcareData.builder()
                    .idNo(nric)
                    .noticeNo(noticeNumber)
                    .assistanceStart(getArrayStringValue(record, 1))     // ASSISTANCE_START
                    .assistanceEnd(getArrayStringValue(record, 2))       // ASSISTANCE_END
                    .beneficiaryName(getArrayStringValue(record, 3))     // BENEFICIARY_NAME
                    .dataDate(getArrayStringValue(record, 4))           // DATA_DATE
                    .paymentDate(getArrayStringValue(record, 5))         // PAYMENT_DATE
                    .referencePeriod(getArrayStringValue(record, 6))     // REFERENCE_PERIOD
                    .source("FSC")
                    .build();
                
                result.getComcareData().add(comcare);
                
                // Store in database
                storeComcareData(comcare);
            } else {
                log.warn("Unexpected FSC Comcare record format: {}", record);
            }
        }
    }
    
    private void processCCCData(JsonNode cccData, String nric, String noticeNumber, NRICDataResult result) {
        // DataHive returns data in array format based on SELECT field order:
        // SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD
        // Index:         0,                1,                2,               3,           4,          5,             6
        for (JsonNode record : cccData) {
            if (record.isArray() && record.size() >= 7) {
                NRICDataResult.ComcareData comcare = NRICDataResult.ComcareData.builder()
                    .idNo(nric)
                    .noticeNo(noticeNumber)
                    .assistanceStart(getArrayStringValue(record, 1))     // ASSISTANCE_START
                    .assistanceEnd(getArrayStringValue(record, 2))       // ASSISTANCE_END
                    .beneficiaryName(getArrayStringValue(record, 3))     // BENEFICIARY_NAME
                    .dataDate(getArrayStringValue(record, 4))           // DATA_DATE
                    .paymentDate(getArrayStringValue(record, 5))         // PAYMENT_DATE
                    .referencePeriod(getArrayStringValue(record, 6))     // REFERENCE_PERIOD
                    .source("CCC")
                    .build();
                
                result.getComcareData().add(comcare);
                
                // Store in database
                storeComcareData(comcare);
            } else {
                log.warn("Unexpected CCC Comcare record format: {}", record);
            }
        }
    }
    
    @Override
    public DataHiveCommonService getCommonService() {
        return commonService;
    }
    
    private void storeComcareData(NRICDataResult.ComcareData comcare) {
        try {
            // Check if record exists - using composite key (id_no, notice_no)
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", comcare.getIdNo());
            filters.put("noticeNo", comcare.getNoticeNo());
            
            List<Map<String, Object>> existingRecords = tableQueryService.query(COMCARE_TABLE, filters);
            
            Map<String, Object> fields = new HashMap<>();
            // Map fields based on entity definition with date conversion for consistency
            fields.put("paymentDate", DataHiveDateUtil.convertEpochToLocalDateTime(comcare.getPaymentDate()));
            fields.put("assistanceStart", DataHiveDateUtil.convertEpochToLocalDateTime(comcare.getAssistanceStart()));
            fields.put("assistanceEnd", DataHiveDateUtil.convertEpochToLocalDateTime(comcare.getAssistanceEnd()));
            fields.put("beneficiaryName", comcare.getBeneficiaryName());
            fields.put("dataDate", DataHiveDateUtil.convertEpochToLocalDateTime(comcare.getDataDate()));
            fields.put("referencePeriod", DataHiveDateUtil.convertEpochDaysToLocalDateTime(comcare.getReferencePeriod()));
            fields.put("source", comcare.getSource());
            // Note: comcare_balance field is in the entity but not in DataHive data
            // Setting a default value or leaving null based on business rules
            
            if (!existingRecords.isEmpty()) {
                // Update existing record - patch only non-key fields
                tableQueryService.patch(COMCARE_TABLE, filters, fields);
                log.info("Updated Comcare {} data for idNo: {}, noticeNo: {} in {} table", 
                    comcare.getSource(), comcare.getIdNo(), comcare.getNoticeNo(), COMCARE_TABLE);
            } else {
                // Create new record - include key fields
                fields.put("idNo", comcare.getIdNo());
                fields.put("noticeNo", comcare.getNoticeNo());
                tableQueryService.post(COMCARE_TABLE, fields);
                log.info("Created new Comcare {} data for idNo: {}, noticeNo: {} in {} table", 
                    comcare.getSource(), comcare.getIdNo(), comcare.getNoticeNo(), COMCARE_TABLE);
            }
            
        } catch (Exception e) {
            log.error("Error storing Comcare data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store Comcare data", e);
        }
    }
    
    
    private String getArrayStringValue(JsonNode arrayNode, int index) {
        if (arrayNode == null || !arrayNode.isArray() || index >= arrayNode.size()) {
            return null;
        }
        JsonNode field = arrayNode.get(index);
        return field.isNull() ? null : field.asText();
    }
}