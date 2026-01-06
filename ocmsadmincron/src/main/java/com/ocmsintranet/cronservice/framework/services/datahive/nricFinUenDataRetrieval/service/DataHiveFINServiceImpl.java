package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FINDataResult;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DataHiveFINService for retrieving FIN holder data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataHiveFINServiceImpl implements DataHiveFINService {
    
    private final DataHiveUtil dataHiveUtil;
    private final DataHiveCommonService commonService;
    private final TableQueryService tableQueryService;
    
    private static final String WORK_PERMIT_TABLE = "ocms_dh_mom_work_permit";
    private static final String PASS_TABLE = "ocms_dh_mha_pass";
    private static final String OFFENCE_NOTICE_TABLE = "ocms_offence_notice_owner_driver";
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    @Override
    public FINDataResult retrieveFINData(String fin, String noticeNumber, 
                                       String ownerDriverIndicator, Date offenceDate) {
        log.info("Retrieving FIN data for notice: {}", noticeNumber);
        
        FINDataResult result = FINDataResult.builder()
            .appliedTransactionCodes(new ArrayList<>())
            .generatedWorkItems(new ArrayList<>())
            .rawResults(new HashMap<>())
            .build();
        
        try {
            // Step 1: Check death status (critical - affects processing)
            getDeathStatus(fin, noticeNumber, offenceDate, ownerDriverIndicator, result);
            
            // Step 2: Check PR/SC conversion
            getPRStatus(fin, noticeNumber, result);
            
            // Step 3: Get pass information (hierarchical check)
            // Check EP first, then WP, then LTVP, then STP
            getWorkPermitInfo(fin, noticeNumber, ownerDriverIndicator, result);
            getPassInfo(fin, noticeNumber, result);
            
            // Step 4: Address resolution
            resolveAndUpdateAddress(result, ownerDriverIndicator, noticeNumber);
            
            // Step 5: Get common prison/custody data
            CommonDataResult commonData = commonService.getPrisonCustodyData("FIN", fin, noticeNumber);
            result.setCommonData(commonData);
            
        } catch (Exception e) {
            log.error("Error retrieving FIN data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve FIN data", e);
        }
        
        return result;
    }
    
    private void getDeathStatus(String fin, String noticeNumber, Date offenceDate, String ownerDriverIndicator, FINDataResult result) {
        try {
            String query = String.format(
                "SELECT FIN, DATE_OF_DEATH, REFERENCE_PERIOD FROM V_DH_MHA_FINDEATH WHERE FIN = '%s'", fin
            );
            
            log.debug("Executing death status query");
            JsonNode deathData = dataHiveUtil.executeQueryAsyncDataOnly(query).get();
            result.getRawResults().put("deathStatus", deathData);
            
            if (deathData != null && deathData.isArray() && deathData.size() > 0) {
                JsonNode record = deathData.get(0);
                String dateOfDeathStr = getStringValue(record, "DATE_OF_DEATH");
                
                if (dateOfDeathStr != null && !dateOfDeathStr.isEmpty()) {
                    Date dateOfDeath = parseDate(dateOfDeathStr);
                    
                    FINDataResult.DeathStatus deathStatus = FINDataResult.DeathStatus.builder()
                        .fin(fin)
                        .dateOfDeath(dateOfDeath)
                        .hasDeathDate(true)
                        .build();
                    
                    // Apply transaction codes based on death date
                    if (dateOfDeath != null && offenceDate != null && dateOfDeath.after(offenceDate)) {
                        deathStatus.setAppliedTransactionCode("PS-RIP");
                        result.getAppliedTransactionCodes().add("PS-RIP");
                        applyTransactionCode(noticeNumber, "PS-RIP");
                    } else {
                        deathStatus.setAppliedTransactionCode("PS-RP2");
                        result.getAppliedTransactionCodes().add("PS-RP2");
                        applyTransactionCode(noticeNumber, "PS-RP2");
                    }
                    
                    // Generate work item if needed
                    if ("H".equals(ownerDriverIndicator) || "D".equals(ownerDriverIndicator)) {
                        result.getGeneratedWorkItems().add("Death status work item for " + ownerDriverIndicator);
                        log.info("Work item placeholder: Death status for owner/driver indicator: {}", ownerDriverIndicator);
                    }
                    
                    result.setDeathStatus(deathStatus);
                }
            }
        } catch (Exception e) {
            log.error("Error getting death status: {}", e.getMessage(), e);
        }
    }
    
    private void getPRStatus(String fin, String noticeNumber, FINDataResult result) {
        try {
            log.info("Checking PR/SC conversion status");

            FINDataResult.PRStatus prStatus = FINDataResult.PRStatus.builder()
                .previousFin(fin)
                .isConverted(false)
                .build();

            // Store raw results for debugging
            if (result.getRawResults() == null) {
                result.setRawResults(new HashMap<>());
            }

            // Step 1: Check persons granted PR in V_DH_MHA_ALIVE_SCPR
            String alivePrQuery = String.format(
                "SELECT UIN, PREVIOUS_FIN, DATE_PR_GRANTED, REFERENCE_PERIOD " +
                "FROM V_DH_MHA_ALIVE_SCPR WHERE FIN = '%s'", fin
            );

            log.debug("Checking persons granted PR in V_DH_MHA_ALIVE_SCPR");

            JsonNode alivePrResult = null;
            try {
                CompletableFuture<JsonNode> alivePrFuture = dataHiveUtil.executeQueryAsyncDataOnly(alivePrQuery);
                alivePrResult = alivePrFuture.get();
                result.getRawResults().put("V_DH_MHA_ALIVE_SCPR", alivePrResult);
            } catch (Exception e) {
                log.warn("V_DH_MHA_ALIVE_SCPR query failed (table may not exist yet): {}", e.getMessage());
                // Continue processing - table not available yet
            }

            if (alivePrResult != null && alivePrResult.has("data") && alivePrResult.get("data").isArray()
                && alivePrResult.get("data").size() > 0) {
                JsonNode record = alivePrResult.get("data").get(0);
                log.info("Found PR record in V_DH_MHA_ALIVE_SCPR");

                // Update date_pr_granted
                String uin = getStringValue(record, "UIN");
                Date datePrGranted = parseDate(getStringValue(record, "DATE_PR_GRANTED"));

                prStatus.setUin(uin);
                prStatus.setDatePrGranted(datePrGranted);
                prStatus.setConverted(true);
                prStatus.setConversionType("PR");
                prStatus.setAppliedTransactionCode("TS-OLD");

                // Update new_nric in offence_notice table
                if (uin != null) {
                    updateNewNric(noticeNumber, uin);
                }

                // Update ocms_dh_mha_pass table with PR information
                updatePassTableForPRSC(fin, noticeNumber, uin, datePrGranted, "PR",
                    getStringValue(record, "PREVIOUS_FIN"),
                    getStringValue(record, "REFERENCE_PERIOD"));

                result.setPrStatus(prStatus);
                result.getAppliedTransactionCodes().add("TS-OLD");

                // Apply TS-OLD
                applyTransactionCode(noticeNumber, "TS-OLD");

                // Add Work Item
                result.getGeneratedWorkItems().add("PR conversion work item");

                log.info("FIN converted to PR");
                return;
            }

            // Step 2: Check persons granted SC in V_DH_MHA_SCGRANT
            String scGrantQuery = String.format(
                "SELECT UIN, PREVIOUS_FIN, SC_GRANT_DATE, REFERENCE_PERIOD " +
                "FROM V_DH_MHA_SCGRANT WHERE FIN = '%s'", fin
            );

            log.debug("Checking persons granted SC in V_DH_MHA_SCGRANT");

            JsonNode scGrantResult = null;
            try {
                CompletableFuture<JsonNode> scGrantFuture = dataHiveUtil.executeQueryAsyncDataOnly(scGrantQuery);
                scGrantResult = scGrantFuture.get();
                result.getRawResults().put("V_DH_MHA_SCGRANT", scGrantResult);
            } catch (Exception e) {
                log.warn("V_DH_MHA_SCGRANT query failed (table may not exist yet): {}", e.getMessage());
                // Continue processing - table not available yet
            }

            if (scGrantResult != null && scGrantResult.has("data") && scGrantResult.get("data").isArray()
                && scGrantResult.get("data").size() > 0) {
                JsonNode record = scGrantResult.get("data").get(0);
                log.info("Found SC record in V_DH_MHA_SCGRANT");

                // Update SC_GRANT_DATE
                String uin = getStringValue(record, "UIN");
                Date dateScGranted = parseDate(getStringValue(record, "SC_GRANT_DATE"));

                prStatus.setUin(uin);
                prStatus.setScGrantDate(dateScGranted);
                prStatus.setConverted(true);
                prStatus.setConversionType("SC");
                prStatus.setAppliedTransactionCode("TS-OLD");

                // Update new_nric in offence_notice table
                if (uin != null) {
                    updateNewNric(noticeNumber, uin);
                }

                // Update ocms_dh_mha_pass table with SC information
                updatePassTableForPRSC(fin, noticeNumber, uin, dateScGranted, "SC",
                    getStringValue(record, "PREVIOUS_FIN"),
                    getStringValue(record, "REFERENCE_PERIOD"));

                result.setPrStatus(prStatus);
                result.getAppliedTransactionCodes().add("TS-OLD");

                // Apply TS-OLD
                applyTransactionCode(noticeNumber, "TS-OLD");

                // Add Work Item
                result.getGeneratedWorkItems().add("SC conversion work item");

                log.info("FIN converted to SC");
                return;
            }

            // Step 3: Not exist in both tables (or tables not available yet)
            log.info("No PR/SC conversion found (tables may not be available yet)");

            // Skip PR/SC processing if tables don't exist
            // TODO: Enable when V_DH_MHA_ALIVE_SCPR and V_DH_MHA_SCGRANT tables are available in DataHive
            prStatus.setRemark("PR/SC tables not available in DataHive");
            result.setPrStatus(prStatus);

            // Don't apply transaction codes or generate work items until tables are ready
            log.debug("Skipping TS-OLD application - PR/SC tables not yet available");

        } catch (Exception e) {
            log.error("Error getting PR/SC status: {}", e.getMessage(), e);
            // Apply default transaction code on error
            FINDataResult.PRStatus prStatus = FINDataResult.PRStatus.builder()
                .previousFin(fin)
                .isConverted(false)
                .appliedTransactionCode("TS-OLD")
                .remark("Error checking PR/SC status")
                .build();

            result.setPrStatus(prStatus);
            result.getAppliedTransactionCodes().add("TS-OLD");
            applyTransactionCode(noticeNumber, "TS-OLD", "Error checking PR/SC status");
        }
    }
    
    private void processPRData(JsonNode record, FINDataResult.PRStatus prStatus, String dateField) {
        String uin = getStringValue(record, "UIN");
        Date grantedDate = parseDate(getStringValue(record, dateField));

        prStatus.setUin(uin);

        if ("DATE_PR_GRANTED".equals(dateField)) {
            prStatus.setDatePrGranted(grantedDate);
        } else if ("SC_GRANT_DATE".equals(dateField)) {
            prStatus.setScGrantDate(grantedDate);
        }

        if (uin != null && !uin.isEmpty()) {
            prStatus.setConverted(true);
        }
    }
    
    // PR/SC query methods removed - tables not available in DataHive
    // V_DH_MHA_ALIVE_SCPR, V_DH_MHA_DEAD_SCPR, V_DH_MHA_SCGRANT do not exist
    
    private void getWorkPermitInfo(String fin, String noticeNumber, String ownerDriverIndicator, FINDataResult result) {
        try {
            // Step 1: Check EP pass status first
            String epQuery = String.format(
                "SELECT CANCELLED_DT, EXPIRY_DT, WITHDRAWN_DT, APPLICATION_DT, " +
                "PASSTYPE_CD, ISSUANCE_DT, UEN, ISACTIVE, FIN " +
                "FROM V_DH_MOM_EPWORKPASS WHERE FIN = '%s' ORDER BY ISSUANCE_DT DESC", fin
            );
            
            JsonNode epPassData = dataHiveUtil.executeQueryAsyncDataOnly(epQuery).get();
            result.getRawResults().put("epPass", epPassData);
            
            boolean hasActiveEP = false;
            if (epPassData != null && epPassData.isArray() && epPassData.size() > 0) {
                JsonNode epRecord = epPassData.get(0);
                String isActive = getStringValue(epRecord, "ISACTIVE");
                
                if ("1".equals(isActive)) {
                    hasActiveEP = true;
                    // Get EP holder info
                    processEPHolderInfo(fin, noticeNumber, result);
                    
                    // Store EP pass details
                    storeWorkPermitData(epRecord, fin, noticeNumber, "EP", result);
                }
            }
            
            // If no active EP, check WP
            if (!hasActiveEP) {
                processWPInfo(fin, noticeNumber, ownerDriverIndicator, result);
            }
            
        } catch (Exception e) {
            log.error("Error getting work permit info: {}", e.getMessage(), e);
        }
    }
    
    private void processEPHolderInfo(String fin, String noticeNumber, FINDataResult result) {
        try {
            String query = String.format(
                "SELECT FOREIGNER_NAME, DATE_OF_BIRTH, SEX_CD, " +
                "BLOCK_HOUSE_NO, STREET_NAME, FLOOR_NO, UNIT_NO, " +
                "POSTAL_CODE_NO, LAST_CHANGE_ADDRESS_DT, ISACTIVE, FIN " +
                "FROM V_DH_MOM_EPFOREIGNER WHERE FIN = '%s'", fin
            );
            
            JsonNode epHolderData = dataHiveUtil.executeQueryAsyncDataOnly(query).get();
            result.getRawResults().put("epHolder", epHolderData);
            
            if (epHolderData != null && epHolderData.isArray() && epHolderData.size() > 0) {
                JsonNode record = epHolderData.get(0);
                
                // Update address information
                FINDataResult.WorkPermitInfo permitInfo = result.getWorkPermitInfo();
                if (permitInfo == null) {
                    permitInfo = FINDataResult.WorkPermitInfo.builder().build();
                    result.setWorkPermitInfo(permitInfo);
                }
                
                permitInfo.setBlockHouseNo(getStringValue(record, "BLOCK_HOUSE_NO"));
                permitInfo.setFloorNo(getStringValue(record, "FLOOR_NO"));
                permitInfo.setUnitNo(getStringValue(record, "UNIT_NO"));
                permitInfo.setStreetName(getStringValue(record, "STREET_NAME"));
                permitInfo.setPostalCode(getStringValue(record, "POSTAL_CODE_NO"));
            }
        } catch (Exception e) {
            log.error("Error processing EP holder info: {}", e.getMessage(), e);
        }
    }
    
    private void processWPInfo(String fin, String noticeNumber, String ownerDriverIndicator, FINDataResult result) {
        try {
            String wpWorkerQuery = String.format(
                "SELECT FIN, WORKER_NAME, DATE_OF_BIRTH, SEX_CD, " +
                "BLOCK_HOUSE_NO, STREET_NAME, FLOOR_NO, UNIT_NO, " +
                "POSTAL_CODE_NO, LAST_CHANGE_ADDRESS_DT, ISACTIVE, WORK_PERMIT_NO " +
                "FROM V_DH_MOM_WPWORKER WHERE FIN = '%s'", fin
            );
            
            JsonNode wpWorkerData = dataHiveUtil.executeQueryAsyncDataOnly(wpWorkerQuery).get();
            result.getRawResults().put("wpWorker", wpWorkerData);
            
            if (wpWorkerData != null && wpWorkerData.isArray() && wpWorkerData.size() > 0) {
                JsonNode workerRecord = wpWorkerData.get(0);
                String workPermitNo = getStringValue(workerRecord, "WORK_PERMIT_NO");
                
                if (workPermitNo != null && !workPermitNo.isEmpty()) {
                    // Get WP pass details
                    String wpPassQuery = String.format(
                        "SELECT EXPIRY_DT, REVOKED_CANCELLED_DT, WORK_PERMIT_NO, " +
                        "APPLICATION_DT, PASS_TYPE_CD, WORK_PASS_STATUS_CD, " +
                        "IPA_EXPIRY_DT, ISSUANCE_DT, UEN, EMPLOYER_NRICFIN, ISACTIVE " +
                        "FROM V_DH_MOM_WPWORKPASS WHERE WORK_PERMIT_NO = '%s'", workPermitNo
                    );
                    
                    JsonNode wpPassData = dataHiveUtil.executeQueryAsyncDataOnly(wpPassQuery).get();
                    result.getRawResults().put("wpPass", wpPassData);
                    
                    if (wpPassData != null && wpPassData.isArray() && wpPassData.size() > 0) {
                        storeWorkPermitData(wpPassData.get(0), fin, noticeNumber, "WP", result);
                        
                        // Set address from worker info
                        FINDataResult.WorkPermitInfo permitInfo = result.getWorkPermitInfo();
                        if (permitInfo == null) {
                            permitInfo = FINDataResult.WorkPermitInfo.builder().build();
                            result.setWorkPermitInfo(permitInfo);
                        }
                        
                        permitInfo.setWorkPermitNo(workPermitNo);
                        permitInfo.setBlockHouseNo(getStringValue(workerRecord, "BLOCK_HOUSE_NO"));
                        permitInfo.setFloorNo(getStringValue(workerRecord, "FLOOR_NO"));
                        permitInfo.setUnitNo(getStringValue(workerRecord, "UNIT_NO"));
                        permitInfo.setStreetName(getStringValue(workerRecord, "STREET_NAME"));
                        permitInfo.setPostalCode(getStringValue(workerRecord, "POSTAL_CODE_NO"));
                    }
                }
            }
            
            // Handle owner/driver indicator logic
            if ("O".equals(ownerDriverIndicator)) {
                checkPastRecordsAndGenerateWorkItem(fin, noticeNumber, result);
            }
            
        } catch (Exception e) {
            log.error("Error processing WP info: {}", e.getMessage(), e);
        }
    }
    
    private void storeWorkPermitData(JsonNode record, String fin, String noticeNumber, String type, FINDataResult result) {
        try {
            FINDataResult.WorkPermitInfo permitInfo = FINDataResult.WorkPermitInfo.builder()
                .idNo(fin)
                .noticeNo(noticeNumber)
                .passType(type.equals("EP") ? getStringValue(record, "PASSTYPE_CD") : getStringValue(record, "PASS_TYPE_CD"))
                .expiryDate(parseDate(getStringValue(record, "EXPIRY_DT")))
                .cancelledDate(parseDate(getStringValue(record, type.equals("EP") ? "CANCELLED_DT" : "REVOKED_CANCELLED_DT")))
                .employerUen(getStringValue(record, "UEN")) // UEN available in both EP and WP records
                .issuanceDate(parseDate(getStringValue(record, "ISSUANCE_DT")))
                .applicationDate(parseDate(getStringValue(record, "APPLICATION_DT")))
                .isActive("1".equals(getStringValue(record, "ISACTIVE")))
                .build();
            
            if (type.equals("EP")) {
                permitInfo.setWithdrawnDate(parseDate(getStringValue(record, "WITHDRAWN_DT")));
            } else {
                permitInfo.setIpaExpiryDate(parseDate(getStringValue(record, "IPA_EXPIRY_DT")));
                permitInfo.setWorkPassStatus(getStringValue(record, "WORK_PASS_STATUS_CD"));
            }
            
            result.setWorkPermitInfo(permitInfo);
            
            // Store in database
            // Check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", permitInfo.getIdNo());
            filters.put("noticeNo", permitInfo.getNoticeNo());
            
            List<Map<String, Object>> existingRecords = tableQueryService.query(WORK_PERMIT_TABLE, filters);
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("workPermitNo", permitInfo.getWorkPermitNo());
            fields.put("passType", permitInfo.getPassType());
            fields.put("expiryDate", permitInfo.getExpiryDate());
            fields.put("cancelledDate", permitInfo.getCancelledDate());
            fields.put("employerUen", permitInfo.getEmployerUen());
            fields.put("issuanceDate", permitInfo.getIssuanceDate());
            fields.put("applicationDate", permitInfo.getApplicationDate());
            fields.put("ipaExpiryDate", permitInfo.getIpaExpiryDate());
            fields.put("workPassStatus", permitInfo.getWorkPassStatus());
            fields.put("withdrawnDate", permitInfo.getWithdrawnDate());
            
            if (!existingRecords.isEmpty()) {
                // Record exists, use patch to update only the specified fields
                tableQueryService.patch(WORK_PERMIT_TABLE, filters, fields);
                log.info("Patched existing work permit data for type: {}", type);
            } else {
                // New record, include all fields for creation
                fields.put("idNo", permitInfo.getIdNo());
                fields.put("noticeNo", permitInfo.getNoticeNo());
                tableQueryService.post(WORK_PERMIT_TABLE, fields);
                log.info("Created new work permit data for type: {}", type);
            }
            
        } catch (Exception e) {
            log.error("Error storing work permit data: {}", e.getMessage(), e);
        }
    }
    
    private void getPassInfo(String fin, String noticeNumber, FINDataResult result) {
        try {
            // Query LTVP and STP in parallel
            CompletableFuture<JsonNode> ltvpFuture = queryLTVP(fin);
            CompletableFuture<JsonNode> stpFuture = querySTP(fin);
            
            CompletableFuture.allOf(ltvpFuture, stpFuture).join();
            
            // Process LTVP
            JsonNode ltvpData = ltvpFuture.get();
            result.getRawResults().put("ltvp", ltvpData);
            
            if (ltvpData != null && ltvpData.isArray() && ltvpData.size() > 0) {
                processPassData(ltvpData.get(0), fin, noticeNumber, "LTVP", result);
            }
            
            // Process STP if no LTVP
            if (result.getPassInfo() == null) {
                JsonNode stpData = stpFuture.get();
                result.getRawResults().put("stp", stpData);
                
                if (stpData != null && stpData.isArray() && stpData.size() > 0) {
                    processPassData(stpData.get(0), fin, noticeNumber, "STP", result);
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting pass info: {}", e.getMessage(), e);
        }
    }
    
    private CompletableFuture<JsonNode> queryLTVP(String fin) {
        String query = String.format(
            "SELECT SEX, BLOCK, FLOOR, UNIT, STREET_NAME, BUILDING_NAME, " +
            "POSTAL_CODE, ADDRESS_INDICATOR, DATEOF_EXPIRY, " +
            "REFERENCE_PERIOD, FIN, PRINCIPAL_NAME, NON_WORK_PASS_TYPE, " +
            "DATE_OF_ISSUE, DATE_OF_BIRTH FROM V_DH_MHA_LTVP WHERE FIN = '%s'", fin
        );
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private CompletableFuture<JsonNode> querySTP(String fin) {
        String query = String.format(
            "SELECT SEX, BLOCK, FLOOR, UNIT, STREET_NAME, BUILDING_NAME, " +
            "POSTAL_CODE, ADDRESS_INDICATOR, DATE_OF_EXPIRY, " +
            "REFERENCE_PERIOD, FIN, PRINCIPAL_NAME, NON_WORK_PASS_TYPE, " +
            "DATE_OF_ISSUE, DATE_OF_BIRTH FROM V_DH_MHA_STP WHERE FIN = '%s'", fin
        );
        return dataHiveUtil.executeQueryAsyncDataOnly(query);
    }
    
    private void processPassData(JsonNode record, String fin, String noticeNumber, String passType, FINDataResult result) {
        try {
            FINDataResult.PassInfo passInfo = FINDataResult.PassInfo.builder()
                .idNo(fin)
                .noticeNo(noticeNumber)
                .passType(getStringValue(record, "NON_WORK_PASS_TYPE"))
                .dateOfIssue(parseDate(getStringValue(record, "DATE_OF_ISSUE")))
                .dateOfExpiry(parseDate(getStringValue(record, passType.equals("LTVP") ? "DATEOF_EXPIRY" : "DATE_OF_EXPIRY")))
                .principalName(getStringValue(record, "PRINCIPAL_NAME"))
                .sex(getStringValue(record, "SEX"))
                .dateOfBirth(parseDate(getStringValue(record, "DATE_OF_BIRTH")))
                .referencePeriod(getStringValue(record, "REFERENCE_PERIOD"))
                .block(getStringValue(record, "BLOCK"))
                .floor(getStringValue(record, "FLOOR"))
                .unit(getStringValue(record, "UNIT"))
                .streetName(getStringValue(record, "STREET_NAME"))
                .buildingName(getStringValue(record, "BUILDING_NAME"))
                .postalCode(getStringValue(record, "POSTAL_CODE")) // Both LTVP and STP now have POSTAL_CODE
                .build();
            
            result.setPassInfo(passInfo);
            
            // Store in database
            // Check if record exists - MHA Pass table has only notice_no as primary key
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", passInfo.getNoticeNo());
            
            List<Map<String, Object>> existingRecords = tableQueryService.query(PASS_TABLE, filters);
            
            Map<String, Object> fields = new HashMap<>();
            // The MHA Pass table only has idNo and mhaPassExpiryDate fields besides the key
            fields.put("idNo", passInfo.getIdNo());
            fields.put("mhaPassExpiryDate", passInfo.getDateOfExpiry());
            
            if (!existingRecords.isEmpty()) {
                // Record exists, use patch to update only the specified fields
                tableQueryService.patch(PASS_TABLE, filters, fields);
                log.info("Patched existing {} pass data", passType);
            } else {
                // New record, include all fields for creation
                fields.put("noticeNo", passInfo.getNoticeNo());
                fields.put("idNo", passInfo.getIdNo());
                fields.put("mhaPassExpiryDate", passInfo.getDateOfExpiry());
                tableQueryService.post(PASS_TABLE, fields);
                log.info("Created new {} pass data", passType);
            }
            
        } catch (Exception e) {
            log.error("Error processing pass data: {}", e.getMessage(), e);
        }
    }
    
    private void resolveAndUpdateAddress(FINDataResult result, String ownerDriverIndicator, String noticeNumber) {
        try {
            // Determine which address to use
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
                filters.put("noticeNo", noticeNumber);
                
                Map<String, Object> updateFields = new HashMap<>();
                if (blockHouseNo != null) updateFields.put("regBlkHseNo", blockHouseNo);
                if (floor != null) updateFields.put("regFloor", floor);
                if (unit != null) updateFields.put("regUnit", unit);
                if (streetName != null) updateFields.put("regStreet", streetName);
                if (buildingName != null) updateFields.put("regBldg", buildingName);
                if (postalCode != null) updateFields.put("regPostalCode", postalCode);
                
                tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
                log.info("Updated address for notice: {}", noticeNumber);
            }
            
        } catch (Exception e) {
            log.error("Error resolving and updating address: {}", e.getMessage(), e);
        }
    }
    
    private void checkPastRecordsAndGenerateWorkItem(String fin, String noticeNumber, FINDataResult result) {
        // Check for past records logic - placeholder
        boolean hasPastRecords = false; // This would be actual check
        
        if (!hasPastRecords) {
            result.getGeneratedWorkItems().add("No past record work item for owner");
            log.info("Work item placeholder: No past record found for owner");
        }
    }
    
    private void applyTransactionCode(String noticeNumber, String transactionCode) {
        applyTransactionCode(noticeNumber, transactionCode, null);
    }
    
    private void applyTransactionCode(String noticeNumber, String transactionCode, String remark) {
        // Transaction code application is handled by workflow/framework
        log.info("Transaction code {} to be applied for notice: {} {}", 
            transactionCode, noticeNumber, remark != null ? "with remark: " + remark : "");
    }
    
    private void updateNewNric(String noticeNumber, String newNric) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);

            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("newNric", newNric);

            tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
            log.info("Updated new_nric for notice: {}", noticeNumber);

        } catch (Exception e) {
            log.error("Error updating new_nric: {}", e.getMessage(), e);
        }
    }

    private void updatePassTableForPRSC(String fin, String noticeNumber, String uin, Date grantDate,
                                        String conversionType, String previousFin, String referencePeriod) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", fin);
            filters.put("noticeNo", noticeNumber);

            // Check if record exists
            List<Map<String, Object>> existingRecords = tableQueryService.query(PASS_TABLE, filters);

            Map<String, Object> fields = new HashMap<>();
            fields.put("previousFin", previousFin);

            if ("PR".equals(conversionType)) {
                fields.put("datePrGranted", grantDate);
                fields.put("referencePeriodAliveScpr", DataHiveDateUtil.convertEpochDaysToLocalDateTime(referencePeriod));
            } else if ("SC".equals(conversionType)) {
                fields.put("scGrantDate", grantDate);
                fields.put("referencePeriodScgrant", DataHiveDateUtil.convertEpochDaysToLocalDateTime(referencePeriod));
            }

            if (!existingRecords.isEmpty()) {
                // Update existing record
                tableQueryService.patch(PASS_TABLE, filters, fields);
                log.info("Updated {} data in ocms_dh_mha_pass for FIN: {}, notice: {}",
                    conversionType, fin, noticeNumber);
            } else {
                // Create new record
                fields.put("idNo", fin);
                fields.put("noticeNo", noticeNumber);
                tableQueryService.post(PASS_TABLE, fields);
                log.info("Created {} record in ocms_dh_mha_pass for FIN: {}, notice: {}",
                    conversionType, fin, noticeNumber);
            }

        } catch (Exception e) {
            log.error("Error updating PASS table for {}: {}", conversionType, e.getMessage(), e);
        }
    }
    
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
    
    private String getStringValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        return field.isNull() ? null : field.asText();
    }
    
    @Override
    public DataHiveCommonService getCommonService() {
        return commonService;
    }
}