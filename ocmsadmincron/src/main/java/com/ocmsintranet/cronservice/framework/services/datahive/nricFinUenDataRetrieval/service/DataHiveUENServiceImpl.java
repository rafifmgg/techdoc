package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveDateUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UENDataResult;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Implementation of DataHiveUENService for retrieving business entity data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataHiveUENServiceImpl implements DataHiveUENService {

    private final DataHiveUtil dataHiveUtil;
    private final TableQueryService tableQueryService;
    private final OcmsSuspensionReasonService suspensionReasonService;
    private final com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient;
    
    private static final String COMPANY_DETAIL_TABLE = "ocms_dh_acra_company_detail";
    private static final String SHAREHOLDER_INFO_TABLE = "ocms_dh_acra_shareholder_info";
    private static final String BOARD_INFO_TABLE = "ocms_dh_acra_board_info";
    private static final String OFFENCE_NOTICE_TABLE = "ocms_offence_notice_owner_driver";
    private static final String OFFENCE_NOTICE_TABLE_ADDR = "ocms_offence_notice_owner_driver_addr";
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    @Override
    public UENDataResult retrieveUENData(String uen, String noticeNumber) {
        log.info("Retrieving UEN data for notice: {}", noticeNumber);
        
        UENDataResult result = UENDataResult.builder()
            .shareholderData(new ArrayList<>())
            .boardData(new ArrayList<>())
            .rawResults(new HashMap<>())
            .tsAcrApplied(false)
            .hasError(false)
            .notFoundUENs(new ArrayList<>())
            .build();
        
        try {
            // Step 1: Get company registration data
            getCompanyRegistrationData(uen, noticeNumber, result);

            // COMMENTED OUT: Shareholder and board info retrieval disabled per user request
            /*
            // Step 2: Get shareholder and board information ONLY for deregistered companies
            // This aligns with the flow diagram specification
            if (result.getCompanyInfo() != null && result.getCompanyInfo().isDeregistered()) {
                log.info("Company is deregistered, retrieving shareholder and board info");

                // Step 2a: Get shareholder information
                getShareholderInfo(uen, noticeNumber, result);

                // Step 2b: Get board information
                getBoardInfo(uen, noticeNumber, result);
            } else if (result.getCompanyInfo() != null && result.getCompanyInfo().isFound()) {
                log.info("Company is registered (not deregistered), skipping shareholder and board info");
            } else {
                log.info("Company not found, skipping shareholder and board info");
            }
            */
            log.info("Shareholder and board info retrieval is DISABLED - skipping");

        } catch (Exception e) {
            log.error("Error retrieving UEN data: {}", e.getMessage(), e);

            // Apply TS-SYS for interface/query errors
            try {
                String errorMessage = "DataHive query error - " + e.getMessage();
                applyTSSYS(noticeNumber, errorMessage);
                log.info("Applied TS-SYS suspension due to DataHive interface error");
            } catch (Exception suspensionError) {
                log.error("Failed to apply TS-SYS suspension after query error: {}", suspensionError.getMessage(), suspensionError);
            }

            result.setHasError(true);
            result.setErrorMessage("Failed to retrieve UEN data: " + e.getMessage());
        }

        return result;
    }
    
    private void getCompanyRegistrationData(String uen, String noticeNumber, UENDataResult result) {
        try {
            // Step 1: Check registered companies first
            String registeredQuery = String.format(
                "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, " +
                "ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, ADDRESS_ONE, " +
                "ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, " +
                "ADDRESS_ONE_STREET_NAME, ADDRESS_ONE_BUILDING_NAME, UEN " +
                "FROM V_DH_ACRA_FIRMINFO_R WHERE UEN = '%s'", uen
            );
            
            log.debug("Executing registered company query");
            JsonNode registeredData = dataHiveUtil.executeQueryAsyncDataOnly(registeredQuery).get();
            result.getRawResults().put("registeredCompany", registeredData);
            
            boolean companyFound = false;
            UENDataResult.CompanyInfo companyInfo = null;
            
            if (registeredData != null && registeredData.isArray() && registeredData.size() > 0) {
                companyInfo = processCompanyData(registeredData.get(0), uen, noticeNumber, false);
                companyFound = true;
                log.info("UEN found in registered companies, skipping de-registered query");
            } else {
                // Step 2: Only check de-registered companies if not found in registered
                log.debug("UEN not found in registered companies, checking de-registered");

                String deregisteredQuery = String.format(
                    "SELECT ENTITY_NAME, ENTITY_TYPE, REGISTRATION_DATE, DEREGISTRATION_DATE, " +
                    "ENTITY_STATUS_CODE, COMPANY_TYPE_CODE, ADDRESS_ONE, " +
                    "ADDRESS_ONE_BLOCK_HOUSE_NUMBER, ADDRESS_ONE_LEVEL_NUMBER, " +
                    "ADDRESS_ONE_UNIT_NUMBER, ADDRESS_ONE_POSTAL_CODE, " +
                    "ADDRESS_ONE_STREET_NAME, ADDRESS_ONE_BUILDING_NAME, UEN " +
                    "FROM V_DH_ACRA_FIRMINFO_D WHERE UEN = '%s'", uen
                );

                log.debug("Executing de-registered company query");
                JsonNode deregisteredData = dataHiveUtil.executeQueryAsyncDataOnly(deregisteredQuery).get();
                result.getRawResults().put("deregisteredCompany", deregisteredData);

                if (deregisteredData != null && deregisteredData.isArray() && deregisteredData.size() > 0) {
                    companyInfo = processCompanyData(deregisteredData.get(0), uen, noticeNumber, true);
                    companyFound = true;
                }
            }

            if (companyFound && companyInfo != null) {
                // Update company details and address
                updateCompanyDetails(companyInfo);
                updateCompanyAddress(companyInfo, noticeNumber);

                // Apply TS-ACR ONLY for de-registered companies
                if (companyInfo.isDeregistered()) {
                    log.info("De-registered company found, applying TS-ACR");
                    applyTSACR(noticeNumber);
                    result.setTsAcrApplied(true);
                } else {
                    // Registered company found - no suspension needed
                    log.info("Registered company found, no suspension applied");
                }

            } else {
                // Company not found in both registered and de-registered
                companyInfo = UENDataResult.CompanyInfo.builder()
                    .uen(uen)
                    .noticeNo(noticeNumber)
                    .isFound(false)
                    .build();

                // Apply TS-SYS (NOT TS-ACR) for UEN not found
                log.warn("UEN not found in either registered or de-registered companies, applying TS-SYS");
                applyTSSYS(noticeNumber, "UEN not found in DataHive");

                // Set error for workflow to handle email notification
                result.setHasError(true);
                result.setErrorMessage("UEN not found in ACRA registered or de-registered companies");
                result.getNotFoundUENs().add(uen);

                log.warn("UEN not found in either registered or de-registered companies for notice: {}", noticeNumber);
            }
            
            result.setCompanyInfo(companyInfo);
            
        } catch (Exception e) {
            log.error("Error getting company registration data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get company registration data", e);
        }
    }
    
    private UENDataResult.CompanyInfo processCompanyData(JsonNode record, String uen, String noticeNumber, boolean isDeregistered) {
        // Debug logging for registration date
        String rawRegistrationDate = getStringValue(record, "REGISTRATION_DATE");
        LocalDateTime parsedRegistrationDate = parseDate(rawRegistrationDate);
        
        // Debug logging for deregistration date
        String rawDeregistrationDate = getStringValue(record, "DEREGISTRATION_DATE");
        LocalDateTime parsedDeregistrationDate = parseDate(rawDeregistrationDate);
        
        UENDataResult.CompanyInfo companyInfo = UENDataResult.CompanyInfo.builder()
            .uen(uen)
            .noticeNo(noticeNumber)
            .entityName(getStringValue(record, "ENTITY_NAME"))
            .entityType(getStringValue(record, "ENTITY_TYPE"))
            .registrationDate(parsedRegistrationDate)
            .deregistrationDate(parsedDeregistrationDate)
            .entityStatusCode(getStringValue(record, "ENTITY_STATUS_CODE"))
            .companyTypeCode(getStringValue(record, "COMPANY_TYPE_CODE"))
            .addressOne(getStringValue(record, "ADDRESS_ONE"))
            .addressOneBlockHouseNumber(getStringValue(record, "ADDRESS_ONE_BLOCK_HOUSE_NUMBER"))
            .addressOneLevelNumber(getStringValue(record, "ADDRESS_ONE_LEVEL_NUMBER"))
            .addressOneUnitNumber(getStringValue(record, "ADDRESS_ONE_UNIT_NUMBER"))
            .addressOnePostalCode(getStringValue(record, "ADDRESS_ONE_POSTAL_CODE"))
            .addressOneStreetName(getStringValue(record, "ADDRESS_ONE_STREET_NAME"))
            .addressOneBuildingName(getStringValue(record, "ADDRESS_ONE_BUILDING_NAME"))
            .isFound(true)
            .isDeregistered(isDeregistered)
            .build();
        
        return companyInfo;
    }
    
    private void updateCompanyDetails(UENDataResult.CompanyInfo companyInfo) {
        try {
            String uen = companyInfo.getUen();
            
            // Check if company already exists
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
                // Record exists, use patch to update only the specified fields
                tableQueryService.patch(COMPANY_DETAIL_TABLE, filters, fields);
                log.info("Patched existing company details");
            } else {
                // New record, include the UEN in fields for creation
                fields.put("uen", uen);
                fields.put("noticeNo", companyInfo.getNoticeNo());
                tableQueryService.post(COMPANY_DETAIL_TABLE, fields);
                log.info("Created new company details");
            }
            
        } catch (Exception e) {
            log.error("Error updating company details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update company details", e);
        }
    }
    
    private void updateCompanyAddress(UENDataResult.CompanyInfo companyInfo, String noticeNumber) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);
            filters.put("ownerDriverIndicator", "O");
            filters.put("typeOfAddress", "mha_reg");

            List<Map<String, Object>> existingRecords = tableQueryService.query(OFFENCE_NOTICE_TABLE_ADDR, filters);
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("noticeNo", companyInfo.getNoticeNo());
            fields.put("blkHseNo", companyInfo.getAddressOneBlockHouseNumber());
            fields.put("floorNo", companyInfo.getAddressOneLevelNumber());
            fields.put("unitNo", companyInfo.getAddressOneUnitNumber());
            fields.put("streetName", companyInfo.getAddressOneStreetName());
            fields.put("bldgName", companyInfo.getAddressOneBuildingName());
            fields.put("postalCode", companyInfo.getAddressOnePostalCode());

            if (!existingRecords.isEmpty()) {
                // Record exists, use patch to update only the specified fields
                tableQueryService.patch(OFFENCE_NOTICE_TABLE_ADDR, filters, fields);
                log.info("Patched existing address details for noticeNumber: {}", noticeNumber);
            } else {
                // New record, include the UEN in fields for creation
                fields.put("ownerDriverIndicator", "O");
                fields.put("typeOfAddress", "mha_reg");
                tableQueryService.post(OFFENCE_NOTICE_TABLE_ADDR, fields);
                log.info("Created new address details for noticeNumber: {}", noticeNumber);
            }
            
        } catch (Exception e) {
            log.error("Error updating company address: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update company address", e);
        }
    }
    
    private void getShareholderInfo(String uen, String noticeNumber, UENDataResult result) {
        try {
            String query = String.format(
                "SELECT SHAREHOLDER_CATEGORY, SHAREHOLDER_COMPANY_PROFILE_UEN, " +
                "SHAREHOLDER_PERSON_ID_NO, SHAREHOLDER_SHARE_ALLOTTED_NO, " +
                "COMPANY_UEN " +
                "FROM V_DH_ACRA_SHAREHOLDER_GZ WHERE COMPANY_UEN = '%s'", uen
            );
            
            log.debug("Executing shareholder query");
            JsonNode shareholderData = dataHiveUtil.executeQueryAsyncDataOnly(query).get();
            result.getRawResults().put("shareholder", shareholderData);
            
            if (shareholderData != null && shareholderData.isArray() && shareholderData.size() > 0) {
                log.debug("Found {} shareholder records", shareholderData.size());
                if (shareholderData.size() > 0 && shareholderData.get(0).isArray()) {
                    log.debug("First shareholder record: {}", shareholderData.get(0));
                }
                
                for (JsonNode record : shareholderData) {
                    UENDataResult.ShareholderInfo shareholder = UENDataResult.ShareholderInfo.builder()
                        .companyUen(uen)
                        .noticeNo(noticeNumber)
                        .category(getStringValue(record, "SHAREHOLDER_CATEGORY"))
                        .companyProfilUen(getStringValue(record, "SHAREHOLDER_COMPANY_PROFILE_UEN"))
                        .personIdNo(getStringValue(record, "SHAREHOLDER_PERSON_ID_NO"))
                        .shareAllotedNo(getStringValue(record, "SHAREHOLDER_SHARE_ALLOTTED_NO"))
                        .build();
                    
                    result.getShareholderData().add(shareholder);
                    
                    // Store in database
                    updateShareholderInfo(shareholder);
                }
                
                // Check if Listed Company
                if (result.getCompanyInfo() != null && 
                    "LC".equals(result.getCompanyInfo().getEntityType())) {
                    updateGazettedFlag(noticeNumber);
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting shareholder info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get shareholder info", e);
        }
    }
    
    private void updateShareholderInfo(UENDataResult.ShareholderInfo shareholder) {
        try {
            // Skip records with NULL person ID - these are corporate shareholders
            if (shareholder.getPersonIdNo() == null || shareholder.getPersonIdNo().trim().isEmpty()) {
                // For corporate shareholders, use the company UEN as the ID
                if (shareholder.getCompanyProfilUen() != null && !shareholder.getCompanyProfilUen().trim().isEmpty()) {
                    // Truncate company UEN to 9 characters if needed
                    String idNo = shareholder.getCompanyProfilUen().trim();
                    if (idNo.length() > 9) {
                        idNo = idNo.substring(0, 9);
                        log.warn("Truncated company UEN from {} to {} for database constraint", 
                            shareholder.getCompanyProfilUen(), idNo);
                    }
                    
                    // Check if record exists
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
                        // Record exists, use patch to update only the specified fields
                        tableQueryService.patch(SHAREHOLDER_INFO_TABLE, filters, fields);
                        log.info("Patched existing corporate shareholder info for UEN: {} with company UEN: {}", 
                            shareholder.getCompanyUen(), idNo);
                    } else {
                        // New record, include all fields for creation
                        fields.put("companyUen", shareholder.getCompanyUen());
                        fields.put("noticeNo", shareholder.getNoticeNo());
                        fields.put("personIdNo", idNo);
                        tableQueryService.post(SHAREHOLDER_INFO_TABLE, fields);
                        log.info("Created new corporate shareholder info for UEN: {} with company UEN: {}", 
                            shareholder.getCompanyUen(), idNo);
                    }
                } else {
                    log.warn("Skipping shareholder record with both NULL person ID and company UEN for UEN: {}", 
                        shareholder.getCompanyUen());
                }
            } else {
                // Individual shareholder with person ID
                // Truncate person ID to 12 characters if needed (data dict says varchar(12))
                String idNo = shareholder.getPersonIdNo().trim();
                if (idNo.length() > 12) {
                    idNo = idNo.substring(0, 12);
                    log.warn("Truncated person ID from {} to {} for database constraint", 
                        shareholder.getPersonIdNo(), idNo);
                }
                
                // Check if record exists
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
                    // Record exists, use patch to update only the specified fields
                    tableQueryService.patch(SHAREHOLDER_INFO_TABLE, filters, fields);
                    log.info("Patched existing individual shareholder info for UEN: {} with person ID: {}", 
                        shareholder.getCompanyUen(), idNo);
                } else {
                    // New record, include all fields for creation
                    fields.put("companyUen", shareholder.getCompanyUen());
                    fields.put("noticeNo", shareholder.getNoticeNo());
                    fields.put("personIdNo", idNo);
                    tableQueryService.post(SHAREHOLDER_INFO_TABLE, fields);
                    log.info("Created new individual shareholder info for UEN: {} with person ID: {}", 
                        shareholder.getCompanyUen(), idNo);
                }
            }
            
        } catch (Exception e) {
            log.error("Error updating shareholder info: {}", e.getMessage(), e);
        }
    }
    
    private void updateGazettedFlag(String noticeNumber) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNumber);
            
            Map<String, Object> updateFields = new HashMap<>();
            updateFields.put("gazettedFlag", "Y");
            
            tableQueryService.patch(OFFENCE_NOTICE_TABLE, filters, updateFields);
            log.info("Updated gazetted_flag to Y for notice: {}", noticeNumber);
            
        } catch (Exception e) {
            log.error("Error updating gazetted flag: {}", e.getMessage(), e);
        }
    }
    
    private void getBoardInfo(String uen, String noticeNumber, UENDataResult result) {
        try {
            String query = String.format(
                "SELECT POSITION_APPOINTMENT_DATE, POSITION_WITHDRAWN_WITHDRAWAL_DATE, " +
                "PERSON_IDENTIFICATION_NUMBER, ENTITY_UEN, POSITION_HELD_CODE, " +
                "REFERENCE_PERIOD " +
                "FROM V_DH_ACRA_BOARD_INFO_FULL WHERE ENTITY_UEN = '%s'", uen
            );
            
            log.debug("Executing board info query");
            JsonNode boardData = dataHiveUtil.executeQueryAsyncDataOnly(query).get();
            result.getRawResults().put("boardInfo", boardData);
            
            if (boardData != null && boardData.isArray() && boardData.size() > 0) {
                for (JsonNode record : boardData) {
                    UENDataResult.BoardInfo board = UENDataResult.BoardInfo.builder()
                        .entityUen(uen)
                        .noticeNo(noticeNumber)
                        .positionAppointmentDate(parseDate(getStringValue(record, "POSITION_APPOINTMENT_DATE")))
                        .positionWithdrawnDate(parseDate(getStringValue(record, "POSITION_WITHDRAWN_WITHDRAWAL_DATE")))
                        .personIdNo(getStringValue(record, "PERSON_IDENTIFICATION_NUMBER"))
                        .positionHeldCode(getStringValue(record, "POSITION_HELD_CODE"))
                        .referencePeriod(DataHiveDateUtil.convertEpochDaysToLocalDateTime(getStringValue(record, "REFERENCE_PERIOD")))
                        .build();
                    
                    result.getBoardData().add(board);
                    
                    // Store in database
                    updateBoardInfo(board);
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting board info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get board info", e);
        }
    }
    
    private void updateBoardInfo(UENDataResult.BoardInfo board) {
        try {
            // Skip if person ID is null (required field)
            if (board.getPersonIdNo() == null || board.getPersonIdNo().trim().isEmpty()) {
                log.warn("Skipping board info record with NULL person ID for UEN: {}", board.getEntityUen());
                return;
            }
            
            // Truncate person ID to 12 characters if needed (data dict says varchar(12))
            String idNo = board.getPersonIdNo().trim();
            if (idNo.length() > 12) {
                idNo = idNo.substring(0, 12);
                log.warn("Truncated person ID from {} to {} for database constraint", 
                    board.getPersonIdNo(), idNo);
            }
            
            // Check if record exists
            Map<String, Object> filters = new HashMap<>();
            filters.put("entityUen", board.getEntityUen());
            filters.put("noticeNo", board.getNoticeNo());
            filters.put("personIdNo", idNo);
            
            List<Map<String, Object>> existingRecords = tableQueryService.query(BOARD_INFO_TABLE, filters);
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("positionHeldCode", board.getPositionHeldCode());
            fields.put("positionAppointmentDate", board.getPositionAppointmentDate());
            fields.put("positionWithdrawnDate", board.getPositionWithdrawnDate());
            fields.put("referencePeriod", board.getReferencePeriod());
            log.info("board: {}", board);
            log.info("referencePeriod: {}", board.getReferencePeriod());

            // Only add referencePeriod if it's not null
            if (board.getReferencePeriod() != null) {
                fields.put("referencePeriod", board.getReferencePeriod());
            }
            
            if (!existingRecords.isEmpty()) {
                // Record exists, use patch to update only the specified fields
                tableQueryService.patch(BOARD_INFO_TABLE, filters, fields);
                log.info("Patched existing board info for UEN: {} with person ID: {}", board.getEntityUen(), idNo);
            } else {
                // New record, include all fields for creation
                fields.put("entityUen", board.getEntityUen());
                fields.put("noticeNo", board.getNoticeNo());
                fields.put("personIdNo", idNo);
                tableQueryService.post(BOARD_INFO_TABLE, fields);
                log.info("Created new board info for UEN: {} with person ID: {}", board.getEntityUen(), idNo);
            }
            
        } catch (Exception e) {
            log.error("Error updating board info: {}", e.getMessage(), e);
        }
    }
    
    private void applyTSACR(String noticeNumber) {
        try {
            log.info("[TS-ACR] Applying TS-ACR suspension via API for notice: {}", noticeNumber);

            // Apply suspension via API
            // Note: TS-ACR has NO revival date (daysToRevive = null)
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

        } catch (Exception e) {
            log.error("[TS-ACR] Error applying TS-ACR suspension for notice {}: {}", noticeNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to apply TS-ACR suspension", e);
        }
    }

    /**
     * Apply TS-SYS suspension for interface errors or UEN not found scenarios
     *
     * @param noticeNumber The offence notice number
     * @param errorMessage The error message to record in suspension_remarks
     */
    private void applyTSSYS(String noticeNumber, String errorMessage) {
        try {
            log.info("[TS-SYS] Applying TS-SYS suspension via API for notice: {} with reason: {}", noticeNumber, errorMessage);

            // Query suspension reason table to get no_of_days_for_revival
            Integer daysForRevival = suspensionReasonService.getNoOfDaysForRevival(SystemConstant.SuspensionType.TEMPORARY, SystemConstant.SuspensionReason.SYS);
            log.info("[TS-SYS] Retrieved {} days for revival for TS-SYS from suspension reason table", daysForRevival);

            // Apply suspension via API
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

        } catch (Exception e) {
            log.error("[TS-SYS] Error applying TS-SYS suspension for notice {}: {}", noticeNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to apply TS-SYS suspension", e);
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr, formatter);
                log.info("localDate before: {}", localDate);
                // Convert LocalDate to LocalDateTime at start of day (00:00:00)
                LocalDateTime localDateTime = localDate.atStartOfDay();
                log.info("localDateTime after: {}", localDateTime);
                return localDateTime;
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
    
    private String getStringValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        
        // Handle array data format from DataHive
        if (node.isArray()) {
            // Map field names to array indices based on DataHive response format
            // Format: ["SHAREHOLDER_CATEGORY", "SHAREHOLDER_COMPANY_PROFILE_UEN", "SHAREHOLDER_PERSON_ID_NO", "SHAREHOLDER_SHARE_ALLOTTED_NO", "COMPANY_UEN"]
            // Example: ["1", "S98765432D", null, "100000", "201234567A"]
            Map<String, Integer> fieldIndexMap = new HashMap<>();
            fieldIndexMap.put("SHAREHOLDER_CATEGORY", 0);
            fieldIndexMap.put("SHAREHOLDER_COMPANY_PROFILE_UEN", 1);
            fieldIndexMap.put("SHAREHOLDER_PERSON_ID_NO", 2);
            fieldIndexMap.put("SHAREHOLDER_SHARE_ALLOTTED_NO", 3);
            fieldIndexMap.put("COMPANY_UEN", 4);
            
            // For company data fields
            fieldIndexMap.put("ENTITY_NAME", 0);
            fieldIndexMap.put("ENTITY_TYPE", 1);
            fieldIndexMap.put("REGISTRATION_DATE", 2);
            fieldIndexMap.put("DEREGISTRATION_DATE", 3);
            fieldIndexMap.put("ENTITY_STATUS_CODE", 4);
            fieldIndexMap.put("COMPANY_TYPE_CODE", 5);
            fieldIndexMap.put("ADDRESS_ONE", 6);
            fieldIndexMap.put("ADDRESS_ONE_BLOCK_HOUSE_NUMBER", 7);
            fieldIndexMap.put("ADDRESS_ONE_LEVEL_NUMBER", 8);
            fieldIndexMap.put("ADDRESS_ONE_UNIT_NUMBER", 9);
            fieldIndexMap.put("ADDRESS_ONE_POSTAL_CODE", 10);
            fieldIndexMap.put("ADDRESS_ONE_STREET_NAME", 11);
            fieldIndexMap.put("ADDRESS_ONE_BUILDING_NAME", 12);
            fieldIndexMap.put("UEN", 13);
            
            // For board info fields
            fieldIndexMap.put("POSITION_APPOINTMENT_DATE", 0);
            fieldIndexMap.put("POSITION_WITHDRAWN_WITHDRAWAL_DATE", 1);
            fieldIndexMap.put("PERSON_IDENTIFICATION_NUMBER", 2);
            fieldIndexMap.put("ENTITY_UEN", 3);
            fieldIndexMap.put("POSITION_HELD_CODE", 4);
            fieldIndexMap.put("REFERENCE_PERIOD", 5);
            
            Integer index = fieldIndexMap.get(fieldName);
            if (index != null && index < node.size()) {
                JsonNode value = node.get(index);
                return value.isNull() ? null : value.asText();
            }
            return null;
        }
        
        // Handle regular object format
        if (!node.has(fieldName)) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        return field.isNull() ? null : field.asText();
    }
}