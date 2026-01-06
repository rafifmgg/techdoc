package com.ocmsintranet.apiservice.workflows.reports.agency.ans;

import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for Advisory Notice System Reports (OCMS 10 Section 9)
 *
 * Endpoints:
 * - POST /v1/ans-report/adhoc - Generate Ad-Hoc ANS Report with search criteria
 */
@RestController
@RequestMapping("/${api.version}/ans-report")
@Slf4j
public class AnsReportController {

    @Autowired
    private final AnsReportService ansReportService;

    public AnsReportController(AnsReportService ansReportService) {
        this.ansReportService = ansReportService;
    }

    /**
     * Generate Ad-Hoc ANS Report (Section 9.2)
     * POST /v1/ans-report/adhoc
     *
     * Request Body:
     * {
     *   "dateOfEansSent": "2024-10-27" (YYYY-MM-DD, optional),
     *   "dateOfAnsLetter": "2024-10-27" (YYYY-MM-DD, optional),
     *   "dateOfOffence": "2024-10-27" (YYYY-MM-DD, optional),
     *   "vehicleRegistrationType": "S" (optional),
     *   "ppCode": "ABC123" (optional),
     *   "carParkName": "City Hall" (optional)
     * }
     *
     * Response:
     * {
     *   "downloadUrl": "https://.../ans-report-20241027.xlsx",
     *   "fileName": "20241027150000-ANS-Report.xlsx",
     *   "fileSize": 45678,
     *   "ansCount": 120,
     *   "anLetterCount": 80,
     *   "totalRecords": 200,
     *   "generatedAt": "2024-10-27T15:00:00",
     *   "searchCriteria": { ... }
     * }
     *
     * Excel file contains 2 tabs:
     * - Tab 1: ANS (eNotifications - SMS/Email sent)
     * - Tab 2: AN Letter (Physical letters sent to Toppan)
     */
    @PostMapping("/adhoc")
    public ResponseEntity<AnsReportResponseDto> generateAdHocReport(
            @Validated @RequestBody AnsReportRequestDto request) {
        try {
            log.info("Received Ad-Hoc ANS Report request with criteria: eAN date={}, letter date={}, offence date={}, vehicle type={}, PP code={}, car park={}",
                    request.getDateOfEansSent(),
                    request.getDateOfAnsLetter(),
                    request.getDateOfOffence(),
                    request.getVehicleRegistrationType(),
                    request.getPpCode(),
                    request.getCarParkName());

            AnsReportResponseDto response = ansReportService.generateAdHocReport(request);

            log.info("Successfully generated Ad-Hoc ANS Report: {} ANS records, {} AN Letter records, file={}",
                    response.getAnsCount(),
                    response.getAnLetterCount(),
                    response.getFileName());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating Ad-Hoc ANS Report", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
