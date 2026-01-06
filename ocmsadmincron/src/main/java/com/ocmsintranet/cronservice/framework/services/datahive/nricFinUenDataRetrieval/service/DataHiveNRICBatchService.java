package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NRICDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NricNoticeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Batch processing service for DataHive NRIC data lookups
 * Processes multiple NRIC records in batches to avoid rate limiting
 *
 * Main benefits:
 * - Reduces API calls from N records to N/batch_size calls per data source
 * - Avoids DataHive rate limiting issues
 * - Improves overall performance with parallel execution
 * - Deduplicates NRICs automatically
 *
 * Data sources queried:
 * - FSC Comcare (V_DH_MSF_I2_FCF)
 * - CCC Comcare (V_DH_MSF_I3_CCF)
 * - Prison/Custody data (via DataHiveCommonService)
 */
@Slf4j
@Service
public class DataHiveNRICBatchService {

    private static final int BATCH_SIZE = 100; // Adjust based on DataHive limits
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long BATCH_TIMEOUT_SECONDS = 90;

    // Table names
    private static final String FSC_TABLE = "V_DH_MSF_I2_FCF";
    private static final String CCC_TABLE = "V_DH_MSF_I3_CCF";

    private final DataHiveUtil dataHiveUtil;
    private final DataHiveCommonService commonService;

    public DataHiveNRICBatchService(
            DataHiveUtil dataHiveUtil,
            DataHiveCommonService commonService) {
        this.dataHiveUtil = dataHiveUtil;
        this.commonService = commonService;
    }

    /**
     * Batch retrieve NRIC data for multiple UIN/notice pairs
     * Groups by unique NRICs and queries in batches to avoid rate limits
     *
     * @param nricNoticePairs List of NRIC/notice pairs to lookup
     * @return Map of "nric|noticeNo" -> NRICDataResult
     */
    public Map<String, NRICDataResult> batchRetrieveNRICData(List<NricNoticeData> nricNoticePairs) {
        log.info("Starting batch DataHive NRIC lookup for {} pairs", nricNoticePairs.size());

        Map<String, NRICDataResult> resultsMap = new HashMap<>();

        // Step 1: Extract unique NRICs
        Set<String> uniqueNrics = nricNoticePairs.stream()
                .map(NricNoticeData::getNric)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("Processing {} unique NRICs in batch", uniqueNrics.size());

        // Step 2: Query all data sources in parallel for all unique NRICs
        Map<String, List<NRICDataResult.ComcareData>> fscResultsByNric = new HashMap<>();
        Map<String, List<NRICDataResult.ComcareData>> cccResultsByNric = new HashMap<>();
        Map<String, CommonDataResult> prisonResultsByNric = new HashMap<>();

        try {
            // Run all 3 data source queries in parallel
            CompletableFuture<Void> fscFuture = CompletableFuture.runAsync(() -> {
                fscResultsByNric.putAll(batchQueryFSCComcare(new ArrayList<>(uniqueNrics)));
            });

            CompletableFuture<Void> cccFuture = CompletableFuture.runAsync(() -> {
                cccResultsByNric.putAll(batchQueryCCCComcare(new ArrayList<>(uniqueNrics)));
            });

            CompletableFuture<Void> prisonFuture = CompletableFuture.runAsync(() -> {
                prisonResultsByNric.putAll(batchQueryPrisonCustody(new ArrayList<>(uniqueNrics)));
            });

            // Wait for all queries to complete
            CompletableFuture.allOf(fscFuture, cccFuture, prisonFuture).join();

            log.info("Batch queries completed - FSC: {}, CCC: {}, Prison: {}",
                    fscResultsByNric.size(), cccResultsByNric.size(), prisonResultsByNric.size());

        } catch (Exception e) {
            log.error("Error during parallel batch queries: {}", e.getMessage(), e);
        }

        // Step 3: Combine results for each NRIC/notice pair
        for (NricNoticeData pair : nricNoticePairs) {
            String nric = pair.getNric();
            String noticeNo = pair.getNoticeNo();
            String cacheKey = pair.getCacheKey();

            try {
                // Combine all data sources for this NRIC
                NRICDataResult result = NRICDataResult.builder()
                        .comcareData(new ArrayList<>())
                        .rawResults(new HashMap<>())
                        .build();

                // Add FSC data if available
                List<NRICDataResult.ComcareData> fscData = fscResultsByNric.get(nric);
                if (fscData != null && !fscData.isEmpty()) {
                    // Set noticeNo for each FSC record
                    fscData.forEach(data -> data.setNoticeNo(noticeNo));
                    result.getComcareData().addAll(fscData);
                }

                // Add CCC data if available
                List<NRICDataResult.ComcareData> cccData = cccResultsByNric.get(nric);
                if (cccData != null && !cccData.isEmpty()) {
                    // Set noticeNo for each CCC record
                    cccData.forEach(data -> data.setNoticeNo(noticeNo));
                    result.getComcareData().addAll(cccData);
                }

                // Add Prison/Custody data if available
                CommonDataResult prisonData = prisonResultsByNric.get(nric);
                if (prisonData != null) {
                    result.setCommonData(prisonData);
                }

                resultsMap.put(cacheKey, result);

            } catch (Exception e) {
                log.error("Error combining results for {}: {}", cacheKey, e.getMessage(), e);
            }
        }

        log.info("Batch lookup completed. Total results: {}", resultsMap.size());
        return resultsMap;
    }

    /**
     * Batch query FSC Comcare data for multiple NRICs
     */
    private Map<String, List<NRICDataResult.ComcareData>> batchQueryFSCComcare(List<String> nrics) {
        log.info("Batch querying FSC Comcare for {} NRICs", nrics.size());
        Map<String, List<NRICDataResult.ComcareData>> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(nrics, BATCH_SIZE);
        log.info("Processing {} FSC batches of max {} NRICs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing FSC batch {}/{} with {} NRICs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryFSC(batch);
                log.debug("FSC batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "FSC batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, List<NRICDataResult.ComcareData>> batchResults = parseFSCBatchResponse(resultData);
                    results.putAll(batchResults);
                    log.info("FSC batch {}/{} completed. Found {} NRICs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No FSC data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing FSC batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query CCC Comcare data for multiple NRICs
     */
    private Map<String, List<NRICDataResult.ComcareData>> batchQueryCCCComcare(List<String> nrics) {
        log.info("Batch querying CCC Comcare for {} NRICs", nrics.size());
        Map<String, List<NRICDataResult.ComcareData>> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(nrics, BATCH_SIZE);
        log.info("Processing {} CCC batches of max {} NRICs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing CCC batch {}/{} with {} NRICs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryCCC(batch);
                log.debug("CCC batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "CCC batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, List<NRICDataResult.ComcareData>> batchResults = parseCCCBatchResponse(resultData);
                    results.putAll(batchResults);
                    log.info("CCC batch {}/{} completed. Found {} NRICs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No CCC data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing CCC batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query Prison/Custody data for multiple NRICs
     * Uses batch implementation from DataHiveCommonService
     */
    private Map<String, CommonDataResult> batchQueryPrisonCustody(List<String> nrics) {
        log.info("Batch querying Prison/Custody data for {} NRICs", nrics.size());
        Map<String, CommonDataResult> results = new HashMap<>();

        try {
            // Use batch method from DataHiveCommonService
            results = commonService.batchGetPrisonCustodyData("NRIC", nrics);
            log.info("Prison/Custody batch query completed. Found {} NRICs with data", results.size());
        } catch (Exception e) {
            log.error("Error in batch prison/custody query: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Build batch query for FSC Comcare with IN clause
     */
    private String buildBatchQueryFSC(List<String> nrics) {
        String nricList = nrics.stream()
                .map(nric -> "'" + nric + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, " +
                "BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
                "FROM %s WHERE BENEFICIARY_ID_NO IN (%s)",
                FSC_TABLE, nricList
        );
    }

    /**
     * Build batch query for CCC Comcare with IN clause
     */
    private String buildBatchQueryCCC(List<String> nrics) {
        String nricList = nrics.stream()
                .map(nric -> "'" + nric + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT BENEFICIARY_ID_NO, ASSISTANCE_START, ASSISTANCE_END, " +
                "BENEFICIARY_NAME, DATA_DATE, PAYMENT_DATE, REFERENCE_PERIOD " +
                "FROM %s WHERE BENEFICIARY_ID_NO IN (%s)",
                CCC_TABLE, nricList
        );
    }

    /**
     * Parse FSC batch response and group by NRIC
     */
    private Map<String, List<NRICDataResult.ComcareData>> parseFSCBatchResponse(JsonNode resultData) {
        Map<String, List<NRICDataResult.ComcareData>> results = new HashMap<>();

        for (JsonNode record : resultData) {
            try {
                if (record.isArray() && record.size() >= 7) {
                    String nric = getArrayStringValue(record, 0);

                    NRICDataResult.ComcareData comcare = NRICDataResult.ComcareData.builder()
                            .idNo(nric)
                            .noticeNo(null) // Will be set later per notice
                            .assistanceStart(getArrayStringValue(record, 1))
                            .assistanceEnd(getArrayStringValue(record, 2))
                            .beneficiaryName(getArrayStringValue(record, 3))
                            .dataDate(getArrayStringValue(record, 4))
                            .paymentDate(getArrayStringValue(record, 5))
                            .referencePeriod(getArrayStringValue(record, 6))
                            .source("FSC")
                            .build();

                    results.computeIfAbsent(nric, k -> new ArrayList<>()).add(comcare);
                }
            } catch (Exception e) {
                log.error("Error parsing FSC record: {}", e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Parse CCC batch response and group by NRIC
     */
    private Map<String, List<NRICDataResult.ComcareData>> parseCCCBatchResponse(JsonNode resultData) {
        Map<String, List<NRICDataResult.ComcareData>> results = new HashMap<>();

        for (JsonNode record : resultData) {
            try {
                if (record.isArray() && record.size() >= 7) {
                    String nric = getArrayStringValue(record, 0);

                    NRICDataResult.ComcareData comcare = NRICDataResult.ComcareData.builder()
                            .idNo(nric)
                            .noticeNo(null) // Will be set later per notice
                            .assistanceStart(getArrayStringValue(record, 1))
                            .assistanceEnd(getArrayStringValue(record, 2))
                            .beneficiaryName(getArrayStringValue(record, 3))
                            .dataDate(getArrayStringValue(record, 4))
                            .paymentDate(getArrayStringValue(record, 5))
                            .referencePeriod(getArrayStringValue(record, 6))
                            .source("CCC")
                            .build();

                    results.computeIfAbsent(nric, k -> new ArrayList<>()).add(comcare);
                }
            } catch (Exception e) {
                log.error("Error parsing CCC record: {}", e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Execute query with retry logic
     */
    private JsonNode executeQueryWithRetry(String sql, String context) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("Executing batch query (attempt {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, context);

                CompletableFuture<JsonNode> future = dataHiveUtil.executeQueryAsyncDataOnly(sql);
                JsonNode result = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                log.info("Batch query successful on attempt {}. Response size: {} records",
                        attempt, result != null && result.isArray() ? result.size() : 0);

                return result;

            } catch (Exception e) {
                log.warn("Batch query failed on attempt {}/{} for {}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, context, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        break;
                    }
                }
            }
        }

        log.error("All retry attempts exhausted for batch query: {}", context);
        return null;
    }

    /**
     * Split list into batches
     */
    private List<List<String>> splitIntoBatches(List<String> nrics, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < nrics.size(); i += batchSize) {
            batches.add(nrics.subList(i, Math.min(i + batchSize, nrics.size())));
        }
        return batches;
    }

    /**
     * Get string value from array node at index
     */
    private String getArrayStringValue(JsonNode arrayNode, int index) {
        if (arrayNode == null || !arrayNode.isArray() || index >= arrayNode.size()) {
            return null;
        }
        JsonNode field = arrayNode.get(index);
        return field.isNull() ? null : field.asText();
    }
}
