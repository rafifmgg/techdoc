package com.ocmsintranet.apiservice.testing.toppan.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for verification logic
 */
@Component
@Slf4j
public class VerificationHelper {

    // Notice numbers that should use checkSuccessFlow() verification
    private static final Set<String> SUCCESS_FLOW_NOTICES = Set.of(
        "500405000A", "500405001E", // ENA to RD1
        "500405006C", "500405007G", // RD1 to RD2
        "500405012C", "500405013G", // RD2 to RR3
        "500405030E", "500405031I", // DN1 to DN2
        "500405036G", "500405037K", // DN2 to DR3
        "500405046A", "500405047E" // have mha_reg
    );

    // Notice numbers that should use checkStayInCurrent() verification
    private static final Set<String> STAY_IN_CURRENT_NOTICES = Set.of(
        "500405002I", "500405003B", "500405004F", "500405005J",
        "500405008K", "500405009D", "500405010F", "500405011J",
        "500405014K", "500405015D", "500405016H", "500405017A",
        "500405032B", "500405033F", "500405034J", "500405035C",
        "500405038D", "500405039H", "500405040J", "500405041C",
        "500405042G", "500405043K", "500405044D"
    );

    // Notice numbers that should use checkStayInCurrent() verification
    private static final Set<String> SUSPEND_FLOW_NOTICES = Set.of(
        "500405045H",
        "500405048I", "500405049B", "500405050D", "500405051H", "500405052A", "500405053E",
        "500405056F", "500405057J", "500405058C", "500405059G", "500405060I", "500405061B",
        "500405066K", "500405067D"
    );

    // Stage transition rules: lastProcessingStage → expected nextProcessingStage
    private static final Map<String, String> STAGE_TRANSITION_RULES = Map.of(
        "RD1", "RD2",
        "RD2", "RR3",
        "RR3", "CPC",
        "DN1", "DN2",
        "DN2", "DR3",
        "DR3", "CPC"
    );

    // Stages that require driver particulars verification
    private static final Set<String> DRIVER_PARTICULARS_STAGES = Set.of("RD1", "RD2", "RR3");

    // Stages that require driver notice verification
    private static final Set<String> DRIVER_NOTICE_STAGES = Set.of("DN1", "DN2", "DR3");

    // Stages that require administration fee in financial calculation
    private static final Set<String> ADMIN_FEE_STAGES = Set.of("RR3", "DR3");

    private final ObjectMapper objectMapper;

    public VerificationHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Check if notice number should use success flow verification
     *
     * @param noticeNo Notice number to check
     * @return true if notice should use success flow
     */
    public boolean isSuccessFlowNotice(String noticeNo) {
        return SUCCESS_FLOW_NOTICES.contains(noticeNo);
    }

    /**
     * Check if notice number should use stay in current flow verification
     *
     * @param noticeNo Notice number to check
     * @return true if notice should use stay in current flow
     */
    public boolean isStayInCurrentNotice(String noticeNo) {
        return STAY_IN_CURRENT_NOTICES.contains(noticeNo);
    }

    /**
     * Check if notice number should use stay in current flow verification
     *
     * @param noticeNo Notice number to check
     * @return true if notice should use stay in current flow
     */
    public boolean isSuspendFlowNotice(String noticeNo) {
        return SUSPEND_FLOW_NOTICES.contains(noticeNo);
    }

    /**
     * Check if notice is in success download flow
     * Download flow notices: 500405006C, 500405007G, 500405030E, 500405031I
     *
     * @param noticeNo Notice number to check
     * @return true if notice is in download flow
     */
    public boolean isSuccessDownloadFlow(String noticeNo) {
        Set<String> downloadNotices = Set.of("500405006C", "500405007G", "500405030E", "500405031I");
        return downloadNotices.contains(noticeNo);
    }

    /**
     * Verify notice number success flow by checking required fields are null
     *
     * @param noticeNo Notice number to verify
     * @param showDetails Whether to include details field in result
     * @param context TestContext containing step3Data
     * @return Map with verification result including status with emoji icon
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkSuccessFlow(String noticeNo, boolean showDetails, TestContext context) {
        log.debug("Verifying notice number with success flow: {} (showDetails={})", noticeNo, showDetails);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);

        // Get scenario for this notice
        String scenario = getScenarioForNotice(noticeNo);
        result.put("scenario", scenario);

        // Step 1: Check if validOffenceNoticeData is available and valid
        Object validOffenceNoticeData = context.getValidOffenceNoticeData();
        if (!(validOffenceNoticeData instanceof List)) {
            result.put("status", "❌ error");
            result.put("message", "validOffenceNoticeData is not available");
            log.warn("Notice {}: validOffenceNoticeData is not a List", noticeNo);
            return result;
        }

        // Step 2: Find notice in validOffenceNoticeData
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) validOffenceNoticeData;
        Map<String, Object> noticeData = findNoticeInData(dataList, noticeNo);

        if (noticeData == null) {
            result.put("status", "❌ error");
            result.put("message", "Notice not found in validOffenceNoticeData");
            log.warn("Notice {}: Not found in validOffenceNoticeData", noticeNo);
            return result;
        }

        // Step 3: Build details with stage fields and verify required fields
        List<String> failedFields = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // Add stage fields to details first
        String prevStage = String.valueOf(noticeData.get("prevProcessingStage"));
        String lastStage = String.valueOf(noticeData.get("lastProcessingStage"));
        String nextStage = String.valueOf(noticeData.get("nextProcessingStage"));
        details.add("von.prevProcessingStage = " + prevStage);
        details.add("von.lastProcessingStage = " + lastStage);
        details.add("von.nextProcessingStage = " + nextStage);

        // Verify null fields
        String[] fieldsToCheck = {
            "suspensionType",
            "eprDateOfSuspension",
            "eprReasonOfSuspension",
            "crsDateOfSuspension",
            "crsReasonOfSuspension"
        };

        for (String field : fieldsToCheck) {
            Object value = noticeData.get(field);

            // Add detail entry showing field name and value
            String detailEntry = "von." + field + " = " + (value == null || isEmptyString(value) ? "null" : value.toString());
            details.add(detailEntry);

            // Field fails if value is not null and not empty string
            if (value != null && !isEmptyString(value)) {
                failedFields.add(field);
                log.debug("Notice {}: Field '{}' is not null (value: {})", noticeNo, field, value);
            }
        }

        // Add financial fields to details
        Object compositionAmount = noticeData.get("compositionAmount");
        Object administrationFee = noticeData.get("administrationFee");
        Object amountPayable = noticeData.get("amountPayable");

        details.add("von.compositionAmount = " + (compositionAmount == null ? "null" : compositionAmount.toString()));
        details.add("von.administrationFee = " + (administrationFee == null ? "null" : administrationFee.toString()));
        details.add("von.amountPayable = " + (amountPayable == null ? "null" : amountPayable.toString()));

        // Verify stage transition
        if (STAGE_TRANSITION_RULES.containsKey(lastStage)) {
            String expectedNextStage = STAGE_TRANSITION_RULES.get(lastStage);
            if (!expectedNextStage.equals(nextStage)) {
                // Stage transition mismatch - add to failed fields
                String stageMismatch = "stageTransition (expected " + lastStage +
                                     " → " + expectedNextStage + ", got " + nextStage + ")";
                failedFields.add(stageMismatch);
                log.debug("Notice {}: Stage transition mismatch. Expected {} → {}, got {}",
                         noticeNo, lastStage, expectedNextStage, nextStage);
            }
        }

        // Verify driver data based on last processing stage
        String driverVerificationMessage = null;

        if (DRIVER_PARTICULARS_STAGES.contains(lastStage)) {
            // Check in driverParticularsData
            Object driverParticularsData = context.getDriverParticularsData();

            if (!(driverParticularsData instanceof List)) {
                failedFields.add("driverParticularsData not available");
                log.debug("Notice {}: driverParticularsData is not available for stage {}", noticeNo, lastStage);
            } else {
                List<Map<String, Object>> driverDataList = (List<Map<String, Object>>) driverParticularsData;
                Map<String, Object> rdpData = findDriverDataByNoticeNo(driverDataList, noticeNo);

                if (rdpData != null) {
                    driverVerificationMessage = "driver particulars verified";
                    log.debug("Notice {}: Driver particulars found for stage {}", noticeNo, lastStage);

                    // Add RDP details
                    Object ownerName = rdpData.get("ownerName");
                    Object dateOfProcessing = rdpData.get("dateOfProcessing");
                    Object dateOfRdp = rdpData.get("dateOfRdp");
                    Object processingStage = rdpData.get("processingStage");
                    Object ownerNricNo = rdpData.get("ownerNricNo");
                    Object postalRegnNo = rdpData.get("postalRegnNo");

                    details.add("rdp.ownerName = " + (ownerName == null ? "null" : ownerName.toString()));
                    details.add("rdp.dateOfProcessing = " + (dateOfProcessing == null ? "null" : dateOfProcessing.toString()));
                    details.add("rdp.dateOfRdp = " + (dateOfRdp == null ? "null" : dateOfRdp.toString()));
                    details.add("rdp.processingStage = " + (processingStage == null ? "null" : processingStage.toString()));
                    details.add("rdp.ownerNricNo = " + (ownerNricNo == null ? "null" : ownerNricNo.toString()));
                    details.add("rdp.postalRegnNo = " + (postalRegnNo == null ? "null" : postalRegnNo.toString()));

                    // Verify postalRegnNo is not null
                    if (postalRegnNo == null) {
                        failedFields.add("rdp.postalRegnNo is null");
                        log.debug("Notice {}: rdp.postalRegnNo is null", noticeNo);
                    }
                } else {
                    failedFields.add("driver particulars not found");
                    log.debug("Notice {}: Driver particulars NOT found for stage {}", noticeNo, lastStage);
                }
            }
        } else if (DRIVER_NOTICE_STAGES.contains(lastStage)) {
            // Check in driverNoticeData
            Object driverNoticeData = context.getDriverNoticeData();

            if (!(driverNoticeData instanceof List)) {
                failedFields.add("driverNoticeData not available");
                log.debug("Notice {}: driverNoticeData is not available for stage {}", noticeNo, lastStage);
            } else {
                List<Map<String, Object>> driverDataList = (List<Map<String, Object>>) driverNoticeData;
                Map<String, Object> dnData = findDriverDataByNoticeNo(driverDataList, noticeNo);

                if (dnData != null) {
                    driverVerificationMessage = "driver notice verified";
                    log.debug("Notice {}: Driver notice found for stage {}", noticeNo, lastStage);

                    // Add DN details
                    Object driverName = dnData.get("driverName");
                    Object dateOfProcessing = dnData.get("dateOfProcessing");
                    Object dateOfDn = dnData.get("dateOfDn");
                    Object processingStage = dnData.get("processingStage");
                    Object driverNricNo = dnData.get("driverNricNo");
                    Object postalRegnNo = dnData.get("postalRegnNo");

                    details.add("dn.driverName = " + (driverName == null ? "null" : driverName.toString()));
                    details.add("dn.dateOfProcessing = " + (dateOfProcessing == null ? "null" : dateOfProcessing.toString()));
                    details.add("dn.dateOfDn = " + (dateOfDn == null ? "null" : dateOfDn.toString()));
                    details.add("dn.processingStage = " + (processingStage == null ? "null" : processingStage.toString()));
                    details.add("dn.driverNricNo = " + (driverNricNo == null ? "null" : driverNricNo.toString()));
                    details.add("dn.postalRegnNo = " + (postalRegnNo == null ? "null" : postalRegnNo.toString()));

                    // Verify postalRegnNo is not null
                    if (postalRegnNo == null) {
                        failedFields.add("dn.postalRegnNo is null");
                        log.debug("Notice {}: dn.postalRegnNo is null", noticeNo);
                    }
                } else {
                    failedFields.add("driver notice not found");
                    log.debug("Notice {}: Driver notice NOT found for stage {}", noticeNo, lastStage);
                }
            }
        }
        // For other stages (ENA, CPC, etc), skip driver verification

        // Verify financial calculation
        String financialVerificationMessage = null;
        try {
            // Parse financial values as BigDecimal for precise comparison
            BigDecimal compositionAmountValue = parseBigDecimal(compositionAmount);
            BigDecimal administrationFeeValue = parseBigDecimal(administrationFee);
            BigDecimal amountPayableValue = parseBigDecimal(amountPayable);

            if (compositionAmountValue != null && amountPayableValue != null) {
                if (ADMIN_FEE_STAGES.contains(lastStage)) {
                    // For RR3/DR3: compositionAmount + administrationFee = amountPayable
                    if (administrationFeeValue != null) {
                        BigDecimal calculatedAmount = compositionAmountValue.add(administrationFeeValue);
                        if (calculatedAmount.compareTo(amountPayableValue) == 0) {
                            financialVerificationMessage = "financial calculation verified (compositionAmount + administrationFee = amountPayable)";
                            log.debug("Notice {}: Financial calculation verified - {} + {} = {}",
                                     noticeNo, compositionAmountValue, administrationFeeValue, amountPayableValue);
                        } else {
                            failedFields.add("financial calculation mismatch: compositionAmount (" + compositionAmountValue +
                                           ") + administrationFee (" + administrationFeeValue +
                                           ") ≠ amountPayable (" + amountPayableValue + ")");
                            log.debug("Notice {}: Financial calculation failed - {} + {} ≠ {}",
                                     noticeNo, compositionAmountValue, administrationFeeValue, amountPayableValue);
                        }
                    } else {
                        failedFields.add("administrationFee is null for stage " + lastStage);
                        log.debug("Notice {}: administrationFee is null for stage {}", noticeNo, lastStage);
                    }
                } else {
                    // For other stages: compositionAmount = amountPayable
                    if (compositionAmountValue.compareTo(amountPayableValue) == 0) {
                        financialVerificationMessage = "financial calculation verified (compositionAmount = amountPayable)";
                        log.debug("Notice {}: Financial calculation verified - {} = {}",
                                 noticeNo, compositionAmountValue, amountPayableValue);
                    } else {
                        failedFields.add("financial calculation mismatch: compositionAmount (" + compositionAmountValue +
                                       ") ≠ amountPayable (" + amountPayableValue + ")");
                        log.debug("Notice {}: Financial calculation failed - {} ≠ {}",
                                 noticeNo, compositionAmountValue, amountPayableValue);
                    }
                }
            } else {
                // Missing financial data
                if (compositionAmountValue == null) {
                    failedFields.add("compositionAmount is null or invalid");
                }
                if (amountPayableValue == null) {
                    failedFields.add("amountPayable is null or invalid");
                }
                log.debug("Notice {}: Missing financial data for verification", noticeNo);
            }
        } catch (Exception e) {
            failedFields.add("financial calculation error: " + e.getMessage());
            log.error("Notice {}: Error during financial calculation: {}", noticeNo, e.getMessage(), e);
        }

        // Verify stage date processing using STAGEDAYS parameter
        String stageDateVerificationMessage = null;
        try {
            // Get date fields from noticeData
            Object lastProcessingDate = noticeData.get("lastProcessingDate");
            Object nextProcessingDate = noticeData.get("nextProcessingDate");

            // Add date fields to details
            details.add("von.lastProcessingDate = " + (lastProcessingDate == null ? "null" : lastProcessingDate.toString()));
            details.add("von.nextProcessingDate = " + (nextProcessingDate == null ? "null" : nextProcessingDate.toString()));

            // Find STAGEDAYS parameter for current lastProcessingStage
            String stageDaysValue = findParameterValue("STAGEDAYS", lastStage, context);

            if (stageDaysValue == null) {
                // Required: parameter must exist
                failedFields.add("STAGEDAYS parameter not found for stage " + lastStage);
                log.debug("Notice {}: STAGEDAYS parameter not found for stage {}", noticeNo, lastStage);
            } else {
                // Parse stage days as integer
                int stageDays = Integer.parseInt(stageDaysValue);
                log.debug("Notice {}: STAGEDAYS for stage {} = {} days", noticeNo, lastStage, stageDays);

                // Parse dates
                LocalDate lastProcDate = parseDate(lastProcessingDate);
                LocalDate nextProcDate = parseDate(nextProcessingDate);

                if (lastProcDate == null || nextProcDate == null) {
                    if (lastProcDate == null) {
                        failedFields.add("lastProcessingDate is null or invalid");
                    }
                    if (nextProcDate == null) {
                        failedFields.add("nextProcessingDate is null or invalid");
                    }
                    log.debug("Notice {}: Missing or invalid date fields for stage date verification", noticeNo);
                } else {
                    // Calculate expected next processing date
                    LocalDate expectedNextDate = lastProcDate.plusDays(stageDays);

                    // Exact match comparison
                    if (expectedNextDate.equals(nextProcDate)) {
                        stageDateVerificationMessage = "stage date verified (lastProcessingDate + " + stageDays + " days = nextProcessingDate)";
                        log.debug("Notice {}: Stage date verified - {} + {} days = {}",
                                 noticeNo, lastProcDate, stageDays, nextProcDate);
                    } else {
                        failedFields.add("stage date mismatch: expected " + expectedNextDate + " (lastProcessingDate + " + stageDays + " days), got " + nextProcDate);
                        log.debug("Notice {}: Stage date mismatch - expected {}, got {}",
                                 noticeNo, expectedNextDate, nextProcDate);
                    }
                }
            }
        } catch (NumberFormatException e) {
            failedFields.add("invalid STAGEDAYS value: " + e.getMessage());
            log.error("Notice {}: Invalid STAGEDAYS value: {}", noticeNo, e.getMessage());
        } catch (Exception e) {
            failedFields.add("stage date verification error: " + e.getMessage());
            log.error("Notice {}: Error during stage date verification: {}", noticeNo, e.getMessage(), e);
        }

        // Step 4: Build result based on validation
        // Only include details field if showDetails is true
        if (showDetails) {
            result.put("details", details);
        }

        if (failedFields.isEmpty()) {
            result.put("status", "✅ success");

            // Build success message with stage transition info
            String successMessage = "No suspension - verified";
            if (STAGE_TRANSITION_RULES.containsKey(lastStage)) {
                String expectedNextStage = STAGE_TRANSITION_RULES.get(lastStage);
                successMessage += ", stageTransition (expected " + lastStage + " → " + expectedNextStage + ") - verified";
            }

            // Add driver verification message if applicable
            if (driverVerificationMessage != null) {
                successMessage += ", " + driverVerificationMessage;
            }

            // Add financial verification message if applicable
            if (financialVerificationMessage != null) {
                successMessage += ", " + financialVerificationMessage;
            }

            // Add stage date verification message if applicable
            if (stageDateVerificationMessage != null) {
                successMessage += ", " + stageDateVerificationMessage;
            }

            result.put("message", successMessage);
            log.info("Notice {}: Verification SUCCESS - {}", noticeNo, successMessage);
        } else {
            result.put("status", "❌ failed");
            result.put("message", "Field validation failed: " + String.join(", ", failedFields));
            result.put("failed_fields", failedFields);
            log.warn("Notice {}: Verification FAILED - fields not null: {}", noticeNo, failedFields);
        }

        return result;
    }

    /**
     * Verify notices that stay in current stage
     *
     * @param noticeNo Notice number to verify
     * @param showDetails Whether to include details field in result
     * @param context TestContext containing step3Data
     * @return Map with verification result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkStayInCurrent(String noticeNo, boolean showDetails, TestContext context) {
        log.debug("Verifying notice number with stay in current flow: {} (showDetails={})", noticeNo, showDetails);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);

        // Get scenario for this notice
        String scenario = getScenarioForNotice(noticeNo);
        result.put("scenario", scenario);

        // Step 1: Check if validOffenceNoticeData is available and valid
        Object validOffenceNoticeData = context.getValidOffenceNoticeData();
        if (!(validOffenceNoticeData instanceof List)) {
            result.put("status", "❌ error");
            result.put("message", "validOffenceNoticeData is not available");
            log.warn("Notice {}: validOffenceNoticeData is not a List", noticeNo);
            return result;
        }

        // Step 2: Find notice in validOffenceNoticeData
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) validOffenceNoticeData;
        Map<String, Object> noticeData = findNoticeInData(dataList, noticeNo);

        if (noticeData == null) {
            result.put("status", "❌ error");
            result.put("message", "Notice not found in validOffenceNoticeData");
            log.warn("Notice {}: Not found in validOffenceNoticeData", noticeNo);
            return result;
        }

        // Step 3: Build details with all required fields (display only, no verification)
        List<String> details = new ArrayList<>();

        // Add stage fields
        String prevStage = String.valueOf(noticeData.get("prevProcessingStage"));
        String lastStage = String.valueOf(noticeData.get("lastProcessingStage"));
        String nextStage = String.valueOf(noticeData.get("nextProcessingStage"));
        details.add("von.prevProcessingStage = " + prevStage);
        details.add("von.lastProcessingStage = " + lastStage);
        details.add("von.nextProcessingStage = " + nextStage);

        // Add suspension fields
        String[] fieldsToDisplay = {
            "suspensionType",
            "eprDateOfSuspension",
            "eprReasonOfSuspension",
            "crsDateOfSuspension",
            "crsReasonOfSuspension"
        };

        for (String field : fieldsToDisplay) {
            Object value = noticeData.get(field);
            String detailEntry = "von." + field + " = " + (value == null || isEmptyString(value) ? "null" : value.toString());
            details.add(detailEntry);
        }

        // Step 4: Conditionally add details field
        if (showDetails) {
            result.put("details", details);
        }

        // Step 5: Return success
        result.put("status", "✅ success");
        result.put("message", "Notice stay in current last processing stage");

        log.info("Notice {}: Stay in current verification completed", noticeNo);

        return result;
    }

    /**
     * Verify notices with suspend flow
     * Validates:
     * - suspensionType must not be null/empty
     * - Either EPR or CRS suspension fields must be filled (XOR validation)
     *   - If eprDateOfSuspension & eprReasonOfSuspension are both null, then crsDateOfSuspension & crsReasonOfSuspension must not be null
     *   - If crsDateOfSuspension & crsReasonOfSuspension are both null, then eprDateOfSuspension & eprReasonOfSuspension must not be null
     *
     * @param noticeNo Notice number to verify
     * @param showDetails Whether to include details field in result
     * @param context TestContext containing step3Data
     * @return Map with verification result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkSuspendFlow(String noticeNo, boolean showDetails, TestContext context) {
        log.debug("Verifying notice number with suspend flow: {} (showDetails={})", noticeNo, showDetails);

        Map<String, Object> result = new HashMap<>();
        result.put("notice_no", noticeNo);

        // Get scenario for this notice
        String scenario = getScenarioForNotice(noticeNo);
        result.put("scenario", scenario);

        // Step 1: Check if validOffenceNoticeData is available and valid
        Object validOffenceNoticeData = context.getValidOffenceNoticeData();
        if (!(validOffenceNoticeData instanceof List)) {
            result.put("status", "❌ error");
            result.put("message", "validOffenceNoticeData is not available");
            log.warn("Notice {}: validOffenceNoticeData is not a List", noticeNo);
            return result;
        }

        // Step 2: Find notice in validOffenceNoticeData
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) validOffenceNoticeData;
        Map<String, Object> noticeData = findNoticeInData(dataList, noticeNo);

        if (noticeData == null) {
            result.put("status", "❌ error");
            result.put("message", "Notice not found in validOffenceNoticeData");
            log.warn("Notice {}: Not found in validOffenceNoticeData", noticeNo);
            return result;
        }

        // Step 3: Build details with stage fields and suspension fields
        List<String> failedFields = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // Add stage fields to details
        String prevStage = String.valueOf(noticeData.get("prevProcessingStage"));
        String lastStage = String.valueOf(noticeData.get("lastProcessingStage"));
        String nextStage = String.valueOf(noticeData.get("nextProcessingStage"));
        details.add("von.prevProcessingStage = " + prevStage);
        details.add("von.lastProcessingStage = " + lastStage);
        details.add("von.nextProcessingStage = " + nextStage);

        // Get suspension fields
        Object suspensionType = noticeData.get("suspensionType");
        Object eprDateOfSuspension = noticeData.get("eprDateOfSuspension");
        Object eprReasonOfSuspension = noticeData.get("eprReasonOfSuspension");
        Object crsDateOfSuspension = noticeData.get("crsDateOfSuspension");
        Object crsReasonOfSuspension = noticeData.get("crsReasonOfSuspension");

        // Add suspension fields to details
        details.add("von.suspensionType = " + (suspensionType == null || isEmptyString(suspensionType) ? "null" : suspensionType.toString()));
        details.add("von.eprDateOfSuspension = " + (eprDateOfSuspension == null || isEmptyString(eprDateOfSuspension) ? "null" : eprDateOfSuspension.toString()));
        details.add("von.eprReasonOfSuspension = " + (eprReasonOfSuspension == null || isEmptyString(eprReasonOfSuspension) ? "null" : eprReasonOfSuspension.toString()));
        details.add("von.crsDateOfSuspension = " + (crsDateOfSuspension == null || isEmptyString(crsDateOfSuspension) ? "null" : crsDateOfSuspension.toString()));
        details.add("von.crsReasonOfSuspension = " + (crsReasonOfSuspension == null || isEmptyString(crsReasonOfSuspension) ? "null" : crsReasonOfSuspension.toString()));

        // Step 4: Validate suspension fields

        // Validate suspensionType is not null
        if (suspensionType == null || isEmptyString(suspensionType)) {
            failedFields.add("suspensionType is null or empty");
            log.debug("Notice {}: suspensionType is null or empty", noticeNo);
        }

        // Check if EPR group is filled (both fields must be non-null)
        boolean eprFilled = (eprDateOfSuspension != null && !isEmptyString(eprDateOfSuspension)) &&
                           (eprReasonOfSuspension != null && !isEmptyString(eprReasonOfSuspension));

        // Check if CRS group is filled (both fields must be non-null)
        boolean crsFilled = (crsDateOfSuspension != null && !isEmptyString(crsDateOfSuspension)) &&
                           (crsReasonOfSuspension != null && !isEmptyString(crsReasonOfSuspension));

        // XOR validation: at least one group must be filled
        if (!eprFilled && !crsFilled) {
            failedFields.add("Either EPR (eprDateOfSuspension & eprReasonOfSuspension) or CRS (crsDateOfSuspension & crsReasonOfSuspension) must be filled");
            log.debug("Notice {}: Neither EPR nor CRS suspension group is filled", noticeNo);
        }

        // Step 5: Conditionally add details field
        if (showDetails) {
            result.put("details", details);
        }

        // Step 6: Build result based on validation
        if (failedFields.isEmpty()) {
            result.put("status", "✅ success");

            // Build success message with suspension info
            String successMessage = "Suspension verified - suspensionType present";
            if (eprFilled && crsFilled) {
                successMessage += ", both EPR and CRS filled";
            } else if (eprFilled) {
                successMessage += ", EPR suspension filled";
            } else if (crsFilled) {
                successMessage += ", CRS suspension filled";
            }

            result.put("message", successMessage);
            log.info("Notice {}: Suspend flow verification SUCCESS - {}", noticeNo, successMessage);
        } else {
            result.put("status", "❌ failed");
            result.put("message", "Suspend flow validation failed: " + String.join(", ", failedFields));
            result.put("failed_fields", failedFields);
            log.warn("Notice {}: Suspend flow verification FAILED - {}", noticeNo, failedFields);
        }

        return result;
    }

    /**
     * Find notice data in the data list by notice number
     *
     * @param dataList List of notice data from Step 3
     * @param noticeNo Notice number to find
     * @return Notice data map if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findNoticeInData(List<Map<String, Object>> dataList, String noticeNo) {
        for (Map<String, Object> item : dataList) {
            Object itemNoticeNo = item.get("noticeNo");
            if (noticeNo.equals(itemNoticeNo)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Find notice by noticeNo in driver data list
     *
     * @param dataList Driver data list (driverParticularsData or driverNoticeData)
     * @param noticeNo Notice number to find
     * @return true if found, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean findNoticeInDriverData(List<Map<String, Object>> dataList, String noticeNo) {
        if (dataList == null) {
            return false;
        }

        for (Map<String, Object> item : dataList) {
            Object itemNoticeNo = item.get("noticeNo");
            if (noticeNo.equals(itemNoticeNo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find driver data by notice number and return the full data object
     *
     * @param dataList List of driver data (RDP or DN)
     * @param noticeNo Notice number to search for
     * @return Driver data Map if found, null otherwise
     */
    private Map<String, Object> findDriverDataByNoticeNo(List<Map<String, Object>> dataList, String noticeNo) {
        if (dataList == null) {
            return null;
        }

        for (Map<String, Object> item : dataList) {
            Object itemNoticeNo = item.get("noticeNo");
            if (noticeNo.equals(itemNoticeNo)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Check if value is an empty string
     *
     * @param value Object to check
     * @return true if value is empty string, false otherwise
     */
    private boolean isEmptyString(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            return str.isEmpty() || str.trim().isEmpty();
        }
        return false;
    }

    /**
     * Parse object value to BigDecimal for financial calculations
     *
     * @param value Object to parse (can be Number, String, or null)
     * @return BigDecimal value, or null if parsing fails or value is null
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }
                return new BigDecimal(strValue);
            }
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from value: {} ({})", value, e.getMessage());
        }

        return null;
    }

    /**
     * Find parameter value from parameter list by parameterId and code
     *
     * @param parameterId Parameter ID to search for (e.g., "STAGEDAYS")
     * @param code Code to match (e.g., lastProcessingStage value)
     * @param context TestContext containing parameterListData
     * @return Parameter value as String, or null if not found
     */
    @SuppressWarnings("unchecked")
    private String findParameterValue(String parameterId, String code, TestContext context) {
        try {
            Object parameterListData = context.getParameterListData();

            if (!(parameterListData instanceof List)) {
                log.debug("parameterListData is not a List");
                return null;
            }

            List<Map<String, Object>> parameterList = (List<Map<String, Object>>) parameterListData;

            for (Map<String, Object> parameter : parameterList) {
                Object paramIdObj = parameter.get("parameterId");
                Object codeObj = parameter.get("code");
                Object valueObj = parameter.get("value");

                if (parameterId.equals(paramIdObj) && code.equals(codeObj)) {
                    log.debug("Found parameter: parameterId={}, code={}, value={}", parameterId, code, valueObj);
                    return valueObj != null ? valueObj.toString() : null;
                }
            }

            log.debug("Parameter not found: parameterId={}, code={}", parameterId, code);
            return null;

        } catch (Exception e) {
            log.warn("Error finding parameter value: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse object to LocalDate for date comparison
     * Handles String (ISO format), java.sql.Date, java.util.Date, LocalDate, Timestamp
     *
     * @param value Object to parse
     * @return LocalDate, or null if parsing fails
     */
    private LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof LocalDate) {
                return (LocalDate) value;
            } else if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            } else if (value instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
            } else if (value instanceof java.util.Date) {
                return LocalDate.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }

                // Try parsing as LocalDateTime first (handles ISO datetime with time component)
                // Formats: 2025-10-22T16:25:26.7170669 or 2025-10-26T00:00:00
                if (strValue.contains("T")) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(strValue);
                        return dateTime.toLocalDate(); // Extract date part only
                    } catch (Exception e) {
                        // If parsing as LocalDateTime fails, try with fractional seconds handling
                        try {
                            // Remove microseconds beyond milliseconds (7+ digits after decimal point)
                            // Example: 2025-10-22T16:25:26.7170669 -> 2025-10-22T16:25:26.717
                            String normalized = strValue.replaceAll("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\d+", "$1");
                            LocalDateTime dateTime = LocalDateTime.parse(normalized);
                            return dateTime.toLocalDate();
                        } catch (Exception ex) {
                            log.debug("Failed to parse datetime with time component: {}", strValue);
                            // Fall through to try date-only parsing
                        }
                    }
                }

                // Fallback: Try parsing ISO format (yyyy-MM-dd) for date-only strings
                return LocalDate.parse(strValue, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.warn("Failed to parse LocalDate from value: {} ({})", value, e.getMessage());
        }

        return null;
    }

    /**
     * Get scenario description for a notice number
     *
     * @param noticeNo Notice number to get scenario for
     * @return Scenario description or "Unknown scenario" if not found
     */
    private String getScenarioForNotice(String noticeNo) {
        try {
            // Read scenario.json file
            String scenarioFilePath = "src/main/java/com/ocmsintranet/apiservice/testing/toppan/data/scenario.json";
            File scenarioFile = new File(scenarioFilePath);

            if (!scenarioFile.exists()) {
                log.warn("Scenario file not found at: {}", scenarioFilePath);
                return "Unknown scenario";
            }

            // Parse JSON file
            List<Map<String, Object>> scenarios = objectMapper.readValue(
                scenarioFile,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Find scenario with matching notice_no
            for (Map<String, Object> scenarioItem : scenarios) {
                String scenarioNoticeNo = (String) scenarioItem.get("notice_no");
                if (noticeNo.equals(scenarioNoticeNo)) {
                    return (String) scenarioItem.get("scenario");
                }
            }

            log.warn("Scenario not found for notice number: {}", noticeNo);
            return "Unknown scenario";

        } catch (Exception e) {
            log.warn("Failed to get scenario for notice {}: {}", noticeNo, e.getMessage());
            return "Unknown scenario";
        }
    }
}
