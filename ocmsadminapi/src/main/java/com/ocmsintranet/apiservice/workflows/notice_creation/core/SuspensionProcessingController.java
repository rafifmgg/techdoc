package com.ocmsintranet.apiservice.workflows.notice_creation.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReason;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.sequence.SequenceService;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.PermanentRevivalHelper;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.PermanentSuspensionHelper;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.SuspensionProcessingHelper;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.TemporaryRevivalHelper;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.TemporarySuspensionHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Suspension Processing Controller
 * Handles suspension and revival operations for both TS (Temporary Suspension) and PS (Permanent Suspension)
 *
 * Suspension Endpoints:
 * 1. POST /staff-apply-suspension - OCMS Staff manual suspension
 * 2. POST /plus-apply-suspension - PLUS Staff manual suspension
 * 3. POST /apply-suspension - OCMS Backend auto suspension (internal)
 *
 * Revival Endpoints:
 * 4. POST /staff-revive-suspension - OCMS Staff manual revival
 * 5. POST /plus-revive-suspension - PLUS Staff manual revival
 *
 * @author Claude Code
 * @since 2025-01-19 (OCMS 17 & 18)
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class SuspensionProcessingController {

    @Autowired
    private TemporarySuspensionHelper tsHelper;

    @Autowired
    private PermanentSuspensionHelper psHelper;

    @Autowired
    private TemporaryRevivalHelper tsRevivalHelper;

    @Autowired
    private PermanentRevivalHelper psRevivalHelper;

    @Autowired
    private SuspensionProcessingHelper suspensionProcessingHelper;

    @Autowired
    private OcmsSuspensionReasonService suspensionReasonService;

    @Autowired
    private SequenceService sequenceService;

    /**
     * Get Suspension Codes - For UI Dropdowns
     *
     * POST Request Body:
     * {
     *   "suspensionType": "TS" or "PS" (optional - if not provided, returns all),
     *   "source": "OCMS" or "PLUS" (optional - if not provided, returns all)
     * }
     *
     * Response:
     * {
     *   "appCode": "OCMS-2000",
     *   "message": "Success",
     *   "suspensionCodes": [
     *     {
     *       "suspensionType": "TS",
     *       "reasonOfSuspension": "ACR",
     *       "description": "ACRA deregistered/director not found",
     *       "noOfDaysForRevival": 21,
     *       "status": "A"
     *     },
     *     ...
     *   ]
     * }
     */
    @PostMapping("/suspension-codes")
    public ResponseEntity<?> getSuspensionCodes(@RequestBody(required = false) Map<String, Object> request) {
        log.info("Get suspension codes request: {}", request);

        try {
            // Extract filter parameters
            String suspensionType = null;
            String source = null;

            if (request != null) {
                suspensionType = (String) request.get("suspensionType");
                source = (String) request.get("source");
            }

            // Get all suspension reasons from database
            List<OcmsSuspensionReason> allReasons = suspensionReasonService.getAll(new HashMap<>()).getData();

            // Filter based on parameters
            List<Map<String, Object>> filteredCodes = new ArrayList<>();

            for (OcmsSuspensionReason reason : allReasons) {
                // Skip inactive codes
                if (!"A".equals(reason.getStatus())) {
                    continue;
                }

                // Filter by suspension type if provided
                if (suspensionType != null && !suspensionType.equals(reason.getId().getSuspensionType())) {
                    continue;
                }

                // Filter by source if provided (based on code permissions)
                if (source != null) {
                    String code = reason.getId().getReasonOfSuspension();
                    boolean isOcmsAllowed = isOcmsCode(code);
                    boolean isPlusAllowed = isPlusCode(code);

                    if ("OCMS".equals(source) && !isOcmsAllowed) {
                        continue;
                    }
                    if ("PLUS".equals(source) && !isPlusAllowed) {
                        continue;
                    }
                }

                // Build response object
                Map<String, Object> codeInfo = new HashMap<>();
                codeInfo.put("suspensionType", reason.getId().getSuspensionType());
                codeInfo.put("reasonOfSuspension", reason.getId().getReasonOfSuspension());
                codeInfo.put("description", reason.getDescription());
                codeInfo.put("noOfDaysForRevival", reason.getNoOfDaysForRevival());
                codeInfo.put("status", reason.getStatus());

                filteredCodes.add(codeInfo);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("appCode", "OCMS-2000");
            response.put("message", "Success");
            response.put("suspensionCodes", filteredCodes);
            response.put("totalCount", filteredCodes.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting suspension codes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * Check if code is allowed for OCMS source
     * OCMS can use: ACR, CLV, FPL, HST, INS, MS, NRO, OLD, OUT, PAM, PDP, PRI, ROV, SYS, UNC
     */
    private boolean isOcmsCode(String code) {
        List<String> ocmsCodes = List.of(
            "ACR", "CLV", "FPL", "HST", "INS", "MS", "NRO", "OLD",
            "OUT", "PAM", "PDP", "PRI", "ROV", "SYS", "UNC"
        );
        return ocmsCodes.contains(code);
    }

    /**
     * Check if code is allowed for PLUS source
     * PLUS can use: APE, APP, CCE, MS, PRI, RED
     */
    private boolean isPlusCode(String code) {
        List<String> plusCodes = List.of("APE", "APP", "CCE", "MS", "PRI", "RED");
        return plusCodes.contains(code);
    }

    /**
     * OCMS Staff Portal - Manual Suspension (TS/PS)
     *
     * Payload:
     * {
     *   "noticeNo": ["500500303J", "500500304J"],
     *   "suspensionType": "TS" or "PS",
     *   "reasonOfSuspension": "APE",
     *   "daysToRevive": 30,  // Optional, only for TS
     *   "suspensionRemarks": "Appeal pending",
     *   "suspensionSource": "<OCMS Code>",
     *   "officerAuthorisingSuspension": "JOHNLEE"
     * }
     */
    @PostMapping("/staff-apply-suspension")
    @Transactional
    public ResponseEntity<?> staffApplySuspension(@RequestBody Map<String, Object> payload) {
        log.info("OCMS Staff suspension request: {}", payload);

        try {
            // Extract common fields
            @SuppressWarnings("unchecked")
            List<String> noticeNos = (List<String>) payload.get("noticeNo");
            String suspensionType = (String) payload.get("suspensionType");
            String reasonOfSuspension = (String) payload.get("reasonOfSuspension");
            String suspensionRemarks = (String) payload.get("suspensionRemarks");
            String officerAuthorisingSuspension = (String) payload.get("officerAuthorisingSuspension");
            String caseNo = (String) payload.getOrDefault("caseNo", "");

            // Force source to OCMS
            String suspensionSource = SystemConstant.Subsystem.OCMS_CODE;

            // Process each notice (checking=false for staff portal)
            List<Map<String, Object>> results = processNoticeList(
                noticeNos, suspensionType, reasonOfSuspension, suspensionRemarks,
                officerAuthorisingSuspension, suspensionSource, caseNo, payload, false
            );

            return ResponseEntity.ok(createBatchResponse(results));

        } catch (Exception e) {
            log.error("Error in staff-apply-suspension: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * PLUS Staff Portal - Manual Suspension (TS/PS)
     *
     * Query Parameter:
     * - checking=true: Validate eligibility without executing (dry-run)
     * - checking=false or not provided: Execute suspension
     *
     * Payload:
     * {
     *   "noticeNo": ["500500303J"],
     *   "caseNo": "P250113001",
     *   "suspensionType": "TS" or "PS",
     *   "reasonOfSuspension": "APP",
     *   "daysToRevive": 30,  // Optional, only for TS
     *   "suspensionRemarks": "New appeal",
     *   "suspensionSource": "<PLUS Code>",
     *   "officerAuthorisingSuspension": "PLU_1"
     * }
     */
    @PostMapping("/plus-apply-suspension")
    @Transactional
    public ResponseEntity<?> plusApplySuspension(
            @RequestBody Map<String, Object> payload,
            @RequestParam(name = "checking", required = false, defaultValue = "false") boolean checking) {
        log.info("PLUS Staff suspension request (checking={}): {}", checking, payload);

        try {
            // Extract common fields
            @SuppressWarnings("unchecked")
            List<String> noticeNos = (List<String>) payload.get("noticeNo");
            String suspensionType = (String) payload.get("suspensionType");
            String reasonOfSuspension = (String) payload.get("reasonOfSuspension");
            String suspensionRemarks = (String) payload.get("suspensionRemarks");
            String officerAuthorisingSuspension = (String) payload.get("officerAuthorisingSuspension");
            String caseNo = (String) payload.getOrDefault("caseNo", "");

            // Force source to PLUS
            String suspensionSource = SystemConstant.Subsystem.PLUS_CODE;

            // Process each notice
            List<Map<String, Object>> results = processNoticeList(
                noticeNos, suspensionType, reasonOfSuspension, suspensionRemarks,
                officerAuthorisingSuspension, suspensionSource, caseNo, payload, checking
            );

            return ResponseEntity.ok(createBatchResponse(results));

        } catch (Exception e) {
            log.error("Error in plus-apply-suspension: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * OCMS Backend - Auto Suspension (TS/PS) - Internal Only
     *
     * Payload:
     * {
     *   "noticeNo": ["500500303J"],
     *   "suspensionType": "TS" or "PS",
     *   "reasonOfSuspension": "ANS",
     *   "daysToRevive": 30,  // Optional, only for TS
     *   "suspensionRemarks": "Auto suspension",
     *   "suspensionSource": "<OCMS Code>",
     *   "officerAuthorisingSuspension": "ocmsizmgr"
     * }
     */
    @PostMapping("/apply-suspension")
    @Transactional
    public ResponseEntity<?> applySuspension(@RequestBody Map<String, Object> payload) {
        log.info("OCMS Backend suspension request: {}", payload);

        try {
            // Extract common fields
            @SuppressWarnings("unchecked")
            List<String> noticeNos = (List<String>) payload.get("noticeNo");
            String suspensionType = (String) payload.get("suspensionType");
            String reasonOfSuspension = (String) payload.get("reasonOfSuspension");
            String suspensionRemarks = (String) payload.get("suspensionRemarks");
            String officerAuthorisingSuspension = (String) payload.get("officerAuthorisingSuspension");
            String suspensionSource = (String) payload.getOrDefault("suspensionSource", SystemConstant.Subsystem.OCMS_CODE);
            String caseNo = (String) payload.getOrDefault("caseNo", "");

            // Process each notice (checking=false for backend/cron)
            List<Map<String, Object>> results = processNoticeList(
                noticeNos, suspensionType, reasonOfSuspension, suspensionRemarks,
                officerAuthorisingSuspension, suspensionSource, caseNo, payload, false
            );

            return ResponseEntity.ok(createBatchResponse(results));

        } catch (Exception e) {
            log.error("Error in apply-suspension: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * OCMS Staff Portal - Manual Revival (TS/PS)
     *
     * Payload:
     * {
     *   "noticeNo": ["500500303J", "500500304J"],
     *   "suspensionType": "TS" or "PS",
     *   "revivalReason": "MAN",
     *   "revivalRemarks": "Issue resolved",
     *   "officerAuthorisingRevival": "JOHNLEE"
     * }
     */
    @PostMapping("/staff-revive-suspension")
    @Transactional
    public ResponseEntity<?> staffReviveSuspension(@RequestBody Map<String, Object> payload) {
        log.info("OCMS Staff revival request: {}", payload);

        try {
            // Extract common fields
            @SuppressWarnings("unchecked")
            List<String> noticeNos = (List<String>) payload.get("noticeNo");
            String suspensionType = (String) payload.get("suspensionType");
            String revivalReason = (String) payload.get("revivalReason");
            String revivalRemarks = (String) payload.get("revivalRemarks");
            String officerAuthorisingRevival = (String) payload.get("officerAuthorisingRevival");

            // Process each notice
            List<Map<String, Object>> results = processRevivalList(
                noticeNos, suspensionType, revivalReason, revivalRemarks, officerAuthorisingRevival
            );

            return ResponseEntity.ok(createBatchResponse(results));

        } catch (Exception e) {
            log.error("Error in staff-revive-suspension: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * PLUS Staff Portal - Manual Revival (TS/PS)
     *
     * Payload:
     * {
     *   "noticeNo": ["500500303J"],
     *   "suspensionType": "TS" or "PS",
     *   "revivalReason": "MAN",
     *   "revivalRemarks": "Appeal resolved",
     *   "officerAuthorisingRevival": "PLU_1"
     * }
     */
    @PostMapping("/plus-revive-suspension")
    @Transactional
    public ResponseEntity<?> plusReviveSuspension(@RequestBody Map<String, Object> payload) {
        log.info("PLUS Staff revival request: {}", payload);

        try {
            // Extract common fields
            @SuppressWarnings("unchecked")
            List<String> noticeNos = (List<String>) payload.get("noticeNo");
            String suspensionType = (String) payload.get("suspensionType");
            String revivalReason = (String) payload.get("revivalReason");
            String revivalRemarks = (String) payload.get("revivalRemarks");
            String officerAuthorisingRevival = (String) payload.get("officerAuthorisingRevival");

            // Process each notice
            List<Map<String, Object>> results = processRevivalList(
                noticeNos, suspensionType, revivalReason, revivalRemarks, officerAuthorisingRevival
            );

            return ResponseEntity.ok(createBatchResponse(results));

        } catch (Exception e) {
            log.error("Error in plus-revive-suspension: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("OCMS-4007", "System error. Please inform Administrator"));
        }
    }

    /**
     * Process a list of notices for revival
     */
    private List<Map<String, Object>> processRevivalList(
            List<String> noticeNos, String suspensionType, String revivalReason,
            String revivalRemarks, String officerAuthorisingRevival) {

        List<Map<String, Object>> results = new ArrayList<>();

        if (noticeNos == null || noticeNos.isEmpty()) {
            results.add(createErrorResponse("OCMS-4001", "Notice number list is empty"));
            return results;
        }

        for (String noticeNo : noticeNos) {
            Map<String, Object> result = processSingleRevival(
                noticeNo, suspensionType, revivalReason, revivalRemarks, officerAuthorisingRevival
            );

            results.add(result);
        }

        return results;
    }

    /**
     * Process a single notice for revival
     * Routes to TS or PS revival helper based on suspensionType
     */
    private Map<String, Object> processSingleRevival(
            String noticeNo, String suspensionType, String revivalReason,
            String revivalRemarks, String officerAuthorisingRevival) {

        try {
            // Route to appropriate helper based on suspension type
            if ("TS".equals(suspensionType)) {
                return tsRevivalHelper.reviveTS(noticeNo, revivalReason,
                    revivalRemarks, officerAuthorisingRevival);

            } else if ("PS".equals(suspensionType)) {
                return psRevivalHelper.revivePS(noticeNo, revivalReason,
                    revivalRemarks, officerAuthorisingRevival);

            } else {
                return createErrorResponse("OCMS-4007", "Invalid Suspension Type: " + suspensionType);
            }

        } catch (Exception e) {
            log.error("Error processing revival for notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse("OCMS-4007", "System error processing revival for notice: " + noticeNo);
        }
    }

    /**
     * Process a list of notices for suspension
     */
    private List<Map<String, Object>> processNoticeList(
            List<String> noticeNos, String suspensionType, String reasonOfSuspension,
            String suspensionRemarks, String officerAuthorisingSuspension,
            String suspensionSource, String caseNo, Map<String, Object> payload, boolean checking) {

        List<Map<String, Object>> results = new ArrayList<>();

        if (noticeNos == null || noticeNos.isEmpty()) {
            results.add(createErrorResponse("OCMS-4001", "Notice number list is empty"));
            return results;
        }

        // Generate SR numbers for batch processing
        int baseSrNo = generateBaseSrNo();

        // Extract daysToRevive (optional, only for TS)
        Integer daysToRevive = null;
        if (payload.containsKey("daysToRevive")) {
            Object daysObj = payload.get("daysToRevive");
            if (daysObj instanceof Integer) {
                daysToRevive = (Integer) daysObj;
            } else if (daysObj instanceof String) {
                try {
                    daysToRevive = Integer.parseInt((String) daysObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid daysToRevive format: {}", daysObj);
                }
            }
        }

        for (int i = 0; i < noticeNos.size(); i++) {
            String noticeNo = noticeNos.get(i);
            String srNo = String.valueOf(baseSrNo + i);

            Map<String, Object> result;

            if (checking) {
                // Validation only (dry-run mode)
                result = suspensionProcessingHelper.validateSuspension(
                    noticeNo, suspensionSource, suspensionType, reasonOfSuspension,
                    daysToRevive, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo
                );
            } else {
                // Execute suspension
                result = processSingleNotice(
                    noticeNo, suspensionType, reasonOfSuspension, suspensionRemarks,
                    officerAuthorisingSuspension, suspensionSource, caseNo, srNo, payload
                );
            }

            results.add(result);
        }

        return results;
    }

    /**
     * Process a single notice for suspension
     * Routes to TS or PS helper based on suspensionType
     */
    private Map<String, Object> processSingleNotice(
            String noticeNo, String suspensionType, String reasonOfSuspension,
            String suspensionRemarks, String officerAuthorisingSuspension,
            String suspensionSource, String caseNo, String srNo,
            Map<String, Object> payload) {

        try {
            // Route to appropriate helper based on suspension type
            if ("TS".equals(suspensionType)) {
                // Extract daysToRevive (optional, only for TS)
                Integer daysToRevive = null;
                if (payload.containsKey("daysToRevive")) {
                    Object daysObj = payload.get("daysToRevive");
                    if (daysObj instanceof Integer) {
                        daysToRevive = (Integer) daysObj;
                    } else if (daysObj instanceof String) {
                        daysToRevive = Integer.parseInt((String) daysObj);
                    }
                }

                return tsHelper.processTS(noticeNo, suspensionSource, reasonOfSuspension,
                    daysToRevive, suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo);

            } else if ("PS".equals(suspensionType)) {
                return psHelper.processPS(noticeNo, suspensionSource, reasonOfSuspension,
                    suspensionRemarks, officerAuthorisingSuspension, srNo, caseNo);

            } else {
                return createErrorResponse("OCMS-4007", "Invalid Suspension Type: " + suspensionType);
            }

        } catch (Exception e) {
            log.error("Error processing notice {}: {}", noticeNo, e.getMessage(), e);
            return createErrorResponse("OCMS-4007", "System error processing notice: " + noticeNo);
        }
    }

    /**
     * Generate base SR number for batch processing using database sequence
     * Uses SUSPENDED_NOTICE_SEQ from database
     */
    private int generateBaseSrNo() {
        return sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");
    }

    /**
     * Create batch response with success/error counts
     */
    private Map<String, Object> createBatchResponse(List<Map<String, Object>> results) {
        Map<String, Object> response = new HashMap<>();

        int successCount = 0;
        int errorCount = 0;

        for (Map<String, Object> result : results) {
            String appCode = (String) result.get("appCode");
            if (appCode != null && appCode.startsWith("OCMS-2")) {
                successCount++;
            } else {
                errorCount++;
            }
        }

        response.put("totalProcessed", results.size());
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("results", results);

        return response;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String appCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("appCode", appCode);
        response.put("message", message);
        return response;
    }
}
