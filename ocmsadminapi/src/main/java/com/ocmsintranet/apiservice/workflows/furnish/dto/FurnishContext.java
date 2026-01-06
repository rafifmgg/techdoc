package com.ocmsintranet.apiservice.workflows.furnish.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.workflows.furnish.domain.AutoApprovalCheckType;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal context object that passes data through the furnish submission workflow.
 * Based on OCMS 41 requirements and reduction workflow pattern.
 *
 * This is used internally by the service implementation to carry state
 * through the various validation and processing steps.
 */
@Data
@Builder
public class FurnishContext {

    // Input request
    private FurnishSubmissionRequest request;

    // Retrieved entities
    private OcmsValidOffenceNotice offenceNotice;
    private OcmsOffenceNoticeOwnerDriver existingOwnerDriver;

    // Created/updated entities
    private OcmsFurnishApplication furnishApplication;
    private OcmsOffenceNoticeOwnerDriver newOwnerDriver;

    // Auto-approval validation results
    @Builder.Default
    private List<String> autoApprovalFailures = new ArrayList<>();

    @Builder.Default
    private List<AutoApprovalCheckType> failedChecks = new ArrayList<>();

    private boolean autoApprovalPassed;

    // Processing flags
    private boolean isResubmission;
    private boolean ownerDriverRecordCreated;
    private boolean suspensionApplied;

    // Metadata
    private String currentUserId;
    private String reasonForManualReview;

    /**
     * Add an auto-approval failure with check type
     */
    public void addAutoApprovalFailure(AutoApprovalCheckType checkType, String message) {
        this.failedChecks.add(checkType);
        this.autoApprovalFailures.add(String.format("[%s] %s", checkType.name(), message));
    }

    /**
     * Check if any auto-approval validations failed
     */
    public boolean hasAutoApprovalFailures() {
        return !autoApprovalFailures.isEmpty();
    }

    /**
     * Get concatenated failure reasons for manual review
     */
    public String getFailureReasonsSummary() {
        return String.join("; ", autoApprovalFailures);
    }
}
