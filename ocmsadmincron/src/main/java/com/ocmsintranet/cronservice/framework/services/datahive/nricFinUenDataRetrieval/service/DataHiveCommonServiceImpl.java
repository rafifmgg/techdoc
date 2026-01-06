package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DataHiveCommonService for retrieving prison/custody data
 * common to both NRIC and FIN ID types
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataHiveCommonServiceImpl implements DataHiveCommonService {
    
    private final DataHiveUtil dataHiveUtil;
    private final TableQueryService tableQueryService;
    
    private static final String CUSTODY_TABLE = "ocms_dh_sps_custody";
    private static final String INCARCERATION_TABLE = "ocms_dh_sps_incarceration";
    
    @Override
    public CommonDataResult getPrisonCustodyData(String idType, String idNumber, String noticeNumber) {
        log.info("Retrieving prison/custody data for {} type: {}, notice: {}", idType, idNumber, noticeNumber);
        
        CommonDataResult result = CommonDataResult.builder()
            .custodyData(new ArrayList<>())
            .incarcerationData(new ArrayList<>())
            .rawResults(new HashMap<>())
            .build();
        
        try {
            // Execute queries in parallel for better performance
            CompletableFuture<JsonNode> custodyFuture = getCustodyStatusAsync(idNumber);
            CompletableFuture<JsonNode> releaseFuture = getReleaseDateAsync(idNumber);
            
            // Wait for both queries to complete
            CompletableFuture.allOf(custodyFuture, releaseFuture).join();
            
            // Process custody status
            JsonNode custodyData = custodyFuture.get();
            result.getRawResults().put("custodyStatus", custodyData);
            
            if (custodyData != null && custodyData.isArray() && custodyData.size() > 0) {
                processCustodyData(custodyData, idNumber, noticeNumber, result);
            }
            
            // Process release date
            JsonNode releaseData = releaseFuture.get();
            result.getRawResults().put("releaseDate", releaseData);

            if (releaseData != null && releaseData.isArray() && releaseData.size() > 0) {
                processReleaseData(releaseData, idNumber, noticeNumber, result);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving prison/custody data for {}: {}", idNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve prison/custody data", e);
        }
        
        return result;
    }
    
    private CompletableFuture<JsonNode> getCustodyStatusAsync(String idNumber) {
        String query = String.format(
            "SELECT UIN, CURRENT_CUSTODY_STATUS, INSTIT_CODE, REFERENCE_PERIOD " +
            "FROM V_DH_SPS_CUSTODY_STATUS WHERE UIN = '%s'", 
            idNumber
        );
        
        log.debug("Executing custody status query");
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private CompletableFuture<JsonNode> getReleaseDateAsync(String idNumber) {
        String query = String.format(
            "SELECT TENTATIVE_DATE_OF_RELEASE, INMATE_NUMBER, UIN, REFERENCE_PERIOD " +
            "FROM V_DH_SPS_RELEASE_DATE WHERE UIN = '%s'",
            idNumber
        );
        
        log.debug("Executing release date query");
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private void processCustodyData(JsonNode custodyData, String idNumber, String noticeNumber, CommonDataResult result) {
        // DataHive returns data in array format based on SELECT field order:
        // SELECT UIN, CURRENT_CUSTODY_STATUS, INSTIT_CODE, REFERENCE_PERIOD
        // Index:   0,          1,                  2,            3
        for (JsonNode record : custodyData) {
            if (record.isArray() && record.size() >= 4) {
                CommonDataResult.CustodyInfo custody = CommonDataResult.CustodyInfo.builder()
                    .currentCustodyStatus(getArrayStringValue(record, 1))  // CURRENT_CUSTODY_STATUS
                    .noticeNo(noticeNumber)
                    .institCode(getArrayStringValue(record, 2))            // INSTIT_CODE
                    .idNo(idNumber)
                    .admDate(getArrayStringValue(record, 3))               // ADM_DT field not yet available in DEV environment, temporarily using reference period value
                    .referencePeriod(getArrayStringValue(record, 3))       // REFERENCE_PERIOD (raw epoch string)
                    .exists(true)
                    .build();
                
                result.getCustodyData().add(custody);
                
                // Store/update in database
                storeCustodyInfo(custody);
            } else {
                log.warn("Unexpected custody record format: {}", record);
            }
        }
    }
    
    private void processReleaseData(JsonNode releaseData, String idNumber, String noticeNumber, CommonDataResult result) {
        try {
            // DataHive returns data in array format based on SELECT field order:
            // SELECT TENTATIVE_DATE_OF_RELEASE, INMATE_NUMBER, UIN, REFERENCE_PERIOD
            // Index:            0,                    1,         2,         3
            for (JsonNode releaseRecord : releaseData) {
                if (releaseRecord.isArray() && releaseRecord.size() >= 4) {
                    String inmateNumber = getArrayStringValue(releaseRecord, 1);  // INMATE_NUMBER

                    if (inmateNumber != null && !inmateNumber.isEmpty()) {
                        CommonDataResult.IncarcerationInfo incarceration = CommonDataResult.IncarcerationInfo.builder()
                            .inmateNumber(inmateNumber)
                            .noticeNo(noticeNumber)
                            .idNo(idNumber)
                            .tentativeReleaseDate(getArrayStringValue(releaseRecord, 0))  // TENTATIVE_DATE_OF_RELEASE
                            .referencePeriodRelease(getArrayStringValue(releaseRecord, 3))  // REFERENCE_PERIOD from release
                            .exists(true)
                            .build();

                        result.getIncarcerationData().add(incarceration);

                        // Store/update in database
                        storeIncarcerationInfo(incarceration);
                    }
                } else {
                    log.warn("Unexpected release record format: {}", releaseRecord);
                }
            }
        } catch (Exception e) {
            log.error("Error processing release data: {}", e.getMessage(), e);
        }
    }
    
    private void storeCustodyInfo(CommonDataResult.CustodyInfo custody) {
        try {
            // Check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", custody.getIdNo());
            filters.put("noticeNo", custody.getNoticeNo());
            
            List<Map<String, Object>> existingRecords = tableQueryService.query(CUSTODY_TABLE, filters);
            
            if (!existingRecords.isEmpty()) {
                // Check if same ADM date exists
                LocalDateTime admDateTime = DataHiveDateUtil.convertEpochToLocalDateTime(custody.getAdmDate());
                boolean sameAdmDateExists = existingRecords.stream()
                    .anyMatch(record -> admDateTime != null && admDateTime.equals(record.get("admDate")));

                if (sameAdmDateExists) {
                    // Update existing record with composite key
                    Map<String, Object> updateFilters = new HashMap<>();
                    updateFilters.put("idNo", custody.getIdNo());
                    updateFilters.put("noticeNo", custody.getNoticeNo());
                    updateFilters.put("admDate", admDateTime);

                    Map<String, Object> updateFields = new HashMap<>();
                    updateFields.put("currentCustodyStatus", custody.getCurrentCustodyStatus());
                    updateFields.put("referencePeriod", DataHiveDateUtil.convertEpochToLocalDateTime(custody.getReferencePeriod()));
                    
                    tableQueryService.patch(CUSTODY_TABLE, updateFilters, updateFields);
                    log.info("Updated custody record for idNo: {}, noticeNo: {}", custody.getIdNo(), custody.getNoticeNo());
                } else {
                    // Create new record
                    createNewCustodyRecord(custody);
                }
            } else {
                // Create new record
                createNewCustodyRecord(custody);
            }
        } catch (Exception e) {
            log.error("Error storing custody info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store custody info", e);
        }
    }
    
    private void createNewCustodyRecord(CommonDataResult.CustodyInfo custody) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("currentCustodyStatus", custody.getCurrentCustodyStatus());
        fields.put("noticeNo", custody.getNoticeNo());
        fields.put("institCode", custody.getInstitCode());
        fields.put("idNo", custody.getIdNo());
        fields.put("admDate", DataHiveDateUtil.convertEpochToLocalDateTime(custody.getAdmDate()));
        fields.put("referencePeriod", DataHiveDateUtil.convertEpochDaysToLocalDateTime(custody.getReferencePeriod()));
        // Don't set audit fields - let JPA @PrePersist handle them
        
        tableQueryService.post(CUSTODY_TABLE, fields);
        log.info("Created new custody record for idNo: {}, noticeNo: {}", custody.getIdNo(), custody.getNoticeNo());
    }
    
    private void storeIncarcerationInfo(CommonDataResult.IncarcerationInfo incarceration) {
        try {
            // Check if record exists by inmateNumber (primary key)
            Map<String, Object> filters = new HashMap<>();
            filters.put("inmateNumber", incarceration.getInmateNumber());

            List<Map<String, Object>> existingRecords = tableQueryService.query(INCARCERATION_TABLE, filters);

            if (!existingRecords.isEmpty()) {
                // Update existing record
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("tentativeReleaseDate", DataHiveDateUtil.convertEpochToLocalDateTime(incarceration.getTentativeReleaseDate()));
                updateFields.put("referencePeriodRelease", DataHiveDateUtil.convertEpochDaysToLocalDateTime(incarceration.getReferencePeriodRelease()));
                // Temporary: set referencePeriodOffenceInfo to current time until DB migration removes this column
                updateFields.put("referencePeriodOffenceInfo", LocalDateTime.now());

                tableQueryService.patch(INCARCERATION_TABLE, filters, updateFields);
                log.info("Updated incarceration record for inmateNumber: {}", incarceration.getInmateNumber());
            } else {
                // Create new record
                Map<String, Object> fields = new HashMap<>();
                fields.put("inmateNumber", incarceration.getInmateNumber());
                fields.put("noticeNo", incarceration.getNoticeNo());
                fields.put("idNo", incarceration.getIdNo());
                fields.put("tentativeReleaseDate", DataHiveDateUtil.convertEpochToLocalDateTime(incarceration.getTentativeReleaseDate()));
                fields.put("referencePeriodRelease", DataHiveDateUtil.convertEpochDaysToLocalDateTime(incarceration.getReferencePeriodRelease()));
                // Temporary: set referencePeriodOffenceInfo to current time until DB migration removes this column
                fields.put("referencePeriodOffenceInfo", LocalDateTime.now());
                // Don't set audit fields - let JPA @PrePersist handle them

                tableQueryService.post(INCARCERATION_TABLE, fields);
                log.info("Created new incarceration record for idNo: {}, noticeNo: {}", incarceration.getIdNo(), incarceration.getNoticeNo());
            }
        } catch (Exception e) {
            log.error("Error storing incarceration info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store incarceration info", e);
        }
    }
    
    
    private String getArrayStringValue(JsonNode arrayNode, int index) {
        if (arrayNode == null || !arrayNode.isArray() || index >= arrayNode.size()) {
            return null;
        }
        JsonNode field = arrayNode.get(index);
        return field.isNull() ? null : field.asText();
    }

    /**
     * BATCH VERSION: Retrieve prison/custody data for multiple IDs in batches
     * Significantly reduces DataHive API calls by batching queries
     */
    @Override
    public Map<String, CommonDataResult> batchGetPrisonCustodyData(String idType, List<String> idNumbers) {
        log.info("Batch retrieving prison/custody data for {} IDs of type {}", idNumbers.size(), idType);
        Map<String, CommonDataResult> results = new HashMap<>();

        if (idNumbers == null || idNumbers.isEmpty()) {
            return results;
        }

        try {
            // Split into batches of 100
            int batchSize = 100;
            List<List<String>> batches = new ArrayList<>();
            for (int i = 0; i < idNumbers.size(); i += batchSize) {
                batches.add(idNumbers.subList(i, Math.min(i + batchSize, idNumbers.size())));
            }

            log.info("Processing {} batches of max {} IDs each", batches.size(), batchSize);

            for (int i = 0; i < batches.size(); i++) {
                List<String> batch = batches.get(i);
                log.info("Processing batch {}/{} with {} IDs", i + 1, batches.size(), batch.size());

                try {
                    // Query custody and release data in parallel for this batch
                    CompletableFuture<Map<String, List<CommonDataResult.CustodyInfo>>> custodyFuture =
                            CompletableFuture.supplyAsync(() -> batchGetCustodyStatus(batch));

                    CompletableFuture<Map<String, List<CommonDataResult.IncarcerationInfo>>> releaseFuture =
                            CompletableFuture.supplyAsync(() -> batchGetReleaseData(batch));

                    // Wait for both to complete
                    CompletableFuture.allOf(custodyFuture, releaseFuture).join();

                    Map<String, List<CommonDataResult.CustodyInfo>> custodyMap = custodyFuture.get();
                    Map<String, List<CommonDataResult.IncarcerationInfo>> releaseMap = releaseFuture.get();

                    // Combine results per ID
                    for (String idNumber : batch) {
                        CommonDataResult result = CommonDataResult.builder()
                                .custodyData(custodyMap.getOrDefault(idNumber, new ArrayList<>()))
                                .incarcerationData(releaseMap.getOrDefault(idNumber, new ArrayList<>()))
                                .rawResults(new HashMap<>())
                                .build();

                        results.put(idNumber, result);

                        // NOTE: Do NOT store here because noticeNo is null
                        // Storage will be handled by calling code after setting noticeNo per pair
                    }

                    log.info("Batch {}/{} completed. Found custody data for {} IDs, release data for {} IDs",
                            i + 1, batches.size(), custodyMap.size(), releaseMap.size());

                } catch (Exception e) {
                    log.error("Error processing batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
                }
            }

            log.info("Batch prison/custody data retrieval completed. Processed {} IDs", results.size());

        } catch (Exception e) {
            log.error("Error in batch prison/custody data retrieval: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Batch query custody status for multiple IDs
     */
    private Map<String, List<CommonDataResult.CustodyInfo>> batchGetCustodyStatus(List<String> idNumbers) {
        Map<String, List<CommonDataResult.CustodyInfo>> results = new HashMap<>();

        try {
            String idList = idNumbers.stream()
                    .map(id -> "'" + id + "'")
                    .collect(java.util.stream.Collectors.joining(", "));

            String query = String.format(
                    "SELECT UIN, CURRENT_CUSTODY_STATUS, INSTIT_CODE, REFERENCE_PERIOD " +
                    "FROM V_DH_SPS_CUSTODY_STATUS WHERE UIN IN (%s)",
                    idList
            );

            log.debug("Executing batch custody status query for {} IDs", idNumbers.size());

            CompletableFuture<JsonNode> future = dataHiveUtil.executeQueryAsyncDataOnly(query);
            JsonNode data = future.get(90, java.util.concurrent.TimeUnit.SECONDS);

            if (data != null && data.isArray()) {
                for (JsonNode record : data) {
                    if (record.isArray() && record.size() >= 4) {
                        String uin = getArrayStringValue(record, 0);

                        CommonDataResult.CustodyInfo custody = CommonDataResult.CustodyInfo.builder()
                                .currentCustodyStatus(getArrayStringValue(record, 1))
                                .noticeNo(null) // Will be set per notice later
                                .institCode(getArrayStringValue(record, 2))
                                .idNo(uin)
                                .admDate(getArrayStringValue(record, 3))
                                .referencePeriod(getArrayStringValue(record, 3))
                                .exists(true)
                                .build();

                        results.computeIfAbsent(uin, k -> new ArrayList<>()).add(custody);
                    }
                }
            }

            log.info("Batch custody query returned data for {} IDs", results.size());

        } catch (Exception e) {
            log.error("Error in batch custody status query: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Batch query release date for multiple IDs
     */
    private Map<String, List<CommonDataResult.IncarcerationInfo>> batchGetReleaseData(List<String> idNumbers) {
        Map<String, List<CommonDataResult.IncarcerationInfo>> results = new HashMap<>();

        try {
            String idList = idNumbers.stream()
                    .map(id -> "'" + id + "'")
                    .collect(java.util.stream.Collectors.joining(", "));

            String releaseQuery = String.format(
                    "SELECT TENTATIVE_DATE_OF_RELEASE, INMATE_NUMBER, UIN, REFERENCE_PERIOD " +
                    "FROM V_DH_SPS_RELEASE_DATE WHERE UIN IN (%s)",
                    idList
            );

            log.debug("Executing batch release date query for {} IDs", idNumbers.size());

            CompletableFuture<JsonNode> releaseFuture = dataHiveUtil.executeQueryAsyncDataOnly(releaseQuery);
            JsonNode releaseData = releaseFuture.get(90, java.util.concurrent.TimeUnit.SECONDS);

            if (releaseData != null && releaseData.isArray()) {
                for (JsonNode releaseRecord : releaseData) {
                    if (releaseRecord.isArray() && releaseRecord.size() >= 4) {
                        String inmateNumber = getArrayStringValue(releaseRecord, 1);
                        String uin = getArrayStringValue(releaseRecord, 2);

                        if (inmateNumber != null && !inmateNumber.isEmpty()) {
                            CommonDataResult.IncarcerationInfo incarceration = CommonDataResult.IncarcerationInfo.builder()
                                    .inmateNumber(inmateNumber)
                                    .noticeNo(null) // Will be set per notice later
                                    .idNo(uin)
                                    .tentativeReleaseDate(getArrayStringValue(releaseRecord, 0))
                                    .referencePeriodRelease(getArrayStringValue(releaseRecord, 3))
                                    .exists(true)
                                    .build();

                            results.computeIfAbsent(uin, k -> new ArrayList<>()).add(incarceration);
                        }
                    }
                }
            }

            log.info("Batch release query returned data for {} IDs", results.size());

        } catch (Exception e) {
            log.error("Error in batch release query: {}", e.getMessage(), e);
        }

        return results;
    }

}
