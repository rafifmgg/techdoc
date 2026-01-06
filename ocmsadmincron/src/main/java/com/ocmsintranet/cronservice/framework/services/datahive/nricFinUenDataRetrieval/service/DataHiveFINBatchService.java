package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FINDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FinNoticeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Batch processing service for DataHive FIN data lookups
 * Processes multiple FIN records in batches to avoid rate limiting
 *
 * Main benefits:
 * - Reduces API calls from N records to N/batch_size calls per data source
 * - Avoids DataHive rate limiting issues
 * - Improves overall performance with parallel execution
 * - Deduplicates FINs automatically
 *
 * Data sources queried:
 * - Death status (V_DH_MHA_FINDEATH)
 * - PR/SC conversion (V_DH_MHA_ALIVE_SCPR, V_DH_MHA_SCGRANT)
 * - Work permits (V_DH_MOM_EPWORKPASS, V_DH_MOM_EPFOREIGNER, V_DH_MOM_WPWORKER, V_DH_MOM_WPWORKPASS)
 * - Passes (V_DH_MHA_LTVP, V_DH_MHA_STP)
 * - Prison/Custody data (via DataHiveCommonService)
 */
@Slf4j
@Service
public class DataHiveFINBatchService {

    private static final int BATCH_SIZE = 100; // Adjust based on DataHive limits
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long BATCH_TIMEOUT_SECONDS = 90;

    // Table names
    private static final String DEATH_TABLE = "V_DH_MHA_FINDEATH";
    private static final String PR_TABLE = "V_DH_MHA_ALIVE_SCPR";
    private static final String SC_TABLE = "V_DH_MHA_SCGRANT";
    private static final String EP_WORKPASS_TABLE = "V_DH_MOM_EPWORKPASS";
    private static final String EP_FOREIGNER_TABLE = "V_DH_MOM_EPFOREIGNER";
    private static final String WP_WORKER_TABLE = "V_DH_MOM_WPWORKER";
    private static final String WP_WORKPASS_TABLE = "V_DH_MOM_WPWORKPASS";
    private static final String LTVP_TABLE = "V_DH_MHA_LTVP";
    private static final String STP_TABLE = "V_DH_MHA_STP";

    private final DataHiveUtil dataHiveUtil;
    private final DataHiveCommonService commonService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DataHiveFINBatchService(
            DataHiveUtil dataHiveUtil,
            DataHiveCommonService commonService) {
        this.dataHiveUtil = dataHiveUtil;
        this.commonService = commonService;
    }

    /**
     * Batch retrieve FIN data for multiple FIN/notice pairs
     * Groups by unique FINs and queries in batches to avoid rate limits
     *
     * @param finNoticePairs List of FIN/notice pairs to lookup
     * @return Map of "fin|noticeNo" -> FINDataResult
     */
    public Map<String, FINDataResult> batchRetrieveFINData(List<FinNoticeData> finNoticePairs) {
        log.info("Starting batch DataHive FIN lookup for {} pairs", finNoticePairs.size());

        Map<String, FINDataResult> resultsMap = new HashMap<>();

        // Step 1: Extract unique FINs
        Set<String> uniqueFins = finNoticePairs.stream()
                .map(FinNoticeData::getFin)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("Processing {} unique FINs in batch", uniqueFins.size());

        // Step 2: Query all data sources in parallel for all unique FINs
        Map<String, FINDataResult.DeathStatus> deathStatusByFin = new HashMap<>();
        Map<String, FINDataResult.PRStatus> prStatusByFin = new HashMap<>();
        Map<String, FINDataResult.WorkPermitInfo> epWorkPermitByFin = new HashMap<>();
        Map<String, Map<String, String>> epAddressByFin = new HashMap<>(); // FIN -> address fields
        Map<String, FINDataResult.WorkPermitInfo> wpWorkPermitByFin = new HashMap<>();
        Map<String, Map<String, String>> wpAddressByFin = new HashMap<>();
        Map<String, FINDataResult.PassInfo> ltvpByFin = new HashMap<>();
        Map<String, FINDataResult.PassInfo> stpByFin = new HashMap<>();
        Map<String, CommonDataResult> prisonByFin = new HashMap<>();

        try {
            // Run all data source queries in parallel
            List<CompletableFuture<Void>> futures = Arrays.asList(
                    CompletableFuture.runAsync(() -> deathStatusByFin.putAll(batchQueryDeathStatus(new ArrayList<>(uniqueFins)))),
                    CompletableFuture.runAsync(() -> prStatusByFin.putAll(batchQueryPRSCStatus(new ArrayList<>(uniqueFins)))),
                    CompletableFuture.runAsync(() -> {
                        Map<String, Object[]> epData = batchQueryEPData(new ArrayList<>(uniqueFins));
                        epData.forEach((fin, data) -> {
                            epWorkPermitByFin.put(fin, (FINDataResult.WorkPermitInfo) data[0]);
                            epAddressByFin.put(fin, (Map<String, String>) data[1]);
                        });
                    }),
                    CompletableFuture.runAsync(() -> {
                        Map<String, Object[]> wpData = batchQueryWPData(new ArrayList<>(uniqueFins));
                        wpData.forEach((fin, data) -> {
                            wpWorkPermitByFin.put(fin, (FINDataResult.WorkPermitInfo) data[0]);
                            wpAddressByFin.put(fin, (Map<String, String>) data[1]);
                        });
                    }),
                    CompletableFuture.runAsync(() -> ltvpByFin.putAll(batchQueryLTVP(new ArrayList<>(uniqueFins)))),
                    CompletableFuture.runAsync(() -> stpByFin.putAll(batchQuerySTP(new ArrayList<>(uniqueFins)))),
                    CompletableFuture.runAsync(() -> prisonByFin.putAll(batchQueryPrisonCustody(new ArrayList<>(uniqueFins))))
            );

            // Wait for all queries to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Batch queries completed - Death: {}, PR/SC: {}, EP: {}, WP: {}, LTVP: {}, STP: {}, Prison: {}",
                    deathStatusByFin.size(), prStatusByFin.size(), epWorkPermitByFin.size(),
                    wpWorkPermitByFin.size(), ltvpByFin.size(), stpByFin.size(), prisonByFin.size());

        } catch (Exception e) {
            log.error("Error during parallel batch queries: {}", e.getMessage(), e);
        }

        // Step 3: Combine results for each FIN/notice pair
        for (FinNoticeData pair : finNoticePairs) {
            String fin = pair.getFin();
            String noticeNo = pair.getNoticeNo();
            String cacheKey = pair.getCacheKey();

            try {
                FINDataResult result = FINDataResult.builder()
                        .appliedTransactionCodes(new ArrayList<>())
                        .generatedWorkItems(new ArrayList<>())
                        .rawResults(new HashMap<>())
                        .build();

                // Add death status
                FINDataResult.DeathStatus deathStatus = deathStatusByFin.get(fin);
                if (deathStatus != null) {
                    result.setDeathStatus(deathStatus);
                    if (deathStatus.getAppliedTransactionCode() != null) {
                        result.getAppliedTransactionCodes().add(deathStatus.getAppliedTransactionCode());
                    }
                }

                // Add PR/SC status
                FINDataResult.PRStatus prStatus = prStatusByFin.get(fin);
                if (prStatus != null) {
                    result.setPrStatus(prStatus);
                    if (prStatus.getAppliedTransactionCode() != null) {
                        result.getAppliedTransactionCodes().add(prStatus.getAppliedTransactionCode());
                    }
                }

                // Add work permit info (EP takes priority over WP)
                FINDataResult.WorkPermitInfo workPermit = epWorkPermitByFin.get(fin);
                Map<String, String> address = epAddressByFin.get(fin);
                if (workPermit == null) {
                    workPermit = wpWorkPermitByFin.get(fin);
                    address = wpAddressByFin.get(fin);
                }
                if (workPermit != null) {
                    workPermit.setNoticeNo(noticeNo);
                    workPermit.setIdNo(fin);
                    // Set address fields
                    if (address != null) {
                        workPermit.setBlockHouseNo(address.get("blockHouseNo"));
                        workPermit.setFloorNo(address.get("floorNo"));
                        workPermit.setUnitNo(address.get("unitNo"));
                        workPermit.setStreetName(address.get("streetName"));
                        workPermit.setPostalCode(address.get("postalCode"));
                    }
                    result.setWorkPermitInfo(workPermit);
                }

                // Add pass info (LTVP takes priority over STP)
                FINDataResult.PassInfo passInfo = ltvpByFin.get(fin);
                if (passInfo == null) {
                    passInfo = stpByFin.get(fin);
                }
                if (passInfo != null) {
                    passInfo.setNoticeNo(noticeNo);
                    passInfo.setIdNo(fin);
                    result.setPassInfo(passInfo);
                }

                // Add prison/custody data
                CommonDataResult prisonData = prisonByFin.get(fin);
                if (prisonData != null) {
                    result.setCommonData(prisonData);
                }

                resultsMap.put(cacheKey, result);

            } catch (Exception e) {
                log.error("Error combining results for {}: {}", cacheKey, e.getMessage(), e);
            }
        }

        log.info("Batch FIN lookup completed. Total results: {}", resultsMap.size());
        return resultsMap;
    }

    /**
     * Batch query death status for multiple FINs
     */
    private Map<String, FINDataResult.DeathStatus> batchQueryDeathStatus(List<String> fins) {
        log.info("Batch querying death status for {} FINs", fins.size());
        Map<String, FINDataResult.DeathStatus> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);
        log.info("Processing {} death status batches", batches.size());

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                String sql = buildBatchQuery(DEATH_TABLE, "FIN", batch,
                        "FIN, DATE_OF_DEATH, REFERENCE_PERIOD");

                JsonNode resultData = executeQueryWithRetry(sql, "Death status batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    for (JsonNode record : resultData) {
                        if (record.isArray() && record.size() >= 3) {
                            String fin = getArrayStringValue(record, 0);
                            String dateOfDeathStr = getArrayStringValue(record, 1);

                            if (dateOfDeathStr != null && !dateOfDeathStr.isEmpty()) {
                                Date dateOfDeath = parseDate(dateOfDeathStr);
                                FINDataResult.DeathStatus deathStatus = FINDataResult.DeathStatus.builder()
                                        .fin(fin)
                                        .dateOfDeath(dateOfDeath)
                                        .hasDeathDate(true)
                                        .build();
                                results.put(fin, deathStatus);
                            }
                        }
                    }
                    log.info("Death status batch {}/{} completed. Found {} FINs",
                            i + 1, batches.size(), results.size());
                }
            } catch (Exception e) {
                log.error("Error processing death status batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query PR/SC conversion status for multiple FINs
     */
    private Map<String, FINDataResult.PRStatus> batchQueryPRSCStatus(List<String> fins) {
        log.info("Batch querying PR/SC status for {} FINs", fins.size());
        Map<String, FINDataResult.PRStatus> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                // Query PR table
                String prSql = buildBatchQuery(PR_TABLE, "FIN", batch,
                        "UIN, PREVIOUS_FIN, DATE_PR_GRANTED, REFERENCE_PERIOD, FIN");

                JsonNode prData = executeQueryWithRetry(prSql, "PR status batch " + (i + 1));

                if (prData != null && prData.isArray()) {
                    for (JsonNode record : prData) {
                        if (record.isArray() && record.size() >= 5) {
                            String fin = getArrayStringValue(record, 4);
                            String uin = getArrayStringValue(record, 0);
                            Date datePrGranted = parseDate(getArrayStringValue(record, 2));

                            FINDataResult.PRStatus prStatus = FINDataResult.PRStatus.builder()
                                    .previousFin(fin)
                                    .uin(uin)
                                    .datePrGranted(datePrGranted)
                                    .isConverted(true)
                                    .conversionType("PR")
                                    .appliedTransactionCode("TS-OLD")
                                    .build();
                            results.put(fin, prStatus);
                        }
                    }
                }

                // Query SC table for FINs not found in PR
                List<String> remainingFins = batch.stream()
                        .filter(fin -> !results.containsKey(fin))
                        .collect(Collectors.toList());

                if (!remainingFins.isEmpty()) {
                    String scSql = buildBatchQuery(SC_TABLE, "FIN", remainingFins,
                            "UIN, PREVIOUS_FIN, SC_GRANT_DATE, REFERENCE_PERIOD, FIN");

                    JsonNode scData = executeQueryWithRetry(scSql, "SC status batch " + (i + 1));

                    if (scData != null && scData.isArray()) {
                        for (JsonNode record : scData) {
                            if (record.isArray() && record.size() >= 5) {
                                String fin = getArrayStringValue(record, 4);
                                String uin = getArrayStringValue(record, 0);
                                Date scGrantDate = parseDate(getArrayStringValue(record, 2));

                                FINDataResult.PRStatus prStatus = FINDataResult.PRStatus.builder()
                                        .previousFin(fin)
                                        .uin(uin)
                                        .scGrantDate(scGrantDate)
                                        .isConverted(true)
                                        .conversionType("SC")
                                        .appliedTransactionCode("TS-OLD")
                                        .build();
                                results.put(fin, prStatus);
                            }
                        }
                    }
                }

                log.info("PR/SC batch {}/{} completed. Found {} FINs", i + 1, batches.size(), results.size());

            } catch (Exception e) {
                log.error("Error processing PR/SC batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query EP (Employment Pass) data for multiple FINs
     * Returns array: [WorkPermitInfo, Address Map]
     */
    private Map<String, Object[]> batchQueryEPData(List<String> fins) {
        log.info("Batch querying EP data for {} FINs", fins.size());
        Map<String, Object[]> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                // Query EP work pass
                String epPassSql = buildBatchQuery(EP_WORKPASS_TABLE, "FIN", batch,
                        "CANCELLED_DT, EXPIRY_DT, WITHDRAWN_DT, APPLICATION_DT, " +
                        "PASSTYPE_CD, ISSUANCE_DT, UEN, ISACTIVE, FIN");

                JsonNode epPassData = executeQueryWithRetry(epPassSql, "EP workpass batch " + (i + 1));

                Map<String, JsonNode> activeEPsByFin = new HashMap<>();
                if (epPassData != null && epPassData.isArray()) {
                    for (JsonNode record : epPassData) {
                        if (record.isArray() && record.size() >= 9) {
                            String fin = getArrayStringValue(record, 8);
                            String isActive = getArrayStringValue(record, 7);
                            if ("1".equals(isActive)) {
                                activeEPsByFin.put(fin, record);
                            }
                        }
                    }
                }

                // Query EP foreigner details for active EPs
                if (!activeEPsByFin.isEmpty()) {
                    List<String> activeFins = new ArrayList<>(activeEPsByFin.keySet());
                    String epForeignerSql = buildBatchQuery(EP_FOREIGNER_TABLE, "FIN", activeFins,
                            "FOREIGNER_NAME, DATE_OF_BIRTH, SEX_CD, " +
                            "BLOCK_HOUSE_NO, STREET_NAME, FLOOR_NO, UNIT_NO, " +
                            "POSTAL_CODE_NO, LAST_CHANGE_ADDRESS_DT, ISACTIVE, FIN");

                    JsonNode epForeignerData = executeQueryWithRetry(epForeignerSql, "EP foreigner batch " + (i + 1));

                    if (epForeignerData != null && epForeignerData.isArray()) {
                        for (JsonNode foreignerRecord : epForeignerData) {
                            if (foreignerRecord.isArray() && foreignerRecord.size() >= 11) {
                                String fin = getArrayStringValue(foreignerRecord, 10);
                                JsonNode passRecord = activeEPsByFin.get(fin);
                                if (passRecord != null) {
                                    // Build work permit info
                                    FINDataResult.WorkPermitInfo workPermit = FINDataResult.WorkPermitInfo.builder()
                                            .passType(getArrayStringValue(passRecord, 4))
                                            .expiryDate(parseDate(getArrayStringValue(passRecord, 1)))
                                            .cancelledDate(parseDate(getArrayStringValue(passRecord, 0)))
                                            .withdrawnDate(parseDate(getArrayStringValue(passRecord, 2)))
                                            .applicationDate(parseDate(getArrayStringValue(passRecord, 3)))
                                            .issuanceDate(parseDate(getArrayStringValue(passRecord, 5)))
                                            .employerUen(getArrayStringValue(passRecord, 6))
                                            .isActive(true)
                                            .build();

                                    // Build address map
                                    Map<String, String> address = new HashMap<>();
                                    address.put("blockHouseNo", getArrayStringValue(foreignerRecord, 3));
                                    address.put("streetName", getArrayStringValue(foreignerRecord, 4));
                                    address.put("floorNo", getArrayStringValue(foreignerRecord, 5));
                                    address.put("unitNo", getArrayStringValue(foreignerRecord, 6));
                                    address.put("postalCode", getArrayStringValue(foreignerRecord, 7));

                                    results.put(fin, new Object[]{workPermit, address});
                                }
                            }
                        }
                    }
                }

                log.info("EP batch {}/{} completed. Found {} FINs", i + 1, batches.size(), results.size());

            } catch (Exception e) {
                log.error("Error processing EP batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query WP (Work Permit) data for multiple FINs
     * Returns array: [WorkPermitInfo, Address Map]
     */
    private Map<String, Object[]> batchQueryWPData(List<String> fins) {
        log.info("Batch querying WP data for {} FINs", fins.size());
        Map<String, Object[]> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                // Query WP worker
                String wpWorkerSql = buildBatchQuery(WP_WORKER_TABLE, "FIN", batch,
                        "FIN, WORKER_NAME, DATE_OF_BIRTH, SEX_CD, " +
                        "BLOCK_HOUSE_NO, STREET_NAME, FLOOR_NO, UNIT_NO, " +
                        "POSTAL_CODE_NO, LAST_CHANGE_ADDRESS_DT, ISACTIVE, WORK_PERMIT_NO");

                JsonNode wpWorkerData = executeQueryWithRetry(wpWorkerSql, "WP worker batch " + (i + 1));

                Map<String, Object[]> workerDataByFin = new HashMap<>();
                List<String> workPermitNos = new ArrayList<>();

                if (wpWorkerData != null && wpWorkerData.isArray()) {
                    for (JsonNode record : wpWorkerData) {
                        if (record.isArray() && record.size() >= 12) {
                            String fin = getArrayStringValue(record, 0);
                            String workPermitNo = getArrayStringValue(record, 11);
                            if (workPermitNo != null && !workPermitNo.isEmpty()) {
                                workPermitNos.add(workPermitNo);

                                // Store address data
                                Map<String, String> address = new HashMap<>();
                                address.put("blockHouseNo", getArrayStringValue(record, 4));
                                address.put("streetName", getArrayStringValue(record, 5));
                                address.put("floorNo", getArrayStringValue(record, 6));
                                address.put("unitNo", getArrayStringValue(record, 7));
                                address.put("postalCode", getArrayStringValue(record, 8));

                                workerDataByFin.put(workPermitNo, new Object[]{fin, address});
                            }
                        }
                    }
                }

                // Query WP workpass for work permit numbers
                if (!workPermitNos.isEmpty()) {
                    String wpPassSql = buildBatchQuery(WP_WORKPASS_TABLE, "WORK_PERMIT_NO", workPermitNos,
                            "EXPIRY_DT, REVOKED_CANCELLED_DT, WORK_PERMIT_NO, " +
                            "APPLICATION_DT, PASS_TYPE_CD, WORK_PASS_STATUS_CD, " +
                            "IPA_EXPIRY_DT, ISSUANCE_DT, UEN, EMPLOYER_NRICFIN, ISACTIVE");

                    JsonNode wpPassData = executeQueryWithRetry(wpPassSql, "WP workpass batch " + (i + 1));

                    if (wpPassData != null && wpPassData.isArray()) {
                        for (JsonNode record : wpPassData) {
                            if (record.isArray() && record.size() >= 11) {
                                String workPermitNo = getArrayStringValue(record, 2);
                                Object[] workerData = workerDataByFin.get(workPermitNo);
                                if (workerData != null) {
                                    String fin = (String) workerData[0];
                                    Map<String, String> address = (Map<String, String>) workerData[1];

                                    FINDataResult.WorkPermitInfo workPermit = FINDataResult.WorkPermitInfo.builder()
                                            .workPermitNo(workPermitNo)
                                            .passType(getArrayStringValue(record, 4))
                                            .expiryDate(parseDate(getArrayStringValue(record, 0)))
                                            .cancelledDate(parseDate(getArrayStringValue(record, 1)))
                                            .applicationDate(parseDate(getArrayStringValue(record, 3)))
                                            .workPassStatus(getArrayStringValue(record, 5))
                                            .ipaExpiryDate(parseDate(getArrayStringValue(record, 6)))
                                            .issuanceDate(parseDate(getArrayStringValue(record, 7)))
                                            .employerUen(getArrayStringValue(record, 8))
                                            .isActive("1".equals(getArrayStringValue(record, 10)))
                                            .build();

                                    results.put(fin, new Object[]{workPermit, address});
                                }
                            }
                        }
                    }
                }

                log.info("WP batch {}/{} completed. Found {} FINs", i + 1, batches.size(), results.size());

            } catch (Exception e) {
                log.error("Error processing WP batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query LTVP data for multiple FINs
     */
    private Map<String, FINDataResult.PassInfo> batchQueryLTVP(List<String> fins) {
        log.info("Batch querying LTVP for {} FINs", fins.size());
        Map<String, FINDataResult.PassInfo> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                String sql = buildBatchQuery(LTVP_TABLE, "FIN", batch,
                        "SEX, BLOCK, FLOOR, UNIT, STREET_NAME, BUILDING_NAME, " +
                        "POSTAL_CODE, ADDRESS_INDICATOR, DATEOF_EXPIRY, " +
                        "REFERENCE_PERIOD, FIN, PRINCIPAL_NAME, NON_WORK_PASS_TYPE, " +
                        "DATE_OF_ISSUE, DATE_OF_BIRTH");

                JsonNode resultData = executeQueryWithRetry(sql, "LTVP batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    for (JsonNode record : resultData) {
                        if (record.isArray() && record.size() >= 15) {
                            String fin = getArrayStringValue(record, 10);
                            FINDataResult.PassInfo passInfo = FINDataResult.PassInfo.builder()
                                    .passType(getArrayStringValue(record, 12))
                                    .dateOfIssue(parseDate(getArrayStringValue(record, 13)))
                                    .dateOfExpiry(parseDate(getArrayStringValue(record, 8)))
                                    .principalName(getArrayStringValue(record, 11))
                                    .sex(getArrayStringValue(record, 0))
                                    .dateOfBirth(parseDate(getArrayStringValue(record, 14)))
                                    .referencePeriod(getArrayStringValue(record, 9))
                                    .block(getArrayStringValue(record, 1))
                                    .floor(getArrayStringValue(record, 2))
                                    .unit(getArrayStringValue(record, 3))
                                    .streetName(getArrayStringValue(record, 4))
                                    .buildingName(getArrayStringValue(record, 5))
                                    .postalCode(getArrayStringValue(record, 6))
                                    .build();
                            results.put(fin, passInfo);
                        }
                    }
                    log.info("LTVP batch {}/{} completed. Found {} FINs", i + 1, batches.size(), results.size());
                }
            } catch (Exception e) {
                log.error("Error processing LTVP batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query STP data for multiple FINs
     */
    private Map<String, FINDataResult.PassInfo> batchQuerySTP(List<String> fins) {
        log.info("Batch querying STP for {} FINs", fins.size());
        Map<String, FINDataResult.PassInfo> results = new HashMap<>();

        List<List<String>> batches = splitIntoBatches(fins, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            try {
                String sql = buildBatchQuery(STP_TABLE, "FIN", batch,
                        "SEX, BLOCK, FLOOR, UNIT, STREET_NAME, BUILDING_NAME, " +
                        "POSTAL_CODE, ADDRESS_INDICATOR, DATE_OF_EXPIRY, " +
                        "REFERENCE_PERIOD, FIN, PRINCIPAL_NAME, NON_WORK_PASS_TYPE, " +
                        "DATE_OF_ISSUE, DATE_OF_BIRTH");

                JsonNode resultData = executeQueryWithRetry(sql, "STP batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    for (JsonNode record : resultData) {
                        if (record.isArray() && record.size() >= 15) {
                            String fin = getArrayStringValue(record, 10);
                            FINDataResult.PassInfo passInfo = FINDataResult.PassInfo.builder()
                                    .passType(getArrayStringValue(record, 12))
                                    .dateOfIssue(parseDate(getArrayStringValue(record, 13)))
                                    .dateOfExpiry(parseDate(getArrayStringValue(record, 8)))
                                    .principalName(getArrayStringValue(record, 11))
                                    .sex(getArrayStringValue(record, 0))
                                    .dateOfBirth(parseDate(getArrayStringValue(record, 14)))
                                    .referencePeriod(getArrayStringValue(record, 9))
                                    .block(getArrayStringValue(record, 1))
                                    .floor(getArrayStringValue(record, 2))
                                    .unit(getArrayStringValue(record, 3))
                                    .streetName(getArrayStringValue(record, 4))
                                    .buildingName(getArrayStringValue(record, 5))
                                    .postalCode(getArrayStringValue(record, 6))
                                    .build();
                            results.put(fin, passInfo);
                        }
                    }
                    log.info("STP batch {}/{} completed. Found {} FINs", i + 1, batches.size(), results.size());
                }
            } catch (Exception e) {
                log.error("Error processing STP batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query Prison/Custody data for multiple FINs
     * Uses batch implementation from DataHiveCommonService
     */
    private Map<String, CommonDataResult> batchQueryPrisonCustody(List<String> fins) {
        log.info("Batch querying Prison/Custody data for {} FINs", fins.size());
        Map<String, CommonDataResult> results = new HashMap<>();

        try {
            results = commonService.batchGetPrisonCustodyData("FIN", fins);
            log.info("Prison/Custody batch query completed. Found {} FINs with data", results.size());
        } catch (Exception e) {
            log.error("Error in batch prison/custody query: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Build batch query with IN clause
     */
    private String buildBatchQuery(String tableName, String columnName, List<String> values, String selectFields) {
        String valueList = values.stream()
                .map(val -> "'" + val + "'")
                .collect(Collectors.joining(", "));

        return String.format("SELECT %s FROM %s WHERE %s IN (%s)",
                selectFields, tableName, columnName, valueList);
    }

    /**
     * Execute query with retry logic
     */
    private JsonNode executeQueryWithRetry(String sql, String context) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.debug("Executing batch query (attempt {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, context);

                CompletableFuture<JsonNode> future = dataHiveUtil.executeQueryAsyncDataOnly(sql);
                JsonNode result = future.get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                log.debug("Batch query successful on attempt {}. Response size: {} records",
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
    private List<List<String>> splitIntoBatches(List<String> fins, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < fins.size(); i += batchSize) {
            batches.add(fins.subList(i, Math.min(i + batchSize, fins.size())));
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

    /**
     * Parse date string to Date object
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}
