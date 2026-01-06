package com.ocmsintranet.apiservice.workflows.notice_management.driver;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.workflows.notice_management.driver.dto.RequestDriverParticularPlusDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticulars;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticularsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling PLUS API request driver particular endpoint
 * NOTE: This controller directly calls the CRUD service (OcmsRequestDriverParticularsService)
 * without an intermediate workflow service layer for simplicity.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class RequestDriverController {

    private final OcmsRequestDriverParticularsService service;

    /**
     * Endpoint for retrieving request driver particulars
     *
     * @param requestBody Request body containing noticeNo and optional pagination parameters
     * @return Response with request driver particular data
     */
    @PostMapping("/plus-request-driver-particular")
    public ResponseEntity<?> getPlusRequestDriverParticular(@RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Received request on /plus-request-driver-particular: {}", requestBody);
        log.info("Processing PLUS request driver particular request with body: {}", requestBody);

        try {
            // Validate required field
            if (requestBody == null || !requestBody.containsKey("noticeNo")) {
                log.warn("Missing required field: noticeNo");
                CrudResponse<?> errorResponse = createErrorResponse("Missing required field: noticeNo");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            String noticeNo = (String) requestBody.get("noticeNo");
            if (noticeNo == null || noticeNo.trim().isEmpty()) {
                log.warn("Invalid noticeNo: {}", noticeNo);
                CrudResponse<?> errorResponse = createErrorResponse("noticeNo cannot be null or empty");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Handle pagination parameters
            int skip = 0;
            int limit = 10;

            Object skipObj = requestBody.get("$skip");
            if (skipObj != null) {
                skip = Integer.parseInt(skipObj.toString());
            }

            Object limitObj = requestBody.get("$limit");
            if (limitObj != null) {
                limit = Integer.parseInt(limitObj.toString());
            }

            log.info("Fetching real data for noticeNo: {}, skip: {}, limit: {}", noticeNo, skip, limit);

            // Build query parameters for BaseService.getAll()
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("noticeNo", new String[]{noticeNo});
            queryParams.put("$skip", new String[]{String.valueOf(skip)});
            queryParams.put("$limit", new String[]{String.valueOf(limit)});
            queryParams.put("$sort", new String[]{"dateOfProcessing:desc"});

            // Fetch data from database using the service
            FindAllResponse<OcmsRequestDriverParticulars> dbResponse = service.getAll(queryParams);

            // Convert entity objects to DTO objects
            List<RequestDriverParticularPlusDto> dtoList = dbResponse.getData().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // Create response with DTOs
            FindAllResponse<RequestDriverParticularPlusDto> plusResponse = new FindAllResponse<>();
            plusResponse.setTotal(dbResponse.getTotal());
            plusResponse.setLimit(dbResponse.getLimit());
            plusResponse.setSkip(dbResponse.getSkip());
            plusResponse.setData(dtoList);

            log.info("Returning PLUS request driver particular response with {} items out of {} total",
                    plusResponse.getData().size(), plusResponse.getTotal());

            return ResponseEntity.ok(plusResponse);

        } catch (Exception e) {
            log.error("Error retrieving PLUS request driver particulars: {}", e.getMessage(), e);
            CrudResponse<?> errorResponse = createErrorResponse("Error retrieving request driver particulars: " + e.getMessage(), CrudResponse.AppCodes.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert OcmsRequestDriverParticulars entity to RequestDriverParticularPlusDto
     */
    private RequestDriverParticularPlusDto convertToDto(OcmsRequestDriverParticulars entity) {
        RequestDriverParticularPlusDto dto = new RequestDriverParticularPlusDto();
        dto.setDateOfProcessing(entity.getDateOfProcessing());
        dto.setNoticeNo(entity.getNoticeNo());
        dto.setDateOfRdp(entity.getDateOfRdp());
        dto.setDateOfReturn(entity.getDateOfReturn());
        dto.setOwnerBldg(entity.getOwnerBldg());
        dto.setOwnerBlkHseNo(entity.getOwnerBlkHseNo());
        dto.setOwnerFloor(entity.getOwnerFloor());
        dto.setOwnerIdType(entity.getOwnerIdType());
        dto.setOwnerName(entity.getOwnerName());
        dto.setOwnerNricNo(entity.getOwnerNricNo());
        dto.setOwnerPostalCode(entity.getOwnerPostalCode());
        dto.setOwnerStreet(entity.getOwnerStreet());
        dto.setOwnerUnit(entity.getOwnerUnit());
        dto.setPostalRegnNo(entity.getPostalRegnNo());
        dto.setProcessingStage(entity.getProcessingStage());
        dto.setReminderFlag(entity.getReminderFlag());
        dto.setUnclaimedReason(entity.getUnclaimedReason());
        return dto;
    }

    /**
     * Create standardized error response with BAD_REQUEST app code
     */
    private CrudResponse<?> createErrorResponse(String message) {
        return createErrorResponse(message, CrudResponse.AppCodes.BAD_REQUEST);
    }

    /**
     * Create standardized error response with custom app code
     */
    private CrudResponse<?> createErrorResponse(String message, String appCode) {
        CrudResponse<?> errorResponse = new CrudResponse<>();
        CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
        responseData.setAppCode(appCode);
        responseData.setMessage(message);
        errorResponse.setData(responseData);
        return errorResponse;
    }
}
