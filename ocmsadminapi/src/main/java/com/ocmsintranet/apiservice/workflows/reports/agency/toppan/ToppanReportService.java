package com.ocmsintranet.apiservice.workflows.reports.agency.toppan;

import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportResponseDto;
import org.springframework.http.ResponseEntity;

public interface ToppanReportService {

    /**
     * Get Toppan report files from blob storage
     * @param request Request containing source (CRS/ACKNOWLEDGEMENT) and reportDate (YYYYMMDD)
     * @return Response containing list of files with download URLs
     */
    ResponseEntity<ToppanReportResponseDto> getToppanReport(ToppanReportRequestDto request);
}
