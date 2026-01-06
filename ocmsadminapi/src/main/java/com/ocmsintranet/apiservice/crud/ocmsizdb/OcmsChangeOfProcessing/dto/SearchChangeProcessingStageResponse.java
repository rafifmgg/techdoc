package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for search notices for Change Processing Stage
 * Based on OCMS CPS Spec §2.3.1
 *
 * Returns two segregated lists:
 * 1. Eligible notices - Can change processing stage
 * 2. Ineligible notices - Cannot change (PS-ed or in Court stage)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchChangeProcessingStageResponse {

    /**
     * Notices eligible for processing stage change
     */
    private List<EligibleNotice> eligibleNotices;

    /**
     * Notices NOT eligible for processing stage change
     * Reasons: Permanent Suspension active OR in Court stage
     */
    private List<IneligibleNotice> ineligibleNotices;

    /**
     * Summary statistics
     */
    private SearchSummary summary;

    /**
     * Eligible notice details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EligibleNotice {
        /**
         * Notice number
         */
        private String noticeNo;

        /**
         * Offence type
         */
        private String offenceType;

        /**
         * Offence date/time
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime offenceDateTime;

        /**
         * Offender name
         */
        private String offenderName;

        /**
         * Offender ID number
         */
        private String offenderId;

        /**
         * Vehicle number
         */
        private String vehicleNo;

        /**
         * Current processing stage
         */
        private String currentProcessingStage;

        /**
         * Current processing stage date
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime currentProcessingStageDate;

        /**
         * Suspension information (if any)
         * Values: "TS" (Temporary), "PS" (Permanent), null (none)
         */
        private String suspensionType;

        /**
         * Suspension status
         * Values: "Active", "Inactive", null
         */
        private String suspensionStatus;

        /**
         * Owner/Driver indicator
         * Values: "D" (Driver), "O" (Owner), "H" (Hirer), "DIR" (Director)
         */
        private String ownerDriverIndicator;

        /**
         * Entity type (for company entities)
         * Values: "BN" (Business Name), "LP" (Limited Partnership), etc.
         */
        private String entityType;
    }

    /**
     * Ineligible notice details with reason
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IneligibleNotice {
        /**
         * Notice number
         */
        private String noticeNo;

        /**
         * Offence type
         */
        private String offenceType;

        /**
         * Offence date/time
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime offenceDateTime;

        /**
         * Offender name
         */
        private String offenderName;

        /**
         * Offender ID number
         */
        private String offenderId;

        /**
         * Vehicle number
         */
        private String vehicleNo;

        /**
         * Current processing stage
         */
        private String currentProcessingStage;

        /**
         * Current processing stage date
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime currentProcessingStageDate;

        /**
         * Reason code for ineligibility
         * Values:
         * - OCMS.CPS.SEARCH.PS_ACTIVE: Permanent Suspension active
         * - OCMS.CPS.SEARCH.COURT_STAGE: Notice in court stage
         */
        private String reasonCode;

        /**
         * Reason message (human-readable)
         */
        private String reasonMessage;
    }

    /**
     * Search summary statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSummary {
        /**
         * Total notices found
         */
        private Integer total;

        /**
         * Number of eligible notices
         */
        private Integer eligible;

        /**
         * Number of ineligible notices
         */
        private Integer ineligible;
    }
}
