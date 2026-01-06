package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.drivernotice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNoticeService;
import com.ocmsintranet.apiservice.workflows.notice_management.retrieval.drivernotice.dto.DriverNoticePlusDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for handling PLUS API driver notice endpoint
 * Note: This controller directly calls CRUD service (OcmsDriverNoticeService)
 * without intermediate workflow service layer for simplified architecture
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class DriverNoticeControllerPlus {

    private final OcmsDriverNoticeService ocmsDriverNoticeService;

    /**
     * Get list of driver notices (PLUS Interface Specification)
     * Fetches real data from ocms_driver_notice table
     *
     * @param requestBody Request body containing noticeNo and pagination parameters
     * @return Response with driver notice data
     */
    @PostMapping("/plus-driver-notice")
    public ResponseEntity<?> getPlusDriverNotice(@RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Received PLUS driver notice request with body: {}", requestBody);

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
            FindAllResponse<OcmsDriverNotice> dbResponse = ocmsDriverNoticeService.getAll(queryParams);

            // Convert entity objects to DTO objects
            List<DriverNoticePlusDto> dtoList = dbResponse.getData().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // Create response with DTOs
            FindAllResponse<DriverNoticePlusDto> plusResponse = new FindAllResponse<>();
            plusResponse.setTotal(dbResponse.getTotal());
            plusResponse.setLimit(dbResponse.getLimit());
            plusResponse.setSkip(dbResponse.getSkip());
            plusResponse.setData(dtoList);

            log.info("Returning PLUS driver notice response with {} items out of {} total",
                    plusResponse.getData().size(), plusResponse.getTotal());

            return ResponseEntity.ok(plusResponse);

        } catch (Exception e) {
            log.error("Error retrieving PLUS driver notices: {}", e.getMessage(), e);
            CrudResponse<?> errorResponse = createErrorResponse("Error retrieving driver notices: " + e.getMessage(), CrudResponse.AppCodes.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert OcmsDriverNotice entity to DriverNoticePlusDto
     */
    private DriverNoticePlusDto convertToDto(OcmsDriverNotice entity) {
        DriverNoticePlusDto dto = new DriverNoticePlusDto();
        dto.setDateOfProcessing(entity.getDateOfProcessing());
        dto.setNoticeNo(entity.getNoticeNo());
        dto.setDateOfDn(entity.getDateOfDn());
        dto.setDateOfReturn(entity.getDateOfReturn());
        dto.setDriverBldg(entity.getDriverBldg());
        dto.setDriverBlkHseNo(entity.getDriverBlkHseNo());
        dto.setDriverFloor(entity.getDriverFloor());
        dto.setDriverIdType(entity.getDriverIdType());
        dto.setDriverName(entity.getDriverName());
        dto.setDriverNricNo(entity.getDriverNricNo());
        dto.setDriverPostalCode(entity.getDriverPostalCode());
        dto.setDriverStreet(entity.getDriverStreet());
        dto.setDriverUnit(entity.getDriverUnit());
        dto.setPostalRegnNo(entity.getPostalRegnNo());
        dto.setProcessingStage(entity.getProcessingStage());
        dto.setReasonForUnclaim(entity.getReasonForUnclaim());
        dto.setReminderFlag(entity.getReminderFlag());
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
