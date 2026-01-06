package com.ocmsintranet.apiservice.workflows.furnish.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.workflows.furnish.domain.AutoApprovalCheckType;
import com.ocmsintranet.apiservice.workflows.furnish.domain.OwnerDriverIndicator;
import com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishContext;
import com.ocmsintranet.apiservice.workflows.furnish.submission.dto.FurnishSubmissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Validator for furnish submission requests.
 * Implements business validation rules and auto-approval checks from OCMS 41.
 *
 * Auto-approval checks (OCMS41.5):
 * 1. Notice is not permanently suspended
 * 2. Furnished ID is not already current offender in OCMS
 * 3. Furnished ID does not exist in Hirer or Driver Details
 * 4. Hirer/Driver particulars not already present (any ID)
 * 5. Owner/Hirer (furnisher/submitter) is still current offender
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FurnishValidator {

    private final OcmsValidOffenceNoticeRepository noticeRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * Validate basic business rules (notice exists, not expired, etc.)
     */
    public void validateBasicBusinessRules(FurnishContext context) {
        FurnishSubmissionRequest request = context.getRequest();
        log.debug("Validating basic business rules for notice: {}", request.getNoticeNo());

        // Retrieve and validate notice exists
        Optional<OcmsValidOffenceNotice> noticeOpt = noticeRepository.findById(request.getNoticeNo());
        if (noticeOpt.isEmpty()) {
            throw new IllegalArgumentException("Notice number not found: " + request.getNoticeNo());
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();
        context.setOffenceNotice(notice);

        // Validate vehicle number matches
        if (!notice.getVehicleNo().equalsIgnoreCase(request.getVehicleNo())) {
            throw new IllegalArgumentException("Vehicle number mismatch for notice: " + request.getNoticeNo());
        }

        // Validate owner/driver indicator
        try {
            OwnerDriverIndicator.fromCode(request.getOwnerDriverIndicator());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid owner/driver indicator. Must be 'H' or 'D'");
        }

        log.debug("Basic business rules validation passed for notice: {}", request.getNoticeNo());
    }

    /**
     * Perform all auto-approval checks from OCMS41.5.
     * If any check fails, the submission requires manual review.
     *
     * @param context The furnish context
     */
    public void performAutoApprovalChecks(FurnishContext context) {
        FurnishSubmissionRequest request = context.getRequest();
        log.debug("Performing auto-approval checks for notice: {}", request.getNoticeNo());

        // Check 1: Notice is not permanently suspended
        checkNotPermanentlySuspended(context);

        // Check 2: Furnished ID is not already current offender
        checkFurnishedIdNotCurrentOffender(context);

        // Check 3: Furnished ID not in Hirer or Driver Details
        checkFurnishedIdNotInHirerDriver(context);

        // Check 4: Hirer/Driver particulars not already present (any ID)
        checkHirerDriverParticularsNotPresent(context);

        // Check 5: Owner/Hirer (furnisher) is still current offender
        checkOwnerStillCurrentOffender(context);

        // Set auto-approval result
        boolean passed = !context.hasAutoApprovalFailures();
        context.setAutoApprovalPassed(passed);

        if (passed) {
            log.info("Auto-approval checks PASSED for notice: {}", request.getNoticeNo());
        } else {
            log.info("Auto-approval checks FAILED for notice: {}. Reasons: {}",
                    request.getNoticeNo(), context.getFailureReasonsSummary());
        }
    }

    /**
     * Check 1: Notice is not permanently suspended
     */
    private void checkNotPermanentlySuspended(FurnishContext context) {
        String noticeNo = context.getRequest().getNoticeNo();

        // Check if notice has permanent suspension (PS)
        List<SuspendedNotice> suspensions = suspendedNoticeRepository.findByNoticeNo(noticeNo);
        boolean hasPermanentSuspension = suspensions.stream()
                .anyMatch(s -> "PS".equals(s.getSuspensionType()));

        if (hasPermanentSuspension) {
            context.addAutoApprovalFailure(
                    AutoApprovalCheckType.NOTICE_PERMANENTLY_SUSPENDED,
                    "Notice is permanently suspended (PS)"
            );
            log.debug("Auto-approval check failed: Notice {} is permanently suspended", noticeNo);
        }
    }

    /**
     * Check 2: Furnished ID is not already current offender in OCMS
     */
    private void checkFurnishedIdNotCurrentOffender(FurnishContext context) {
        String furnishIdNo = context.getRequest().getFurnishIdNo();
        String noticeNo = context.getRequest().getNoticeNo();

        // Check if furnished ID is current offender on this notice
        List<OcmsOffenceNoticeOwnerDriver> ownerDriverRecords =
                ownerDriverRepository.findByNoticeNo(noticeNo);

        boolean isFurnishedIdCurrentOffender = ownerDriverRecords.stream()
                .anyMatch(record -> "Y".equals(record.getOffenderIndicator()) &&
                        furnishIdNo.equals(record.getIdNo()));

        if (isFurnishedIdCurrentOffender) {
            context.addAutoApprovalFailure(
                    AutoApprovalCheckType.FURNISHED_ID_CURRENT_OFFENDER,
                    "Furnished ID is already current offender in OCMS"
            );
            log.debug("Auto-approval check failed: Furnished ID {} is current offender for notice {}",
                    furnishIdNo, noticeNo);
        }
    }

    /**
     * Check 3: Furnished ID does not exist in Hirer or Driver Details
     * (regardless of current offender status)
     */
    private void checkFurnishedIdNotInHirerDriver(FurnishContext context) {
        String furnishIdNo = context.getRequest().getFurnishIdNo();
        String noticeNo = context.getRequest().getNoticeNo();

        // Check if furnished ID exists in ANY owner/driver record for this notice
        List<OcmsOffenceNoticeOwnerDriver> ownerDriverRecords =
                ownerDriverRepository.findByNoticeNo(noticeNo);

        boolean furnishedIdExists = ownerDriverRecords.stream()
                .anyMatch(record -> furnishIdNo.equals(record.getIdNo()));

        if (furnishedIdExists) {
            context.addAutoApprovalFailure(
                    AutoApprovalCheckType.FURNISHED_ID_IN_HIRER_DRIVER,
                    "Furnished ID already exists in Hirer or Driver Details"
            );
            log.debug("Auto-approval check failed: Furnished ID {} exists in hirer/driver for notice {}",
                    furnishIdNo, noticeNo);
        }
    }

    /**
     * Check 4: Hirer/Driver particulars not already present (any ID)
     * This checks if there's already a hirer/driver record matching the type
     */
    private void checkHirerDriverParticularsNotPresent(FurnishContext context) {
        String noticeNo = context.getRequest().getNoticeNo();
        String ownerDriverIndicator = context.getRequest().getOwnerDriverIndicator();

        // Check if hirer/driver record already exists for this type
        List<OcmsOffenceNoticeOwnerDriver> ownerDriverRecords =
                ownerDriverRepository.findByNoticeNo(noticeNo);

        boolean particularsExist = ownerDriverRecords.stream()
                .anyMatch(record -> ownerDriverIndicator.equals(record.getOwnerDriverIndicator()));

        if (particularsExist) {
            context.addAutoApprovalFailure(
                    AutoApprovalCheckType.HIRER_DRIVER_PARTICULARS_EXISTS,
                    "Hirer/Driver particulars already present for this notice"
            );
            log.debug("Auto-approval check failed: {}/{} particulars already exist for notice {}",
                    ownerDriverIndicator.equals("H") ? "Hirer" : "Driver",
                    ownerDriverIndicator, noticeNo);
        }
    }

    /**
     * Check 5: Owner/Hirer (furnisher/submitter) is still current offender
     */
    private void checkOwnerStillCurrentOffender(FurnishContext context) {
        String ownerIdNo = context.getRequest().getOwnerIdNo();
        String noticeNo = context.getRequest().getNoticeNo();

        // Check if owner/submitter is current offender
        List<OcmsOffenceNoticeOwnerDriver> ownerDriverRecords =
                ownerDriverRepository.findByNoticeNo(noticeNo);

        boolean ownerIsCurrentOffender = ownerDriverRecords.stream()
                .anyMatch(record -> "Y".equals(record.getOffenderIndicator()) &&
                        ownerIdNo.equals(record.getIdNo()));

        if (!ownerIsCurrentOffender) {
            context.addAutoApprovalFailure(
                    AutoApprovalCheckType.OWNER_NOT_CURRENT_OFFENDER,
                    "Owner/Hirer (furnisher) is no longer current offender"
            );
            log.debug("Auto-approval check failed: Owner {} is not current offender for notice {}",
                    ownerIdNo, noticeNo);
        }
    }

    /**
     * Check if this is a resubmission (previous furnish application exists)
     */
    public boolean isResubmission(String noticeNo) {
        // This would check OcmsFurnishApplication table for existing records
        // Implementation depends on repository being available
        // For now, return false - will be implemented in persistence service
        return false;
    }
}
