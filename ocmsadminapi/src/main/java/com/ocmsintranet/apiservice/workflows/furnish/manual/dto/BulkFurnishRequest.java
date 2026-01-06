package com.ocmsintranet.apiservice.workflows.furnish.manual.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for officer bulk furnish request (multiple notices).
 * Based on OCMS 41 User Stories 41.50-41.51.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkFurnishRequest {

    @NotEmpty(message = "Notice numbers list cannot be empty")
    private List<String> noticeNos;

    @NotBlank(message = "Officer ID is required")
    private String officerId;

    @NotBlank(message = "Owner/driver indicator is required (H or D)")
    private String ownerDriverIndicator; // H=Hirer, D=Driver

    // Furnished person details (same for all notices)
    @NotBlank(message = "ID type is required")
    private String idType;

    @NotBlank(message = "ID number is required")
    private String idNo;

    @NotBlank(message = "Name is required")
    private String name;

    // Address details (same for all notices)
    @NotBlank(message = "Block/house number is required")
    private String blkNo;

    private String floor;

    @NotBlank(message = "Street name is required")
    private String streetName;

    private String unitNo;

    private String bldgName;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    // Contact details (same for all notices)
    private String telCode;
    private String telNo;
    private String emailAddr;

    // Overwrite existing flag
    @NotNull(message = "Overwrite existing flag is required")
    private Boolean overwriteExisting;

    // Remarks (optional)
    private String remarks;
}
