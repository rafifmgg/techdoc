package com.ocmsintranet.apiservice.workflows.furnish.manual.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.workflows.furnish.dto.ExistingParticularsResponse;
import com.ocmsintranet.apiservice.workflows.furnish.dto.FurnishableCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validation service for manual furnish operations.
 * Based on OCMS 41 User Stories 41.43-41.45.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManualFurnishValidationService {

    private final OcmsValidOffenceNoticeRepository noticeRepository;
    private final OcmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    private final OcmsOffenceNoticeOwnerDriverAddrRepository ownerDriverAddrRepository;

    // Furnishable stages (up to CPC as per OCMS41.43)
    private static final List<String> FURNISHABLE_STAGES = Arrays.asList(
            "PRE", "1ST", "2ND", "3RD", "CPC"
            // Add more stages as needed based on actual processing stage codes
    );

    /**
     * Check if notice is at furnishable stage (OCMS41.43)
     */
    public FurnishableCheckResponse checkFurnishable(String noticeNo) {
        log.debug("Checking if notice {} is furnishable", noticeNo);

        Optional<OcmsValidOffenceNotice> noticeOpt = noticeRepository.findById(noticeNo);

        if (noticeOpt.isEmpty()) {
            return FurnishableCheckResponse.builder()
                    .noticeNo(noticeNo)
                    .isFurnishable(false)
                    .reason("Notice not found")
                    .build();
        }

        OcmsValidOffenceNotice notice = noticeOpt.get();
        String currentStage = notice.getLastProcessingStage();

        boolean isFurnishable = FURNISHABLE_STAGES.contains(currentStage);

        String reason = isFurnishable ? null :
                String.format("Notice is at stage '%s', which is not furnishable (only stages up to CPC are furnishable)", currentStage);

        return FurnishableCheckResponse.builder()
                .noticeNo(noticeNo)
                .isFurnishable(isFurnishable)
                .currentProcessingStage(currentStage)
                .reason(reason)
                .build();
    }

    /**
     * Check existing owner/hirer/driver particulars (OCMS41.44-41.45)
     */
    public ExistingParticularsResponse checkExistingParticulars(String noticeNo) {
        log.debug("Checking existing particulars for notice {}", noticeNo);

        List<OcmsOffenceNoticeOwnerDriver> existingRecords = ownerDriverRepository.findByNoticeNo(noticeNo);

        if (existingRecords.isEmpty()) {
            return ExistingParticularsResponse.builder()
                    .noticeNo(noticeNo)
                    .hasExistingParticulars(false)
                    .existingParticulars(List.of())
                    .build();
        }

        List<ExistingParticularsResponse.ExistingParticularDetail> details = existingRecords.stream()
                .map(record -> {
                    // Get address for this record
                    List<OcmsOffenceNoticeOwnerDriverAddr> addresses =
                            ownerDriverAddrRepository.findByNoticeNoAndIdNo(noticeNo, record.getIdNo());

                    // Find registered mail address
                    OcmsOffenceNoticeOwnerDriverAddr registeredAddr = addresses.stream()
                            .filter(addr -> "registered_mail".equals(addr.getAddrType()))
                            .findFirst()
                            .orElse(null);

                    return ExistingParticularsResponse.ExistingParticularDetail.builder()
                            .ownerDriverIndicator(record.getOwnerDriverIndicator())
                            .idType(record.getIdType())
                            .idNo(record.getIdNo())
                            .name(record.getName())
                            .currentOffenderIndicator(record.getOffenderIndicator())
                            .blkNo(registeredAddr != null ? registeredAddr.getBlkNo() : null)
                            .floor(registeredAddr != null ? registeredAddr.getFloor() : null)
                            .streetName(registeredAddr != null ? registeredAddr.getStreetName() : null)
                            .unitNo(registeredAddr != null ? registeredAddr.getUnitNo() : null)
                            .bldgName(registeredAddr != null ? registeredAddr.getBldgName() : null)
                            .postalCode(registeredAddr != null ? registeredAddr.getPostalCode() : null)
                            .telCode(record.getOffenderTelCode())
                            .telNo(record.getOffenderTelNo())
                            .emailAddr(record.getEmailAddr())
                            .build();
                })
                .collect(Collectors.toList());

        return ExistingParticularsResponse.builder()
                .noticeNo(noticeNo)
                .hasExistingParticulars(true)
                .existingParticulars(details)
                .build();
    }

    /**
     * Validate if notice exists
     */
    public boolean noticeExists(String noticeNo) {
        return noticeRepository.findById(noticeNo).isPresent();
    }

    /**
     * Validate owner/driver indicator
     */
    public boolean isValidOwnerDriverIndicator(String indicator) {
        return "H".equals(indicator) || "D".equals(indicator);
    }
}
