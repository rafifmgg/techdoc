package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for validating change processing stage eligibility
 * Based on OCMS CPS Spec §2.4.2
 *
 * This endpoint validates notices BEFORE actual submission to identify
 * which notices are changeable vs non-changeable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateChangeProcessingStageRequest {

    /**
     * List of notices to validate
     */
    @NotNull(message = "notices is required")
    @NotEmpty(message = "notices cannot be empty")
    @Valid
    private List<NoticeToValidate> notices;

    /**
     * New processing stage to validate against
     */
    @NotNull(message = "newProcessingStage is required")
    private String newProcessingStage;

    /**
     * Reason for change (optional for validation)
     */
    private String reasonOfChange;

    /**
     * Remarks (optional for validation)
     */
    private String remarks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoticeToValidate {

        /**
         * Notice number
         */
        @NotNull(message = "noticeNo is required")
        private String noticeNo;

        /**
         * Current processing stage (optional - will be fetched from DB if not provided)
         */
        private String currentStage;

        /**
         * Offender type (optional - will be determined from ONOD if not provided)
         * Values: DRIVER, OWNER, HIRER, DIRECTOR
         */
        private String offenderType;

        /**
         * Entity type (optional - required for CFC validation)
         * Values: BN (Business Name), LP (Limited Partnership), etc.
         */
        private String entityType;
    }
}
