package com.ocmsintranet.apiservice.workflows.notice_creation.core;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit.WebTransactionAuditService;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;

import java.time.format.DateTimeFormatter;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.ocmsintranet.apiservice.utilities.AzureKeyVaultUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import com.ocmsintranet.apiservice.crud.exception.ErrorCodes;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetailService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterId;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CreateNoticeServiceImpl implements CreateNoticeService {

    @Value("${ocms.APIM.secretName}")
    private String apimSecretHeaderName;
    
    @Autowired
    private WebTransactionAuditService webTransactionAuditService;
    
    @Autowired
    private AzureKeyVaultUtil azureKeyVaultUtil;
    
    @Autowired
    private OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    @Autowired
    private EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    @Autowired
    private OcmsOffenceNoticeDetailService offenceNoticeDetailService;
    
    @Autowired
    private SuspendedNoticeService ocmsSuspendedNoticeService;
    
    @Autowired
    private OcmsSuspensionReasonService ocmsSuspensionReasonService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ProcessCreateNoticeHelper processCreateNoticeHelper;
    
    @Autowired
    private ProcessREPCCSHelper processREPCCSHelper;
    
    @Autowired
    private CreateNoticeHelper createNoticeHelper;
    
    @Autowired
    private FileProcessingHelper fileProcessingHelper;
    
    @Autowired
    private com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.SpecialVehUtils specialVehUtils;

    @Autowired
    private com.ocmsintranet.apiservice.utilities.emailutility.BatchErrorCollector batchErrorCollector;

    @Autowired
    private ValidationServices validationServices;

    @Autowired
    private RepWebHookPayloadValidator repWebHookPayloadValidator;

    @Autowired
    private AutoSuspensionHelper autoSuspensionHelper;

    private AzureKeyVaultUtil keyVaultUtil;

    @Value("${certis.bluekey}")
    private String apimBluekey;

    private final ObjectMapper objectMapper;

    public CreateNoticeServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Helper method to extract values from string using a prefix
     */
    /**
     * Helper method to extract values from string using a prefix
     */
    private String extractValue(String source, String prefix) {
        if (source == null || prefix == null) {
            return null;
        }
        
        int startIndex = source.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += prefix.length();
        int endIndex = source.indexOf(",", startIndex);
        
        if (endIndex == -1) {
            // If no comma, look for <End> tag or just take the rest
            endIndex = source.indexOf("<End>", startIndex);
            if (endIndex == -1) {
                return source.substring(startIndex).trim();
            }
        }
        
        return source.substring(startIndex, endIndex).trim();
    }
    
    /**
     * Helper method to mask sensitive data in logs
     * @param data The sensitive data to mask
     * @return A masked version of the data
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "[EMPTY]";
        }

        if (data.length() <= 4) {
            return "****";
        }

        // Keep first and last character, mask the rest
        return data.substring(0, 2) +
               "*".repeat(Math.min(data.length() - 4, 8)) +
               data.substring(data.length() - 2);
    }

    /**
     * OCMS 21: Get suspension duration in days from system parameters
     * @param suspensionType Type of suspension (DBB, OLD, etc.)
     * @param defaultDays Default duration if parameter not found
     * @return Suspension duration in days
     */
    private int getSuspensionDurationDays(String suspensionType, int defaultDays) {
        try {
            String parameterId = suspensionType + "_SUSPENSION_DURATION";
            ParameterId paramId = new ParameterId(parameterId, "DAYS");

            java.util.Optional<Parameter> paramOpt = parameterService.getById(paramId);
            if (paramOpt.isPresent()) {
                String value = paramOpt.get().getValue();
                int days = Integer.parseInt(value);
                log.info("Retrieved {} suspension duration from parameters: {} days", suspensionType, days);
                return days;
            } else {
                log.warn("Parameter {} not found, using default: {} days", parameterId, defaultDays);
                return defaultDays;
            }
        } catch (Exception e) {
            log.error("Error retrieving {} suspension duration parameter, using default: {} days. Error: {}",
                    suspensionType, defaultDays, e.getMessage());
            return defaultDays;
        }
    }

    // Main entry points for each endpoint
    @Override
    public ResponseEntity<NoticeResponse> processCreateNotice(HttpServletRequest request, String rawData) {
        log.info("Processing standard create notice request");
        // Initialize batch error collector for this request
        batchErrorCollector.initBatch("/v1/createnotice");
        return processNoticeWithDtoMapping(rawData);
    }
    
    @Override
    public ResponseEntity<NoticeResponse> processStaffCreateNotice(HttpServletRequest request, String rawData) {
        log.info("Processing staff create notice request");
        return processNoticeWithDtoMapping(rawData);
    }
    
    @Override
    public ResponseEntity<NoticeResponse> processRepccsCreateNotice(RepWebHookPayloadDto rawData) {
        log.info("Processing Repccs Webhook create notice request");

        String webTxnId =null;

        System.out.println("value of txnid"+ webTxnId);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            webTxnId = SystemConstant.Rep.REP_TXN_CODE+ LocalDateTime.now().format(formatter);
            validationServices.saveAudit(rawData,webTxnId);

            repWebHookPayloadValidator.validateMandatoryFields(rawData);

            RepCreateNoticeDto requestDto = validationServices.mapToDto(rawData);
            String formattedPayload = objectMapper.writeValueAsString(requestDto);


            String noticeNo = null;
            try {
                // Try to parse the raw JSON to get the noticeNo field
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(formattedPayload);
                if (jsonNode.has("noticeNo")) {
                    noticeNo = jsonNode.get("noticeNo").asText();
                    log.info("Extracted noticeNo from JSON: {}", noticeNo);
                } else {
                    log.warn("No 'noticeNo' field found in JSON payload");
                    // Return error if no noticeNo found
                    NoticeResponse errorResponse = new NoticeResponse();
                    errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
                    validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_3,new IllegalArgumentException("Invalid input format or failed validation"));
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                log.error("Error extracting noticeNo from JSON: {}", e.getMessage());
                NoticeResponse errorResponse = new NoticeResponse();
                validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_3,new IllegalArgumentException("Invalid input format or failed validation"));
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Check for duplicate notice (noticeNo is guaranteed to be non-null here)
            if (noticeNo.trim().isEmpty()) {
                log.warn("Empty notice number provided");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
                validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_3,new IllegalArgumentException("Invalid input format or failed validation"));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            log.info("Checking if notice with noticeNo '{}' already exists", noticeNo);
            boolean noticeExists = validationServices.checkNoticeExisting(noticeNo);

            if (noticeExists) {
                log.warn("Duplicate notice detected with noticeNo: {}, stopping process", noticeNo);
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.READY_USED, "Invalid input format or failed validation");
                validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_7,new IllegalArgumentException("Invalid input format or failed validation"));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            log.info("No duplicate found for noticeNo: {}, continuing with processing", noticeNo);
            return processNoticeWithDtoMapping(formattedPayload);

        } catch (IllegalArgumentException e) {

            log.warn("Validation failed: {}", e.getMessage());
            NoticeResponse errorResponse = new NoticeResponse();
            validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_2,new SecurityException("Invalid input format or failed validation"));
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            log.error("Unexpected error in processRepccsCreateNotice: {}", e.getMessage(), e);
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.INTERNAL_SERVER_ERROR, "System Error: " + e.getMessage());
            validationServices.updateAuditError(webTxnId,SystemConstant.Rep.STEP_9,new SecurityException("Invalid input format or failed validation"));
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    
    @Override
    public ResponseEntity<NoticeResponse> processEHTSFTP(HttpServletRequest request, String rawData) {
        log.info("Processing EHT SFTP create notice request");
        return processNoticeWithDtoMapping(rawData);
    }


    @Override
    public ResponseEntity<NoticeResponse> processEHTWebhook(HttpServletRequest request, String rawData) {
        log.info("Processing EHT Webhook create notice request");
        log.info("check the payload from certis ++ " + rawData);

        try {
            // Extract noticeNo directly from JSON
            String noticeNo = null;
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(rawData);
            try {

                if (jsonNode.has("noticeNo")) {
                    noticeNo = jsonNode.get("noticeNo").asText();

                } else {
                    log.warn("No 'noticeNo' field found in JSON payload");
                    // Return error if no noticeNo found
                    NoticeResponse errorResponse = new NoticeResponse();
                    errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                log.error("Error extracting noticeNo from JSON: {}", e.getMessage());
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Invalid JSON format: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Check for duplicate notice (noticeNo is guaranteed to be non-null here)
            if (noticeNo.trim().isEmpty()) {
                log.warn("Empty notice number provided");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            String subsystemlabel = jsonNode.get("subsystemLabel").asText();
            if (subsystemlabel == null || subsystemlabel.trim().isEmpty()) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation - not Exist");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            String prefix = subsystemlabel.trim();
            if (prefix.length() < 3) {
                log.info("Checking if subsystemlabel Length < 3");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation - subsystemlabel Length < 3");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            if (prefix.length() > 8) {
                log.info("Checking if subsystemlabel Length > 8");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation - subsystemlabel Length > 8");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            String labelPrefix = prefix.substring(0, 3);
            if (!labelPrefix.matches("\\d{3}")) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation - Subsystem label must be a 3-digit numeric value");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            int labelValue = Integer.parseInt(labelPrefix);
            if (labelValue < 30 || labelValue > 999) {
                log.info("Checking if subsystemlabel Not in Range 030 - 999");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.MISSING_MANDATORY, "Invalid input format or failed validation - subsystemlabel Not in Range 030 - 999");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            log.info("Checking if notice with noticeNo '{}' already exists", noticeNo);
            boolean noticeExists = validationServices.checkNoticeExisting(noticeNo);

            if (noticeExists) {
                log.warn("Duplicate notice detected with noticeNo: {}, stopping process", noticeNo);
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.IM_USED, "Notice no Already exists");
                return new ResponseEntity<>(errorResponse, HttpStatus.IM_USED);
            }

            CesCreateNoticeDto requestDto = objectMapper.readValue(rawData, CesCreateNoticeDto.class);
            //note new update for validate offence Type
            //rolled back to 8 as user meeting 24 oct
            requestDto.setSubsystemLabel(prefix);


            try {
            CesCreateNoticeDto repDto = validationServices.validateToDto(requestDto);
            String finalRawData = objectMapper.writeValueAsString(repDto);
            log.info("No duplicate found for noticeNo: {}, continuing with processing", noticeNo);
            return processNoticeWithDtoMapping(finalRawData);
            } catch (Exception e) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(
                        ErrorCodes.IM_USED,
                        "Invalid input format or failed validation - Invalid Offence Rule Code"
                );
                return new ResponseEntity<>(errorResponse, HttpStatus.IM_USED);
            }

        } catch (Exception e) {
            log.error("Error in duplicate notice check: {}", e.getMessage());
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.INTERNAL_SERVER_ERROR, "Error in duplicate check: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<NoticeResponse> processPlusCreateNotice(HttpServletRequest request, String rawData) {
        log.info("Processing PLUS create notice request");
        try {
            // Check if input is array or single object
            boolean isArray = rawData.trim().startsWith("[");

            if (isArray) {
                // Handle array of objects
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> noticeList = objectMapper.readValue(rawData, List.class);

                for (Map<String, Object> notice : noticeList) {
                    if (!notice.containsKey("creDate") || notice.get("creDate") == null) {
                        notice.put("creDate", LocalDateTime.now().toString());
                        log.info("Added creDate for notice: {}", notice.get("noticeNo"));
                    }
                }
                rawData = objectMapper.writeValueAsString(noticeList);
            } else {
                // Handle single object
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonMap = objectMapper.readValue(rawData, Map.class);

                if (!jsonMap.containsKey("creDate") || jsonMap.get("creDate") == null) {
                    jsonMap.put("creDate", LocalDateTime.now().toString());
                    log.info("Added creDate for notice: {}", jsonMap.get("noticeNo"));
                }
                rawData = objectMapper.writeValueAsString(jsonMap);
            }

            // Parse the input JSON
            Map<String, Object> mappedData = mapJsonToDtos(rawData);

            // Check for errors in mapping
            if (mappedData.containsKey("error")) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Invalid JSON format: " + mappedData.get("error"));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Extract the DTO list
            @SuppressWarnings("unchecked")
            List<OffenceNoticeDto> dtoList = (List<OffenceNoticeDto>) mappedData.get("dtoList");

            // Set subsystem label for PLUS for all DTOs
            for (OffenceNoticeDto dto : dtoList) {
                dto.setSubsystemLabel(SystemConstant.Subsystem.PLUS_CODE); // Set PLUS subsystem label
                log.info("Set subsystemLabel to {} (PLUS) for notice with vehicle: {}",
                        SystemConstant.Subsystem.PLUS_CODE, dto.getVehicleNo());
            }

            // Update the mapped data with modified DTOs
            mappedData.put("dtoList", dtoList);

            // Continue with standard processing
            return processNoticeWithDtoMapping(rawData, mappedData);
        } catch (Exception e) {
            log.error("Error in processing PLUS notice: {}", e.getMessage(), e);
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.INTERNAL_SERVER_ERROR, "Processing Error: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Common method for processing notice requests that come in standard DTO format with pre-mapped data
     */
    private ResponseEntity<NoticeResponse> processNoticeWithDtoMapping(String inputString, Map<String, Object> mappedData) {
        try {
            // Extract the DTO list
            @SuppressWarnings("unchecked")
            List<OffenceNoticeDto> dtoList = (List<OffenceNoticeDto>) mappedData.get("dtoList");

            // Step 3: Validate mandatory fields
            log.info("Step 3: Validating mandatory fields");
            List<OffenceNoticeDto> validDtos = createNoticeHelper.validateMandatoryFieldsForBatch(dtoList);

            // Identify failed DTOs (those that didn't pass validation)
            Map<String, Object> failedValidationNotices = new HashMap<>();
            for (OffenceNoticeDto dto : dtoList) {
                if (!validDtos.contains(dto)) {
                    String noticeIdentifier = dto.getNoticeNo() != null ? dto.getNoticeNo() :
                                           (dto.getVehicleNo() != null ? dto.getVehicleNo() : "Unknown");
                    failedValidationNotices.put(noticeIdentifier, "Missing Mandatory Fields");
                }
            }

            if (validDtos.isEmpty()) {
                // Check if we have multiple notices with missing fields
                if (dtoList.size() > 1) {
                    log.info("All {} notices have missing mandatory fields", dtoList.size());

                    // Use the helper to prepare the proper response format
                    NoticeResponse response = createNoticeHelper.prepareSuccessAndFailureResponse(
                        new ArrayList<>(), // empty successful notices
                        failedValidationNotices,     // all notices failed
                        new ArrayList<>()  // no duplicate notices
                    );

                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    // Single notice with missing fields - use single notice failure format
                    NoticeResponse errorResponse = new NoticeResponse();
                    errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Missing Mandatory Fields");
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
            }

            // Step 4: Prepare mapped data
            log.info("Step 4: Preparing mapped data for createNotice");
            Map<String, Object> finalMappedData = new HashMap<>();
            finalMappedData.put("dtoList", validDtos);
            // Pass failed validation notices to createNotice so it knows about them
            finalMappedData.put("failedValidationNotices", failedValidationNotices);
            // add other mapped data if needed

            // Step 5: Call createNotice
            log.info("Step 5: Calling createNotice with mapped data");
            return createNotice(finalMappedData);
            
        } catch (Exception e) {
            log.error("Error in processing notice with DTO mapping: {}", e.getMessage(), e);
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.INTERNAL_SERVER_ERROR, "Processing Error: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Common method for processing notice requests that come in standard DTO format
     */
     public ResponseEntity<NoticeResponse> processNoticeWithDtoMapping(String inputString) {
        try {
            // Flow for processing notice with DTO mapping
            // Step 1: Parse input string to extract elements
            // Step 2: Map input data to OffenceNoticeDTO
            // Step 3: Validate mandatory fields
            // Step 4: Prepare mapped data for createNotice
            // Step 5: Call createNotice with mapped data

            // Step 1: Parse input string
            log.info("Step 1: Parsing input string");
            if (inputString == null || inputString.trim().isEmpty()) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Empty or null input");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Step 2: Map to DTO
            log.info("Step 2: Mapping input data to OffenceNoticeDTO");
            Map<String, Object> mappedData = mapJsonToDtos(inputString);

            // Check for errors in mapping
            if (mappedData.containsKey("error")) {
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Invalid JSON format: " + mappedData.get("error"));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Extract the DTO list
            @SuppressWarnings("unchecked")
            List<OffenceNoticeDto> dtoList = (List<OffenceNoticeDto>) mappedData.get("dtoList");

            // Step 3: Validate mandatory fields
            log.info("Step 3: Validating mandatory fields");
            List<OffenceNoticeDto> validDtos = createNoticeHelper.validateMandatoryFieldsForBatch(dtoList);

            // Identify failed DTOs (those that didn't pass validation)
            Map<String, Object> failedValidationNotices = new HashMap<>();
            for (OffenceNoticeDto dto : dtoList) {
                if (!validDtos.contains(dto)) {
                    String noticeIdentifier = dto.getNoticeNo() != null ? dto.getNoticeNo() :
                                           (dto.getVehicleNo() != null ? dto.getVehicleNo() : "Unknown");
                    failedValidationNotices.put(noticeIdentifier, "Missing Mandatory Fields");
                }
            }

            if (validDtos.isEmpty()) {
                // Check if we have multiple notices with missing fields
                if (dtoList.size() > 1) {
                    log.info("All {} notices have missing mandatory fields", dtoList.size());

                    // Use the helper to prepare the proper response format
                    NoticeResponse response = createNoticeHelper.prepareSuccessAndFailureResponse(
                        new ArrayList<>(), // empty successful notices
                        failedValidationNotices,     // all notices failed
                        new ArrayList<>()  // no duplicate notices
                    );

                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    // Single notice with missing fields - use single notice failure format
                    NoticeResponse errorResponse = new NoticeResponse();
                    errorResponse.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, "Missing Mandatory Fields");
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
            }

            // Step 4: Prepare mapped data
            log.info("Step 4: Preparing mapped data for createNotice");
            Map<String, Object> finalMappedData = new HashMap<>();
            finalMappedData.put("dtoList", validDtos);
            // Pass failed validation notices to createNotice so it knows about them
            finalMappedData.put("failedValidationNotices", failedValidationNotices);
            // add other mapped data if needed

            // Step 5: Call createNotice
            log.info("Step 5: Calling createNotice with mapped data");

            return createNotice(finalMappedData);
            
        } catch (Exception e) {
            log.error("Error in processing notice with DTO mapping: {}", e.getMessage(), e);
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.createSingleNoticeFailureResponse(ErrorCodes.INTERNAL_SERVER_ERROR, "Processing Error: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Map JSON directly to DTOs (for standard format inputs)
     */
    private Map<String, Object> mapJsonToDtos(String rawData) {
        Map<String, Object> mappedData = new HashMap<>();
        
        try {
            List<OffenceNoticeDto> dtoList;
            
            // Check if the input is an array or single object
            if (rawData.trim().startsWith("[")) {
                // It's an array
                dtoList = objectMapper.readValue(rawData, new TypeReference<List<OffenceNoticeDto>>() {});
            } else {
                // It's a single object
                OffenceNoticeDto singleDto = objectMapper.readValue(rawData, OffenceNoticeDto.class);
                dtoList = new ArrayList<>();
                dtoList.add(singleDto);
            }
            
            log.info("Successfully mapped {} DTOs from JSON", dtoList.size());
            
            // Log some details about the mapped DTOs
            for (int i = 0; i < dtoList.size(); i++) {
                OffenceNoticeDto dto = dtoList.get(i);
                log.info("DTO #{} - NoticeNo: {}, VehicleNo: {}, ComputerRuleCode: {}", 
                        i + 1, dto.getNoticeNo(), dto.getVehicleNo(), dto.getComputerRuleCode());
            }
            
            mappedData.put("dtoList", dtoList);
            
            // If there's just one DTO, also put it directly for convenience
            if (dtoList.size() == 1) {
                mappedData.put("dto", dtoList.get(0));
            }
            
        } catch (Exception e) {
            log.error("Error mapping JSON to DTOs: {}", e.getMessage(), e);
            mappedData.put("error", e.getMessage());
        }
        
        return mappedData;
    }

    
    /**
     * Create a notice with the provided mapped data.
     */
    public ResponseEntity<NoticeResponse> createNotice(Map<String, Object> mappedData) {
        try {
            log.info("================================================");
            log.info("Notice creation process started");
            log.info("================================================");
            
            // Flow for notice creation
            // Note: Validation (mandatory fields and data format) already done in processNoticeWithDtoMapping
            // Step 1: Extract validated DTO list
            // Step 2: Process each DTO individually by calling processSingleNotice
            //         - Continue processing all notices even if some fail
            //         - Track both successful and failed notices
            // Step 3: Collect processed notice numbers and failed notices
            // Step 4: Prepare and send response with both success and failure information

            // Step 1: Extract validated DTO list (already validated in processNoticeWithDtoMapping)
            log.info("Step 1: Extracting validated DTO list");
            List<OffenceNoticeDto> dtoList = createNoticeHelper.extractDtoList(mappedData);
            if (dtoList == null || dtoList.isEmpty()) {
                log.warn("No valid DTOs found in the mapped data");
                NoticeResponse errorResponse = new NoticeResponse();
                errorResponse.setHTTPStatusCode("400");
                errorResponse.setHTTPStatusDescription("No Valid Data Found");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            log.info("Found {} DTOs to process", dtoList.size());
            
            // Step 2: Process each DTO individually using processSingleNotice
            log.info("Step 2: Processing DTOs individually");
            List<String> processedNoticeNumbers = new ArrayList<>();
            List<String> duplicateNotices = new ArrayList<>();
            Map<String, Object> failedNotices = new HashMap<>();

            // Add failed validation notices to the failedNotices map
            @SuppressWarnings("unchecked")
            Map<String, Object> failedValidationNotices = (Map<String, Object>) mappedData.get("failedValidationNotices");
            if (failedValidationNotices != null && !failedValidationNotices.isEmpty()) {
                failedNotices.putAll(failedValidationNotices);
                log.info("Added {} failed validation notices to failedNotices", failedValidationNotices.size());
            }

            int processingCount = 0;
            for (OffenceNoticeDto dto : dtoList) {
                processingCount++;
                log.info("Processing DTO {}/{} with Notice No: {}", processingCount, dtoList.size(), dto.getNoticeNo());
                Map<String, Object> singleDtoMap = createNoticeHelper.prepareSingleDtoMap(mappedData, dto);
                
                try {
                    // Process single notice through the standard workflow
                    String processedNoticeNumber = processSingleNotice(singleDtoMap);
                    if (processedNoticeNumber != null) {
                        // Check if this was a duplicate notice
                        if (Boolean.TRUE.equals(singleDtoMap.get("isDuplicate"))) {
                            String noticeWithInfo = (String) singleDtoMap.get("noticeWithInfo");
                            duplicateNotices.add(noticeWithInfo);
                            log.info("Added duplicate notice information: {}", noticeWithInfo);
                            
                            // Add duplicate to failedNotices with appropriate message
                            String vehicleNo = dto.getVehicleNo();
                            String offenceDate = dto.getNoticeDateAndTime() != null ? 
                                dto.getNoticeDateAndTime().toString() : "unknown date";
                            String errorMsg = "Duplicate notice detected for vehicle: " + vehicleNo + 
                                ", date: " + offenceDate;
                            
                            // Use notice number if available, otherwise use vehicle number as key
                            String noticeKey = dto.getNoticeNo() != null ? dto.getNoticeNo() : vehicleNo;
                            failedNotices.put(noticeKey, errorMsg);
                            log.info("Added duplicate notice to failedNotices: {}", errorMsg);
                        } else {
                            // Only add to processedNoticeNumbers if it's not a duplicate
                            processedNoticeNumbers.add(processedNoticeNumber);
                            log.info("Successfully processed notice: {}", processedNoticeNumber);
                        }
                    }
                } catch (Exception e) {
                    // Log the error without stack trace and add to failed notices
                    String noticeNo = dto.getNoticeNo();
                    log.error("Failed to process notice {}: {}", noticeNo, e.getMessage());
                    failedNotices.put(noticeNo, e.getMessage());
                }
            }
            
            // Step 3: Collect processed notice numbers and failed notices
            // (Already collected during processing)
            log.info("Step 3: Collection complete - Success: {}, Failed: {}, Duplicates: {}", 
                    processedNoticeNumbers.size(), failedNotices.size(), duplicateNotices.size());
            
            // Step 4: Prepare and send response with both success and failure information
            log.info("Step 4: Preparing response");
            NoticeResponse response = createNoticeHelper.prepareSuccessAndFailureResponse(
                    processedNoticeNumbers, failedNotices, duplicateNotices);
            
            log.info("================================================");
            log.info("Notice creation process completed. Successful: {}, Failed: {}", 
                    processedNoticeNumbers.size(), failedNotices.size());
            log.info("================================================");
            
            // Send consolidated error email if any errors were collected
            batchErrorCollector.sendConsolidatedErrorEmail();
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error in creating notice: {}", e.getMessage(), e);
            NoticeResponse errorResponse = new NoticeResponse();
            errorResponse.setHTTPStatusCode("500");
            errorResponse.setHTTPStatusDescription("Notice Creation Error: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Process a single notice through the full workflow
     * Updated to match flowchart exactly with all decision points
     */
    private String processSingleNotice(Map<String, Object> singleDtoMap) throws Exception {
        // Flow for processing a single notice
        // STEP 1: Check for duplicate notice numbers or offense details
        // STEP 2: Get offense type from offense_rule_code table
        // STEP 3: Check for duplicate offense details (PS-DBB)
        // STEP 4: Check vehicle registration type
        // STEP 5: Process based on vehicle registration type
        // STEP 6: For type F vehicles - check Foreign/VIP/CAS and detect PS-FOR
        // STEP 7: For V/S/X/D types with non-U offense - identify subsystem label
        // STEP 8: For V/S/X/D types with non-U offense - check ANS eligibility based on subsystem
        // STEP 9: For V/S/X/D types with non-U offense - check if eligible for PS-ANS
        // STEP 10: For V/S/X/D types with non-U offense - detect PS-ANS if eligible
        // STEP 11: Process specific vehicle types and set processing stages
        // STEP 12: Generate/extract notice number
        // STEP 13: Create notice records in database
        // STEP 14: Check if any PS detected (FOR/ANS/DBB)
        // STEP 15: Insert into suspended_notice if PS detected
        // STEP 16: Populate address for military (if type I)
        // STEP 17: Prepare to return the notice number
        // STEP 18: Process uploaded file if present in payload and Insert record into ocmsizmgr.ocms_offence_attachment table

        OffenceNoticeDto dto = (OffenceNoticeDto) singleDtoMap.get("dto");
        String noticeNumber = dto.getNoticeNo();
        
        try {
            // Standardize empty vehicle numbers to UNLICENSED_PARKING for consistent duplicate checking
            String vehicleNo = dto.getVehicleNo();
            if (vehicleNo == null || vehicleNo.isEmpty() || vehicleNo.trim().isEmpty()) {
                log.info("Empty vehicle number detected, standardizing to '{}' for consistent duplicate checking",
                    SystemConstant.VehicleNumber.UNLICENSED_PARKING);
                vehicleNo = SystemConstant.VehicleNumber.UNLICENSED_PARKING;
                dto.setVehicleNo(SystemConstant.VehicleNumber.UNLICENSED_PARKING);
            }
            // Step 1: Check for duplicate notice numbers or offense details
            log.info("Step 1: Checking for duplicates (notice number: {}, vehicle: {}, date: {})", 
                    dto.getNoticeNo(), dto.getVehicleNo(), dto.getNoticeDateAndTime());
            /* USER ASK TO TAKE OUT THIS PROCESS FOR NOW
            if (createNoticeHelper.checkDuplicateNoticeNumber(singleDtoMap, ocmsValidOffenceNoticeService)) {
                // Determine if it's a duplicate notice number or duplicate offense details
                String noticeNo = dto.getNoticeNo();
                String errorMessage;
                String errorType = "DUPLICATE_NOTICE";
                
                if (noticeNo != null && !noticeNo.trim().isEmpty()) {
                    errorMessage = "Duplicate notice number detected: " + noticeNo;
                    log.error("Duplicate notice number detected for: {}. Skipping this notice.", noticeNo);
                } else {
                    errorMessage = "Duplicate notice detected for vehicle: " + dto.getVehicleNo() + ", date: " + dto.getNoticeDateAndTime();
                    log.error("Duplicate notice detected for vehicle: {}, date: {}, rule code: {}", 
                            dto.getVehicleNo(), dto.getNoticeDateAndTime(), dto.getComputerRuleCode());
                }
                
                // Add to batch error collector - it will handle EHTSFTP filtering internally
                batchErrorCollector.addError(dto, errorType, errorMessage, 409); // 409 Conflict
                
                // Instead of throwing an exception, handle as a duplicate notice
                // Add duplicate information to the data map for response
                singleDtoMap.put("isDuplicate", true);
                String duplicateInfo = "Vehicle: " + dto.getVehicleNo() + ", Date: " + dto.getNoticeDateAndTime() + ", Rule Code: " + dto.getComputerRuleCode();
                singleDtoMap.put("duplicateFields", duplicateInfo);
                
                // Create a special format for the notice number to indicate it's a duplicate
                String noticeWithInfo = (noticeNo != null && !noticeNo.trim().isEmpty() ? noticeNo : dto.getVehicleNo()) + 
                    " (DUPLICATE: " + duplicateInfo + ")";
                singleDtoMap.put("noticeWithInfo", noticeWithInfo);
                
                log.info("Processed as duplicate notice");
                return noticeNo != null && !noticeNo.trim().isEmpty() ? noticeNo : dto.getVehicleNo();
            }
            */
            // Step 2: Get offense type from offense_rule_code table
            log.info("Step 2: Getting offense type from rule code");
            String offenseType;
            try {
                offenseType = createNoticeHelper.getOffenseTypeFromRuleCode(singleDtoMap);
                singleDtoMap.put("offenseType", offenseType);
            } catch (Exception e) {
                String errorMessage = "Failed to get offense type for notice: " + (noticeNumber != null ? noticeNumber : "N/A") + ". Error: " + e.getMessage();
                log.error(errorMessage);
                
                // Add to batch error collector
                batchErrorCollector.addError(dto, "RULE_CODE_ERROR", errorMessage, 400); // 400 Bad Request
                
                throw e;
            }
            
            /* USER ASK TO TAKE OUT THIS PROCESS FOR NOW
            // Step 3: Check for duplicate offense details (PS-DBB)
            log.info("Step 3: Checking for duplicate offense details");
            if (createNoticeHelper.checkDuplicateOffenseDetails(singleDtoMap, ocmsValidOffenceNoticeService)) {
                log.info("Duplicate offense detected, processing as duplicate notice");
                
                // Process duplicate notice - create notice and suspended notice records
                String createdNoticeNumber = createNoticeHelper.processDuplicateNotice(
                    singleDtoMap, 
                    ocmsValidOffenceNoticeService, 
                    ocmsSuspendedNoticeService, 
                    ocmsSuspensionReasonService
                );
                
                // Add duplicate information to the data map for response
                singleDtoMap.put("isDuplicate", true);
                String duplicateInfo = "Vehicle: " + dto.getVehicleNo() + ", Date: " + dto.getNoticeDateAndTime() + ", Rule Code: " + dto.getComputerRuleCode();
                singleDtoMap.put("duplicateFields", duplicateInfo);
                
                // Create a special format for the notice number to indicate it's a duplicate
                String noticeWithInfo = createdNoticeNumber + " (DUPLICATE: " + duplicateInfo + ")";
                singleDtoMap.put("noticeWithInfo", noticeWithInfo);
                
                log.info("Successfully processed duplicate notice: {}", createdNoticeNumber);
                return createdNoticeNumber;
            }
             */
            
            // Step 4: Check vehicle registration type
            log.info("Step 4: Checking vehicle registration type");
            String vehicleRegType;
            
            // Check if this is a duplicate notice
            Boolean isDuplicate = (Boolean) singleDtoMap.get("isDuplicate");
            
            // Vehicle number has already been standardized to UNLICENSED_PARKING if it was empty
            // Now set vehicle registration type based on duplicate status
            if (SystemConstant.VehicleNumber.UNLICENSED_PARKING.equals(dto.getVehicleNo()) &&
                (isDuplicate == null || !isDuplicate)) {
                // For non-duplicate notices with empty vehicle numbers, always set type to X (UPL)
                vehicleRegType = "X";
                log.info("Setting vehicle registration type to X for {} vehicle number in non-duplicate notice",
                    SystemConstant.VehicleNumber.UNLICENSED_PARKING);
            } else if (isDuplicate != null && isDuplicate) {
                // For duplicate notices, don't set vehicle registration type
                vehicleRegType = null;
                log.info("Not setting vehicle registration type for duplicate notice");
            } else {
                // For all other cases (non-empty vehicle numbers in non-duplicate notices), use SpecialVehUtils
                String vehNo = dto.getVehicleNo();
                
                // Always check for MID vehicles first regardless of user-provided type
                if (vehNo != null && (vehNo.contains("MID") || vehNo.contains("MINDEF"))) {
                    vehicleRegType = "I"; // Force type I for MID vehicles
                    log.info("MID/MINDEF detected in vehicle number, forcing vehicle registration type to I");
                } else {
                    // For all other vehicles, use standard detection
                    // Pass null for sourceProvidedType as DTO doesn't currently have this field
                    vehicleRegType = specialVehUtils.checkVehregistration(vehNo, null);
                    log.info("Vehicle registration type determined: {}", vehicleRegType);
                }
            }
            
            log.info("Step 4: Vehicle registration type identified as: {}", vehicleRegType);
            singleDtoMap.put("vehicleRegType", vehicleRegType);

            // Update the DTO with the determined vehicle registration type
            dto.setVehicleRegistrationType(vehicleRegType);

            // ============================================================================
            // STEP 4: EHT/CERTIS AN CHECK (OLD LOGIC - BEFORE Notice Creation)
            // ============================================================================
            log.info("Step 4: Checking EHT/Certis Advisory Notice eligibility");

            // Identify subsystem label first (needed for EHT check)
            String subsystemLabel = dto.getSubsystemLabel();
            if (subsystemLabel == null || subsystemLabel.isEmpty()) {
                throw new IllegalArgumentException("Subsystem label is required");
            }
            log.info("Step 4: Subsystem label identified as: {}", subsystemLabel);
            singleDtoMap.put("subsystemLabel", subsystemLabel);

            // Check if this is EHT/Certis subsystem (030-999)
            boolean isEhtCertis = false;
            if (subsystemLabel != null && subsystemLabel.length() >= 3) {
                String firstThreeDigits = subsystemLabel.substring(0, 3);
                if (firstThreeDigits.matches("\\d{3}")) {
                    int value = Integer.parseInt(firstThreeDigits);
                    if (value >= 30 && value <= 999) {
                        isEhtCertis = true;
                        log.info("EHT/Certis subsystem detected: {}", value);
                    }
                }
            }

            // If EHT/Certis, check an_flag from payload
            if (isEhtCertis) {
                log.info("Checking AN flag for EHT/Certis subsystem");
                boolean eligibleForPSANS = createNoticeHelper.checkANSForEHT(singleDtoMap);

                if (eligibleForPSANS) {
                    log.info("EHT/Certis AN detected (an_flag=Y), creating notice with PS-ANS");

                    // Generate notice number
                    noticeNumber = createNoticeHelper.processNoticeNumber(singleDtoMap);
                    log.info("Generated notice number: {}", noticeNumber);

                    // Create notice records
                    log.info("Creating notice records in database for EHT AN");
                    createNoticeHelper.createNoticeRecord(singleDtoMap, ocmsValidOffenceNoticeService, offenceNoticeDetailService);

                    // Trigger PS-ANS suspension
                    String ansReason = "Advisory Notice detected - subsystem: " + subsystemLabel;
                    autoSuspensionHelper.triggerAdvisoryNotice(noticeNumber, ansReason);
                    log.info("PS-ANS triggered for EHT AN notice {}", noticeNumber);

                    // Process uploaded file
                    String userId = dto.getCreUserId();
                    boolean fileProcessed = fileProcessingHelper.processUploadedFile(noticeNumber, singleDtoMap, userId);
                    if (!fileProcessed) {
                        log.warn("File processing failed for EHT AN notice {}, but notice creation succeeded", noticeNumber);
                    }

                    log.info("Successfully processed EHT AN notice: {}", noticeNumber);
                    // Early return for EHT AN
                    return noticeNumber;
                }
                log.info("EHT/Certis subsystem but an_flag=N, continuing to Step 5");
            } else {
                log.info("Non-EHT subsystem, continuing to Step 5");
            }

            // ============================================================================
            // STEP 5: CREATE NOTICE (For Non-EHT OR EHT with an_flag='N')
            // ============================================================================
            log.info("Step 5: Creating notice (restructured to handle all vehicle types)");

            // Sub-step 5a: Set vehicle type-specific settings BEFORE creation
            log.info("Step 5a: Setting vehicle type-specific processing stages");

            if ("F".equals(vehicleRegType)) {
                log.info("Type F: Processing foreign vehicle settings");
                // Set DTO fields only - DON'T create notice or suspension yet
                createNoticeHelper.setForeignVehicleSettings(singleDtoMap);

            } else if ("I".equals(vehicleRegType)) {
                log.info("Type I: Processing military vehicle settings");
                createNoticeHelper.processMilitaryVehicle(singleDtoMap);

            } else if ("X".equals(vehicleRegType)) {
                log.info("Type X: Processing UPL settings");
                createNoticeHelper.setProcessingStagesForX(singleDtoMap);

            } else if (vehicleRegType != null && vehicleRegType.matches("[VSD]")) {
                log.info("Type V/S/D: Processing VSD settings");
                createNoticeHelper.setProcessingStagesForVSD(singleDtoMap);
            }

            // Sub-step 5b: Subsystem label (already identified in Step 4)
            log.info("Step 5b: Subsystem label: {}", subsystemLabel);

            // Sub-step 5c: Generate notice number
            log.info("Step 5c: Generating notice number");
            noticeNumber = createNoticeHelper.processNoticeNumber(singleDtoMap);
            log.info("Generated notice number: {}", noticeNumber);

            // Sub-step 5d: CREATE NOTICE RECORDS
            log.info("Step 5d: Creating notice records in database");
            createNoticeHelper.createNoticeRecord(singleDtoMap, ocmsValidOffenceNoticeService, offenceNoticeDetailService);

            // Sub-step 5e: Create PS-FOR suspension if Type F
            if ("F".equals(vehicleRegType)) {
                log.info("Step 5e: Creating PS-FOR suspension for foreign vehicle");
                createNoticeHelper.createForeignVehicleSuspension(
                    noticeNumber,
                    singleDtoMap,
                    ocmsSuspendedNoticeService,
                    ocmsSuspensionReasonService
                );
            }

            // Sub-step 5f: Create TS-OLD suspension if Type V (VIP)
            if ("V".equals(vehicleRegType)) {
                log.info("Step 5f: Creating TS-OLD suspension for VIP vehicle");
                createNoticeHelper.createVipVehicleSuspension(
                    noticeNumber,
                    singleDtoMap,
                    ocmsSuspendedNoticeService,
                    ocmsSuspensionReasonService
                );
            }

            log.info("Step 5: Notice creation complete");
            // ⚠️ IMPORTANT: NO EARLY RETURN - continue to next steps

            // ============================================================================
            // STEP 6: CHECK FOR DOUBLE BOOKING (DBB Check - AFTER Notice Creation)
            // ============================================================================
            log.info("Step 6: Checking for double booking (duplicate offense details)");

            boolean isDuplicateOffense = createNoticeHelper.checkDuplicateOffenseDetails(singleDtoMap, ocmsValidOffenceNoticeService);

            // OCMS 21: Check if DBB query failed (all retries exhausted)
            Boolean dbbQueryFailed = (Boolean) singleDtoMap.get("dbbQueryFailed");
            if (Boolean.TRUE.equals(dbbQueryFailed)) {
                String queryException = (String) singleDtoMap.get("dbbQueryException");
                log.error("DBB query failed for notice {}, applying TS-OLD fallback. Error: {}", noticeNumber, queryException);

                // Apply TS-OLD suspension as fallback
                try {
                    java.time.LocalDateTime currentDate = java.time.LocalDateTime.now();

                    // OCMS 21: Get suspension duration and calculate due date for TS-OLD
                    int suspensionDurationDays = getSuspensionDurationDays("OLD", 30); // Default 30 days
                    java.time.LocalDateTime dueDateOfRevival = currentDate.plusDays(suspensionDurationDays);
                    log.info("Calculated due_date_of_revival for TS-OLD: {} (current date + {} days)", dueDateOfRevival, suspensionDurationDays);

                    // Update the notice itself with TS-OLD suspension fields
                    log.info("Updating notice {} with TS-OLD suspension fields (DBB query failure fallback)", noticeNumber);
                    java.util.Optional<com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice> noticeOpt =
                        ocmsValidOffenceNoticeService.getById(noticeNumber);

                    if (noticeOpt.isPresent()) {
                        com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice notice = noticeOpt.get();
                        notice.setSuspensionType("TS");
                        notice.setEprReasonOfSuspension("OLD");
                        notice.setEprDateOfSuspension(currentDate);
                        notice.setDueDateOfRevival(dueDateOfRevival);
                        ocmsValidOffenceNoticeService.save(notice);
                        log.info("Updated notice {} with suspension_type=TS, epr_reason_of_suspension=OLD, due_date_of_revival={}", noticeNumber, dueDateOfRevival);
                    } else {
                        log.error("Could not find notice {} to update with TS-OLD fields", noticeNumber);
                    }

                    // Update Internet DB with same TS-OLD suspension fields
                    log.info("Updating Internet DB (eocms_valid_offence_notice) for notice {}", noticeNumber);
                    try {
                        java.util.Optional<com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice> eocmsNoticeOpt =
                            eocmsValidOffenceNoticeService.getById(noticeNumber);

                        if (eocmsNoticeOpt.isPresent()) {
                            com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice eocmsNotice = eocmsNoticeOpt.get();
                            eocmsNotice.setSuspensionType("TS");
                            eocmsNotice.setEprReasonOfSuspension("OLD");
                            eocmsNotice.setEprDateOfSuspension(currentDate);
                            eocmsValidOffenceNoticeService.save(eocmsNotice);
                            log.info("Updated Internet DB notice {} with TS-OLD suspension fields", noticeNumber);
                        } else {
                            log.warn("Notice {} not found in Internet DB (eocms_valid_offence_notice), skipping Internet DB update", noticeNumber);
                        }
                    } catch (Exception e) {
                        log.error("Error updating Internet DB for notice {}: {}", noticeNumber, e.getMessage());
                        // Don't fail the entire process if Internet DB update fails
                    }

                    // Create suspended notice record with TS-OLD
                    com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice suspendedNotice =
                        new com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice();

                    // Get next SR number
                    Integer maxSrNo = ocmsSuspendedNoticeService.getMaxSrNoForNotice(noticeNumber);
                    suspendedNotice.setSrNo(maxSrNo != null ? maxSrNo + 1 : 1);

                    suspendedNotice.setNoticeNo(noticeNumber);
                    suspendedNotice.setDateOfSuspension(currentDate);
                    suspendedNotice.setSuspensionSource("ocmsiz_app_conn");
                    suspendedNotice.setSuspensionType("TS");
                    suspendedNotice.setReasonOfSuspension("OLD");
                    suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");
                    suspendedNotice.setDateOfRevival(null);
                    suspendedNotice.setDueDateOfRevival(dueDateOfRevival);
                    suspendedNotice.setSuspensionRemarks("TS-OLD: DBB query failed after 3 retries - " + queryException);

                    ocmsSuspendedNoticeService.save(suspendedNotice);

                    log.info("TS-OLD suspension created successfully for notice {} (DBB query failure fallback)", noticeNumber);

                    // Process uploaded file before returning
                    String userId = dto.getCreUserId();
                    boolean fileProcessed = fileProcessingHelper.processUploadedFile(noticeNumber, singleDtoMap, userId);
                    if (!fileProcessed) {
                        log.warn("File processing failed for TS-OLD notice {}, but notice creation succeeded", noticeNumber);
                    }

                    // Early return for TS-OLD fallback - skip Step 7 and 8
                    log.info("Successfully processed TS-OLD fallback notice: {}", noticeNumber);
                    return noticeNumber;

                } catch (Exception e) {
                    log.error("Error creating TS-OLD suspension for notice {}: {}", noticeNumber, e.getMessage(), e);
                    // Continue to Step 7 even if TS-OLD creation fails
                }
            }

            if (isDuplicateOffense) {
                log.info("Duplicate offense detected for notice {}, creating PS-DBB suspension", noticeNumber);

                // Create PS-DBB suspension for the already-created notice
                try {
                    java.time.LocalDateTime currentDate = java.time.LocalDateTime.now();

                    // OCMS 21: Get suspension duration and calculate due date
                    int suspensionDurationDays = getSuspensionDurationDays("DBB", 30); // Default 30 days
                    java.time.LocalDateTime dueDateOfRevival = currentDate.plusDays(suspensionDurationDays);
                    log.info("Calculated due_date_of_revival for PS-DBB: {} (current date + {} days)", dueDateOfRevival, suspensionDurationDays);

                    // OCMS 21: Update the notice itself with PS-DBB suspension fields
                    log.info("Updating notice {} with PS-DBB suspension fields", noticeNumber);
                    java.util.Optional<com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice> noticeOpt =
                        ocmsValidOffenceNoticeService.getById(noticeNumber);

                    if (noticeOpt.isPresent()) {
                        com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice notice = noticeOpt.get();
                        notice.setSuspensionType("PS");
                        notice.setEprReasonOfSuspension("DBB");
                        notice.setEprDateOfSuspension(currentDate);
                        notice.setDueDateOfRevival(dueDateOfRevival);
                        ocmsValidOffenceNoticeService.save(notice);
                        log.info("Updated notice {} with suspension_type=PS, epr_reason_of_suspension=DBB, due_date_of_revival={}", noticeNumber, dueDateOfRevival);
                    } else {
                        log.error("Could not find notice {} to update with PS-DBB fields", noticeNumber);
                    }

                    // OCMS 21: Update Internet DB with same PS-DBB suspension fields
                    log.info("Updating Internet DB (eocms_valid_offence_notice) for notice {}", noticeNumber);
                    try {
                        java.util.Optional<com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice> eocmsNoticeOpt =
                            eocmsValidOffenceNoticeService.getById(noticeNumber);

                        if (eocmsNoticeOpt.isPresent()) {
                            com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice eocmsNotice = eocmsNoticeOpt.get();
                            eocmsNotice.setSuspensionType("PS");
                            eocmsNotice.setEprReasonOfSuspension("DBB");
                            eocmsNotice.setEprDateOfSuspension(currentDate);
                            eocmsValidOffenceNoticeService.save(eocmsNotice);
                            log.info("Updated Internet DB notice {} with PS-DBB suspension fields", noticeNumber);
                        } else {
                            log.warn("Notice {} not found in Internet DB (eocms_valid_offence_notice), skipping Internet DB update", noticeNumber);
                        }
                    } catch (Exception e) {
                        log.error("Error updating Internet DB for notice {}: {}", noticeNumber, e.getMessage());
                        // Don't fail the entire process if Internet DB update fails
                    }

                    // Create suspended notice record
                    com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice suspendedNotice =
                        new com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice();

                    // Get next SR number
                    Integer maxSrNo = ocmsSuspendedNoticeService.getMaxSrNoForNotice(noticeNumber);
                    suspendedNotice.setSrNo(maxSrNo != null ? maxSrNo + 1 : 1);

                    suspendedNotice.setNoticeNo(noticeNumber);
                    suspendedNotice.setDateOfSuspension(currentDate);
                    suspendedNotice.setSuspensionSource("ocmsiz_app_conn");
                    suspendedNotice.setSuspensionType("PS");
                    suspendedNotice.setReasonOfSuspension("DBB");
                    suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");
                    suspendedNotice.setDateOfRevival(null);
                    suspendedNotice.setDueDateOfRevival(dueDateOfRevival);

                    // Get duplicate notice number and reason from data map (set by checkDuplicateOffenseDetails)
                    String duplicateNoticeNo = (String) singleDtoMap.get("duplicateNoticeNo");
                    String duplicateReason = (String) singleDtoMap.get("duplicateReason");
                    suspendedNotice.setSuspensionRemarks("DBB: " +
                        (duplicateReason != null ? duplicateReason : "Duplicate booking detected") +
                        " - Original notice: " + (duplicateNoticeNo != null ? duplicateNoticeNo : "Unknown"));

                    ocmsSuspendedNoticeService.save(suspendedNotice);

                    log.info("PS-DBB suspension created successfully for notice {}", noticeNumber);

                    // Process uploaded file before returning
                    String userId = dto.getCreUserId();
                    boolean fileProcessed = fileProcessingHelper.processUploadedFile(noticeNumber, singleDtoMap, userId);
                    if (!fileProcessed) {
                        log.warn("File processing failed for DBB notice {}, but notice creation succeeded", noticeNumber);
                    }

                    // Early return for DBB - skip Step 7 and 8
                    log.info("Successfully processed DBB notice: {}", noticeNumber);
                    return noticeNumber;

                } catch (Exception e) {
                    log.error("Error creating PS-DBB suspension for notice {}: {}", noticeNumber, e.getMessage(), e);
                    // Don't fail the entire notice creation, just log the error
                    // Continue to Step 7
                }
            }

            log.info("Step 6: No duplicate offense detected, continuing to Step 7");

            // ============================================================================
            // STEP 7: OCMS AN QUALIFICATION (NEW LOGIC - AFTER Notice Creation)
            // ============================================================================
            log.info("Step 7: OCMS AN Qualification check");

            // Only check for offenseType='O' and vehicleType in [S,D,V,I]
            if ("O".equals(offenseType) && vehicleRegType != null && vehicleRegType.matches("[SDVI]")) {
                AdvisoryNoticeHelper.AdvisoryNoticeResult anResult = createNoticeHelper.checkAnQualification(singleDtoMap);

                if (anResult.isQualified()) {
                    log.info("Notice qualifies for OCMS AN: {}", anResult.getDetails());
                    createNoticeHelper.updateNoticeWithAnFlags(noticeNumber);
                    log.info("Updated notice {} with an_flag='Y' and payment_acceptance_allowed='N'", noticeNumber);
                } else {
                    log.info("Notice does not qualify for OCMS AN: {}", anResult.getReasonNotQualified());
                }
            } else {
                log.info("Skipping OCMS AN check - offenseType={}, vehicleType={}", offenseType, vehicleRegType);
            }

            log.info("Step 7: OCMS AN check complete");

            // ============================================================================
            // STEP 8: FINAL PROCESSING
            // ============================================================================
            log.info("Step 8: Final processing");

            // Populate address for military
            if ("I".equals(vehicleRegType)) {
                log.info("Populating address for military vehicle");
                createNoticeHelper.populateAddressForMilitary(singleDtoMap);
            }

            // Process uploaded file
            log.info("Processing uploaded file if present");
            String userId = dto.getCreUserId();
            boolean fileProcessed = fileProcessingHelper.processUploadedFile(noticeNumber, singleDtoMap, userId);
            if (!fileProcessed) {
                log.warn("File processing failed for notice {}, but notice creation will continue", noticeNumber);
            }

            log.info("Successfully processed notice: {}", noticeNumber);
            return noticeNumber;
            
        } catch (Exception e) {
            String errorMessage = "Error processing notice " + (noticeNumber != null ? noticeNumber : "N/A") + ": " + e.getMessage();
            log.error(errorMessage);
            
            // Add to batch error collector
            // Skip if it's already a known error type that was added earlier
            if (!(e.getMessage() != null && (
                e.getMessage().contains("Duplicate notice") || 
                e.getMessage().contains("Failed to get offense type")))) {
                
                batchErrorCollector.addError(dto, "PROCESSING_ERROR", errorMessage, 500);
            }
            
            throw e;
        }
    }
}