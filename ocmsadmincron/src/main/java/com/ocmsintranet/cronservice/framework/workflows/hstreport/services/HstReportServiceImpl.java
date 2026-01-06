package com.ocmsintranet.cronservice.framework.workflows.hstreport.services;

import com.ocmsintranet.cronservice.framework.workflows.hstreport.dto.ReportResult;
import com.ocmsintranet.cronservice.framework.workflows.hstreport.helpers.HstReportHelper;
import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst.OcmsHst;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst.OcmsHstService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTemp;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp.OcmsNroTempService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of HstReportService
 * Handles Monthly HST report generation and upload to Azure Blob Storage
 */
@Slf4j
@Service
public class HstReportServiceImpl implements HstReportService {

    private final HstReportHelper reportHelper;
    private final AzureBlobStorageUtil blobStorageUtil;
    private final OcmsHstService ocmsHstService;
    private final OcmsNroTempService nroTempService;
    private final EmailService emailService;

    @Value("${blob.folder.hst.report:/offence/reports/hst/}")
    private String blobFolderPath;

    @Value("${email.report.hst.oic.recipients:joey_lee@ura.gov.sg,neo_yi_jie@ura.gov.sg}")
    private String oicEmailRecipients;

    @Value("${spring.profiles.active:unknown}")
    private String environment;

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    public HstReportServiceImpl(
            HstReportHelper reportHelper,
            AzureBlobStorageUtil blobStorageUtil,
            OcmsHstService ocmsHstService,
            OcmsNroTempService nroTempService,
            EmailService emailService) {
        this.reportHelper = reportHelper;
        this.blobStorageUtil = blobStorageUtil;
        this.ocmsHstService = ocmsHstService;
        this.nroTempService = nroTempService;
        this.emailService = emailService;
    }

    @Override
    @Async
    public CompletableFuture<ReportResult> executeJob() {
        log.info("Starting Monthly HST report generation job");

        try {
            // Generate report for current date
            String reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            ReportResult result = generateReport(reportDate);

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error executing Monthly HST report generation job", e);
            return CompletableFuture.completedFuture(
                ReportResult.error("Job execution failed: " + e.getMessage())
            );
        }
    }

    @Override
    public ReportResult generateReport(String reportDate) {
        log.info("Generating Monthly HST report for date: {}", reportDate);

        try {
            // Step 1: Fetch monthly HST data
            log.info("Step 1: Fetching monthly HST data");
            Map<String, Object> reportData = reportHelper.fetchMonthlyHstData();

            if (reportData.isEmpty()) {
                log.warn("No HST data found for report date: {}", reportDate);
                return ReportResult.error("No HST data found");
            }

            int recordCount = (int) reportData.getOrDefault("totalCount", 0);
            log.info("Found {} HST records", recordCount);

            // Step 2: Generate Excel report
            log.info("Step 2: Generating Excel report");
            byte[] excelBytes = reportHelper.generateExcelReport(reportData, reportDate);
            log.info("Excel report generated: {} bytes", excelBytes.length);

            // Step 3: Upload to Azure Blob Storage
            log.info("Step 3: Uploading report to Azure Blob Storage");
            String fileName = generateFileName(reportDate);
            String fullBlobPath = blobFolderPath + fileName;

            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                    blobStorageUtil.uploadBytesToBlob(excelBytes, fullBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload report to blob storage: {}", uploadResponse.getErrorMessage());
                return ReportResult.error("Failed to upload report: " + uploadResponse.getErrorMessage());
            }

            String reportUrl = uploadResponse.getFileUrl();
            log.info("Report uploaded successfully to: {}", reportUrl);

            // Return success result
            return ReportResult.success(reportUrl, recordCount);

        } catch (Exception e) {
            log.error("Error generating Monthly HST report", e);
            return ReportResult.error(e.getMessage());
        }
    }

    /**
     * Generate filename for the report
     *
     * @param reportDate Report date in yyyy-MM-dd format
     * @return Filename in format: MonthlyHSTReport_yyyyMM.xlsx
     */
    private String generateFileName(String reportDate) {
        try {
            LocalDate date = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);
            String yearMonth = date.format(FILENAME_FORMATTER);
            return String.format("MonthlyHSTReport_%s.xlsx", yearMonth);
        } catch (Exception e) {
            log.warn("Failed to parse report date, using current timestamp", e);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            return String.format("MonthlyHSTReport_%s.xlsx", timestamp);
        }
    }

    @Override
    public int queueHstIdsForMonthlyCheck() {
        log.info("Starting monthly HST ID queuing for MHA/DataHive check");

        try {
            // Get all HST records using getAll with empty params
            Map<String, String[]> emptyParams = new HashMap<>();
            com.ocmsintranet.cronservice.crud.beans.FindAllResponse<OcmsHst> response =
                    ocmsHstService.getAll(emptyParams);
            List<OcmsHst> hstRecords = response.getItems();
            log.info("Found {} HST records to queue", hstRecords.size());

            int queuedCount = 0;

            // Queue each HST ID for MHA/DataHive query
            for (OcmsHst hst : hstRecords) {
                try {
                    OcmsNroTemp nroTemp = new OcmsNroTemp();
                    nroTemp.setIdNo(hst.getIdNo());
                    nroTemp.setIdType(hst.getIdType());
                    nroTemp.setQueryReason("HST");
                    nroTemp.setProcessed(false);
                    nroTemp.setCreUserId("SYSTEM_MONTHLY_HST_CHECK");
                    nroTemp.setCreDate(LocalDateTime.now());

                    nroTempService.save(nroTemp);
                    queuedCount++;

                    log.debug("Queued HST ID {} for MHA/DataHive check", hst.getIdNo());

                } catch (Exception e) {
                    log.error("Failed to queue HST ID {} for MHA/DataHive check", hst.getIdNo(), e);
                    // Continue with next record
                }
            }

            log.info("Successfully queued {} out of {} HST IDs for monthly MHA/DataHive check",
                    queuedCount, hstRecords.size());

            return queuedCount;

        } catch (Exception e) {
            log.error("Error queuing HST IDs for monthly check", e);
            return 0;
        }
    }

    @Override
    public ReportResult checkAndGenerateReports() {
        log.info("Checking for new MHA/DataHive results and generating reports if ready");

        try {
            // Check if there are unprocessed results in ocms_temp_unc_hst_addr
            boolean hasNewResults = reportHelper.hasUnprocessedHstResults();

            if (!hasNewResults) {
                log.info("No new MHA/DataHive HST results available for reporting");
                return null;
            }

            log.info("Found new MHA/DataHive HST results. Generating reports...");
            String reportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Generate Monthly HST Data Report (with MHA/DataHive address data)
            log.info("Step 1: Generating Monthly HST Data Report with MHA/DataHive results");
            ReportResult dataReportResult = generateMonthlyHstDataReport(reportDate);

            if (!dataReportResult.isSuccess()) {
                log.error("Failed to generate Monthly HST Data Report: {}", dataReportResult.getMessage());
                return dataReportResult;
            }

            // Generate Monthly HST Work Items Report (11 statistics)
            log.info("Step 2: Generating Monthly HST Work Items Report");
            ReportResult workItemsResult = generateMonthlyHstWorkItemsReport(reportDate);

            if (!workItemsResult.isSuccess()) {
                log.error("Failed to generate Monthly HST Work Items Report: {}", workItemsResult.getMessage());
                return workItemsResult;
            }

            // Mark HST results as processed
            reportHelper.markHstResultsAsProcessed();

            log.info("Successfully generated both HST reports");
            return ReportResult.success(
                String.format("Generated 2 reports: Data Report (%d records), Work Items Report",
                    dataReportResult.getRecordCount()),
                dataReportResult.getRecordCount()
            );

        } catch (Exception e) {
            log.error("Error checking for MHA/DataHive results", e);
            return ReportResult.error("Failed to check for results: " + e.getMessage());
        }
    }

    /**
     * Generate Monthly HST Data Report with MHA/DataHive address comparison
     * This report shows HST IDs with old vs new addresses from MHA/DataHive
     */
    private ReportResult generateMonthlyHstDataReport(String reportDate) {
        log.info("Generating Monthly HST Data Report with MHA/DataHive comparison");

        try {
            // Fetch HST data with MHA/DataHive address comparison
            Map<String, Object> reportData = reportHelper.fetchHstDataWithAddressComparison();

            if (reportData.isEmpty()) {
                log.warn("No HST data with address comparison found");
                return ReportResult.error("No HST data available");
            }

            int recordCount = (int) reportData.getOrDefault("totalCount", 0);
            log.info("Found {} HST records with address data", recordCount);

            // Generate Excel report
            byte[] excelBytes = reportHelper.generateHstDataComparisonReport(reportData, reportDate);
            log.info("Monthly HST Data Report generated: {} bytes", excelBytes.length);

            // Upload to Azure Blob Storage
            String fileName = String.format("MonthlyHSTDataReport_%s.xlsx",
                LocalDate.parse(reportDate).format(FILENAME_FORMATTER));
            String fullBlobPath = blobFolderPath + fileName;

            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                blobStorageUtil.uploadBytesToBlob(excelBytes, fullBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload HST Data Report: {}", uploadResponse.getErrorMessage());
                return ReportResult.error("Failed to upload report: " + uploadResponse.getErrorMessage());
            }

            log.info("Monthly HST Data Report uploaded to: {}", uploadResponse.getFileUrl());
            return ReportResult.success(uploadResponse.getFileUrl(), recordCount);

        } catch (Exception e) {
            log.error("Error generating Monthly HST Data Report", e);
            return ReportResult.error(e.getMessage());
        }
    }

    /**
     * Generate Monthly HST Work Items Report (11 statistics)
     * This report contains summary statistics comparing old vs new addresses
     * and is emailed to OIC
     */
    private ReportResult generateMonthlyHstWorkItemsReport(String reportDate) {
        log.info("Generating Monthly HST Work Items Report (statistics)");

        try {
            // Fetch HST work items statistics
            Map<String, Object> statistics = reportHelper.calculateHstWorkItemsStatistics();

            if (statistics.isEmpty()) {
                log.warn("No HST statistics available");
                return ReportResult.error("No statistics available");
            }

            log.info("Calculated HST work items statistics: {}", statistics);

            // Generate Excel report with statistics
            byte[] excelBytes = reportHelper.generateHstWorkItemsReport(statistics, reportDate);
            log.info("Monthly HST Work Items Report generated: {} bytes", excelBytes.length);

            // Upload to Azure Blob Storage
            String fileName = String.format("MonthlyHSTWorkItemsReport_%s.xlsx",
                LocalDate.parse(reportDate).format(FILENAME_FORMATTER));
            String fullBlobPath = blobFolderPath + fileName;

            AzureBlobStorageUtil.FileUploadResponse uploadResponse =
                blobStorageUtil.uploadBytesToBlob(excelBytes, fullBlobPath);

            if (!uploadResponse.isSuccess()) {
                log.error("Failed to upload HST Work Items Report: {}", uploadResponse.getErrorMessage());
                return ReportResult.error("Failed to upload report: " + uploadResponse.getErrorMessage());
            }

            log.info("Monthly HST Work Items Report uploaded to: {}", uploadResponse.getFileUrl());

            // Email report to OIC with attachment
            boolean emailSent = sendHstWorkItemsReportEmail(
                uploadResponse.getFileUrl(),
                fileName,
                excelBytes,
                statistics
            );

            if (!emailSent) {
                log.warn("Failed to send email notification to OIC. Report still available at: {}",
                    uploadResponse.getFileUrl());
            }

            return ReportResult.success(uploadResponse.getFileUrl(), 1);

        } catch (Exception e) {
            log.error("Error generating Monthly HST Work Items Report", e);
            return ReportResult.error(e.getMessage());
        }
    }

    /**
     * Send HST Work Items Report email to OIC with Excel attachment
     *
     * @param reportUrl The Azure Blob Storage URL of the report
     * @param fileName The report filename
     * @param excelBytes The Excel file content
     * @param statistics The report statistics for email body
     * @return true if email sent successfully, false otherwise
     */
    private boolean sendHstWorkItemsReportEmail(
            String reportUrl,
            String fileName,
            byte[] excelBytes,
            Map<String, Object> statistics) {

        try {
            if (oicEmailRecipients == null || oicEmailRecipients.trim().isEmpty()) {
                log.warn("No OIC email recipients configured for HST Work Items Report");
                return false;
            }

            // Build email content with statistics summary
            String emailContent = buildHstWorkItemsEmailContent(reportUrl, statistics);

            // Create email request with attachment
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(oicEmailRecipients);
            emailRequest.setSubject(String.format("[%s] Monthly HST Work Items Report - %s",
                environment.toUpperCase(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))));
            emailRequest.setHtmlContent(emailContent);

            // Attach the Excel report
            EmailRequest.Attachment attachment = new EmailRequest.Attachment();
            attachment.setFileName(fileName);
            attachment.setFileContent(excelBytes);
            emailRequest.setAttachments(java.util.Collections.singletonList(attachment));

            boolean sent = emailService.sendEmail(emailRequest);

            if (sent) {
                log.info("HST Work Items Report email sent successfully to OIC: {}", oicEmailRecipients);
            } else {
                log.error("Failed to send HST Work Items Report email to OIC");
            }

            return sent;

        } catch (Exception e) {
            log.error("Error sending HST Work Items Report email to OIC", e);
            return false;
        }
    }

    /**
     * Build HTML email content for HST Work Items Report
     *
     * @param reportUrl The Azure Blob Storage URL
     * @param statistics The report statistics
     * @return HTML formatted email content
     */
    private String buildHstWorkItemsEmailContent(String reportUrl, Map<String, Object> statistics) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>Monthly HST Work Items Report</h2>");
        html.append("<p>Dear OIC,</p>");
        html.append("<p>Please find attached the Monthly HST Work Items Report for ")
            .append(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")))
            .append(".</p>");

        html.append("<h3>Report Summary:</h3>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        html.append("<tr><th style='background-color: #f0f0f0; text-align: left;'>Statistic</th>")
            .append("<th style='background-color: #f0f0f0; text-align: right;'>Count</th></tr>");

        // Display all 11 statistics
        addStatisticRow(html, "Total HST IDs", statistics.get("totalHstIds"));
        addStatisticRow(html, "HST IDs Checked This Month", statistics.get("hstIdsCheckedThisMonth"));
        addStatisticRow(html, "Addresses Changed", statistics.get("addressesChanged"));
        addStatisticRow(html, "Addresses Marked Invalid", statistics.get("addressesMarkedInvalid"));
        addStatisticRow(html, "Addresses Now Valid", statistics.get("addressesNowValid"));
        addStatisticRow(html, "Total Active TS-HST Suspensions", statistics.get("totalActiveTsHstSuspensions"));
        addStatisticRow(html, "New TS-HST Suspensions This Month", statistics.get("newTsHstSuspensionsThisMonth"));
        addStatisticRow(html, "TS-HST Reapplied (Looping)", statistics.get("tsHstReappliedLooping"));
        addStatisticRow(html, "Total Notices Under TS-HST", statistics.get("totalNoticesUnderTsHst"));
        addStatisticRow(html, "HST IDs Eligible for Revival", statistics.get("hstIdsEligibleForRevival"));
        addStatisticRow(html, "HST IDs Still Suspended", statistics.get("hstIdsStillSuspended"));

        html.append("</table>");

        html.append("<p><br/>The detailed Excel report is attached to this email.</p>");
        html.append("<p>Report also available at: <a href='").append(reportUrl).append("'>Azure Blob Storage</a></p>");

        html.append("<p><br/>Best regards,<br/>");
        html.append("OCMS Automated Reporting System</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Helper method to add a statistic row to the email HTML table
     */
    private void addStatisticRow(StringBuilder html, String label, Object value) {
        html.append("<tr><td>").append(label).append("</td>")
            .append("<td style='text-align: right;'>").append(value != null ? value.toString() : "0")
            .append("</td></tr>");
    }
}
