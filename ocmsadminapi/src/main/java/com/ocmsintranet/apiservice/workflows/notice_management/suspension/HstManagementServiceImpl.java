package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst.OcmsHst;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst.OcmsHstService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of HstManagementService
 * Handles HST (House Tenant) suspension management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HstManagementServiceImpl implements HstManagementService {

    private final OcmsHstService hstService;
    private final SuspendedNoticeService suspendedNoticeService;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsOffenceNoticeOwnerDriverService onodService;
    private final OcmsOffenceNoticeOwnerDriverRepository onodRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository offenceNoticeAddressRepository;

    @Override
    public boolean hstExists(String idNo) {
        Optional<OcmsHst> hst = hstService.getById(idNo);
        return hst.isPresent();
    }

    @Override
    public List<HstProcessingResult> applyHstSuspension(HstApplyDto request) throws HstAlreadyExistsException {
        log.info("Applying HST suspension for ID: {}", request.getIdNo());

        // 1. Check if HST ID already exists
        if (hstExists(request.getIdNo())) {
            throw new HstAlreadyExistsException("HST ID already exists: " + request.getIdNo());
        }

        List<HstProcessingResult> results = new ArrayList<>();

        try {
            // 2. Create HST record
            OcmsHst hst = new OcmsHst();
            hst.setIdNo(request.getIdNo());
            hst.setName(request.getName());
            hst.setStreetName(request.getStreetName());
            hst.setBlkHseNo(request.getBlkHseNo());
            hst.setFloorNo(request.getFloorNo());
            hst.setUnitNo(request.getUnitNo());
            hst.setBldgName(request.getBldgName());
            hst.setPostalCode(request.getPostalCode());
            hst.setCreUserId("SYSTEM"); // TODO: Get from security context

            // TODO: Determine idType from idNo (N for NRIC, F for FIN, etc.)
            hst.setIdType("N");

            hstService.save(hst);
            log.info("Created HST record for ID: {}", request.getIdNo());

            // 3. Find all outstanding notices for this ID
            List<OcmsOffenceNoticeOwnerDriver> onods = onodRepository.findByNoticeNo(request.getIdNo());
            // Filter for offenders only and get unique notice numbers
            List<String> noticeNumbers = onods.stream()
                .filter(o -> "Y".equals(o.getOffenderIndicator()))
                .map(OcmsOffenceNoticeOwnerDriver::getNoticeNo)
                .distinct()
                .collect(Collectors.toList());

            log.info("Found {} outstanding notices for ID: {}", noticeNumbers.size(), request.getIdNo());

            // 4. Apply TS-HST to each notice
            for (String noticeNo : noticeNumbers) {
                try {
                    applyHstToNotice(noticeNo);
                    results.add(HstProcessingResult.success(noticeNo));
                    log.info("Applied TS-HST to notice: {}", noticeNo);
                } catch (Exception e) {
                    log.error("Failed to apply TS-HST to notice: {}", noticeNo, e);
                    results.add(HstProcessingResult.error(noticeNo, e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Error applying HST suspension", e);
            // If HST creation fails, return error for all
            results.add(HstProcessingResult.error("ALL", "Failed to create HST record: " + e.getMessage()));
        }

        return results;
    }

    @Override
    public List<HstProcessingResult> updateHst(String idNo, HstUpdateDto request) {
        log.info("Updating HST record for ID: {}", idNo);

        List<HstProcessingResult> results = new ArrayList<>();

        try {
            // 1. Get HST record
            Optional<OcmsHst> hstOpt = hstService.getById(idNo);
            if (hstOpt.isEmpty()) {
                results.add(HstProcessingResult.error("ALL", "HST ID not found"));
                return results;
            }

            OcmsHst hst = hstOpt.get();

            // 2. Update HST record
            hst.setName(request.getName());
            hst.setStreetName(request.getStreetName());
            hst.setBlkHseNo(request.getBlkHseNo());
            hst.setFloorNo(request.getFloorNo());
            hst.setUnitNo(request.getUnitNo());
            hst.setBldgName(request.getBldgName());
            hst.setPostalCode(request.getPostalCode());
            hst.setUpdUserId("SYSTEM"); // TODO: Get from security context

            hstService.update(idNo, hst);
            log.info("Updated HST record for ID: {}", idNo);

            // 3. Find all TS-HST suspended notices for this ID
            List<OcmsOffenceNoticeOwnerDriver> onods = onodRepository.findByNoticeNo(idNo);
            List<String> noticeNumbers = onods.stream()
                .filter(o -> "Y".equals(o.getOffenderIndicator()))
                .map(OcmsOffenceNoticeOwnerDriver::getNoticeNo)
                .distinct()
                .collect(Collectors.toList());

            // 4. Update address in ONOD for each notice
            for (String noticeNo : noticeNumbers) {
                try {
                    List<OcmsOffenceNoticeOwnerDriver> noticeOnods = onodService.findByNoticeNo(noticeNo);
                    for (OcmsOffenceNoticeOwnerDriver onod : noticeOnods) {
                        if (onod.getIdNo().equals(idNo)) {
                            onod.setName(request.getName());
                            onod.setStreetName(request.getStreetName());
                            onod.setBlkHseNo(request.getBlkHseNo());
                            onod.setFloorNo(request.getFloorNo());
                            onod.setUnitNo(request.getUnitNo());
                            onod.setBldgName(request.getBldgName());
                            onod.setPostalCode(request.getPostalCode());
                            onod.setUpdUserId("SYSTEM"); // TODO: Get from security context

                            onodService.update(onod.getId(), onod);
                        }
                    }
                    results.add(HstProcessingResult.success(noticeNo));
                    log.info("Updated notice: {}", noticeNo);
                } catch (Exception e) {
                    log.error("Failed to update notice: {}", noticeNo, e);
                    results.add(HstProcessingResult.error(noticeNo, e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Error updating HST", e);
            results.add(HstProcessingResult.error("ALL", "Failed to update HST: " + e.getMessage()));
        }

        return results;
    }

    @Override
    public List<HstProcessingResult> reviveHst(HstReviveDto request) {
        log.info("Reviving HST suspensions for ID: {}", request.getIdNo());

        List<HstProcessingResult> results = new ArrayList<>();

        try {
            // 1. Find all TS-HST suspended notices for this ID
            List<OcmsOffenceNoticeOwnerDriver> onods = onodRepository.findByNoticeNo(request.getIdNo());
            List<String> noticeNumbers = onods.stream()
                .filter(o -> "Y".equals(o.getOffenderIndicator()))
                .map(OcmsOffenceNoticeOwnerDriver::getNoticeNo)
                .distinct()
                .collect(Collectors.toList());

            // 2. Revive each notice and update address
            for (String noticeNo : noticeNumbers) {
                try {
                    // Revive TS-HST suspension
                    reviveHstSuspension(noticeNo);

                    // Update address in ONOD
                    List<OcmsOffenceNoticeOwnerDriver> noticeOnods = onodService.findByNoticeNo(noticeNo);
                    for (OcmsOffenceNoticeOwnerDriver onod : noticeOnods) {
                        if (onod.getIdNo().equals(request.getIdNo())) {
                            onod.setName(request.getName());
                            onod.setStreetName(request.getStreetName());
                            onod.setBlkHseNo(request.getBlkHseNo());
                            onod.setFloorNo(request.getFloorNo());
                            onod.setUnitNo(request.getUnitNo());
                            onod.setBldgName(request.getBldgName());
                            onod.setPostalCode(request.getPostalCode());
                            onod.setUpdUserId("SYSTEM"); // TODO: Get from security context

                            onodService.update(onod.getId(), onod);

                            // Update mailing address in OcmsOffenceNoticeOwnerDriverAddr table
                            updateMailingAddress(noticeNo, onod.getOwnerDriverIndicator(), request);
                        }
                    }

                    results.add(HstProcessingResult.success(noticeNo));
                    log.info("Revived notice: {}", noticeNo);
                } catch (Exception e) {
                    log.error("Failed to revive notice: {}", noticeNo, e);
                    results.add(HstProcessingResult.error(noticeNo, e.getMessage()));
                }
            }

            // 3. Delete HST record
            hstService.delete(request.getIdNo());
            log.info("Deleted HST record for ID: {}", request.getIdNo());

        } catch (Exception e) {
            log.error("Error reviving HST", e);
            results.add(HstProcessingResult.error("ALL", "Failed to revive HST: " + e.getMessage()));
        }

        return results;
    }

    /**
     * Apply TS-HST suspension to a notice
     */
    private void applyHstToNotice(String noticeNo) {
        // Get next srNo
        Integer maxSrNo = suspendedNoticeRepository.findMaxSrNoByNoticeNo(noticeNo);
        Integer nextSrNo = (maxSrNo != null ? maxSrNo : 0) + 1;

        LocalDateTime now = LocalDateTime.now();

        // Create TS-HST suspension (no revival date - loops indefinitely)
        SuspendedNotice suspension = SuspendedNotice.builder()
            .noticeNo(noticeNo)
            .dateOfSuspension(now)
            .srNo(nextSrNo)
            .suspensionSource("OCMS")
            .suspensionType("TS")
            .reasonOfSuspension("HST")
            .officerAuthorisingSupension("SYSTEM") // TODO: Get from security context
            .suspensionRemarks("HST Suspension - Address invalid")
            .build();

        suspension.setCreUserId("SYSTEM");
        suspendedNoticeService.save(suspension);
    }

    /**
     * Revive TS-HST suspension for a notice
     */
    private void reviveHstSuspension(String noticeNo) {
        // Find the latest TS-HST suspension
        Optional<SuspendedNotice> latestOpt = suspendedNoticeRepository.findTopByNoticeNoOrderBySrNoDesc(noticeNo);

        if (latestOpt.isPresent()) {
            SuspendedNotice latest = latestOpt.get();

            // Only revive if it's TS-HST and not already revived
            if ("TS".equals(latest.getSuspensionType()) &&
                "HST".equals(latest.getReasonOfSuspension()) &&
                latest.getDateOfRevival() == null) {

                latest.setDateOfRevival(LocalDateTime.now());
                latest.setRevivalReason("HST");
                latest.setOfficerAuthorisingRevival("SYSTEM"); // TODO: Get from security context
                latest.setRevivalRemarks("HST Revival - New address obtained");
                latest.setUpdUserId("SYSTEM");

                // Create composite ID for update
                SuspendedNoticeId id = new SuspendedNoticeId();
                id.setNoticeNo(latest.getNoticeNo());
                id.setDateOfSuspension(latest.getDateOfSuspension());
                id.setSrNo(latest.getSrNo());
                suspendedNoticeService.update(id, latest);
            }
        }
    }

    /**
     * Update mailing address in OcmsOffenceNoticeOwnerDriverAddr table
     * Based on OCMS 20 Spec requirement - update furnished_mail address when HST is revived
     */
    private void updateMailingAddress(String noticeNo, String ownerDriverIndicator, HstReviveDto addressData) {
        try {
            // Type of address for furnished mailing address
            String typeOfAddress = "furnished_mail";

            // Try to find existing furnished_mail address record
            Optional<OcmsOffenceNoticeOwnerDriverAddr> existingOpt = offenceNoticeAddressRepository
                .findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(
                    noticeNo,
                    ownerDriverIndicator,
                    typeOfAddress
                );

            OcmsOffenceNoticeOwnerDriverAddr mailAddress;
            boolean isNew = false;

            if (existingOpt.isPresent()) {
                mailAddress = existingOpt.get();
            } else {
                // Create new furnished_mail address record
                mailAddress = new OcmsOffenceNoticeOwnerDriverAddr();
                mailAddress.setNoticeNo(noticeNo);
                mailAddress.setOwnerDriverIndicator(ownerDriverIndicator);
                mailAddress.setTypeOfAddress(typeOfAddress);
                mailAddress.setCreUserId("SYSTEM"); // TODO: Get from security context
                isNew = true;
            }

            // Update address fields
            mailAddress.setBldgName(addressData.getBldgName());
            mailAddress.setBlkHseNo(addressData.getBlkHseNo());
            mailAddress.setStreetName(addressData.getStreetName());
            mailAddress.setFloorNo(addressData.getFloorNo());
            mailAddress.setUnitNo(addressData.getUnitNo());
            mailAddress.setPostalCode(addressData.getPostalCode());
            mailAddress.setProcessingDateTime(LocalDateTime.now());
            mailAddress.setUpdUserId("SYSTEM"); // TODO: Get from security context

            // Save or update
            if (isNew) {
                offenceNoticeAddressRepository.save(mailAddress);
                log.info("Created furnished_mail address for notice: {}", noticeNo);
            } else {
                offenceNoticeAddressRepository.save(mailAddress);
                log.info("Updated furnished_mail address for notice: {}", noticeNo);
            }

        } catch (Exception e) {
            log.error("Failed to update mailing address for notice: {}", noticeNo, e);
            // Don't throw - this is supplementary update
        }
    }
}
