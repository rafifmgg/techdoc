package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.helpers;

import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveNRICService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveFINService;
import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service.DataHiveUENService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.upload.models.ToppanStage;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.ParameterRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.ParameterId;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticulars;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticularsService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTempService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsStageMap.OcmsStageMap;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsStageMap.OcmsStageMapRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for Toppan upload workflow
 * Handles data access operations for different stages
 */
@Slf4j
@Component
public class ToppanUploadHelper {
    private final TableQueryService tableQueryService;
    private final DataHiveNRICService dataHiveNRICService;
    private final DataHiveFINService dataHiveFINService;
    private final DataHiveUENService dataHiveUENService;
    private final ParameterRepository parameterRepository;
    private final OcmsDriverNoticeService driverNoticeService;
    private final OcmsRequestDriverParticularsService requestDriverParticularsService;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final OcmsStageMapRepository stageMapRepository;
    private final com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    public ToppanUploadHelper(
            TableQueryService tableQueryService,
            DataHiveNRICService dataHiveNRICService,
            DataHiveFINService dataHiveFINService,
            DataHiveUENService dataHiveUENService,
            ParameterRepository parameterRepository,
            @Autowired(required = false) OcmsDriverNoticeService driverNoticeService,
            @Autowired(required = false) OcmsRequestDriverParticularsService requestDriverParticularsService,
            OcmsValidOffenceNoticeRepository validOffenceNoticeRepository,
            OcmsStageMapRepository stageMapRepository,
            com.ocmsintranet.cronservice.utilities.SuspensionApiClient suspensionApiClient) {
        this.tableQueryService = tableQueryService;
        this.dataHiveNRICService = dataHiveNRICService;
        this.dataHiveFINService = dataHiveFINService;
        this.dataHiveUENService = dataHiveUENService;
        this.parameterRepository = parameterRepository;
        this.driverNoticeService = driverNoticeService;
        this.requestDriverParticularsService = requestDriverParticularsService;
        this.validOffenceNoticeRepository = validOffenceNoticeRepository;
        this.stageMapRepository = stageMapRepository;
        this.suspensionApiClient = suspensionApiClient;
    }
    
    /**
     * Get the next processing stage from ocms_stage_map table
     *
     * @param currentStage The current processing stage (e.g., "RD1", "RD2")
     * @return The next processing stage, or null if not found
     */
    private String getNextProcessingStageFromMap(String currentStage) {
        try {
            return stageMapRepository.findById(currentStage)
                    .map(OcmsStageMap::getNextProcessingStage)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get next processing stage from ocms_stage_map for {}: {}", currentStage, e.getMessage());
            return null;
        }
    }

    /**
     * Get the duration days for a stage from ocms_parameter table (STAGEDAY parameter)
     *
     * @param stage The processing stage (e.g., "RD1", "RD2")
     * @return The number of days, or null if not found
     */
    private Integer getStageDurationDays(String stage) {
        try {
            String daysValue = getParameterValue("STAGEDAYS", stage);
            if (daysValue != null) {
                return Integer.parseInt(daysValue);
            }
            return null;
        } catch (NumberFormatException e) {
            log.warn("Invalid STAGEDAY value for stage {}: {}", stage, e.getMessage());
            return null;
        }
    }

    /**
     * Get parameter value from ocms_parameter table
     *
     * @param parameterId The parameter ID (e.g., "POS", "PDD")
     * @param code The stage code (e.g., "RD1", "RD2")
     * @return The parameter value, or null if not found
     */
    private String getParameterValue(String parameterId, String code) {
        try {
            ParameterId id = new ParameterId(parameterId, code);
            return parameterRepository.findById(id)
                    .map(Parameter::getValue)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get parameter value for {} - {}: {}", parameterId, code, e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate the letter date based on parameter table configuration
     * 
     * @param stage The processing stage
     * @param baseDate The base date for calculation
     * @return The calculated letter date
     */
    private LocalDateTime calculateLetterDate(ToppanStage stage, LocalDateTime baseDate) {
        String ldateValue = getParameterValue("POS", stage.getCurrentStage());
        if (ldateValue != null) {
            try {
                int daysToAdd = Integer.parseInt(ldateValue);
                return baseDate.plusDays(daysToAdd);
            } catch (NumberFormatException e) {
                log.warn("Invalid POS value for stage {}: {}", stage.getCurrentStage(), ldateValue);
            }
        }
        // Default to 14 days if not configured
        return baseDate.plusDays(14);
    }
    
    /**
     * Calculate the payment due date based on parameter table configuration
     * 
     * @param stage The processing stage
     * @param baseDate The base date for calculation
     * @return The calculated payment due date
     */
    private LocalDateTime calculatePaymentDueDate(ToppanStage stage, LocalDateTime baseDate) {
        String pddValue = getParameterValue("PDD", stage.getCurrentStage());
        if (pddValue != null) {
            try {
                int daysToAdd = Integer.parseInt(pddValue);
                return baseDate.plusDays(daysToAdd);
            } catch (NumberFormatException e) {
                log.warn("Invalid PDD value for stage {}: {}", stage.getCurrentStage(), pddValue);
            }
        }
        // Default to 28 days if not configured
        return baseDate.plusDays(28);
    }

    /**
     * Enrich notices with DataHive data (mandatory)
     * This method queries DataHive for NRIC/FIN/UEN data and enriches the offence notices
     * Throws exception if DataHive query fails (mandatory requirement)
     */
    public void enrichNoticesWithDataHive(List<Map<String, Object>> offenceNotices) {
        log.info("Starting DataHive enrichment for {} offence notices", offenceNotices.size());
        
        // First, we need to get the owner/driver records for these notices to extract ID information
        List<String> noticeNumbers = offenceNotices.stream()
            .map(notice -> (String) notice.get("noticeNo"))
            .filter(noticeNo -> noticeNo != null)
            .collect(Collectors.toList());
        
        if (noticeNumbers.isEmpty()) {
            log.warn("No valid notice numbers found for DataHive enrichment");
            return;
        }
        
        // Query owner/driver records to get ID information
        Map<String, Object> ownerDriverFilter = new HashMap<>();
        ownerDriverFilter.put("$limit", 10000);
        
        List<Map<String, Object>> ownerDriverRecords = tableQueryService.query(
            "ocms_offence_notice_owner_driver", ownerDriverFilter);
        
        // Filter to only our notices and offenders
        Map<String, Map<String, Object>> noticeToOwnerDriver = ownerDriverRecords.stream()
            .filter(record -> {
                // Try both camelCase and snake_case field names
                String noticeNo = (String) record.get("noticeNo");
                String offenderIndicator = (String) record.get("offenderIndicator");
                return noticeNumbers.contains(noticeNo) && "Y".equals(offenderIndicator);
            })
            .collect(Collectors.toMap(
                record -> {
                    String noticeNo = (String) record.get("noticeNo");
                    return noticeNo;
                },
                record -> record,
                (existing, replacement) -> existing // Keep first if duplicates
            ));
        
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        // Process each notice with DataHive
        for (Map<String, Object> notice : offenceNotices) {
            String noticeNo = (String) notice.get("noticeNo");
            Map<String, Object> ownerDriver = noticeToOwnerDriver.get(noticeNo);
            
            if (ownerDriver == null) {
                log.warn("No owner/driver record found for notice {}, skipping DataHive enrichment", noticeNo);
                continue;
            }
            
            String idNo = (String) ownerDriver.get("idNo");
           
            String idType = (String) ownerDriver.get("idType");
           
            if (idNo == null || idType == null) {
                log.warn("Missing ID information for notice {} - idNo: {}, idType: {}, skipping DataHive enrichment",
                    noticeNo, idNo, idType);
                continue;
            }
            
            try {
                // Determine which DataHive service to use based on ID type
                boolean enriched = false;
                
                // if ("S".equals(idType)) {
                //     // Singapore NRIC
                //     log.debug("Querying DataHive NRIC service for notice {} with NRIC {}", noticeNo, idNo);
                //     Object nricData = dataHiveNRICService.retrieveNRICData(idNo, noticeNo);
                //     if (nricData != null) {
                //         log.info("Successfully retrieved NRIC data from DataHive for notice {}", noticeNo);
                //         // DataHive service automatically handles database updates
                //         enriched = true;
                //     }
                // } 
                
                if ("F".equals(idType)) {
                    // FIN (Foreign Identification Number)
                    log.debug("Querying DataHive FIN service for notice {} with FIN {}", noticeNo, idNo);
                    String ownerDriverIndicator = (String) ownerDriver.get("ownerDriverIndicator");
                    if (ownerDriverIndicator == null) {
                        ownerDriverIndicator = (String) ownerDriver.get("owner_driver_indicator");
                    }
                    // Convert LocalDateTime to Date if needed
                    Object offenceDateObj = notice.get("noticeDateAndTime");
                    java.util.Date offenceDate = null;
                    if (offenceDateObj instanceof LocalDateTime) {
                        offenceDate = java.sql.Timestamp.valueOf((LocalDateTime) offenceDateObj);
                    } else if (offenceDateObj instanceof java.util.Date) {
                        offenceDate = (java.util.Date) offenceDateObj;
                    }
                    Object finData = dataHiveFINService.retrieveFINData(idNo, noticeNo, 
                        ownerDriverIndicator != null ? ownerDriverIndicator : "O", offenceDate);
                    if (finData != null) {
                        log.info("Successfully retrieved FIN data from DataHive for notice {}", noticeNo);
                        // DataHive service automatically handles database updates
                        enriched = true;
                    }
                } else if ("B".equals(idType)) {
                    // Business/UEN
                    log.debug("Querying DataHive UEN service for notice {} with UEN {}", noticeNo, idNo);
                    Object uenData = dataHiveUENService.retrieveUENData(idNo, noticeNo);
                    if (uenData != null) {
                        log.info("Successfully retrieved UEN data from DataHive for notice {}", noticeNo);
                        // DataHive service automatically handles database updates
                        enriched = true;
                    }
                } else {
                    log.debug("ID type {} not supported for DataHive enrichment for notice {}", idType, noticeNo);
                }
                
                if (enriched) {
                    successCount++;
                    // Add a flag to indicate this notice has been enriched with DataHive data
                    notice.put("dataHiveEnriched", true);
                }
                
            } catch (Exception e) {
                // DataHive is mandatory - collect errors but process all records first
                String errorMsg = String.format("DataHive query failed for notice %s (ID: %s, Type: %s): %s", 
                    noticeNo, idNo, idType, e.getMessage());
                log.error(errorMsg, e);
                errors.add(errorMsg);
                errorCount++;
            }
        }
        
        log.info("DataHive enrichment completed: {} successful, {} errors out of {} notices", 
            successCount, errorCount, offenceNotices.size());
        
        // If there were any errors, throw exception since DataHive is mandatory
        if (!errors.isEmpty()) {
            String errorSummary = String.format(
                "DataHive enrichment failed for %d notices. First error: %s", 
                errors.size(), errors.get(0));
            throw new RuntimeException(errorSummary);
        }
    }
    
    /**
     * Update the processing stage for the processed offence notices
     * OCMS 10: For ANL stage, apply PS-ANS suspension instead of updating processing stage
     */
    public void updateProcessingStage(ToppanStage stage, List<Map<String, Object>> offenceNotices, LocalDateTime processingDate) {
        if (offenceNotices == null || offenceNotices.isEmpty()) {
            log.info("No offence notices to update processing stage");
            return;
        }

        // OCMS 10: Special handling for ANL - apply PS-ANS suspension after letter sent to Toppan
        if (stage == ToppanStage.ANL) {
            log.info("OCMS 10: Applying PS-ANS suspension for {} AN letters after Toppan upload",
                     offenceNotices.size());
            int successCount = 0;
            for (Map<String, Object> notice : offenceNotices) {
                String noticeNo = (String) notice.get("noticeNo");
                if (noticeNo == null) {
                    continue;
                }
                try {
                    // Apply PS-ANS suspension
                    Map<String, Object> response = suspensionApiClient.applySuspensionSingle(
                        noticeNo,
                        "PS",  // Permanent Suspension
                        "ANS", // Advisory Notice System
                        "AN Letter sent to Toppan",
                        SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                        SystemConstant.Subsystem.OCMS_CODE,
                        null,  // caseNo
                        null   // daysToRevive - N/A for PS
                    );

                    if (suspensionApiClient.isSuccess(response)) {
                        log.info("Successfully applied PS-ANS suspension for AN letter: {}", noticeNo);
                        successCount++;
                    } else {
                        String errorMsg = suspensionApiClient.getErrorMessage(response);
                        log.error("Failed to apply PS-ANS suspension for AN letter {}: {}", noticeNo, errorMsg);
                    }
                } catch (Exception e) {
                    log.error("Error applying PS-ANS suspension for AN letter {}: {}", noticeNo, e.getMessage(), e);
                }
            }
            log.info("Successfully applied PS-ANS suspension for {}/{} AN letters",
                    successCount, offenceNotices.size());

            // Record notices in tracking tables (ocms_an_letter)
            recordNoticesInTrackingTables(stage, offenceNotices, processingDate);
            return;
        }

        log.info("Updating processing stage for {} offence notices from {} to {}",
                 offenceNotices.size(), stage.getCurrentStage(), stage.getNextStage());

        List<String> noticeNos = offenceNotices.stream()
            .map(notice -> (String) notice.get("noticeNo"))
            .filter(noticeNo -> noticeNo != null)
            .collect(Collectors.toList());

        if (noticeNos.isEmpty()) {
            log.warn("No valid notice numbers found for stage update");
            return;
        }

        // Process each notice individually to ensure data integrity
        int successCount = 0;
        for (Map<String, Object> notice : offenceNotices) {
            String noticeNo = (String) notice.get("noticeNo");
            if (noticeNo == null) {
                continue;
            }
            try {
                updateSingleNoticeStage(noticeNo, stage, processingDate, notice);
                successCount++;
            } catch (Exception e) {
                log.error("Error updating processing stage for notice {}: {}", noticeNo, e.getMessage(), e);
            }
        }

        log.info("Successfully updated processing stage for {}/{} offence notices",
                successCount, noticeNos.size());

        // Record notices in tracking tables
        recordNoticesInTrackingTables(stage, offenceNotices, processingDate);

        // OCMS 17: Apply TS-CLV for VIP vehicles at RR3/DR3 stages (after stage update and tracking table recording)
        if (stage == ToppanStage.RR3 || stage == ToppanStage.DR3) {
            applyClvSuspensionForVipVehicles(offenceNotices, stage);
        }
    }

    /**
     * Apply TS-CLV suspension for VIP vehicles at RR3/DR3 stages
     * OCMS 17 - Auto TS trigger with looping (CLV code auto re-applies every 21 days)
     *
     * @param offenceNotices List of notices that completed RR3/DR3 stage
     * @param stage Current stage (RR3 or DR3)
     */
    private void applyClvSuspensionForVipVehicles(List<Map<String, Object>> offenceNotices, ToppanStage stage) {
        log.info("OCMS 17: Checking for VIP vehicles at {} stage to apply TS-CLV suspension", stage.getCurrentStage());

        int successCount = 0;
        int vipCount = 0;

        for (Map<String, Object> notice : offenceNotices) {
            String noticeNo = (String) notice.get("noticeNo");
            String vehicleNo = (String) notice.get("vehicleNo");

            if (noticeNo == null || vehicleNo == null) {
                continue;
            }

            // Check if vehicle is VIP (starts with "V")
            if (vehicleNo.matches("^V[A-Z0-9]+$")) {
                vipCount++;
                try {
                    log.info("OCMS 17: Applying TS-CLV suspension for VIP vehicle {} (notice: {}) at {} stage",
                        vehicleNo, noticeNo, stage.getCurrentStage());

                    Map<String, Object> response = suspensionApiClient.applySuspensionSingle(
                        noticeNo,
                        "TS",  // Temporary Suspension
                        "CLV", // Classified Vehicle (VIP)
                        "Auto-suspended: VIP vehicle at reminder end (" + stage.getCurrentStage() + ")",
                        SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                        SystemConstant.Subsystem.OCMS_CODE,
                        null,  // caseNo
                        null   // daysToRevive - NULL to use default from suspension_reason table (21 days with looping)
                    );

                    if (suspensionApiClient.isSuccess(response)) {
                        log.info("Successfully applied TS-CLV suspension for VIP vehicle {} (notice: {})",
                            vehicleNo, noticeNo);
                        successCount++;
                    } else {
                        String errorMsg = suspensionApiClient.getErrorMessage(response);
                        log.error("Failed to apply TS-CLV suspension for VIP vehicle {} (notice: {}): {}",
                            vehicleNo, noticeNo, errorMsg);
                    }
                } catch (Exception e) {
                    log.error("Error applying TS-CLV suspension for VIP vehicle {} (notice: {}): {}",
                        vehicleNo, noticeNo, e.getMessage(), e);
                }
            }
        }

        if (vipCount > 0) {
            log.info("OCMS 17: Applied TS-CLV suspension for {}/{} VIP vehicles at {} stage (total notices processed: {})",
                successCount, vipCount, stage.getCurrentStage(), offenceNotices.size());
        } else {
            log.debug("OCMS 17: No VIP vehicles found in {} notices at {} stage",
                offenceNotices.size(), stage.getCurrentStage());
        }
    }

    /**
     * Record notices in tracking tables based on current processing stage
     * If current stage = RD1/RD2/RR3 -> ocms_request_driver_particulars
     * If current stage = DN1/DN2/DR3 -> ocms_driver_notice
     * If current stage = ANL -> ocms_an_letter (OCMS 10)
     */
    private void recordNoticesInTrackingTables(ToppanStage stage, List<Map<String, Object>> offenceNotices, LocalDateTime processingDate) {
        String currentStage = stage.getCurrentStage();

        // OCMS 10: Special handling for ANL (Advisory Notice Letters)
        if ("ANL".equals(currentStage)) {
            log.info("OCMS 10: Recording AN letters to ocms_an_letter tracking table");
            for (Map<String, Object> notice : offenceNotices) {
                String noticeNo = (String) notice.get("noticeNo");
                try {
                    recordAnLetter(notice, processingDate);
                    log.debug("Recorded AN letter for notice {} in ocms_an_letter", noticeNo);
                } catch (Exception e) {
                    log.error("Error recording AN letter {} in tracking table: {}", noticeNo, e.getMessage());
                }
            }
            return;
        }

        if (driverNoticeService == null || requestDriverParticularsService == null) {
            log.warn("Tracking table services not available, skipping recording");
            return;
        }

        for (Map<String, Object> notice : offenceNotices) {
            String noticeNo = (String) notice.get("noticeNo");

            try {
                // Check if current processing stage is RD1, RD2, or RR3
                // These are "Request Driver Particulars" stages
                if ("RD1".equals(currentStage) ||
                    "RD2".equals(currentStage) ||
                    "RR3".equals(currentStage)) {
                    // Record in ocms_request_driver_particulars table
                    log.debug("Recording notice {} to ocms_request_driver_particulars (stage: {})",
                             noticeNo, currentStage);
                    recordRequestDriverParticulars(stage, notice, processingDate);
                } else {
                    // DN1, DN2, DR3 - Record in ocms_driver_notice table
                    log.debug("Recording notice {} to ocms_driver_notice (stage: {})",
                             noticeNo, currentStage);
                    recordDriverNotice(stage, notice, processingDate);
                }
            } catch (Exception e) {
                log.error("Error recording notice {} in tracking table: {}", noticeNo, e.getMessage());
            }
        }
    }
    
    /**
     * Record a driver notice in ocms_driver_notice table
     */
    private void recordDriverNotice(ToppanStage stage, Map<String, Object> noticeData, LocalDateTime processingDate) {
        String noticeNo = (String) noticeData.get("noticeNo");

        // Fix F-38: Normalize to start of day for idempotency (same-day reruns don't create duplicates)
        LocalDateTime dayStart = processingDate.toLocalDate().atStartOfDay();

        // Check if already exists
        Map<String, Object> ownerDriverFilter = new HashMap<>();
        ownerDriverFilter.put("dateOfProcessing", dayStart);
        ownerDriverFilter.put("noticeNo", noticeNo);
        List<Map<String, Object>> existingRecords = tableQueryService.query(
                "ocms_driver_notice", ownerDriverFilter);
        if (existingRecords != null && !existingRecords.isEmpty()) {
            log.debug("Driver notice already exists for {} on {}", noticeNo, dayStart.toLocalDate());
            return;
        }

        // Debug: Log the values being retrieved
        String idType = (String) noticeData.get("ownerDriverIdType");
        String idNo = (String) noticeData.get("ownerDriverIdNo");
        String name = (String) noticeData.get("ownerDriverName");

        log.debug("Recording driver notice {} - idType: {}, idNo: {}, name: {}",
            noticeNo, idType, idNo, name);

        // Extract letterDate from noticeData (calculated based on POS parameter)
        LocalDateTime letterDate = (LocalDateTime) noticeData.get("letterDate");
      

        // Use enriched data from noticeData (which includes address information)
        OcmsDriverNotice driverNotice = OcmsDriverNotice.builder()
            .dateOfProcessing(processingDate)
            .noticeNo(noticeNo)
            .processingStage(stage.getCurrentStage())
            .dateOfDn(letterDate)  
            .driverIdType(idType)
            .driverNricNo(idNo)
            .driverName(name)
            .driverBlkHseNo((String) noticeData.get("blkHseNo"))
            .driverStreet((String) noticeData.get("streetName"))
            .driverFloor((String) noticeData.get("floorNo"))
            .driverUnit((String) noticeData.get("unitNo"))
            .driverBldg((String) noticeData.get("buildingName"))
            .driverPostalCode((String) noticeData.get("postalCode"))
            .reminderFlag(stage.getCurrentStage().endsWith("2") || stage.getCurrentStage().endsWith("3") ? "Y" : "N")
            .creDate(LocalDateTime.now())
            .creUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
            .build();

        driverNoticeService.save(driverNotice);
        log.debug("Recorded driver notice for {} in stage {}", noticeNo, stage.getCurrentStage());
    }
    
    /**
     * Record an Advisory Notice letter in ocms_an_letter table (OCMS 10)
     */
    private void recordAnLetter(Map<String, Object> noticeData, LocalDateTime processingDate) {
        String noticeNo = (String) noticeData.get("noticeNo");

        // Fix F-38: Normalize to start of day for idempotency (same-day reruns don't create duplicates)
        LocalDateTime dayStart = processingDate.toLocalDate().atStartOfDay();

        // Check if already exists
        Map<String, Object> anLetterFilter = new HashMap<>();
        anLetterFilter.put("dateOfProcessing", dayStart);
        anLetterFilter.put("noticeNo", noticeNo);
        List<Map<String, Object>> existingRecords = tableQueryService.query(
                "ocms_an_letter", anLetterFilter);
        if (existingRecords != null && !existingRecords.isEmpty()) {
            log.debug("AN letter already exists for {} on {}", noticeNo, dayStart.toLocalDate());
            return;
        }

        // Extract letterDate from noticeData
        LocalDateTime letterDate = (LocalDateTime) noticeData.get("letterDate");

        // Get processing stage from the notice data
        String processingStage = (String) noticeData.get("lastProcessingStage");
        if (processingStage == null) {
            processingStage = (String) noticeData.get("nextProcessingStage");
        }

        // Prepare record for insertion using TableQueryService
        Map<String, Object> anLetterRecord = new HashMap<>();
        anLetterRecord.put("dateOfProcessing", processingDate);
        anLetterRecord.put("noticeNo", noticeNo);
        anLetterRecord.put("dateOfAnLetter", letterDate);
        anLetterRecord.put("processingStage", processingStage);
        anLetterRecord.put("ownerIdType", noticeData.get("ownerDriverIdType"));
        anLetterRecord.put("ownerNricNo", noticeData.get("ownerDriverIdNo"));
        anLetterRecord.put("ownerName", noticeData.get("ownerDriverName"));
        anLetterRecord.put("ownerBlkHseNo", noticeData.get("blkHseNo"));
        anLetterRecord.put("ownerStreet", noticeData.get("streetName"));
        anLetterRecord.put("ownerFloor", noticeData.get("floorNo"));
        anLetterRecord.put("ownerUnit", noticeData.get("unitNo"));
        anLetterRecord.put("ownerBldg", noticeData.get("buildingName"));
        anLetterRecord.put("ownerPostalCode", noticeData.get("postalCode"));
        anLetterRecord.put("creDate", LocalDateTime.now());
        anLetterRecord.put("creUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

        // Insert record
        // TODO OCMS 10: Create OcmsAnLetterService to properly save records
        // For now, skip insertion as TableQueryService doesn't have insert() method
        // Alternative: Use JPA repository or create entity service
        log.warn("OCMS 10 TODO: AN letter recording for {} not implemented - need to create OcmsAnLetterService", noticeNo);
    }

    /**
     * Record a request for driver particulars in ocms_request_driver_particulars table
     */
    private void recordRequestDriverParticulars(ToppanStage stage, Map<String, Object> noticeData, LocalDateTime processingDate) {
        String noticeNo = (String) noticeData.get("noticeNo");

        // Fix F-38: Normalize to start of day for idempotency (same-day reruns don't create duplicates)
        LocalDateTime dayStart = processingDate.toLocalDate().atStartOfDay();

        // Check if already exists
        Map<String, Object> requestDriverParticularFilter = new HashMap<>();
        requestDriverParticularFilter.put("dateOfProcessing", dayStart);
        requestDriverParticularFilter.put("noticeNo", noticeNo);
        List<Map<String, Object>> existingRdpRecords = tableQueryService.query(
                "ocms_request_driver_particulars", requestDriverParticularFilter);
        if (existingRdpRecords != null && !existingRdpRecords.isEmpty()) {
            log.debug("Request driver particulars already exists for {} on {}", noticeNo, dayStart.toLocalDate());
            return;
        }

        // Extract letterDate from noticeData (calculated based on POS parameter)
        LocalDateTime letterDate = (LocalDateTime) noticeData.get("letterDate");
       
        // Use enriched data from noticeData (which includes address information)
        OcmsRequestDriverParticulars request = OcmsRequestDriverParticulars.builder()
            .dateOfProcessing(processingDate)
            .noticeNo(noticeNo)
            .processingStage(stage.getCurrentStage())
            .dateOfRdp(letterDate)  
            .ownerIdType((String) noticeData.get("ownerDriverIdType"))
            .ownerNricNo((String) noticeData.get("ownerDriverIdNo"))
            .ownerName((String) noticeData.get("ownerDriverName"))
            .ownerBlkHseNo((String) noticeData.get("blkHseNo"))
            .ownerStreet((String) noticeData.get("streetName"))
            .ownerFloor((String) noticeData.get("floorNo"))
            .ownerUnit((String) noticeData.get("unitNo"))
            .ownerBldg((String) noticeData.get("buildingName"))
            .ownerPostalCode((String) noticeData.get("postalCode"))
            .reminderFlag(stage.getCurrentStage().endsWith("3") ? "Y" : "N")
            .creDate(LocalDateTime.now())
            .creUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
            .build();

        requestDriverParticularsService.save(request);
        log.debug("Recorded request driver particulars for {} in stage {}", noticeNo, stage.getCurrentStage());
    }

    /**
     * Get warrant description based on stage
     */
    private String getWarrantDescription(ToppanStage stage) {
        switch (stage) {
            case RD1:
            case DN1:
                return "First Notice";
            case RD2:
            case DN2:
                return "Reminder Notice";
            case RR3:
            case DR3:
                return "Final Reminder Notice";
            default:
                return "";
        }
    }

    /**
     * Update processing stage for a single notice
     * Uses database-driven configuration from ocms_stage_map and ocms_parameter tables
     *
     * @param noticeNo The notice number to update
     * @param stage The current processing stage
     * @param processingDate The processing date
     * @param noticeData The notice data map containing amountPayable and other enriched fields
     */
    private void updateSingleNoticeStage(String noticeNo, ToppanStage stage, LocalDateTime processingDate, Map<String, Object> noticeData) {
        Map<String, Object> currentFilter = new HashMap<>();
        currentFilter.put("noticeNo", noticeNo);

        List<Map<String, Object>> currentRecords = tableQueryService.query("ocms_valid_offence_notice", currentFilter);

        if (currentRecords.isEmpty()) {
            log.warn("No record found for notice number: {}", noticeNo);
            return;
        }

        Map<String, Object> currentRecord = currentRecords.get(0);
        Map<String, Object> updates = new HashMap<>();

        // Preserve history by moving current values to previous fields
        updates.put("prevProcessingStage", currentRecord.get("lastProcessingStage"));
        updates.put("prevProcessingDate", currentRecord.get("lastProcessingDate"));

        // Update current processing information
        updates.put("lastProcessingStage", stage.getCurrentStage());
        updates.put("lastProcessingDate", processingDate);

        // NEW: Get next processing stage from ocms_stage_map table (database-driven)
        String nextStage = getNextProcessingStageFromMap(stage.getCurrentStage());

        // NEW: Get duration days from ocms_parameter table (STAGEDAY parameter)
        Integer durationDays = getStageDurationDays(stage.getCurrentStage());

        // Fallback to enum values if database lookup fails
        if (nextStage == null) {
            log.warn("Next stage not found in ocms_stage_map for stage {}, using enum fallback", stage.getCurrentStage());
            // OLD CODE (commented): Using enum values as fallback
            if (stage == ToppanStage.DR3 || stage == ToppanStage.RR3) {
                nextStage = "CPC";
            } else if (stage.getNextStage() != null) {
                nextStage = stage.getNextStage();
            }
        }

        if (durationDays == null) {
            log.warn("Duration days not found in ocms_parameter (STAGEDAY) for stage {}, using enum fallback", stage.getCurrentStage());
            // OLD CODE (commented): Using enum values as fallback
            durationDays = stage.getDurationDays();
        }

        // Set next processing stage and date using database values
        // Ensure next_processing_date is set to midnight (00:00:00) for consistency
        if (nextStage != null) {
            updates.put("nextProcessingStage", nextStage);
            LocalDateTime nextProcessingDate = processingDate.toLocalDate().plusDays(durationDays).atStartOfDay();
            updates.put("nextProcessingDate", nextProcessingDate);
            log.debug("Next stage from database: {} with {} days (next date: {})", nextStage, durationDays, nextProcessingDate);
        }

        /* OLD CODE (commented out - replaced with database-driven approach):
        // Set next processing stage and date
        if (stage == ToppanStage.DR3 || stage == ToppanStage.RR3) {
            updates.put("nextProcessingStage", "CPC");
            updates.put("nextProcessingDate", processingDate.plusDays(stage.getDurationDays()));
        } else if (stage.getNextStage() != null) {
            updates.put("nextProcessingStage", stage.getNextStage());
            updates.put("nextProcessingDate", processingDate.plusDays(stage.getDurationDays()));
        }
        */

        // Get and convert compositionAmount from database (used for both default amountPayable and RR3/DR3 calculation)
        Object compositionAmountObj = currentRecord.get("compositionAmount");
        Double compositionAmount = null;
        if (compositionAmountObj != null) {
            if (compositionAmountObj instanceof java.math.BigDecimal) {
                compositionAmount = ((java.math.BigDecimal) compositionAmountObj).doubleValue();
            } else if (compositionAmountObj instanceof Double) {
                compositionAmount = (Double) compositionAmountObj;
            } else if (compositionAmountObj instanceof Number) {
                compositionAmount = ((Number) compositionAmountObj).doubleValue();
            }
        }

        // Set default amountPayable = compositionAmount for all stages
        updates.put("amountPayable", compositionAmount);

        // For RR3/DR3 stages: recalculate amountPayable and set administration fee
        String currentStage = stage.getCurrentStage();
        if ("RR3".equals(currentStage) || "DR3".equals(currentStage)) {
            // Get administration fee from ocms_parameter table
            String adminFeeValue = getParameterValue("ADM", currentStage);
            if (adminFeeValue != null) {
                try {
                    Double administrationFee = Double.parseDouble(adminFeeValue);
                    updates.put("administrationFee", administrationFee);

                    // Recalculate amountPayable = compositionAmount + administrationFee
                    if (compositionAmount != null) {
                        Double recalculatedAmountPayable = compositionAmount + administrationFee;
                        updates.put("amountPayable", recalculatedAmountPayable);
                        log.debug("Updating VON for notice {} ({}): amountPayable={} (composition={} + adminFee={})",
                                 noticeNo, currentStage, recalculatedAmountPayable, compositionAmount, administrationFee);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid administration fee value from parameter table for stage {}: {}",
                            currentStage, adminFeeValue);
                }
            } else {
                log.warn("Administration fee not found in parameter table for stage {}", currentStage);
            }
        }

        // Calculate and update payment_due_date
        LocalDateTime letterDate = calculateLetterDate(stage, processingDate);
        LocalDateTime paymentDueDate = calculatePaymentDueDate(stage, letterDate);
        updates.put("paymentDueDate", paymentDueDate);
        log.debug("Updating payment_due_date for notice {} ({}): {}", noticeNo, currentStage, paymentDueDate);

        updates.put("lastUpdatedDate", LocalDateTime.now());

        // PROCESS 6: Set is_sync to N before updating VON
        updates.put("isSync", "N");

        // Perform the update to VON
        tableQueryService.patch("ocms_valid_offence_notice", currentFilter, updates);

        log.debug("Updated processing stage for notice {}: {} -> {} (isSync=N)",
                 noticeNo, stage.getCurrentStage(), updates.get("nextProcessingStage"));

        // Delete notice from nro_temp after successful stage update
        try {
            Map<String, Object> nroTempFilter = new HashMap<>();
            nroTempFilter.put("noticeNo", noticeNo);
            tableQueryService.delete("ocms_nro_temp", nroTempFilter);
            log.debug("Deleted notice {} from ocms_nro_temp after stage update", noticeNo);
        } catch (Exception e) {
            log.warn("Failed to delete notice {} from ocms_nro_temp (non-critical): {}", noticeNo, e.getMessage());
            // Don't fail the entire process if nro_temp deletion fails
        }

        // PROCESS 6: Immediately sync to eVON (Internet database)
        if (paymentSyncEnabled) {
            try {
                syncToEocmsValidOffenceNotice(noticeNo, updates);

                // After successful eVON sync, update is_sync to Y
                Map<String, Object> syncUpdate = new HashMap<>();
                syncUpdate.put("isSync", "Y");
                tableQueryService.patch("ocms_valid_offence_notice", currentFilter, syncUpdate);
                log.info("Successfully synced notice {} to eVON and set isSync=Y", noticeNo);

            } catch (Exception e) {
                log.warn("Failed to sync notice {} to eVON - isSync remains N, will retry in batch sync (Process 7): {}",
                        noticeNo, e.getMessage());
                // Continue processing - batch sync will handle retry
            }
        } else {
            log.debug("Payment sync disabled - skipping immediate sync for notice {}, isSync remains N", noticeNo);
        }
    }

    /**
     * Sync relevant fields to eocms_valid_offence_notice (Internet database)
     * Called immediately after updating intranet VON for Process 6 (Toppan Upload)
     * Syncs: upd_date, upd_user_id, suspension_type, epr_reason_of_suspension,
     *        epr_date_of_suspension, amount_payable, last_processing_stage,
     *        next_processing_stage, eservice_msg, payment_acceptance_allowed
     */
    private void syncToEocmsValidOffenceNotice(String noticeNo, Map<String, Object> vonUpdates) {
        try {
            // Query existing eVON record
            Map<String, Object> eVonFilter = new HashMap<>();
            eVonFilter.put("noticeNo", noticeNo);

            List<Map<String, Object>> eVonRecords = tableQueryService.query("eocms_valid_offence_notice", eVonFilter);

            if (eVonRecords == null || eVonRecords.isEmpty()) {
                String msg = "eVON record not found for notice " + noticeNo + " - cannot sync to eVON";
                log.debug(msg);
                throw new RuntimeException(msg);
            }

            // Prepare eVON updates with fields specified in flowchart
            Map<String, Object> eVonUpdates = new HashMap<>();

            // 1. upd_date and upd_user_id (always set)
            eVonUpdates.put("updDate", LocalDateTime.now());
            eVonUpdates.put("updUserId", SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

            // 2. suspension_type
            if (vonUpdates.containsKey("suspensionType")) {
                eVonUpdates.put("suspensionType", vonUpdates.get("suspensionType"));
            }

            // 3. epr_reason_of_suspension
            if (vonUpdates.containsKey("eprReasonOfSuspension")) {
                eVonUpdates.put("eprReasonOfSuspension", vonUpdates.get("eprReasonOfSuspension"));
            }

            // 4. epr_date_of_suspension
            if (vonUpdates.containsKey("eprDateOfSuspension")) {
                eVonUpdates.put("eprDateOfSuspension", vonUpdates.get("eprDateOfSuspension"));
            }

            // 5. amount_payable (important for RR3/DR3 with administration fee)
            if (vonUpdates.containsKey("amountPayable")) {
                eVonUpdates.put("amountPayable", vonUpdates.get("amountPayable"));
            }

            // 6. last_processing_stage
            if (vonUpdates.containsKey("lastProcessingStage")) {
                eVonUpdates.put("lastProcessingStage", vonUpdates.get("lastProcessingStage"));
            }

            // 7. next_processing_stage
            if (vonUpdates.containsKey("nextProcessingStage")) {
                eVonUpdates.put("nextProcessingStage", vonUpdates.get("nextProcessingStage"));
            }

            // 8. eservice_msg
            if (vonUpdates.containsKey("eserviceMessageCode")) {
                eVonUpdates.put("eserviceMessageCode", vonUpdates.get("eserviceMessageCode"));
            }

            // 9. payment_acceptance_allowed
            if (vonUpdates.containsKey("paymentAcceptanceAllowed")) {
                eVonUpdates.put("paymentAcceptanceAllowed", vonUpdates.get("paymentAcceptanceAllowed"));
            }

            // Perform the sync
            tableQueryService.patch("eocms_valid_offence_notice", eVonFilter, eVonUpdates);
            log.info("Synced {} fields to eVON for notice {}", eVonUpdates.size(), noticeNo);

        } catch (Exception e) {
            log.error("Error syncing to eVON for notice {}: {}", noticeNo, e.getMessage(), e);
            throw e;
        }
    }

    // ========== OPTIMIZED SQL-BASED METHODS ==========

    /**
     * Get valid offence notices using optimized SQL query (NEW OPTIMIZED VERSION)
     * Replaces the old Java-based filtering approach with database-side filtering
     *
     * @param stage The processing stage
     * @param processingDate The processing date
     * @return List of valid offence notices with all required data
     */
    public List<Map<String, Object>> getValidOffenceNoticesOptimized(ToppanStage stage, LocalDateTime processingDate) {
        log.info("Querying valid offence notices (OPTIMIZED) for Toppan {} stage: {}",
                stage.getCurrentStage(), stage.getDescription());

        // OCMS 10: Special handling for ANL (Advisory Notice Letters)
        if (stage == ToppanStage.ANL) {
            log.info("OCMS 10: Using specialized query for Advisory Notice Letters");
            return getAdvisoryNoticeLettersOptimized(processingDate);
        }

        try {
            // Step 1: Get notices using optimized SQL query (includes database-side exclusions)
            List<Object[]> queryResults = validOffenceNoticeRepository.findToppanNoticesForStage(stage.getCurrentStage());

            if (queryResults.isEmpty()) {
                log.info("No valid offence notices found for stage {} (OPTIMIZED)", stage.getCurrentStage());
                return new ArrayList<>();
            }

            log.info("OPTIMIZED query returned {} notices (vs old approach: fetch all → filter in Java)", queryResults.size());

            // Step 2: Map query results to Map<String, Object>
            List<Map<String, Object>> notices = new ArrayList<>();
            for (Object[] row : queryResults) {
                Map<String, Object> notice = new HashMap<>();
                // Map based on query column order
                notice.put("noticeNo", row[0]);
                notice.put("vehicleNo", row[1]);
                notice.put("noticeDateAndTime", row[2]);
                notice.put("ppName", row[3]);
                notice.put("compositionAmount", row[4]);
                notice.put("nextProcessingStage", row[5]);
                notice.put("nextProcessingDate", row[6]);
                notice.put("ruleDesc", row[7]);
                notice.put("ruleNo", row[8]);
                notice.put("computerRuleCode", row[9]);

                // Map owner/driver fields with correct names expected by tracking table
                // Convert CHAR fields to String
                notice.put("ownerDriverName", row[10]);
                notice.put("ownerDriverIdNo", row[11]);
                notice.put("ownerDriverIdType", convertToString(row[12]));
                notice.put("offenderIndicator", convertToString(row[13]));
                notice.put("ownerDriverIndicator", convertToString(row[14]));
                notice.put("vehicleRegistrationType", convertToString(row[15]));

                // Debug log for first notice to verify mapping
                if (notices.isEmpty()) {
                    log.debug("Sample notice mapping - noticeNo: {}, idType: {} (class: {}), idNo: {}, name: {}",
                        row[0],
                        row[11],
                        row[11] != null ? row[11].getClass().getSimpleName() : "null",
                        row[10],
                        row[9]);
                }

                notices.add(notice);
            }

            log.debug("Mapped {} notices to Map objects", notices.size());

            // Step 3: Enrich with addresses using optimized query
            if (!notices.isEmpty()) {
                enrichNoticesWithAddressesOptimized(notices, stage);
                log.info("Enriched notices with addresses (OPTIMIZED query)");
            }

            // Step 3a: Filter out MID/DIP vehicles at RD2/DN2 stages (OCMS 14 requirement)
            int beforeMidDipFilter = notices.size();
            String currentStage = stage.getCurrentStage();
            if ("RD2".equals(currentStage) || "DN2".equals(currentStage)) {
                List<String> excludedMidDipNotices = new ArrayList<>();
                notices = notices.stream()
                    .filter(n -> {
                        String vehicleRegType = convertToString(n.get("vehicleRegistrationType"));
                        String noticeNo = (String) n.get("noticeNo");

                        // Exclude Military (I) and Diplomatic (D) vehicles at RD2/DN2
                        boolean isMidOrDip = "I".equals(vehicleRegType) || "D".equals(vehicleRegType);

                        if (isMidOrDip) {
                            excludedMidDipNotices.add(noticeNo);
                            log.info("OCMS 14 - Skipping {} letter for {} vehicle (notice: {})",
                                currentStage,
                                "I".equals(vehicleRegType) ? "Military" : "Diplomatic",
                                noticeNo);
                        }

                        return !isMidOrDip; // Keep only non-MID/DIP vehicles
                    })
                    .collect(Collectors.toList());

                int excludedMidDipCount = beforeMidDipFilter - notices.size();
                if (excludedMidDipCount > 0) {
                    log.info("OCMS 14 - Excluded {} MID/DIP vehicles from {} stage: {}",
                        excludedMidDipCount,
                        currentStage,
                        excludedMidDipNotices.size() <= 10 ? excludedMidDipNotices :
                            excludedMidDipNotices.subList(0, 10) + "... (+" + (excludedMidDipNotices.size() - 10) + " more)");
                }
            }

            // Step 4: Filter out notices with no deliverable address (F-17)
            int beforeAddressFilter = notices.size();
            List<String> excludedNotices = new ArrayList<>();
            notices = notices.stream()
                .filter(n -> {
                    // Keep notice only if it has at least one address field populated
                    boolean hasAddress = n.get("addressType") != null
                        || n.get("postalCode") != null
                        || n.get("blkHseNo") != null
                        || n.get("streetName") != null
                        || n.get("buildingName") != null;

                    if (!hasAddress) {
                        String noticeNo = (String) n.get("noticeNo");
                        excludedNotices.add(noticeNo);
                        log.warn("F-17 Address Filter: Excluding notice {} - All address fields are null (addressType={}, postalCode={}, blkHseNo={}, streetName={}, buildingName={})",
                            noticeNo,
                            n.get("addressType"),
                            n.get("postalCode"),
                            n.get("blkHseNo"),
                            n.get("streetName"),
                            n.get("buildingName"));
                    }

                    return hasAddress;
                })
                .collect(Collectors.toList());
            int excludedCount = beforeAddressFilter - notices.size();
            if (excludedCount > 0) {
                log.info("F-17 Address Filter: Excluded {} notices with no deliverable address: {}",
                    excludedCount,
                    excludedNotices.size() <= 10 ? excludedNotices : excludedNotices.subList(0, 10) + "... (+" + (excludedNotices.size() - 10) + " more)");
            }

            // NOTE: DataHive enrichment is done separately BEFORE this method is called
            // See ToppanLettersGeneratorJob.doExecute() - Step 1: DataHive enrichment
            // This follows the high-level flow where DataHive runs first for all stages

            // Step 5: Add calculated dates
            LocalDateTime letterDate = calculateLetterDate(stage, processingDate);
            LocalDateTime paymentDueDate = calculatePaymentDueDate(stage, letterDate);

            // Step 6: Calculate amount payable for RR3/DR3 stages (F-24)
            String stageCode = stage.getCurrentStage();
            boolean isRR3orDR3 = "RR3".equals(stageCode) || "DR3".equals(stageCode);

            // Get administration fee only for RR3/DR3
            Double administrationFee = 0.0;
            if (isRR3orDR3) {
                String adminFeeValue = getParameterValue("ADM", stageCode);
                try {
                    administrationFee = adminFeeValue != null ? Double.parseDouble(adminFeeValue) : 0.0;
                    log.debug("OPTIMIZED - Administration fee for stage {}: {}", stageCode, administrationFee);
                } catch (NumberFormatException ex) {
                    log.warn("OPTIMIZED - Invalid administration fee value for stage {}: {}", stageCode, adminFeeValue);
                    administrationFee = 0.0;
                }
            }

            for (Map<String, Object> notice : notices) {
                notice.put("letterDate", letterDate);
                notice.put("paymentDueDate", paymentDueDate);
                notice.put("warrantDescription", getWarrantDescription(stage));

                // F-24: Calculate and add amount payable
                // Convert compositionAmount from BigDecimal to Double (SQL Server returns BigDecimal for numeric fields)
                Object compositionAmountObj = notice.get("compositionAmount");
                Double compositionAmount = 0.0;
                if (compositionAmountObj != null) {
                    if (compositionAmountObj instanceof java.math.BigDecimal) {
                        compositionAmount = ((java.math.BigDecimal) compositionAmountObj).doubleValue();
                    } else if (compositionAmountObj instanceof Double) {
                        compositionAmount = (Double) compositionAmountObj;
                    } else if (compositionAmountObj instanceof Number) {
                        compositionAmount = ((Number) compositionAmountObj).doubleValue();
                    }
                }

                Double amountPayable;

                if (isRR3orDR3) {
                    // For RR3/DR3: amount_payable = composition_amount + administration_fee
                    amountPayable = compositionAmount + administrationFee;
                } else {
                    // For other stages: use composition_amount directly
                    amountPayable = compositionAmount;
                }

                notice.put("amountPayable", amountPayable);

                // Retrieve SUR parameter (Param ID = SUR, Param code = CPC) for Fine Amount calculation
                String surValue = getParameterValue("SUR", "CPC");
                Double surAmount = 0.0;
                if (surValue != null) {
                    try {
                        surAmount = Double.parseDouble(surValue);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid SUR parameter value: {}", surValue);
                    }
                }
                notice.put("surAmount", surAmount);
            }

            log.info("OPTIMIZED: Found {} valid offence notices for stage {}",
                    notices.size(), stage.getCurrentStage());

            return notices;

        } catch (Exception e) {
            log.error("Error querying valid offence notices (OPTIMIZED) for stage {}: {}", stage, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Enrich notices with addresses using optimized SQL query with priority ordering
     *
     * @param notices List of notices to enrich
     * @param stage The processing stage
     */
    private void enrichNoticesWithAddressesOptimized(List<Map<String, Object>> notices, ToppanStage stage) {
        try {
            // Extract notice numbers
            List<String> noticeNumbers = notices.stream()
                .map(n -> (String) n.get("noticeNo"))
                .filter(noticeNo -> noticeNo != null)
                .collect(Collectors.toList());

            if (noticeNumbers.isEmpty()) {
                log.warn("No notice numbers to fetch addresses for");
                return;
            }

            // Get addresses using optimized query (already ordered by priority)
            List<Object[]> addressResults = validOffenceNoticeRepository.findAddressesForNoticesWithPriority(noticeNumbers);

            log.debug("Retrieved {} address records for {} notices", addressResults.size(), noticeNumbers.size());

            // Group addresses by notice number and owner/driver indicator
            Map<String, List<Object[]>> addressesByNotice = new HashMap<>();
            for (Object[] row : addressResults) {
                String noticeNo = (String) row[0];
                // CHAR(1) fields come back as Character, need to convert to String
                String ownerDriverIndicator = convertToString(row[1]);
                String key = noticeNo + "_" + ownerDriverIndicator;

                addressesByNotice.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            // Enrich each notice with address
            for (Map<String, Object> notice : notices) {
                String noticeNo = (String) notice.get("noticeNo");
                // Handle both Character and String types
                String ownerDriverIndicator = convertToString(notice.get("ownerDriverIndicator"));
                String key = noticeNo + "_" + ownerDriverIndicator;

                List<Object[]> noticeAddresses = addressesByNotice.get(key);
                if (noticeAddresses != null && !noticeAddresses.isEmpty()) {
                    // Address validation rules:
                    // DN1: Require furnished_mail, else suspend with TS-OLD
                    // id_type = 'P': Fallback chain - mha_reg -> lta_reg -> lta_mail -> TS-OLD
                    // Other stages: Require mha_reg, else suspend with TS-OLD
                    Object[] selectedAddress = null;
                    String idType = convertToString(notice.get("ownerDriverIdType"));

                    if ("DN1".equals(stage.getCurrentStage())) {
                        // DN1: Find furnished_mail address
                        selectedAddress = noticeAddresses.stream()
                            .filter(addr -> {
                                String addressType = convertToString(addr[2]);
                                return "furnished_mail".equals(addressType);
                            })
                            .findFirst()
                            .orElse(null);

                        if (selectedAddress == null) {
                            log.warn("Address Enrichment: Notice {} (ownerDriverIndicator={}) - No 'furnished_mail' address found for DN1 stage. Available address types: {}",
                                noticeNo, ownerDriverIndicator,
                                noticeAddresses.stream().map(addr -> convertToString(addr[2])).collect(Collectors.toList()));

                            // DN1-NO-MAIL: Apply TS-OLD suspension for notices without furnished_mail address
                            log.info("DN1-NO-MAIL: Suspending notice {} with TS-OLD status (no furnished_mail address)", noticeNo);
                            applySuspension(noticeNo, "DN1: No furnished mail address", "OLD");
                        }
                    } else if ("P".equals(idType)) {
                        // ID Type 'P': Apply fallback chain - mha_reg -> lta_reg -> lta_mail -> TS-OLD
                        log.debug("Address Enrichment: Notice {} (idType=P) - Applying fallback chain: mha_reg -> lta_reg -> lta_mail", noticeNo);

                        // Try mha_reg first
                        selectedAddress = noticeAddresses.stream()
                            .filter(addr -> "mha_reg".equals(convertToString(addr[2])))
                            .findFirst()
                            .orElse(null);

                        String selectedType = selectedAddress != null ? convertToString(selectedAddress[2]) : null;

                        // If no mha_reg, try lta_reg
                        if (selectedAddress == null) {
                            selectedAddress = noticeAddresses.stream()
                                .filter(addr -> "lta_reg".equals(convertToString(addr[2])))
                                .findFirst()
                                .orElse(null);
                            selectedType = selectedAddress != null ? convertToString(selectedAddress[2]) : null;
                        }

                        // If no lta_reg, try lta_mail
                        if (selectedAddress == null) {
                            selectedAddress = noticeAddresses.stream()
                                .filter(addr -> "lta_mail".equals(convertToString(addr[2])))
                                .findFirst()
                                .orElse(null);
                            selectedType = selectedAddress != null ? convertToString(selectedAddress[2]) : null;
                        }

                        if (selectedAddress == null) {
                            log.warn("Address Enrichment: Notice {} (idType=P, ownerDriverIndicator={}) - No valid address found after fallback chain. Available address types: {}",
                                noticeNo, ownerDriverIndicator,
                                noticeAddresses.stream().map(addr -> convertToString(addr[2])).collect(Collectors.toList()));

                            // ID Type P - NO ADDRESS: Apply TS-OLD suspension
                            log.info("ID-TYPE-P-NO-ADDRESS: Suspending notice {} with TS-OLD status (no mha_reg/lta_reg/lta_mail address found)", noticeNo);
                            applySuspension(noticeNo, "ID Type P: No mha_reg/lta_reg/lta_mail address found", "OLD");
                        } else {
                            log.info("Address Enrichment: Notice {} (idType=P) - Selected address type: {}", noticeNo, selectedType);
                        }
                    } else {
                        // Other stages (DN2, DN3, RD1, RD2, RR3, DR3): Require mha_reg only
                        selectedAddress = noticeAddresses.stream()
                            .filter(addr -> {
                                String addressType = convertToString(addr[2]);
                                return "mha_reg".equals(addressType);
                            })
                            .findFirst()
                            .orElse(null);

                        if (selectedAddress == null) {
                            log.warn("Address Enrichment: Notice {} (ownerDriverIndicator={}) - No 'mha_reg' address found for {} stage. Available address types: {}",
                                noticeNo, ownerDriverIndicator, stage.getCurrentStage(),
                                noticeAddresses.stream().map(addr -> convertToString(addr[2])).collect(Collectors.toList()));

                            // Other stages: Apply TS-OLD suspension when no mha_reg address found
                            log.info("{}-NO-MHA: Suspending notice {} with TS-OLD status (no mha_reg address)", stage.getCurrentStage(), noticeNo);
                            applySuspension(noticeNo, stage.getCurrentStage() + ": No mha_reg address", "OLD");
                        }
                    }

                    if (selectedAddress != null) {
                        // Validate that the address has actual data, not just a type
                        String blkHseNo = (String) selectedAddress[3];
                        String streetName = (String) selectedAddress[4];
                        String postalCode = (String) selectedAddress[8];
                        String buildingName = (String) selectedAddress[7];

                        boolean hasActualAddressData = (blkHseNo != null && !blkHseNo.trim().isEmpty())
                            || (streetName != null && !streetName.trim().isEmpty())
                            || (postalCode != null && !postalCode.trim().isEmpty())
                            || (buildingName != null && !buildingName.trim().isEmpty());

                        if (!hasActualAddressData) {
                            // Address record exists but all fields are empty - treat as no address
                            log.warn("Address Enrichment: Notice {} (ownerDriverIndicator={}) - Address record found (type={}) but all address fields are null/empty",
                                noticeNo, ownerDriverIndicator, selectedAddress[2]);
                            log.info("EMPTY-ADDRESS: Suspending notice {} with TS-OLD status (address record has no data)", noticeNo);
                            applySuspension(noticeNo, "Address record found but all fields are empty", "OLD");
                        } else {
                            // Map address fields
                            notice.put("addressType", selectedAddress[2]);
                            notice.put("blkHseNo", blkHseNo);
                            notice.put("streetName", streetName);
                            notice.put("floorNo", selectedAddress[5]);
                            notice.put("unitNo", selectedAddress[6]);
                            notice.put("buildingName", buildingName);
                            notice.put("postalCode", postalCode);
                        }
                    }
                } else {
                    // No address records found in ocms_offence_notice_owner_driver_addr table
                    log.warn("Address Enrichment: Notice {} (ownerDriverIndicator={}) - No address records found in database (key: {})",
                        noticeNo, ownerDriverIndicator, key);

                    // Apply TS-OLD suspension for notices without any address records
                    log.info("NO-ADDRESS: Suspending notice {} with TS-OLD status (no address records found)", noticeNo);
                    applySuspension(noticeNo, "No address records found in database", "OLD");
                }
            }

        } catch (Exception e) {
            log.error("Error enriching notices with addresses (OPTIMIZED): {}", e.getMessage(), e);
        }
    }

    /**
     * Get notices for DataHive enrichment using optimized SQL query
     *
     * @return List of notices needing DataHive enrichment
     */
    public List<Map<String, Object>> getNoticesForDataHiveEnrichmentOptimized() {
        log.info("Querying notices for DataHive enrichment (OPTIMIZED)");

        try {
            List<Object[]> queryResults = validOffenceNoticeRepository.findNoticesForDataHiveEnrichment();

            if (queryResults.isEmpty()) {
                log.info("No notices found for DataHive enrichment (OPTIMIZED)");
                return new ArrayList<>();
            }

            log.info("OPTIMIZED query returned {} notices for DataHive enrichment", queryResults.size());

            // Map query results
            List<Map<String, Object>> notices = new ArrayList<>();
            for (Object[] row : queryResults) {
                Map<String, Object> notice = new HashMap<>();
                notice.put("noticeNo", row[0]);
                notice.put("idNo", row[1]);
                notice.put("idType", row[2]);
                notice.put("ownerDriverIndicator", row[3]);
                notices.add(notice);
            }

            return notices;

        } catch (Exception e) {
            log.error("Error querying notices for DataHive enrichment (OPTIMIZED): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Apply TS suspension to a notice with configurable reason code
     *
     * Generic suspension handler for notices without required address:
     * - suspension_type = 'TS' (Temporary Suspension)
     * - eprReasonOfSuspension = reasonCode parameter (e.g., 'ROV', 'OLD')
     * - eprDateOfSuspension = current date
     * - Create suspended notice record with revival date
     * - Set dueDateOfRevival in VON
     * - For TS-OLD: nextProcessingDate is NOT patched
     * - For other reasons: nextProcessingDate is patched to revival date
     *
     * @param noticeNo The notice number to suspend
     * @param suspensionRemarks The suspension remarks describing why notice was suspended
     * @param reasonCode The reason code for suspension (e.g., 'ROV', 'OLD')
     */
    private void applySuspension(String noticeNo, String suspensionRemarks, String reasonCode) {
        try {
            log.info("Applying suspension for notice {} with TS/{} status and remarks: {}", noticeNo, reasonCode, suspensionRemarks);

            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", noticeNo);

            // Step 1: Query existing record
            List<Map<String, Object>> existingRecords = tableQueryService.query("ocms_valid_offence_notice", filters);
            if (existingRecords == null || existingRecords.isEmpty()) {
                log.warn("No record found for notice {} when applying suspension", noticeNo);
                return;
            }

            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDate currentDate = currentDateTime.toLocalDate();
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
            String formattedDate = currentDate.format(dateFormatter);

            // Step 2: Query suspension reason table to get noOfDaysForRevival for the given reason
            Map<String, Object> suspReasonFilters = new HashMap<>();
            suspReasonFilters.put("suspensionType", "TS");                         // TS = Temporary Suspension
            suspReasonFilters.put("reasonOfSuspension", reasonCode);               // Dynamic reason code

            List<Map<String, Object>> suspReasonResults = tableQueryService.query("ocms_suspension_reason", suspReasonFilters);

            if (suspReasonResults == null || suspReasonResults.isEmpty()) {
                log.error("No suspension reason found for TS/{} when applying suspension for notice {}", reasonCode, noticeNo);
                return;
            }

            // Get the noOfDaysForRevival value
            Object noOfDaysObj = suspReasonResults.get(0).get("noOfDaysForRevival");
            if (noOfDaysObj == null) {
                log.error("noOfDaysForRevival is null for TS/{} suspension reason - cannot apply suspension for notice {}", reasonCode, noticeNo);
                return;
            }

            int noOfDaysForRevival;
            try {
                if (noOfDaysObj instanceof Number) {
                    noOfDaysForRevival = ((Number) noOfDaysObj).intValue();
                } else {
                    noOfDaysForRevival = Integer.parseInt(noOfDaysObj.toString().trim());
                }
            } catch (NumberFormatException e) {
                log.error("Invalid noOfDaysForRevival value: '{}' - cannot apply suspension for notice {}", noOfDaysObj, noticeNo);
                return;
            }

            log.debug("Retrieved noOfDaysForRevival for TS/{}: {} days", reasonCode, noOfDaysForRevival);

            // Step 3: Calculate revival date
            LocalDate revivalDate = currentDate.plusDays(noOfDaysForRevival);
            String formattedRevivalDate = revivalDate.format(dateFormatter);
            log.info("Calculated revival date {} (current date + {} days) for suspended notice {}",
                formattedRevivalDate, noOfDaysForRevival, noticeNo);

            // Step 4: Apply suspension via API (replaces direct DB suspended_notice creation)
            try {
                log.info("[TS-{}] Applying TS-{} suspension via API for notice {}", reasonCode, reasonCode, noticeNo);

                Map<String, Object> apiResponse = suspensionApiClient.applySuspensionSingle(
                    noticeNo,
                    "TS", // Temporary Suspension
                    reasonCode, // Dynamic reason code (e.g., "OLD")
                    suspensionRemarks,
                    SystemConstant.User.DEFAULT_SYSTEM_USER_ID,
                    SystemConstant.Subsystem.OCMS_CODE,
                    null, // caseNo
                    noOfDaysForRevival
                );

                // Check API response
                if (suspensionApiClient.isSuccess(apiResponse)) {
                    log.info("[TS-{}] Successfully applied suspension via API for notice {}", reasonCode, noticeNo);
                } else {
                    String errorMsg = suspensionApiClient.getErrorMessage(apiResponse);
                    log.warn("[TS-{}] API returned error for notice {}: {}", reasonCode, noticeNo, errorMsg);
                }
            } catch (Exception e) {
                log.error("[TS-{}] Error calling suspension API for notice {}: {}", reasonCode, noticeNo, e.getMessage(), e);
                // Don't throw - log error and continue
            }

            // Step 5: Update VON (Valid Offence Notice) table with suspension fields
            try {
                log.debug("Updating VON suspension for notice {}", noticeNo);

                Map<String, Object> vonUpdateFields = new HashMap<>();
                vonUpdateFields.put("suspensionType", "TS");                       // Temporary Suspension
                vonUpdateFields.put("eprReasonOfSuspension", reasonCode);          // Dynamic reason code
                vonUpdateFields.put("eprDateOfSuspension", currentDate);           // Current date
                vonUpdateFields.put("dueDateOfRevival", formattedRevivalDate);     // Revival date

                // PROCESS 6: Set is_sync to N before updating VON
                vonUpdateFields.put("isSync", "N");

                List<Map<String, Object>> updatedRecords = tableQueryService.patch("ocms_valid_offence_notice", filters, vonUpdateFields);

                if (updatedRecords != null && !updatedRecords.isEmpty()) {
                    log.info("Successfully applied suspension for notice {}: suspension_type=TS, eprReasonOfSuspension={}, eprDateOfSuspension={}, dueDateOfRevival={} (isSync=N)",
                        noticeNo, reasonCode, currentDate, formattedRevivalDate);
                } else {
                    log.warn("Failed to apply suspension for notice {}", noticeNo);
                    return; // Exit if VON update failed
                }

                // PROCESS 6: Immediately sync to eVON (Internet database)
                if (paymentSyncEnabled) {
                    try {
                        syncToEocmsValidOffenceNotice(noticeNo, vonUpdateFields);

                        // After successful eVON sync, update is_sync to Y
                        Map<String, Object> syncUpdate = new HashMap<>();
                        syncUpdate.put("isSync", "Y");
                        tableQueryService.patch("ocms_valid_offence_notice", filters, syncUpdate);
                        log.info("Successfully synced suspension for notice {} to eVON and set isSync=Y", noticeNo);

                    } catch (Exception e) {
                        log.warn("Failed to sync suspension for notice {} to eVON - isSync remains N, will retry in batch sync (Process 7): {}",
                                noticeNo, e.getMessage());
                        // Continue processing - batch sync will handle retry
                    }
                } else {
                    log.debug("Payment sync disabled - skipping immediate sync for suspension on notice {}, isSync remains N", noticeNo);
                }

            } catch (Exception e) {
                log.error("Error updating VON for notice {}: {}", noticeNo, e.getMessage(), e);
                throw new RuntimeException("Failed to update VON", e);
            }

        } catch (Exception e) {
            log.error("Error applying suspension for notice {}: {}", noticeNo, e.getMessage(), e);
        }
    }

    /**
     * Helper method to convert database CHAR fields to String
     * SQL Server CHAR(1) fields return as Character objects
     *
     * @param value The database value (could be Character, String, or null)
     * @return String representation of the value
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Character) {
            return String.valueOf((Character) value);
        }
        return (String) value;
    }

    /**
     * Get valid Advisory Notice (AN) letters using optimized SQL query
     * OCMS 10: Query for AN letters that need to be sent to Toppan
     *
     * @param processingDate The processing date
     * @return List of valid Advisory Notice letters with all required data
     */
    public List<Map<String, Object>> getAdvisoryNoticeLettersOptimized(LocalDateTime processingDate) {
        log.info("Querying valid Advisory Notice (AN) letters for Toppan (OPTIMIZED)");

        try {
            // Step 1: Get AN letters using optimized SQL query
            List<Object[]> queryResults = validOffenceNoticeRepository.findAdvisoryNoticeLetters();

            if (queryResults.isEmpty()) {
                log.info("No valid Advisory Notice letters found (OPTIMIZED)");
                return new ArrayList<>();
            }

            log.info("OPTIMIZED query returned {} AN letters", queryResults.size());

            // Step 2: Map query results to Map<String, Object>
            List<Map<String, Object>> notices = new ArrayList<>();
            for (Object[] row : queryResults) {
                Map<String, Object> notice = new HashMap<>();
                // Map based on query column order (matches findAdvisoryNoticeLetters query)
                notice.put("noticeNo", row[0]);
                notice.put("vehicleNo", row[1]);
                notice.put("noticeDateAndTime", row[2]);
                notice.put("ppName", row[3]);
                notice.put("compositionAmount", row[4]);
                notice.put("lastProcessingStage", row[5]);
                notice.put("nextProcessingStage", row[6]);
                notice.put("nextProcessingDate", row[7]);
                notice.put("ruleDesc", row[8]);
                notice.put("ruleNo", row[9]);
                notice.put("computerRuleCode", row[10]);

                // Map owner/driver fields
                notice.put("ownerDriverName", row[11]);
                notice.put("ownerDriverIdNo", row[12]);
                notice.put("ownerDriverIdType", convertToString(row[13]));
                notice.put("offenderIndicator", convertToString(row[14]));
                notice.put("ownerDriverIndicator", convertToString(row[15]));
                notice.put("vehicleRegistrationType", convertToString(row[16]));

                notices.add(notice);
            }

            log.debug("Mapped {} AN letters to Map objects", notices.size());

            // Step 3: Enrich with addresses using optimized query
            if (!notices.isEmpty()) {
                enrichNoticesWithAddressesOptimized(notices, ToppanStage.ANL);
                log.info("Enriched AN letters with addresses (OPTIMIZED query)");
            }

            // Step 4: Filter out notices with no deliverable address (same as regular Toppan flow)
            int beforeAddressFilter = notices.size();
            List<String> excludedNotices = new ArrayList<>();
            notices = notices.stream()
                .filter(n -> {
                    // Keep notice only if it has at least one address field populated
                    boolean hasAddress = n.get("addressType") != null
                        || n.get("postalCode") != null
                        || n.get("blkHseNo") != null
                        || n.get("streetName") != null
                        || n.get("buildingName") != null;

                    if (!hasAddress) {
                        String noticeNo = (String) n.get("noticeNo");
                        excludedNotices.add(noticeNo);
                        log.warn("F-17 Address Filter: Excluding AN letter {} - All address fields are null (addressType={}, postalCode={}, blkHseNo={}, streetName={}, buildingName={})",
                            noticeNo,
                            n.get("addressType"),
                            n.get("postalCode"),
                            n.get("blkHseNo"),
                            n.get("streetName"),
                            n.get("buildingName"));
                    }

                    return hasAddress;
                })
                .collect(Collectors.toList());
            int excludedCount = beforeAddressFilter - notices.size();
            if (excludedCount > 0) {
                log.info("F-17 Address Filter: Excluded {} AN letters with no deliverable address: {}",
                    excludedCount,
                    excludedNotices.size() <= 10 ? excludedNotices : excludedNotices.subList(0, 10) + "... (+" + (excludedNotices.size() - 10) + " more)");
            }

            // Step 5: Add calculated dates for AN letters
            // AN letters use current processing date as letter date
            LocalDateTime letterDate = processingDate;
            // Payment due date is not applicable for AN letters (they are advisory only)
            LocalDateTime paymentDueDate = null;

            for (Map<String, Object> notice : notices) {
                notice.put("letterDate", letterDate);
                notice.put("paymentDueDate", paymentDueDate);
                notice.put("warrantDescription", ""); // No warrant for AN

                // Amount payable = composition amount (no admin fee for AN)
                Object compositionAmountObj = notice.get("compositionAmount");
                Double compositionAmount = 0.0;
                if (compositionAmountObj != null) {
                    if (compositionAmountObj instanceof java.math.BigDecimal) {
                        compositionAmount = ((java.math.BigDecimal) compositionAmountObj).doubleValue();
                    } else if (compositionAmountObj instanceof Double) {
                        compositionAmount = (Double) compositionAmountObj;
                    } else if (compositionAmountObj instanceof Number) {
                        compositionAmount = ((Number) compositionAmountObj).doubleValue();
                    }
                }

                notice.put("amountPayable", compositionAmount);

                // Get SUR parameter (same as regular Toppan letters)
                String surValue = getParameterValue("SUR", "CPC");
                Double surAmount = 0.0;
                if (surValue != null) {
                    try {
                        surAmount = Double.parseDouble(surValue);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid SUR parameter value: {}", surValue);
                    }
                }
                notice.put("surAmount", surAmount);
            }

            log.info("OPTIMIZED: Found {} valid Advisory Notice letters", notices.size());

            return notices;

        } catch (Exception e) {
            log.error("Error querying valid Advisory Notice letters (OPTIMIZED): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    }
