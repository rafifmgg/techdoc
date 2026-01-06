package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.upload.helpers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTemp;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTempService;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for MHA NRIC data operations.
 * Handles data retrieval for MHA NRIC upload workflow.
 *
 * Implements the SQL logic:
 * von.next_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3')
 * AND von.next_processing_date <= next_day
 * AND (von.suspension_type IS NULL
 *      OR (von.suspension_type = 'TS'
 *          AND von.epr_reason_of_suspension IN ('HST', 'UNC')))
 * AND von.notice_no NOT IN (SELECT notice_no FROM ocms_nro_temp)
 * AND onod.id_no NOT IN (SELECT id_no FROM ocms_hst)
 * AND onod.offender_indicator = 'Y'
 * AND onod.id_type IN ('F', 'B')
 *
 * von = ocms_valid_offence_notice
 * onod = ocms_offence_notice_owner_driver
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MhaNricDataHelper {

    private final TableQueryService tableQueryService;
    private final OcmsNroTempService ocmsNroTempService;

    /**
     * Queries the database for valid offence notices using default next day (current time + 1 day).
     * 
     * @return List of valid offence notices that meet the criteria
     */
    public List<Map<String, Object>> queryMhaNricData() {
        return queryMhaNricData(LocalDateTime.now().plusDays(1));
    }
    
    /**
     * Queries the database for valid offence notices.
     * 
     * @param nextDay The date to use for filtering notices by nextProcessingDate
     * @return List of valid offence notices that meet the criteria
     */
    public List<Map<String, Object>> queryMhaNricData(LocalDateTime nextDay) {
        try {
            log.info("Querying MHA NRIC data for processing date <= {}", nextDay);
            
            // Step 1: Get all notice numbers from excluded tables
            Set<String> excludedNoticesFromNroTemp = getExcludedNoticesFromNroTemp();
            Set<String> excludedIdNumbersFromHst = getExcludedIdNumbersFromHst();
            
            log.debug("Excluded notices from NRO temp: {}", excludedNoticesFromNroTemp.size());
            log.debug("Excluded ID numbers from HST: {}", excludedIdNumbersFromHst.size());
            
            // Step 2: Query the main table with basic filters
            List<Map<String, Object>> validOffenceNotices = queryValidOffenceNoticesBasic(nextDay, excludedNoticesFromNroTemp);
            
            if (validOffenceNotices.isEmpty()) {
                log.info("No valid offence notices found after basic filtering");
                return validOffenceNotices;
            }
            
            log.debug("Found {} notices after basic filtering", validOffenceNotices.size());
            
            // Step 3: Apply owner/driver filtering
            List<Map<String, Object>> filteredNotices = filterByOwnerDriverCriteria(validOffenceNotices, excludedIdNumbersFromHst);
            
            log.info("Found {} valid offence notices after all filtering", filteredNotices.size());
            
            return filteredNotices;
            
        } catch (Exception e) {
            log.error("Error querying MHA NRIC data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get excluded notice numbers from ocms_nro_temp table
     */
    private Set<String> getExcludedNoticesFromNroTemp() {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("$limit", 10000);
            
            List<Map<String, Object>> nroTempRecords = tableQueryService.query("ocms_nro_temp", filters);
            
            Set<String> excludedNotices = nroTempRecords.stream()
                .map(record -> (String) record.get("noticeNo"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toSet());
                
            log.debug("Found {} excluded notice numbers from ocms_nro_temp", excludedNotices.size());
            return excludedNotices;
            
        } catch (Exception e) {
            log.error("Error querying ocms_nro_temp for exclusions: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Get excluded ID numbers from ocms_hst table
     */
    private Set<String> getExcludedIdNumbersFromHst() {
        return Collections.emptySet();
        /*try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("$limit", 10000);
            
            List<Map<String, Object>> hstRecords = tableQueryService.query("ocms_hst", filters);
            
            log.debug("HST records raw data sample: {}", 
                hstRecords.stream().limit(3).map(r -> r.toString()).collect(Collectors.joining(", ")));
            
            // Log all available keys in the first record
            if (!hstRecords.isEmpty()) {
                log.debug("HST record available keys: {}", 
                    hstRecords.get(0).keySet().stream().collect(Collectors.joining(", ")));
            }
            
            // Use camelCase consistently (TableQueryHelper returns camelCase field names)
            // Try both possible field names to see which one works
            Set<String> excludedIdNumbers = hstRecords.stream()
                .map(record -> {
                    String idNo = (String) record.get("idNo");
                    String nricNo = (String) record.get("IdNo");
                    log.debug("HST record field check - idNo: {}, nricNo: {}", idNo, nricNo);
                    return idNo != null ? idNo : nricNo;
                })
                .filter(idNo -> idNo != null)
                .collect(Collectors.toSet());
                
            log.debug("Found {} excluded ID numbers from ocms_hst: {}", 
                excludedIdNumbers.size(), 
                excludedIdNumbers.stream().limit(10).collect(Collectors.joining(", ")));
            return excludedIdNumbers;
            
        } catch (Exception e) {
            log.error("Error querying ocms_hst for exclusions: {}", e.getMessage(), e);
            return Collections.emptySet();
        }*/
    }
    
    /**
     * Query valid offence notices with basic filters and manual exclusion
     */
    private List<Map<String, Object>> queryValidOffenceNoticesBasic(LocalDateTime nextDay, Set<String> excludedNotices) {
        try {
            // Include: suspension_type IS NULL OR (suspension_type='TS' AND epr_reason_of_suspension IN ('HST','UNC'))
            // This allows re-verification of TS-HST and TS-UNC suspensions
            // Since TableQueryService doesn't support OR operator, we'll do 2 separate queries and merge

            // Common filter values used by both queries
            List<String> validProcessingStages = Arrays.asList(
                SystemConstant.SuspensionReason.RD1,
                SystemConstant.SuspensionReason.RD2,
                SystemConstant.SuspensionReason.RR3,
                SystemConstant.SuspensionReason.DN2,
                SystemConstant.SuspensionReason.DR3
            );
            String nextProcessingDateFilter = nextDay.toString();

            // Query 1: Get records with suspensionType IS NULL
            Map<String, Object> filters1 = new HashMap<>();
            filters1.put("nextProcessingStage.in", validProcessingStages);
            filters1.put("nextProcessingDate[$lte]", nextProcessingDateFilter);
            filters1.put("suspensionType.null", true);
            filters1.put("$limit", 10000);

            log.debug("Query 1 - Fetching records with suspensionType IS NULL");
            List<Map<String, Object>> nullSuspensionRecords = tableQueryService.query(
                "ocms_valid_offence_notice", filters1);
            log.debug("Found {} records with NULL suspension", nullSuspensionRecords.size());

            // Query 2: Get records with suspensionType='TS' AND epr_reason_of_suspension IN ('HST','UNC')
            Map<String, Object> filters2 = new HashMap<>();
            filters2.put("nextProcessingStage.in", validProcessingStages);
            filters2.put("nextProcessingDate[$lte]", nextProcessingDateFilter);
            filters2.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            filters2.put("eprReasonOfSuspension.in", Arrays.asList(
                SystemConstant.SuspensionReason.HST,
                SystemConstant.SuspensionReason.UNC
            ));
            filters2.put("$limit", 10000);

            log.debug("Query 2 - Fetching TS-HST and TS-UNC records");
            List<Map<String, Object>> tsHstUncRecords = tableQueryService.query(
                "ocms_valid_offence_notice", filters2);
            log.debug("Found {} records with TS-HST/UNC", tsHstUncRecords.size());

            // Merge results and remove duplicates based on noticeNo
            List<Map<String, Object>> basicResults = new ArrayList<>(nullSuspensionRecords);
            Set<String> existingNoticeNos = nullSuspensionRecords.stream()
                .map(r -> (String) r.get("noticeNo"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            for (Map<String, Object> record : tsHstUncRecords) {
                String noticeNo = (String) record.get("noticeNo");
                if (noticeNo != null && !existingNoticeNos.contains(noticeNo)) {
                    basicResults.add(record);
                }
            }

            log.debug("Total merged records: {} (NULL: {}, TS-HST/UNC: {}, duplicates removed: {})",
                basicResults.size(), nullSuspensionRecords.size(), tsHstUncRecords.size(),
                (nullSuspensionRecords.size() + tsHstUncRecords.size() - basicResults.size()));
            
            log.debug("Basic query returned {} records before NRO exclusions", basicResults.size());
            
            // Debugging: Check what we actually got
            if (basicResults.isEmpty()) {
                log.debug("No records found in ocms_valid_offence_notice with the given filters");
                
                // Try without date filter to see if records exist
                Map<String, Object> simpleFilters = new HashMap<>();
                simpleFilters.put("nextProcessingStage.in", Arrays.asList(
                    SystemConstant.SuspensionReason.RD1,
                    SystemConstant.SuspensionReason.RD2,
                    SystemConstant.SuspensionReason.RR3,
                    SystemConstant.SuspensionReason.DN2,
                    SystemConstant.SuspensionReason.DR3
                ));
                simpleFilters.put("$limit", 10);
                List<Map<String, Object>> stageRecords = tableQueryService.query("ocms_valid_offence_notice", simpleFilters);
                log.debug("Found {} records with nextProcessingStage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3') (ignoring date)", stageRecords.size());
                
                if (!stageRecords.isEmpty()) {
                    log.debug("Sample record: {}", stageRecords.get(0));
                    log.debug("nextProcessingDate value: {}", stageRecords.get(0).get("nextProcessingDate"));
                    log.debug("nextProcessingDate class: {}", 
                        stageRecords.get(0).get("nextProcessingDate") != null ? 
                        stageRecords.get(0).get("nextProcessingDate").getClass().getName() : "null");
                }
                
                // Try with test notice numbers
                Map<String, Object> testFilter = new HashMap<>();
                testFilter.put("noticeNo[$like]", "MHATEST%");
                testFilter.put("$limit", 10);
                List<Map<String, Object>> testRecords = tableQueryService.query("ocms_valid_offence_notice", testFilter);
                log.debug("Found {} test records with noticeNo like 'MHATEST%'", testRecords.size());
                
                if (!testRecords.isEmpty()) {
                    log.debug("Sample test record: {}", testRecords.get(0));
                }
            } else {
                log.debug("Sample record from basic query: {}", basicResults.get(0));
                log.debug("Available fields: {}", basicResults.get(0).keySet());
            }
            
            // Manual filtering: Apply NRO exclusions in Java code
            List<Map<String, Object>> filteredResults = basicResults.stream()
                .filter(notice -> {
                    String noticeNo = (String) notice.get("noticeNo");
                    if (excludedNotices.contains(noticeNo)) {
                        log.debug("Manually filtering out notice {} due to NRO exclusion", noticeNo);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            log.debug("After manual NRO exclusions: {} records", filteredResults.size());
            
            return filteredResults;
        } catch (Exception e) {
            log.error("Error querying valid offence notices: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Filter by owner/driver criteria using manual filtering
     */
    private List<Map<String, Object>> filterByOwnerDriverCriteria(List<Map<String, Object>> notices, Set<String> excludedIdNumbers) {
        if (notices.isEmpty()) {
            return notices;
        }
        
        try {
            // Extract notice numbers for filtering
            List<String> noticeNumbers = notices.stream()
                .map(notice -> (String) notice.get("noticeNo"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toList());
            
            if (noticeNumbers.isEmpty()) {
                log.warn("No valid notice numbers found for owner/driver filtering");
                return new ArrayList<>();
            }
            
            log.debug("Filtering owner/driver records for {} notice numbers: {}", 
                noticeNumbers.size(), noticeNumbers);
            
            // Query all owner/driver records and filter manually (most reliable approach)
            Map<String, Object> filters = new HashMap<>();
            filters.put("$limit", 10000);
            
            log.debug("Querying ALL owner/driver records and filtering manually for notice numbers: {}", noticeNumbers);
            List<Map<String, Object>> allOwnerDriverDbRecords = tableQueryService.query("ocms_offence_notice_owner_driver", filters);
            log.debug("Total owner/driver records in database: {}", allOwnerDriverDbRecords.size());
            
            // Filter manually for our notice numbers
            List<Map<String, Object>> allOwnerDriverRecords = allOwnerDriverDbRecords.stream()
                .filter(record -> {
                    String noticeNo = (String) record.get("noticeNo");
                    boolean matches = noticeNumbers.contains(noticeNo);
                    if (matches) {
                        log.debug("Found matching owner/driver record for notice: {}", noticeNo);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            log.debug("Manual filtering found {} owner/driver records for our {} notice numbers", 
                allOwnerDriverRecords.size(), noticeNumbers.size());
            
            log.debug("Found {} owner/driver records for the notice numbers", allOwnerDriverRecords.size());
            
            // Log a sample of owner/driver records
            if (!allOwnerDriverRecords.isEmpty()) {
                log.debug("Owner/Driver records raw data sample: {}", 
                    allOwnerDriverRecords.stream().limit(3).map(r -> r.toString()).collect(Collectors.joining(", ")));
                
                log.debug("Owner/Driver record available keys: {}", 
                    allOwnerDriverRecords.get(0).keySet().stream().collect(Collectors.joining(", ")));
                
                // Log sample values to verify field names
                Map<String, Object> sampleRecord = allOwnerDriverRecords.get(0);
                log.debug("Sample field values - noticeNo: {}, offenderIndicator: {}, idType: {}, nricNo: {}", 
                    sampleRecord.get("noticeNo"), sampleRecord.get("offenderIndicator"), 
                    sampleRecord.get("idType"), sampleRecord.get("idNo"));
            } else {
                log.warn("No owner/driver records found for notice numbers: {}", noticeNumbers);
                
                // Check if ANY test records exist
                Map<String, Object> testFilters = new HashMap<>();
                testFilters.put("noticeNo[$like]", "MHATEST%");
                testFilters.put("$limit", 10);
                List<Map<String, Object>> testOwnerRecords = tableQueryService.query("ocms_offence_notice_owner_driver", testFilters);
                log.debug("Found {} test owner/driver records with noticeNo like 'MHATEST%'", testOwnerRecords.size());
                
                if (!testOwnerRecords.isEmpty()) {
                    log.debug("Sample test owner/driver record: {}", testOwnerRecords.get(0));
                }
                
                return new ArrayList<>();
            }
            
            // Manual filtering: Apply all criteria using camelCase field names
            List<Map<String, Object>> validOwnerDriverRecords = allOwnerDriverRecords.stream()
                .filter(record -> {
                    // Log the raw record for debugging
                    log.debug("Processing owner/driver record: {}", record);
                    
                    // Use camelCase field names consistently (TableQueryHelper returns camelCase)
                    String noticeNo = (String) record.get("noticeNo");
                    String offenderIndicator = (String) record.get("offenderIndicator");
                    String idType = (String) record.get("idType");
                    String nricNo = (String) record.get("idNo");
                    
                    log.debug("Record values - noticeNo: {}, offenderIndicator: {}, idType: {}, nricNo: {}", 
                        noticeNo, offenderIndicator, idType, nricNo);
                    
                    // Filter 1: Notice number must be in our list
                    boolean passesNoticeFilter = noticeNumbers.contains(noticeNo);
                    
                    // Filter 2: Must be an offender (this is CRITICAL and often the issue)
                    boolean passesOffenderFilter = "Y".equals(offenderIndicator);
                    
                    // Filter 3: ID type must be N (matching test data and SQL query)
                    boolean passesIdTypeFilter = "N".equals(idType);
                    
                    // Filter 4: NRIC must not be in HST exclusions
                    boolean passesHstFilter = nricNo != null && !excludedIdNumbers.contains(nricNo);
                    
                    log.debug("Filter results - notice: {}, offender: {}, idType: {}, hst: {}", 
                        passesNoticeFilter, passesOffenderFilter, passesIdTypeFilter, passesHstFilter);
                    
                    boolean passes = passesNoticeFilter && passesOffenderFilter && passesIdTypeFilter && passesHstFilter;
                    
                    if (!passes) {
                        log.debug("Record REJECTED for noticeNo: {}", noticeNo);
                    } else {
                        log.debug("Record ACCEPTED for noticeNo: {}", noticeNo);
                    }
                    
                    return passes;
                })
                .collect(Collectors.toList());
            
            log.debug("Manual filtering found {} valid owner/driver records", validOwnerDriverRecords.size());
            
            // Extract notice numbers that passed all criteria
            Set<String> validNoticeNumbers = validOwnerDriverRecords.stream()
                .map(record -> (String) record.get("noticeNo"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toSet());
            
            log.debug("Valid notice numbers after owner/driver filtering: {}", validNoticeNumbers);
            
            // Create a map of notice numbers to their corresponding owner/driver records with NRIC numbers
            Map<String, Map<String, Object>> noticeToOwnerDriverMap = validOwnerDriverRecords.stream()
                .collect(Collectors.toMap(
                    record -> (String) record.get("noticeNo"),
                    record -> record,
                    (existing, replacement) -> existing // Keep first in case of duplicates
                ));
            
            // Filter original notices to only include those with valid owner/driver records
            // AND enrich them with the NRIC number from the owner/driver record
            List<Map<String, Object>> filteredNotices = notices.stream()
                .filter(notice -> {
                    String noticeNo = (String) notice.get("noticeNo");
                    return noticeNo != null && validNoticeNumbers.contains(noticeNo);
                })
                .map(notice -> {
                    // Create a new map with all the original notice data
                    Map<String, Object> enrichedNotice = new HashMap<>(notice);
                    
                    // Add the NRIC number from the corresponding owner/driver record
                    String noticeNo = (String) notice.get("noticeNo");
                    Map<String, Object> ownerDriverRecord = noticeToOwnerDriverMap.get(noticeNo);
                    if (ownerDriverRecord != null) {
                        // Use camelCase field name consistently
                        String nricNo = (String) ownerDriverRecord.get("idNo");
                        if (nricNo != null && !nricNo.isEmpty()) {
                            enrichedNotice.put("idNo", nricNo);
                            log.debug("Enriched notice {} with NRIC {}", noticeNo, nricNo);
                        }
                    }
                    
                    return enrichedNotice;
                })
                .collect(Collectors.toList());
            
            log.debug("Filtered notices from {} to {} based on manual owner/driver criteria", 
                    notices.size(), filteredNotices.size());
            
            return filteredNotices;
            
        } catch (Exception e) {
            log.error("Error filtering by owner/driver criteria: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Query UNC and HST IDs from ocms_nro_temp for MHA/DataHive verification
     * Based on OCMS 20 Spec: Process queued UNC/HST IDs separately from NRIC stage changes
     *
     * @return List of UNC/HST records ready for MHA/DataHive query
     */
    public List<Map<String, Object>> queryUnclaimedAndHstData() {
        try {
            log.info("Querying UNC and HST IDs from ocms_nro_temp for MHA/DataHive verification");

            // Query ocms_nro_temp WHERE queryReason IN ('UNC', 'HST') AND processed = false
            Map<String, Object> filters = new HashMap<>();
            filters.put("queryReason.in", Arrays.asList("UNC", "HST"));
            filters.put("processed", false);
            filters.put("$limit", 10000);

            List<Map<String, Object>> queuedRecords = tableQueryService.query("ocms_nro_temp", filters);
            log.info("Found {} UNC/HST records queued for MHA/DataHive verification", queuedRecords.size());

            // Transform to match expected format for MHA file generation
            List<Map<String, Object>> transformedRecords = queuedRecords.stream()
                .map(record -> {
                    Map<String, Object> transformed = new HashMap<>();
                    transformed.put("idNo", record.get("idNo"));
                    transformed.put("idType", record.get("idType"));
                    transformed.put("queryReason", record.get("queryReason"));
                    return transformed;
                })
                .collect(Collectors.toList());

            log.info("Transformed {} UNC/HST records for MHA/DataHive upload", transformedRecords.size());
            return transformedRecords;

        } catch (Exception e) {
            log.error("Error querying UNC/HST data from ocms_nro_temp: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Insert uploaded notice numbers to ocms_nro_temp table
     * This prevents duplicate uploads in future job runs
     *
     * Updated for OCMS 20: Now populates idNo, idType, and queryReason fields
     * - queryReason = 'NRIC' for regular MHA NRIC verification
     * - queryReason will be 'UNC' or 'HST' when called from Unclaimed/HST workflows
     *
     * @param nricData List of records that were successfully uploaded
     * @return Number of records inserted
     */
    public int insertToNroTemp(List<Map<String, Object>> nricData) {
        log.info("Inserting {} notice numbers to ocms_nro_temp for tracking", nricData.size());

        try {
            // Extract unique notice numbers from uploaded data
            List<OcmsNroTemp> nroTempRecords = nricData.stream()
                .map(record -> {
                    String noticeNo = (String) record.get("noticeNo");
                    if (noticeNo == null || noticeNo.isEmpty()) {
                        log.warn("Skipping record with null/empty noticeNo: {}", record);
                        return null;
                    }

                    // Extract NRIC number from the enriched record (added during owner/driver filtering)
                    String idNo = (String) record.get("idNo");
                    if (idNo == null || idNo.isEmpty()) {
                        log.warn("Missing idNo for noticeNo {}, will insert without it", noticeNo);
                    }

                    OcmsNroTemp nroTemp = new OcmsNroTemp();
                    nroTemp.setNoticeNo(noticeNo);
                    nroTemp.setIdNo(idNo);

                    // Set idType based on idNo format (N for NRIC, F for FIN, etc.)
                    // This is a simplified logic - actual logic may vary
                    if (idNo != null && !idNo.isEmpty()) {
                        if (idNo.startsWith("S") || idNo.startsWith("T")) {
                            nroTemp.setIdType("N"); // NRIC
                        } else if (idNo.startsWith("F") || idNo.startsWith("G")) {
                            nroTemp.setIdType("F"); // FIN
                        } else {
                            nroTemp.setIdType("B"); // Others
                        }
                    }

                    // Set queryReason to 'NRIC' for regular MHA verification
                    // This distinguishes from 'UNC' and 'HST' queries submitted by API
                    nroTemp.setQueryReason("NRIC");

                    // Set processed flag to false (will be updated when MHA response is received)
                    nroTemp.setProcessed(false);

                    // Audit fields (creDate, creUserId) will be auto-populated by @PrePersist

                    return nroTemp;
                })
                .filter(record -> record != null)
                .collect(Collectors.toList());

            // Batch insert to database
            List<OcmsNroTemp> savedRecords = ocmsNroTempService.saveAll(nroTempRecords);

            log.info("Successfully inserted {} records to ocms_nro_temp with queryReason=NRIC", savedRecords.size());
            return savedRecords.size();

        } catch (Exception e) {
            log.error("Error inserting to ocms_nro_temp: {}", e.getMessage(), e);
            // Don't fail the job, just log the error
            // The upload already succeeded, this is just for tracking
            return 0;
        }
    }

}
