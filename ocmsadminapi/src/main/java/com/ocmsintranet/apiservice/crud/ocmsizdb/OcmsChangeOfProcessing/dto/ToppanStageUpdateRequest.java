package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for Toppan cron to update VON processing stages
 *
 * This is used internally by the Toppan cron job (generate_toppan_letters) to update
 * processing stages after Toppan enquiry files are generated and uploaded.
 *
 * Based on OCMS 15 Spec §2.5.2 - Handling for Manual Stage Change Notice in Toppan Cron
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToppanStageUpdateRequest {

    /**
     * List of notice numbers that were processed by Toppan cron
     * These are notices for which Toppan reminder letters were generated
     */
    @NotEmpty(message = "Notice numbers cannot be empty")
    private List<String> noticeNumbers;

    /**
     * Current processing stage for which Toppan letters were generated
     * Values: RD1, RD2, RR3, DN1, DN2, DR3
     */
    @NotNull(message = "Current stage cannot be null")
    private String currentStage;

    /**
     * Processing date/time when Toppan cron job ran
     * Used to check for manual changes on the same date
     */
    @NotNull(message = "Processing date cannot be null")
    private LocalDateTime processingDate;
}
