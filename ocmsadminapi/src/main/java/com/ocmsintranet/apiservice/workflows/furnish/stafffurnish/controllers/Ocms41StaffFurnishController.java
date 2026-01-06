package com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.controllers;

import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.BatchFurnishRequest;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.BatchFurnishResult;
import com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services.Ocms41StaffFurnishService.BatchUpdateAddressRequest;
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
 * OCMS 41: Staff Furnish Controller
 *
 * REST APIs for OCMS Staff Portal to manually furnish hirer/driver particulars.
 *
 * Endpoints (ALL POST):
 * - POST /furnish-redirect-check: Check if notice can be furnished/redirected
 * - POST /staff-furnish: Create new offender record
 * - POST /staff-redirect: Overwrite existing offender record
 * - POST /staff-update: Update offender details
 * - POST /batch-furnish: Furnish multiple notices with same offender
 * - POST /batch-update-address: Update address for multiple notices
 *
 * Based on Functional Spec v1.1 Sections 4.6.2 and 5 (Batch Operations)
 */
@RestController
@RequestMapping("/${api.version}/ocms41/staff-furnish")
@Slf4j
@RequiredArgsConstructor
public class Ocms41StaffFurnishController {

    private final Ocms41StaffFurnishService staffFurnishService;

    /**
     * API 19: Furnish-Redirect Check
     *
     * Checks whether a notice can be furnished or redirected.
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
        log.info("REST request to check furnishability for notice={}", request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
                response.put("furnishable", false);
                response.put("reasonCode", "MISSING_NOTICE_NO");
                response.put("reasonMessage", "Notice number is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Check furnishability
            FurnishCheckResult result = staffFurnishService.checkFurnishable(request.getNoticeNo());

            response.put("furnishable", result.isFurnishable());
            response.put("reasonCode", result.getReasonCode());
            response.put("reasonMessage", result.getReasonMessage());

            return result.isFurnishable()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("Error checking furnishability for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("furnishable", false);
            response.put("reasonCode", "ERROR");
            response.put("reasonMessage", "Error checking furnishability: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 20: Staff Furnish
     *
     * Creates a new offender record for furnished hirer/driver particulars.
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
     * @param request Staff furnish request
     * @return FurnishResult
     */
    @PostMapping("/staff-furnish")
    public ResponseEntity<Map<String, Object>> staffFurnish(@RequestBody StaffFurnishRequest request) {
        log.info("REST request to staff furnish {} for notice={}", request.getOwnerDriverIndicator(), request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate mandatory fields
            String validationError = validateFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Staff furnish
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
            log.error("Error staff furnishing for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error furnishing offender: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 21: Staff Redirect
     *
     * Overwrites an existing offender record with new furnished particulars.
     *
     * Request Body: Same as Staff Furnish
     *
     * Response: Same as Staff Furnish
     *
     * @param request Staff redirect request
     * @return FurnishResult
     */
    @PostMapping("/staff-redirect")
    public ResponseEntity<Map<String, Object>> staffRedirect(@RequestBody StaffFurnishRequest request) {
        log.info("REST request to staff redirect {} for notice={}", request.getOwnerDriverIndicator(), request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate mandatory fields
            String validationError = validateFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Staff redirect
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
            log.error("Error staff redirecting for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error redirecting offender: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 22: Staff Update
     *
     * Updates existing offender details.
     *
     * Request Body: Same as Staff Furnish
     *
     * Response: Same as Staff Furnish
     *
     * @param request Staff update request
     * @return FurnishResult
     */
    @PostMapping("/staff-update")
    public ResponseEntity<Map<String, Object>> staffUpdate(@RequestBody StaffFurnishRequest request) {
        log.info("REST request to staff update {} for notice={}", request.getOwnerDriverIndicator(), request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate mandatory fields
            String validationError = validateFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Staff update
            FurnishResult result = staffFurnishService.staffUpdate(request);

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
            log.error("Error staff updating for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error updating offender: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 23: Batch Furnish Processing
     *
     * Furnishes multiple notices with the same offender particulars.
     * This is a Staff Portal feature to apply the same furnish operation to multiple notices.
     *
     * Request Body:
     * {
     *   "noticeNumbers": ["500506201A", "500506202B", "500506203C"],
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
     *   "totalCount": 3,
     *   "successCount": 2,
     *   "failureCount": 1,
     *   "allSuccess": false,
     *   "results": {
     *     "500506201A": {"success": true, "message": "Success", ...},
     *     "500506202B": {"success": true, "message": "Success", ...},
     *     "500506203C": {"success": false, "message": "Notice has active PS", ...}
     *   }
     * }
     *
     * @param request Batch furnish request
     * @return BatchFurnishResult
     */
    @PostMapping("/batch-furnish")
    public ResponseEntity<Map<String, Object>> batchFurnish(@RequestBody BatchFurnishRequest request) {
        log.info("REST request to batch furnish {} for {} notices",
                request.getOwnerDriverIndicator(), request.getNoticeNumbers().size());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getNoticeNumbers() == null || request.getNoticeNumbers().isEmpty()) {
                response.put("success", false);
                response.put("message", "Notice numbers list is required and cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            String validationError = validateBatchFurnishRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Batch furnish
            BatchFurnishResult result = staffFurnishService.batchFurnish(request);

            // Build response
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("allSuccess", result.isAllSuccess());

            // Convert results map to response format
            Map<String, Map<String, Object>> resultsMap = new HashMap<>();
            for (Map.Entry<String, FurnishResult> entry : result.getAllResults().entrySet()) {
                String noticeNo = entry.getKey();
                FurnishResult furnishResult = entry.getValue();

                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", furnishResult.isSuccess());
                resultData.put("message", furnishResult.getMessage());
                if (furnishResult.isSuccess()) {
                    resultData.put("noticeNo", furnishResult.getNoticeNo());
                    resultData.put("offenderName", furnishResult.getOffenderName());
                    resultData.put("offenderIdNo", furnishResult.getOffenderIdNo());
                    resultData.put("newProcessingStage", furnishResult.getNewProcessingStage());
                }
                resultsMap.put(noticeNo, resultData);
            }
            response.put("results", resultsMap);

            return result.isAllSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(207).body(response); // 207 Multi-Status for partial success

        } catch (Exception e) {
            log.error("Error batch furnishing: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error batch furnishing: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 24: Batch Update Address
     *
     * Updates mailing address for multiple notices with the same offender ID.
     * This is a Staff Portal feature to update the address for all notices belonging to the same offender.
     *
     * Request Body:
     * {
     *   "noticeNumbers": ["500506201A", "500506202B"],
     *   "ownerDriverIndicator": "H",
     *   "name": "John Tan",
     *   "idType": "N",
     *   "idNo": "S1234567A",
     *   "offenderTelCode": "65",
     *   "offenderTelNo": "91234567",
     *   "emailAddr": "john.tan@newmail.com",
     *   "postalCode": "408732",
     *   "blkHseNo": "12",
     *   "streetName": "Toa Payoh Lorong 1",
     *   "floorNo": "8",
     *   "unitNo": "01",
     *   "bldgName": "Starlight Centre"
     * }
     *
     * Response: Same as Batch Furnish
     *
     * @param request Batch update address request
     * @return BatchFurnishResult
     */
    @PostMapping("/batch-update-address")
    public ResponseEntity<Map<String, Object>> batchUpdateAddress(@RequestBody BatchUpdateAddressRequest request) {
        log.info("REST request to batch update address for ID={}, {} notices",
                request.getIdNo(), request.getNoticeNumbers().size());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getNoticeNumbers() == null || request.getNoticeNumbers().isEmpty()) {
                response.put("success", false);
                response.put("message", "Notice numbers list is required and cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            String validationError = validateBatchUpdateAddressRequest(request);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            // Batch update address
            BatchFurnishResult result = staffFurnishService.batchUpdateAddress(request);

            // Build response (same format as batch furnish)
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("allSuccess", result.isAllSuccess());

            // Convert results map to response format
            Map<String, Map<String, Object>> resultsMap = new HashMap<>();
            for (Map.Entry<String, FurnishResult> entry : result.getAllResults().entrySet()) {
                String noticeNo = entry.getKey();
                FurnishResult furnishResult = entry.getValue();

                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", furnishResult.isSuccess());
                resultData.put("message", furnishResult.getMessage());
                if (furnishResult.isSuccess()) {
                    resultData.put("noticeNo", furnishResult.getNoticeNo());
                    resultData.put("offenderName", furnishResult.getOffenderName());
                    resultData.put("offenderIdNo", furnishResult.getOffenderIdNo());
                    resultData.put("newProcessingStage", furnishResult.getNewProcessingStage());
                }
                resultsMap.put(noticeNo, resultData);
            }
            response.put("results", resultsMap);

            return result.isAllSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(207).body(response); // 207 Multi-Status for partial success

        } catch (Exception e) {
            log.error("Error batch updating address: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error batch updating address: " + e.getMessage());
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

    /**
     * Validate batch furnish request - check all mandatory fields
     */
    private String validateBatchFurnishRequest(BatchFurnishRequest request) {
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

    /**
     * Validate batch update address request - check all mandatory fields
     */
    private String validateBatchUpdateAddressRequest(BatchUpdateAddressRequest request) {
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
