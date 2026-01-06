package com.ocmsintranet.apiservice.workflows.reports.agency.ps;

import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * OCMS 18 - PS Report Service Implementation
 *
 * Service implementation for PS Report generation
 * Section 6: Permanent Suspension Report
 */
@Service
@Slf4j
public class PsReportServiceImpl implements PsReportService {

    @Autowired
    private SuspendedNoticeService suspendedNoticeService;

    @Override
    public ResponseEntity<PsReportResponseDto> getPsBySystemReport(PsReportRequestDto request) {
        try {
            log.info("Processing PS by System report request");
            return generatePsReport(request, false);
        } catch (Exception e) {
            log.error("Error processing PS by System report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<PsReportResponseDto> getPsByOfficerReport(PsReportRequestDto request) {
        try {
            log.info("Processing PS by Officer report request");
            return generatePsReport(request, true);
        } catch (Exception e) {
            log.error("Error processing PS by Officer report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Common method to generate PS reports
     *
     * @param request Request DTO
     * @param includeOfficerFields Whether to include officer-specific fields (for PS by Officer report)
     * @return Response with PS report data
     */
    private ResponseEntity<PsReportResponseDto> generatePsReport(PsReportRequestDto request, boolean includeOfficerFields) {
        // Set default values if not provided
        if (request.getSkip() == null) {
            request.setSkip(0);
        }
        if (request.getLimit() == null) {
            request.setLimit(10);
        }

        // Validate date range is provided
        if (request.getDateFromSuspension() == null || request.getDateToSuspension() == null) {
            log.error("Date range is required for PS report");
            return ResponseEntity.badRequest().build();
        }

        // Convert LocalDateTime to yyyy-MM-dd format
        String fromDate = request.getDateFromSuspension().toLocalDate().toString();
        String toDate = request.getDateToSuspension().toLocalDate().toString();

        // Build order by map from request (single field only), or use default
        Map<String, Integer> orderByFields = new HashMap<>();
        if (request.getSortField() != null && !request.getSortField().isEmpty()) {
            Integer direction = request.getSortDirection() != null ? request.getSortDirection() : 1;
            orderByFields.put(request.getSortField(), direction);
            log.info("Sorting by field: {}, direction: {}", request.getSortField(), direction);
        } else {
            orderByFields.put("dateOfSuspension", 1); // Default sort by date_of_suspension ASC
            log.info("Using default sort: dateOfSuspension ASC");
        }

        // Call the custom suspended notice service
        List<Map<String, Object>> psRecords = suspendedNoticeService.getPsReportRecords(
            fromDate,
            toDate,
            request.getSuspensionSource(),
            request.getOfficerAuthorisingSupension(),
            request.getCreUserId(),
            orderByFields
        );

        log.info("Retrieved {} PS report records", psRecords.size());

        // Convert PS records to DTO
        List<PsReportResponseDto.PsRecord> psReportRecords = convertToPsRecords(psRecords, includeOfficerFields);

        // Apply pagination
        int total = psReportRecords.size();
        int fromIndex = Math.min(request.getSkip(), total);
        int toIndex = Math.min(request.getSkip() + request.getLimit(), total);
        List<PsReportResponseDto.PsRecord> paginatedRecords = psReportRecords.subList(fromIndex, toIndex);

        PsReportResponseDto response = new PsReportResponseDto();
        response.setData(paginatedRecords);
        response.setTotal(total);
        response.setSkip(request.getSkip());
        response.setLimit(request.getLimit());

        return ResponseEntity.ok(response);
    }

    /**
     * Convert suspended notice Map records to PsRecord DTOs
     */
    private List<PsReportResponseDto.PsRecord> convertToPsRecords(List<Map<String, Object>> psRecords, boolean includeOfficerFields) {
        List<PsReportResponseDto.PsRecord> records = new ArrayList<>();

        for (Map<String, Object> record : psRecords) {
            PsReportResponseDto.PsRecord psRecord = new PsReportResponseDto.PsRecord();

            // From ocms_valid_offence_notice
            psRecord.setVehicleNo(convertToString(record.get("vehicleNo")));
            psRecord.setOffenceNoticeType(convertToString(record.get("offenceNoticeType")));
            psRecord.setVehicleRegistrationType(convertToString(record.get("vehicleRegistrationType")));
            psRecord.setVehicleCategory(convertToString(record.get("vehicleCategory")));
            psRecord.setComputerRuleCode(convertToString(record.get("computerRuleCode")));
            psRecord.setPpCode(convertToString(record.get("ppCode")));

            // From ocms_suspended_notice
            psRecord.setNoticeNo(convertToString(record.get("noticeNo")));
            psRecord.setReasonOfSuspension(convertToString(record.get("reasonOfSuspension")));
            psRecord.setSuspensionSource(convertToString(record.get("suspensionSource")));

            // Officer fields - only for PS by Officer report
            if (includeOfficerFields) {
                psRecord.setOfficerAuthorisingSupension(convertToString(record.get("officerAuthorisingSupension")));
                psRecord.setCreUserId(convertToString(record.get("creUserId")));
                psRecord.setSuspensionRemarks(convertToString(record.get("suspensionRemarks")));
            }

            // Previous PS details
            psRecord.setPreviousPsReason(convertToString(record.get("previousPsReason")));

            // Date fields
            psRecord.setNoticeDateAndTime(convertToLocalDateTime(record.get("noticeDateAndTime")));
            psRecord.setDateOfSuspension(convertToLocalDateTime(record.get("dateOfSuspension")));
            psRecord.setRefundIdentifiedDate(convertToLocalDateTime(record.get("refundIdentifiedDate")));
            psRecord.setPreviousPsDate(convertToLocalDateTime(record.get("previousPsDate")));

            records.add(psRecord);
        }

        return records;
    }

    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Character) {
            return obj.toString();
        }
        return obj.toString();
    }

    private java.time.LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        } else if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate().atStartOfDay();
        } else if (obj instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) obj;
        } else if (obj instanceof java.time.LocalDate) {
            return ((java.time.LocalDate) obj).atStartOfDay();
        }
        return null;
    }
}
