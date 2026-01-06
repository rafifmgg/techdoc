package com.ocmsintranet.apiservice.workflows.reports.agency.ps.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OCMS 18 - PS Report Response DTO
 *
 * Response DTO for Permanent Suspension Reports
 * Section 6: Permanent Suspension Report
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PsReportResponseDto {

    private List<PsRecord> data;
    private int total;
    private int skip;
    private int limit;

    /**
     * PS Record DTO - Contains all fields from Section 6.3.1 and 6.3.2
     *
     * Fields are included from both "PS by System" and "PS by Officer" reports
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PsRecord {

        // From ocms_valid_offence_notice
        private String vehicleNo;
        private String offenceNoticeType;
        private String vehicleRegistrationType;
        private String vehicleCategory;
        private String computerRuleCode;
        private LocalDateTime noticeDateAndTime; // Date & time of offence
        private String ppCode; // Place of offence

        // From ocms_suspended_notice
        private String noticeNo;
        private LocalDateTime dateOfSuspension; // Sorted in ascending order
        private String reasonOfSuspension; // Current suspension reason
        private String suspensionSource; // OCMS, PLUS, CERTIS, EEPS, REPCCS
        private String suspensionRemarks; // Only for PS by Officer report

        // Officer fields - for PS by Officer report
        private String officerAuthorisingSupension; // Authorising Officer
        private String creUserId; // PSing Officer

        // Refund tracking - from new refund table
        private LocalDateTime refundIdentifiedDate;

        // Previous PS suspension details
        private String previousPsReason; // Previous PS suspension reason
        private LocalDateTime previousPsDate; // Previous PS suspension date
    }
}
