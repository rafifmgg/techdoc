package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for change processing stage validation
 * Based on OCMS CPS Spec §2.4.2
 *
 * Returns segregated lists of changeable and non-changeable notices
 * with specific error codes for ineligible notices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateChangeProcessingStageResponse {

    /**
     * Notices that are eligible for stage change
     */
    private List<ChangeableNotice> changeableNotices;

    /**
     * Notices that are NOT eligible for stage change with reasons
     */
    private List<NonChangeableNotice> nonChangeableNotices;

    /**
     * Summary statistics
     */
    private ValidationSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeableNotice {
        /**
         * Notice number
         */
        private String noticeNo;

        /**
         * Current processing stage
         */
        private String currentStage;

        /**
         * Offender type (DRIVER, OWNER, HIRER, DIRECTOR)
         */
        private String offenderType;

        /**
         * Entity type (if applicable)
         */
        private String entityType;

        /**
         * Validation message
         */
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NonChangeableNotice {
        /**
         * Notice number
         */
        private String noticeNo;

        /**
         * Current processing stage (if found)
         */
        private String currentStage;

        /**
         * Error code
         * Possible values:
         * - OCMS.CPS.NOT_FOUND: VON not found
         * - OCMS.CPS.INELIGIBLE_STAGE: Stage not allowed for offender type
         * - OCMS.CPS.NO_STAGE_RULE: No stage mapping rule found
         * - OCMS.CPS.ROLE_CONFLICT: Offender type mismatch
         * - OCMS.CPS.COURT_STAGE: Notice in court stage
         * - OCMS.CPS.PS_BLOCKED: Permanent suspension active
         * - OCMS.CPS.TS_BLOCKED: Temporary suspension active
         * - OCMS.CPS.ENTITY_TYPE_INVALID: Invalid entity type for CFC
         * - OCMS.CPS.REMARKS_REQUIRED: Remarks mandatory when reason=OTH
         */
        private String code;

        /**
         * Error message explaining why notice is not changeable
         */
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationSummary {
        /**
         * Total notices validated
         */
        private Integer total;

        /**
         * Number of changeable notices
         */
        private Integer changeable;

        /**
         * Number of non-changeable notices
         */
        private Integer nonChangeable;
    }
}
