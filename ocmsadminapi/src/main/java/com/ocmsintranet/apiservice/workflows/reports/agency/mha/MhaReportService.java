package com.ocmsintranet.apiservice.workflows.reports.agency.mha;

import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportResponseDto;
import org.springframework.http.ResponseEntity;

public interface MhaReportService {

    ResponseEntity<MhaReportResponseDto> getMhaReport(MhaReportRequestDto request);
}