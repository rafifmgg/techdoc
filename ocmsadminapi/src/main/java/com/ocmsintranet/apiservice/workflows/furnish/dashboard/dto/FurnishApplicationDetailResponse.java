package com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for detailed view of a furnish application.
 * Based on OCMS 41 User Story 41.14.
 *
 * Details to display:
 * - Notice No., Vehicle No., Place of Offence, Offence Date & Time
 * - Current Processing Stage, Status, Submission Date
 * - Owner details (name, ID, contact, email)
 * - Furnished person details (name, ID type, ID no, address, contact, email)
 * - Hirer/Owner relationship
 * - Questionnaire answers
 * - Rental period (if applicable)
 * - Document attachments
 * - Reason for review (if manual review)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishApplicationDetailResponse {

    // Notice details
    private String noticeNo;
    private String vehicleNo;
    private String ppCode;
    private String ppName;
    private LocalDateTime offenceDate;

    /**
     * The current processing stage of the notice.
     *
     * <p><strong>Field Naming Explanation:</strong></p>
     * <ul>
     *   <li><strong>Database:</strong> Stored as {@code last_processing_stage} in OCMS legacy schema</li>
     *   <li><strong>Entity:</strong> Accessed via {@code OcmsValidOffenceNotice.getCurrentProcessingStage()}</li>
     *   <li><strong>API/DTO:</strong> Exposed as {@code currentProcessingStage} for clarity</li>
     * </ul>
     *
     * <p>Despite the database field being named "last_processing_stage", this represents
     * the CURRENT state of the notice from a business perspective. The API uses the more
     * intuitive name {@code currentProcessingStage} to avoid confusion.</p>
     *
     * @see com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice#getCurrentProcessingStage()
     */
    private String currentProcessingStage;

    // Submission details
    private String txnNo;
    private String status;
    private LocalDateTime submissionDate;
    private String reasonForReview;
    private Integer workingDaysPending;

    // Owner (submitter) details
    private String ownerName;
    private String ownerIdNo;
    private String ownerTelCode;
    private String ownerTelNo;
    private String ownerEmailAddr;
    private String corppassStaffName;

    // Furnished person details
    private String furnishName;
    private String furnishIdType;
    private String furnishIdNo;
    private String furnishTelCode;
    private String furnishTelNo;
    private String furnishEmailAddr;

    // Furnished person address
    private String furnishMailBlkNo;
    private String furnishMailFloor;
    private String furnishMailStreetName;
    private String furnishMailUnitNo;
    private String furnishMailBldgName;
    private String furnishMailPostalCode;

    // Furnish details
    private String ownerDriverIndicator; // H or D
    private String hirerOwnerRelationship; // F, E, L, S, O
    private String othersRelationshipDesc;

    // Questionnaire
    private String quesOneAns;
    private String quesTwoAns;
    private String quesThreeAns;

    // Rental period
    private LocalDateTime rentalPeriodFrom;
    private LocalDateTime rentalPeriodTo;

    // Document attachments
    private List<DocumentInfo> documents;

    // Audit fields
    private LocalDateTime creDate;
    private String creUserId;
    private LocalDateTime updDate;
    private String updUserId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private String txnNo;
        private Integer attachmentId;
        private String docName; // Azure Blob path
    }

    /**
     * Create from entity
     */
    public static FurnishApplicationDetailResponse fromEntity(
            OcmsFurnishApplication application,
            String currentProcessingStage,
            Integer workingDaysPending,
            List<DocumentInfo> documents) {

        return FurnishApplicationDetailResponse.builder()
                .noticeNo(application.getNoticeNo())
                .vehicleNo(application.getVehicleNo())
                .ppCode(application.getPpCode())
                .ppName(application.getPpName())
                .offenceDate(application.getOffenceDate())
                .currentProcessingStage(currentProcessingStage)
                .txnNo(application.getTxnNo())
                .status(application.getStatus())
                .submissionDate(application.getCreDate())
                .reasonForReview(application.getReasonForReview())
                .workingDaysPending(workingDaysPending)
                .ownerName(application.getOwnerName())
                .ownerIdNo(application.getOwnerIdNo())
                .ownerTelCode(application.getOwnerTelCode())
                .ownerTelNo(application.getOwnerTelNo())
                .ownerEmailAddr(application.getOwnerEmailAddr())
                .corppassStaffName(application.getCorppassStaffName())
                .furnishName(application.getFurnishName())
                .furnishIdType(application.getFurnishIdType())
                .furnishIdNo(application.getFurnishIdNo())
                .furnishTelCode(application.getFurnishTelCode())
                .furnishTelNo(application.getFurnishTelNo())
                .furnishEmailAddr(application.getFurnishEmailAddr())
                .furnishMailBlkNo(application.getFurnishMailBlkNo())
                .furnishMailFloor(application.getFurnishMailFloor())
                .furnishMailStreetName(application.getFurnishMailStreetName())
                .furnishMailUnitNo(application.getFurnishMailUnitNo())
                .furnishMailBldgName(application.getFurnishMailBldgName())
                .furnishMailPostalCode(application.getFurnishMailPostalCode())
                .ownerDriverIndicator(application.getOwnerDriverIndicator())
                .hirerOwnerRelationship(application.getHirerOwnerRelationship())
                .othersRelationshipDesc(application.getOthersRelationshipDesc())
                .quesOneAns(application.getQuesOneAns())
                .quesTwoAns(application.getQuesTwoAns())
                .quesThreeAns(application.getQuesThreeAns())
                .rentalPeriodFrom(application.getRentalPeriodFrom())
                .rentalPeriodTo(application.getRentalPeriodTo())
                .documents(documents)
                .creDate(application.getCreDate())
                .creUserId(application.getCreUserId())
                .updDate(application.getUpdDate())
                .updUserId(application.getUpdUserId())
                .build();
    }
}
