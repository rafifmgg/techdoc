package com.ocmsintranet.apiservice.workflows.furnish.plusintegration.controllers;

import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.FurnishCheckResult;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.FurnishResult;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.StaffFurnishRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OCMS 41: PLUS Integration Controller
 *
 * REST APIs for PLUS Staff Portal to furnish/redirect offender particulars.
 * PLUS is the Appeals system used by Appeal Processing Officers (PLMs).
 *
 * These endpoints trigger the SAME backend processing functions as OCMS Staff Portal,
 * but are exposed as separate endpoints for PLUS to call.
 *
 * Endpoints (ALL POST):
 * - POST /furnish-redirect-check: Check if notice can be furnished/redirected
 * - POST /plus-furnish: Add new Hirer/Driver as Current Offender
 * - POST /plus-redirect: Redirect Notice to existing Owner/Hirer/Driver
 *
 * Based on Functional Spec v1.1 Section 6 (Integration with PLUS)
 *
 * Note: These endpoints reuse Ocms41StaffFurnishService for all business logic.
 */
@RestController
@RequestMapping("/${api.version}/ocms41/plus-integration")
@Slf4j
@RequiredArgsConstructor
public class Ocms41PlusIntegrationController {

    private final Ocms41StaffFurnishService staffFurnishService;

    /**
     * API 25: PLUS Furnish-Redirect Check
     *
     * Checks whether a notice can be furnished or redirected from PLUS Staff Portal.
     * This is the same check used by OCMS Staff Portal.
     *
     * Request Body:
     * {
     *   "noticeNo": "500506201A"
     * }
     *
     * Response:
     * {
     *   "furnishable": true,
     *   "reasonCode": null,
     *   "reasonMessage": null
     * }
     *
     * @param request Notice number to check
     * @return FurnishCheckResult
     */
    @PostMapping("/furnish-redirect-check")
    public ResponseEntity<Map<String, Object>> checkFurnishable(@RequestBody FurnishCheckRequest request) {
        log.info("PLUS REST request to check furnishability for notice={}", request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
                response.put("furnishable", false);
                response.put("reasonCode", "MISSING_NOTICE_NO");
                response.put("reasonMessage", "Notice number is required");
                return ResponseEntity.badRequest().body(response);
            }

            // REUSE: Call same backend function as OCMS Staff Portal
            FurnishCheckResult result = staffFurnishService.checkFurnishable(request.getNoticeNo());

            response.put("furnishable", result.isFurnishable());
            response.put("reasonCode", result.getReasonCode());
            response.put("reasonMessage", result.getReasonMessage());

            return result.isFurnishable()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("PLUS error checking furnishability for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("furnishable", false);
            response.put("reasonCode", "ERROR");
            response.put("reasonMessage", "Error checking furnishability: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 26: PLUS Furnish
     *
     * Add a new Hirer or Driver as the Current Offender from PLUS Staff Portal.
     *
     * This endpoint triggers the same backend processing function as OCMS Staff Portal
     * (staffFurnishService.staffFurnish).
     *
     * Request Body:
     * {
     *   "noticeNo": "500506201A",
     *   "ownerDriverIndicator": "H",
     *   "name": "John Tan",
     *   "idType": "N",
     *   "idNo": "S1234567A",
     *   "offenderTelCode": "65",
     *   "offenderTelNo": "91234567",
     *   "emailAddr": "john.tan@abcmail.com",
     *   "postalCode": "408732",
     *   "blkHseNo": "12",
     *   "streetName": "Toa Payoh Lorong 1",
     *   "floorNo": "8",
     *   "unitNo": "01",
     *   "bldgName": "Starlight Centre"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Success",
     *   "noticeNo": "500506201A",
     *   "offenderName": "John Tan",
     *   "offenderIdNo": "S1234567A",
     *   "newProcessingStage": "RD1"
     * }
     *
     * @param request PLUS furnish request
     * @return FurnishResult
     */
    @PostMapping("/plus-furnish")
    public ResponseEntity<Map<String, Object>> plusFurnish(@RequestBody StaffFurnishRequest request) {
        log.info("PLUS REST request to furnish {} for notice={}", request.getOwnerDriverIndicator(), request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate mandatory fields
            String validationError = validateFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // REUSE: Call same backend function as OCMS Staff Portal
            FurnishResult result = staffFurnishService.staffFurnish(request);

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.isSuccess()) {
                response.put("noticeNo", result.getNoticeNo());
                response.put("offenderName", result.getOffenderName());
                response.put("offenderIdNo", result.getOffenderIdNo());
                response.put("newProcessingStage", result.getNewProcessingStage());
            }

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("PLUS error furnishing for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error furnishing offender: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 27: PLUS Redirect
     *
     * Redirect Notice to existing Owner, Hirer or Driver from PLUS Staff Portal.
     *
     * This endpoint triggers the same backend processing function as OCMS Staff Portal
     * (staffFurnishService.staffRedirect).
     *
     * Request Body: Same as PLUS Furnish
     *
     * Response: Same as PLUS Furnish
     *
     * @param request PLUS redirect request
     * @return FurnishResult
     */
    @PostMapping("/plus-redirect")
    public ResponseEntity<Map<String, Object>> plusRedirect(@RequestBody StaffFurnishRequest request) {
        log.info("PLUS REST request to redirect {} for notice={}", request.getOwnerDriverIndicator(), request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate mandatory fields
            String validationError = validateFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // REUSE: Call same backend function as OCMS Staff Portal
            FurnishResult result = staffFurnishService.staffRedirect(request);

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.isSuccess()) {
                response.put("noticeNo", result.getNoticeNo());
                response.put("offenderName", result.getOffenderName());
                response.put("offenderIdNo", result.getOffenderIdNo());
                response.put("newProcessingStage", result.getNewProcessingStage());
            }

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("PLUS error redirecting for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error redirecting offender: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validate furnish request - check all mandatory fields
     */
    private String validateFurnishRequest(StaffFurnishRequest request) {
        if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
            return "Notice number is required";
        }
        if (request.getOwnerDriverIndicator() == null || request.getOwnerDriverIndicator().isBlank()) {
            return "Owner/Driver indicator is required";
        }
        if (request.getName() == null || request.getName().isBlank()) {
            return "Name is required";
        }
        if (request.getIdType() == null || request.getIdType().isBlank()) {
            return "ID type is required";
        }
        if (request.getIdNo() == null || request.getIdNo().isBlank()) {
            return "ID number is required";
        }
        if (request.getPostalCode() == null || request.getPostalCode().isBlank()) {
            return "Postal code is required";
        }
        if (request.getBlkHseNo() == null || request.getBlkHseNo().isBlank()) {
            return "Block/House number is required";
        }
        if (request.getStreetName() == null || request.getStreetName().isBlank()) {
            return "Street name is required";
        }
        return null; // All validations passed
    }

    // ==================== REQUEST DTOs ====================

    @Data
    public static class FurnishCheckRequest {
        private String noticeNo;
    }
}
