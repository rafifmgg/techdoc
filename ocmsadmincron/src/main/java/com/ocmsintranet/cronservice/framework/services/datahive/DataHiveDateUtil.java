package com.ocmsintranet.cronservice.framework.services.datahive;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility class for DataHive date conversions.
 * Provides centralized date conversion logic for all DataHive services.
 */
@Slf4j
public class DataHiveDateUtil {

    /**
     * Converts DataHive date string to LocalDateTime.
     * Supports epoch timestamps, date-only strings, and datetime strings.
     *
     * @param dateString the date string from DataHive
     * @return LocalDateTime representation, or null if conversion fails
     */
    public static LocalDateTime convertEpochToLocalDateTime(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            // DataHive returns three types of date formats:
            // 1. Epoch timestamps like "1764892800.000000000"
            // 2. Regular timestamps like "2020-05-12 07:20:00.000"
            // 3. Date-only strings like "2017-07-13"

            if (dateString.contains("-")) {
                if (dateString.contains(":")) {
                    // Regular timestamp format - parse directly
                    // Remove milliseconds if present: "2020-05-12 07:20:00.000" -> "2020-05-12 07:20:00"
                    String cleanDateString = dateString.split("\\.")[0];
                    return LocalDateTime.parse(cleanDateString.replace(" ", "T"));
                } else {
                    // Date-only format (YYYY-MM-DD) - parse as date and convert to LocalDateTime at start of day
                    LocalDate date = LocalDate.parse(dateString);
                    return date.atStartOfDay();
                }
            } else {
                // Epoch timestamp format - convert from epoch seconds
                long epochSeconds = Long.parseLong(dateString.split("\\.")[0]);
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
            }
        } catch (Exception e) {
            log.warn("Failed to convert timestamp '{}' to LocalDateTime: {}", dateString, e.getMessage());
            return null;
        }
    }

    /**
     * Converts DataHive epoch days to LocalDateTime.
     * Used for REFERENCE_PERIOD fields that store days since January 1, 1970.
     *
     * @param daysString the epoch days string from DataHive (e.g., "19205")
     * @return LocalDateTime representation, or January 1, 1970 00:00:00 if null/invalid
     */
    public static LocalDateTime convertEpochDaysToLocalDateTime(String daysString) {
        if (daysString == null || daysString.isEmpty()) {
            // Return epoch start date (January 1, 1970 00:00:00) as default
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault());
        }

        try {
            // Parse the number of days since epoch (January 1, 1970)
            long epochDays = Long.parseLong(daysString.split("\\.")[0]);

            // Convert days to seconds (86400 seconds per day)
            long epochSeconds = epochDays * 86400;

            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
        } catch (Exception e) {
            log.warn("Failed to convert epoch days '{}' to LocalDateTime, using default 1970-01-01", daysString);
            // Return epoch start date (January 1, 1970 00:00:00) as default
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault());
        }
    }
}