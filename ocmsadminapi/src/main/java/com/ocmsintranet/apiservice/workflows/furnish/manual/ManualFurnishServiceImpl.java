package com.ocmsintranet.apiservice.workflows.furnish.manual;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.workflows.furnish.dto.*;
import com.ocmsintranet.apiservice.workflows.furnish.manual.dto.*;
import com.ocmsintranet.apiservice.workflows.furnish.manual.helpers.ManualFurnishValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of manual furnish service.
 * Based on OCMS 41 User Stories 41.46-41.51.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishServiceImpl implements ManualFurnishService {

    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;
    private final ManualFurnishValidationService validationService;

    @Override
    @Transactional
    public ManualFurnishResult handleManualFurnish(ManualFurnishRequest request) {
        try {
            log.info("Processing manual furnish - NoticeNo: {}, Officer: {}, Type: {}",
                    request.getNoticeNo(), request.getOfficerId(), request.getOwnerDriverIndicator());

            // Step 1: Validate notice exists
            if (!validationService.noticeExists(request.getNoticeNo())) {
                return ManualFurnishResult.ValidationError.builder()
                        .field("noticeNo")
                        .message("Notice not found: " + request.getNoticeNo())
                        .build();
            }

            // Step 2: Check if notice is furnishable
            FurnishableCheckResponse furnishableCheck = validationService.checkFurnishable(request.getNoticeNo());
            if (!furnishableCheck.isFurnishable()) {
                return ManualFurnishResult.BusinessError.builder()
                        .reason("NOT_FURNISHABLE")
                        .message(furnishableCheck.getReason())
                        .build();
            }

            // Step 3: Validate owner/driver indicator
            if (!validationService.isValidOwnerDriverIndicator(request.getOwnerDriverIndicator())) {
                return ManualFurnishResult.ValidationError.builder()
                        .field("ownerDriverIndicator")
                        .message("Invalid owner/driver indicator. Must be 'H' or 'D'")
                        .build();
            }

            // Step 4: Check existing particulars
            ExistingParticularsResponse existingCheck = validationService.checkExistingParticulars(request.getNoticeNo());

            // Check if same type already exists
            boolean sameTypeExists = existingCheck.getExistingParticulars().stream()
                    .anyMatch(p -> request.getOwnerDriverIndicator().equals(p.getOwnerDriverIndicator()));

            if (sameTypeExists && !request.getOverwriteExisting()) {
                return ManualFurnishResult.BusinessError.builder()
                        .reason("RECORD_EXISTS")
                        .message(String.format("Record exists for %s. Set overwriteExisting=true to update.",
                                request.getOwnerDriverIndicator().equals("H") ? "Hirer" : "Driver"))
                        .build();
            }

            // Step 5: Update existing current offenders to N
            List<OcmsOffenceNoticeOwnerDriver> existingRecords = ownerDriverRepository.findByNoticeNo(request.getNoticeNo());
            for (OcmsOffenceNoticeOwnerDriver existing : existingRecords) {
                if ("Y".equals(existing.getOffenderIndicator())) {
                    existing.setOffenderIndicator("N");
                    ownerDriverRepository.save(existing);
                    log.debug("Updated existing record {} to NOT current offender", existing.getIdNo());
                }
            }

            // Step 6: Check if record already exists for this ID and type
            OcmsOffenceNoticeOwnerDriver existingRecord = existingRecords.stream()
                    .filter(r -> request.getOwnerDriverIndicator().equals(r.getOwnerDriverIndicator()) &&
                                 request.getIdNo().equals(r.getIdNo()))
                    .findFirst()
                    .orElse(null);

            boolean isUpdate = existingRecord != null;

            OcmsOffenceNoticeOwnerDriver ownerDriver;
            if (isUpdate) {
                // Update existing record
                ownerDriver = existingRecord;
                log.debug("Updating existing record for noticeNo: {}, idNo: {}", request.getNoticeNo(), request.getIdNo());
            } else {
                // Create new record
                ownerDriver = new OcmsOffenceNoticeOwnerDriver();
                ownerDriver.setNoticeNo(request.getNoticeNo());
                ownerDriver.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
                log.debug("Creating new record for noticeNo: {}, idNo: {}", request.getNoticeNo(), request.getIdNo());
            }

            // Set/update fields
            ownerDriver.setIdType(request.getIdType());
            ownerDriver.setIdNo(request.getIdNo());
            ownerDriver.setName(request.getName());
            ownerDriver.setOffenderIndicator("Y");
            ownerDriver.setOffenderTelCode(request.getTelCode());
            ownerDriver.setOffenderTelNo(request.getTelNo());
            ownerDriver.setEmailAddr(request.getEmailAddr());

            ownerDriverRepository.save(ownerDriver);

            // Step 7: Create/update address record
            createOrUpdateAddress(request);

            log.info("Successfully {} manual furnish for notice: {}, type: {}, ID: {}",
                    isUpdate ? "updated" : "created", request.getNoticeNo(),
                    request.getOwnerDriverIndicator(), request.getIdNo());

            return ManualFurnishResult.Success.builder()
                    .noticeNo(request.getNoticeNo())
                    .ownerDriverIndicator(request.getOwnerDriverIndicator())
                    .idNo(request.getIdNo())
                    .name(request.getName())
                    .recordUpdated(isUpdate)
                    .message(String.format("%s's particulars %s for notice no. %s",
                            request.getOwnerDriverIndicator().equals("H") ? "Hirer" : "Driver",
                            isUpdate ? "updated" : "created",
                            request.getNoticeNo()))
                    .build();

        } catch (Exception e) {
            log.error("Technical error during manual furnish: noticeNo={}", request.getNoticeNo(), e);

            Map<String, Object> details = new HashMap<>();
            details.put("noticeNo", request.getNoticeNo());
            details.put("officerId", request.getOfficerId());
            details.put("exceptionMessage", e.getMessage());

            return ManualFurnishResult.TechnicalError.builder()
                    .operation("manual furnish")
                    .message("Technical error during manual furnish: " + e.getMessage())
                    .cause(e.getClass().getName())
                    .details(details)
                    .build();
        }
    }

    @Override
    @Transactional
    public BulkFurnishResult handleBulkFurnish(BulkFurnishRequest request) {
        try {
            log.info("Processing bulk furnish - {} notices, Officer: {}, Type: {}",
                    request.getNoticeNos().size(), request.getOfficerId(), request.getOwnerDriverIndicator());

            List<String> successNotices = new ArrayList<>();
            List<BulkFurnishResult.FailedNotice> failedNotices = new ArrayList<>();

            for (String noticeNo : request.getNoticeNos()) {
                // Create individual manual furnish request
                ManualFurnishRequest singleRequest = ManualFurnishRequest.builder()
                        .noticeNo(noticeNo)
                        .officerId(request.getOfficerId())
                        .ownerDriverIndicator(request.getOwnerDriverIndicator())
                        .idType(request.getIdType())
                        .idNo(request.getIdNo())
                        .name(request.getName())
                        .blkNo(request.getBlkNo())
                        .floor(request.getFloor())
                        .streetName(request.getStreetName())
                        .unitNo(request.getUnitNo())
                        .bldgName(request.getBldgName())
                        .postalCode(request.getPostalCode())
                        .telCode(request.getTelCode())
                        .telNo(request.getTelNo())
                        .emailAddr(request.getEmailAddr())
                        .overwriteExisting(request.getOverwriteExisting())
                        .remarks(request.getRemarks())
                        .build();

                // Process individual notice
                ManualFurnishResult result = handleManualFurnish(singleRequest);

                if (result instanceof ManualFurnishResult.Success) {
                    successNotices.add(noticeNo);
                } else {
                    String reason;
                    String message;

                    if (result instanceof ManualFurnishResult.ValidationError validationError) {
                        reason = "VALIDATION_ERROR";
                        message = validationError.message();
                    } else if (result instanceof ManualFurnishResult.BusinessError businessError) {
                        reason = businessError.reason();
                        message = businessError.message();
                    } else if (result instanceof ManualFurnishResult.TechnicalError technicalError) {
                        reason = "TECHNICAL_ERROR";
                        message = technicalError.message();
                    } else {
                        reason = "UNKNOWN_ERROR";
                        message = "Unknown error occurred";
                    }

                    failedNotices.add(BulkFurnishResult.FailedNotice.builder()
                            .noticeNo(noticeNo)
                            .reason(reason)
                            .message(message)
                            .build());
                }
            }

            log.info("Bulk furnish completed - Total: {}, Success: {}, Failed: {}",
                    request.getNoticeNos().size(), successNotices.size(), failedNotices.size());

            return BulkFurnishResult.Success.builder()
                    .totalNotices(request.getNoticeNos().size())
                    .successCount(successNotices.size())
                    .failedCount(failedNotices.size())
                    .successNotices(successNotices)
                    .failedNotices(failedNotices)
                    .message(String.format("Bulk furnish completed. Success: %d, Failed: %d",
                            successNotices.size(), failedNotices.size()))
                    .build();

        } catch (Exception e) {
            log.error("Technical error during bulk furnish", e);

            Map<String, Object> details = new HashMap<>();
            details.put("totalNotices", request.getNoticeNos().size());
            details.put("officerId", request.getOfficerId());
            details.put("exceptionMessage", e.getMessage());

            return BulkFurnishResult.TechnicalError.builder()
                    .operation("bulk furnish")
                    .message("Technical error during bulk furnish: " + e.getMessage())
                    .cause(e.getClass().getName())
                    .details(details)
                    .build();
        }
    }

    /**
     * Create or update address record
     */
    private void createOrUpdateAddress(ManualFurnishRequest request) {
        // Check if address already exists
        List<OcmsOffenceNoticeOwnerDriverAddr> existingAddresses =
                ownerDriverAddrRepository.findByNoticeNoAndOwnerDriverIndicator(
                        request.getNoticeNo(), request.getOwnerDriverIndicator());

        OcmsOffenceNoticeOwnerDriverAddr registeredAddr = existingAddresses.stream()
                .filter(addr -> "registered_mail".equals(addr.getTypeOfAddress()))
                .findFirst()
                .orElse(null);

        if (registeredAddr != null) {
            // Update existing address
            log.debug("Updating existing address for noticeNo: {}, idNo: {}", request.getNoticeNo(), request.getIdNo());
        } else {
            // Create new address
            registeredAddr = new OcmsOffenceNoticeOwnerDriverAddr();
            registeredAddr.setNoticeNo(request.getNoticeNo());
            registeredAddr.setOwnerDriverIndicator(request.getOwnerDriverIndicator());
            registeredAddr.setTypeOfAddress("registered_mail");
            log.debug("Creating new address for noticeNo: {}, ownerDriverIndicator: {}",
                    request.getNoticeNo(), request.getOwnerDriverIndicator());
        }

        registeredAddr.setBlkHseNo(request.getBlkNo());
        registeredAddr.setFloorNo(request.getFloor());
        registeredAddr.setStreetName(request.getStreetName());
        registeredAddr.setUnitNo(request.getUnitNo());
        registeredAddr.setBldgName(request.getBldgName());
        registeredAddr.setPostalCode(request.getPostalCode());

        ownerDriverAddrRepository.save(registeredAddr);
    }
}
