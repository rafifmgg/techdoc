package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UENDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UenNoticeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Batch processing service for DataHive UEN data lookups
 * Processes multiple UEN records in batches to avoid rate limiting
 *
 * Main benefits:
 * - Reduces API calls from N records to N/batch_size calls per data source
 * - Avoids DataHive rate limiting issues
 * - Improves overall performance with parallel execution
 * - Deduplicates UENs automatically
 *
 * Data sources queried:
 * - Registered companies (V_DH_ACRA_FIRMINFO_R)
 * - De-registered companies (V_DH_ACRA_FIRMINFO_D)
 * - Shareholder information (V_DH_ACRA_SHAREHOLDER_GZ) - only for de-registered
 * - Board information (V_DH_ACRA_BOARD_INFO_FULL) - only for de-registered
 */
@Slf4j
@Service
public class DataHiveUENBatchService {

    private static final int BATCH_SIZE = 100; // Adjust based on DataHive limits
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long BATCH_TIMEOUT_SECONDS = 90;

    // Table names
    private static final String REGISTERED_COMPANIES_TABLE = "V_DH_ACRA_FIRMINFO_R";
    private static final String DEREGISTERED_COMPANIES_TABLE = "V_DH_ACRA_FIRMINFO_D";
    private static final String SHAREHOLDER_TABLE = "V_DH_ACRA_SHAREHOLDER_GZ";
    private static final String BOARD_INFO_TABLE = "V_DH_ACRA_BOARD_INFO_FULL";

    private final DataHiveUtil dataHiveUtil;

    public DataHiveUENBatchService(DataHiveUtil dataHiveUtil) {
        this.dataHiveUtil = dataHiveUtil;
    }

    /**
     * Batch retrieve UEN data for multiple UEN/notice pairs
     * Groups by unique UENs and queries in batches to avoid rate limits
     *
     * @param uenNoticePairs List of UEN/notice pairs to lookup
     * @return Map of "uen|noticeNo" -> UENDataResult
     */
    public Map<String, UENDataResult> batchRetrieveUENData(List<UenNoticeData> uenNoticePairs) {
        log.info("Starting batch DataHive UEN lookup for {} pairs", uenNoticePairs.size());

        Map<String, UENDataResult> resultsMap = new HashMap<>();

        // Step 1: Extract unique UENs
        Set<String> uniqueUens = uenNoticePairs.stream()
                .map(UenNoticeData::getUen)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("Processing {} unique UENs in batch", uniqueUens.size());

        // Step 2: Query registered companies FIRST for ALL UENs
        Map<String, UENDataResult.CompanyInfo> registeredCompaniesByUen = new HashMap<>();
        Map<String, UENDataResult.CompanyInfo> deregisteredCompaniesByUen = new HashMap<>();

        try {
            // Query registered companies first
            log.info("Querying registered companies table first for all {} UENs", uniqueUens.size());
            registeredCompaniesByUen.putAll(batchQueryRegisteredCompanies(new ArrayList<>(uniqueUens)));
            log.info("Found {} UENs in registered companies table", registeredCompaniesByUen.size());

            // Step 3: Collect UENs NOT found in registered table
            List<String> notFoundInRegistered = uniqueUens.stream()
                    .filter(uen -> !registeredCompaniesByUen.containsKey(uen))
                    .collect(Collectors.toList());

            // Step 4: Query de-registered table ONLY for UENs not found in registered
            if (!notFoundInRegistered.isEmpty()) {
                log.info("Querying de-registered companies table for {} UENs not found in registered table",
                        notFoundInRegistered.size());
                deregisteredCompaniesByUen.putAll(batchQueryDeregisteredCompanies(notFoundInRegistered));
                log.info("Found {} UENs in de-registered companies table", deregisteredCompaniesByUen.size());
            } else {
                log.info("All UENs found in registered table, skipping de-registered query");
            }

            log.info("Batch company queries completed - Registered: {}, De-registered: {}",
                    registeredCompaniesByUen.size(), deregisteredCompaniesByUen.size());

        } catch (Exception e) {
            log.error("Error during batch company queries: {}", e.getMessage(), e);
        }

        // Step 5: Get list of de-registered UENs for shareholder/board queries
        // COMMENTED OUT: Shareholder and board info retrieval disabled per user request
        /*
        List<String> deregisteredUens = deregisteredCompaniesByUen.keySet().stream()
                .collect(Collectors.toList());

        Map<String, List<UENDataResult.ShareholderInfo>> shareholdersByUen = new HashMap<>();
        Map<String, List<UENDataResult.BoardInfo>> boardInfoByUen = new HashMap<>();

        if (!deregisteredUens.isEmpty()) {
            log.info("Querying shareholder and board info for {} de-registered companies", deregisteredUens.size());

            try {
                // Run shareholder and board queries in parallel for de-registered companies
                CompletableFuture<Void> shareholderFuture = CompletableFuture.runAsync(() -> {
                    shareholdersByUen.putAll(batchQueryShareholders(deregisteredUens));
                });

                CompletableFuture<Void> boardFuture = CompletableFuture.runAsync(() -> {
                    boardInfoByUen.putAll(batchQueryBoardInfo(deregisteredUens));
                });

                // Wait for both queries to complete
                CompletableFuture.allOf(shareholderFuture, boardFuture).join();

                log.info("Batch shareholder/board queries completed - Shareholders: {}, Board: {}",
                        shareholdersByUen.size(), boardInfoByUen.size());

            } catch (Exception e) {
                log.error("Error during parallel shareholder/board queries: {}", e.getMessage(), e);
            }
        }
        */

        // Initialize empty maps since shareholder/board info retrieval is disabled
        Map<String, List<UENDataResult.ShareholderInfo>> shareholdersByUen = new HashMap<>();
        Map<String, List<UENDataResult.BoardInfo>> boardInfoByUen = new HashMap<>();
        log.info("Shareholder and board info retrieval is DISABLED - skipping queries");

        // Step 6: Combine results for each UEN/notice pair
        for (UenNoticeData pair : uenNoticePairs) {
            String uen = pair.getUen();
            String noticeNo = pair.getNoticeNo();
            String cacheKey = pair.getCacheKey();

            try {
                UENDataResult result = UENDataResult.builder()
                        .shareholderData(new ArrayList<>())
                        .boardData(new ArrayList<>())
                        .rawResults(new HashMap<>())
                        .tsAcrApplied(false)
                        .hasError(false)
                        .notFoundUENs(new ArrayList<>())
                        .build();

                // Check registered companies first
                UENDataResult.CompanyInfo companyInfo = registeredCompaniesByUen.get(uen);
                boolean isDeregistered = false;

                if (companyInfo != null) {
                    log.debug("UEN {} found in registered companies", uen);
                    companyInfo.setNoticeNo(noticeNo);
                    companyInfo.setDeregistered(false);
                } else {
                    // Check de-registered companies
                    companyInfo = deregisteredCompaniesByUen.get(uen);
                    if (companyInfo != null) {
                        log.debug("UEN {} found in de-registered companies", uen);
                        companyInfo.setNoticeNo(noticeNo);
                        companyInfo.setDeregistered(true);
                        isDeregistered = true;
                        result.setTsAcrApplied(true);
                    } else {
                        // Not found in either
                        log.warn("UEN {} not found in registered or de-registered companies", uen);
                        companyInfo = UENDataResult.CompanyInfo.builder()
                                .uen(uen)
                                .noticeNo(noticeNo)
                                .isFound(false)
                                .build();
                        result.setHasError(true);
                        result.setErrorMessage("UEN not found in ACRA registered or de-registered companies");
                        result.getNotFoundUENs().add(uen);
                    }
                }

                result.setCompanyInfo(companyInfo);

                // COMMENTED OUT: Shareholder and board info storage disabled per user request
                /*
                // Add shareholder and board info if de-registered
                if (isDeregistered) {
                    List<UENDataResult.ShareholderInfo> shareholders = shareholdersByUen.get(uen);
                    if (shareholders != null && !shareholders.isEmpty()) {
                        // Set noticeNo for each shareholder record
                        shareholders.forEach(sh -> sh.setNoticeNo(noticeNo));
                        result.getShareholderData().addAll(shareholders);
                    }

                    List<UENDataResult.BoardInfo> boardMembers = boardInfoByUen.get(uen);
                    if (boardMembers != null && !boardMembers.isEmpty()) {
                        // Set noticeNo for each board member record
                        boardMembers.forEach(bm -> bm.setNoticeNo(noticeNo));
                        result.getBoardData().addAll(boardMembers);
                    }
                }
                */

                resultsMap.put(cacheKey, result);

            } catch (Exception e) {
                log.error("Error combining results for {}: {}", cacheKey, e.getMessage(), e);
            }
        }

        log.info("Batch UEN lookup completed. Total results: {}", resultsMap.size());
        return resultsMap;
    }

    /**
     * Batch query registered companies for multiple UENs
     */
    private Map<String, UENDataResult.CompanyInfo> batchQueryRegisteredCompanies(List<String> uens) {
        log.info("Batch querying registered companies for {} UENs", uens.size());
        Map<String, UENDataResult.CompanyInfo> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(uens, BATCH_SIZE);
        log.info("Processing {} registered company batches of max {} UENs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing registered batch {}/{} with {} UENs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryRegistered(batch);
                log.debug("Registered batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "Registered batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, UENDataResult.CompanyInfo> batchResults = parseCompanyBatchResponse(resultData, false);
                    results.putAll(batchResults);
                    log.info("Registered batch {}/{} completed. Found {} UENs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No registered company data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing registered batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query de-registered companies for multiple UENs
     */
    private Map<String, UENDataResult.CompanyInfo> batchQueryDeregisteredCompanies(List<String> uens) {
        log.info("Batch querying de-registered companies for {} UENs", uens.size());
        Map<String, UENDataResult.CompanyInfo> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(uens, BATCH_SIZE);
        log.info("Processing {} de-registered company batches of max {} UENs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing de-registered batch {}/{} with {} UENs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryDeregistered(batch);
                log.debug("De-registered batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "De-registered batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, UENDataResult.CompanyInfo> batchResults = parseCompanyBatchResponse(resultData, true);
                    results.putAll(batchResults);
                    log.info("De-registered batch {}/{} completed. Found {} UENs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No de-registered company data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing de-registered batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query shareholder information for multiple UENs
     */
    private Map<String, List<UENDataResult.ShareholderInfo>> batchQueryShareholders(List<String> uens) {
        log.info("Batch querying shareholders for {} UENs", uens.size());
        Map<String, List<UENDataResult.ShareholderInfo>> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(uens, BATCH_SIZE);
        log.info("Processing {} shareholder batches of max {} UENs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing shareholder batch {}/{} with {} UENs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryShareholders(batch);
                log.debug("Shareholder batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "Shareholder batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, List<UENDataResult.ShareholderInfo>> batchResults = parseShareholderBatchResponse(resultData);
                    results.putAll(batchResults);
                    log.info("Shareholder batch {}/{} completed. Found {} UENs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No shareholder data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing shareholder batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Batch query board information for multiple UENs
     */
    private Map<String, List<UENDataResult.BoardInfo>> batchQueryBoardInfo(List<String> uens) {
        log.info("Batch querying board info for {} UENs", uens.size());
        Map<String, List<UENDataResult.BoardInfo>> results = new HashMap<>();

        // Split into batches
        List<List<String>> batches = splitIntoBatches(uens, BATCH_SIZE);
        log.info("Processing {} board info batches of max {} UENs", batches.size(), BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing board info batch {}/{} with {} UENs", i + 1, batches.size(), batch.size());

            try {
                String sql = buildBatchQueryBoardInfo(batch);
                log.debug("Board info batch query: {}", sql);

                JsonNode resultData = executeQueryWithRetry(sql, "Board info batch " + (i + 1));

                if (resultData != null && resultData.isArray()) {
                    Map<String, List<UENDataResult.BoardInfo>> batchResults = parseBoardInfoBatchResponse(resultData);
                    results.putAll(batchResults);
                    log.info("Board info batch {}/{} completed. Found {} UENs with data",
                            i + 1, batches.size(), batchResults.size());
                } else {
                    log.warn("No board info data returned for batch {}/{}", i + 1, batches.size());
                }

            } catch (Exception e) {
                log.error("Error processing board info batch {}/{}: {}", i + 1, batches.size(), e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Build batch query for registered companies with IN clause
     */
    private String buildBatchQueryRegistered(List<String> uens) {
        String uenList = uens.stream()
                .map(uen -> "'" + uen + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, " +
                "ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, ADDRESS_ONE, " +
                "ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, " +
                "ADDRESS_ONE_STREET_NAME, ADDRESS_ONE_BUILDING_NAME, UEN " +
                "FROM %s WHERE UEN IN (%s)",
                REGISTERED_COMPANIES_TABLE, uenList
        );
    }

    /**
     * Build batch query for de-registered companies with IN clause
     */
    private String buildBatchQueryDeregistered(List<String> uens) {
        String uenList = uens.stream()
                .map(uen -> "'" + uen + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, " +
                "ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, ADDRESS_ONE, " +
                "ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, " +
                "ADDRESS_ONE_STREET_NAME, ADDRESS_ONE_BUILDING_NAME, UEN " +
                "FROM %s WHERE UEN IN (%s)",
                DEREGISTERED_COMPANIES_TABLE, uenList
        );
    }

    /**
     * Build batch query for shareholders with IN clause
     */
    private String buildBatchQueryShareholders(List<String> uens) {
        String uenList = uens.stream()
                .map(uen -> "'" + uen + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN, " +
                "SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO, " +
                "COMPANY_UEN " +
                "FROM %s WHERE COMPANY_UEN IN (%s)",
                SHAREHOLDER_TABLE, uenList
        );
    }

    /**
     * Build batch query for board info with IN clause
     */
    private String buildBatchQueryBoardInfo(List<String> uens) {
        String uenList = uens.stream()
                .map(uen -> "'" + uen + "'")
                .collect(Collectors.joining(", "));

        return String.format(
                "SELECT POSITION_APPOINTMENT_DATE, POSITION_WITHDRAWN_WITHDRAWAL_DATE, " +
                "PERSON_IDENTIFICATION_NUMBER, ENTITY_UEN, POSITION_HELD_CODE, " +
                "REFERENCE_PERIOD " +
                "FROM %s WHERE ENTITY_UEN IN (%s)",
                BOARD_INFO_TABLE, uenList
        );
    }

    /**
     * Parse company batch response and group by UEN
     * Array format: [ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE,
     *                ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, ADDRESS_ONE,
     *                ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER,
     *                ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE,
     *                ADDRESS_ONE_STREET_NAME, ADDRESS_ONE_BUILDING_NAME, UEN]
     */
    private Map<String, UENDataResult.CompanyInfo> parseCompanyBatchResponse(JsonNode resultData, boolean isDeregistered) {
        Map<String, UENDataResult.CompanyInfo> results = new HashMap<>();

        for (JsonNode record : resultData) {
            try {
                if (record.isArray() && record.size() >= 14) {
                    String uen = getArrayStringValue(record, 13);

                    UENDataResult.CompanyInfo companyInfo = UENDataResult.CompanyInfo.builder()
                            .uen(uen)
                            .noticeNo(null) // Will be set later per notice
                            .entityName(getArrayStringValue(record, 0))
                            .entityType(getArrayStringValue(record, 1))
                            .registrationDate(parseDate(getArrayStringValue(record, 2)))
                            .deregistrationDate(parseDate(getArrayStringValue(record, 3)))
                            .entityStatusCode(getArrayStringValue(record, 4))
                            .companyTypeCode(getArrayStringValue(record, 5))
                            .addressOne(getArrayStringValue(record, 6))
                            .addressOneBlockHouseNumber(getArrayStringValue(record, 7))
                            .addressOneLevelNumber(getArrayStringValue(record, 8))
                            .addressOneUnitNumber(getArrayStringValue(record, 9))
                            .addressOnePostalCode(getArrayStringValue(record, 10))
                            .addressOneStreetName(getArrayStringValue(record, 11))
                            .addressOneBuildingName(getArrayStringValue(record, 12))
                            .isFound(true)
                            .isDeregistered(isDeregistered)
                            .build();

                    results.put(uen, companyInfo);
                }
            } catch (Exception e) {
                log.error("Error parsing company record: {}", e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Parse shareholder batch response and group by company UEN
     * Array format: [SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN,
     *                SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO, COMPANY_UEN]
     */
    private Map<String, List<UENDataResult.ShareholderInfo>> parseShareholderBatchResponse(JsonNode resultData) {
        Map<String, List<UENDataResult.ShareholderInfo>> results = new HashMap<>();

        for (JsonNode record : resultData) {
            try {
                if (record.isArray() && record.size() >= 5) {
                    String companyUen = getArrayStringValue(record, 4);

                    UENDataResult.ShareholderInfo shareholder = UENDataResult.ShareholderInfo.builder()
                            .companyUen(companyUen)
                            .noticeNo(null) // Will be set later per notice
                            .category(getArrayStringValue(record, 0))
                            .companyProfilUen(getArrayStringValue(record, 1))
                            .personIdNo(getArrayStringValue(record, 2))
                            .shareAllotedNo(getArrayStringValue(record, 3))
                            .build();

                    results.computeIfAbsent(companyUen, k -> new ArrayList<>()).add(shareholder);
                }
            } catch (Exception e) {
                log.error("Error parsing shareholder record: {}", e.getMessage(), e);
            }
        }

        return results;
    }

    /**
     * Parse board info batch response and group by entity UEN
     * Array format: [POSITION_APPOINTMENT_DATE, POSITION_WITHDRAWN_WITHDRAWAL_DATE,
     *                PERSON_IDENTIFICATION_NUMBER, ENTITY_UEN, POSITION_HELD_CODE, REFERENCE_PERIOD]
     */
    private Map<String, List<UENDataResult.BoardInfo>> parseBoardInfoBatchResponse(JsonNode resultData) {
        Map<String, List<UENDataResult.BoardInfo>> results = new HashMap<>();

        for (JsonNode record : resultData) {
            try {
                if (record.isArray() && record.size() >= 6) {
                    String entityUen = getArrayStringValue(record, 3);

                    UENDataResult.BoardInfo boardInfo = UENDataResult.BoardInfo.builder()
                            .entityUen(entityUen)
                            .noticeNo(null) // Will be set later per notice
                            .positionAppointmentDate(parseDate(getArrayStringValue(record, 0)))
                            .positionWithdrawnDate(parseDate(getArrayStringValue(record, 1)))
                            .personIdNo(getArrayStringValue(record, 2))
                            .positionHeldCode(getArrayStringValue(record, 4))
                            .referencePeriod(DataHiveDateUtil.convertEpochDaysToLocalDateTime(getArrayStringValue(record, 5)))
                            .build();

                    results.computeIfAbsent(entityUen, k -> new ArrayList<>()).add(boardInfo);
                }
            } catch (Exception e) {
                log.error("Error parsing board info record: {}", e.getMessage(), e);
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
    private List<List<String>> splitIntoBatches(List<String> uens, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < uens.size(); i += batchSize) {
            batches.add(uens.subList(i, Math.min(i + batchSize, uens.size())));
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
     * Parse date string to LocalDateTime
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, formatter);
                return localDate.atStartOfDay();
            } catch (DateTimeParseException e) {
                // Continue to next formatter
            }
        }

        log.warn("Failed to parse date with all available formats: {}", dateStr);
        return null;
    }

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd-M-yyyy")
    };
}
