package com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveFINService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveUENService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveCommonService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveUENBatchService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveFINBatchService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UENDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FINDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UenNoticeData;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FinNoticeData;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.datahiveuenfin.models.OffenceNoticeRecord;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTemp;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTempService;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for DataHive UEN & FIN operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataHiveUenFinService {

    /**
     * Enum to represent address update result
     */
    private enum AddressUpdateResult {
        SUCCESS,        // Address updated successfully
        ALL_NULL,       // All address fields are null
        INVALID_POSTAL, // Postal code is not 6 digits
        INVALID_BLOCK,  // Block number is blank
        INVALID_FLOOR   // Floor number is blank
    }

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;
    private final DataHiveFINService dataHiveFINService;
    private final DataHiveUENService dataHiveUENService;
    private final DataHiveCommonService dataHiveCommonService;
    private final DataHiveUENBatchService dataHiveUENBatchService;
    private final DataHiveFINBatchService dataHiveFINBatchService;
    private final TableQueryService tableQueryService;
    private final OcmsSuspensionReasonService suspensionReasonService;
    private final OcmsNroTempService ocmsNroTempService;
    private final com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient;

    // Table names
    private static final String COMPANY_DETAIL_TABLE = "ocms_dh_acra_company_detail";
    private static final String SHAREHOLDER_INFO_TABLE = "ocms_dh_acra_shareholder_info";
    private static final String BOARD_INFO_TABLE = "ocms_dh_acra_board_info";
    private static final String OFFENCE_NOTICE_TABLE = "ocms_offence_notice_owner_driver";
    private static final String OFFENCE_NOTICE_TABLE_ADDR = "ocms_offence_notice_owner_driver_addr";
    private static final String WORK_PERMIT_TABLE = "ocms_dh_mom_work_permit";
    private static final String PASS_TABLE = "ocms_dh_mha_pass";

    // UEN status map for tracking registered vs deregistered (noticeNo -> "REGISTERED" or "DEREGISTERED")
    private final Map<String, String> uenStatusMap = new HashMap<>();

    /**
     * Get list of notices that need DataHive FIN & UEN processing
     *
     * Query executed:
     * SELECT onod.id_no, onod.id_type, onod.owner_driver_indicator
     * FROM ocms_valid_offence_notice AS von
     * JOIN ocms_offence_notice_owner_driver AS onod ON von.notice_no = onod.notice_no
     * WHERE von.next_processing_stage IN ('RD1','RD2','RR3','DN2','DR3')
     *   AND von.next_processing_date <= NEXT_DAY
     *   AND von.suspension_type IS NULL
     *   AND onod.id_type IN ('F', 'B')
     *   AND von.notice_no NOT IN (SELECT notice_no FROM ocms_nro_temp)
     *   -- AND onod.id_no NOT IN (SELECT id_no FROM ocms_hst) [Commented out - table only exists in local env]
     *   AND onod.offender_indicator = 'Y'
     *
     * @return List of offence notice records requiring DataHive processing
     */
    public List<OffenceNoticeRecord> getNoticesForDataHiveProcessing() {
        log.info("Executing query to get notices for DataHive FIN & UEN processing");

        try {
            String sql =
                "SELECT " +
                "onod.id_no AS idNo, " +
                "onod.id_type AS idType, " +
                "von.notice_no AS noticeNo, " +
                "von.next_processing_stage AS nextProcessingStage, " +
                "von.next_processing_date AS nextProcessingDate, " +
                "von.suspension_type AS suspensionType, " +
                "onod.offender_indicator AS offenderIndicator, " +
                "onod.owner_driver_indicator AS ownerDriverIndicator " +
                "FROM ocmsizmgr.ocms_valid_offence_notice AS von " +
                "JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS onod " +
                "ON von.notice_no = onod.notice_no " +
                "WHERE von.next_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3') " +
                "AND von.next_processing_date <= CAST(GETDATE() AS DATE) " +
                "AND von.suspension_type IS NULL " +
                "AND onod.id_type IN ('F', 'B') " +
                "AND von.notice_no NOT IN ( " +
                "    SELECT notice_no FROM ocmsizmgr.ocms_nro_temp " +
                ") " +
                // Commented out - ocms_hst table only exists in local environment
                // "AND onod.id_no NOT IN ( " +
                // "    SELECT id_no FROM ocmsizmgr.ocms_hst " +
                // ") " +
                "AND onod.offender_indicator = 'Y'";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            // Convert SQL results to OffenceNoticeRecord objects
            List<OffenceNoticeRecord> records = results.stream()
                .map(this::mapToOffenceNoticeRecord)
                .collect(Collectors.toList());

            log.info("Successfully retrieved {} notices for DataHive processing", records.size());
            return records;
        } catch (Exception e) {
            log.error("Error retrieving notices for DataHive processing", e);
            throw new RuntimeException("Failed to retrieve notices for DataHive processing", e);
        }
    }

    /**
     * Maps SQL result row to OffenceNoticeRecord object
     *
     * @param row SQL result row as Map
     * @return OffenceNoticeRecord object
     */
    private OffenceNoticeRecord mapToOffenceNoticeRecord(Map<String, Object> row) {
        log.debug("Mapping SQL row to OffenceNoticeRecord");

        // Convert Timestamp to java.sql.Date
        java.sql.Date processingDate = null;
        if (row.get("nextProcessingDate") != null) {
            processingDate = new java.sql.Date(((java.sql.Timestamp) row.get("nextProcessingDate")).getTime());
        }

        return OffenceNoticeRecord.builder()
            .idNo((String) row.get("idNo"))
            .idType((String) row.get("idType"))
            .noticeNo((String) row.get("noticeNo"))
            .nextProcessingStage((String) row.get("nextProcessingStage"))
            .nextProcessingDate(processingDate)
            .suspensionType((String) row.get("suspensionType"))
            .offenderIndicator((String) row.get("offenderIndicator"))
            .ownerDriverIndicator((String) row.get("ownerDriverIndicator"))
            .createdDate(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()))
            .processStatus("PENDING")
            .build();
    }

    /**
     * Process UEN record by retrieving and storing data from DataHive
     * Follows the same pattern as ToppanUploadHelper.enrichNoticesWithDataHive()
     *
     * @param record the UEN record to process
     * @return true if processing was successful
     */
    public boolean processUenRecord(OffenceNoticeRecord record) {
        log.info("Processing UEN record for notice: {}", record.getNoticeNo());

        try {
            // Retrieve UEN data from DataHive using Snowflake
            // DataHive service automatically handles database updates and storage
            log.debug("Querying DataHive UEN service for notice: {}", record.getNoticeNo());

            Object uenData = dataHiveUENService.retrieveUENData(record.getIdNo(), record.getNoticeNo());

            if (uenData != null) {
                log.info("Successfully retrieved and stored UEN data from DataHive for notice {}",
                        record.getNoticeNo());
                return true;
            } else {
                log.warn("No UEN data returned from DataHive for notice {}", record.getNoticeNo());
                return false;
            }

        } catch (Exception e) {
            log.error("Error processing UEN record for notice: {}: {}",
                     record.getNoticeNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process FIN record by retrieving and storing data from DataHive
     * Follows the same pattern as ToppanUploadHelper.enrichNoticesWithDataHive()
     *
     * @param record the FIN record to process
     * @return true if processing was successful
     */
    public boolean processFinRecord(OffenceNoticeRecord record) {
        log.info("Processing FIN record for notice: {}", record.getNoticeNo());

        try {
            // Retrieve FIN data from DataHive using Snowflake
            // DataHive service automatically handles database updates and storage
            log.debug("Querying DataHive FIN service for notice: {}", record.getNoticeNo());

            // Get offence date from record - required for FIN data retrieval
            java.util.Date offenceDate = record.getNextProcessingDate() != null ?
                new java.util.Date(record.getNextProcessingDate().getTime()) : new java.util.Date();

            // Assume owner/driver indicator is 'O' for owner (default)
            // This matches the pattern in ToppanUploadHelper
            Object finData = dataHiveFINService.retrieveFINData(
                record.getIdNo(),
                record.getNoticeNo(),
                "O", // ownerDriverIndicator
                offenceDate
            );

            if (finData != null) {
                log.info("Successfully retrieved and stored FIN data from DataHive for notice {}",
                        record.getNoticeNo());
                return true;
            } else {
                log.warn("No FIN data returned from DataHive for notice {}", record.getNoticeNo());
                return false;
            }

        } catch (Exception e) {
            log.error("Error processing FIN record for notice: {}: {}",
                     record.getNoticeNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retrieve UEN data from DataHive using Snowflake
     * Note: DataHiveUENService.retrieveUENData() automatically handles storage
     *
     * @return true if data retrieval was successful
     */
    public boolean retrieveUenDataFromDataHive() {
        log.info("DataHiveUENService handles both retrieval and storage automatically");
        // This method is kept for backward compatibility but delegates to processUenRecord
        return true;
    }

    /**
     * Store UEN data retrieved from DataHive
     * Note: DataHiveUENService.retrieveUENData() automatically handles storage
     *
     * @return true if data storage was successful
     */
    public boolean storeUenData() {
        log.info("DataHiveUENService handles both retrieval and storage automatically");
        // This method is kept for backward compatibility
        return true;
    }

    /**
     * Retrieve FIN data from DataHive using Snowflake
     * Note: DataHiveFINService.retrieveFINData() automatically handles storage
     *
     * @return true if data retrieval was successful
     */
    public boolean retrieveFinDataFromDataHive() {
        log.info("DataHiveFINService handles both retrieval and storage automatically");
        // This method is kept for backward compatibility but delegates to processFinRecord
        return true;
    }

    /**
     * Store FIN data retrieved from DataHive
     * Note: DataHiveFINService.retrieveFINData() automatically handles storage
     *
     * @return true if data storage was successful
     */
    public boolean storeFinData() {
        log.info("DataHiveFINService handles both retrieval and storage automatically");
        // This method is kept for backward compatibility
        return true;
    }

    /**
     * Retrieve common dataset (prison/custody data) from DataHive using Snowflake
     * This retrieves custody status, release dates, and offence information
     * that's common across NRIC and FIN ID types
     *
     * @param records List of records to process for common dataset
     * @return true if data retrieval was successful for all records
     */
    public boolean retrieveCommonDatasetFromDataHive(List<OffenceNoticeRecord> records) {
        log.info("Retrieving common dataset (prison/custody data) from DataHive for {} records", records.size());

        try {
            int successCount = 0;
            int errorCount = 0;

            for (OffenceNoticeRecord record : records) {
                try {
                    // Common dataset only applies to NRIC and FIN types, not UEN
                    if ("F".equals(record.getIdType())) {
                        // Retrieve prison/custody data for FIN
                        log.debug("Retrieving common dataset for FIN on notice: {}", record.getNoticeNo());

                        CommonDataResult commonData = dataHiveCommonService.getPrisonCustodyData(
                            "FIN",
                            record.getIdNo(),
                            record.getNoticeNo()
                        );

                        if (commonData != null) {
                            log.debug("Successfully retrieved common dataset for notice {}", record.getNoticeNo());
                            successCount++;
                        }
                    } else if ("S".equals(record.getIdType())) {
                        // Retrieve prison/custody data for NRIC (if applicable)
                        log.debug("Retrieving common dataset for NRIC on notice: {}", record.getNoticeNo());

                        CommonDataResult commonData = dataHiveCommonService.getPrisonCustodyData(
                            "NRIC",
                            record.getIdNo(),
                            record.getNoticeNo()
                        );

                        if (commonData != null) {
                            log.debug("Successfully retrieved common dataset for notice {}", record.getNoticeNo());
                            successCount++;
                        }
                    } else {
                        // UEN ('B' type) doesn't have prison/custody data
                        log.debug("Skipping common dataset for UEN type on notice {}", record.getNoticeNo());
                        successCount++; // Count as success since it's not applicable
                    }
                } catch (Exception e) {
                    log.error("Error retrieving common dataset for record {}: {}",
                            record.getNoticeNo(), e.getMessage(), e);
                    errorCount++;
                }
            }

            log.info("Common dataset retrieval completed: {} successful, {} errors out of {} records",
                    successCount, errorCount, records.size());

            return errorCount == 0;

        } catch (Exception e) {
            log.error("Error retrieving common dataset from DataHive", e);
            return false;
        }
    }

    /**
     * Store common dataset retrieved from DataHive
     * Note: DataHiveCommonService.getPrisonCustodyData() automatically handles storage
     *
     * @return true if data storage was successful
     */
    public boolean storeCommonDataset() {
        log.info("DataHiveCommonService handles both retrieval and storage automatically");
        // This method is kept for backward compatibility
        return true;
    }

    /**
     * Add processed records to ocms_nro_temp table
     * This marks the records as processed and prevents reprocessing
     * Follows the same pattern as recordRequestDriverParticulars in ToppanUploadHelper
     *
     * @param records List of offence notice records to add to the temp table
     * @return true if records were added successfully
     */
    public boolean addRecordsToNroTemp(List<OffenceNoticeRecord> records) {
        log.info("Adding {} processed records to ocms_nro_temp table", records.size());

        try {
            int successCount = 0;
            int skipCount = 0;

            for (OffenceNoticeRecord record : records) {
                String noticeNo = record.getNoticeNo();

                try {
                    // Check if already exists (prevent duplicates)
                    // Use getById() which returns Optional<OcmsNroTemp>
                    if (ocmsNroTempService.getById(noticeNo).isPresent()) {
                        log.debug("Notice {} already exists in ocms_nro_temp, skipping", noticeNo);
                        skipCount++;
                        continue;
                    }

                    // Create new OcmsNroTemp entity
                    OcmsNroTemp nroTemp = new OcmsNroTemp();
                    nroTemp.setNoticeNo(noticeNo);

                    // Save to database
                    ocmsNroTempService.save(nroTemp);
                    successCount++;

                    log.debug("Successfully added notice {} to ocms_nro_temp", noticeNo);

                } catch (Exception e) {
                    log.error("Error adding notice {} to ocms_nro_temp: {}", noticeNo, e.getMessage(), e);
                    // Continue processing other records even if one fails
                }
            }

            log.info("Successfully added {} records to ocms_nro_temp table ({} skipped as already existing)",
                    successCount, skipCount);

            // Return true if at least some records were processed successfully
            return successCount > 0 || skipCount > 0;

        } catch (Exception e) {
            log.error("Error adding records to ocms_nro_temp table", e);
            return false;
        }
    }

    /**
     * Check for interface errors during the DataHive synchronization process
     *
     * @return true if there are interface errors
     */
    public boolean hasInterfaceErrors() {
        log.info("Checking for interface errors");

        try {
            // TODO: Implement interface error checking logic
            // This should verify if there were any communication or data errors

            return false; // No errors by default
        } catch (Exception e) {
            log.error("Error checking for interface errors", e);
            return true; // Assume errors if check fails
        }
    }

    /**
     * BATCH PROCESSING: Process multiple UEN records in batch
     * Significantly improves performance by reducing API calls from N to N/100
     *
     * @param records List of UEN records to process
     * @return Map of noticeNo -> processing success status
     */
    @Transactional
    public Map<String, Boolean> batchProcessUenRecords(List<OffenceNoticeRecord> records) {
        log.info("Batch processing {} UEN records", records.size());
        Map<String, Boolean> results = new HashMap<>();

        try {
            // Clear previous UEN status map
            uenStatusMap.clear();

            // Prepare UEN/notice pairs for batch processing
            List<UenNoticeData> uenNoticePairs = records.stream()
                    .map(record -> UenNoticeData.builder()
                            .uen(record.getIdNo())
                            .noticeNo(record.getNoticeNo())
                            .build())
                    .collect(Collectors.toList());

            // Batch retrieve UEN data from DataHive
            Map<String, UENDataResult> batchResults = dataHiveUENBatchService.batchRetrieveUENData(uenNoticePairs);

            // Process each result and store to database
            for (OffenceNoticeRecord record : records) {
                String cacheKey = record.getIdNo() + "|" + record.getNoticeNo();
                UENDataResult result = batchResults.get(cacheKey);

                if (result != null) {
                    // Track UEN status (registered vs deregistered)
                    if (result.getCompanyInfo() != null && result.getCompanyInfo().isFound()) {
                        String status = result.getCompanyInfo().isDeregistered() ? "DEREGISTERED" : "REGISTERED";
                        uenStatusMap.put(record.getNoticeNo(), status);
                    }

                    boolean success = storeUenDataResult(result, record);
                    results.put(record.getNoticeNo(), success);
                } else {
                    log.warn("No batch result found for notice: {}", record.getNoticeNo());
                    results.put(record.getNoticeNo(), false);
                }
            }

            log.info("Batch UEN processing completed: {} successful out of {} records",
                    results.values().stream().filter(Boolean::booleanValue).count(), records.size());

        } catch (Exception e) {
            log.error("Error in batch UEN processing: {}", e.getMessage(), e);
            // Mark all as failed
            records.forEach(record -> results.put(record.getNoticeNo(), false));
        }

        return results;
    }

    /**
     * BATCH PROCESSING: Process multiple FIN records in batch
     * Significantly improves performance by reducing API calls from N to N/100
     *
     * @param records List of FIN records to process
     * @return Map of noticeNo -> processing success status
     */
    @Transactional
    public Map<String, Boolean> batchProcessFinRecords(List<OffenceNoticeRecord> records) {
        log.info("Batch processing {} FIN records", records.size());
        Map<String, Boolean> results = new HashMap<>();

        try {
            // Prepare FIN/notice pairs for batch processing
            List<FinNoticeData> finNoticePairs = records.stream()
                    .map(record -> FinNoticeData.builder()
                            .fin(record.getIdNo())
                            .noticeNo(record.getNoticeNo())
                            .ownerDriverIndicator("O") // Default to owner
                            .offenceDate(record.getNextProcessingDate() != null ?
                                    new java.util.Date(record.getNextProcessingDate().getTime()) :
                                    new java.util.Date())
                            .build())
                    .collect(Collectors.toList());

            // Batch retrieve FIN data from DataHive
            Map<String, FINDataResult> batchResults = dataHiveFINBatchService.batchRetrieveFINData(finNoticePairs);

            // Process each result and store to database
            for (OffenceNoticeRecord record : records) {
                String cacheKey = record.getIdNo() + "|" + record.getNoticeNo();
                FINDataResult result = batchResults.get(cacheKey);

                if (result != null) {
                    boolean success = storeFinDataResult(result, record);
                    results.put(record.getNoticeNo(), success);
                } else {
                    log.warn("No batch result found for notice: {}", record.getNoticeNo());
                    results.put(record.getNoticeNo(), false);
                }
            }

            log.info("Batch FIN processing completed: {} successful out of {} records",
                    results.values().stream().filter(Boolean::booleanValue).count(), records.size());

        } catch (Exception e) {
            log.error("Error in batch FIN processing: {}", e.getMessage(), e);
            // Mark all as failed
            records.forEach(record -> results.put(record.getNoticeNo(), false));
        }

        return results;
    }

    /**
     * Get the UEN status map populated during batch processing
     * Maps noticeNo to "REGISTERED" or "DEREGISTERED" status
     *
     * @return Map of noticeNo -> UEN status
     */
    public Map<String, String> getUenStatusMap() {
        return new HashMap<>(uenStatusMap); // Return copy to prevent external modification
    }

    /**
     * BATCH PROCESSING: Process common dataset (prison/custody) for multiple records in batch
     * Uses batch implementation from DataHiveCommonService
     *
     * @param records List of FIN/NRIC records to process (UEN records are skipped)
     * @return true if all records processed successfully
     */
    @Transactional
    public boolean batchProcessCommonDataset(List<OffenceNoticeRecord> records) {
        log.info("Batch processing common dataset for {} records", records.size());

        try {
            // Filter only FIN records (common dataset not applicable to UEN)
            List<OffenceNoticeRecord> finRecords = records.stream()
                    .filter(record -> "F".equals(record.getIdType()))
                    .collect(Collectors.toList());

            if (finRecords.isEmpty()) {
                log.info("No FIN records to process for common dataset");
                return true;
            }

            // Extract unique FINs
            List<String> fins = finRecords.stream()
                    .map(OffenceNoticeRecord::getIdNo)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("Batch querying common dataset for {} unique FINs", fins.size());

            // Batch retrieve common dataset
            Map<String, CommonDataResult> batchResults = dataHiveCommonService.batchGetPrisonCustodyData("FIN", fins);

            log.info("Batch common dataset retrieval completed: {} FINs with data", batchResults.size());

            // Note: DataHiveCommonService.batchGetPrisonCustodyData already stores to database
            // but doesn't set noticeNo per record, so we need to store again per notice
            int successCount = 0;
            int errorCount = 0;

            for (OffenceNoticeRecord record : finRecords) {
                try {
                    CommonDataResult result = batchResults.get(record.getIdNo());
                    if (result != null) {
                        // Update noticeNo for custody and incarceration data
                        if (result.getCustodyData() != null) {
                            result.getCustodyData().forEach(custody -> custody.setNoticeNo(record.getNoticeNo()));
                        }
                        if (result.getIncarcerationData() != null) {
                            result.getIncarcerationData().forEach(incarc -> incarc.setNoticeNo(record.getNoticeNo()));
                        }
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing common dataset for notice {}: {}",
                            record.getNoticeNo(), e.getMessage(), e);
                    errorCount++;
                }
            }

            log.info("Batch common dataset processing completed: {} successful, {} errors", successCount, errorCount);
            return errorCount == 0;

        } catch (Exception e) {
            log.error("Error in batch common dataset processing: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Store UEN data result to database
     * Stores company details, address, shareholder, and board info
     * Applies suspensions (TS-ACR for de-registered, TS-SYS for not found)
     * Note: Must be called within a @Transactional context
     */
    private boolean storeUenDataResult(UENDataResult result, OffenceNoticeRecord record) {
        try {
            // Check for errors (query failure, interface errors, etc.)
            if (result.isHasError()) {
                log.warn("UEN data has errors for notice {}: {}", record.getNoticeNo(), result.getErrorMessage());
                // Apply TS-SYS for interface errors or query failures
                applyTSSYS(record.getNoticeNo(), result.getErrorMessage() != null ?
                    result.getErrorMessage() : "DataHive interface error");
                return false;
            }

            // Check if company was found
            UENDataResult.CompanyInfo companyInfo = result.getCompanyInfo();
            if (companyInfo == null || !companyInfo.isFound()) {
                log.warn("Company not found for notice: {}, applying TS-SYS", record.getNoticeNo());
                applyTSSYS(record.getNoticeNo(), "UEN not found in DataHive");
                return false;
            }

            // 1. Store company details
            updateCompanyDetails(companyInfo);

            // 2. Store company address and check for null/invalid address data
            AddressUpdateResult addressUpdateResult = updateCompanyAddress(companyInfo, record.getNoticeNo(), record.getOwnerDriverIndicator());

            if (addressUpdateResult == AddressUpdateResult.ALL_NULL) {
                // All address fields are null, apply TS-SYS and do not update address
                log.warn("All address fields are null for notice: {}, applying TS-SYS", record.getNoticeNo());
                applyTSSYS(record.getNoticeNo(), "No address returned from DataHive");
            } else if (addressUpdateResult == AddressUpdateResult.INVALID_BLOCK) {
                // Block number is blank, address is updated but apply TS-SYS
                log.warn("Block number is blank for notice: {}, applying TS-SYS", record.getNoticeNo());
                applyTSSYS(record.getNoticeNo(), "Invalid House/Block Number");
            } else if (addressUpdateResult == AddressUpdateResult.INVALID_FLOOR) {
                // Floor number is blank, address is updated but apply TS-SYS
                log.warn("Floor number is blank for notice: {}, applying TS-SYS", record.getNoticeNo());
                applyTSSYS(record.getNoticeNo(), "Invalid Floor Number");
            } else if (addressUpdateResult == AddressUpdateResult.INVALID_POSTAL) {
                // Invalid postal code, address is updated but apply TS-SYS
                log.warn("Invalid postal code for notice: {}, applying TS-SYS", record.getNoticeNo());
                applyTSSYS(record.getNoticeNo(), "Invalid postal code");
            }

            // 3. Update company name in offence notice owner driver table
            updateCompanyName(companyInfo, record.getNoticeNo(), record.getIdNo());

            // COMMENTED OUT: Shareholder and board info storage disabled per user request
            /*
            // 4. Store shareholder info (only for de-registered)
            if (companyInfo.isDeregistered() && result.getShareholderData() != null) {
                for (UENDataResult.ShareholderInfo shareholder : result.getShareholderData()) {
                    updateShareholderInfo(shareholder);
                }

                // Check if Listed Company and update gazetted flag
                if ("LC".equals(companyInfo.getEntityType())) {
                    updateGazettedFlag(record.getNoticeNo());
                }
            }

            // 5. Store board info (only for de-registered)
            if (companyInfo.isDeregistered() && result.getBoardData() != null) {
                for (UENDataResult.BoardInfo boardInfo : result.getBoardData()) {
                    updateBoardInfo(boardInfo);
                }
            }
            */
            log.debug("Shareholder and board info storage is DISABLED - skipping");

            // 4. Apply suspensions
            if (companyInfo.isDeregistered()) {
                log.info("Company is de-registered for notice: {}, applying TS-ACR", record.getNoticeNo());
                applyTSACR(record.getNoticeNo());
            } else {
                log.info("Company is registered for notice: {}, no suspension needed", record.getNoticeNo());
            }

            log.debug("UEN data stored successfully for notice {}", record.getNoticeNo());
            return true;

        } catch (Exception e) {
            log.error("Error storing UEN data result for notice {}: {}", record.getNoticeNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Store FIN data result to database
     * Stores work permit, pass data, address, PR/SC conversion
     * Applies transaction codes (PS-RIP, PS-RP2, TS-OLD)
     */
    private boolean storeFinDataResult(FINDataResult result, OffenceNoticeRecord record) {
        try {
            String fin = record.getIdNo();
            String noticeNo = record.getNoticeNo();

            // 1. Store work permit data if available
            if (result.getWorkPermitInfo() != null) {
                updateWorkPermitData(result.getWorkPermitInfo(), fin, noticeNo);
            }

            // 2. Store pass data if available
            if (result.getPassInfo() != null) {
                updatePassData(result.getPassInfo(), fin, noticeNo);
            }

            // 3. Update address from work permit or pass info
            updateAddressForFIN(result, noticeNo);

            // 4. Handle PR/SC conversion
            if (result.getPrStatus() != null && result.getPrStatus().isConverted()) {
                updateNewNricForPRSC(result.getPrStatus(), noticeNo);
            }

            // 5. Apply transaction codes (these are logged, not stored in DB)
            if (result.getAppliedTransactionCodes() != null) {
                for (String transactionCode : result.getAppliedTransactionCodes()) {
                    log.info("Transaction code {} applied for notice: {}", transactionCode, noticeNo);
                }
            }

            log.debug("FIN data stored successfully for notice {}", record.getNoticeNo());
            return true;

        } catch (Exception e) {
            log.error("Error storing FIN data result for notice {}: {}", record.getNoticeNo(), e.getMessage(), e);
            return false;
        }
    }

    // ========== UEN STORAGE HELPER METHODS ==========

    private void updateCompanyDetails(UENDataResult.CompanyInfo companyInfo) {
        try {
            String uen = companyInfo.getUen();

            Map<String, Object> filters = new HashMap<>();
            filters.put("uen", uen);
            filters.put("noticeNo", companyInfo.getNoticeNo());

            List<Map<String, Object>> existingRecords = tableQueryService.query(COMPANY_DETAIL_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("entityName", companyInfo.getEntityName());
            fields.put("entityStatusCode", companyInfo.getEntityStatusCode());
            fields.put("companyTypeCode", companyInfo.getCompanyTypeCode());
            fields.put("entityType", companyInfo.getEntityType());
            fields.put("registrationDate", companyInfo.getRegistrationDate());
            fields.put("deregistrationDate", companyInfo.getDeregistrationDate());

            if (!existingRecords.isEmpty()) {
                tableQueryService.patch(COMPANY_DETAIL_TABLE, filters, fields);
                log.info("Updated company details for UEN: {}", uen);
            } else {
                fields.put("uen", uen);
                fields.put("noticeNo", companyInfo.getNoticeNo());
                tableQueryService.post(COMPANY_DETAIL_TABLE, fields);
                log.info("Created company details for UEN: {}", uen);
            }

        } catch (Exception e) {
            log.error("Error updating company details: {}", e.getMessage(), e);
        }
    }

    private AddressUpdateResult updateCompanyAddress(UENDataResult.CompanyInfo companyInfo, String noticeNumber, String ownerDriverIndicator) {
        try {
            String blockHouseNo = companyInfo.getAddressOneBlockHouseNumber();
            String levelNo = companyInfo.getAddressOneLevelNumber();
            String unitNo = companyInfo.getAddressOneUnitNumber();
            String streetName = companyInfo.getAddressOneStreetName();
            String buildingName = companyInfo.getAddressOneBuildingName();
            String postalCode = companyInfo.getAddressOnePostalCode();

            // Check if ALL address fields are null or empty
            boolean allNull = isNullOrEmpty(blockHouseNo) &&
                             isNullOrEmpty(levelNo) &&
                             isNullOrEmpty(unitNo) &&
                             isNullOrEmpty(streetName) &&
                             isNullOrEmpty(buildingName) &&
                             isNullOrEmpty(postalCode);

            if (allNull) {
                log.warn("All address fields are null for notice: {}", noticeNumber);
                return AddressUpdateResult.ALL_NULL;
            }

            // Perform validations
            boolean invalidBlock = isNullOrEmpty(blockHouseNo);
            boolean invalidFloor = isNullOrEmpty(levelNo);
            boolean invalidPostal = false;

            if (!isNullOrEmpty(postalCode)) {
                String postalCodeTrimmed = postalCode.trim();
                if (postalCodeTrimmed.length() != 6 || !postalCodeTrimmed.matches("\\d{6}")) {
                    invalidPostal = true;
                }
            }

            // Log validation issues
            if (invalidBlock) {
                log.warn("Block number is blank for notice: {}", noticeNumber);
            }
            if (invalidFloor) {
                log.warn("Floor number is blank for notice: {}", noticeNumber);
            }
            if (invalidPostal) {
                log.warn("Invalid postal code format for notice: {}", noticeNumber);
            }

            // Update address using native SQL to properly handle NULL values
            updateCompanyAddressWithNullSupport(noticeNumber, ownerDriverIndicator,
                blockHouseNo, levelNo, unitNo, streetName, buildingName, postalCode);

            // Return result based on validation priority
            if (invalidBlock) {
                return AddressUpdateResult.INVALID_BLOCK;
            }
            if (invalidFloor) {
                return AddressUpdateResult.INVALID_FLOOR;
            }
            if (invalidPostal) {
                return AddressUpdateResult.INVALID_POSTAL;
            }

            return AddressUpdateResult.SUCCESS;

        } catch (Exception e) {
            log.error("Error updating company address: {}", e.getMessage(), e);
            return AddressUpdateResult.SUCCESS; // Treat as success to not block processing
        }
    }

    /**
     * Helper method to check if a string is null or empty
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Update company address using native SQL to properly handle NULL values
     * This ensures that null values from DataHive properly override existing data
     * Note: Must be called within a @Transactional context
     */
    private void updateCompanyAddressWithNullSupport(String noticeNumber, String ownerDriverIndicator,
            String blockHouseNo, String levelNo, String unitNo,
            String streetName, String buildingName, String postalCode) {
        try {
            // Check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);
            filters.put("ownerDriverIndicator", ownerDriverIndicator);
            filters.put("typeOfAddress", "mha_reg");

            List<Map<String, Object>> existingRecords = tableQueryService.query(OFFENCE_NOTICE_TABLE_ADDR, filters);

            if (existingRecords.isEmpty()) {
                // Create new record using tableQueryService
                Map<String, Object> fields = new HashMap<>();
                fields.put("noticeNo", noticeNumber);
                fields.put("ownerDriverIndicator", ownerDriverIndicator);
                fields.put("typeOfAddress", "mha_reg");
                fields.put("blkHseNo", blockHouseNo);
                fields.put("floorNo", levelNo);
                fields.put("unitNo", unitNo);
                fields.put("streetName", streetName);
                fields.put("bldgName", buildingName);
                fields.put("postalCode", postalCode);

                tableQueryService.post(OFFENCE_NOTICE_TABLE_ADDR, fields);
                log.info("Created address for notice: {}, ownerDriverIndicator: {}", noticeNumber, ownerDriverIndicator);
            } else {
                // Update existing record using native SQL to support NULL values
                String sql = "UPDATE ocmsizmgr.ocms_offence_notice_owner_driver_addr " +
                           "SET blk_hse_no = ?, " +
                           "    floor_no = ?, " +
                           "    unit_no = ?, " +
                           "    street_name = ?, " +
                           "    bldg_name = ?, " +
                           "    postal_code = ?, " +
                           "    upd_user_id = ? " +
                           "WHERE notice_no = ? " +
                           "  AND owner_driver_indicator = ? " +
                           "  AND type_of_address = 'mha_reg'";

                int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter(1, blockHouseNo)
                    .setParameter(2, levelNo)
                    .setParameter(3, unitNo)
                    .setParameter(4, streetName)
                    .setParameter(5, buildingName)
                    .setParameter(6, postalCode)
                    .setParameter(7, SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
                    .setParameter(8, noticeNumber)
                    .setParameter(9, ownerDriverIndicator)
                    .executeUpdate();

                log.info("Updated address for notice: {}, ownerDriverIndicator: {} ({} rows affected)",
                        noticeNumber, ownerDriverIndicator, updatedCount);
            }

        } catch (Exception e) {
            log.error("Error updating company address with NULL support: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update company address", e);
        }
    }

    private void updateCompanyName(UENDataResult.CompanyInfo companyInfo, String noticeNumber, String idNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);
            filters.put("idNo", idNo);
            filters.put("offenderIndicator", "Y");

            Map<String, Object> fields = new HashMap<>();
            fields.put("name", companyInfo.getEntityName());

            tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, fields);
            log.info("Updated company name for notice: {}", noticeNumber);

        } catch (Exception e) {
            log.error("Error updating company name: {}", e.getMessage(), e);
        }
    }

    private void updateShareholderInfo(UENDataResult.ShareholderInfo shareholder) {
        try {
            // Skip records with NULL person ID - these are corporate shareholders
            String idNo;
            if (shareholder.getPersonIdNo() == null || shareholder.getPersonIdNo().trim().isEmpty()) {
                if (shareholder.getCompanyProfilUen() != null && !shareholder.getCompanyProfilUen().trim().isEmpty()) {
                    idNo = shareholder.getCompanyProfilUen().trim();
                    if (idNo.length() > 9) {
                        idNo = idNo.substring(0, 9);
                    }
                } else {
                    log.warn("Skipping shareholder with no ID for UEN: {}", shareholder.getCompanyUen());
                    return;
                }
            } else {
                idNo = shareholder.getPersonIdNo().trim();
                if (idNo.length() > 12) {
                    idNo = idNo.substring(0, 12);
                }
            }

            Map<String, Object> filters = new HashMap<>();
            filters.put("companyUen", shareholder.getCompanyUen());
            filters.put("noticeNo", shareholder.getNoticeNo());
            filters.put("personIdNo", idNo);

            List<Map<String, Object>> existingRecords = tableQueryService.query(SHAREHOLDER_INFO_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("shareAllottedNo", shareholder.getShareAllotedNo() != null ?
                    Integer.parseInt(shareholder.getShareAllotedNo()) : null);
            fields.put("companyProfileUen", shareholder.getCompanyProfilUen());
            fields.put("category", shareholder.getCategory());

            if (!existingRecords.isEmpty()) {
                tableQueryService.patch(SHAREHOLDER_INFO_TABLE, filters, fields);
                log.info("Updated shareholder for UEN: {}", shareholder.getCompanyUen());
            } else {
                fields.put("companyUen", shareholder.getCompanyUen());
                fields.put("noticeNo", shareholder.getNoticeNo());
                fields.put("personIdNo", idNo);
                tableQueryService.post(SHAREHOLDER_INFO_TABLE, fields);
                log.info("Created shareholder for UEN: {}", shareholder.getCompanyUen());
            }

        } catch (Exception e) {
            log.error("Error updating shareholder info: {}", e.getMessage(), e);
        }
    }

    private void updateBoardInfo(UENDataResult.BoardInfo board) {
        try {
            if (board.getPersonIdNo() == null || board.getPersonIdNo().trim().isEmpty()) {
                log.warn("Skipping board info with NULL person ID for UEN: {}", board.getEntityUen());
                return;
            }

            String idNo = board.getPersonIdNo().trim();
            if (idNo.length() > 12) {
                idNo = idNo.substring(0, 12);
            }

            Map<String, Object> filters = new HashMap<>();
            filters.put("entityUen", board.getEntityUen());
            filters.put("noticeNo", board.getNoticeNo());
            filters.put("personIdNo", idNo);

            List<Map<String, Object>> existingRecords = tableQueryService.query(BOARD_INFO_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("positionHeldCode", board.getPositionHeldCode());
            fields.put("positionAppointmentDate", board.getPositionAppointmentDate());
            fields.put("positionWithdrawnDate", board.getPositionWithdrawnDate());
            if (board.getReferencePeriod() != null) {
                fields.put("referencePeriod", board.getReferencePeriod());
            }

            if (!existingRecords.isEmpty()) {
                tableQueryService.patch(BOARD_INFO_TABLE, filters, fields);
                log.info("Updated board info for UEN: {}", board.getEntityUen());
            } else {
                fields.put("entityUen", board.getEntityUen());
                fields.put("noticeNo", board.getNoticeNo());
                fields.put("personIdNo", idNo);
                tableQueryService.post(BOARD_INFO_TABLE, fields);
                log.info("Created board info for UEN: {}", board.getEntityUen());
            }

        } catch (Exception e) {
            log.error("Error updating board info: {}", e.getMessage(), e);
        }
    }

    private void updateGazettedFlag(String noticeNumber) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);

            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("gazettedFlag", "Y");

            tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
            log.info("Updated gazetted_flag for notice: {}", noticeNumber);

        } catch (Exception e) {
            log.error("Error updating gazetted flag: {}", e.getMessage(), e);
        }
    }

    private void applyTSACR(String noticeNumber) {
        try {
            log.info("Applying TS-ACR suspension for notice: {}", noticeNumber);

            LocalDateTime currentDateTime = LocalDateTime.now();
            String formattedDate = currentDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Update ocms_valid_offence_notice
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);

            Map<String, Object> vonUpdateFields = new HashMap<>();
            vonUpdateFields.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            vonUpdateFields.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.ACR);
            vonUpdateFields.put("eprDateOfSuspension", formattedDate);
            vonUpdateFields.put("dueDateOfRevival", null);

            // PROCESS 4: Set is_sync to N to trigger cron batch sync to internet DB
            vonUpdateFields.put("isSync", "N");
            log.info("Setting isSync=N for TS-ACR suspension (notice: {}) to trigger internet sync via cron batch (Process 7)", noticeNumber);

            tableQueryService.patch("ocms_valid_offence_notice", filters, vonUpdateFields);

            // Apply suspension via API (replaces direct suspended_notice table creation)
            // Note: TS-ACR has NO revival date (daysToRevive = null) per de-registered flow diagram
            log.info("[TS-ACR] Calling suspension API for notice: {}", noticeNumber);
            Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                noticeNumber,
                SystemConstant.SuspensionType.TEMPORARY, // TS
                SystemConstant.SuspensionReason.ACR,
                "ID found in Deregistered Firms", // Per de-registered flow diagram
                SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                SystemConstant.Subsystem.OCMS_CODE,
                null, // caseNo
                null  // daysToRevive - NULL for TS-ACR per de-registered flow diagram
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("[TS-ACR] Successfully applied TS-ACR suspension via API for notice {}", noticeNumber);
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                log.error("[TS-ACR] API returned error for notice {}: {}", noticeNumber, errorMsg);
                throw new RuntimeException("Failed to apply TS-ACR suspension via API: " + errorMsg);
            }

            log.info("Applied TS-ACR suspension for notice {}", noticeNumber);

        } catch (Exception e) {
            log.error("Error applying TS-ACR suspension: {}", e.getMessage(), e);
        }
    }

    private void applyTSSYS(String noticeNumber, String errorMessage) {
        try {
            log.info("Applying TS-SYS suspension for notice: {}", noticeNumber);

            // Get no_of_days_for_revival from suspension reason table
            Integer daysForRevival = suspensionReasonService.getNoOfDaysForRevival(
                    SystemConstant.SuspensionType.TEMPORARY,
                    SystemConstant.SuspensionReason.SYS);

            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDate dueDateOfRevival = currentDateTime.toLocalDate().plusDays(daysForRevival);
            String formattedDueDate = dueDateOfRevival.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String formattedCurrentDate = currentDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Update ocms_valid_offence_notice
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);

            Map<String, Object> vonUpdateFields = new HashMap<>();
            vonUpdateFields.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            vonUpdateFields.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.SYS);
            vonUpdateFields.put("eprDateOfSuspension", formattedCurrentDate);
            vonUpdateFields.put("dueDateOfRevival", formattedDueDate);

            // PROCESS 4: Set is_sync to N to trigger cron batch sync to internet DB
            vonUpdateFields.put("isSync", "N");
            log.info("Setting isSync=N for TS-SYS suspension (notice: {}) to trigger internet sync via cron batch (Process 7)", noticeNumber);

            tableQueryService.patch("ocms_valid_offence_notice", filters, vonUpdateFields);

            // Apply suspension via API (replaces direct suspended_notice table creation)
            log.info("[TS-SYS] Calling suspension API for notice: {} with reason: {}", noticeNumber, errorMessage);
            Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                noticeNumber,
                SystemConstant.SuspensionType.TEMPORARY, // TS
                SystemConstant.SuspensionReason.SYS,
                errorMessage, // e.g., "UEN not found in DataHive" or "DataHive query error"
                SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                SystemConstant.Subsystem.OCMS_CODE,
                null, // caseNo
                daysForRevival
            );

            // Check API response
            if (suspensionApiClient.isSuccess(apiResponse)) {
                log.info("[TS-SYS] Successfully applied TS-SYS suspension via API for notice {}", noticeNumber);
            } else {
                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                log.error("[TS-SYS] API returned error for notice {}: {}", noticeNumber, errorMsg);
                throw new RuntimeException("Failed to apply TS-SYS suspension via API: " + errorMsg);
            }

            log.info("Applied TS-SYS suspension for notice {}", noticeNumber);

        } catch (Exception e) {
            log.error("Error applying TS-SYS suspension: {}", e.getMessage(), e);
        }
    }

    // ========== FIN STORAGE HELPER METHODS ==========

    private void updateWorkPermitData(FINDataResult.WorkPermitInfo workPermit, String fin, String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", fin);
            filters.put("noticeNo", noticeNo);

            List<Map<String, Object>> existingRecords = tableQueryService.query(WORK_PERMIT_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("workPermitNo", workPermit.getWorkPermitNo());
            fields.put("passType", workPermit.getPassType());
            fields.put("expiryDate", workPermit.getExpiryDate());
            fields.put("cancelledDate", workPermit.getCancelledDate());
            fields.put("employerUen", workPermit.getEmployerUen());
            fields.put("issuanceDate", workPermit.getIssuanceDate());
            fields.put("applicationDate", workPermit.getApplicationDate());
            fields.put("ipaExpiryDate", workPermit.getIpaExpiryDate());
            fields.put("workPassStatus", workPermit.getWorkPassStatus());
            fields.put("withdrawnDate", workPermit.getWithdrawnDate());

            if (!existingRecords.isEmpty()) {
                tableQueryService.patch(WORK_PERMIT_TABLE, filters, fields);
                log.info("Updated work permit for FIN: {}", fin);
            } else {
                fields.put("idNo", fin);
                fields.put("noticeNo", noticeNo);
                tableQueryService.post(WORK_PERMIT_TABLE, fields);
                log.info("Created work permit for FIN: {}", fin);
            }

        } catch (Exception e) {
            log.error("Error updating work permit data: {}", e.getMessage(), e);
        }
    }

    private void updatePassData(FINDataResult.PassInfo passInfo, String fin, String noticeNo) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);

            List<Map<String, Object>> existingRecords = tableQueryService.query(PASS_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("idNo", fin);
            fields.put("mhaPassExpiryDate", passInfo.getDateOfExpiry());

            if (!existingRecords.isEmpty()) {
                tableQueryService.patch(PASS_TABLE, filters, fields);
                log.info("Updated pass data for FIN: {}", fin);
            } else {
                fields.put("noticeNo", noticeNo);
                tableQueryService.post(PASS_TABLE, fields);
                log.info("Created pass data for FIN: {}", fin);
            }

        } catch (Exception e) {
            log.error("Error updating pass data: {}", e.getMessage(), e);
        }
    }

    private void updateAddressForFIN(FINDataResult result, String noticeNo) {
        try {
            String blockHouseNo = null;
            String floor = null;
            String unit = null;
            String streetName = null;
            String buildingName = null;
            String postalCode = null;

            // Priority: Work permit address > Pass address
            if (result.getWorkPermitInfo() != null) {
                FINDataResult.WorkPermitInfo permit = result.getWorkPermitInfo();
                blockHouseNo = permit.getBlockHouseNo();
                floor = permit.getFloorNo();
                unit = permit.getUnitNo();
                streetName = permit.getStreetName();
                postalCode = permit.getPostalCode();
            } else if (result.getPassInfo() != null) {
                FINDataResult.PassInfo pass = result.getPassInfo();
                blockHouseNo = pass.getBlock();
                floor = pass.getFloor();
                unit = pass.getUnit();
                streetName = pass.getStreetName();
                buildingName = pass.getBuildingName();
                postalCode = pass.getPostalCode();
            }

            // Update address if found
            if (blockHouseNo != null || streetName != null || postalCode != null) {
                Map<String, Object> filters = new HashMap<>();
                filters.put("noticeNo", noticeNo);

                Map<String, Object> updateFields = new HashMap<>();
                if (blockHouseNo != null) updateFields.put("regBlkHseNo", blockHouseNo);
                if (floor != null) updateFields.put("regFloor", floor);
                if (unit != null) updateFields.put("regUnit", unit);
                if (streetName != null) updateFields.put("regStreet", streetName);
                if (buildingName != null) updateFields.put("regBldg", buildingName);
                if (postalCode != null) updateFields.put("regPostalCode", postalCode);

                tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
                log.info("Updated address for FIN notice: {}", noticeNo);
            }

        } catch (Exception e) {
            log.error("Error updating address for FIN: {}", e.getMessage(), e);
        }
    }

    private void updateNewNricForPRSC(FINDataResult.PRStatus prStatus, String noticeNo) {
        try {
            if (prStatus.getUin() != null && !prStatus.getUin().isEmpty()) {
                Map<String, Object> filters = new HashMap<>();
                filters.put("noticeNo", noticeNo);

                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("newNric", prStatus.getUin());

                tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
                log.info("Updated new_nric for PR/SC conversion, notice: {}", noticeNo);
            }

        } catch (Exception e) {
            log.error("Error updating new_nric for PR/SC: {}", e.getMessage(), e);
        }
    }
}
