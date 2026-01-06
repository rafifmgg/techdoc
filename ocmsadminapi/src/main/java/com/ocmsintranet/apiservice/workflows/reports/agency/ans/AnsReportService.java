package com.ocmsintranet.apiservice.workflows.reports.agency.ans;

import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto.AnsReportResponseDto;

/**
 * Service interface for Advisory Notice System Reports (OCMS 10 Section 9)
 */
public interface AnsReportService {

    /**
     * Generate Ad-Hoc ANS Report with Excel export (Section 9.2)
     *
     * Creates an Excel file with 2 tabs:
     * - Tab 1: ANS (eNotifications sent via SMS/Email)
     * - Tab 2: AN Letter (Physical letters sent to Toppan)
     *
     * @param request Search criteria
     * @return Response with download URL and statistics
     * @throws AnsReportException if report generation fails
     */
    AnsReportResponseDto generateAdHocReport(AnsReportRequestDto request) throws AnsReportException;

    /**
     * Custom exception for ANS report generation errors
     */
    class AnsReportException extends Exception {
        public AnsReportException(String message) {
            super(message);
        }

        public AnsReportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
