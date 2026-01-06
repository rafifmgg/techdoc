package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Helper class for OcmsRefundNotices entity.
 * Provides utility method for generating refund_notice_id.
 *
 * refund_notice_id format: RE{YYYYMMDD}{NNN} (13 characters)
 * Example: RE20251211001
 * Where:
 * - RE = Prefix for refund (2 chars)
 * - YYYYMMDD = Date the refund was triggered (8 chars)
 * - NNN = Running number from sequence (3 chars, cycles 001-999)
 */
public class OcmsRefundNoticesId {

    private static final String PREFIX = "RE";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate refund_notice_id with new format
     * Format: RE{YYYYMMDD}{NNN} (13 characters)
     * Example: RE20251211001
     *
     * @param runningNumber The running number from sequence (1-999)
     * @return The generated refund_notice_id (e.g., RE20251211001)
     */
    public static String generateRefundNoticeId(int runningNumber) {
        return generateRefundNoticeId(LocalDate.now(), runningNumber);
    }

    /**
     * Generate refund_notice_id with specific date
     * Format: RE{YYYYMMDD}{NNN} (13 characters)
     *
     * @param date The date for the refund
     * @param runningNumber The running number from sequence (1-999)
     * @return The generated refund_notice_id (e.g., RE20251211001)
     */
    public static String generateRefundNoticeId(LocalDate date, int runningNumber) {
        String dateStr = date.format(DATE_FORMATTER);
        String runningStr = String.format("%03d", runningNumber);
        return PREFIX + dateStr + runningStr;
    }
}
