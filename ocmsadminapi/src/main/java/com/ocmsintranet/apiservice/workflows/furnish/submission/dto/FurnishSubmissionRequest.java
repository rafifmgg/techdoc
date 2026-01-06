package com.ocmsintranet.apiservice.workflows.furnish.submission.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for furnish hirer/driver submission request from eService.
 * Contains JSR-380 bean validation annotations for basic format validation.
 * Business validation is performed separately in FurnishValidator.
 *
 * Based on OCMS 41 User Story 41.4 and data dictionary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishSubmissionRequest {

    // Transaction and notice details
    @NotBlank(message = "Transaction number is required")
    private String txnNo;

    @NotBlank(message = "Notice number is required")
    private String noticeNo;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNo;

    @NotNull(message = "Offence date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime offenceDate;

    @NotBlank(message = "Car park code is required")
    private String ppCode;

    @NotBlank(message = "Car park name is required")
    private String ppName;

    // Furnished person details
    @NotBlank(message = "Furnished person name is required")
    private String furnishName;

    @NotBlank(message = "Furnished person ID type is required")
    private String furnishIdType;

    @NotBlank(message = "Furnished person ID number is required")
    private String furnishIdNo;

    // Furnished person address
    @NotBlank(message = "Block/house number is required")
    private String furnishMailBlkNo;

    private String furnishMailFloor;

    @NotBlank(message = "Street name is required")
    private String furnishMailStreetName;

    private String furnishMailUnitNo;

    private String furnishMailBldgName;

    @NotBlank(message = "Postal code is required")
    private String furnishMailPostalCode;

    // Furnished person contact
    private String furnishTelCode;

    private String furnishTelNo;

    private String furnishEmailAddr;

    // Furnish details
    @NotBlank(message = "Owner/driver indicator is required (H or D)")
    private String ownerDriverIndicator;

    @NotBlank(message = "Hirer/owner relationship is required")
    private String hirerOwnerRelationship;

    private String othersRelationshipDesc;

    // Questionnaire answers
    @NotBlank(message = "Question 1 answer is required")
    private String quesOneAns;

    @NotBlank(message = "Question 2 answer is required")
    private String quesTwoAns;

    private String quesThreeAns;

    // Rental period (if applicable)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rentalPeriodFrom;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rentalPeriodTo;

    // Submitter (owner/furnisher) details
    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Owner ID number is required")
    private String ownerIdNo;

    private String ownerTelCode;

    private String ownerTelNo;

    private String ownerEmailAddr;

    // CorpPass business rep (if company submission)
    private String corppassStaffName;

    // Document references (for uploaded files)
    private List<String> documentReferences;
}
