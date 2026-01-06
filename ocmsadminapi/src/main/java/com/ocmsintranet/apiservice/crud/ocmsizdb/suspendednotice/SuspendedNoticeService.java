package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import com.ocmsintranet.apiservice.crud.BaseService;
import java.util.List;
import java.util.Map;

public interface SuspendedNoticeService extends BaseService<SuspendedNotice, SuspendedNoticeId> {

    /**
     * Fetch successful records for the Success sheet.
     * Focuses on valid_offence_notice data with valid ID information.
     *
     * @param fromDate The start date to filter by (format: yyyy-MM-dd)
     * @param toDate The end date to filter by (format: yyyy-MM-dd)
     * @param orderByFields Map of field names to sort direction (1 for ASC, -1 for DESC).
     *                      Valid fields: notice_no, blk_hse_no, street_name, floor_no, unit_no, bldg_name, postal_code
     * @return List of maps containing the joined data for successful records
     */
    List<Map<String, Object>> getRecordsForReport(String fromDate, String toDate, Map<String, Integer> orderByFields);

    /**
     * Fetch MHA control metrics from ocms_batch_job log entries for a given date range.
     * The metrics are parsed from the `log_text` field for the job named 'process_mha_vrls_files'.
     * Expected keys to populate in the map: recordsSubmitted, recordsReturned, recordsRead,
     * recordsMatched, invalidUinFinCount, validUnmatchedCount.
     *
     * @param startDate yyyy-MM-dd
     * @param endDate yyyy-MM-dd
     * @return map of metrics (non-null)
     */
    Map<String, Integer> getMhaControlMetrics(String startDate, String endDate);

    /**
     * Get the maximum SR number for a given notice number.
     * OCMS 21: Used for creating PS-DBB and other suspensions.
     *
     * @param noticeNo The notice number
     * @return Maximum SR number, or null if no suspensions exist for this notice
     */
    Integer getMaxSrNoForNotice(String noticeNo);

    /**
     * Fetch PS report records (PS by System and PS by Officer).
     * OCMS 18 Section 6: Permanent Suspension Report
     *
     * @param fromDate The start date to filter by (format: yyyy-MM-dd)
     * @param toDate The end date to filter by (format: yyyy-MM-dd)
     * @param suspensionSource Filter by suspension_source (OCMS, PLUS, etc.) or null for ALL
     * @param officerAuthorisingSupension Filter by officer (PS by Officer report) or null for all
     * @param creUserId Filter by PSing officer or null for all
     * @param orderByFields Map of field names to sort direction (1 for ASC, -1 for DESC)
     * @return List of maps containing PS report data
     */
    List<Map<String, Object>> getPsReportRecords(String fromDate, String toDate, String suspensionSource,
                                                  String officerAuthorisingSupension, String creUserId,
                                                  Map<String, Integer> orderByFields);

    /**
     * Find all suspended notices for a given notice number and suspension type.
     * Used in OCMS 41 to check for active PS (Permanent Suspension).
     *
     * @param noticeNo Notice number to search for
     * @param suspensionType Suspension type (e.g., "PS", "TS")
     * @return List of suspended notices matching the criteria
     */
    List<SuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType);

}
