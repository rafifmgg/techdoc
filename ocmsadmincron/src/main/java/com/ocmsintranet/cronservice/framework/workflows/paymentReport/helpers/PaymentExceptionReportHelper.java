package com.ocmsintranet.cronservice.framework.workflows.paymentReport.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class for Payment Exception Report generation.
 * This class handles data fetching and HTML email body generation.
 *
 * The report identifies payment exceptions including:
 * - PS-PRA: Permanent Suspension - Payment Refund Approved
 * - TS-PAM: Temporary Suspension - Payment Amount Mismatch
 * - Refund notices created on the report date
 *
 * The query joins multiple tables:
 * - ocms_valid_offence_notice
 * - ocms_refund_notice
 * - ocms_web_txn_detail
 */
@Slf4j
@Component
public class PaymentExceptionReportHelper {

    private final JdbcTemplate jdbcTemplate;

    public PaymentExceptionReportHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches payment exception records from the database for the specified date
     *
     * @param reportDate The date for which to fetch data (format: yyyy-MM-dd)
     * @return Map containing the fetched records and count
     */
    public Map<String, Object> fetchExceptionRecords(String reportDate) {
        log.info("[PaymentExceptionReport] Fetching exception records for date: {}", reportDate);

        try {
            // Parse the report date
            LocalDate reportDateObj = LocalDate.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE);

            // SQL query to fetch PS-PRA and TS-PAM exception records
            String sql = "SELECT " +
                        "von.notice_no, " +
                        "von.suspension_type, " +
                        "von.crs_reason_of_suspension, " +
                        "von.epr_reason_of_suspension, " +
                        "wtd.transaction_date_and_time, " +
                        "wtd.receipt_no, " +
                        "wtd.receipt_no as transaction_id, " +
                        "wtd.sender, " +
                        "wtd.payment_amount " +
                        "FROM ocmsizmgr.ocms_valid_offence_notice von " +
                        "LEFT JOIN ocmsizmgr.ocms_web_txn_detail wtd ON wtd.offence_notice_no = von.notice_no " +
                        "WHERE ( " +
                        "    von.suspension_type = 'PS' " +
                        "    AND CAST(von.crs_date_of_suspension AS DATE) = ? " +
                        "    AND von.crs_reason_of_suspension = 'PRA' " +
                        ") " +
                        "OR ( " +
                        "    von.suspension_type = 'TS' " +
                        "    AND CAST(von.epr_date_of_suspension AS DATE) = ? " +
                        "    AND von.epr_reason_of_suspension = 'PAM' " +
                        ")";

            // Execute PS-PRA and TS-PAM query
            List<Map<String, Object>> exceptionRecords = jdbcTemplate.queryForList(
                sql,
                java.sql.Date.valueOf(reportDateObj),
                java.sql.Date.valueOf(reportDateObj)
            );

            log.info("[PaymentExceptionReport] Found {} PS-PRA/TS-PAM records for date: {}", exceptionRecords.size(), reportDate);

            // Deduplicate exception records - keep only first transaction per notice
            Map<String, Map<String, Object>> firstByNoticeNo = new LinkedHashMap<>();
            for (Map<String, Object> record : exceptionRecords) {
                String noticeNo = record.get("notice_no") != null ? record.get("notice_no").toString() : "";
                if (noticeNo.isEmpty()) continue;

                Map<String, Object> existing = firstByNoticeNo.get(noticeNo);
                if (existing == null) {
                    firstByNoticeNo.put(noticeNo, record);
                } else {
                    // Compare transaction_date_and_time to keep the earliest (first)
                    Object currentDateTime = record.get("transaction_date_and_time");
                    Object existingDateTime = existing.get("transaction_date_and_time");

                    if (currentDateTime != null && existingDateTime != null) {
                        java.sql.Timestamp currentTs = currentDateTime instanceof java.sql.Timestamp ?
                            (java.sql.Timestamp) currentDateTime : null;
                        java.sql.Timestamp existingTs = existingDateTime instanceof java.sql.Timestamp ?
                            (java.sql.Timestamp) existingDateTime : null;

                        if (currentTs != null && existingTs != null && currentTs.before(existingTs)) {
                            firstByNoticeNo.put(noticeNo, record);
                        }
                    }
                }
            }

            // Separate query for REFUND records - directly from ocms_refund_notice table
            // receipt_no is now a direct field in ocms_refund_notice (no SUBSTRING needed)
            String refundSql = "SELECT " +
                        "rn.notice_no, " +
                        "rn.suspension_type, " +
                        "rn.crs_reason_of_suspension, " +
                        "rn.epr_reason_of_suspension, " +
                        "wtd.transaction_date_and_time, " +
                        "rn.receipt_no, " +
                        "rn.receipt_no as transaction_id, " +
                        "wtd.sender, " +
                        "wtd.payment_amount, " +
                        "rn.cre_date as refund_identified_date " +
                        "FROM ocmsizmgr.ocms_refund_notice rn " +
                        "LEFT JOIN ocmsizmgr.ocms_web_txn_detail wtd ON wtd.offence_notice_no = rn.notice_no " +
                        "    AND wtd.receipt_no = rn.receipt_no " +
                        "WHERE CAST(rn.cre_date AS DATE) = ?";

            List<Map<String, Object>> refundRecords = jdbcTemplate.queryForList(
                refundSql,
                java.sql.Date.valueOf(reportDateObj)
            );

            log.info("[PaymentExceptionReport] Found {} refund records for date: {}", refundRecords.size(), reportDate);

            // Combine exception records and refund records
            List<Map<String, Object>> records = new ArrayList<>(firstByNoticeNo.values());

            // Add exception_type to PS-PRA/TS-PAM records and collect their receipt_nos
            Set<String> exceptionReceiptNos = new HashSet<>();
            for (Map<String, Object> record : records) {
                record.put("exception_type", determineSingleRecordType(record));
                String receiptNo = record.get("receipt_no") != null ? record.get("receipt_no").toString() : "";
                if (!receiptNo.isEmpty()) {
                    exceptionReceiptNos.add(receiptNo);
                }
            }

            // Add refund records with REFUND type, skip if receipt already shown as PS-PRA/TS-PAM
            for (Map<String, Object> refund : refundRecords) {
                String receiptNo = refund.get("receipt_no") != null ? refund.get("receipt_no").toString() : "";
                if (!exceptionReceiptNos.contains(receiptNo)) {
                    refund.put("exception_type", "REFUND");
                    records.add(refund);
                }
            }

            // Sort all records by notice_no and transaction_date_and_time
            records.sort((r1, r2) -> {
                String notice1 = r1.get("notice_no") != null ? r1.get("notice_no").toString() : "";
                String notice2 = r2.get("notice_no") != null ? r2.get("notice_no").toString() : "";
                int noticeCompare = notice1.compareTo(notice2);
                if (noticeCompare != 0) return noticeCompare;

                Object dt1 = r1.get("transaction_date_and_time");
                Object dt2 = r2.get("transaction_date_and_time");
                if (dt1 == null && dt2 == null) return 0;
                if (dt1 == null) return 1;
                if (dt2 == null) return -1;
                return dt1.toString().compareTo(dt2.toString());
            });

            int totalCount = records.size();

            log.info("[PaymentExceptionReport] Found {} exception records for date: {}", totalCount, reportDate);

            if (!records.isEmpty()) {
                log.debug("[PaymentExceptionReport] Sample record: {}", records.get(0));
            }

            // Prepare result map
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("records", records);

            return result;

        } catch (Exception e) {
            log.error("[PaymentExceptionReport] Error fetching exception records: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Generate HTML email body with the exception records
     *
     * @param records The list of exception records
     * @param reportDate The report date
     * @return HTML string for email body
     */
    public String generateEmailBody(List<Map<String, Object>> records, String reportDate) {
        log.info("[PaymentExceptionReport] Generating email body for {} records", records.size());

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2>OCMS Payment Exception Report</h2>");
        html.append("<p><strong>Report Date:</strong> ").append(reportDate).append("</p>");
        html.append("<p><strong>Total Records:</strong> ").append(records.size()).append("</p>");
        html.append("<hr/>");

        if (records.isEmpty()) {
            html.append("<p style='color: green; font-weight: bold;'>No exception records found for this date.</p>");
        } else {
            html.append("<p style='color: orange; font-weight: bold;'>Exception records requiring attention:</p>");
            html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%; font-size: 12px;'>");

            // Table header - matching Excel format exactly
            html.append("<thead style='background-color: #4CAF50; color: white;'>");
            html.append("<tr>");
            html.append("<th>S/N</th>");
            html.append("<th>Transaction Date/Time</th>");
            html.append("<th>Exception Type</th>");
            html.append("<th>Notice No.</th>");
            html.append("<th>Receipt No.</th>");
            html.append("<th>Transaction ID</th>");
            html.append("<th>Payment Channel</th>");
            html.append("<th>Amount Paid</th>");
            html.append("</tr>");
            html.append("</thead>");

            // Table body
            html.append("<tbody>");
            int sn = 1;
            for (Map<String, Object> record : records) {
                // Get pre-determined exception type (set by determineExceptionTypes)
                String exceptionType = record.get("exception_type") != null ? record.get("exception_type").toString() : "";

                // Determine payment channel based on sender
                String sender = record.get("sender") != null ? record.get("sender").toString().toUpperCase() : "";
                String paymentChannel = "";
                if (sender.contains("URA")) {
                    paymentChannel = "eService";
                } else if (sender.contains("AXS")) {
                    paymentChannel = "AXS";
                } else if (sender.contains("OCMS")) {
                    paymentChannel = "Offline";
                } else if (sender.contains("JTC")) {
                    paymentChannel = "JTC";
                } else {
                    paymentChannel = sender;
                }

                html.append("<tr style='background-color: ").append(sn % 2 == 0 ? "#f2f2f2" : "white").append(";'>");
                html.append("<td align='center'>").append(sn++).append("</td>");
                html.append("<td>").append(formatValue(record.get("transaction_date_and_time"))).append("</td>");
                html.append("<td align='center'>").append(exceptionType).append("</td>");
                html.append("<td>").append(formatValue(record.get("notice_no"))).append("</td>");
                html.append("<td>").append(formatValue(record.get("receipt_no"))).append("</td>");
                html.append("<td>").append(formatValue(record.get("transaction_id"))).append("</td>");
                html.append("<td align='center'>").append(paymentChannel).append("</td>");
                html.append("<td align='right'>").append(formatValue(record.get("payment_amount"))).append("</td>");
                html.append("</tr>");
            }
            html.append("</tbody>");
            html.append("</table>");
        }

        html.append("<br/>");
        html.append("<p style='font-size: 11px; color: #666;'>This is an automated report. Please do not reply to this email.</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Format value for display in HTML table
     * Handles null values and converts objects to strings
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "-";
        }

        // Format dates if needed
        if (value instanceof java.sql.Timestamp || value instanceof java.sql.Date) {
            return value.toString();
        }

        return value.toString();
    }
    /**
     * Determine exception types for each record with proper grouping logic.
     * For notices with PS-PRA status and refund records:
     * - First transaction (earliest by date) → PS-PRA
     * - Subsequent transactions → REFUND
     *
     * @param records The list of records to process
     * @return The same list with exception_type field added to each record
     */
    private List<Map<String, Object>> determineExceptionTypes(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return records;
        }

        // Group records by notice_no
        Map<String, List<Map<String, Object>>> groupedByNotice = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            String noticeNo = record.get("notice_no") != null ? record.get("notice_no").toString() : "";
            groupedByNotice.computeIfAbsent(noticeNo, k -> new ArrayList<>()).add(record);
        }

        // Process each notice group
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByNotice.entrySet()) {
            List<Map<String, Object>> noticeRecords = entry.getValue();

            if (noticeRecords.size() == 1) {
                // Single record - determine type normally
                Map<String, Object> record = noticeRecords.get(0);
                record.put("exception_type", determineSingleRecordType(record));
            } else {
                // Multiple records for same notice - need special handling
                // Sort by transaction_date_and_time to find the earliest
                noticeRecords.sort((r1, r2) -> {
                    Object dt1 = r1.get("transaction_date_and_time");
                    Object dt2 = r2.get("transaction_date_and_time");
                    if (dt1 == null && dt2 == null) return 0;
                    if (dt1 == null) return 1;
                    if (dt2 == null) return -1;
                    return dt1.toString().compareTo(dt2.toString());
                });

                // Check if this notice has PS-PRA or TS-PAM status
                Map<String, Object> firstRecord = noticeRecords.get(0);
                String suspensionType = firstRecord.get("suspension_type") != null ? firstRecord.get("suspension_type").toString() : "";
                String crsReason = firstRecord.get("crs_reason_of_suspension") != null ? firstRecord.get("crs_reason_of_suspension").toString() : "";
                String eprReason = firstRecord.get("epr_reason_of_suspension") != null ? firstRecord.get("epr_reason_of_suspension").toString() : "";

                boolean isPsPra = SystemConstant.SuspensionType.PERMANENT.equals(suspensionType) && SystemConstant.SuspensionReason.PARTIAL_AMOUNT.equals(crsReason);
                boolean isTsPam = SystemConstant.SuspensionType.TEMPORARY.equals(suspensionType) && SystemConstant.SuspensionReason.PAYMENT_AMOUNT_MISMATCH.equals(eprReason);

                // Assign exception types
                boolean firstAssigned = false;
                for (Map<String, Object> record : noticeRecords) {
                    if (!firstAssigned && (isPsPra || isTsPam)) {
                        // First transaction gets PS-PRA or TS-PAM
                        if (isPsPra) {
                            record.put("exception_type", "PS-PRA");
                        } else {
                            record.put("exception_type", "TS-PAM");
                        }
                        firstAssigned = true;
                    } else {
                        // Subsequent transactions are REFUND
                        record.put("exception_type", "REFUND");
                    }
                }
            }
        }

        return records;
    }

    /**
     * Determine exception type for a single record (no grouping needed)
     */
    private String determineSingleRecordType(Map<String, Object> record) {
        String suspensionType = record.get("suspension_type") != null ? record.get("suspension_type").toString() : "";
        String crsReason = record.get("crs_reason_of_suspension") != null ? record.get("crs_reason_of_suspension").toString() : "";
        String eprReason = record.get("epr_reason_of_suspension") != null ? record.get("epr_reason_of_suspension").toString() : "";

        if (SystemConstant.SuspensionType.PERMANENT.equals(suspensionType) && SystemConstant.SuspensionReason.PARTIAL_AMOUNT.equals(crsReason)) {
            return "PS-PRA";
        } else if (SystemConstant.SuspensionType.TEMPORARY.equals(suspensionType) && SystemConstant.SuspensionReason.PAYMENT_AMOUNT_MISMATCH.equals(eprReason)) {
            return "TS-PAM";
        } else if (record.get("refund_identified_date") != null) {
            return "REFUND";
        }
        return "";
    }
}
