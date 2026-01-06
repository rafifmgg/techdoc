package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.agencyFileExchange.helper.MhaFileFormatHelper;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NRICDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NricNoticeData;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveCommonService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveNRICBatchService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveNRICService;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;

import com.ocmsintranet.cronservice.framework.common.FieldValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper class for MHA NRIC download operations.
 * Provides methods for processing MHA response files.
 */
@Slf4j
@Component
public class MhaNricDownloadHelper {

    @PersistenceContext
    private EntityManager entityManager;

    private final MhaFileFormatHelper mhaFileFormatHelper;
    private final TableQueryService tableQueryService;
    private final DataHiveNRICService dataHiveNRICService;
    private final DataHiveNRICBatchService dataHiveNRICBatchService;
    private final DataHiveCommonService dataHiveCommonService;
    private final com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient;
    // Using tableQueryService for database operations instead of direct repository access

    public MhaNricDownloadHelper(MhaFileFormatHelper mhaFileFormatHelper,
                               TableQueryService tableQueryService,
                               DataHiveNRICService dataHiveNRICService,
                               DataHiveNRICBatchService dataHiveNRICBatchService,
                               DataHiveCommonService dataHiveCommonService,
                               com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient) {
        this.mhaFileFormatHelper = mhaFileFormatHelper;
        this.tableQueryService = tableQueryService;
        this.dataHiveNRICService = dataHiveNRICService;
        this.dataHiveNRICBatchService = dataHiveNRICBatchService;
        this.dataHiveCommonService = dataHiveCommonService;
        this.suspensionApiClient = suspensionApiClient;
    }

    /**
     * Process the main MHA response file (NRO2URA_YYYYMMDDHHMMSS)
     * 
     * @param fileContent The content of the response file
     * @return List of parsed records
     */
    public List<Map<String, Object>> processResponseFile(byte[] fileContent) {
        log.info("Processing MHA response file");
        List<Map<String, Object>> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true;
            String dateOfRun = null;
            String timeOfRun = null;
            int totalRecords = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // First line is the header with date, time and record count
                if (isFirstLine) {
                    log.info("Processing header line: {}", line);
                    // Ensure the header line has the correct length
                    if (line.length() >= 238) {
                        // Format: [Filler 9][Date of run 8][Time of Run 6][No of records 6][Filler 186]
                        dateOfRun = line.substring(9, 17).trim(); // karakter 10-17
                        timeOfRun = line.substring(17, 23).trim(); // karakter 18-23
                        String recordCountStr = line.substring(23, 29).trim(); // karakter 24-29
                        
                        try {
                            totalRecords = Integer.parseInt(recordCountStr);
                            log.info("File header indicates {} records with date {} and time {}", 
                                     totalRecords, dateOfRun, timeOfRun);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse record count from header: {}", recordCountStr);
                        }
                    } else {
                        log.warn("Header line too short (length: {}), expected 238 characters. Header: {}", 
                                line.length(), line);
                    }
                    isFirstLine = false;
                    continue;
                }

                // PINK Step 3: Validate record length = 238 characters (from flowchart)
                if (line.length() != 238) {
                    log.warn("Invalid record length: {} (expected 238). Skipping record: {}",
                            line.length(), line.substring(0, Math.min(50, line.length())));
                    continue; // Skip invalid length records
                }

                // Parse the fixed-width record
                Map<String, Object> record = mhaFileFormatHelper.parseResponseRecord(line);
                if (dateOfRun != null && timeOfRun != null) {
                    record.put("fileDate", dateOfRun);
                    record.put("fileTime", timeOfRun);
                }
                
                // Get the UIN for validation and logging
                String uin = (String) record.get("uin");
                String identifier = uin != null ? uin : "UNKNOWN";
                
                // Basic required field validation
                if (uin == null || uin.isEmpty()) {
                    log.error("Missing UIN in record");
                    log.warn("Skipping invalid record due to missing UIN");
                    continue;
                }
                
                String uraReferenceNo = (String) record.get("uraReferenceNo");
                if (uraReferenceNo == null || uraReferenceNo.isEmpty()) {
                    log.error("Missing URA reference number in record for UIN {}", uin);
                    log.warn("Skipping invalid record due to missing URA reference number");
                    continue;
                }
                
                // Validate field lengths using the specialized method in FieldValidationHelper
                if (FieldValidationHelper.validateMhaFieldLengths(record, identifier)) {
                    records.add(record);
                    log.debug("Processed valid record: UIN={}, Name={}", uin, record.get("name"));
                } else {
                    log.warn("Skipping invalid record due to field length validation failures for UIN {}", uin);
                }
            }
            
            // Verify record count if header was present
            if (totalRecords > 0 && records.size() != totalRecords) {
                log.warn("Header indicated {} records but {} were actually processed", totalRecords, records.size());
            }
            
            log.info("Successfully processed {} records from MHA response file", records.size());
        } catch (Exception e) {
            log.error("Error processing MHA response file", e);
            throw new RuntimeException("Error processing MHA response file", e);
        }
        
        return records;
    }

    /**
     * Process the MHA control totals report file (REPORT_YYYYMMDDHHMMSS.TOT)
     * 
     * @param fileContent The content of the report file
     * @return Map containing the control totals
     */
    public Map<String, Integer> processControlTotalsReport(byte[] fileContent) {
        log.info("Processing MHA control totals report");
        Map<String, Integer> totals = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {
            
            // Original pattern to match control total lines like "A) NO. OF RECORDS READ = 46"
            Pattern pattern = Pattern.compile("([A-Z])\\)\\s+([^=]+)\\s+=\\s+(\\d+)");
            
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String key = matcher.group(2).trim();
                        int value = Integer.parseInt(matcher.group(3));
                        String normalizedKey = normalizeKey(key);
                        totals.put(normalizedKey, value);
                        log.debug("Found control total: {} = {} (normalized: {})", key, value, normalizedKey);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Could not parse number from line: {}", line);
                }
            }
            
            log.info("Successfully processed control totals report with {} entries", totals.size());
        } catch (Exception e) {
            log.error("Error processing MHA control totals report", e);
            throw new RuntimeException("Error processing MHA control totals report", e);
        }
        
        return totals;
    }
    
    /**
     * Normalizes the key from the TOT file to a standard format
     * @param key The original key from the TOT file
     * @return Normalized key in UPPER_SNAKE_CASE
     */
    private String normalizeKey(String key) {
        if (key == null) return "UNKNOWN";
        
        // The current regex captures the text between ')' and '='
        // So we need to handle the full text including "NO. OF RECORDS READ"
        
        if (key.contains("NO. OF RECORDS READ")) {
            return "TOTAL_RECORDS_READ";
        } else if (key.contains("NO. OF RECORDS MATCH")) {
            return "RECORDS_MATCHED";
        } else if (key.contains("NO. OF RECORDS WITH INVALID UIN/FIN")) {
            return "INVALID_UIN_FIN";
        } else if (key.contains("NO. OF RECORDS WITH VALID UIN/FIN UNMATCHED")) {
            return "VALID_UIN_FIN_UNMATCHED";
        }
        
        // Fallback: convert to UPPER_SNAKE_CASE
        return key.replaceAll("\\s+", "_").toUpperCase();
    }

    /**
     * Process the MHA exceptions report file (REPORT_YYYYMMDDHHMMSS.EXP)
     * 
     * @param fileContent The content of the exceptions report
     * @return List of exception records
     */
    public List<Map<String, String>> processExceptionsReport(byte[] fileContent) {
        log.info("Processing MHA exceptions report");
        List<Map<String, String>> exceptions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {
            
            // Pattern to match exception lines with serial number, ID number, and exception status
            // Pattern pattern = Pattern.compile("\\s+(\\d+)\\s+([A-Z0-9]+)\\s+(.+)");
            Pattern pattern = Pattern.compile("\\s+(\\d+)\\s+(.{9})\\s+(.+)");
            boolean dataSection = false;
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Check if we've reached the data section
                if (line.contains("SERIAL NO") && line.contains("ID NUMBER") && line.contains("EXCEPTION STATUS")) {
                    dataSection = true;
                    continue;
                }
                
                // Check if we've reached the end of the data section
                if (line.contains("****  E N D  O F  R E P O R T  ****")) {
                    break;
                }
                
                // Process data lines
                if (dataSection) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        Map<String, String> exception = new HashMap<>();
                        exception.put("serialNo", matcher.group(1).trim());
                        exception.put("idNumber", matcher.group(2).trim());
                        exception.put("exceptionStatus", matcher.group(3).trim());
                        exceptions.add(exception);
                        
                        log.debug("Found exception: ID={}, Status={}", 
                                exception.get("idNumber"), exception.get("exceptionStatus"));
                    }
                }
            }
            
            log.info("Successfully processed exceptions report with {} entries", exceptions.size());
        } catch (Exception e) {
            log.error("Error processing MHA exceptions report", e);
            throw new RuntimeException("Error processing MHA exceptions report", e);
        }
        
        return exceptions;
    }

    /**
     * Apply status updates based on MHA verification results
     * 
     * @param records List of processed records from the main response file
     * @param exceptions List of exception records from the exceptions report
     * @return Number of records updated
     */
    @Transactional
    public int applyStatusUpdates(List<Map<String, Object>> records, List<Map<String, String>> exceptions) {
        log.info("Applying status updates based on MHA verification results - {} main records, {} exception records", 
                records.size(), exceptions.size());
        int updatedCount = 0;
        
        try {
            // Process successful verifications from the main response file
            for (Map<String, Object> record : records) {
                String uin = (String) record.get("uin");
                if (uin == null || uin.isEmpty()) {
                    log.warn("Skipping record with empty UIN");
                    continue;
                }
                
                // Find the owner_driver record by UIN
                Map<String, Object> ownerDriverFilter = new HashMap<>();
                ownerDriverFilter.put("idNo", uin); // Updated to use idNo instead of nricNo to match entity
                // Use idType 'N' to match the test data and upload workflow
                ownerDriverFilter.put("idType", "N");
                ownerDriverFilter.put("noticeNo", record.get("uraReferenceNo"));
                
                log.info("Searching for owner_driver record with idType = [N]"); // Updated to use idNo instead of nricNo
                
                // Prepare fields to update in ocms_offence_notice_owner_driver
                Map<String, Object> ownerDriverUpdates = new HashMap<>();
                
                // Update owner data from MHA response
                ownerDriverUpdates.put("name", record.get("name"));
                ownerDriverUpdates.put("lifeStatus", record.get("lifeStatus"));
                
                // Set address type if available in the response
                if (record.get("addressType") != null) {
                    ownerDriverUpdates.put("addressType", record.get("addressType"));
                }
                
                // Parse and set date of birth if available
                String dobStr = (String) record.get("dateOfBirth");
                LocalDateTime dob = parseDate(dobStr);
                if (dob != null) {
                    ownerDriverUpdates.put("dateOfBirth", dob);
                }
                
                // Parse and set date of death if available
                String dodStr = (String) record.get("dateOfDeath");
                LocalDateTime dod = parseDate(dodStr);
                if (dod != null) {
                    ownerDriverUpdates.put("dateOfDeath", dod);
                }
                
                // Process MHA registration address using the new address entity
                List<Map<String, Object>> ownerDriverRecordsList = tableQueryService.query(
                        "ocms_offence_notice_owner_driver", ownerDriverFilter);
                
                if (ownerDriverRecordsList != null && !ownerDriverRecordsList.isEmpty()) {
                    for (Map<String, Object> ownerDriverRecord : ownerDriverRecordsList) {
                        String noticeNo = (String) ownerDriverRecord.get("noticeNo");
                        String ownerDriverIndicator = (String) ownerDriverRecord.get("ownerDriverIndicator");
                        
                        if (noticeNo != null && ownerDriverIndicator != null) {
                            updateMhaRegistrationAddress(noticeNo, ownerDriverIndicator, record);
                        }
                    }
                }
                
                // Parse and set address change date if available
                String lastChangeAddressDateStr = (String) record.get("lastChangeAddressDate");
                LocalDateTime lastChangeAddressDate = parseDate(lastChangeAddressDateStr);
                if (lastChangeAddressDate != null) {
                    ownerDriverUpdates.put("lastChangeAddressDate", lastChangeAddressDate);
                }
                
                // Set MHA processing timestamp
                LocalDateTime now = LocalDateTime.now();
                ownerDriverUpdates.put("mhaProcessingDateTime", now);

                // PROCESS 3: Set is_sync to N to trigger cron batch sync to internet DB
                ownerDriverUpdates.put("isSync", "N");
                log.info("Setting isSync=N for ONOD (owner/driver) record for UIN {} to trigger internet sync via cron batch (Process 7)", uin);

                // Preserve the existing LTA processing date time if it exists
                // This ensures we don't overwrite LTA data during MHA processing
                List<Map<String, Object>> existingRecords = tableQueryService.query(
                        "ocms_offence_notice_owner_driver", ownerDriverFilter);
                
                log.info("Query result for UIN {}: found {} records", uin, 
                        existingRecords != null ? existingRecords.size() : 0);
                
                if (existingRecords == null || existingRecords.isEmpty()) {
                    log.warn("No existing owner_driver record found for UIN {}, skipping update", uin);
                    continue;
                }
                
                Map<String, Object> existingRecord = existingRecords.get(0);
                log.debug("Found existing record for UIN {}: noticeNo={}, name={}", 
                        uin, existingRecord.get("noticeNo"), existingRecord.get("name"));
                                        
                // Log the update attempt details
                log.info("Attempting to update owner_driver record for UIN {} with {} fields", 
                        uin, ownerDriverUpdates.size());
                log.debug("Update fields for UIN {}: {}", uin, ownerDriverUpdates);
                
                // Update the owner_driver record
                List<Map<String, Object>> updatedOwnerDriverRecords = tableQueryService.patch(
                        "ocms_offence_notice_owner_driver", ownerDriverFilter, ownerDriverUpdates);
                
                if (updatedOwnerDriverRecords != null && !updatedOwnerDriverRecords.isEmpty()) {
                    log.info("Successfully updated {} owner_driver record(s) for UIN {}", 
                            updatedOwnerDriverRecords.size(), uin);
                    updatedCount += updatedOwnerDriverRecords.size();
                } else {
                    log.warn("Failed to update owner_driver record for UIN {} - no records matched or updated", uin);
                }

                // Apply PS-RIP / PS-RP2 when Date of Death present (feature-flagged) and lifeStatus == 'D'
                String lifeStatusVal = (String) record.get("lifeStatus");
                if (dod != null && lifeStatusVal != null && "D".equalsIgnoreCase(lifeStatusVal)) {
                    try {
                        // Collect all notice numbers for this UIN
                        List<String> noticeIdsForUin = new ArrayList<>();
                        for (Map<String, Object> rec : existingRecords) {
                            String noticeNo = (String) rec.get("noticeNo");
                            if (noticeNo != null && !noticeNo.isEmpty()) {
                                noticeIdsForUin.add(noticeNo);
                            }
                        }

                        if (!noticeIdsForUin.isEmpty()) {
                            // NEW VALIDATION: Check if Date of Death > today → Apply TS-NRO instead of PS-RIP/PS-RP2
                            if (dod.toLocalDate().isAfter(LocalDate.now())) {
                                log.info("[TS-NRO] Date of Death ({}) is in the future (> today) for UIN {}, applying TS-NRO instead of PS-RIP/PS-RP2",
                                        dod.toLocalDate(), uin);

                                // Only apply to notices with no suspension yet
                                Map<String, Object> vonFilterFutureDod = new HashMap<>();
                                vonFilterFutureDod.put("noticeNo.in", noticeIdsForUin);
                                vonFilterFutureDod.put("suspensionType.null", true);

                                List<Map<String, Object>> eligibleVonFutureDod = tableQueryService.query(
                                        "ocms_valid_offence_notice", vonFilterFutureDod);

                                List<String> eligibleFutureDodNoticeNos = new ArrayList<>();
                                if (eligibleVonFutureDod != null) {
                                    for (Map<String, Object> r : eligibleVonFutureDod) {
                                        String nn = (String) r.get("noticeNo");
                                        if (nn != null && !nn.isEmpty()) {
                                            eligibleFutureDodNoticeNos.add(nn);
                                        }
                                    }
                                }

                                if (!eligibleFutureDodNoticeNos.isEmpty()) {
                                    String futureDodReason = "Date of Death (" + dod.toLocalDate() + ") is in the future";
                                    int tsNroCount = applyTsNroSuspension(eligibleFutureDodNoticeNos, futureDodReason);
                                    updatedCount += tsNroCount;
                                    log.info("[TS-NRO] Applied TS-NRO to {} notices for UIN {} due to future Date of Death", tsNroCount, uin);
                                } else {
                                    log.info("[TS-NRO] No eligible notices for UIN {} (all have suspension already)", uin);
                                }

                                // Skip PS-RIP/PS-RP2 logic since we applied TS-NRO
                                continue;
                            }

                            // Split per notice: PS-RIP (DoD on/after offence) vs PS-RP2 (DoD before offence)
                            List<String> psRipNoticeNos = new ArrayList<>();
                            List<String> psRp2NoticeNos = new ArrayList<>();
                            for (String noticeNo : noticeIdsForUin) {
                                LocalDateTime offenceDt = getNoticeDateAndTime(noticeNo);
                                if (offenceDt == null) {
                                    log.warn("Could not determine offence date/time for notice {}, skipping PS update", noticeNo);
                                    continue;
                                }
                                if (dod.toLocalDate().isBefore(offenceDt.toLocalDate())) {
                                    psRp2NoticeNos.add(noticeNo);
                                } else {
                                    psRipNoticeNos.add(noticeNo);
                                }
                            }

                            // Batch update PS-RIP
                            if (!psRipNoticeNos.isEmpty()) {
                                Map<String, Object> vonFilterRip = new HashMap<>();
                                vonFilterRip.put("noticeNo.in", psRipNoticeNos);
                                log.info("Applying PS-RIP to {} notice(s), psRipNoticeNos: {}", psRipNoticeNos.size(), psRipNoticeNos.toString());

                                LocalDateTime suspensionDateRip = LocalDateTime.now();
                                // First update the suspension fields using tableQueryService
                                Map<String, Object> vonUpdatesRip = new HashMap<>();
                                vonUpdatesRip.put("suspensionType", SystemConstant.SuspensionType.PERMANENT);
                                vonUpdatesRip.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.RIP);
                                vonUpdatesRip.put("eprDateOfSuspension", suspensionDateRip);

                                // PROCESS 3: Set is_sync to N to trigger cron batch sync to internet DB
                                vonUpdatesRip.put("isSync", "N");
                                log.info("Setting isSync=N for PS-RIP notices to trigger internet sync via cron batch (Process 7)");

                                // Update all fields except dueDateOfRevival
                                List<Map<String, Object>> updatedVonRip = tableQueryService.patch(
                                        "ocms_valid_offence_notice", vonFilterRip, vonUpdatesRip);
                                
                                // Now update dueDateOfRevival to NULL using direct SQL
                                int nullUpdateCount = updateDueDateOfRevivalToNull(psRipNoticeNos);
                                
                                if (updatedVonRip != null && !updatedVonRip.isEmpty()) {
                                    log.info("Applied PS-RIP to {} notice(s) and set dueDateOfRevival to NULL for {} notice(s) for UIN {}",
                                            updatedVonRip.size(), nullUpdateCount, uin);
                                    updatedCount += updatedVonRip.size();

                                    // Apply suspension via API (batch call for all PS-RIP notices)
                                    try {
                                        log.info("[PS-RIP] Applying PS-RIP suspension via API for {} notice(s)", psRipNoticeNos.size());

                                        // Call suspension API with batch of notices
                                        List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                                            psRipNoticeNos,
                                            SystemConstant.SuspensionType.PERMANENT, // PS
                                            SystemConstant.SuspensionReason.RIP,
                                            "Auto-applied PS-RIP due to DateOfDeath from MHA", // suspensionRemarks
                                            SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                                            SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                                            null, // caseNo
                                            null // daysToRevive (PS has no revival)
                                        );

                                        // Log results for each notice
                                        int successCount = 0;
                                        for (Map<String, Object> apiResponse : apiResponses) {
                                            String noticeNo = (String) apiResponse.get("noticeNo");
                                            if (suspensionApiClient.isSuccess(apiResponse)) {
                                                successCount++;
                                                log.info("[PS-RIP] Successfully applied PS-RIP suspension via API for notice {}", noticeNo);
                                            } else {
                                                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                                                log.warn("[PS-RIP] API returned error when applying PS-RIP suspension for notice {}: {}",
                                                        noticeNo, errorMsg);
                                            }
                                        }
                                        log.info("[PS-RIP] API call completed: {}/{} notices suspended successfully",
                                                successCount, psRipNoticeNos.size());
                                    } catch (Exception ex) {
                                        log.error("[PS-RIP] Error calling suspension API for PS-RIP notices: {}", ex.getMessage(), ex);
                                        // Don't throw - log error and continue processing
                                    }
                                } else {
                                    log.warn("No VON records updated for PS-RIP for UIN {}", uin);
                                }
                            }

                            // Batch update PS-RP2
                            if (!psRp2NoticeNos.isEmpty()) {
                                Map<String, Object> vonFilterRp2 = new HashMap<>();
                                vonFilterRp2.put("noticeNo.in", psRp2NoticeNos);
                                log.info("Applying PS-RP2 to {} notice(s), psRp2NoticeNos: {}", psRp2NoticeNos.size(), psRp2NoticeNos.toString());

                                LocalDateTime suspensionDateRp2 = LocalDateTime.now();
                                Map<String, Object> vonUpdatesRp2 = new HashMap<>();
                                vonUpdatesRp2.put("suspensionType", SystemConstant.SuspensionType.PERMANENT);
                                vonUpdatesRp2.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.RP2);
                                vonUpdatesRp2.put("eprDateOfSuspension", suspensionDateRp2);

                                // PROCESS 3: Set is_sync to N to trigger cron batch sync to internet DB
                                vonUpdatesRp2.put("isSync", "N");
                                log.info("Setting isSync=N for PS-RP2 notices to trigger internet sync via cron batch (Process 7)");

                                log.info("Updating PS-RP2, vonUpdatesRp2: {}", vonUpdatesRp2.toString());

                                // First update all fields except dueDateOfRevival
                                List<Map<String, Object>> updatedVonRp2 = tableQueryService.patch(
                                        "ocms_valid_offence_notice", vonFilterRp2, vonUpdatesRp2);
                                
                                // Then update dueDateOfRevival to NULL using direct SQL
                                int nullUpdateCount = updateDueDateOfRevivalToNull(psRp2NoticeNos);

                                if (updatedVonRp2 != null && !updatedVonRp2.isEmpty()) {
                                    log.info("Applied PS-RP2 to {} notice(s) and set dueDateOfRevival to NULL for {} notice(s) for UIN {}",
                                            updatedVonRp2.size(), nullUpdateCount, uin);
                                    updatedCount += updatedVonRp2.size();

                                    // Apply suspension via API (batch call for all PS-RP2 notices)
                                    try {
                                        log.info("[PS-RP2] Applying PS-RP2 suspension via API for {} notice(s)", psRp2NoticeNos.size());

                                        // Call suspension API with batch of notices
                                        List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                                            psRp2NoticeNos,
                                            SystemConstant.SuspensionType.PERMANENT, // PS
                                            SystemConstant.SuspensionReason.RP2,
                                            "Auto-applied PS-RP2 as DoD before offence date", // suspensionRemarks
                                            SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                                            SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                                            null, // caseNo
                                            null // daysToRevive (PS has no revival)
                                        );

                                        // Log results for each notice
                                        int successCount = 0;
                                        for (Map<String, Object> apiResponse : apiResponses) {
                                            String noticeNo = (String) apiResponse.get("noticeNo");
                                            if (suspensionApiClient.isSuccess(apiResponse)) {
                                                successCount++;
                                                log.info("[PS-RP2] Successfully applied PS-RP2 suspension via API for notice {}", noticeNo);
                                            } else {
                                                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                                                log.warn("[PS-RP2] API returned error when applying PS-RP2 suspension for notice {}: {}",
                                                        noticeNo, errorMsg);
                                            }
                                        }
                                        log.info("[PS-RP2] API call completed: {}/{} notices suspended successfully",
                                                successCount, psRp2NoticeNos.size());
                                    } catch (Exception ex) {
                                        log.error("[PS-RP2] Error calling suspension API for PS-RP2 notices: {}", ex.getMessage(), ex);
                                        // Don't throw - log error and continue processing
                                    }
                                } else {
                                    log.warn("No VON records updated for PS-RP2 for UIN {}", uin);
                                }
                            }
                        } else {
                            log.warn("No notice numbers found for UIN {} to apply PS updates", uin);
                        }
                    } catch (Exception ex) {
                        log.warn("Error applying PS updates for UIN {}: {}", uin, ex.getMessage());
                    }
                }

                // WHITE/PINK FLOWCHART: Check if record was flagged with TS-NRO during validation
                String tsNroFlag = (String) record.get("TS_NRO_FLAG");
                String tsNroReason = (String) record.get("TS_NRO_REASON");

                if ("Y".equalsIgnoreCase(tsNroFlag)) {
                    log.info("[TS-NRO-FLAG] Record UIN {} flagged during validation with reason: {}", uin, tsNroReason);

                    try {
                        // Collect notice numbers for this UIN
                        List<String> flaggedTsNoticeNos = new ArrayList<>();
                        if (existingRecords != null) {
                            for (Map<String, Object> rec : existingRecords) {
                                String noticeNo = (String) rec.get("noticeNo");
                                if (noticeNo != null && !noticeNo.isEmpty()) {
                                    flaggedTsNoticeNos.add(noticeNo);
                                }
                            }
                        }

                        if (!flaggedTsNoticeNos.isEmpty()) {
                            // Only apply to notices with no suspension yet (avoid overriding PS/TS)
                            Map<String, Object> vonFilterFlagged = new HashMap<>();
                            vonFilterFlagged.put("noticeNo.in", flaggedTsNoticeNos);
                            vonFilterFlagged.put("suspensionType.null", true);

                            log.info("[TS-NRO-FLAG] Querying VON records for flagged UIN {} with filter: {}", uin, vonFilterFlagged);
                            List<Map<String, Object>> eligibleVonFlagged = tableQueryService.query(
                                    "ocms_valid_offence_notice", vonFilterFlagged);

                            List<String> eligibleFlaggedNoticeNos = new ArrayList<>();
                            if (eligibleVonFlagged != null) {
                                for (Map<String, Object> r : eligibleVonFlagged) {
                                    String nn = (String) r.get("noticeNo");
                                    if (nn != null && !nn.isEmpty()) {
                                        eligibleFlaggedNoticeNos.add(nn);
                                    }
                                }
                            }

                            if (!eligibleFlaggedNoticeNos.isEmpty()) {
                                log.info("[TS-NRO-FLAG] Applying TS-NRO to {} flagged notices for UIN {}: {}",
                                        eligibleFlaggedNoticeNos.size(), uin, eligibleFlaggedNoticeNos);
                                int tsNroCount = applyTsNroSuspension(eligibleFlaggedNoticeNos, tsNroReason);
                                updatedCount += tsNroCount;
                                log.info("[TS-NRO-FLAG] Applied TS-NRO to {} notices (reason: {})", tsNroCount, tsNroReason);
                            } else {
                                log.info("[TS-NRO-FLAG] No eligible notices for UIN {} (all have suspension already)", uin);
                            }
                        } else {
                            log.warn("[TS-NRO-FLAG] No notice numbers found for flagged UIN {}", uin);
                        }
                    } catch (Exception ex) {
                        log.error("[TS-NRO-FLAG] Error applying TS-NRO for flagged UIN {}: {}", uin, ex.getMessage(), ex);
                    }
                }

                // TS-NRO from main record when address invalid (Invalid Address Tag or Street='NA' & Postal='000000')
                try {
                    // Collect notice numbers for this UIN
                    List<String> tsCandidateNoticeNos = new ArrayList<>();
                    if (existingRecords != null) {
                        for (Map<String, Object> rec2 : existingRecords) {
                            String nNo = (String) rec2.get("noticeNo");
                            if (nNo != null && !nNo.isEmpty()) tsCandidateNoticeNos.add(nNo);
                        }
                    }
                    log.info("[TS-NRO] Found {} candidate notice numbers for UIN {}: {}", 
                            tsCandidateNoticeNos.size(), uin, tsCandidateNoticeNos);

                    if (!tsCandidateNoticeNos.isEmpty()) {
                        Object invalidAddrTagVal = record.get("invalidAddressTag");
                        String streetNameStr = (String) record.get("streetName");
                        String postalCodeStr = (String) record.get("postalCode");
                        boolean condInvalidTag = invalidAddrTagVal != null && !invalidAddrTagVal.toString().trim().isEmpty();
                        boolean condNaOrZero = (streetNameStr != null && "NA".equalsIgnoreCase(streetNameStr.trim()))
                                || (postalCodeStr != null && "000000".equals(postalCodeStr.trim()));
                        
                        // Check if Date Address Change > today
                        String addressChangeDateStr = (String) record.get("lastChangeAddressDate");
                        LocalDateTime addressChangeDate = parseDate(addressChangeDateStr);
                        boolean condDateChangeAfterToday = addressChangeDate != null && 
                                addressChangeDate.toLocalDate().isAfter(LocalDate.now());
                        
                        log.info("[TS-NRO] Address validation for UIN {}: invalidAddressTag={}, streetName={}, postalCode={}, lastChangeAddressDate={}", 
                                uin, invalidAddrTagVal, streetNameStr, postalCodeStr, addressChangeDateStr);
                        log.info("[TS-NRO] Address conditions: condInvalidTag={}, condNaOrZero={}, condDateChangeAfterToday={}", 
                                condInvalidTag, condNaOrZero, condDateChangeAfterToday);

                        log.info("[TS-NRO] Checking conditions for TS-NRO application:");
                        log.info("[TS-NRO] - Invalid Address Tag has value: {}", condInvalidTag);
                        log.info("[TS-NRO] - Street='NA' OR Postal='000000': {}", condNaOrZero);
                        log.info("[TS-NRO] - Date Address Change > today: {}", condDateChangeAfterToday);
                        
                        // Apply TS-NRO if: Invalid Address Tag has value OR (Street='NA' OR Postal='000000') OR Date Address Change > today
                        if (condInvalidTag || condNaOrZero || condDateChangeAfterToday) {
                            // Only apply to notices with no suspension yet (avoid overriding PS/TS)
                            Map<String, Object> vonPreFilterTs = new HashMap<>();
                            vonPreFilterTs.put("noticeNo.in", tsCandidateNoticeNos);
                            vonPreFilterTs.put("suspensionType.null", true);
                            
                            log.info("[TS-NRO] Querying VON records with filter: {}", vonPreFilterTs);
                            List<Map<String, Object>> eligibleVonRecords = tableQueryService.query(
                                    "ocms_valid_offence_notice", vonPreFilterTs);
                            
                            log.info("[TS-NRO] Found {} eligible VON records with null suspension_type", 
                                    eligibleVonRecords != null ? eligibleVonRecords.size() : 0);

                            List<String> eligibleTsNoticeNos = new ArrayList<>();
                            if (eligibleVonRecords != null) {
                                for (Map<String, Object> r : eligibleVonRecords) {
                                    String nn = (String) r.get("noticeNo");
                                    if (nn != null && !nn.isEmpty()) eligibleTsNoticeNos.add(nn);
                                }
                            }
                            
                            log.info("[TS-NRO] Eligible notice numbers for TS-NRO: {}", eligibleTsNoticeNos);
                            
                            if (!eligibleTsNoticeNos.isEmpty()) {
                                // Apply TS-NRO suspension with consolidated VON updates
                                String invalidAddressReason = "Invalid address from MHA response";
                                int tsNroCount = applyTsNroSuspension(eligibleTsNoticeNos, invalidAddressReason);
                                updatedCount += tsNroCount;
                                log.info("[TS-NRO] Updated count is now {} after applying TS-NRO to {} notices", updatedCount, tsNroCount);
                            } else {
                                log.info("[TS-NRO] No eligible notices found for TS-NRO application (all have suspension_type already set)");
                            }
                        } else {
                            log.info("[TS-NRO] Address is valid, skipping TS-NRO application for UIN {}", uin);
                        }
                    } else {
                        log.info("[TS-NRO] No candidate notice numbers found for UIN {}, skipping TS-NRO application", uin);
                    }
                } catch (Exception ex) {
                    log.warn("Error applying TS-NRO (main record) for UIN {}: {}", uin, ex.getMessage());
                }

            }
            
            // Process exceptions from the exceptions report
            for (Map<String, String> exception : exceptions) {
                String idNumber = exception.get("idNumber");
                if (idNumber == null || idNumber.isEmpty()) {
                    log.warn("Skipping exception with empty ID number");
                    continue;
                }
                // 1) Get noticeNos by owner/driver (idNo + idType='N')
                Map<String, Object> ownerDriverFilter = new HashMap<>();
                ownerDriverFilter.put("idNo", idNumber);
                ownerDriverFilter.put("idType", "N");
                List<Map<String, Object>> ownerDriverRecords = tableQueryService.query(
                        "ocms_offence_notice_owner_driver", ownerDriverFilter);
                if (ownerDriverRecords == null || ownerDriverRecords.isEmpty()) {
                    log.warn("No owner_driver record found for exception ID {}, skipping update", idNumber);
                    continue;
                }
                List<String> noticeIds = new ArrayList<>();
                for (Map<String, Object> record : ownerDriverRecords) {
                    String noticeNo = (String) record.get("noticeNo");
                    if (noticeNo != null && !noticeNo.isEmpty()) {
                        noticeIds.add(noticeNo);
                    }
                }
                if (noticeIds.isEmpty()) {
                    log.warn("No notice numbers found for ID {}", idNumber);
                    continue;
                }
                // 2) Exclude notices present in ocms_nro_temp
                List<String> candidateNoticeNos = new ArrayList<>(noticeIds);
                try {
                    Map<String, Object> nroTempFilter = new HashMap<>();
                    nroTempFilter.put("noticeNo.in", noticeIds);
                    List<Map<String, Object>> nroTempRecords = tableQueryService.query("ocms_nro_temp", nroTempFilter);
                    if (nroTempRecords != null && !nroTempRecords.isEmpty()) {
                        for (Map<String, Object> r : nroTempRecords) {
                            String nn = (String) r.get("noticeNo");
                            if (nn == null) {
                                candidateNoticeNos.remove(nn);
                            }
                        }
                    }
                } catch (Exception ex0) {
                    log.warn("Error checking ocms_nro_temp exclusions for ID {}: {}", idNumber, ex0.getMessage());
                }
                if (candidateNoticeNos.isEmpty()) {
                    log.info("All candidate notices are excluded by ocms_nro_temp for ID {}", idNumber);
                    continue;
                }
                // 3) Intersect with VON where suspensionType is null (avoid overriding existing suspensions)
                List<String> eligibleNoticeNos = new ArrayList<>();
                try {
                    Map<String, Object> vonPreFilter = new HashMap<>();
                    vonPreFilter.put("noticeNo.in", candidateNoticeNos);
                    vonPreFilter.put("suspensionType.null", true);
                    List<Map<String, Object>> eligibleVonRecords = tableQueryService.query(
                            "ocms_valid_offence_notice", vonPreFilter);
                    if (eligibleVonRecords != null) {
                        for (Map<String, Object> r : eligibleVonRecords) {
                            String nn = (String) r.get("noticeNo");
                            if (nn != null && !nn.isEmpty()) eligibleNoticeNos.add(nn);
                        }
                    }
                } catch (Exception ex1) {
                    log.warn("Error intersecting with VON for ID {}: {}", idNumber, ex1.getMessage());
                }
                if (eligibleNoticeNos.isEmpty()) {
                    log.info("No eligible VON records (null suspension) for ID {}", idNumber);
                    continue;
                }
                // 4) Apply TS-NRO to eligible VON records
                Map<String, Object> noticeFilter = new HashMap<>();
                noticeFilter.put("noticeNo.in", eligibleNoticeNos);
                LocalDateTime suspensionDate = LocalDateTime.now();
                Map<String, Object> noticeUpdates = new HashMap<>();
                noticeUpdates.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
                noticeUpdates.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.NRO);
                noticeUpdates.put("eprDateOfSuspension", suspensionDate);

                // PROCESS 3: Set is_sync to N to trigger cron batch sync to internet DB
                noticeUpdates.put("isSync", "N");
                log.info("Setting isSync=N for exception-based TS-NRO notices (ID: {}) to trigger internet sync via cron batch (Process 7)", idNumber);

                List<Map<String, Object>> updatedNoticeRecords = tableQueryService.patch(
                        "ocms_valid_offence_notice", noticeFilter, noticeUpdates);
                if (updatedNoticeRecords != null && !updatedNoticeRecords.isEmpty()) {
                    updatedCount += updatedNoticeRecords.size();
                    // 5) Compute dateOfRevival from ocms_suspension_reason (TS/NRO)
                    LocalDateTime dateOfRevivalVal = null;
                    try {
                        Map<String, Object> srFilter = new HashMap<>();
                        srFilter.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
                        srFilter.put("reasonOfSuspension", SystemConstant.SuspensionReason.NRO);
                        List<Map<String, Object>> srRecords = tableQueryService.query("ocms_suspension_reason", srFilter);
                        if (srRecords != null && !srRecords.isEmpty()) {
                            Object daysObj = srRecords.get(0).get("noOfDaysForRevival");
                            if (daysObj instanceof Number) {
                                dateOfRevivalVal = suspensionDate.plusDays(((Number) daysObj).longValue());
                            } else if (daysObj != null) {
                                try { dateOfRevivalVal = suspensionDate.plusDays(Long.parseLong(daysObj.toString())); } catch (Exception ignore) {}
                            }
                        }
                    } catch (Exception ex2) {
                        log.warn("Error retrieving noOfDaysForRevival for TS/NRO: {}", ex2.getMessage());
                    }

                    // 6) Apply suspension via API for exception-based TS-NRO
                    String exceptionReason = "MHA exception: " + exception.get("exceptionStatus");
                    try {
                        log.info("[TS-NRO-EXCEPTION] Applying TS-NRO suspension via API for {} exception notice(s)", eligibleNoticeNos.size());

                        // Calculate daysToRevive from dateOfRevivalVal if available
                        Integer daysToRevive = null;
                        if (dateOfRevivalVal != null) {
                            long daysDiff = java.time.Duration.between(suspensionDate, dateOfRevivalVal).toDays();
                            daysToRevive = (int) daysDiff;
                        }

                        // Call suspension API with batch of notices
                        List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                            eligibleNoticeNos,
                            SystemConstant.SuspensionType.TEMPORARY, // TS
                            SystemConstant.SuspensionReason.NRO,
                            exceptionReason, // suspensionRemarks
                            SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                            SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                            null, // caseNo
                            daysToRevive // daysToRevive
                        );

                        // Log results for each notice
                        int successCount = 0;
                        for (Map<String, Object> apiResponse : apiResponses) {
                            String noticeNo = (String) apiResponse.get("noticeNo");
                            if (suspensionApiClient.isSuccess(apiResponse)) {
                                successCount++;
                                log.info("[TS-NRO-EXCEPTION] Successfully applied TS-NRO suspension via API for notice {}", noticeNo);
                            } else {
                                String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                                log.warn("[TS-NRO-EXCEPTION] API returned error when applying TS-NRO suspension for notice {}: {}",
                                        noticeNo, errorMsg);
                            }
                        }
                        log.info("[TS-NRO-EXCEPTION] API call completed: {}/{} notices suspended successfully",
                                successCount, eligibleNoticeNos.size());
                    } catch (Exception ex3) {
                        log.error("[TS-NRO-EXCEPTION] Error calling suspension API for TS-NRO exception notices: {}", ex3.getMessage(), ex3);
                        // Don't throw - log error and continue processing
                    }
                } else {
                    log.warn("Failed to update offence notice records for exception ID {} - no records matched or updated", idNumber);
                }
            }
            
            if (updatedCount > 0) {
                log.info("Successfully updated {} records with MHA verification results", updatedCount);
            } else {
                log.warn("No records were updated during MHA verification processing - check if test data exists and matches the UINs in the response files");
            }
        } catch (Exception e) {
            log.error("Error applying status updates: {}", e.getMessage(), e);
            throw new RuntimeException("Error applying status updates", e);
        }
        
        return updatedCount;
    }
    
    /**
     * Process DataHive NRIC integration for MHA records
     * This method extracts NRIC and notice data from OCMS database and calls DataHive service
     * 
     * @param records List of MHA records to process
     * @return Number of records processed
     */
    public int processNRICDataHiveIntegration(List<Map<String, Object>> records) {
        log.info("Starting DataHive integration for {} MHA records", records.size());
        int processedCount = 0;
        
        try {
            // Extract unique UINs from MHA records
            List<String> uniqueUins = new ArrayList<>();
            for (Map<String, Object> record : records) {
                String uin = (String) record.get("uin");
                if (uin != null && !uin.isEmpty() && !uniqueUins.contains(uin)) {
                    uniqueUins.add(uin);
                }
            }
            
            log.info("Found {} unique UINs for DataHive processing", uniqueUins.size());
            
            // Process each unique UIN
            for (String uin : uniqueUins) {
                try {
                    List<Map<String, Object>> nricNoticeData = extractNRICAndNoticeData(uin, records);
                    if (nricNoticeData != null && !nricNoticeData.isEmpty()) {
                        for (Map<String, Object> data : nricNoticeData) {
                            processDataHiveForRecord(data);
                            processedCount++;
                        }
                    } else {
                        log.warn("No NRIC/notice data found");
                    }
                } catch (Exception e) {
                    log.error("Error processing DataHive integration: {}", e.getMessage(), e);
                }
            }
            
            log.info("DataHive integration completed. Processed {} records", processedCount);
            
        } catch (Exception e) {
            log.error("Error in DataHive integration: {}", e.getMessage(), e);
            throw new RuntimeException("DataHive NRIC integration failed", e);
        }
        
        return processedCount;
    }

    /**
     * BATCH VERSION: Process DataHive NRIC integration for MHA records using batch lookups
     * This method queries DataHive in batches to avoid rate limiting
     *
     * Benefits over individual processing:
     * - Reduces API calls from N to N/100 per data source (FSC, CCC, Prison)
     * - Avoids DataHive rate limiting issues
     * - Improves overall performance through parallel execution
     * - Automatically deduplicates NRICs
     *
     * @param records List of MHA records to process
     * @return Number of NRIC/notice pairs processed
     */
    public int processNRICDataHiveIntegrationBatch(List<Map<String, Object>> records) {
        log.info("Starting BATCH DataHive integration for {} MHA records", records.size());
        int processedCount = 0;

        try {
            // Step 1: Extract all NRIC/notice pairs from MHA records
            List<NricNoticeData> allPairs = new ArrayList<>();
            for (Map<String, Object> record : records) {
                String uin = (String) record.get("uin");
                String noticeNo = (String) record.get("uraReferenceNo");

                if (uin != null && !uin.isEmpty() && noticeNo != null && !noticeNo.isEmpty()) {
                    NricNoticeData pair = NricNoticeData.builder()
                            .nric(uin)
                            .noticeNo(noticeNo)
                            .build();
                    allPairs.add(pair);
                } else {
                    log.warn("Skipping record with missing UIN or notice number: UIN={}, NoticeNo={}", uin, noticeNo);
                }
            }

            log.info("Extracted {} ID/notice pairs for batch DataHive processing", allPairs.size());

            if (allPairs.isEmpty()) {
                log.warn("No valid ID/notice pairs found for DataHive processing");
                return 0;
            }

            // Step 2: Batch query DataHive for all pairs (runs FSC, CCC, Prison queries in parallel)
            Map<String, NRICDataResult> dataHiveCache = dataHiveNRICBatchService.batchRetrieveNRICData(allPairs);
            log.info("Batch DataHive lookup completed. Retrieved {} cached results", dataHiveCache.size());

            // Step 3: Process and store each result
            for (NricNoticeData pair : allPairs) {
                try {
                    String cacheKey = pair.getCacheKey();
                    NRICDataResult result = dataHiveCache.get(cacheKey);

                    if (result != null) {
                        // Store the DataHive result (this will call DataHiveNRICServiceImpl.storeComcareData)
                        storeNRICDataResult(result, pair.getNric(), pair.getNoticeNo());
                        processedCount++;

                        log.debug("Stored DataHive result for Notice: {} - Comcare records: {}, Prison data: {}",
                                pair.getNoticeNo(),
                                result.getComcareData() != null ? result.getComcareData().size() : 0,
                                result.getCommonData() != null ? "available" : "none");
                    } else {
                        log.warn("No DataHive result found for Notice: {}", pair.getNoticeNo());
                    }

                } catch (Exception e) {
                    log.error("Error storing DataHive result for {}: {}", pair.getCacheKey(), e.getMessage(), e);
                }
            }

            log.info("Batch DataHive integration completed. Processed {} records", processedCount);

        } catch (Exception e) {
            log.error("Error in batch DataHive integration: {}", e.getMessage(), e);
            throw new RuntimeException("Batch DataHive NRIC integration failed", e);
        }

        return processedCount;
    }

    /**
     * Store NRIC DataHive result in database
     * This method delegates to DataHiveNRICServiceImpl's storage logic
     *
     * @param result The DataHive result containing Comcare and Prison data
     * @param nric The NRIC number
     * @param noticeNo The notice number
     */
    private void storeNRICDataResult(NRICDataResult result, String nric, String noticeNo) {
        try {
            // Store Comcare data (FSC + CCC) - reuse existing DataHiveNRICServiceImpl.storeComcareData logic
            if (result.getComcareData() != null && !result.getComcareData().isEmpty()) {
                for (NRICDataResult.ComcareData comcare : result.getComcareData()) {
                    // The noticeNo should already be set by the batch service
                    // But ensure it's set correctly
                    if (comcare.getNoticeNo() == null || comcare.getNoticeNo().isEmpty()) {
                        comcare.setNoticeNo(noticeNo);
                    }
                    if (comcare.getIdNo() == null || comcare.getIdNo().isEmpty()) {
                        comcare.setIdNo(nric);
                    }

                    // Store in ocms_dh_msf_comcare_fund table
                    storeComcareDataInDatabase(comcare);
                }
            }

            // Store Prison/Custody data
            if (result.getCommonData() != null) {
                // Store custody data
                if (result.getCommonData().getCustodyData() != null && !result.getCommonData().getCustodyData().isEmpty()) {
                    for (CommonDataResult.CustodyInfo custody : result.getCommonData().getCustodyData()) {
                        // Set noticeNo if not already set
                        if (custody.getNoticeNo() == null || custody.getNoticeNo().isEmpty()) {
                            custody.setNoticeNo(noticeNo);
                        }

                        // Store custody info using DataHiveCommonServiceImpl storage method
                        try {
                            storeCustodyData(custody);
                            log.debug("Stored custody data for Notice: {}", noticeNo);
                        } catch (Exception e) {
                            log.error("Error storing custody data for Notice: {}: {}",
                                    noticeNo, e.getMessage(), e);
                        }
                    }
                }

                // Store incarceration data
                if (result.getCommonData().getIncarcerationData() != null && !result.getCommonData().getIncarcerationData().isEmpty()) {
                    for (CommonDataResult.IncarcerationInfo incarceration : result.getCommonData().getIncarcerationData()) {
                        // Set noticeNo if not already set
                        if (incarceration.getNoticeNo() == null || incarceration.getNoticeNo().isEmpty()) {
                            incarceration.setNoticeNo(noticeNo);
                        }

                        // Store incarceration info using DataHiveCommonServiceImpl storage method
                        try {
                            storeIncarcerationData(incarceration);
                            log.debug("Stored incarceration data for Notice: {}", noticeNo);
                        } catch (Exception e) {
                            log.error("Error storing incarceration data for Notice: {}: {}",
                                    noticeNo, e.getMessage(), e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error storing DataHive result for Notice: {}: {}",
                    noticeNo, e.getMessage(), e);
            throw new RuntimeException("Failed to store NRIC DataHive result", e);
        }
    }

    /**
     * Store Comcare data in database
     * Reuses the same logic as DataHiveNRICServiceImpl.storeComcareData
     */
    private void storeComcareDataInDatabase(NRICDataResult.ComcareData comcare) {
        try {
            String COMCARE_TABLE = "ocms_dh_msf_comcare_fund";

            // Check if record exists - using composite key (id_no, notice_no)
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", comcare.getIdNo());
            filters.put("noticeNo", comcare.getNoticeNo());

            List<Map<String, Object>> existingRecords = tableQueryService.query(COMCARE_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            // Map fields - storing as strings since DataHive returns epoch values as strings
            fields.put("paymentDate", comcare.getPaymentDate());
            fields.put("assistanceStart", comcare.getAssistanceStart());
            fields.put("assistanceEnd", comcare.getAssistanceEnd());
            fields.put("beneficiaryName", comcare.getBeneficiaryName());
            fields.put("dataDate", comcare.getDataDate());

            // Convert reference period from epoch days format (e.g., "20089") to LocalDateTime
            String referencePeriodStr = comcare.getReferencePeriod();
            LocalDateTime referencePeriod = (referencePeriodStr != null && !referencePeriodStr.isEmpty())
                    ? com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil.convertEpochDaysToLocalDateTime(referencePeriodStr)
                    : LocalDateTime.of(1970, 1, 1, 0, 0, 0); // Default to epoch start if null
            fields.put("referencePeriod", referencePeriod);

            fields.put("source", comcare.getSource());

            if (!existingRecords.isEmpty()) {
                // Update existing record
                tableQueryService.patch(COMCARE_TABLE, filters, fields);
                log.debug("Updated Comcare {} data for idNo: {}, noticeNo: {}",
                        comcare.getSource(), comcare.getIdNo(), comcare.getNoticeNo());
            } else {
                // Create new record - include key fields
                fields.put("idNo", comcare.getIdNo());
                fields.put("noticeNo", comcare.getNoticeNo());
                tableQueryService.post(COMCARE_TABLE, fields);
                log.debug("Created new Comcare {} data for idNo: {}, noticeNo: {}",
                        comcare.getSource(), comcare.getIdNo(), comcare.getNoticeNo());
            }

        } catch (Exception e) {
            log.error("Error storing Comcare data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store Comcare data", e);
        }
    }

    /**
     * Store custody data using DataHiveCommonService storage method
     * Delegates to DataHiveCommonServiceImpl.storeCustodyInfo
     *
     * @param custody The custody data to store
     */
    private void storeCustodyData(CommonDataResult.CustodyInfo custody) {
        try {
            String CUSTODY_TABLE = "ocms_dh_sps_custody";

            // Check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", custody.getIdNo());
            filters.put("noticeNo", custody.getNoticeNo());

            List<Map<String, Object>> existingRecords = tableQueryService.query(CUSTODY_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("currentCustodyStatus", custody.getCurrentCustodyStatus());
            fields.put("institCode", custody.getInstitCode());

            // Convert reference period from epoch days format (e.g., "20089") to LocalDateTime
            String referencePeriodStr = custody.getReferencePeriod();
            LocalDateTime referencePeriod = (referencePeriodStr != null && !referencePeriodStr.isEmpty())
                    ? com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil.convertEpochDaysToLocalDateTime(referencePeriodStr)
                    : LocalDateTime.of(1970, 1, 1, 0, 0, 0); // Default to epoch start if null
            fields.put("referencePeriod", referencePeriod);

            if (!existingRecords.isEmpty()) {
                // Update existing record
                tableQueryService.patch(CUSTODY_TABLE, filters, fields);
                log.debug("Updated custody data for idNo: {}, noticeNo: {}", custody.getIdNo(), custody.getNoticeNo());
            } else {
                // Create new record - include key fields
                fields.put("idNo", custody.getIdNo());
                fields.put("noticeNo", custody.getNoticeNo());
                fields.put("admDate", custody.getAdmDate());
                tableQueryService.post(CUSTODY_TABLE, fields);
                log.debug("Created new custody data for idNo: {}, noticeNo: {}", custody.getIdNo(), custody.getNoticeNo());
            }

        } catch (Exception e) {
            log.error("Error storing custody data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store custody data", e);
        }
    }

    /**
     * Store incarceration data using DataHiveCommonService storage method
     * Delegates to DataHiveCommonServiceImpl.storeIncarcerationInfo
     *
     * @param incarceration The incarceration data to store
     */
    private void storeIncarcerationData(CommonDataResult.IncarcerationInfo incarceration) {
        try {
            String INCARCERATION_TABLE = "ocms_dh_sps_incarceration";

            // Default date for null reference period values: 1970-01-01 00:00:00 (epoch start)
            LocalDateTime defaultDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

            // Check if record exists by inmateNumber (primary key)
            Map<String, Object> filters = new HashMap<>();
            filters.put("inmateNumber", incarceration.getInmateNumber());

            List<Map<String, Object>> existingRecords = tableQueryService.query(INCARCERATION_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("tentativeReleaseDate", incarceration.getTentativeReleaseDate());
            // Temporary: set referencePeriodOffenceInfo to current time until DB migration removes this column
            fields.put("referencePeriodOffenceInfo", LocalDateTime.now());

            // Set referencePeriodRelease - convert from epoch days format (e.g., "20089")
            // Use DataHiveDateUtil to convert, or default if null (mandatory field)
            String refPeriodReleaseStr = incarceration.getReferencePeriodRelease();
            LocalDateTime refPeriodRelease = (refPeriodReleaseStr != null && !refPeriodReleaseStr.isEmpty())
                    ? com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil.convertEpochDaysToLocalDateTime(refPeriodReleaseStr)
                    : defaultDateTime;
            fields.put("referencePeriodRelease", refPeriodRelease);

            if (!existingRecords.isEmpty()) {
                // Update existing record
                tableQueryService.patch(INCARCERATION_TABLE, filters, fields);
                log.debug("Updated incarceration data for inmateNumber: {}", incarceration.getInmateNumber());
            } else {
                // Create new record - include key fields
                fields.put("inmateNumber", incarceration.getInmateNumber());
                fields.put("noticeNo", incarceration.getNoticeNo());
                fields.put("idNo", incarceration.getIdNo());
                tableQueryService.post(INCARCERATION_TABLE, fields);
                log.debug("Created new incarceration data for idNo: {}, noticeNo: {}", incarceration.getIdNo(), incarceration.getNoticeNo());
            }

        } catch (Exception e) {
            log.error("Error storing incarceration data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store incarceration data", e);
        }
    }

    /**
     * Extract NRIC and notice number data from MHA response records for a given UIN
     *
     * @param uin The UIN to search for
     * @param mhaRecords The MHA response records containing UIN and notice number mappings
     * @return List of maps containing NRIC and notice data
     */
    private List<Map<String, Object>> extractNRICAndNoticeData(String uin, List<Map<String, Object>> mhaRecords) {
        log.debug("Extracting NRIC and notice data from MHA records");

        List<Map<String, Object>> nricNoticeData = new ArrayList<>();

        try {
            // Extract from MHA records instead of querying database
            for (Map<String, Object> record : mhaRecords) {
                String recordUin = (String) record.get("uin");
                String noticeNo = (String) record.get("uraReferenceNo");

                // Match the UIN and validate
                if (uin.equals(recordUin) && noticeNo != null && !noticeNo.isEmpty()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("nric", recordUin);
                    data.put("noticeNo", noticeNo);
                    nricNoticeData.add(data);
                }
            }

            log.debug("Extracted {} NRIC/notice pairs", nricNoticeData.size());
            return nricNoticeData;

        } catch (Exception e) {
            log.error("Error extracting NRIC and notice data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract NRIC and notice data", e);
        }
    }
    
    /**
     * Process DataHive lookup for a single NRIC/notice record
     * 
     * @param data Map containing NRIC and notice number
     */
    private void processDataHiveForRecord(Map<String, Object> data) {
        String nric = (String) data.get("nric");
        String noticeNo = (String) data.get("noticeNo");
        
        log.info("Processing DataHive integration for Notice: {}", noticeNo);
        
        try {
            // Call DataHive NRIC service
            NRICDataResult result = dataHiveNRICService.retrieveNRICData(nric, noticeNo);
            
            if (result != null) {
                log.info("DataHive lookup successful for Notice: {}", noticeNo);
                log.debug("DataHive result - Comcare records: {}, Common data: {}",
                        result.getComcareData() != null ? result.getComcareData().size() : 0,
                        result.getCommonData() != null ? "available" : "none");
            } else {
                log.warn("DataHive lookup returned null result for Notice: {}", noticeNo);
            }
            
        } catch (Exception e) {
            log.error("DataHive lookup failed, Error: {}", e.getMessage(), e);
            throw new RuntimeException("DataHive service error", e);
        }
    }
    
    /*
     * Update or create MHA registration address record
     * This method follows the same pattern as LtaProcessingOrchestrator's address update methods
     * with improved error handling, transaction management, and logging
     * Uses tableQueryService for database operations instead of direct repository access
     * 
     * @param noticeNo The notice number
     * @param ownerDriverIndicator The owner/driver indicator
     * @param record The MHA record with address data
     */
    private void updateMhaRegistrationAddress(String noticeNo, String ownerDriverIndicator, Map<String, Object> record) {
        try {
            log.info("Updating MHA registration address for notice {}, owner/driver {}", noticeNo, ownerDriverIndicator);
            
            // Define filters to check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);
            filters.put("ownerDriverIndicator", ownerDriverIndicator);
            filters.put("typeOfAddress", "mha_reg");
            
            // Check if record exists using tableQueryService
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_offence_notice_owner_driver_addr", filters);
            boolean isNewRecord = existingRecords.isEmpty();
            
            // Prepare fields for update or create
            Map<String, Object> fields = new HashMap<>();
            
            // Map address fields from MHA response - only add if not null or empty
            addIfPresentMapped(fields, record, "blockHouseNo", "blkHseNo");
            addIfPresentMapped(fields, record, "streetName", "streetName");
            addIfPresentMapped(fields, record, "floorNo", "floorNo");
            addIfPresentMapped(fields, record, "unitNo", "unitNo");
            addIfPresentMapped(fields, record, "buildingName", "bldgName");
            addIfPresentMapped(fields, record, "postalCode", "postalCode");
            addIfPresentMapped(fields, record, "addressType", "addressType");
            addIfPresentMapped(fields, record, "invalidAddressTag", "invalidAddrTag");
            
            // Set processing date time
            LocalDateTime now = LocalDateTime.now();
            fields.put("processingDateTime", now);
            
            // Set effective date if address change date is available
            String addressChangeDateStr = (String) record.get("lastChangeAddressDate");
            LocalDateTime addressChangeDate = parseDate(addressChangeDateStr);
            if (addressChangeDate != null) {
                fields.put("effectiveDate", addressChangeDate);
            }
            
            // Set error code if available
            String errorCode = (String) record.get("errorCode");
            if (errorCode != null && !errorCode.trim().isEmpty()) {
                fields.put("errorCode", errorCode);
            }
            
            if (isNewRecord) {
                // For create operation, add required fields
                fields.put("noticeNo", noticeNo);
                fields.put("ownerDriverIndicator", ownerDriverIndicator);
                fields.put("typeOfAddress", "mha_reg");
                
                // Add creation audit fields
                fields.put("creDate", now);
                fields.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.info("Creating new MHA registration address record for notice {}", noticeNo);
                tableQueryService.post("ocms_offence_notice_owner_driver_addr", fields);
            } else {
                // For update operation, add update audit fields
                fields.put("updDate", now);
                fields.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                
                log.info("Updating existing MHA registration address for notice {}", noticeNo);
                tableQueryService.patch("ocms_offence_notice_owner_driver_addr", filters, fields);
            }
            
            log.info("Successfully {} MHA registration address for notice {}", 
                    isNewRecord ? "created" : "updated", noticeNo);
            
        } catch (Exception e) {
            log.error("Error updating MHA registration address for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }
    
    /**
     * Get offence date/time (noticeDateAndTime) for a notice
     *
     * @param noticeNo notice number
     * @return LocalDateTime or null
     */
    private LocalDateTime getNoticeDateAndTime(String noticeNo) {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("noticeNo", noticeNo);
            List<Map<String, Object>> records = tableQueryService.query("ocms_valid_offence_notice", filter);
            if (records != null && !records.isEmpty()) {
                Object val = records.get(0).get("noticeDateAndTime");
                if (val instanceof LocalDateTime) {
                    return (LocalDateTime) val;
                } else if (val != null) {
                    try {
                        return LocalDateTime.parse(val.toString());
                    } catch (Exception ignore) {
                        log.warn("Could not parse noticeDateAndTime {} for notice {}", val, noticeNo);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error retrieving offence date/time for notice {}: {}", noticeNo, e.getMessage());
        }
        return null;
    }
    
    /**
     * Convert a date string from YYYYMMDD format to LocalDateTime
     * 
     * @param dateStr Date string in YYYYMMDD format
     * @return LocalDateTime object or null if parsing fails
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Parse date without time component
            return LocalDateTime.parse(dateStr + "000000", 
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateStr);
            return null;
        }
    }
    
    /**
     * Helper method to add a value to a map if the source value is present and not empty
     * 
     * @param targetMap The map to add the value to
     * @param sourceMap The map containing the source value
     * @param sourceKey The key in the source map
     * @param targetKey The key to use in the target map
     */
    private void addIfPresentMapped(Map<String, Object> targetMap, Map<String, Object> sourceMap, 
                                  String sourceKey, String targetKey) {
        Object value = sourceMap.get(sourceKey);
        if (value != null && (!(value instanceof String) || !((String) value).trim().isEmpty())) {
            targetMap.put(targetKey, value);
        }
    }
    
    /**
     * Apply TS-NRO suspension for notices with invalid addresses
     * This method implements the complete TS-NRO trigger logic:
     * 1. Queries suspension_reason table for noOfDaysForRevival
     * 2. Calculates revival date based on current date + noOfDaysForRevival
     * 3. Creates a record in Suspended Notices table
     * 4. Updates VON (Valid Offence Notice) table with revival date and other fields
     *
     * @param noticeNumbers List of notice numbers to apply TS-NRO suspension to
     * @param suspensionReason Reason for suspension (will be saved to suspension_remarks)
     * @return Number of notices updated
     */
    @Transactional
    private int applyTsNroSuspension(List<String> noticeNumbers, String suspensionReason) {
        if (noticeNumbers == null || noticeNumbers.isEmpty()) {
            return 0;
        }
        
        log.info("[TS-NRO] Applying TS-NRO suspension to {} notices", noticeNumbers.size());
        int updatedCount = 0;
        
        try {
            LocalDate currentDate = LocalDate.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Step 1: Query suspension reason table to get noOfDaysForRevival
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
            suspReasonFilters.put("reasonOfSuspension", SystemConstant.SuspensionReason.NRO); // NRO
            
            log.info("[TS-NRO] Querying suspension_reason table for TS-NRO revival days");
            List<Map<String, Object>> suspReasonResults = tableQueryService.query("ocms_suspension_reason", suspReasonFilters);
            
            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("[TS-NRO] No suspension reason found for TS-NRO - cannot apply suspension");
                return 0;
            }
            
            // Get the noOfDaysForRevival value
            Object noOfDaysObj = suspReasonResults.get(0).get("noOfDaysForRevival");
            if (noOfDaysObj == null) {
                log.error("[TS-NRO] noOfDaysForRevival is null for TS-NRO suspension reason - cannot apply suspension");
                return 0;
            }
            
            int noOfDaysForRevival;
            try {
                if (noOfDaysObj instanceof Number) {
                    noOfDaysForRevival = ((Number) noOfDaysObj).intValue();
                } else {
                    noOfDaysForRevival = Integer.parseInt(noOfDaysObj.toString().trim());
                }
            } catch (NumberFormatException e) {
                log.error("[TS-NRO] Invalid noOfDaysForRevival value: '{}' - cannot apply suspension", noOfDaysObj);
                return 0;
            }
            
            // Step 2: Calculate revival date
            LocalDate revivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = revivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            log.info("[TS-NRO] Calculated revival date {} (current date + {} days)", 
                    formattedRevivalDate, noOfDaysForRevival);
            
            // Create filter for VON table update
            Map<String, Object> vonFilterTs = new HashMap<>();
            vonFilterTs.put("noticeNo.in", noticeNumbers);
            
            // Prepare all fields for VON table update in one go
            LocalDateTime suspensionDate = LocalDateTime.now();
            Map<String, Object> vonUpdatesTs = new HashMap<>();
            vonUpdatesTs.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY); // TS
            vonUpdatesTs.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.NRO); // NRO
            vonUpdatesTs.put("eprDateOfSuspension", suspensionDate);
            vonUpdatesTs.put("dueDateOfRevival", formattedRevivalDate);

            // PROCESS 3: Set is_sync to N to trigger cron batch sync to internet DB
            vonUpdatesTs.put("isSync", "N");
            log.info("[TS-NRO] Setting isSync=N for TS-NRO notices to trigger internet sync via cron batch (Process 7)");

            log.info("[TS-NRO] Updating VON table with filter: {}, updates: {}", vonFilterTs, vonUpdatesTs);

            // Update VON table with all fields in one go
            List<Map<String, Object>> updatedNoticeRecordsTs = tableQueryService.patch(
                    "ocms_valid_offence_notice", vonFilterTs, vonUpdatesTs);
            
            if (updatedNoticeRecordsTs != null && !updatedNoticeRecordsTs.isEmpty()) {
                updatedCount = updatedNoticeRecordsTs.size();
                log.info("[TS-NRO] Successfully updated {} VON records", updatedCount);

                // Apply suspension via API (batch call for all TS-NRO notices)
                try {
                    log.info("[TS-NRO] Applying TS-NRO suspension via API for {} notice(s)", noticeNumbers.size());

                    // Use provided suspensionReason or fallback to default
                    String remarks = (suspensionReason != null && !suspensionReason.trim().isEmpty())
                            ? suspensionReason
                            : "NRO check";

                    // Call suspension API with batch of notices
                    List<Map<String, Object>> apiResponses = suspensionApiClient.applySuspension(
                        noticeNumbers,
                        SystemConstant.SuspensionType.TEMPORARY, // TS
                        SystemConstant.SuspensionReason.NRO,
                        remarks, // suspensionRemarks
                        SystemConstant.User.DEFAULT_SYSTEM_USER_ID, // officerAuthorisingSuspension
                        SystemConstant.Subsystem.OCMS_CODE, // suspensionSource
                        null, // caseNo
                        noOfDaysForRevival // daysToRevive
                    );

                    // Log results for each notice
                    int successCount = 0;
                    for (Map<String, Object> apiResponse : apiResponses) {
                        String noticeNo = (String) apiResponse.get("noticeNo");
                        if (suspensionApiClient.isSuccess(apiResponse)) {
                            successCount++;
                            log.info("[TS-NRO] Successfully applied TS-NRO suspension via API for notice {}", noticeNo);
                        } else {
                            String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                            log.warn("[TS-NRO] API returned error when applying TS-NRO suspension for notice {}: {}",
                                    noticeNo, errorMsg);
                        }
                    }
                    log.info("[TS-NRO] API call completed: {}/{} notices suspended successfully",
                            successCount, noticeNumbers.size());
                } catch (Exception ex) {
                    log.error("[TS-NRO] Error calling suspension API for TS-NRO notices: {}", ex.getMessage(), ex);
                    // Don't throw - log error but still return updatedCount for VON updates
                }
            } else {
                log.warn("[TS-NRO] No VON records were updated");
            }
            
            return updatedCount;
        } catch (Exception e) {
            log.error("[TS-NRO] Error applying TS-NRO suspension: {}", e.getMessage(), e);
            throw new RuntimeException("Error applying TS-NRO suspension", e);
        }
    }
    
    /**
     * Update due_date_of_revival to NULL for the given notice numbers using direct SQL
     * 
     * @param noticeNos List of notice numbers to update
     * @return Number of records updated
     */
    @Transactional
    public int updateDueDateOfRevivalToNull(List<String> noticeNos) {
        log.info("updateDueDateOfRevivalToNull noticeNos: {}", noticeNos);
        if (noticeNos == null || noticeNos.isEmpty()) {
            return 0;
        }
        
        // Create a comma-separated list of quoted notice numbers for the IN clause
        String inClause = noticeNos.stream()
                .map(noticeNo -> "'" + noticeNo + "'")
                .collect(java.util.stream.Collectors.joining(","));
        
        String sql = "UPDATE ocmsizmgr.ocms_valid_offence_notice " +
                   "SET due_date_of_revival = NULL " +
                   "WHERE notice_no IN (" + inClause + ")";

        log.info("updateDueDateOfRevivalToNull sql: {}", sql);
        
        try {
            int updatedCount = entityManager.createNativeQuery(sql).executeUpdate();
            log.info("Successfully set due_date_of_revival to NULL for {} notice(s)", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("Error updating due_date_of_revival to NULL for noticeNos {}: {}", 
                     noticeNos, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Mark ocms_nro_temp records as processed after receiving MHA response
     * Updated for OCMS 20 to support UNC/HST query processing
     *
     * This method updates the 'processed' flag to true for notice numbers that have been
     * processed in the MHA response. This prevents them from being queried again.
     *
     * @param noticeNumbers List of notice numbers that have been processed
     * @return Number of records marked as processed
     */
    @Transactional
    public int markNroTempRecordsAsProcessed(List<String> noticeNumbers) {
        if (noticeNumbers == null || noticeNumbers.isEmpty()) {
            log.debug("[NRO_TEMP] No notice numbers to mark as processed");
            return 0;
        }

        log.info("[NRO_TEMP] Marking {} notice numbers as processed in ocms_nro_temp", noticeNumbers.size());

        try {
            // Filter for notice numbers
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo.in", noticeNumbers);
            filters.put("processed", false); // Only update unprocessed records

            // Update fields
            Map<String, Object> updates = new HashMap<>();
            updates.put("processed", true);
            updates.put("updDate", LocalDateTime.now());
            updates.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // Perform batch update
            List<Map<String, Object>> updatedRecords = tableQueryService.patch(
                    "ocms_nro_temp", filters, updates);

            int updatedCount = updatedRecords != null ? updatedRecords.size() : 0;
            log.info("[NRO_TEMP] Marked {} records as processed in ocms_nro_temp", updatedCount);

            // Log query reasons for debugging
            if (updatedCount > 0) {
                Map<String, Object> queryFilter = new HashMap<>();
                queryFilter.put("noticeNo.in", noticeNumbers);
                List<Map<String, Object>> records = tableQueryService.query("ocms_nro_temp", queryFilter);

                Map<String, Integer> queryReasonCounts = new HashMap<>();
                for (Map<String, Object> record : records) {
                    String queryReason = (String) record.get("queryReason");
                    String reason = (queryReason != null ? queryReason : "NRIC");
                    queryReasonCounts.put(reason, queryReasonCounts.getOrDefault(reason, 0) + 1);
                }

                log.info("[NRO_TEMP] Processed records by query reason: {}", queryReasonCounts);
            }

            return updatedCount;

        } catch (Exception e) {
            log.error("[NRO_TEMP] Error marking records as processed: {}", e.getMessage(), e);
            // Don't throw - log error and continue
            return 0;
        }
    }

    /**
     * Store UNC/HST results in ocms_temp_unc_hst_addr temporary table
     * OCMS 20: For UNC/HST query results, store addresses for later report generation
     *
     * This method stores the MHA/DataHive address results for UNC/HST queries in a temporary
     * table. These results will be used later to:
     * 1. Generate Monthly HST Data Report
     * 2. Generate Monthly HST Work Items Report
     * 3. Trigger auto-suspensions based on address validity
     *
     * @param records List of MHA response records
     * @return Number of UNC/HST records stored
     */
    @Transactional
    public int storeUncHstResults(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            log.debug("[UNC/HST] No records to store in temp table");
            return 0;
        }

        log.info("[UNC/HST] Storing MHA results for UNC/HST queries in ocms_temp_unc_hst_addr");

        int storedCount = 0;

        try {
            // For each MHA record, check if it was from a UNC/HST query
            for (Map<String, Object> record : records) {
                String uin = (String) record.get("uin");
                String noticeNo = (String) record.get("uraReferenceNo");

                if (uin == null || uin.isEmpty() || noticeNo == null || noticeNo.isEmpty()) {
                    continue;
                }

                // Check if this record was from a UNC/HST query by looking up ocms_nro_temp
                Map<String, Object> nroTempFilter = new HashMap<>();
                nroTempFilter.put("idNo", uin);
                nroTempFilter.put("queryReason.in", Arrays.asList("UNC", "HST"));

                List<Map<String, Object>> nroTempRecords = tableQueryService.query("ocms_nro_temp", nroTempFilter);

                if (nroTempRecords != null && !nroTempRecords.isEmpty()) {
                    // This is a UNC/HST query result - store it in temp table
                    Map<String, Object> tempRecord = nroTempRecords.get(0);
                    String queryReason = (String) tempRecord.get("queryReason");

                    // Check if record already exists (based on idNo + queryReason + noticeNo)
                    Map<String, Object> existingFilter = new HashMap<>();
                    existingFilter.put("idNo", uin);
                    existingFilter.put("queryReason", queryReason);
                    existingFilter.put("noticeNo", noticeNo);

                    List<Map<String, Object>> existingRecords = tableQueryService.query(
                            "ocms_temp_unc_hst_addr", existingFilter);

                    Map<String, Object> addressData = new HashMap<>();

                    // Map MHA address fields
                    addressData.put("blockHouseNo", record.get("blockHouseNo"));
                    addressData.put("streetName", record.get("streetName"));
                    addressData.put("floorNo", record.get("floorNo"));
                    addressData.put("unitNo", record.get("unitNo"));
                    addressData.put("buildingName", record.get("buildingName"));
                    addressData.put("postalCode", record.get("postalCode"));
                    addressData.put("addressType", record.get("addressType"));
                    addressData.put("invalidAddressTag", record.get("invalidAddressTag"));

                    // Map MHA response date
                    String addressChangeDateStr = (String) record.get("lastChangeAddressDate");
                    LocalDateTime addressChangeDate = parseDate(addressChangeDateStr);
                    if (addressChangeDate != null) {
                        addressData.put("lastChangeAddressDate", addressChangeDate);
                    }

                    // Store response timestamp
                    addressData.put("responseDateTime", LocalDateTime.now());

                    if (existingRecords != null && !existingRecords.isEmpty()) {
                        // Update existing record
                        addressData.put("updDate", LocalDateTime.now());
                        addressData.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                        tableQueryService.patch("ocms_temp_unc_hst_addr", existingFilter, addressData);
                        log.debug("[UNC/HST] Updated temp record for idNo={}, queryReason={}, noticeNo={}",
                                uin, queryReason, noticeNo);
                    } else {
                        // Create new record
                        addressData.put("idNo", uin);
                        addressData.put("idType", record.get("idType") != null ? record.get("idType") : "N");
                        addressData.put("queryReason", queryReason);
                        addressData.put("noticeNo", noticeNo);
                        addressData.put("creDate", LocalDateTime.now());
                        addressData.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

                        tableQueryService.post("ocms_temp_unc_hst_addr", addressData);
                        log.debug("[UNC/HST] Created temp record for idNo={}, queryReason={}, noticeNo={}",
                                uin, queryReason, noticeNo);
                    }

                    storedCount++;
                }
            }

            log.info("[UNC/HST] Stored {} UNC/HST results in ocms_temp_unc_hst_addr", storedCount);
            return storedCount;

        } catch (Exception e) {
            log.error("[UNC/HST] Error storing UNC/HST results: {}", e.getMessage(), e);
            // Don't throw - log error and continue
            return storedCount;
        }
    }
}
