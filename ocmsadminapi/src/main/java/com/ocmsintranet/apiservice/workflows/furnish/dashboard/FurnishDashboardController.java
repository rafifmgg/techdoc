package com.ocmsintranet.apiservice.workflows.furnish.dashboard;

import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListRequest;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for officer dashboard operations.
 * Based on OCMS 41 User Stories 41.9-41.14.
 */
@RestController
@RequestMapping("/${api.version}/furnish/officer")
@Slf4j
@RequiredArgsConstructor
public class FurnishDashboardController {

    private final FurnishDashboardService dashboardService;

    /**
     * List furnish applications with optional filters (OCMS41.9-41.12)
     * GET/POST /furnish/officer/list
     *
     * @param request Search and filter criteria
     * @return List of furnish applications with pagination
     */
    @PostMapping("/list")
    public ResponseEntity<FurnishApplicationListResponse> listApplications(
            @RequestBody(required = false) FurnishApplicationListRequest request) {

        log.info("Officer dashboard list request: filters={}", request);

        if (request == null) {
            request = new FurnishApplicationListRequest();
        }

        FurnishApplicationListResponse response = dashboardService.listFurnishApplications(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed view of a furnish application (OCMS41.13-41.14)
     * GET /furnish/officer/{txnNo}
     *
     * @param txnNo Transaction number
     * @return Detailed application information
     */
    @GetMapping("/{txnNo}")
    public ResponseEntity<Map<String, Object>> getApplicationDetail(@PathVariable String txnNo) {
        log.info("Officer retrieving application detail: txnNo={}", txnNo);

        try {
            FurnishApplicationDetailResponse detail = dashboardService.getApplicationDetail(txnNo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", detail);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", "NOT_FOUND");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            log.error("Error retrieving application detail: txnNo={}", txnNo, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", "TECHNICAL_ERROR");
            errorResponse.put("message", "Error retrieving application: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Exception handler for generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorType", "TECHNICAL_ERROR");
        response.put("message", "Unexpected error: " + ex.getMessage());

        log.error("Unexpected error in furnish dashboard controller", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
