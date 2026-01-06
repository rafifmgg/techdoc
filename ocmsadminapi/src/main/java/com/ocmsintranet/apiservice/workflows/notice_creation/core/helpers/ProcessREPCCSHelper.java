package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto;
import com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit.WebTransactionAudit;
import com.ocmsintranet.apiservice.crud.ocmsezdb.WebTransactionAudit.WebTransactionAuditService;
import com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark.Icarpark;
import com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark.IcarparkRepository;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;

/**
 * Helper class for the processREPCCS method in CreateNoticeServiceImpl
 */
@Component
@Slf4j
public class ProcessREPCCSHelper {

    @Autowired
    private IcarparkRepository icarparkRepository;

    /**
     * Generates a transaction ID for audit tracking
     */
    public String generateTransactionId() {
        // Implementation for generating unique transaction ID
        return "TXN" + System.currentTimeMillis();
    }
    
    /**
     * Extracts the sender information from the raw data
     */
    public String extractSender(String rawData) {
        // Extract sender information from the raw data
        return "SND01";
    }
    
    /**
     * Extracts the receiver information from the raw data
     */
    public String extractReceiver(String rawData) {
        // Extract receiver information from the raw data
        return "RCV01";
    }
    
    /**
     * Creates the initial audit record for transaction tracking
     */
    public WebTransactionAudit createInitialAuditRecord(String txnId, String sender, String receiver, String rawData) {
        WebTransactionAudit audit = new WebTransactionAudit();
        audit.setWebTxnId(txnId);
        audit.setSender(sender);
        audit.setTargetReceiver(receiver);
        
        // Set current date and time
        LocalDateTime now = LocalDateTime.now();
        audit.setCreDate(now);
        audit.setSendDate(now);  
        audit.setSendTime(now.toLocalTime());  
        
        audit.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
        audit.setStatusNum(SystemConstant.Status.TRANSACTION_PROCESSING); // Processing
        audit.setTxnDetail(rawData);
        audit.setRecordCounter(1);
        return audit;
    }
    
    /**
     * Updates an audit record with error information
     */
    public void updateAuditWithError(WebTransactionAudit audit, String statusCode, String errorMessage, 
                                    WebTransactionAuditService webTransactionAuditService) {
        if (audit != null) {
            // Set the updated values
            audit.setStatusNum(SystemConstant.Status.TRANSACTION_ERROR);
            audit.setMsgError(errorMessage);
            audit.setUpdDate(LocalDateTime.now());
            
            // Use service's base implementation to save
            webTransactionAuditService.save(audit);
        }
    }
    
    /**
     * Updates an audit record with success information
     */
    public void updateAuditWithSuccess(WebTransactionAudit audit, String message,
                                      WebTransactionAuditService webTransactionAuditService) {
        if (audit != null) {
            // Set the updated values
            audit.setStatusNum(SystemConstant.Status.TRANSACTION_COMPLETED);
            audit.setMsgError(message);
            audit.setUpdDate(LocalDateTime.now());
            
            // Use service's base implementation to save
            webTransactionAuditService.save(audit);
        }
    }
    
    /**
     * Maps incoming JSON data to a standardized format
     */
    public Map<String, Object> mapIncomingData(String rawData) {
        Map<String, Object> mappedData = new HashMap<>();
        
        try {
            // Parse the JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // For handling date/time
            
            // Check if the input is an array
            JsonNode rootNode = objectMapper.readTree(rawData);
            
            // Log the incoming data structure
            log.info("Incoming data structure: {}", rootNode.getNodeType());
            
            List<OffenceNoticeDto> dtoList = new ArrayList<>();
            
            if (rootNode.isArray()) {
                // Process each item in the array
                log.info("Processing JSON array with {} elements", rootNode.size());
                
                for (JsonNode jsonNode : rootNode) {
                    OffenceNoticeDto dto = processJsonNode(jsonNode);
                    if (dto != null) {
                        dtoList.add(dto);
                        log.info("Added DTO with noticeNo: {}, vehicleNo: {}", dto.getNoticeNo(), dto.getVehicleNo());
                        
                        // Log the complete DTO object
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                        log.info("Complete DTO: {}", mapper.writeValueAsString(dto));
                    }
                }
            } else {
                // Process single object
                log.info("Processing single JSON object");
                OffenceNoticeDto dto = processJsonNode(rootNode);
                if (dto != null) {
                    dtoList.add(dto);
                    log.info("Added DTO with noticeNo: {}, vehicleNo: {}", dto.getNoticeNo(), dto.getVehicleNo());
                    
                    // Log the complete DTO object
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    log.info("Complete DTO: {}", mapper.writeValueAsString(dto));
                }
            }
            
            log.info("Processed {} DTOs from the input data", dtoList.size());
            
            // Log each DTO in the list with more details
            for (int i = 0; i < dtoList.size(); i++) {
                OffenceNoticeDto dto = dtoList.get(i);
                log.info("DTO #{} details:", i + 1);
                log.info("  - NoticeNo: {}", dto.getNoticeNo());
                log.info("  - VehicleNo: {}", dto.getVehicleNo());
                log.info("  - VehicleCategory: {}", dto.getVehicleCategory());
                log.info("  - ComputerRuleCode: {}", dto.getComputerRuleCode());
                log.info("  - CompositionAmount: {}", dto.getCompositionAmount());
                log.info("  - NoticeDateAndTime: {}", dto.getNoticeDateAndTime());
                log.info("  - TransactionId: {}", dto.getTransactionId());
                log.info("  - SubsystemLabel: {}", dto.getSubsystemLabel());
            }
            
            mappedData.put("dtoList", dtoList);
            
            // If there's just one DTO, also put it directly for convenience
            if (dtoList.size() == 1) {
                mappedData.put("dto", dtoList.get(0));
                log.info("Single DTO added directly to mappedData with key 'dto'");
            }
            
            log.info("Mapped data created successfully with {} entries", mappedData.size());
            
        } catch (Exception e) {
            log.error("Error mapping data: {}", e.getMessage(), e);
            mappedData.put("error", e.getMessage());
        }
        
        return mappedData;
    }

    /**
     * Process a single JSON node into an OffenceNoticeDto
     */
    private OffenceNoticeDto processJsonNode(JsonNode jsonNode) {
        try {
            OffenceNoticeDto dto = new OffenceNoticeDto();
            
            // Map fields from JSON to DTO
            if (jsonNode.has("CaptureDateTime")) {
                String captureDateTimeStr = jsonNode.get("CaptureDateTime").asText();
                LocalDateTime captureDateTime = parseDateTime(captureDateTimeStr);
                dto.setNoticeDateAndTime(captureDateTime);
                log.info("Mapped CaptureDateTime: {} to NoticeDateAndTime: {}", captureDateTimeStr, captureDateTime);
            }
            
            if (jsonNode.has("ChargeAmount")) {
                String chargeAmountStr = jsonNode.get("ChargeAmount").asText();
                try {
                    BigDecimal chargeAmount = new BigDecimal(chargeAmountStr);
                    dto.setCompositionAmount(chargeAmount);
                    log.info("Mapped ChargeAmount: {} to CompositionAmount: {}", chargeAmountStr, chargeAmount);
                } catch (NumberFormatException e) {
                    log.error("Error parsing ChargeAmount: {}", chargeAmountStr);
                }
            }
            
            if (jsonNode.has("VehicleRegistration")) {
                String regType = jsonNode.get("VehicleRegistration").asText();
                dto.setVehicleRegistrationType(regType);
                log.info("Mapped VehicleRegistration: {} to VehicleRegistrationType", regType);
            }
            
            if (jsonNode.has("VehicleCategory")) {
                String vehicleCategory = jsonNode.get("VehicleCategory").asText();
                // Apply transformation: if "HV" then use HEAVY_VEHICLE constant, otherwise, original value
                if ("HV".equals(vehicleCategory)) {
                    dto.setVehicleCategory(SystemConstant.VehicleCategory.HEAVY_VEHICLE);
                } else {
                    dto.setVehicleCategory(vehicleCategory);
                }
                log.info("Mapped VehicleCategory: {} to VehicleCategory: {}", 
                        vehicleCategory, dto.getVehicleCategory());
            }
            
            if (jsonNode.has("LicencePlate")) {
                String vehicleNo = jsonNode.get("LicencePlate").asText();
                dto.setVehicleNo(vehicleNo);
                log.info("Mapped LicencePlate: {} to VehicleNo", vehicleNo);
            }
            
            if (jsonNode.has("OffenceCode")) {
                String offenceCodeStr = jsonNode.get("OffenceCode").asText();
                // Instead of trying to parse as Integer directly, check if it's numeric
                if (offenceCodeStr.matches("\\d+")) {
                    try {
                        Integer offenceCode = Integer.parseInt(offenceCodeStr);
                        dto.setComputerRuleCode(offenceCode);
                        log.info("Mapped OffenceCode: {} to ComputerRuleCode: {}", 
                                 offenceCodeStr, offenceCode);
                    } catch (NumberFormatException e) {
                        log.error("Error parsing OffenceCode as Integer: {}", offenceCodeStr);
                        dto.setRuleNo(offenceCodeStr);
                    }
                } else {
                    // For non-numeric codes, store in ruleNo
                    dto.setRuleNo(offenceCodeStr);
                    log.info("Mapped non-numeric OffenceCode: {} to RuleNo", offenceCodeStr);
                }
            }
            
            if (jsonNode.has("NOPONumber")) {
                String noticeNo = jsonNode.get("NOPONumber").asText();
                dto.setNoticeNo(noticeNo);
                log.info("Mapped NOPONumber: {} to NoticeNo", noticeNo);
            }
            
            if (jsonNode.has("ANSFlag")) {
                String ansFlag = jsonNode.get("ANSFlag").asText();
                dto.setAnFlag(ansFlag);
                log.info("Mapped ANSFlag: {} to AnFlag", ansFlag);
            }
            
            // Handle EnforcementCarParkID mapping to ppCode and ppName lookup
            if (jsonNode.has("EnforcementCarParkID")) {
                String ppCode = jsonNode.get("EnforcementCarParkID").asText();
                dto.setPpCode(ppCode);
                log.info("Mapped EnforcementCarParkID: {} to ppCode", ppCode);
                
                // Look up ppName based on ppCode
                String ppName = lookupParkingPlaceName(ppCode);
                if (ppName != null && !ppName.isEmpty()) {
                    dto.setPpName(ppName);
                    log.info("Looked up ppName: {} for ppCode: {}", ppName, ppCode);
                } else {
                    log.warn("Could not find ppName for ppCode: {}", ppCode);
                    // Set a default value or handle the missing ppName as required
                    dto.setPpName("Unknown Parking Place " + ppCode);
                    log.info("Set default ppName for ppCode: {}", ppCode);
                }
            }
            
            if (jsonNode.has("TransactionID")) {
                String transactionId = jsonNode.get("TransactionID").asText();
                dto.setTransactionId(transactionId);
                log.info("Mapped TransactionID: {} to TransactionId", transactionId);
            }
            
            if (jsonNode.has("SubSystemLabel")) {
                String subsystemLabel = jsonNode.get("SubSystemLabel").asText();
                // dto.setSubsystemLabel(subsystemLabel);
                dto.setSubsystemLabel(SystemConstant.Subsystem.REPCCS_EHT_CODE);
                log.info("Mapped SubSystemLabel: {} to SubsystemLabel", subsystemLabel);
            }
            
            if (jsonNode.has("OperatorID")) {
                String operatorId = jsonNode.get("OperatorID").asText();
                dto.setRepOperatorId(operatorId);
                log.info("Mapped OperatorID: {} to RepOperatorId", operatorId);
            }
            
            if (jsonNode.has("CreatedDateTime")) {
                String createdDateTimeStr = jsonNode.get("CreatedDateTime").asText();
                LocalDateTime createdDateTime = parseDateTime(createdDateTimeStr);
                dto.setCreatedDt(createdDateTime);
                log.info("Mapped CreatedDateTime: {} to CreatedDt: {}", 
                         createdDateTimeStr, createdDateTime);
            }
            
            if (jsonNode.has("ParkingEntryDateTime")) {
                String parkingEntryDateTimeStr = jsonNode.get("ParkingEntryDateTime").asText();
                LocalDateTime parkingEntryDateTime = parseDateTime(parkingEntryDateTimeStr);
                dto.setRepParkingEntryDt(parkingEntryDateTime);
                log.info("Mapped ParkingEntryDateTime: {} to RepParkingEntryDt: {}", 
                         parkingEntryDateTimeStr, parkingEntryDateTime);
            }
            
            if (jsonNode.has("ParkingStartDateTime")) {
                String parkingStartDateTimeStr = jsonNode.get("ParkingStartDateTime").asText();
                LocalDateTime parkingStartDateTime = parseDateTime(parkingStartDateTimeStr);
                dto.setRepParkingStartDt(parkingStartDateTime);
                log.info("Mapped ParkingStartDateTime: {} to RepParkingStartDt: {}", 
                        parkingStartDateTimeStr, parkingStartDateTime);
            }
            
            if (jsonNode.has("ParkingEndDateTime")) {
                String parkingEndDateTimeStr = jsonNode.get("ParkingEndDateTime").asText();
                LocalDateTime parkingEndDateTime = parseDateTime(parkingEndDateTimeStr);
                dto.setRepParkingEndDt(parkingEndDateTime);
                log.info("Mapped ParkingEndDateTime: {} to RepParkingEndDt: {}", 
                         parkingEndDateTimeStr, parkingEndDateTime);
            }
            
            if (jsonNode.has("ViolationCode")) {
                String violationCode = jsonNode.get("ViolationCode").asText();
                dto.setRepViolationCode(violationCode);
                log.info("Mapped ViolationCode: {} to RepViolationCode", violationCode);
            }
            
            if (jsonNode.has("OBULabel")) {
                String iuNo = jsonNode.get("OBULabel").asText();
                dto.setIuNo(iuNo);
                log.info("Mapped OBULabel: {} to IuNo", iuNo);
            }
            
            if (jsonNode.has("OBULatitude")) {
                String latitude = jsonNode.get("OBULatitude").asText();
                dto.setRepObuLatitude(latitude);
                log.info("Mapped OBULatitude: {} to RepObuLatitude", latitude);
            }
            
            if (jsonNode.has("OBULongitude")) {
                String longitude = jsonNode.get("OBULongitude").asText();
                dto.setRepObuLongitude(longitude);
                log.info("Mapped OBULongitude: {} to RepObuLongitude", longitude);
            }
            
            if (jsonNode.has("ImageID")) {
                String imageId = jsonNode.get("ImageID").asText();
                dto.setImageId(imageId);
                log.info("Mapped ImageID: {} to ImageId", imageId);
            }
            
            if (jsonNode.has("VideoID")) {
                String videoId = jsonNode.get("VideoID").asText();
                dto.setVideoId(videoId);
                log.info("Mapped VideoID: {} to VideoId", videoId);
            }
                        
            if (jsonNode.has("ERP2CCSRemarks")) {
                String remarks = jsonNode.get("ERP2CCSRemarks").asText();
                dto.setRuleRemarks(remarks);
                log.info("Mapped ERP2CCSRemarks: {} to RuleRemarks", remarks);
            }
            
            if (jsonNode.has("REPCCSRemarks")) {
                String remarks = jsonNode.get("REPCCSRemarks").asText();
                dto.setUserNonPrintedComment(remarks);
                log.info("Mapped REPCCSRemarks: {} to UserNonPrintedComment", remarks);
            }
            
            // Set required fields that might not be in the incoming data
            if (dto.getCreDate() == null) {
                dto.setCreDate(LocalDateTime.now());
                log.info("Set default CreDate: {}", dto.getCreDate());
            }
            
            if (dto.getCreUserId() == null) {
                dto.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
                log.info("Set default CreUserId: {}", dto.getCreUserId());
            }
            
            if (dto.getLastProcessingDate() == null) {
                dto.setLastProcessingDate(LocalDateTime.now());
                log.info("Set default LastProcessingDate: {}", dto.getLastProcessingDate());
            }
            
            if (dto.getLastProcessingStage() == null) {
                dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
                log.info("Set default LastProcessingStage: {}", dto.getLastProcessingStage());
            }
            
            if (dto.getOffenceNoticeType() == null) {
                dto.setOffenceNoticeType(SystemConstant.OffenceNoticeType.ELECTRONIC);
                log.info("Set default OffenceNoticeType: {}", dto.getOffenceNoticeType());
            }
            
            return dto;
        } catch (Exception e) {
            log.error("Error processing JSON node: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Looks up the parking place name (ppName) based on the parking place code (ppCode)
     * using the Icarpark entity.
     * 
     * @param ppCode The parking place code to look up
     * @return The corresponding parking place name, or null if not found
     */
    private String lookupParkingPlaceName(String ppCode) {
        if (ppCode == null || ppCode.isEmpty()) {
            log.warn("Empty ppCode provided for lookup");
            return null;
        }
        
        log.info("Looking up parking place name for code: {}", ppCode);
        
        try {
            // Use the findByCarParkId method to find car parks with the given code
            List<Icarpark> carparks = icarparkRepository.findByCarParkId(ppCode);
            
            // If we found any results, use the first one
            if (carparks != null && !carparks.isEmpty()) {
                Icarpark carpark = carparks.get(0); // Get the first record
                String carParkName = carpark.getCarParkName();
                
                if (carparks.size() > 1) {
                    log.info("Found {} car parks with code: {}, using the first one", carparks.size(), ppCode);
                }
                
                log.info("Found parking place name: {} for code: {}", carParkName, ppCode);
                return carParkName;
            }
            
            log.warn("No parking place found for code: {}", ppCode);
            return null;
        } catch (Exception e) {
            log.error("Error looking up parking place name for code: {}: {}", ppCode, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Parse different date/time string formats into LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            log.warn("Empty date/time string provided");
            return null;
        }
    
        log.info("Attempting to parse date/time string: {}", dateTimeStr);
        
        try {
            // If it's numeric format like "20231117151222123" (YYYYMMDDHHmmssSSS)
            if (dateTimeStr.matches("\\d{17}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                return LocalDateTime.parse(dateTimeStr, formatter);
            }
            
            // If it's a timestamp number (milliseconds since epoch)
            if (dateTimeStr.matches("\\d+")) {
                long timestamp = Long.parseLong(dateTimeStr);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            }
            
            // Try standard ISO format
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.error("Error parsing date/time string: {}", dateTimeStr, e);
            return null;
        }
    }
}