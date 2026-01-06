package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.OffenceNoticeWithOwnerDto;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
// import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
// import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
// import com.ocmsintranet.apiservice.workflows.notice_management.suspension.suspendednotice.dto.SuspendedNoticePlusDto;
import com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice.dto.PlusOffenceNoticeResponse;
import com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice.mapper.NoticeResponseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of GetNoticeService for handling PLUS API notice-related requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetNoticeServiceImpl implements GetNoticeService {

    private final OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;
    private final NoticeResponseMapper mapper;
    // private final SuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<?> processPlusOffenceNotice(
            HttpServletRequest request, Map<String, Object> body) {
        try {
            log.info("Processing PLUS offence notice request");

            // Check if suspendedIndicator is Y
            // boolean includeSuspendedInfo = "Y".equals(body.get("suspendedIndicator"));
            boolean includeSuspendedInfo = false;

            // Convert the request body to parameters map
            Map<String, String[]> params = convertBodyToParams(body);

            // Call the existing getAllWithOwnerInfo method
            FindAllResponse<OffenceNoticeWithOwnerDto> response =
                ocmsValidOffenceNoticeService.getAllWithOwnerInfo(params);

            // Fetch suspended notices if requested
            // Map<String, List<SuspendedNoticePlusDto>> suspendedNoticesMap = new HashMap<>();

            // if (includeSuspendedInfo) {
            //     log.info("Fetching suspended notices for response");
            //     suspendedNoticesMap = fetchSuspendedNotices(response.getData());
            // }

            // Map the response to the PLUS API format
            PlusOffenceNoticeResponse plusResponse = mapper.plusOffenceNoticeResponse(
                response, includeSuspendedInfo);
            

            log.info("Successfully processed PLUS offence notice request. Total records: {}",
                    plusResponse.getTotal());
            return ResponseEntity.ok(plusResponse);
        } catch (Exception e) {
            log.error("Error processing PLUS offence notice request", e);
            return createErrorResponse("OCMS-5000",
                "Something went wrong on our end. Please try again later.");
        }
    }
    
    /**
     * Converts request body map to parameters map for service layer
     *
     * @param body Request body map
     * @return Parameters map
     */
    private Map<String, String[]> convertBodyToParams(Map<String, Object> body) {
        Map<String, String[]> params = new HashMap<>();

        // Handle pagination parameters
        if (body.containsKey("$skip")) {
            params.put("$skip", new String[]{body.get("$skip").toString()});
        }

        if (body.containsKey("$limit")) {
            params.put("$limit", new String[]{body.get("$limit").toString()});
        }

        // Handle filter parameters
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip pagination parameters and special indicators
            if (key.equals("$skip") || key.equals("$limit") || key.equals("suspendedIndicator")) {
                continue;
            }

            // Handle filterByReceipt parameter (pass through as-is)
            if (key.equals("filterByReceipt")) {
                params.put(key, new String[]{value.toString()});
                continue;
            }
            
            if (value instanceof String) {
                params.put(key, new String[]{(String) value});
            } else if (value instanceof Number) {
                params.put(key, new String[]{value.toString()});
            } else if (value instanceof Boolean) {
                params.put(key, new String[]{value.toString()});
            } else if (value instanceof Map) {
                // Handle operator objects like {$gte: "value"}
                @SuppressWarnings("unchecked")
                Map<String, Object> operatorMap = (Map<String, Object>) value;
                for (Map.Entry<String, Object> opEntry : operatorMap.entrySet()) {
                    String operator = opEntry.getKey();
                    Object opValue = opEntry.getValue();
                    
                    // Format as field[$operator]=value
                    params.put(key + "[" + operator + "]", 
                        new String[]{opValue != null ? opValue.toString() : null});
                }
            }
            // Add more type handling as needed
        }
        
        return params;
    }

    /**
     * Fetches suspended notices for a list of offence notices
     *
     * @param offenceNotices List of offence notices
     * @return Map of notice number to list of suspended notices
     */
    // private Map<String, List<SuspendedNoticePlusDto>> fetchSuspendedNotices(
    //         List<OffenceNoticeWithOwnerDto> offenceNotices) {

    //     Map<String, List<SuspendedNoticePlusDto>> suspendedNoticesMap = new HashMap<>();

    //     for (OffenceNoticeWithOwnerDto notice : offenceNotices) {
    //         String noticeNo = notice.getNoticeNo();

    //         // Fetch suspended notices for this notice number
    //         // List<SuspendedNotice> suspendedNotices =
    //         //     suspendedNoticeRepository.findByNoticeNo(noticeNo);

    //         // Convert to DTOs
    //         List<SuspendedNoticePlusDto> suspendedNoticeDtos = suspendedNotices.stream()
    //             .map(SuspendedNoticePlusDto::new)
    //             .collect(Collectors.toList());

    //         suspendedNoticesMap.put(noticeNo, suspendedNoticeDtos);
    //     }

    //     return suspendedNoticesMap;
    // }

    /**
     * Creates an error response with the specified app code and message
     *
     * @param appCode Application error code
     * @param message Error message
     * @return ResponseEntity with error details
     */
    private ResponseEntity<?> createErrorResponse(String appCode, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("appCode", appCode);
        errorData.put("message", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", errorData);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
