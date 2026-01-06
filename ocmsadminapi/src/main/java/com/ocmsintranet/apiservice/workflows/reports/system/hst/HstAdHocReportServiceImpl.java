package com.ocmsintranet.apiservice.workflows.reports.system.hst;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst.OcmsHstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for HST ad-hoc report generation
 * OCMS 20: Section 3.6.3 & 3.6.4 - Ad-hoc Reports
 *
 * Reports return JSON data - Excel generation is handled by Staff Portal (client-side)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HstAdHocReportServiceImpl implements HstAdHocReportService {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsHstRepository hstRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Generate Hirer/Driver Furnished Report
     * Shows HST records where address was furnished by Hirer (H) or Driver (D)
     *
     * @param startDate Optional start date filter (yyyy-MM-dd)
     * @param endDate Optional end date filter (yyyy-MM-dd)
     * @return Map with records and recordCount
     */
    @Override
    public Map<String, Object> generateHirerDriverFurnishedReport(String startDate, String endDate) {
        log.info("Generating HST Hirer/Driver Furnished Report. startDate={}, endDate={}", startDate, endDate);

        try {
            // Parse date parameters (optional)
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);

            // Query furnish applications for Hirer/Driver
            List<Map<String, Object>> records = furnishApplicationRepository
                    .findHirerDriverFurnishedByDateRange(start, end);

            log.info("Found {} Hirer/Driver furnished records", records.size());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("records", records);
            response.put("recordCount", records.size());
            response.put("reportType", "HIRER_DRIVER_FURNISHED");
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("generatedAt", LocalDate.now().format(DATE_FORMATTER));

            return response;

        } catch (Exception e) {
            log.error("Error generating Hirer/Driver furnished report", e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Alternative Address Furnished Report
     * Shows HST records where address was furnished by eService (ES) or LTA
     *
     * @param startDate Optional start date filter (yyyy-MM-dd)
     * @param endDate Optional end date filter (yyyy-MM-dd)
     * @return Map with records and recordCount
     */
    @Override
    public Map<String, Object> generateAlternativeAddressFurnishedReport(String startDate, String endDate) {
        log.info("Generating Alternative Address Furnished Report. startDate={}, endDate={}", startDate, endDate);

        try {
            // Parse date parameters (optional)
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);

            // Query furnish applications for eService/LTA sources
            List<Map<String, Object>> records = furnishApplicationRepository
                    .findAlternativeAddressByDateRange(start, end);

            log.info("Found {} alternative address furnished records", records.size());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("records", records);
            response.put("recordCount", records.size());
            response.put("reportType", "ALTERNATIVE_ADDRESS_FURNISHED");
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("generatedAt", LocalDate.now().format(DATE_FORMATTER));

            return response;

        } catch (Exception e) {
            log.error("Error generating alternative address furnished report", e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    /**
     * Parse date string to LocalDate (returns null if invalid or empty)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Invalid date format: {}. Using null.", dateStr);
            return null;
        }
    }
}
