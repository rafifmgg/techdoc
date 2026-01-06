package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for OcmsWebTransactionDetail entity
 * Provides endpoints for web transaction detail operations
 */
@RestController
@RequestMapping("/${api.version}")
@Slf4j
public class OcmsWebTransactionDetailController {

    private final OcmsWebTransactionDetailService service;
    private final OcmsWebTransactionDetailRepository repository;

    public OcmsWebTransactionDetailController(OcmsWebTransactionDetailService service,
                                               OcmsWebTransactionDetailRepository repository) {
        this.service = service;
        this.repository = repository;
        log.info("OcmsWebTransactionDetail controller initialized");
    }

    /**
     * POST endpoint for listing web transaction details
     */
    @PostMapping("/webtxndetaillist")
    public ResponseEntity<FindAllResponse<OcmsWebTransactionDetail>> getAllPost(
            @RequestBody(required = false) Map<String, Object> requestBody) {
        Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(requestBody);
        FindAllResponse<OcmsWebTransactionDetail> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * POST endpoint to create a new web transaction detail
     */
    @PostMapping("/webtxndetail")
    public ResponseEntity<?> create(@RequestBody OcmsWebTransactionDetail entity) {
        try {
            OcmsWebTransactionDetail saved = service.save(entity);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating web transaction detail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating web transaction detail: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET endpoint to get web transaction detail by composite ID
     */
    @GetMapping("/webtxndetail/{receiptNo}/{offenceNoticeNo}")
    public ResponseEntity<?> getById(@PathVariable String receiptNo,
                                     @PathVariable String offenceNoticeNo) {
        try {
            OcmsWebTransactionDetailId id = new OcmsWebTransactionDetailId(receiptNo, offenceNoticeNo);
            Optional<OcmsWebTransactionDetail> result = service.getById(id);

            if (result.isPresent()) {
                return new ResponseEntity<>(result.get(), HttpStatus.OK);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Web transaction detail not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error getting web transaction detail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error getting web transaction detail: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET endpoint to get all web transaction details by offence notice number
     */
    @GetMapping("/webtxndetail/bynotice/{offenceNoticeNo}")
    public ResponseEntity<?> getByOffenceNoticeNo(@PathVariable String offenceNoticeNo) {
        try {
            List<OcmsWebTransactionDetail> results = repository.findByOffenceNoticeNo(offenceNoticeNo);
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error getting web transaction details by notice: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error getting web transaction details: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET endpoint to get all web transaction details by receipt number
     */
    @GetMapping("/webtxndetail/byreceipt/{receiptNo}")
    public ResponseEntity<?> getByReceiptNo(@PathVariable String receiptNo) {
        try {
            List<OcmsWebTransactionDetail> results = repository.findByReceiptNo(receiptNo);
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error getting web transaction details by receipt: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error getting web transaction details: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST endpoint to check if a web transaction detail exists by ID
     */
    @PostMapping("/webtxndetailexists")
    public ResponseEntity<?> checkExists(@RequestBody Map<String, String> request) {
        try {
            String receiptNo = request.get("receiptNo");
            String offenceNoticeNo = request.get("offenceNoticeNo");

            if (receiptNo == null || offenceNoticeNo == null) {
                throw new IllegalArgumentException("receiptNo and offenceNoticeNo are required");
            }

            OcmsWebTransactionDetailId id = new OcmsWebTransactionDetailId(receiptNo, offenceNoticeNo);
            Optional<OcmsWebTransactionDetail> result = service.getById(id);
            boolean exists = result.isPresent();

            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error checking web transaction detail existence: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error checking existence: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PATCH endpoint to update a web transaction detail
     */
    @PatchMapping("/webtxndetail/{receiptNo}/{offenceNoticeNo}")
    public ResponseEntity<?> patch(@PathVariable String receiptNo,
                                   @PathVariable String offenceNoticeNo,
                                   @RequestBody OcmsWebTransactionDetail partialEntity) {
        try {
            OcmsWebTransactionDetailId id = new OcmsWebTransactionDetailId(receiptNo, offenceNoticeNo);
            OcmsWebTransactionDetail updated = service.patch(id, partialEntity);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error patching web transaction detail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error updating web transaction detail: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * DELETE endpoint to delete a web transaction detail
     */
    @DeleteMapping("/webtxndetail/{receiptNo}/{offenceNoticeNo}")
    public ResponseEntity<?> delete(@PathVariable String receiptNo,
                                    @PathVariable String offenceNoticeNo) {
        try {
            OcmsWebTransactionDetailId id = new OcmsWebTransactionDetailId(receiptNo, offenceNoticeNo);
            service.delete(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Web transaction detail deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error deleting web transaction detail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error deleting web transaction detail: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
