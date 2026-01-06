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
 * Request DTO for Change Processing Stage batch operation
 * Based on OCMS CPS Spec §5.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOfProcessingRequest {

    @NotNull(message = "items is required")
    @NotEmpty(message = "items cannot be empty")
    @Valid
    private List<ChangeOfProcessingItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeOfProcessingItem {

        @NotNull(message = "noticeNo is required")
        private String noticeNo;

        /**
         * Requested new processing stage (optional, can be derived from StageMap)
         */
        private String newStage;

        /**
         * Reason for the change
         */
        private String reason;

        /**
         * Additional remarks
         */
        private String remark;

        /**
         * Source of the change (e.g., PORTAL, PLUS, SYSTEM)
         * Optional - defaults to PORTAL if not provided
         */
        private String source;

        /**
         * Flag to update DH MHA check
         */
        @Builder.Default
        private Boolean dhMhaCheck = false;

        /**
         * Confirmation flag for existing record override
         * Based on OCMS CPS Spec §2.5.1 Step 4
         *
         * If duplicate record exists for same notice on same date:
         * - isConfirmation=false: Return warning to user
         * - isConfirmation=true: Proceed with update (user confirmed)
         */
        @Builder.Default
        private Boolean isConfirmation = false;
    }
}
