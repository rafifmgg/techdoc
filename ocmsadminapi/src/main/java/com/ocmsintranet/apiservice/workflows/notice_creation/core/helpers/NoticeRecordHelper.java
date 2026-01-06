package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.LocalDateTime;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCode;
import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeRepository;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Component
@Slf4j
public class NoticeRecordHelper {

    @Autowired
    private OcmsOffenceNoticeDetailService ocmsOffenceNoticeDetailService;

    @Autowired
    private OffenceRuleCodeRepository offenceRuleCodeRepository;

    @Autowired
    private EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    /**
     * Creates notice records in both valid_offence_notice and offence_notice_detail tables
     */
    public void createNoticeRecord(Map<String, Object> data, 
                                  OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService, 
                                  OcmsOffenceNoticeDetailService passedDetailService) {
        
        log.info("========================================");
        log.info("Starting createNoticeRecord");
        log.info("========================================");
        
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found in data map");
                throw new RuntimeException("No DTO found in data map");
            }

            String noticeNumber = dto.getNoticeNo();
            log.info("Creating notice records for notice number: {}", noticeNumber);

            // Create valid offence notice entity
            OcmsValidOffenceNotice validOffenceNotice = new OcmsValidOffenceNotice();
            
            // Map DTO to entity with automatic truncation
            mapDtoToValidOffenceNotice(dto, validOffenceNotice);

            // Save valid offence notice
            OcmsValidOffenceNotice savedNotice = null;
            try {
                log.info("========================================");
                log.info("Attempting to save record to ocms_valid_offence_notice table");
                log.info("========================================");
                
                savedNotice = ocmsValidOffenceNoticeService.save(validOffenceNotice);
                
                log.info("Successfully saved to ocms_valid_offence_notice table");
                log.info("Saved notice number: {}", savedNotice.getNoticeNo());
            } catch (Exception e) {
                log.error("Error saving to ocms_valid_offence_notice table");
                log.error("Error message: {}", e.getMessage());
                log.error("Full stack trace:", e);
                throw e;
            }

            // Save notice detail only if valid notice saved successfully
            if (savedNotice != null) {
                try {
                    // Use the passed service parameter if provided, otherwise use the injected one
                    OcmsOffenceNoticeDetailService detailService = (passedDetailService != null) ? 
                        passedDetailService : this.ocmsOffenceNoticeDetailService;
                    
                    if (detailService == null) {
                        log.error("OcmsOffenceNoticeDetailService is null - both passed parameter and injected service are null");
                        log.error("Passed service: {}, Injected service: {}", passedDetailService, this.ocmsOffenceNoticeDetailService);
                        throw new RuntimeException("OcmsOffenceNoticeDetailService is not available");
                    }
                    
                    // Create offence notice detail entity
                    OcmsOffenceNoticeDetail ocmsOffenceNoticeDetail = new OcmsOffenceNoticeDetail();
                    
                    // Use the saved notice number
                    String savedNoticeNo = savedNotice.getNoticeNo();
                    ocmsOffenceNoticeDetail.setNoticeNo(savedNoticeNo);
                    if (dto.getTransactionId() != null && !dto.getTransactionId().isEmpty()) {
                        // just for repccs for
                        ocmsOffenceNoticeDetail.setTransactionId(dto.getTransactionId());
                    }
                    log.info("Using notice number for detail record: {}", savedNoticeNo);
                    
                    // Map DTO to entity with automatic truncation
                    mapDtoToOffenceNoticeDetail(dto, ocmsOffenceNoticeDetail);

                    log.info("========================================");
                    log.info("Attempting to save record to ocms_offence_notice_detail table");
                    log.info("========================================");
                    
                    detailService.save(ocmsOffenceNoticeDetail);
                    
                    log.info("Successfully saved to ocms_offence_notice_detail table");

                    // Update the DTO with the saved notice number for downstream processing
                    dto.setNoticeNo(savedNoticeNo);
                } catch (Exception e) {
                    log.error("Error saving to ocms_offence_notice_detail table");
                    log.error("Error message: {}", e.getMessage());
                    log.error("Full stack trace:", e);
                    throw e;
                }

                // PROCESS 1: Sync to Internet Database (eVON) immediately after intranet save
                if (paymentSyncEnabled) {
                    try {
                        log.info("========================================");
                        log.info("PROCESS 1: Syncing to Internet Database (eVON)");
                        log.info("========================================");

                        // Create internet database entity
                        EocmsValidOffenceNotice eVon = new EocmsValidOffenceNotice();
                        mapDtoToEocmsValidOffenceNotice(dto, savedNotice, eVon);

                        // Save to internet database
                        log.info("Attempting to save record to eocms_valid_offence_notice table (Internet DB)");
                        eocmsValidOffenceNoticeService.save(eVon);
                        log.info("Successfully saved to eocms_valid_offence_notice table (Internet DB)");

                        // Update is_sync flag to "Y" in intranet database
                        log.info("Updating is_sync flag to 'Y' in intranet database");
                        savedNotice.setIsSync("Y");
                        savedNotice.setUpdDate(LocalDateTime.now());
                        ocmsValidOffenceNoticeService.save(savedNotice);
                        log.info("Successfully updated is_sync flag to true");

                        log.info("========================================");
                        log.info("PROCESS 1: Internet sync completed successfully");
                        log.info("========================================");
                    } catch (Exception e) {
                        log.error("========================================");
                        log.error("PROCESS 1: Error syncing to internet database");
                        log.error("Error message: {}", e.getMessage());
                        log.error("Full stack trace:", e);
                        log.error("========================================");
                        // Note: We don't throw here - the intranet record was saved successfully
                        // The is_sync flag remains "N" so the cron job can retry the sync
                        log.warn("Record saved to intranet but NOT synced to internet. is_sync flag remains 'N' for cron retry");
                    }
                } else {
                    log.info("========================================");
                    log.info("PROCESS 1: Payment sync disabled - skipping immediate sync to Internet DB");
                    log.info("Notice {} saved to intranet with is_sync=false for batch sync retry", savedNotice.getNoticeNo());
                    log.info("========================================");
                }
            }

            log.info("========================================");
            log.info("Completed createNoticeRecord successfully");
            log.info("========================================");
        } catch (Exception e) {
            log.error("========================================");
            log.error("Unexpected error in createNoticeRecord");
            log.error("Error message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            log.error("========================================");
            throw e;
        }
    }

    /**
     * Generic method to truncate string if it exceeds max length
     */
    private String truncateString(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            String truncated = value.substring(0, maxLength);
            log.warn("{} truncated from '{}' to '{}'", fieldName, value, truncated);
            return truncated;
        }
        return value;
    }

    /**
     * Map DTO to ValidOffenceNotice entity with automatic truncation
     */
    private void mapDtoToValidOffenceNotice(OffenceNoticeDto dto, OcmsValidOffenceNotice entity) {
        // Get column lengths from the helper method
        Map<String, Integer> fieldLengths = getValidOffenceNoticeFieldLengths();
        
        // Map all fields
        entity.setNoticeNo(truncateString(dto.getNoticeNo(), fieldLengths.getOrDefault("noticeNo", 10), "noticeNo"));
        entity.setCompositionAmount(dto.getCompositionAmount());
        
        // Set amountPayable equal to compositionAmount for all vehicle types
        entity.setAmountPayable(dto.getCompositionAmount());
        log.info("Setting amount payable equal to composition amount: {}", dto.getCompositionAmount());
        
        entity.setComputerRuleCode(dto.getComputerRuleCode());
        entity.setCreDate(dto.getCreDate());
        entity.setCreUserId(truncateString(dto.getCreUserId(), fieldLengths.getOrDefault("creUserId", 50), "creUserId"));
        entity.setLastProcessingDate(dto.getLastProcessingDate());
        entity.setLastProcessingStage(truncateString(dto.getLastProcessingStage(), fieldLengths.getOrDefault("lastProcessingStage", 3), "lastProcessingStage"));
        entity.setNextProcessingDate(dto.getNextProcessingDate());
        entity.setNextProcessingStage(truncateString(dto.getNextProcessingStage(), fieldLengths.getOrDefault("nextProcessingStage", 3), "nextProcessingStage"));
        entity.setNoticeDateAndTime(dto.getNoticeDateAndTime());
        entity.setOffenceNoticeType(truncateString(dto.getOffenceNoticeType(), fieldLengths.getOrDefault("offenceNoticeType", 1), "offenceNoticeType"));
        entity.setParkingLotNo(truncateString(dto.getParkingLotNo(), fieldLengths.getOrDefault("parkingLotNo", 5), "parkingLotNo"));
        entity.setPpCode(truncateString(dto.getPpCode(), fieldLengths.getOrDefault("ppCode", 5), "ppCode"));
        entity.setPpName(truncateString(dto.getPpName(), fieldLengths.getOrDefault("ppName", 100), "ppName"));
        entity.setPrevProcessingDate(dto.getPrevProcessingDate());
        entity.setPrevProcessingStage(truncateString(dto.getPrevProcessingStage(), fieldLengths.getOrDefault("prevProcessingStage", 3), "prevProcessingStage"));
        entity.setSuspensionType(truncateString(dto.getSuspensionType(), fieldLengths.getOrDefault("suspensionType", 2), "suspensionType"));
        entity.setUpdDate(dto.getUpdDate());
        entity.setUpdUserId(truncateString(dto.getUpdUserId(), fieldLengths.getOrDefault("updUserId", 50), "updUserId"));
        entity.setVehicleCategory(truncateString(dto.getVehicleCategory(), fieldLengths.getOrDefault("vehicleCategory", 1), "vehicleCategory"));
        entity.setVehicleNo(truncateString(dto.getVehicleNo(), fieldLengths.getOrDefault("vehicleNo", 14), "vehicleNo"));
        
        // Log vehicle number and registration type before saving
        String vehicleNo = dto.getVehicleNo();
        String vehicleRegType = dto.getVehicleRegistrationType();
        
        log.info("Before saving to database - Vehicle No: '{}', Vehicle Registration Type: '{}'", vehicleNo, vehicleRegType);
        
        entity.setVehicleRegistrationType(truncateString(vehicleRegType, fieldLengths.getOrDefault("vehicleRegistrationType", 1), "vehicleRegistrationType"));
        entity.setSubsystemLabel(truncateString(dto.getSubsystemLabel(), fieldLengths.getOrDefault("subsystemLabel", 50), "subsystemLabel"));
        entity.setWardenNo(truncateString(dto.getWardenNo(), fieldLengths.getOrDefault("wardenNo", 5), "wardenNo"));
        entity.setRepChargeAmount(dto.getRepChargeAmount());
        entity.setAnFlag(truncateString(dto.getAnFlag(), fieldLengths.getOrDefault("anFlag", 1), "anFlag"));


        // Map EPR suspension fields - these were missing and causing the bug with foreign vehicles
        entity.setEprReasonOfSuspension(truncateString(dto.getEprReasonOfSuspension(), fieldLengths.getOrDefault("eprReasonOfSuspension", 3), "eprReasonOfSuspension"));
        entity.setEprDateOfSuspension(dto.getEprDateOfSuspension());
        entity.setOtherRemark(dto.getOtherRemark());
        log.info("Setting EPR reason of suspension: '{}', EPR date of suspension: '{}'", dto.getEprReasonOfSuspension(), dto.getEprDateOfSuspension());

        // Set is_sync flag to "N" (not synced) initially
        entity.setIsSync("N");
        log.info("Setting is_sync flag to 'N' (not synced to internet database)");

        // Map eservice message code
        entity.setEserviceMessageCode(truncateString(dto.getEserviceMessageCode(), fieldLengths.getOrDefault("eserviceMessageCode", 10), "eserviceMessageCode"));

        // Map payment acceptance allowed - default to "Y" if not provided
        String paymentAllowed = dto.getPaymentAcceptanceAllowed();
        if (paymentAllowed == null || paymentAllowed.trim().isEmpty()) {
            paymentAllowed = "Y"; // Default to allow payment
            log.info("Setting payment_acceptance_allowed to default 'Y'");
        }
        entity.setPaymentAcceptanceAllowed(truncateString(paymentAllowed, fieldLengths.getOrDefault("paymentAcceptanceAllowed", 1), "paymentAcceptanceAllowed"));

        // Map payment status
        entity.setPaymentStatus(truncateString(dto.getPaymentStatus(), fieldLengths.getOrDefault("paymentStatus", 2), "paymentStatus"));
    }
    
    /**
     * Map DTO to OffenceNoticeDetail entity with automatic truncation
     */
    private void mapDtoToOffenceNoticeDetail(OffenceNoticeDto dto, OcmsOffenceNoticeDetail entity) {
        Map<String, Integer> fieldLengths = getOffenceNoticeDetailFieldLengths();

        // Map all fields with truncation where needed
        entity.setColorOfVehicle(truncateString(dto.getColorOfVehicle(), fieldLengths.getOrDefault("colorOfVehicle", 15), "colorOfVehicle"));
        // entity.setComments(truncateString(dto.getComments(), fieldLengths.getOrDefault("comments", 30), "comments"));
        entity.setConditionInvalidCoupon1(truncateString(dto.getConditionInvalidCoupon1(), fieldLengths.getOrDefault("conditionInvalidCoupon1", 25), "conditionInvalidCoupon1"));
        entity.setConditionInvalidCoupon2(truncateString(dto.getConditionInvalidCoupon2(), fieldLengths.getOrDefault("conditionInvalidCoupon2", 25), "conditionInvalidCoupon2"));

        // Audit fields are handled by BaseEntity
        // No need to set them manually here
        entity.setCreUserId(dto.getCreUserId());
        entity.setDenomInvalidCoupon1(dto.getDenomInvalidCoupon1());
        entity.setDenomInvalidCoupon2(dto.getDenomInvalidCoupon2());
        entity.setDenomOfValidCoupon(dto.getDenomOfValidCoupon());
        entity.setExpiryTime(dto.getExpiryTime());
        // entity.setInvalidCoupon1CreasedTab(truncateString(dto.getInvalidCoupon1CreasedTab(), fieldLengths.getOrDefault("invalidCoupon1CreasedTab", 40), "invalidCoupon1CreasedTab"));
        // entity.setInvalidCoupon1Subtype(truncateString(dto.getInvalidCoupon1Subtype(), fieldLengths.getOrDefault("invalidCoupon1Subtype", 10), "invalidCoupon1Subtype"));
        // entity.setInvalidCoupon2CreasedTab(truncateString(dto.getInvalidCoupon2CreasedTab(), fieldLengths.getOrDefault("invalidCoupon2CreasedTab", 40), "invalidCoupon2CreasedTab"));
        // entity.setInvalidCoupon2Subtype(truncateString(dto.getInvalidCoupon2Subtype(), fieldLengths.getOrDefault("invalidCoupon2Subtype", 10), "invalidCoupon2Subtype"));
        entity.setConditionInvalidCoupon3(truncateString(dto.getConditionInvalidCoupon3(), fieldLengths.getOrDefault("conditionInvalidCoupon3", 25), "conditionInvalidCoupon3"));
        // entity.setInvalidCoupon3CreasedTab(truncateString(dto.getInvalidCoupon3CreasedTab(), fieldLengths.getOrDefault("invalidCoupon3CreasedTab", 40), "invalidCoupon3CreasedTab"));
        entity.setDenomInvalidCoupon3(dto.getDenomInvalidCoupon3());
        entity.setInvalidCouponNo3(truncateString(dto.getInvalidCouponNo3(), fieldLengths.getOrDefault("invalidCouponNo3", 9), "invalidCouponNo3"));
        // entity.setInvalidCoupon3Subtype(truncateString(dto.getInvalidCoupon3Subtype(), fieldLengths.getOrDefault("invalidCoupon3Subtype", 10), "invalidCoupon3Subtype"));
        entity.setInvalidCouponNo1(truncateString(dto.getInvalidCouponNo1(), fieldLengths.getOrDefault("invalidCouponNo1", 9), "invalidCouponNo1"));
        entity.setInvalidCouponNo2(truncateString(dto.getInvalidCouponNo2(), fieldLengths.getOrDefault("invalidCouponNo2", 9), "invalidCouponNo2"));
        entity.setIuNo(truncateString(dto.getIuNo(), fieldLengths.getOrDefault("iuNo", 10), "iuNo"));
        entity.setLastValidCouponNo(truncateString(dto.getLastValidCouponNo(), fieldLengths.getOrDefault("lastValidCouponNo", 9), "lastValidCouponNo"));

        // REMOVED: This field doesn't exist in the database
        // entity.setLastValidCouponSubtype(truncateString(dto.getLastValidCouponSubtype(), fieldLengths.getOrDefault("lastValidCouponSubtype", 10), "lastValidCouponSubtype"));

        entity.setLtaChassisNumber(truncateString(dto.getLtaChassisNumber(), fieldLengths.getOrDefault("ltaChassisNumber", 25), "ltaChassisNumber"));
        entity.setLtaDeregistrationDate(dto.getLtaDeregistrationDate());
        entity.setLtaDiplomaticFlag(truncateString(dto.getLtaDiplomaticFlag(), fieldLengths.getOrDefault("ltaDiplomaticFlag", 1), "ltaDiplomaticFlag"));
        entity.setLtaEffOwnershipDate(dto.getLtaEffOwnershipDate());
        entity.setLtaMakeDescription(truncateString(dto.getLtaMakeDescription(), fieldLengths.getOrDefault("ltaMakeDescription", 100), "ltaMakeDescription"));
        entity.setLtaMaxLadenWeight(dto.getLtaMaxLadenWeight());
        entity.setLtaPrimaryColour(truncateString(dto.getLtaPrimaryColour(), fieldLengths.getOrDefault("ltaPrimaryColour", 100), "ltaPrimaryColour"));
        entity.setLtaRoadTaxExpiryDate(dto.getLtaRoadTaxExpiryDate());
        entity.setLtaSecondaryColour(truncateString(dto.getLtaSecondaryColour(), fieldLengths.getOrDefault("ltaSecondaryColour", 100), "ltaSecondaryColour"));
        entity.setLtaUnladenWeight(dto.getLtaUnladenWeight());
        entity.setRepParkingEndDt(dto.getRepParkingEndDt());
        entity.setRepParkingEntryDt(dto.getRepParkingEntryDt());
        entity.setRepParkingStartDt(dto.getRepParkingStartDt());
        entity.setTotalCouponsDisplayed(dto.getTotalCouponsDisplayed());
        entity.setVehicleMake(truncateString(dto.getVehicleMake(), fieldLengths.getOrDefault("vehicleMake", 50), "vehicleMake"));
        entity.setRepViolationCode(truncateString(dto.getRepViolationCode(), fieldLengths.getOrDefault("repViolationCode", 2), "repViolationCode"));
        entity.setCreatedDt(dto.getCreatedDt());

        entity.setRepObuLatitude(dto.getRepObuLatitude());
        entity.setRepObuLongitude(dto.getRepObuLongitude());
        entity.setRepOperatorId(truncateString(dto.getRepOperatorId(), fieldLengths.getOrDefault("repOperatorId", 20), "repOperatorId"));
        // entity.setRule11CouponTime(dto.getRule11CouponTime());
        // entity.setLastValidCouponExpiredDate(dto.getLastValidCouponExpiredDate());
        // entity.setLastValidCouponExpiredTime(dto.getLastValidCouponExpiredTime());
        // entity.setFirstCouponTime(dto.getFirstCouponTime());
        // entity.setExpiredCoupon1No(truncateString(dto.getExpiredCoupon1No(), fieldLengths.getOrDefault("expiredCoupon1No", 9), "expiredCoupon1No"));
        // entity.setExpiredCoupon1Denom(dto.getExpiredCoupon1Denom());
        // entity.setExpiredCoupon1Subtype(truncateString(dto.getExpiredCoupon1Subtype(), fieldLengths.getOrDefault("expiredCoupon1Subtype", 10), "expiredCoupon1Subtype"));
        // entity.setExpiredCoupon2No(truncateString(dto.getExpiredCoupon2No(), fieldLengths.getOrDefault("expiredCoupon2No", 9), "expiredCoupon2No"));
        // entity.setExpiredCoupon2Denom(dto.getExpiredCoupon2Denom());
        // entity.setExpiredCoupon2Subtype(truncateString(dto.getExpiredCoupon2Subtype(), fieldLengths.getOrDefault("expiredCoupon2Subtype", 10), "expiredCoupon2Subtype"));
        // entity.setLastValidCouponDenom(dto.getLastValidCouponDenom());

        entity.setRuleRemark1(dto.getRuleRemark1());
        entity.setRuleRemark2(dto.getRuleRemark2());
        entity.setCreatedDt(dto.getCreatedDt());
        entity.setSysNonPrintedComment(dto.getSysNonPrintedComment());
        entity.setUserNonPrintedComment(dto.getUserNonPrintedComment());

        // Fetch and set ruleNo and ruleDesc from OffenceRuleCode table
        fetchAndSetRuleNoAndDesc(dto, entity, fieldLengths);
    }

    /**
     * Fetch ruleNo and description from OffenceRuleCode table using computerRuleCode, vehicleCategory, and offenceNoticeType
     */
    private void fetchAndSetRuleNoAndDesc(OffenceNoticeDto dto, OcmsOffenceNoticeDetail entity, Map<String, Integer> fieldLengths) {
        try {
            Integer computerRuleCode = dto.getComputerRuleCode();
            String vehicleCategory = dto.getVehicleCategory();
            String offenceNoticeType = dto.getOffenceNoticeType();

            // Validate required parameters
            if (computerRuleCode == null || vehicleCategory == null || offenceNoticeType == null) {
                log.warn("Missing required parameters for fetching rule details - computerRuleCode: {}, vehicleCategory: {}, offenceNoticeType: {}",
                    computerRuleCode, vehicleCategory, offenceNoticeType);
                return;
            }

            // Use current date for effective date range check
            LocalDateTime currentDate = LocalDateTime.now();

            // Query OffenceRuleCode
            var offenceRuleCode = offenceRuleCodeRepository.findByRuleCodeVehicleCategoryAndOffenceType(
                computerRuleCode,
                vehicleCategory,
                offenceNoticeType,
                currentDate
            );

            if (offenceRuleCode.isPresent()) {
                OffenceRuleCode ruleCodeEntity = offenceRuleCode.get();
                String ruleNo = ruleCodeEntity.getRuleNo();
                String description = ruleCodeEntity.getDescription();

                entity.setRuleNo(truncateString(ruleNo, fieldLengths.getOrDefault("ruleNo", 5), "ruleNo"));
                entity.setRuleDesc(truncateString(description, fieldLengths.getOrDefault("ruleDesc", 255), "ruleDesc"));

                log.info("Successfully fetched rule details - ruleNo: {}, description: {}", ruleNo, description);
            } else {
                log.warn("No matching OffenceRuleCode found for computerRuleCode: {}, vehicleCategory: {}, offenceNoticeType: {}",
                    computerRuleCode, vehicleCategory, offenceNoticeType);
            }
        } catch (Exception e) {
            log.error("Error fetching rule details from OffenceRuleCode table", e);
        }
    }

/**
 * Get field lengths for ValidOffenceNotice entity
 */
private Map<String, Integer> getValidOffenceNoticeFieldLengths() {
    Map<String, Integer> fieldLengths = new HashMap<>();
    fieldLengths.put("noticeNo", 10);
    fieldLengths.put("creUserId", 50);
    fieldLengths.put("lastProcessingStage", 3);
    fieldLengths.put("nextProcessingStage", 3);
    fieldLengths.put("offenceNoticeType", 1);
    fieldLengths.put("parkingLotNo", 5);
    fieldLengths.put("ppCode", 5);
    fieldLengths.put("ppName", 100);
    fieldLengths.put("prevProcessingStage", 3);
    fieldLengths.put("suspensionType", 2);
    fieldLengths.put("updUserId", 50);
    fieldLengths.put("vehicleCategory", 1);
    fieldLengths.put("vehicleNo", 14);
    fieldLengths.put("vehicleRegistrationType", 1);
    fieldLengths.put("subsystemLabel", 50);
    fieldLengths.put("wardenNo", 5);
    fieldLengths.put("anFlag", 1);
    fieldLengths.put("eserviceMessageCode", 10);
    fieldLengths.put("paymentAcceptanceAllowed", 1);
    fieldLengths.put("paymentStatus", 2);
    return fieldLengths;
}

/**
 * Get field lengths for OffenceNoticeDetail entity
 */
private Map<String, Integer> getOffenceNoticeDetailFieldLengths() {
    Map<String, Integer> fieldLengths = new HashMap<>();
    fieldLengths.put("colorOfVehicle", 15);
    fieldLengths.put("comments", 30);
    fieldLengths.put("conditionInvalidCoupon1", 25);
    fieldLengths.put("conditionInvalidCoupon2", 25);
    fieldLengths.put("creUserId", 50);
    fieldLengths.put("invalidCoupon1CreasedTab", 40);
    fieldLengths.put("invalidCoupon1Subtype", 10);
    fieldLengths.put("invalidCoupon2CreasedTab", 40);
    fieldLengths.put("invalidCoupon2Subtype", 10);
    fieldLengths.put("conditionInvalidCoupon3", 25);
    fieldLengths.put("invalidCoupon3CreasedTab", 40);
    fieldLengths.put("invalidCoupon3No", 9);
    fieldLengths.put("invalidCoupon3Subtype", 10);
    fieldLengths.put("invalidCouponNo1", 9);
    fieldLengths.put("invalidCouponNo2", 9);
    fieldLengths.put("iuNo", 10);
    fieldLengths.put("lastValidCouponNo", 9);
    fieldLengths.put("lastValidCouponSubtype", 10);
    fieldLengths.put("ltaChassisNumber", 25);
    fieldLengths.put("ltaDiplomaticFlag", 1);
    fieldLengths.put("ltaMakeDescription", 100);
    fieldLengths.put("ltaPrimaryColour", 100);
    fieldLengths.put("ltaSecondaryColour", 100);
    fieldLengths.put("vehicleMake", 50);
    fieldLengths.put("repViolationCode", 2);
    fieldLengths.put("ruleNo", 5);
    fieldLengths.put("ruleDesc", 255);
    return fieldLengths;
    }

    /**
     * Map DTO and saved intranet notice to EocmsValidOffenceNotice (Internet DB entity)
     * Maps only public-facing fields (excludes sensitive internal data)
     */
    private void mapDtoToEocmsValidOffenceNotice(OffenceNoticeDto dto, OcmsValidOffenceNotice intranetNotice, EocmsValidOffenceNotice entity) {
        log.info("Mapping DTO to EocmsValidOffenceNotice for internet database");

        // Copy from saved intranet notice (source of truth)
        entity.setNoticeNo(intranetNotice.getNoticeNo());
        entity.setVehicleNo(intranetNotice.getVehicleNo());
        entity.setAnFlag(intranetNotice.getAnFlag());
        entity.setNoticeDateAndTime(intranetNotice.getNoticeDateAndTime());
        entity.setAmountPayable(intranetNotice.getAmountPayable());
        entity.setPpCode(intranetNotice.getPpCode());
        entity.setPpName(intranetNotice.getPpName());
        entity.setLastProcessingStage(intranetNotice.getLastProcessingStage());
        entity.setNextProcessingStage(intranetNotice.getNextProcessingStage());
        entity.setVehicleRegistrationType(intranetNotice.getVehicleRegistrationType());
        entity.setSuspensionType(intranetNotice.getSuspensionType());
        entity.setCrsReasonOfSuspension(intranetNotice.getCrsReasonOfSuspension());
        entity.setCrsDateOfSuspension(intranetNotice.getCrsDateOfSuspension());
        entity.setEprReasonOfSuspension(intranetNotice.getEprReasonOfSuspension());
        entity.setEprDateOfSuspension(intranetNotice.getEprDateOfSuspension());
        entity.setOffenceNoticeType(intranetNotice.getOffenceNoticeType());

        // Copy payment and eservice fields from intranet notice (already set there)
        entity.setPaymentAcceptanceAllowed(intranetNotice.getPaymentAcceptanceAllowed());
        entity.setPaymentStatus(intranetNotice.getPaymentStatus());
        entity.setEserviceMessageCode(intranetNotice.getEserviceMessageCode());

        // Set creation audit fields
        entity.setCreDate(LocalDateTime.now());
        entity.setCreUserId(dto.getCreUserId());

        // Set is_sync flag to "Y" (record is synced)
        entity.setIsSync("Y");

        log.info("Successfully mapped to EocmsValidOffenceNotice for notice: {}", entity.getNoticeNo());
    }
}