package com.ocmsintranet.apiservice.workflows.reports.agency.ans.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for Ad-Hoc ANS Report (OCMS 10 Section 9.2)
 *
 * Search criteria for querying Advisory Notices
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnsReportRequestDto {

    /**
     * Date of eAN sent (YYYY-MM-DD format)
     * Cannot be future date
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of eAN sent must be in YYYY-MM-DD format")
    private String dateOfEansSent;

    /**
     * Date of ANS letter (YYYY-MM-DD format)
     * Cannot be future date
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of ANS letter must be in YYYY-MM-DD format")
    private String dateOfAnsLetter;

    /**
     * Date of offence (YYYY-MM-DD format)
     * Cannot be future date
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of offence must be in YYYY-MM-DD format")
    private String dateOfOffence;

    /**
     * Vehicle registration type (e.g., S, D, V, I)
     * Optional - from Standard Code DB
     */
    private String vehicleRegistrationType;

    /**
     * Place of offence - PP Code
     * Optional - Car Park Code
     */
    private String ppCode;

    /**
     * Place of offence - Car Park Name
     * Optional - Car Park Name
     */
    private String carParkName;
}
