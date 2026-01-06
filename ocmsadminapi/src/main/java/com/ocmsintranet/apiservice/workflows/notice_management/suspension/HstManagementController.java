package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for OCMS 20 HST (House Tenant) Management functionality
 * Endpoints:
 * 1. POST /v1/apply-hst - Apply HST suspension to all outstanding notices
 * 2. POST /v1/update-hst/{id} - Update HST offender name/address
 * 3. POST /v1/revive-hst - Revive TS-HST suspensions and remove from HST table
 */
@RestController
@RequestMapping("/${api.version}")
@RequiredArgsConstructor
@Slf4j
public class HstManagementController {

    private final HstManagementService hstManagementService;
    private final ObjectMapper objectMapper;

    /**
     * POST /v1/apply-hst
     * Create HST record and apply TS-HST suspension to all outstanding notices
     *
     * Request Body:
     * {
     *   "idNo": "S1234567A",
     *   "name": "John Doe",
     *   "streetName": "Choa Chu Kang Ave 5",
     *   "blkHseNo": "12",
     *   "floorNo": "17",
     *   "unitNo": "281",
     *   "bldgName": "Green Residences",
     *   "postalCode": "103431"
     * }
     *
     * Response:
     * [
     *   {
     *     "noticeNo": "541000009T",
     *     "appCode": "OCMS-2000",
     *     "message": "OK"
     *   },
     *   {
     *     "noticeNo": "541000008T",
     *     "appCode": "OCMS-4002",
     *     "message": "Undefined Error"
     *   }
     * ]
     */
    @PostMapping("/apply-hst")
    public ResponseEntity<?> applyHstSuspension(@RequestBody HstApplyDto request) {
        try {
            log.info("Received apply-hst request for ID: {}", request.getIdNo());

            // Validate request
            if (request.getIdNo() == null || request.getIdNo().isEmpty()) {
                return createErrorResponse("OCMS-4000", "idNo is required");
            }

            log.info("Applying HST suspension for ID: {}", request.getIdNo());

            // Call service to apply HST suspension
            List<HstProcessingResult> results = hstManagementService.applyHstSuspension(request);

            // Return array of results (per notice)
            return new ResponseEntity<>(results, HttpStatus.OK);

        } catch (HstManagementService.HstAlreadyExistsException e) {
            log.warn("HST ID already exists: {}", request.getIdNo());
            return createErrorResponse("OCMS-4001", "HST ID already exists in system");
        } catch (Exception e) {
            log.error("Error applying HST suspension", e);
            return createErrorResponse("OCMS-5000", "Error applying HST suspension: " + e.getMessage());
        }
    }

    /**
     * POST /v1/update-hst/{id}
     * Update HST offender name and address
     *
     * Request Body:
     * {
     *   "name": "John Doe",
     *   "streetName": "Choa Chu Kang Ave 5",
     *   "blkHseNo": "15",
     *   "floorNo": "17",
     *   "unitNo": "285",
     *   "bldgName": "Green Residences",
     *   "postalCode": "103431"
     * }
     *
     * Response:
     * [
     *   {
     *     "noticeNo": "541000009T",
     *     "appCode": "OCMS-2000",
     *     "message": "Update Success"
     *   }
     * ]
     */
    @PostMapping("/update-hst/{idNo}")
    public ResponseEntity<?> updateHst(
            @PathVariable String idNo,
            @RequestBody HstUpdateDto request) {
        try {
            log.info("Received update-hst request for ID: {}", idNo);

            // Validate ID exists in HST table
            if (!hstManagementService.hstExists(idNo)) {
                return createErrorResponse("OCMS-4001", "HST ID not found in system");
            }

            log.info("Updating HST record for ID: {}", idNo);

            // Call service to update HST record
            List<HstProcessingResult> results = hstManagementService.updateHst(idNo, request);

            // Return array of results
            return new ResponseEntity<>(results, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error updating HST record", e);
            return createErrorResponse("OCMS-5000", "Error updating HST record: " + e.getMessage());
        }
    }

    /**
     * POST /v1/revive-hst
     * Revive all TS-HST suspensions and remove HST record
     *
     * Request Body:
     * {
     *   "idNo": "S1234567A",
     *   "name": "John Doe",
     *   "streetName": "Bukit Merah View",
     *   "blkHseNo": "65",
     *   "floorNo": "29",
     *   "unitNo": "1551",
     *   "bldgName": "The Amberlyn",
     *   "postalCode": "578640"
     * }
     *
     * Response:
     * [
     *   {
     *     "noticeNo": "541000009T",
     *     "appCode": "OCMS-2000",
     *     "message": "OK"
     *   }
     * ]
     */
    @PostMapping("/revive-hst")
    public ResponseEntity<?> reviveHst(@RequestBody HstReviveDto request) {
        try {
            log.info("Received revive-hst request for ID: {}", request.getIdNo());

            // Validate request
            if (request.getIdNo() == null || request.getIdNo().isEmpty()) {
                return createErrorResponse("OCMS-4000", "idNo is required");
            }

            // Validate ID exists in HST table
            if (!hstManagementService.hstExists(request.getIdNo())) {
                return createErrorResponse("OCMS-4001", "HST ID not found in system");
            }

            log.info("Reviving HST suspensions for ID: {}", request.getIdNo());

            // Call service to revive HST suspensions
            List<HstProcessingResult> results = hstManagementService.reviveHst(request);

            // Return array of results
            return new ResponseEntity<>(results, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error reviving HST suspensions", e);
            return createErrorResponse("OCMS-5000", "Error reviving HST suspensions: " + e.getMessage());
        }
    }

    /**
     * Create error response in OCMS API format
     */
    private ResponseEntity<?> createErrorResponse(String appCode, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("appCode", appCode);
        errorData.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", errorData);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
