package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.LocalDateTime;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCode;

@Component
@Slf4j
public class NoticeValidationHelper {
    
    @Autowired
    private OffenceRuleCodeService offenceRuleCodeService;

    /**
     * Creates HTTP error response with status code and message
     */
    public Map<String, Object> createErrorResponse(String statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("HTTPStatusCode", statusCode);
        response.put("HTTPStatusDescription", message);
        return response;
    }
    
    /**
     * Extracts DTO list from mapped data
     */
    @SuppressWarnings("unchecked")
    public List<OffenceNoticeDto> extractDtoList(Map<String, Object> mappedData) {
        return (List<OffenceNoticeDto>) mappedData.get("dtoList");
    }

    /**
     * Validates mandatory fields for batch of DTOs
     * Returns only DTOs that pass validation
     */
    public List<OffenceNoticeDto> validateMandatoryFieldsForBatch(List<OffenceNoticeDto> dtoList) {
        log.info("Validating mandatory fields for {} DTOs", dtoList.size());
        List<OffenceNoticeDto> validDtos = new ArrayList<>();
        
        for (OffenceNoticeDto dto : dtoList) {
            // Validate each mandatory field
            boolean isValid = true;
            
            // Check if notice number is mandatory based on subsystem label
//            String subsystemLabel = dto.getSubsystemLabel();
//            if (subsystemLabel != null &&
//                !"023".equals(subsystemLabel) &&
//                !"021".equals(subsystemLabel) &&
//                !"020".equals(subsystemLabel) &&
//                !"010".equals(subsystemLabel) &&
//                !"009".equals(subsystemLabel) &&
//                !"008".equals(subsystemLabel) &&
//                !"007".equals(subsystemLabel) &&
//                !"006".equals(subsystemLabel) &&
//                !"005".equals(subsystemLabel) &&
//                !"004".equals(subsystemLabel) &&
//                !"003".equals(subsystemLabel) &&
//                !"001".equals(subsystemLabel)) {
//
//                if (dto.getNoticeNo() == null || dto.getNoticeNo().isEmpty()) {
//                    log.error("Mandatory field 'noticeNo' is missing for subsystem: {}", subsystemLabel);
//                    isValid = false;
//                }
//            }
//
            String subsystemLabel = dto.getSubsystemLabel();
            boolean isExemptFromNoticeNo = false;

            if (subsystemLabel != null) {

                if (subsystemLabel.matches("\\d{3}")) {
                    try {
                        int value = Integer.parseInt(subsystemLabel);
                        if (value >= 3 && value <= 10) {
                            isExemptFromNoticeNo = true;
                        }
                        else if (value >= 20 && value <= 21) {
                            isExemptFromNoticeNo = true;
                        }
                        else if (value == 23) {
                            isExemptFromNoticeNo = true;
                        }
                        else if (value >= 30 && value <= 999) {
                            isExemptFromNoticeNo = true;
                        }
                    } catch (NumberFormatException e) {

                    }
                }
            }
           if (!isExemptFromNoticeNo) {
                if (dto.getNoticeNo() == null || dto.getNoticeNo().isEmpty()) {
                    log.error("Mandatory field 'noticeNo' is missing for subsystem: {}", subsystemLabel);
                    isValid = false;
                }
            }
            if (dto.getCompositionAmount() == null) {
                log.error("Mandatory field 'compositionAmount' is missing");
                isValid = false;
            }
            
            if (dto.getComputerRuleCode() == null) {
                log.error("Mandatory field 'computerRuleCode' is missing");
                isValid = false;
            }
            
            if (dto.getCreDate() == null) {
                log.error("Mandatory field 'creDate' is missing");
                isValid = false;
            }
            
            if (dto.getCreUserId() == null || dto.getCreUserId().isEmpty()) {
                log.error("Mandatory field 'creUserId' is missing");
                isValid = false;
            }
            
            // lastProcessingDate and lastProcessingStage are auto-populated by the backend
            // No validation needed as these will be set based on vehicle type
            
            if (dto.getNoticeDateAndTime() == null) {
                log.error("Mandatory field 'noticeDateAndTime' is missing");
                isValid = false;
            }
            
            if (dto.getOffenceNoticeType() == null || dto.getOffenceNoticeType().isEmpty()) {
                log.error("Mandatory field 'offenceNoticeType' is missing");
                isValid = false;
            }
            
            if (dto.getPpCode() == null || dto.getPpCode().isEmpty()) {
                log.error("Mandatory field 'ppCode' is missing");
                isValid = false;
            }
            
            if (dto.getPpName() == null || dto.getPpName().isEmpty()) {
                log.error("Mandatory field 'ppName' is missing");
                isValid = false;
            }
            
            if (dto.getVehicleCategory() == null || dto.getVehicleCategory().isEmpty()) {
                log.error("Mandatory field 'vehicleCategory' is missing");
                isValid = false;
            }
            
            if (isValid) {
                log.info("DTO with noticeNo: {} passed mandatory field validation", dto.getNoticeNo());
                validDtos.add(dto);
            } else {
                log.error("DTO with noticeNo: {} failed mandatory field validation", dto.getNoticeNo());
            }
        }
        
        log.info("{} out of {} DTOs passed mandatory field validation", validDtos.size(), dtoList.size());
        return validDtos;
    }

    /**
     * Prepares single DTO map for processing by copying mapped data and adding DTO
     */
    public Map<String, Object> prepareSingleDtoMap(Map<String, Object> mappedData, OffenceNoticeDto dto) {
        Map<String, Object> singleDtoMap = new HashMap<>(mappedData);
        singleDtoMap.put("dto", dto);
        return singleDtoMap;
    }
    
    /**
     * Checks if notice number already exists OR if offence details already exist
     * Returns true if duplicate found based on either criteria
     */
    public boolean checkDuplicateNoticeNumber(Map<String, Object> data, OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService) {
        long startTime = System.currentTimeMillis();
        log.info("Starting duplicate check process");
        
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("Missing DTO for duplicate check");
                return false;
            }
            
            String noticeNo = dto.getNoticeNo();
            String subsystemLabel = dto.getSubsystemLabel();
            
            // Skip notice number duplicate check if:
            // 1. Notice number is null/empty AND
            // 2. Subsystem is one of the known subsystems that generate notice numbers (OCMS, PLUS, EEPS)
            boolean skipNoticeNumberCheck = (noticeNo == null || noticeNo.trim().isEmpty()) && 
                                           (subsystemLabel != null && 
                                            (subsystemLabel.equals("023") || 
                                            subsystemLabel.equals("021") || 
                                            subsystemLabel.equals("020") || 
                                            subsystemLabel.equals("010") || 
                                            subsystemLabel.equals("009") || 
                                            subsystemLabel.equals("008") || 
                                            subsystemLabel.equals("007") || 
                                            subsystemLabel.equals("006") || 
                                            subsystemLabel.equals("005") || 
                                            subsystemLabel.equals("004") || 
                                            subsystemLabel.equals("003") || 
                                             subsystemLabel.equals("001")));
            
            if (skipNoticeNumberCheck) {
                log.info("Skipping notice number duplicate check for new notice with subsystem: {}", subsystemLabel);
            }
            // First check: Notice number duplicate (only if notice number exists and we're not skipping the check)
            else if (noticeNo != null && !noticeNo.trim().isEmpty()) {
                log.info("Checking for duplicate notice number: {}", noticeNo);
                Optional<OcmsValidOffenceNotice> existingNotice = ocmsValidOffenceNoticeService.getById(noticeNo);
                
                if (existingNotice.isPresent()) {
                    OcmsValidOffenceNotice notice = existingNotice.get();
                    log.warn("Duplicate notice number detected: {}, Vehicle: {}, Date: {}", 
                            noticeNo, notice.getVehicleNo(), notice.getNoticeDateAndTime());
                    log.info("Duplicate check completed in {} ms", System.currentTimeMillis() - startTime);
                    return true;
                }
                log.info("No duplicate notice number found");
            } else {
                log.info("No notice number provided, skipping notice number check");
            }
            
            // Second check: Check for duplicate offence details
            log.info("Checking for duplicate offence details");
            Map<String, String[]> queryParams = new HashMap<>();
            
            // Add mandatory fields to query
            if (dto.getVehicleNo() != null) {
                queryParams.put("vehicleNo", new String[]{dto.getVehicleNo()});
                log.debug("Added vehicleNo to query: {}", dto.getVehicleNo());
            }
            if (dto.getNoticeDateAndTime() != null) {
                queryParams.put("noticeDateAndTime", new String[]{dto.getNoticeDateAndTime().toString()});
                log.debug("Added noticeDateAndTime to query: {}", dto.getNoticeDateAndTime());
            }
            if (dto.getComputerRuleCode() != null) {
                queryParams.put("computerRuleCode", new String[]{dto.getComputerRuleCode().toString()});
                log.debug("Added computerRuleCode to query: {}", dto.getComputerRuleCode());
            }
            if (dto.getParkingLotNo() != null) {
                queryParams.put("parkingLotNo", new String[]{dto.getParkingLotNo()});
                log.debug("Added parkingLotNo to query: {}", dto.getParkingLotNo());
            }
            if (dto.getPpCode() != null) {
                queryParams.put("ppCode", new String[]{dto.getPpCode()});
                log.debug("Added ppCode to query: {}", dto.getPpCode());
            }
            if (dto.getPpName() != null) {
                queryParams.put("ppName", new String[]{dto.getPpName()});
                log.debug("Added ppName to query: {}", dto.getPpName());
            }
            
            // Check if all required fields are present for duplicate check
            if (queryParams.size() < 5) {
                log.warn("Incomplete fields for duplicate check. Only {} of 5 required fields present", queryParams.size());
                log.info("Duplicate check completed in {} ms", System.currentTimeMillis() - startTime);
                return false;
            }
            
            log.info("Querying database with {} parameters", queryParams.size());
            
            // Get all records matching the criteria
            FindAllResponse<OcmsValidOffenceNotice> response = ocmsValidOffenceNoticeService.getAll(queryParams);
            
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                log.info("Found {} potential duplicate records", response.getData().size());
                
                // Check each record for exact match
                for (OcmsValidOffenceNotice existingRecord : response.getData()) {
                    boolean vehicleMatch = Objects.equals(existingRecord.getVehicleNo(), dto.getVehicleNo());
                    boolean dateMatch = Objects.equals(existingRecord.getNoticeDateAndTime(), dto.getNoticeDateAndTime());
                    boolean ruleMatch = Objects.equals(existingRecord.getComputerRuleCode(), dto.getComputerRuleCode());
                    boolean parkingMatch = Objects.equals(existingRecord.getParkingLotNo(), dto.getParkingLotNo());
                    boolean ppCodeMatch = Objects.equals(existingRecord.getPpCode(), dto.getPpCode());
                    boolean ppNameMatch = Objects.equals(existingRecord.getPpName(), dto.getPpName());
                    
                    boolean isExactMatch = vehicleMatch && dateMatch && ruleMatch && parkingMatch && ppCodeMatch && ppNameMatch;
                    
                    if (isExactMatch) {
                        log.warn("Duplicate offence details detected!");
                        log.warn("  Existing Notice: {}", existingRecord.getNoticeNo());
                        log.warn("  Vehicle: {}", existingRecord.getVehicleNo());
                        log.warn("  Date: {}", existingRecord.getNoticeDateAndTime());
                        log.warn("  Rule Code: {}", existingRecord.getComputerRuleCode());
                        log.warn("  Parking Lot: {}", existingRecord.getParkingLotNo());
                        log.warn("  PP Code: {}", existingRecord.getPpCode());
                        log.warn("  PP Name: {}", existingRecord.getPpName());
                        log.info("Duplicate check completed in {} ms", System.currentTimeMillis() - startTime);
                        return true;
                    } else {
                        log.debug("Record {} is not an exact match", existingRecord.getNoticeNo());
                        log.debug("  Vehicle match: {}, Date match: {}, Rule match: {}, Parking match: {}, PP Code match: {}, PP Name match: {}",
                                vehicleMatch, dateMatch, ruleMatch, parkingMatch, ppCodeMatch, ppNameMatch);
                    }
                }
            } else {
                log.info("No potential duplicate records found");
            }
            
            log.info("No duplicates found");
            log.info("Duplicate check completed in {} ms", System.currentTimeMillis() - startTime);
            return false;
        } catch (Exception e) {
            log.error("Duplicate check failed: {}", e.getMessage(), e);
            log.info("Duplicate check failed after {} ms", System.currentTimeMillis() - startTime);
            return false;
        }
    }

    /**
     * Gets offense type from offense_rule_code table using computer rule code
     * Throws RuntimeException if not found
     */
    public String getOffenseTypeFromRuleCode(Map<String, Object> data) {
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null || dto.getComputerRuleCode() == null) {
                log.error("Cannot get offense type: DTO or computer rule code is null");
                throw new RuntimeException("Missing required data for offense type lookup");
            }
            
            Integer computerRuleCode = dto.getComputerRuleCode();
            log.info("Looking up offense type for computerRuleCode: {}", computerRuleCode);
            
            // Query offense_rule_code table by computer rule code only
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("computerRuleCode", new String[]{computerRuleCode.toString()});
            
            FindAllResponse<OffenceRuleCode> response = offenceRuleCodeService.getAll(queryParams);
            log.debug("OffenceRuleCode response: {}", response);

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                String offenseType = response.getData().get(0).getOffenceType();
                log.info("Found offense type: {} for rule code: {}", offenseType, computerRuleCode);
                return offenseType;
            } else {
                log.error("No offense rule found for computerRuleCode: {}", computerRuleCode);
                throw new RuntimeException("No offense rule found for computer rule code: " + computerRuleCode);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving offense type from rule code table: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving offense type: " + e.getMessage(), e);
        }
    }

    /**
     * Checks for duplicate offense details
     * OCMS 21: Type O (Parking) = date only, Type E (Enforcement) = date + time (HH:MM)
     */
    public boolean checkDuplicateOffenseDetails(Map<String, Object> data, OcmsValidOffenceNoticeService validOffenceNoticeService) {
        log.info("Checking for duplicate offense details");

        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for duplicate offense details check");
                return false;
            }

            // Get offense type from data map (set by caller)
            String offenseType = (String) data.get("offenseType");

            String vehicleNo = dto.getVehicleNo();
            LocalDateTime noticeDateAndTime = dto.getNoticeDateAndTime();
            Integer computerRuleCode = dto.getComputerRuleCode();
            String ppCode = dto.getPpCode();

            // Check if any required field is missing
            if (vehicleNo == null || noticeDateAndTime == null || computerRuleCode == null || ppCode == null) {
                log.info("One or more required fields for duplicate check are null");
                return false;
            }

            log.info("DBB check for Offense Type: {}, Vehicle: {}, Date: {}, RuleCode: {}, PP: {}",
                    offenseType, vehicleNo, noticeDateAndTime, computerRuleCode, ppCode);

            // Create a map of query parameters for filtering
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("vehicleNo", new String[]{vehicleNo});
            queryParams.put("computerRuleCode", new String[]{computerRuleCode.toString()});
            queryParams.put("ppCode", new String[]{ppCode});

            // OCMS 21: Query for existing notices with retry logic (3 attempts)
            FindAllResponse<OcmsValidOffenceNotice> response = null;
            int maxRetries = 3;
            int retryCount = 0;
            Exception lastException = null;

            while (retryCount < maxRetries && response == null) {
                try {
                    retryCount++;
                    log.info("DBB query attempt {}/{}", retryCount, maxRetries);
                    response = validOffenceNoticeService.getAll(queryParams);
                    log.info("DBB query successful on attempt {}", retryCount);
                } catch (Exception e) {
                    lastException = e;
                    log.warn("DBB query attempt {}/{} failed: {}", retryCount, maxRetries, e.getMessage());

                    if (retryCount < maxRetries) {
                        // Wait before retry (exponential backoff: 100ms, 200ms, 400ms)
                        try {
                            long waitTime = (long) (100 * Math.pow(2, retryCount - 1));
                            log.info("Waiting {}ms before retry", waitTime);
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Retry wait interrupted");
                        }
                    }
                }
            }

            // OCMS 21: If all retries failed, flag for TS-OLD fallback
            if (response == null) {
                log.error("DBB query failed after {} attempts, will apply TS-OLD fallback", maxRetries);
                data.put("dbbQueryFailed", true);
                data.put("dbbQueryException", lastException != null ? lastException.getMessage() : "Unknown error");
                return false; // Return false to indicate no duplicate found, but flag is set for TS-OLD
            }

            if (response != null && response.getData() != null) {
                log.info("Found {} potential duplicate notices to check", response.getData().size());

                // Check if any of the found notices match our criteria
                for (OcmsValidOffenceNotice notice : response.getData()) {
                    // Skip comparing with itself if notice number is the same
                    if (dto.getNoticeNo() != null && dto.getNoticeNo().equals(notice.getNoticeNo())) {
                        log.debug("Skipping self-comparison for notice {}", notice.getNoticeNo());
                        continue;
                    }

                    LocalDateTime existingNoticeDateTime = notice.getNoticeDateAndTime();
                    if (existingNoticeDateTime == null) {
                        log.debug("Skipping notice {} - no date/time", notice.getNoticeNo());
                        continue;
                    }

                    // OCMS 21: Type O vs Type E date comparison
                    boolean dateTimeMatch = false;

                    if ("O".equals(offenseType)) {
                        // Type O (Parking): Compare DATE only (ignore time)
                        boolean dateOnlyMatch = noticeDateAndTime.toLocalDate().equals(existingNoticeDateTime.toLocalDate());
                        dateTimeMatch = dateOnlyMatch;
                        log.debug("Type O check - New: {}, Existing: {}, Match: {}",
                                noticeDateAndTime.toLocalDate(), existingNoticeDateTime.toLocalDate(), dateTimeMatch);
                    } else if ("E".equals(offenseType)) {
                        // Type E (Enforcement): Compare DATE + TIME (HH:MM only, ignore seconds)
                        boolean dateMatch = noticeDateAndTime.toLocalDate().equals(existingNoticeDateTime.toLocalDate());
                        boolean hourMatch = noticeDateAndTime.getHour() == existingNoticeDateTime.getHour();
                        boolean minuteMatch = noticeDateAndTime.getMinute() == existingNoticeDateTime.getMinute();
                        dateTimeMatch = dateMatch && hourMatch && minuteMatch;
                        log.debug("Type E check - New: {} {}:{}, Existing: {} {}:{}, Match: {}",
                                noticeDateAndTime.toLocalDate(), noticeDateAndTime.getHour(), noticeDateAndTime.getMinute(),
                                existingNoticeDateTime.toLocalDate(), existingNoticeDateTime.getHour(), existingNoticeDateTime.getMinute(),
                                dateTimeMatch);
                    } else {
                        // Type U or unknown: Use exact date/time match
                        dateTimeMatch = noticeDateAndTime.equals(existingNoticeDateTime);
                        log.debug("Type {} (unknown) check - using exact match: {}", offenseType, dateTimeMatch);
                    }

                    if (dateTimeMatch) {
                        log.info("Date/time match found with existing notice {}", notice.getNoticeNo());

                        // OCMS 21: Check suspension status to determine if this is a qualifying duplicate
                        String suspensionType = notice.getSuspensionType();
                        String eprReason = notice.getEprReasonOfSuspension();
                        String crsReason = notice.getCrsReasonOfSuspension();

                        log.info("Existing notice {} - SuspensionType: {}, EPR: {}, CRS: {}",
                                notice.getNoticeNo(), suspensionType, eprReason, crsReason);

                        boolean isDuplicate = false;
                        String duplicateReason = "";

                        // Check if existing notice is NOT suspended (active notice)
                        if (suspensionType == null || suspensionType.trim().isEmpty()) {
                            isDuplicate = true;
                            duplicateReason = "Existing notice is active (not suspended)";
                        }
                        // Check if existing notice is Provisionally Suspended (PS)
                        else if ("PS".equals(suspensionType)) {
                            // Check EPR (Enforcement Processing) suspension reasons
                            if ("FOR".equals(eprReason)) {
                                isDuplicate = true;
                                duplicateReason = "Existing notice is PS-FOR (Foreign)";
                            }
                            // OCMS 21: ANS check ONLY for Type O (Parking), NOT for Type E (Enforcement)
                            else if ("ANS".equals(eprReason) && "O".equals(offenseType)) {
                                isDuplicate = true;
                                duplicateReason = "Existing notice is PS-ANS (Advisory Notice Sent) - Type O only";
                            } else if ("DBB".equals(eprReason)) {
                                isDuplicate = true;
                                duplicateReason = "Existing notice is PS-DBB (Double Booking)";
                            }
                            // Check CRS (Court Related) suspension reasons
                            else if ("FP".equals(crsReason) || "PRA".equals(crsReason)) {
                                isDuplicate = true;
                                duplicateReason = "Existing notice is PS-" + crsReason;
                            } else {
                                // Other PS codes - NOT a duplicate
                                log.info("Existing notice has PS-{}/{} - NOT qualifying for DBB (offenseType={})", eprReason, crsReason, offenseType);
                                continue;
                            }
                        } else {
                            // Other suspension types (TS, etc.) - check if it's active
                            log.info("Existing notice has suspension type {} - treating as active", suspensionType);
                            isDuplicate = true;
                            duplicateReason = "Existing notice is suspended with type " + suspensionType;
                        }

                        if (isDuplicate) {
                            log.warn("DUPLICATE OFFENSE DETECTED!");
                            log.warn("  Reason: {}", duplicateReason);
                            log.warn("  Existing Notice: {}", notice.getNoticeNo());
                            log.warn("  Vehicle: {}", vehicleNo);
                            log.warn("  Date/Time: New={}, Existing={}", noticeDateAndTime, existingNoticeDateTime);
                            log.warn("  Rule Code: {}", computerRuleCode);
                            log.warn("  PP Code: {}", ppCode);

                            // Store the duplicate notice number in the data map for later use
                            data.put("duplicateNoticeNo", notice.getNoticeNo());
                            data.put("duplicateReason", duplicateReason);
                            return true;
                        }
                    }
                }
            }

            log.info("No duplicate offense details found");
            return false;

        } catch (Exception e) {
            log.error("Error checking for duplicate offense details: {}", e.getMessage(), e);
            return false;
        }
    }
}