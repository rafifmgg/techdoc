package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for PLUS Manual Stage Change API
 * Based on OCMS × PLUS Interface Spec
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlusChangeStageRequest {

    /**
     * List of notice numbers to apply stage change (batch)
     */
    @NotNull(message = "noticeNo is required")
    @NotEmpty(message = "noticeNo cannot be empty")
    private List<String> noticeNo;

    /**
     * Current/last processing stage name
     */
    @NotNull(message = "lastStageName is required")
    @NotEmpty(message = "lastStageName cannot be empty")
    private String lastStageName;

    /**
     * New/next processing stage name
     */
    @NotNull(message = "nextStageName is required")
    @NotEmpty(message = "nextStageName cannot be empty")
    private String nextStageName;

    /**
     * Last stage date (ISO 8601 format)
     * Example: "2025-09-25T06:58:42"
     */
    private String lastStageDate;

    /**
     * New stage date (ISO 8601 format)
     * Example: "2025-09-30T06:58:42"
     */
    private String newStageDate;

    /**
     * Offence type / Owner-Driver indicator
     * O = Owner, D = Driver, H = Hirer, DIR = Director
     */
    @NotNull(message = "offenceType is required")
    @NotEmpty(message = "offenceType cannot be empty")
    private String offenceType;

    /**
     * Source of the change
     * Example: "004" for PLUS
     */
    @NotNull(message = "source is required")
    @NotEmpty(message = "source cannot be empty")
    private String source;
}
