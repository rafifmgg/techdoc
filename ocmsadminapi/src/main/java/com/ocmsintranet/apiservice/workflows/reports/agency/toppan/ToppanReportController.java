package com.ocmsintranet.apiservice.workflows.reports.agency.toppan;

import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/${api.version}/toppan-report")
@Slf4j
public class ToppanReportController {

    @Autowired
    private final ToppanReportService toppanReportService;

    public ToppanReportController(ToppanReportService toppanReportService) {
        this.toppanReportService = toppanReportService;
    }

    /**
     * Get Toppan report files from blob storage
     * POST /v1/toppan-report/search
     *
     * Request Body:
     * {
     *   "source": "CRS" or "ACKNOWLEDGEMENT",
     *   "reportDate": "2024-10-27" (YYYY-MM-DD format)
     * }
     *
     * For CRS:
     *   - Container: ocms
     *   - Lists all files in path: offence/sftp/toppan/input/20241027/ (YYYYMMDD folder)
     *
     * For ACKNOWLEDGEMENT:
     *   - Container: ocms
     *   - Searches for files with prefixes (using YYYYMMDD converted from request):
     *     * offence/sftp/toppan/output/DPT-URA-PDF-D2-20241027*
     *     * offence/sftp/toppan/output/DPT-URA-LOG-PDF-20241027*
     *     * offence/sftp/toppan/output/DPT-URA-LOG-D2-20241027*
     *     * offence/sftp/toppan/output/DPT-URA-RD2-D2-20241027*
     *     * offence/sftp/toppan/output/DPT-URA-DN2-D2-20241027*
     *   - Example files matched: DPT-URA-PDF-D2-20241027143055.pdf, DPT-URA-LOG-D2-20241027150000.log
     *
     * Response:
     * {
     *   "data": [
     *     {
     *       "fileName": "file.txt",
     *       "downloadUrl": "https://...",
     *       "fileSize": 12345
     *     }
     *   ],
     *   "total": 1,
     *   "source": "CRS",
     *   "reportDate": "2024-10-27"
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<ToppanReportResponseDto> getToppanReport(
            @Validated @RequestBody ToppanReportRequestDto request) {
        try {
            log.info("Received Toppan report request - Source: {}, ReportDate: {}",
                    request.getSource(), request.getReportDate());

            return toppanReportService.getToppanReport(request);

        } catch (Exception e) {
            log.error("Error processing Toppan report request", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
