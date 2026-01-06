package com.ocmsintranet.apiservice.workflows.plus.evalidnotice;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.workflows.plus.evalidnotice.dto.EocmsValidOffenceNoticePlusDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvalidNoticeServiceImpl implements EvalidNoticeService {

    @Override
    public ResponseEntity<?> processPlusEValidOffenceNotice(Map<String, Object> requestBody) {
        log.info("Processing PLUS internet valid offence notice request with body: {}", requestBody);

        try {
            // Handle optional filtering
            String vehicleNo = null;
            String noticeNo = null;
            String paymentStatus = null;

            if (requestBody != null) {
                vehicleNo = (String) requestBody.get("vehicleNo");

                // Handle noticeNo with or without operators (e.g., noticeNo[$in])
                noticeNo = (String) requestBody.get("noticeNo");
                if (noticeNo == null) {
                    // Check for noticeNo with operators like noticeNo[$in], noticeNo[$ne], etc.
                    for (String key : requestBody.keySet()) {
                        if (key.startsWith("noticeNo")) {
                            Object value = requestBody.get(key);
                            if (value instanceof String) {
                                noticeNo = (String) value;
                                // For operators like $in, get the first value
                                if (noticeNo.contains(",")) {
                                    noticeNo = noticeNo.split(",")[0].trim();
                                }
                                break;
                            }
                        }
                    }
                }

                paymentStatus = (String) requestBody.get("paymentStatus");
            }

            // Handle pagination parameters
            int skip = 0;
            int limit = 10;

            if (requestBody != null) {
                Object skipObj = requestBody.get("$skip");
                if (skipObj != null) {
                    skip = Integer.parseInt(skipObj.toString());
                }

                Object limitObj = requestBody.get("$limit");
                if (limitObj != null) {
                    limit = Integer.parseInt(limitObj.toString());
                }
            }

            log.info("Returning dummy data for vehicleNo: {}, noticeNo: {}, paymentStatus: {}, skip: {}, limit: {}",
                    vehicleNo, noticeNo, paymentStatus, skip, limit);

            // Generate dummy response
            FindAllResponse<EocmsValidOffenceNoticePlusDto> plusResponse = generateDummyInternetNoticeResponse(
                    vehicleNo, noticeNo, paymentStatus, skip, limit);

            log.info("Returning PLUS internet valid offence notice response with {} items",
                    plusResponse.getData().size());

            return ResponseEntity.ok(plusResponse);

        } catch (Exception e) {
            log.error("Error retrieving PLUS internet valid offence notices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate dummy response for PLUS Internet Valid Offence Notice
     */
    private FindAllResponse<EocmsValidOffenceNoticePlusDto> generateDummyInternetNoticeResponse(
            String vehicleNo, String noticeNo, String paymentStatus, int skip, int limit) {
        FindAllResponse<EocmsValidOffenceNoticePlusDto> response = new FindAllResponse<>();

        // Create dummy data
        List<EocmsValidOffenceNoticePlusDto> dummyData = createDummyInternetNoticeData(vehicleNo, noticeNo, paymentStatus);

        // Apply pagination
        int total = dummyData.size();
        int endIndex = Math.min(skip + limit, total);
        List<EocmsValidOffenceNoticePlusDto> paginatedData = skip < total ?
                dummyData.subList(skip, endIndex) : new ArrayList<>();

        // Set response
        response.setTotal(total);
        response.setLimit(limit);
        response.setSkip(skip);
        response.setData(paginatedData);

        return response;
    }

    /**
     * Create dummy Internet Valid Offence Notice data
     */
    private List<EocmsValidOffenceNoticePlusDto> createDummyInternetNoticeData(
            String vehicleNo, String noticeNo, String paymentStatus) {
        List<EocmsValidOffenceNoticePlusDto> dummyData = new ArrayList<>();

        EocmsValidOffenceNoticePlusDto notice = new EocmsValidOffenceNoticePlusDto();
        notice.setNoticeNo(noticeNo != null ? noticeNo : "E2025001001");
        notice.setVehicleNo(vehicleNo != null ? vehicleNo : "SBA1234A");
        notice.setOffenceNoticeType("O");
        notice.setEprReasonOfSuspension(null);
        notice.setCrsReasonOfSuspension("Non-payment of composition");
        notice.setCrsDateOfSuspension(LocalDateTime.of(2025, 3, 15, 10, 30, 0));
        notice.setNextProcessingStage("CRT");
        notice.setEprDateOfSuspension(LocalDateTime.of(2025, 2, 20, 14, 0, 0));

        // Set processing stage and an_flag based on noticeNo
        if (noticeNo != null) {
            switch (noticeNo) {
                case "441000223K":
                    // RD1, an_flag: N
                    notice.setLastProcessingStage("RD1");
                    notice.setAnFlag("N");
                    break;
                case "441000456V":
                    // RD1, an_flag: Y
                    notice.setLastProcessingStage("RD1");
                    notice.setAnFlag("Y");
                    break;
                case "441000405B":
                    // CPC, an_flag: N
                    notice.setLastProcessingStage("CPC");
                    notice.setAnFlag("N");
                    break;
                default:
                    // CPC, an_flag: N
                    notice.setLastProcessingStage("CPC");
                    notice.setAnFlag("N");
                    break;
            }
        } else {
            // Default values when noticeNo is null
            notice.setLastProcessingStage("CPC");
            notice.setAnFlag("N");
        }

        dummyData.add(notice);

        return dummyData;
    }
}
