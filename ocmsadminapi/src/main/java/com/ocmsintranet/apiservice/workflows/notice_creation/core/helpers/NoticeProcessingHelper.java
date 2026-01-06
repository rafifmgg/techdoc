package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetail;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetailService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDateTime;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.sequence.SequenceService;
import com.ocmsintranet.apiservice.crud.exception.ErrorCodes;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@Slf4j
public class NoticeProcessingHelper {
    
    @Autowired
    private SequenceService sequenceService;


    @Autowired
    private OcmsOffenceNoticeOwnerDriverService ocmsOffenceNoticeOwnerDriverService;
    
    @Autowired
    private OcmsOffenceNoticeOwnerDriverAddrService offenceNoticeAddressService;

    @Autowired
    private OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    @Autowired
    private OcmsOffenceNoticeDetailService ocmsOffenceNoticeDetailService;


    private OcmsValidOffenceNotice ocmsValidOffenceNotice;

    private OcmsOffenceNoticeDetail ocmsOffenceNoticeDetail;

    @Autowired
    private OcmsValidOffenceNoticeRepository ocmsValidOffenceNoticeRepository;

    @Autowired
    private NoticeRecordHelper noticeRecordHelper;



    /**
     * Check ANS eligibility for EHT subsystem
     * @param data The data map containing the DTO
     * @return true if eligible for ANS, false otherwise
     */
    public boolean checkANSForEHT(Map<String, Object> data) {
        log.info("ANS check for EHT executed");
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.warn("No DTO found in data map for EHT ANS check");
                return false;
            }
            
            String anFlag = dto.getAnFlag();
            if (anFlag == null) {
                log.info("ANS flag is null for EHT, not eligible for PS-ANS");
                return false;
            }
            
            // Check if anFlag indicates eligibility ("Y" means eligible)
            boolean isEligible = "Y".equalsIgnoreCase(anFlag);
            log.info("EHT ANS flag value: {}, eligible for PS-ANS: {}", anFlag, isEligible);

            String vehicleNo = dto.getVehicleNo();
            if (vehicleNo == null) {
                log.info("Vehicle No is null for EHT, not eligible for PS-ANS");
                return false;
            }


            String offenceNoticeType = dto.getOffenceNoticeType();
            log.info("check offenceNoticeType is exist" +offenceNoticeType);
            if (offenceNoticeType == null || "0".equals(offenceNoticeType)) {
                log.info("Offence Notice type  is null , not eligible for PS-ANS");
                return false;
            }

            //this validation is commented for next epic
//            int hasRecentOffence = ocmsValidOffenceNoticeRepository.existsByVehicleNoAndNoticeDateTimeWithinLast2Years(vehicleNo);
//            if (hasRecentOffence > 0) {
//                log.info("Notice {} has a recent offence within last 2 years → NOT eligible for PS-ANS", vehicleNo);
//                return false;
//            }
            return isEligible;
        } catch (Exception e) {
            log.error("Error checking EHT ANS eligibility: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check ANS eligibility for other subsystems based on complex flow
     * @param data The data map containing the DTO
     * @return true if eligible for ANS, false otherwise
     */
    public boolean checkANSForOthers(Map<String, Object> data) {
        log.info("ANS check for other subsystems executed");
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.warn("No DTO found in data map for non-EHT ANS check");
                return false;
            }
            
            Integer computerRuleCode = dto.getComputerRuleCode();
            String vehicleNo = dto.getVehicleNo();
            
            if (computerRuleCode == null) {
                log.warn("Missing computer rule code for ANS check");
                return false;
            }
            
            // Vehicle number can be null for foreign vehicles (type F)
            // If vehicle number is null, ANS check is not applicable
            if (vehicleNo == null ||
                SystemConstant.VehicleNumber.UNLICENSED_PARKING.equals(vehicleNo)) {
                log.info("Vehicle number is null or {}, ANS check not applicable",
                    SystemConstant.VehicleNumber.UNLICENSED_PARKING);
                return false;
            }
            
            // Step 1: Query ANS Exemption Rules DB using New Notice's Computer Rule Code
            // TODO: Implement query to ANS Exemption Rules
            boolean isExemptFromANS = false; // Placeholder - should query from database
            log.info("Rule code {} exempt from ANS: {}", computerRuleCode, isExemptFromANS);
            
            // If rule code is exempt from ANS, return false (not eligible for ANS)
            if (isExemptFromANS) {
                return false;
            }
            
            // Step 2: Query VON using vehicle number for notices in past 24 months
            // TODO: Implement query to find notices from past 24 months
            // TODO: When implementing the actual query, use LocalDateTime.now().minusMonths(24) to calculate the date range
            
            // Placeholder for query to find past notices
            boolean hasNoticesInPast24Months = false; // Should query from database
            log.info("Vehicle {} has notices in past 24 months: {}", vehicleNo, hasNoticesInPast24Months);
            
            // If no notices in past 24 months, return false (not eligible for ANS)
            if (!hasNoticesInPast24Months) {
                return false;
            }
            
            // Step 3: Process past notices
            // TODO: Implement processing of past notices
            boolean isPastNoticeSuspensionTypePS = false; // Placeholder - should check past notices
            
            // If past notice's suspension type is not PS, return false
            if (!isPastNoticeSuspensionTypePS) {
                return false;
            }
            
            // Step 4: Query Standard Code (Ref Code = ANS PS Reason)
            // TODO: Implement query for ANS PS Reason codes
            boolean isPastNoticeReasonInANSReasonList = false; // Placeholder - should query from database
            
            // If past notice's reason not in ANS reason list, return false
            if (!isPastNoticeReasonInANSReasonList) {
                return false;
            }
            
            // Step 5: Check if PS ANS Reason status is active
            // TODO: Implement check for active status
            boolean isPSANSReasonActive = false; // Placeholder - should check status
            
            // If PS ANS Reason is not active, check more past notices if available
            if (!isPSANSReasonActive) {
                // TODO: Check if more past notices to process
                boolean hasMorePastNoticesToProcess = false; // Placeholder
                
                // If more past notices to process, return to Step 3
                if (hasMorePastNoticesToProcess) {
                    // This would be a recursive call or loop in the actual implementation
                    log.info("More past notices to process, continuing check");
                    // For now, just return false as placeholder
                    return false;
                } else {
                    // No more past notices and current one doesn't qualify
                    return false;
                }
            }
            
            // If we've reached here, the notice qualifies for ANS
            log.info("Notice qualifies for PS-ANS");
            return true;
            
        } catch (Exception e) {
            log.error("Error checking non-EHT ANS eligibility: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process PS-ANS detection (Processing Stage - ANS)
     * Sets the appropriate fields in the DTO and creates a suspended notice record
     * As per flow diagram:
     * - If an_flag = Y: payment_acceptance_allowed = N, eservice_message_code = E5
     *
     * @param data Map containing the DTO and other processing data
     * @param ocmsSuspendedNoticeService Service for suspended notice operations
     * @param ocmsSuspensionReasonService Service for suspension reason operations
     */
    public void processPS_ANS(Map<String, Object> data,
                          SuspendedNoticeService ocmsSuspendedNoticeService,
                          OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        log.info("Processing PS-ANS (ANS Detection)");

        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for PS-ANS processing");
                return;
            }

            // Step 1: Set the required fields for PS-ANS in the DTO (valid offense notice table)
            LocalDateTime currentDate = LocalDateTime.now();

            // Set processing stages first (required for database NOT NULL constraint)
            dto.setPrevProcessingStage(null);
            dto.setPrevProcessingDate(null);
            dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            dto.setLastProcessingDate(currentDate);
            dto.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
            dto.setNextProcessingDate(currentDate);
            log.info("Set processing stages for ANS: Last=NPA, Next=ROV");

            // Then set suspension fields
            dto.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            dto.setEprReasonOfSuspension(SystemConstant.SuspensionReason.ANS);
            dto.setEprDateOfSuspension(currentDate);

            // Step 2: For ANS cases (an_flag = Y), set payment_acceptance_allowed = N and eservice_message_code = E5
            dto.setPaymentAcceptanceAllowed(SystemConstant.PaymentAcceptance.NOT_ALLOWED);
            dto.setEserviceMessageCode(SystemConstant.EServiceMessageCode.ANS);
            log.info("Set payment_acceptance_allowed = N and eservice_message_code = E5 for ANS case (an_flag = Y)");

            // Step 3: Create notice records using NoticeRecordHelper (same as non-ANS flow)
            String noticeNumber = dto.getNoticeNo();
            if (noticeNumber != null && !noticeNumber.isEmpty()) {
                log.info("Creating notice records for ANS case using NoticeRecordHelper: {}", noticeNumber);
                noticeRecordHelper.createNoticeRecord(data, ocmsValidOffenceNoticeService, ocmsOffenceNoticeDetailService);

                // Step 4: Create the suspended notice record for ANS
                log.info("Creating ANS suspended notice for: {}", noticeNumber);
                createANSSuspendedNotice(noticeNumber, currentDate, ocmsSuspendedNoticeService, ocmsSuspensionReasonService);
            }

            log.info("PS-ANS processing completed for notice: {} - payment_acceptance_allowed=N, eservice_message_code=E5", noticeNumber);
        } catch (Exception e) {
            log.error("Error processing PS-ANS: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a suspended notice record for ANS
     * 
     * @param noticeNumber The notice number
     * @param currentDate The current date/time
     * @param ocmsSuspendedNoticeService Service for suspended notice operations
     * @param ocmsSuspensionReasonService Service for suspension reason operations
     */
    private void createANSSuspendedNotice(String noticeNumber, LocalDateTime currentDate,
                                      SuspendedNoticeService ocmsSuspendedNoticeService,
                                      OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        log.info("Creating suspended notice record for ANS: {}", noticeNumber);
        
        try {
            // Create the suspended notice record
            SuspendedNotice suspendedNotice = new SuspendedNotice();

            // Get the next sequence number for sr_no
            Integer srNo = sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");

            // Set the fields for suspended notice
            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSrNo(srNo);
            suspendedNotice.setSuspensionSource(SystemConstant.Subsystem.OCMS_CODE);
            suspendedNotice.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            suspendedNotice.setReasonOfSuspension(SystemConstant.SuspensionReason.ANS);

            // All suspension types should not have revival dates
            suspendedNotice.setDateOfRevival(null);
            suspendedNotice.setDueDateOfRevival(null);

            suspendedNotice.setOfficerAuthorisingSupension(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            suspendedNotice.setSuspensionRemarks("ANS detected by system");

            // Save the suspended notice record
            ocmsSuspendedNoticeService.save(suspendedNotice);

            log.info("Successfully created suspended notice record for ANS: {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error creating suspended notice record for ANS: {}", e.getMessage(), e);
        }
    }

    /**
     * Set processing stages for X-type vehicles (all offense type U)
     */
    public void setProcessingStagesForX(Map<String, Object> data) {
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for setting X-type processing stages");
                return;
            }
            
            LocalDateTime currentDate = LocalDateTime.now();
            
            dto.setPrevProcessingStage(null);
            dto.setPrevProcessingDate(null);
            dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            dto.setLastProcessingDate(currentDate);
            dto.setNextProcessingStage(SystemConstant.ProcessingStage.DEMAND_NOTE_1);
            dto.setNextProcessingDate(currentDate);
            
            log.info("Set processing stages for X-type vehicle - Next stage: DN1");
        } catch (Exception e) {
            log.error("Error setting processing stages for X-type: {}", e.getMessage(), e);
        }
    }

    /**
     * Set processing stages for V/S/D-type vehicles
     * As per flow diagram:
     * - First step: Set payment_acceptance_allowed = Y
     * - Then: Set eservice_message_code = E1 for local vehicles (S, V, D types)
     */
    public void setProcessingStagesForVSD(Map<String, Object> data) {
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for setting V/S/D-type processing stages");
                return;
            }

            LocalDateTime currentDate = LocalDateTime.now();

            // Step 1: Set payment_acceptance_allowed = Y (first step in create notice flow)
            dto.setPaymentAcceptanceAllowed(SystemConstant.PaymentAcceptance.ALLOWED);
            log.info("Set payment_acceptance_allowed = Y (first step)");

            // Step 2: Set processing stages
            dto.setPrevProcessingStage(null);
            dto.setPrevProcessingDate(null);
            dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            dto.setLastProcessingDate(currentDate);
            dto.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
            dto.setNextProcessingDate(currentDate);

            // Step 3: Set eservice_message_code = E1 (payment allowed)
            dto.setEserviceMessageCode(SystemConstant.EServiceMessageCode.ALLOW_PAYMENT);
            log.info("Set eservice_message_code = E1 (payment allowed)");

            log.info("Set processing stages for V/S/D-type vehicle - payment_acceptance_allowed=Y, eservice_message_code=E1");
        } catch (Exception e) {
            log.error("Error setting processing stages for V/S/D-type: {}", e.getMessage(), e);
        }
    }

    /**
     * PHASE 1: Set foreign vehicle processing settings (Step 5a)
     * Sets DTO fields ONLY - does NOT create notice or suspension
     * Extracted from processForeignVehicle to allow notice creation in between
     *
     * @param data Map containing the DTO and other processing data
     */
    public void setForeignVehicleSettings(Map<String, Object> data) {
        log.info("Setting foreign vehicle processing settings (Phase 1 - settings only)");

        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for foreign vehicle settings");
                return;
            }

            LocalDateTime currentDate = LocalDateTime.now();

            // Step 1: Set payment_acceptance_allowed = Y
            dto.setPaymentAcceptanceAllowed(SystemConstant.PaymentAcceptance.ALLOWED);
            log.info("Set payment_acceptance_allowed = Y (first step)");

            // Step 2: Set processing stages for foreign vehicles
            dto.setPrevProcessingStage(null);
            dto.setPrevProcessingDate(null);
            dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            dto.setLastProcessingDate(currentDate);
            dto.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
            dto.setNextProcessingDate(currentDate);

            // Step 3: Apply PS-FOR suspension fields (for later suspension creation)
            dto.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            dto.setEprReasonOfSuspension(SystemConstant.SuspensionReason.FOREIGN);
            dto.setEprDateOfSuspension(currentDate);

            // Step 4: Set eservice_message_code = E6 for foreign vehicles
            dto.setEserviceMessageCode(SystemConstant.EServiceMessageCode.FOREIGN_VEHICLE);
            log.info("Set eservice_message_code = E6 for foreign vehicle");

            // Mark that PS-FOR was detected (for later suspension creation)
            data.put("psForDetected", true);

            log.info("Foreign vehicle settings applied - payment_acceptance_allowed=Y, eservice_message_code=E6, PS-FOR fields set");
        } catch (Exception e) {
            log.error("Error setting foreign vehicle settings: {}", e.getMessage(), e);
        }
    }

    /**
     * Process foreign vehicle (type F)
     * As per flow diagram:
     * - First step: Set payment_acceptance_allowed = Y
     * - Then: Set eservice_message_code = E6 for foreign vehicles
     */
    public void processForeignVehicle(Map<String, Object> data,
                                        SuspendedNoticeService ocmsSuspendedNoticeService,
                                        OcmsSuspensionReasonService ocmsSuspensionReasonService) {
            log.info("Processing foreign vehicle (type F)");

            try {
                OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
                if (dto == null) {
                    log.error("No DTO found for foreign vehicle processing");
                    return;
                }

                LocalDateTime currentDate = LocalDateTime.now();

                // Step 1: Set payment_acceptance_allowed = Y (first step in create notice flow)
                dto.setPaymentAcceptanceAllowed(SystemConstant.PaymentAcceptance.ALLOWED);
                log.info("Set payment_acceptance_allowed = Y (first step)");

                // Step 2: Set processing stages for foreign vehicles in the valid offense notice table
                dto.setPrevProcessingStage(null);
                dto.setPrevProcessingDate(null);
                dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
                dto.setLastProcessingDate(currentDate);
                dto.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
                dto.setNextProcessingDate(currentDate);

                // Step 3: Apply PS-FOR suspension fields in the valid offense notice table
                dto.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
                dto.setEprReasonOfSuspension(SystemConstant.SuspensionReason.FOREIGN);
                dto.setEprDateOfSuspension(currentDate);

                // Step 4: Set eservice_message_code = E6 for foreign vehicles
                dto.setEserviceMessageCode(SystemConstant.EServiceMessageCode.FOREIGN_VEHICLE);
                log.info("Set eservice_message_code = E6 for foreign vehicle");

                // Mark that PS-FOR was detected
                data.put("psForDetected", true);

                // Step 5: Create notice at NPA for foreign vehicles
                String noticeNumber = dto.getNoticeNo();
                if (noticeNumber != null && !noticeNumber.isEmpty()) {
                    log.info("Creating notice at NPA for foreign vehicle: {}", noticeNumber);

                    // Step 6: Create the suspended notice record for foreign vehicle
                    createForeignVehicleSuspendedNotice(noticeNumber, currentDate, ocmsSuspendedNoticeService, ocmsSuspensionReasonService);
                }

                log.info("Foreign vehicle processing completed - payment_acceptance_allowed=Y, eservice_message_code=E6, PS-FOR applied");
            } catch (Exception e) {
                log.error("Error processing foreign vehicle: {}", e.getMessage(), e);
            }
        }

    /**
     * Create a suspended notice record for a foreign vehicle
     * 
     * @param noticeNumber The notice number
     * @param currentDate The current date/time
     * @param ocmsSuspendedNoticeService Service for suspended notice operations
     * @param ocmsSuspensionReasonService Service for suspension reason operations
     */
    private void createForeignVehicleSuspendedNotice(String noticeNumber, 
                                                 LocalDateTime currentDate,
                                                 SuspendedNoticeService ocmsSuspendedNoticeService,
                                                 OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        log.info("Creating suspended notice record for foreign vehicle: {}", noticeNumber);
        
        try {
            // Create the suspended notice record
            SuspendedNotice suspendedNotice = new SuspendedNotice();
            
            // Get the next sequence number for sr_no
            Integer srNo = sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");
            
            // Set the fields for suspended notice
            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSrNo(srNo);
            suspendedNotice.setSuspensionSource(SystemConstant.Subsystem.OCMS_CODE);
            suspendedNotice.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            suspendedNotice.setReasonOfSuspension(SystemConstant.SuspensionReason.FOREIGN);
            
            // Foreign vehicles should not have revival dates
            suspendedNotice.setDateOfRevival(null);
            suspendedNotice.setDueDateOfRevival(null);
            
            suspendedNotice.setOfficerAuthorisingSupension(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            suspendedNotice.setSuspensionRemarks("Foreign vehicle detected by system");
            
            // Save the record
            ocmsSuspendedNoticeService.save(suspendedNotice);
            
            log.info("Successfully created suspended notice record for foreign vehicle: {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error creating suspended notice record for foreign vehicle: {}", e.getMessage(), e);
        }
    }
        
    /**
     * Process military vehicle (type I) with MID handling
     */
    public void processMilitaryVehicle(Map<String, Object> data) {
        log.info("Processing military vehicle (type I)");
        
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for military vehicle processing");
                return;
            }
            
            LocalDateTime currentDate = LocalDateTime.now();
            
            // Step 1: Set processing stages to NPA → RD1
            dto.setPrevProcessingStage(null);
            dto.setPrevProcessingDate(null);
            dto.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            dto.setLastProcessingDate(currentDate);
            dto.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_DOCUMENT_1);
            dto.setNextProcessingDate(currentDate);
            
            log.info("Set processing stages for military vehicle: Last=NPA, Next=RD1");
            
            // Step 2: Mark that this is a military vehicle for later address population
            data.put("isMilitaryVehicle", true);
            
            log.info("Military vehicle processing completed - Next stage: RD1");
        } catch (Exception e) {
            log.error("Error processing military vehicle: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a duplicate notice by creating notice and suspended notice records
     */
    public String processDuplicateNotice(Map<String, Object> data, 
                                       OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService,
                                       SuspendedNoticeService ocmsSuspendedNoticeService,
                                       OcmsSuspensionReasonService ocmsSuspensionReasonService,
                                       NoticeNumberHelper noticeNumberHelper,
                                       NoticeRecordHelper noticeRecordHelper) {
        log.info("Processing duplicate notice");
        
        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for duplicate notice processing");
                throw new RuntimeException("Missing DTO for duplicate notice processing");
            }
            
            // Step 1: Generate a new notice number
            String noticeNumber = noticeNumberHelper.processNoticeNumber(data);
            dto.setNoticeNo(noticeNumber);
            
            // Step 2: Handle vehicle number for duplicate notices
            String vehicleNo = dto.getVehicleNo();
            if (vehicleNo == null || vehicleNo.isEmpty() || vehicleNo.trim().isEmpty()) {
                log.info("Empty vehicle number detected in duplicate notice, keeping it empty");
                // Keep vehicle number as empty - don't change to N.A.
            } else {
                log.info("Using vehicle number '{}' for duplicate notice", vehicleNo);
            }
            
            // For duplicate notices, do not set vehicle registration type at all
            log.info("Not setting vehicle registration type for duplicate notice as per requirements");
            // Clear any previously set vehicle registration type
            dto.setVehicleRegistrationType(null);
            
            // Step 3: Set the required fields for duplicate notice
            LocalDateTime currentDate = LocalDateTime.now();
            dto.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            dto.setEprReasonOfSuspension(SystemConstant.SuspensionReason.DUPLICATE);
            
            // Step 4: Create the valid offence notice record FIRST
            log.info("Creating valid offence notice record for duplicate notice: {}", noticeNumber);
            noticeRecordHelper.createNoticeRecord(data, ocmsValidOffenceNoticeService, null);
            
            // Step 5: Create the suspended notice record AFTER the valid offence notice
            log.info("Creating suspended notice record for duplicate notice: {}", noticeNumber);
            SuspendedNotice suspendedNotice = new SuspendedNotice();
            
            // Get the next sequence number for sr_no
            Integer srNo = sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");
            
            // Set the fields for suspended notice
            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSrNo(srNo);
            suspendedNotice.setSuspensionSource(SystemConstant.Subsystem.OCMS_CODE);
            suspendedNotice.setSuspensionType(SystemConstant.SuspensionType.PERMANENT);
            suspendedNotice.setReasonOfSuspension(SystemConstant.SuspensionReason.DUPLICATE);
            
            // All suspension types should not have revival dates
            suspendedNotice.setDateOfRevival(null);
            suspendedNotice.setDueDateOfRevival(null);
            
            suspendedNotice.setOfficerAuthorisingSupension(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            suspendedNotice.setSuspensionRemarks("Duplicate offense detected by system");
            
            // Save the suspended notice record AFTER the valid offence notice is created
            ocmsSuspendedNoticeService.save(suspendedNotice);
            
            log.info("Successfully processed duplicate notice: {}", noticeNumber);
            return noticeNumber;
            
        } catch (Exception e) {
            log.error("Error processing duplicate notice: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process duplicate notice", e);
        }
    }

    /**
     * Populate address for military vehicles in the owner/driver record
     * For military vehicles (Type 'I'), auto-populate vehicle owner as Ministry of Defence
     */
    public void populateAddressForMilitary(Map<String, Object> data) {
        log.info("Populating address for military vehicle in owner/driver record");
        
        try {
            // Check if this is actually a military vehicle
            Boolean isMilitaryVehicle = (Boolean) data.get("isMilitaryVehicle");
            if (isMilitaryVehicle == null || !isMilitaryVehicle) {
                log.info("Not a military vehicle, skipping Ministry of Defence address population");
                return;
            }
            
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for military address population");
                return;
            }
            
            String noticeNumber = dto.getNoticeNo();
            if (noticeNumber == null || noticeNumber.isEmpty()) {
                log.error("No notice number available for military address population");
                return;
            }
            
            log.info("Creating owner record for military vehicle notice: {} with Ministry of Defence address", noticeNumber);
            
            // Create a new owner record for the military vehicle
            OcmsOffenceNoticeOwnerDriver ownerRecord = new OcmsOffenceNoticeOwnerDriver();
            
            // Set composite key
            ownerRecord.setNoticeNo(noticeNumber);
            ownerRecord.setOwnerDriverIndicator(SystemConstant.Military.Owner.DRIVER_INDICATOR); // Owner
            
            // Set Ministry of Defence address fields from centralized constants
            ownerRecord.setName(SystemConstant.Military.Owner.NAME);
            ownerRecord.setIdType(SystemConstant.Military.Owner.ID_TYPE);
            ownerRecord.setIdNo(SystemConstant.Military.Owner.ID_NO); 
            ownerRecord.setOffenderIndicator(SystemConstant.Military.Owner.OFFENDER_INDICATOR);
                        
            // Set creation/update fields
            ownerRecord.setCreUserId(dto.getCreUserId() != null ? dto.getCreUserId() : SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            // Save the owner record first
            ocmsOffenceNoticeOwnerDriverService.save(ownerRecord);
            
            // Create a new address record for the registration address
            OcmsOffenceNoticeOwnerDriverAddr regAddress = new OcmsOffenceNoticeOwnerDriverAddr();
            regAddress.setNoticeNo(noticeNumber);
            regAddress.setOwnerDriverIndicator(SystemConstant.Military.Owner.DRIVER_INDICATOR); // Owner
            regAddress.setTypeOfAddress(SystemConstant.Military.Address.TYPE);
            
            // Set Ministry of Defence address fields from centralized constants
            regAddress.setBlkHseNo(SystemConstant.Military.Address.BLK_HSE_NO);
            regAddress.setStreetName(SystemConstant.Military.Address.STREET_NAME);
            regAddress.setBldgName(SystemConstant.Military.Address.BLDG_NAME);
            regAddress.setPostalCode(SystemConstant.Military.Address.POSTAL_CODE);
            
            // Set creation fields
            // regAddress.setEffectiveDate(LocalDateTime.now());
            regAddress.setProcessingDateTime(LocalDateTime.now());
            regAddress.setCreDate(LocalDateTime.now());
            regAddress.setCreUserId(dto.getCreUserId() != null ? dto.getCreUserId() : SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            
            // Save the address record
            offenceNoticeAddressService.save(regAddress);
            
            log.info("Ministry of Defence owner record and address successfully created for military vehicle notice: {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error populating address for military vehicle: {}", e.getMessage(), e);
        }
    }

    // First implementation of prepareSuccessAndFailureResponse removed to avoid duplication
    // The complete implementation is available below

    /**
     * Determine the appropriate error code based on the error message
     * Currently unused but kept for potential future error handling enhancements
     * @param errorMsg The error message
     * @return The appropriate ErrorCodes enum value
     */
    private ErrorCodes determineErrorCode(String errorMsg) {
        if (errorMsg == null) {
            return ErrorCodes.INTERNAL_SERVER_ERROR;
        }
        
        String lowerCaseMsg = errorMsg.toLowerCase();
        
        if (lowerCaseMsg.contains("duplicate")) {
            return ErrorCodes.BAD_REQUEST;
        } else if (lowerCaseMsg.contains("not found") || lowerCaseMsg.contains("missing")) {
            return ErrorCodes.NOT_FOUND;
        } else if (lowerCaseMsg.contains("permission") || lowerCaseMsg.contains("access")) {
            return ErrorCodes.FORBIDDEN;
        } else if (lowerCaseMsg.contains("timeout") || lowerCaseMsg.contains("timed out")) {
            return ErrorCodes.REQUEST_TIMEOUT;
        } else if (lowerCaseMsg.contains("database") || lowerCaseMsg.contains("db")) {
            return ErrorCodes.DATABASE_CONNECTION_FAILED;
        } else if (lowerCaseMsg.contains("unauthorized") || lowerCaseMsg.contains("login")) {
            return ErrorCodes.UNAUTHORIZED;
        } else if (lowerCaseMsg.contains("validation") || lowerCaseMsg.contains("invalid")) {
            return ErrorCodes.BAD_REQUEST;
        } else {
            return ErrorCodes.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Prepare response with successful, failed, and duplicate notices
     * @param processedNoticeNumbers List of successfully processed notice numbers
     * @param failedNotices Map of failed notices with error messages
     * @param duplicateNotices List of duplicate notice numbers
     * @return NoticeResponse object with appropriate status and messages
     */
    public NoticeResponse prepareSuccessAndFailureResponse(List<String> processedNoticeNumbers,
                                                   Map<String, Object> failedNotices,
                                                   List<String> duplicateNotices) {
        // Create response
        NoticeResponse response = new NoticeResponse();

        // Calculate total notices
        int totalProcessed = processedNoticeNumbers.size();
        int totalFailed = failedNotices.size();
        int totalDuplicates = (duplicateNotices != null) ? duplicateNotices.size() : 0;
        // Total notices should be the sum of processed and failed (failed already includes duplicates)
        int totalNotices = totalProcessed + totalFailed;
        
        // Format duplicate notices if any
        String duplicateNoticesStr = null;
        if (duplicateNotices != null && !duplicateNotices.isEmpty()) {
            duplicateNoticesStr = String.join("; ", duplicateNotices);
        }
        
        // Format failed notices if any
        String failedNoticesStr = null;
        if (!failedNotices.isEmpty()) {
            StringBuilder failedInfo = new StringBuilder();
            failedNotices.forEach((noticeNo, errorMsg) -> {
                if (failedInfo.length() > 0) {
                    failedInfo.append("; ");
                }
                failedInfo.append(noticeNo).append(": ").append(errorMsg);
            });
            failedNoticesStr = failedInfo.toString();
        }
        
        // Case 1: Single notice success
        if (totalNotices == 1 && processedNoticeNumbers.size() == 1 && failedNotices.isEmpty()) {
            response.createSingleNoticeSuccessResponse(processedNoticeNumbers.get(0));
            return response;
        }
        
        // Case 2: Single notice failed - but not for duplicates when we have multiple notices
        if (totalNotices == 1 && processedNoticeNumbers.isEmpty() && failedNotices.size() == 1 && 
            (duplicateNotices == null || duplicateNotices.size() <= 1)) {
            String errorKey = failedNotices.keySet().iterator().next();
            String errorMsg = (String) failedNotices.get(errorKey);
            
            // Use OCMS-4000 for failed notices
            response.createSingleNoticeFailureResponse(ErrorCodes.BAD_REQUEST, errorMsg);
            response.getData().setAppCode("OCMS-4000");
            return response;
        }
        
        // Case 3: Multiple notices - all success
        if (totalNotices > 1 && failedNotices.isEmpty() && (duplicateNotices == null || duplicateNotices.isEmpty())) {
            response.createMultipleNoticeAllSuccessResponse(
                processedNoticeNumbers, 
                String.valueOf(totalNotices)
            );
            return response;
        }
        
        // Case 4: Multiple notices - some failed
        if (totalNotices > 1 && processedNoticeNumbers.size() > 0 && failedNotices.size() > 0) {
            response.createMultipleNoticePartialSuccessResponse(
                processedNoticeNumbers,
                failedNoticesStr,
                totalDuplicates > 0 ? String.valueOf(totalDuplicates) : null,
                duplicateNoticesStr,
                String.valueOf(totalNotices)
            );
            // Ensure the app code is OCMS-2006
            response.getData().setAppCode("OCMS-2006");
            return response;
        }
        
        // Case 5: Multiple notices - all failed (including all duplicates)
        if (totalNotices > 0 && processedNoticeNumbers.isEmpty()) {
            response.createMultipleNoticeAllFailedResponse(
                ErrorCodes.BAD_REQUEST,
                "Failed to create notices",
                "0",
                String.valueOf(totalFailed),
                failedNoticesStr,
                totalDuplicates > 0 ? String.valueOf(totalDuplicates) : null,
                duplicateNoticesStr,
                String.valueOf(totalNotices)
            );
            // Ensure the app code is OCMS-4000
            response.getData().setAppCode("OCMS-4000");
            return response;
        }
        
        // Default fallback
        return response;
    }

    private OcmsValidOffenceNotice mapToOcmsValidOffenceNotice(OffenceNoticeDto dto) {
        OcmsValidOffenceNotice entity = new OcmsValidOffenceNotice();

        // Required fields (non-nullable in DB)
        entity.setNoticeNo(dto.getNoticeNo());
        log.info("check amount" + dto.getCompositionAmount());
        entity.setCompositionAmount(dto.getCompositionAmount() != null ?
                dto.getCompositionAmount() : null);
        entity.setComputerRuleCode(dto.getComputerRuleCode());
        entity.setLastProcessingDate(dto.getLastProcessingDate() != null ?
                dto.getLastProcessingDate() : LocalDateTime.now()); // fallback
        entity.setNoticeDateAndTime(dto.getNoticeDateAndTime());
        entity.setOffenceNoticeType(dto.getOffenceNoticeType());
        entity.setPpCode(dto.getPpCode());
        entity.setPpName(dto.getPpName());
        entity.setVehicleCategory(dto.getVehicleCategory());
        entity.setVehicleNo(dto.getVehicleNo());

        // Optional fields
        entity.setParkingLotNo(dto.getParkingLotNo());
        entity.setPrevProcessingDate(dto.getPrevProcessingDate());
        entity.setPrevProcessingStage(dto.getPrevProcessingStage());
        entity.setSuspensionType(dto.getSuspensionType());
        entity.setVehicleRegistrationType(dto.getVehicleRegistrationType());
        entity.setSubsystemLabel(dto.getSubsystemLabel());
        entity.setWardenNo(dto.getWardenNo());
        entity.setRepChargeAmount(dto.getRepChargeAmount() != null ?
                dto.getRepChargeAmount(): null);
        entity.setAdministrationFee(dto.getAdministrationFee() != null ?
                dto.getAdministrationFee() : null);
        entity.setAmountPaid(dto.getAmountPaid() != null ?
                dto.getAmountPaid() : null);
        entity.setAmountPayable(dto.getCompositionAmount() != null ?
                dto.getCompositionAmount() : null);
        entity.setPaymentDueDate(dto.getPaymentDueDate() != null ?
                dto.getPaymentDueDate() : null);
        entity.setAnFlag(dto.getAnFlag());
        entity.setOtherRemark(dto.getOtherRemark());
        entity.setCrsDateOfSuspension(dto.getCrsDateOfSuspension());
        entity.setCrsReasonOfSuspension(dto.getCrsReasonOfSuspension());
        entity.setDueDateOfRevival(dto.getDueDateOfRevival());
        entity.setEprDateOfSuspension(dto.getEprDateOfSuspension());
        entity.setEprReasonOfSuspension(dto.getEprReasonOfSuspension());
        entity.setNextProcessingDate(dto.getNextProcessingDate() !=null ? dto.getNextProcessingDate() : LocalDateTime.now());

        // Set processing stages based on conditions
        if ("O".equals(dto.getOffenceNoticeType()) &&
                (dto.getVehicleRegistrationType() != null && ("V".equals(dto.getVehicleRegistrationType()) || "S".equals(dto.getVehicleRegistrationType()) || "D".equals(dto.getVehicleRegistrationType())))) {
            entity.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
            entity.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
        }

        // Fallback: Ensure lastProcessingStage is never null (safety net for edge cases)
        if (entity.getLastProcessingStage() == null) {
            // Use DTO value if available
            if (dto.getLastProcessingStage() != null) {
                entity.setLastProcessingStage(dto.getLastProcessingStage());
                log.debug("Set lastProcessingStage from DTO: {}", dto.getLastProcessingStage());
            } else {
                // Final fallback to prevent NOT NULL constraint violation
                entity.setLastProcessingStage(SystemConstant.ProcessingStage.NEW_PROCESSING_APPLICATION);
                log.warn("lastProcessingStage was null, set default: NPA for offenceType={}, vehicleType={}",
                    dto.getOffenceNoticeType(), dto.getVehicleRegistrationType());
            }
        }

        // Fallback: Ensure nextProcessingStage is never null
        if (entity.getNextProcessingStage() == null) {
            // Use DTO value if available
            if (dto.getNextProcessingStage() != null) {
                entity.setNextProcessingStage(dto.getNextProcessingStage());
                log.debug("Set nextProcessingStage from DTO: {}", dto.getNextProcessingStage());
            } else {
                // Determine based on vehicle type
                if ("X".equals(dto.getVehicleRegistrationType())) {
                    entity.setNextProcessingStage(SystemConstant.ProcessingStage.DEMAND_NOTE_1);
                    log.debug("Set nextProcessingStage for X-type: DN1");
                } else {
                    entity.setNextProcessingStage(SystemConstant.ProcessingStage.REGISTRATION_OF_VEHICLE);
                    log.debug("Set nextProcessingStage default: ROV");
                }
            }
        }

        // Note: Fields like paymentDueDate, parkingFee, etc. are commented out in entity → skip them

        return entity;
    }

    private OcmsOffenceNoticeDetail mapToOcmsOffenceNoticeDetail(OffenceNoticeDto dto) {
        OcmsOffenceNoticeDetail detail = new OcmsOffenceNoticeDetail();

        // Primary key
        detail.setNoticeNo(dto.getNoticeNo());

        // Basic fields
        detail.setColorOfVehicle(dto.getColorOfVehicle());
        // detail.setComments(dto.getComments());
        detail.setConditionInvalidCoupon1(dto.getConditionInvalidCoupon1());
        detail.setConditionInvalidCoupon2(dto.getConditionInvalidCoupon2());
        detail.setCreatedDt(dto.getCreatedDt()); // if null, may default to now()

        // Coupon denomination fields (convert Double → BigDecimal if needed)
        detail.setDenomInvalidCoupon1(dto.getDenomInvalidCoupon1() != null ?
                dto.getDenomInvalidCoupon1() : null);
        detail.setDenomInvalidCoupon2(dto.getDenomInvalidCoupon2() != null ?
                dto.getDenomInvalidCoupon2() : null);
        detail.setDenomOfValidCoupon(dto.getDenomOfValidCoupon() != null ?
                dto.getDenomOfValidCoupon() : null);

        // Expiry & timing
        detail.setExpiryTime(dto.getExpiryTime());

        // Media IDs
        detail.setImageId(dto.getImageId());
        detail.setVideoId(dto.getVideoId());

        // Invalid coupon numbers
        detail.setInvalidCouponNo1(dto.getInvalidCouponNo1());
        detail.setInvalidCouponNo2(dto.getInvalidCouponNo2());
        //detail.setInvalidCouponNo3(dto.getInvalidCouponNo3()); // if exists in DTO
        detail.setConditionInvalidCoupon3(dto.getConditionInvalidCoupon2()); // note: field name mismatch?

        // IU / OBU
        detail.setIuNo(dto.getIuNo());
        detail.setRepObuLatitude(dto.getRepObuLatitude());
        detail.setRepObuLongitude(dto.getRepObuLongitude());
        detail.setRepOperatorId(dto.getRepOperatorId());

        // Parking timestamps
        detail.setRepParkingEntryDt(dto.getRepParkingEntryDt());
        detail.setRepParkingStartDt(dto.getRepParkingStartDt());
        detail.setRepParkingEndDt(dto.getRepParkingEndDt());
        detail.setRepParkingExitDt(dto.getRepParkingExitDt());

        // LTA vehicle info
        detail.setLtaChassisNumber(dto.getLtaChassisNumber());
        detail.setLtaDeregistrationDate(dto.getLtaDeregistrationDate());
        detail.setLtaDiplomaticFlag(dto.getLtaDiplomaticFlag());
        detail.setLtaEffOwnershipDate(dto.getLtaEffOwnershipDate());
        detail.setLtaMakeDescription(dto.getLtaMakeDescription());
        detail.setLtaMaxLadenWeight(dto.getLtaMaxLadenWeight());
        detail.setLtaPrimaryColour(dto.getLtaPrimaryColour());
        detail.setLtaRoadTaxExpiryDate(dto.getLtaRoadTaxExpiryDate());
        detail.setLtaSecondaryColour(dto.getLtaSecondaryColour());
        detail.setLtaUnladenWeight(dto.getLtaUnladenWeight());

        // Other fields
        detail.setLastValidCouponNo(dto.getLastValidCouponNo());
        detail.setRoadTaxExpiryDate(dto.getRoadTaxExpiryDate());
        detail.setRuleRemark1(dto.getRuleRemark1());
        detail.setRuleRemark2(dto.getRuleRemark2());
        detail.setSysNonPrintedComment(dto.getSysNonPrintedComment());
        detail.setUserNonPrintedComment(dto.getUserNonPrintedComment());
        detail.setTotalCouponsDisplayed(dto.getTotalCouponsDisplayed());
        detail.setTransactionId(dto.getTransactionId());
        detail.setVehicleMake(dto.getVehicleMake());
        detail.setRepViolationCode(dto.getRepViolationCode());

        // Note: lifeStatus — if needed, set to current time or from DTO
        // detail.setLifeStatus(LocalDateTime.now());

        return detail;
    }
}