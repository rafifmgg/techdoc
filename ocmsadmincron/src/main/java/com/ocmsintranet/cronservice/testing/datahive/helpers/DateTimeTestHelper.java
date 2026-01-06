package com.ocmsintranet.cronservice.testing.datahive.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Helper utility for date time operations in DataHive test services.
 *
 * Provides centralized date time conversion and comparison methods
 * to eliminate code duplication across test services.
 */
@Component
@Slf4j
public class DateTimeTestHelper {

    /**
     * Compare two LocalDateTime values dengan tolerance untuk minor differences.
     */
    public boolean isTimestampEqual(LocalDateTime time1, LocalDateTime time2) {
        if (time1 == null || time2 == null) {
            return time1 == time2;
        }
        return Math.abs(time1.toEpochSecond(ZoneOffset.UTC) - time2.toEpochSecond(ZoneOffset.UTC)) <= 1;
    }

    /**
     * Convert database Timestamp/LocalDateTime object to LocalDateTime.
     */
    public LocalDateTime convertTimestampToLocalDateTime(Object dbTimestamp) {
        if (dbTimestamp == null) {
            return null;
        }
        if (dbTimestamp instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) dbTimestamp).toLocalDateTime();
        } else if (dbTimestamp instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) dbTimestamp;
        } else if (dbTimestamp instanceof String) {
            return parseDate((String) dbTimestamp);
        } else {
            log.warn("Unknown timestamp type: {}", dbTimestamp.getClass().getName());
            return null;
        }
    }

    /**
     * Convert epoch timestamp dengan nanoseconds to LocalDateTime.
     */
    public LocalDateTime convertEpochToLocalDateTime(String epochWithNanos) {
        if (epochWithNanos == null || epochWithNanos.trim().isEmpty()) {
            return null;
        }
        try {
            String epochSeconds = epochWithNanos.split("\\.")[0];
            return LocalDateTime.ofEpochSecond(Long.parseLong(epochSeconds), 0, ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to convert epoch timestamp: {}", epochWithNanos, e);
            return null;
        }
    }

    /**
     * Parse date field yang bisa formatted string atau epoch.
     */
    public LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            // Try parsing as ISO datetime string
            if (dateString.contains("-") && dateString.contains(":")) {
                return LocalDateTime.parse(dateString.replace(" ", "T").substring(0, 19));
            } else {
                // Try parsing as epoch
                return convertEpochToLocalDateTime(dateString);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateString, e);
            return null;
        }
    }

    /**
     * Parse admission date dengan special handling untuk berbagai format.
     */
    public LocalDateTime parseAdmissionDate(String admDateString) {
        if (admDateString == null || admDateString.trim().isEmpty()) {
            return null;
        }
        try {
            // Handle ISO format dengan timestamp
            if (admDateString.contains("-") && admDateString.contains(":")) {
                return LocalDateTime.parse(admDateString.replace(" ", "T").substring(0, 19));
            } else {
                // Handle epoch format
                return convertEpochToLocalDateTime(admDateString);
            }
        } catch (Exception e) {
            log.warn("Failed to parse ADM_DT: {}", admDateString, e);
            return null;
        }
    }

    /**
     * Get current timestamp untuk test operations.
     */
    public LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Get timestamp untuk past date (untuk test scenarios).
     */
    public LocalDateTime getPastTimestamp(int daysAgo) {
        return LocalDateTime.now().minusDays(daysAgo);
    }

    /**
     * Get timestamp untuk future date (untuk test scenarios).
     */
    public LocalDateTime getFutureTimestamp(int daysFromNow) {
        return LocalDateTime.now().plusDays(daysFromNow);
    }

    /**
     * Create test timestamp dengan specific date parameters.
     */
    public LocalDateTime createTestTimestamp(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    /**
     * Format LocalDateTime untuk logging purposes.
     */
    public String formatForLogging(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "null";
        }
        return dateTime.toString();
    }
}