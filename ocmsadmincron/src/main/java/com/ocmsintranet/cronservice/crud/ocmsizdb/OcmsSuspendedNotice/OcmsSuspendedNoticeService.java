package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;
import java.util.Map;

public interface OcmsSuspendedNoticeService extends BaseService<OcmsSuspendedNotice, OcmsSuspendedNoticeId> {
    
    /**
     * Fetch successful records for the Success sheet.
     * Focuses on valid_offence_notice data with valid ID information.
     * 
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for successful records
     */
    List<Map<String, Object>> getSuccessRecordsForReport(String reportDate);
    
    /**
     * Fetch error records for the Error sheet.
     * Focuses on suspended_notice data with missing or invalid information.
     * 
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for error records
     */
    List<Map<String, Object>> getErrorRecordsForReport(String reportDate);
    
    /**
     * Fetch all suspended notices with suspension_type in ('TS', 'PS') and reason_of_suspension in ('NRO', 'RP2').
     *
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for TS/PS notices with NRO/RP2 reason
     */
    List<Map<String, Object>> getSuspendedNoticesForReport(String reportDate);

    /**
     * PERFORMANCE OPTIMIZATION: Get maximum sr_no for a given notice_no
     * Uses optimized MAX query instead of retrieving all records
     *
     * @param noticeNo The notice number to query
     * @return Maximum sr_no for the notice, or null if no records exist
     */
    Integer findMaxSrNoByNoticeNo(String noticeNo);

    /**
     * Find suspended notices by notice number and suspension type
     * Used by auto-approval to check for active PS (Permanent Suspension)
     *
     * @param noticeNo Notice number
     * @param suspensionType Suspension type (PS, TS, etc.)
     * @return List of suspended notice records
     */
    List<OcmsSuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType);
}
