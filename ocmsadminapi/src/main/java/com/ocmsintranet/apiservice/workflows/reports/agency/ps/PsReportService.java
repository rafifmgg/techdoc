package com.ocmsintranet.apiservice.workflows.reports.agency.ps;

import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto.PsReportResponseDto;
import org.springframework.http.ResponseEntity;

/**
 * OCMS 18 - PS Report Service
 *
 * Service interface for PS Report generation
 * Section 6: Permanent Suspension Report
 */
public interface PsReportService {

    /**
     * Get PS by System report
     * Section 6.3.1: PS suspensions applied by various sub-systems
     *
     * @param request Request DTO containing filters and pagination
     * @return Response with PS report data
     */
    ResponseEntity<PsReportResponseDto> getPsBySystemReport(PsReportRequestDto request);

    /**
     * Get PS by Officer report
     * Section 6.3.2: PS suspensions applied by OICs
     *
     * @param request Request DTO containing filters and pagination
     * @return Response with PS report data
     */
    ResponseEntity<PsReportResponseDto> getPsByOfficerReport(PsReportRequestDto request);
}
