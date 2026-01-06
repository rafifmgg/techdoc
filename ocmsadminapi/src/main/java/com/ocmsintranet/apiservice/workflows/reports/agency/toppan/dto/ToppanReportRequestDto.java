package com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToppanReportRequestDto {

    /**
     * Source type: CRS or ACKNOWLEDGEMENT
     */
    @NotNull(message = "Source is required")
    @Pattern(regexp = "^(CRS|ACKNOWLEDGEMENT)$", message = "Source must be either CRS or ACKNOWLEDGEMENT")
    private String source;

    /**
     * Report date in YYYY-MM-DD format
     */
    @NotNull(message = "Report date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Report date must be in YYYY-MM-DD format")
    private String reportDate;
}
