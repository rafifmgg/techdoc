package com.ocmsintranet.apiservice.workflows.furnish.stafffurnish.services;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OCMS 41: Staff Furnish Service
 *
 * Provides backend APIs for OCMS Staff Portal to manually furnish hirer/driver particulars.
 * This allows OIC to furnish offender details on behalf of the public (alternative to eService).
 *
 * APIs:
 * 1. Furnish-Redirect Check (POST) - Validates if notice can be furnished/redirected
 * 2. Staff Furnish (POST) - Create new offender record
 * 3. Staff Redirect (POST) - Overwrite existing offender record
 * 4. Staff Update (POST) - Update offender details
 * 5. Batch Furnish (POST) - Furnish multiple notices with same offender particulars
 * 6. Batch Update Address (POST) - Update address for multiple notices with same offender ID
 *
 * Based on Functional Spec v1.1 Sections 4.6.2 and 5 (Batch Operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ocms41StaffFurnishService {

    private final OcmsValidOffenceNoticeService noticeService;
    private final SuspendedNoticeService suspensionService;
    private final OcmsOffenceNoticeOwnerDriverService offenderService;

    // Furnishable/Redirectable stages (before CPC)
    private static final List<String> FURNISHABLE_STAGES = List.of(
            "NI1", "NI2", "NI3", "NI4", "NI5", "NI6",
            "RD1", "RD2", "DN1", "DN2",
            "RR1", "RR2", "RR3", "DR1", "DR2", "DR3"
    );

    // Suspension type
    private static final String SUSPENSION_TYPE_PS = "PS";

    // Offender roles
    private static final String ROLE_OWNER = "O";
    private static final String ROLE_HIRER = "H";
    private static final String ROLE_DRIVER = "D";

    // Processing stages for furnish
    private static final String STAGE_RD1 = "RD1"; // Owner/Hirer furnished
    private static final String STAGE_DN1 = "DN1"; // Driver furnished

    /**
     * API 19: Furnish-Redirect Check
     *
     * Validates whether a notice can be furnished or redirected.
     *
     * Checks:
     * 1. Notice does not have active Permanent Suspension (PS)
     * 2. Notice's last processing stage is eligible for furnishing/redirect (before CPC)
     *
     * @param noticeNo Notice number to check
     * @return FurnishCheckResult with furnishable status and reason
     */
    public FurnishCheckResult checkFurnishable(String noticeNo) {
        log.info("Checking if notice {} can be furnished/redirected", noticeNo);

        // Get notice
        Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(noticeNo);
        if (noticeOpt.isEmpty()) {
            log.error("Notice not found: {}", noticeNo);
            return FurnishCheckResult.notFurnishable("NOTICE_NOT_FOUND", "Notice not found");
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();

        // CHECK 1: Active Permanent Suspension
        List<SuspendedNotice> suspensions = suspensionService.findByNoticeNoAndSuspensionType(
                noticeNo, SUSPENSION_TYPE_PS);

        boolean hasActivePS = suspensions.stream()
                .anyMatch(s -> s.getDateOfRevival() == null || s.getDateOfRevival().isAfter(LocalDateTime.now()));

        if (hasActivePS) {
            log.warn("Notice {} has active Permanent Suspension", noticeNo);
            return FurnishCheckResult.notFurnishable("ACTIVE_PS",
                    "Notice cannot be furnished - Active Permanent Suspension");
        }

        // CHECK 2: Last Processing Stage must be before CPC (furnishable stage)
        String lastStage = notice.getLastProcessingStage();
        if (!FURNISHABLE_STAGES.contains(lastStage)) {
            log.warn("Notice {} is not in furnishable stage. Current stage: {}", noticeNo, lastStage);
            return FurnishCheckResult.notFurnishable("LAST_STAGE_AFTER_CPC",
                    "Notice cannot be furnished - Last Processing Stage is after CPC");
        }

        log.info("Notice {} is furnishable", noticeNo);
        return FurnishCheckResult.furnishable();
    }

    /**
     * API 20: Staff Furnish (POST)
     *
     * Creates a new offender record for furnished hirer/driver particulars.
     * This is used when there is NO existing offender for the role.
     *
     * Steps:
     * 1. Validate furnishability
     * 2. Create new offender record (ocms_offence_notice_owner_driver)
     * 3. Create mailing address record (ocms_offence_notice_owner_driver_addr)
     * 4. Change processing stage (Owner/Hirer→RD1, Driver→DN1)
     *
     * @param request Furnish request with offender details
     * @return FurnishResult with success status and furnished offender info
     */
    @Transactional
    public FurnishResult staffFurnish(StaffFurnishRequest request) {
        log.info("Staff furnishing {} for notice={}, id={}",
                request.getOwnerDriverIndicator(), request.getNoticeNo(), request.getIdNo());

        try {
            // Validate furnishability
            FurnishCheckResult checkResult = checkFurnishable(request.getNoticeNo());
            if (!checkResult.isFurnishable()) {
                return FurnishResult.failure(checkResult.getReasonMessage());
            }

            // Get notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(request.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                return FurnishResult.failure("Notice not found");
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Create new offender record
            OcmsOffenceNoticeOwnerDriver newOffender = new OcmsOffenceNoticeOwnerDriver();
            newOffender.setNoticeNo(request.getNoticeNo());
            newOffender.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
            newOffender.setName(request.getName());
            newOffender.setIdType(request.getIdType());
            newOffender.setIdNo(request.getIdNo());
            newOffender.setOffenderTelCode(request.getOffenderTelCode());
            newOffender.setOffenderTelNo(request.getOffenderTelNo());
            newOffender.setEmailAddr(request.getEmailAddr());
            newOffender.setOffenderIndicator("Y"); // Furnished person is current offender

            // Save offender
            offenderService.save(newOffender);

            // TODO: Create address record in ocms_offence_notice_owner_driver_addr
            // This requires the address entity/service which may not exist yet

            // Change processing stage
            String newStage = determineNewStage(request.getOwnerDriverIndicator());
            notice.setLastProcessingStage(newStage);
            notice.setNextProcessingStage(newStage);
            noticeService.update(notice.getNoticeNo(), notice);

            log.info("Successfully furnished {} for notice={}",
                    request.getOwnerDriverIndicator(), request.getNoticeNo());

            return FurnishResult.success(
                    request.getNoticeNo(),
                    newOffender.getName(),
                    newOffender.getIdNo(),
                    newStage
            );

        } catch (Exception e) {
            log.error("Failed to furnish {} for notice={}: {}",
                    request.getOwnerDriverIndicator(), request.getNoticeNo(), e.getMessage(), e);
            return FurnishResult.failure("Error furnishing offender: " + e.getMessage());
        }
    }

    /**
     * API 21: Staff Redirect (POST)
     *
     * Overwrites an existing offender record with new furnished particulars.
     * This is used when there IS an existing offender for the role and OIC wants to overwrite.
     *
     * Steps:
     * 1. Validate furnishability
     * 2. Overwrite existing offender record with new data
     * 3. Update mailing address
     * 4. Change processing stage
     *
     * @param request Redirect request with new offender details
     * @return FurnishResult with success status
     */
    @Transactional
    public FurnishResult staffRedirect(StaffFurnishRequest request) {
        log.info("Staff redirecting {} for notice={}, id={}",
                request.getOwnerDriverIndicator(), request.getNoticeNo(), request.getIdNo());

        try {
            // Validate furnishability
            FurnishCheckResult checkResult = checkFurnishable(request.getNoticeNo());
            if (!checkResult.isFurnishable()) {
                return FurnishResult.failure(checkResult.getReasonMessage());
            }

            // Get notice
            Optional<OcmsValidOffenceNotice> noticeOpt = noticeService.getById(request.getNoticeNo());
            if (noticeOpt.isEmpty()) {
                return FurnishResult.failure("Notice not found");
            }

            OcmsValidOffenceNotice notice = noticeOpt.get();

            // Update existing offender record
            OcmsOffenceNoticeOwnerDriver updatedOffender = new OcmsOffenceNoticeOwnerDriver();
            updatedOffender.setNoticeNo(request.getNoticeNo());
            updatedOffender.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
            updatedOffender.setName(request.getName());
            updatedOffender.setIdType(request.getIdType());
            updatedOffender.setIdNo(request.getIdNo());
            updatedOffender.setOffenderTelCode(request.getOffenderTelCode());
            updatedOffender.setOffenderTelNo(request.getOffenderTelNo());
            updatedOffender.setEmailAddr(request.getEmailAddr());
            updatedOffender.setOffenderIndicator("Y");

            // Create composite ID
            var compositeKey = updatedOffender.getId();

            // Update offender
            offenderService.update(compositeKey, updatedOffender);

            // TODO: Update address record

            // Change processing stage
            String newStage = determineNewStage(request.getOwnerDriverIndicator());
            notice.setLastProcessingStage(newStage);
            notice.setNextProcessingStage(newStage);
            noticeService.update(notice.getNoticeNo(), notice);

            log.info("Successfully redirected {} for notice={}",
                    request.getOwnerDriverIndicator(), request.getNoticeNo());

            return FurnishResult.success(
                    request.getNoticeNo(),
                    updatedOffender.getName(),
                    updatedOffender.getIdNo(),
                    newStage
            );

        } catch (Exception e) {
            log.error("Failed to redirect {} for notice={}: {}",
                    request.getOwnerDriverIndicator(), request.getNoticeNo(), e.getMessage(), e);
            return FurnishResult.failure("Error redirecting offender: " + e.getMessage());
        }
    }

    /**
     * API 22: Staff Update (POST)
     *
     * Updates existing offender details without changing the offender identity.
     * This is used for updating contact info, address, etc.
     *
     * @param request Update request
     * @return FurnishResult with success status
     */
    @Transactional
    public FurnishResult staffUpdate(StaffFurnishRequest request) {
        log.info("Staff updating {} for notice={}",
                request.getOwnerDriverIndicator(), request.getNoticeNo());

        // For now, same implementation as redirect
        // In the future, this might have different business logic
        return staffRedirect(request);
    }

    /**
     * API 23: Batch Furnish Processing
     *
     * Furnishes multiple notices with the same offender particulars.
     * Internally loops through each notice and calls staffFurnish() or staffRedirect().
     *
     * @param request Batch furnish request with list of notice numbers
     * @return BatchFurnishResult with results for each notice
     */
    @Transactional
    public BatchFurnishResult batchFurnish(BatchFurnishRequest request) {
        log.info("Batch furnishing {} for {} notices",
                request.getOwnerDriverIndicator(), request.getNoticeNumbers().size());

        BatchFurnishResult batchResult = new BatchFurnishResult();

        for (String noticeNo : request.getNoticeNumbers()) {
            // Create individual furnish request
            StaffFurnishRequest individualRequest = new StaffFurnishRequest();
            individualRequest.setNoticeNo(noticeNo);
            individualRequest.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
            individualRequest.setName(request.getName());
            individualRequest.setIdType(request.getIdType());
            individualRequest.setIdNo(request.getIdNo());
            individualRequest.setOffenderTelCode(request.getOffenderTelCode());
            individualRequest.setOffenderTelNo(request.getOffenderTelNo());
            individualRequest.setEmailAddr(request.getEmailAddr());
            individualRequest.setPostalCode(request.getPostalCode());
            individualRequest.setBlkHseNo(request.getBlkHseNo());
            individualRequest.setStreetName(request.getStreetName());
            individualRequest.setFloorNo(request.getFloorNo());
            individualRequest.setUnitNo(request.getUnitNo());
            individualRequest.setBldgName(request.getBldgName());

            // Furnish (uses same backend logic as single furnish)
            FurnishResult result = staffFurnish(individualRequest);

            // Add to batch result
            batchResult.addResult(noticeNo, result);
        }

        log.info("Batch furnish complete: {} successes, {} failures",
                batchResult.getSuccessCount(), batchResult.getFailureCount());

        return batchResult;
    }

    /**
     * API 24: Batch Update Address
     *
     * Updates mailing address for multiple notices with the same offender ID.
     *
     * @param request Batch update request with ID number and new address
     * @return BatchFurnishResult with results for each notice
     */
    @Transactional
    public BatchFurnishResult batchUpdateAddress(BatchUpdateAddressRequest request) {
        log.info("Batch updating address for ID={}, {} notices",
                request.getIdNo(), request.getNoticeNumbers().size());

        BatchFurnishResult batchResult = new BatchFurnishResult();

        for (String noticeNo : request.getNoticeNumbers()) {
            // Create individual update request
            StaffFurnishRequest individualRequest = new StaffFurnishRequest();
            individualRequest.setNoticeNo(noticeNo);
            individualRequest.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
            individualRequest.setName(request.getName());
            individualRequest.setIdType(request.getIdType());
            individualRequest.setIdNo(request.getIdNo());
            individualRequest.setOffenderTelCode(request.getOffenderTelCode());
            individualRequest.setOffenderTelNo(request.getOffenderTelNo());
            individualRequest.setEmailAddr(request.getEmailAddr());
            individualRequest.setPostalCode(request.getPostalCode());
            individualRequest.setBlkHseNo(request.getBlkHseNo());
            individualRequest.setStreetName(request.getStreetName());
            individualRequest.setFloorNo(request.getFloorNo());
            individualRequest.setUnitNo(request.getUnitNo());
            individualRequest.setBldgName(request.getBldgName());

            // Update (uses same backend logic as single update)
            FurnishResult result = staffUpdate(individualRequest);

            // Add to batch result
            batchResult.addResult(noticeNo, result);
        }

        log.info("Batch address update complete: {} successes, {} failures",
                batchResult.getSuccessCount(), batchResult.getFailureCount());

        return batchResult;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Determine new processing stage based on offender role
     *
     * Owner/Hirer → RD1
     * Driver → DN1
     */
    private String determineNewStage(String role) {
        if (ROLE_OWNER.equals(role) || ROLE_HIRER.equals(role)) {
            return STAGE_RD1;
        } else if (ROLE_DRIVER.equals(role)) {
            return STAGE_DN1;
        } else {
            log.warn("Unknown role: {}, defaulting to RD1", role);
            return STAGE_RD1;
        }
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Result class for furnish-redirect check
     */
    public static class FurnishCheckResult {
        private final boolean furnishable;
        private final String reasonCode;
        private final String reasonMessage;

        private FurnishCheckResult(boolean furnishable, String reasonCode, String reasonMessage) {
            this.furnishable = furnishable;
            this.reasonCode = reasonCode;
            this.reasonMessage = reasonMessage;
        }

        public static FurnishCheckResult furnishable() {
            return new FurnishCheckResult(true, null, null);
        }

        public static FurnishCheckResult notFurnishable(String reasonCode, String reasonMessage) {
            return new FurnishCheckResult(false, reasonCode, reasonMessage);
        }

        public boolean isFurnishable() {
            return furnishable;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getReasonMessage() {
            return reasonMessage;
        }
    }

    /**
     * Result class for furnish/redirect/update operations
     */
    public static class FurnishResult {
        private final boolean success;
        private final String message;
        private final String noticeNo;
        private final String offenderName;
        private final String offenderIdNo;
        private final String newProcessingStage;

        private FurnishResult(boolean success, String message, String noticeNo,
                             String offenderName, String offenderIdNo, String newProcessingStage) {
            this.success = success;
            this.message = message;
            this.noticeNo = noticeNo;
            this.offenderName = offenderName;
            this.offenderIdNo = offenderIdNo;
            this.newProcessingStage = newProcessingStage;
        }

        public static FurnishResult success(String noticeNo, String offenderName,
                                           String offenderIdNo, String newProcessingStage) {
            return new FurnishResult(true, "Success", noticeNo, offenderName,
                                   offenderIdNo, newProcessingStage);
        }

        public static FurnishResult failure(String message) {
            return new FurnishResult(false, message, null, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getNoticeNo() {
            return noticeNo;
        }

        public String getOffenderName() {
            return offenderName;
        }

        public String getOffenderIdNo() {
            return offenderIdNo;
        }

        public String getNewProcessingStage() {
            return newProcessingStage;
        }
    }

    /**
     * Request DTO for staff furnish/redirect/update operations
     */
    public static class StaffFurnishRequest {
        private String noticeNo;
        private String ownerDriverIndicator; // O, H, or D
        private String name;
        private String idType; // N, F, B, P
        private String idNo;
        private String offenderTelCode;
        private String offenderTelNo;
        private String emailAddr;

        // Address fields
        private String postalCode;
        private String blkHseNo;
        private String streetName;
        private String floorNo;
        private String unitNo;
        private String bldgName;

        // Getters and setters
        public String getNoticeNo() { return noticeNo; }
        public void setNoticeNo(String noticeNo) { this.noticeNo = noticeNo; }

        public String getOwnerDriverIndicator() { return ownerDriverIndicator; }
        public void setOwnerDriverIndicator(String ownerDriverIndicator) {
            this.ownerDriverIndicator = ownerDriverIndicator;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIdType() { return idType; }
        public void setIdType(String idType) { this.idType = idType; }

        public String getIdNo() { return idNo; }
        public void setIdNo(String idNo) { this.idNo = idNo; }

        public String getOffenderTelCode() { return offenderTelCode; }
        public void setOffenderTelCode(String offenderTelCode) {
            this.offenderTelCode = offenderTelCode;
        }

        public String getOffenderTelNo() { return offenderTelNo; }
        public void setOffenderTelNo(String offenderTelNo) {
            this.offenderTelNo = offenderTelNo;
        }

        public String getEmailAddr() { return emailAddr; }
        public void setEmailAddr(String emailAddr) { this.emailAddr = emailAddr; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

        public String getBlkHseNo() { return blkHseNo; }
        public void setBlkHseNo(String blkHseNo) { this.blkHseNo = blkHseNo; }

        public String getStreetName() { return streetName; }
        public void setStreetName(String streetName) { this.streetName = streetName; }

        public String getFloorNo() { return floorNo; }
        public void setFloorNo(String floorNo) { this.floorNo = floorNo; }

        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }

        public String getBldgName() { return bldgName; }
        public void setBldgName(String bldgName) { this.bldgName = bldgName; }
    }

    /**
     * Request DTO for batch furnish operations (API 23)
     *
     * Contains a list of notice numbers + one set of offender particulars
     * to be applied to all notices.
     */
    public static class BatchFurnishRequest {
        private List<String> noticeNumbers;
        private String ownerDriverIndicator; // O, H, or D
        private String name;
        private String idType; // N, F, B, P
        private String idNo;
        private String offenderTelCode;
        private String offenderTelNo;
        private String emailAddr;

        // Address fields
        private String postalCode;
        private String blkHseNo;
        private String streetName;
        private String floorNo;
        private String unitNo;
        private String bldgName;

        // Getters and setters
        public List<String> getNoticeNumbers() { return noticeNumbers; }
        public void setNoticeNumbers(List<String> noticeNumbers) {
            this.noticeNumbers = noticeNumbers;
        }

        public String getOwnerDriverIndicator() { return ownerDriverIndicator; }
        public void setOwnerDriverIndicator(String ownerDriverIndicator) {
            this.ownerDriverIndicator = ownerDriverIndicator;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIdType() { return idType; }
        public void setIdType(String idType) { this.idType = idType; }

        public String getIdNo() { return idNo; }
        public void setIdNo(String idNo) { this.idNo = idNo; }

        public String getOffenderTelCode() { return offenderTelCode; }
        public void setOffenderTelCode(String offenderTelCode) {
            this.offenderTelCode = offenderTelCode;
        }

        public String getOffenderTelNo() { return offenderTelNo; }
        public void setOffenderTelNo(String offenderTelNo) {
            this.offenderTelNo = offenderTelNo;
        }

        public String getEmailAddr() { return emailAddr; }
        public void setEmailAddr(String emailAddr) { this.emailAddr = emailAddr; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

        public String getBlkHseNo() { return blkHseNo; }
        public void setBlkHseNo(String blkHseNo) { this.blkHseNo = blkHseNo; }

        public String getStreetName() { return streetName; }
        public void setStreetName(String streetName) { this.streetName = streetName; }

        public String getFloorNo() { return floorNo; }
        public void setFloorNo(String floorNo) { this.floorNo = floorNo; }

        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }

        public String getBldgName() { return bldgName; }
        public void setBldgName(String bldgName) { this.bldgName = bldgName; }
    }

    /**
     * Request DTO for batch update address operations (API 24)
     *
     * Contains a list of notice numbers + new address details.
     * All notices must belong to the same offender (same ID number).
     */
    public static class BatchUpdateAddressRequest {
        private List<String> noticeNumbers;
        private String ownerDriverIndicator; // O, H, or D
        private String name;
        private String idType; // N, F, B, P
        private String idNo;
        private String offenderTelCode;
        private String offenderTelNo;
        private String emailAddr;

        // Address fields
        private String postalCode;
        private String blkHseNo;
        private String streetName;
        private String floorNo;
        private String unitNo;
        private String bldgName;

        // Getters and setters
        public List<String> getNoticeNumbers() { return noticeNumbers; }
        public void setNoticeNumbers(List<String> noticeNumbers) {
            this.noticeNumbers = noticeNumbers;
        }

        public String getOwnerDriverIndicator() { return ownerDriverIndicator; }
        public void setOwnerDriverIndicator(String ownerDriverIndicator) {
            this.ownerDriverIndicator = ownerDriverIndicator;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIdType() { return idType; }
        public void setIdType(String idType) { this.idType = idType; }

        public String getIdNo() { return idNo; }
        public void setIdNo(String idNo) { this.idNo = idNo; }

        public String getOffenderTelCode() { return offenderTelCode; }
        public void setOffenderTelCode(String offenderTelCode) {
            this.offenderTelCode = offenderTelCode;
        }

        public String getOffenderTelNo() { return offenderTelNo; }
        public void setOffenderTelNo(String offenderTelNo) {
            this.offenderTelNo = offenderTelNo;
        }

        public String getEmailAddr() { return emailAddr; }
        public void setEmailAddr(String emailAddr) { this.emailAddr = emailAddr; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

        public String getBlkHseNo() { return blkHseNo; }
        public void setBlkHseNo(String blkHseNo) { this.blkHseNo = blkHseNo; }

        public String getStreetName() { return streetName; }
        public void setStreetName(String streetName) { this.streetName = streetName; }

        public String getFloorNo() { return floorNo; }
        public void setFloorNo(String floorNo) { this.floorNo = floorNo; }

        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }

        public String getBldgName() { return bldgName; }
        public void setBldgName(String bldgName) { this.bldgName = bldgName; }
    }

    /**
     * Result DTO for batch operations (API 23 and 24)
     *
     * Contains results for each notice in the batch.
     */
    public static class BatchFurnishResult {
        private final java.util.Map<String, FurnishResult> results = new java.util.HashMap<>();
        private int successCount = 0;
        private int failureCount = 0;

        /**
         * Add result for a notice
         */
        public void addResult(String noticeNo, FurnishResult result) {
            results.put(noticeNo, result);
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        /**
         * Get result for a specific notice
         */
        public FurnishResult getResult(String noticeNo) {
            return results.get(noticeNo);
        }

        /**
         * Get all results
         */
        public java.util.Map<String, FurnishResult> getAllResults() {
            return results;
        }

        /**
         * Get number of successful operations
         */
        public int getSuccessCount() {
            return successCount;
        }

        /**
         * Get number of failed operations
         */
        public int getFailureCount() {
            return failureCount;
        }

        /**
         * Get total number of operations
         */
        public int getTotalCount() {
            return successCount + failureCount;
        }

        /**
         * Check if all operations succeeded
         */
        public boolean isAllSuccess() {
            return failureCount == 0;
        }
    }
}
