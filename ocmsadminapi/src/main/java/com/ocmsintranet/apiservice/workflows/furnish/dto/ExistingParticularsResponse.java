package com.ocmsintranet.apiservice.workflows.furnish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for existing particulars check response (OCMS41.44-41.45).
 * Checks if owner/hirer/driver particulars already exist for a notice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistingParticularsResponse {

    private String noticeNo;
    private boolean hasExistingParticulars;
    private List<ExistingParticularDetail> existingParticulars;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExistingParticularDetail {
        private String ownerDriverIndicator; // H or D
        private String idType;
        private String idNo;
        private String name;
        private String currentOffenderIndicator; // Y or N

        // Address details
        private String blkNo;
        private String floor;
        private String streetName;
        private String unitNo;
        private String bldgName;
        private String postalCode;

        // Contact details
        private String telCode;
        private String telNo;
        private String emailAddr;
    }
}
