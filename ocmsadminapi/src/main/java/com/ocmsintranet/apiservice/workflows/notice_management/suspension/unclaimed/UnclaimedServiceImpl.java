package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticulars;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars.OcmsRequestDriverParticularsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice.OcmsDriverNoticeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of UnclaimedService
 * Handles Unclaimed Reminders processing logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnclaimedServiceImpl implements UnclaimedService {

    private final OcmsValidOffenceNoticeService validOffenceNoticeService;
    private final OcmsOffenceNoticeOwnerDriverService onodService;
    private final SuspendedNoticeService suspendedNoticeService;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsRequestDriverParticularsRepository rdpRepository;
    private final OcmsDriverNoticeRepository dnRepository;

    @Override
    public List<UnclaimedReminderDto> checkUnclaimedNotices(List<String> noticeNumbers) {
        log.info("Checking {} notice numbers for unclaimed processing", noticeNumbers.size());

        List<UnclaimedReminderDto> results = new ArrayList<>();

        for (String noticeNo : noticeNumbers) {
            try {
                // 1. Check if notice exists
                Optional<OcmsValidOffenceNotice> vonOpt = validOffenceNoticeService.getById(noticeNo);
                if (vonOpt.isEmpty()) {
                    log.warn("Notice not found: {}", noticeNo);
                    UnclaimedReminderDto errorDto = new UnclaimedReminderDto();
                    errorDto.setNoticeNo(noticeNo);
                    results.add(errorDto);
                    continue;
                }

                OcmsValidOffenceNotice von = vonOpt.get();

                // 2. Get offender details from ONOD
                List<OcmsOffenceNoticeOwnerDriver> onods = onodService.findByNoticeNo(noticeNo);

                // Get the offender (not just owner/hirer)
                OcmsOffenceNoticeOwnerDriver offender = onods.stream()
                    .filter(o -> "Y".equals(o.getOffenderIndicator()))
                    .findFirst()
                    .orElse(onods.isEmpty() ? null : onods.get(0));

                if (offender == null) {
                    log.warn("No offender found for notice: {}", noticeNo);
                    UnclaimedReminderDto errorDto = new UnclaimedReminderDto();
                    errorDto.setNoticeNo(noticeNo);
                    results.add(errorDto);
                    continue;
                }

                // 3. Create DTO with retrieved data
                UnclaimedReminderDto dto = new UnclaimedReminderDto();
                dto.setNoticeNo(noticeNo);
                dto.setLastProcessingStage(von.getProcessingStage());
                dto.setIdNumber(offender.getIdNo());
                dto.setIdType(offender.getIdType());
                dto.setOwnerHirerIndicator(offender.getOwnerDriverIndicator());

                // TODO: Get reminder letter date
                // This might require a separate ReminderLetter entity/service
                // For now, leaving it null

                results.add(dto);
                log.debug("Retrieved details for notice: {}", noticeNo);

            } catch (Exception e) {
                log.error("Error checking notice: {}", noticeNo, e);
                UnclaimedReminderDto errorDto = new UnclaimedReminderDto();
                errorDto.setNoticeNo(noticeNo);
                results.add(errorDto);
            }
        }

        log.info("Completed checking {} notices", noticeNumbers.size());
        return results;
    }

    @Override
    public List<UnclaimedProcessingResult> processUnclaimedReminders(List<UnclaimedReminderDto> unclaimedRecords) {
        log.info("Processing {} unclaimed reminder records", unclaimedRecords.size());

        List<UnclaimedProcessingResult> results = new ArrayList<>();

        for (UnclaimedReminderDto record : unclaimedRecords) {
            try {
                String noticeNo = record.getNoticeNo();
                log.info("Processing unclaimed reminder for notice: {}", noticeNo);

                // 1. Validate notice exists
                Optional<OcmsValidOffenceNotice> vonOpt = validOffenceNoticeService.getById(noticeNo);
                if (vonOpt.isEmpty()) {
                    log.error("Notice not found: {}", noticeNo);
                    results.add(new UnclaimedProcessingResult(
                        noticeNo,
                        false,
                        "Notice not found",
                        "Notice does not exist in system"
                    ));
                    continue;
                }

                // 2. Get next srNo for this notice
                Integer maxSrNo = suspendedNoticeRepository.findMaxSrNoByNoticeNo(noticeNo);
                Integer nextSrNo = (maxSrNo != null ? maxSrNo : 0) + 1;

                // 3. Calculate revival date
                LocalDateTime now = LocalDateTime.now();
                Integer daysOfRevival = record.getDaysOfRevival() != null ? record.getDaysOfRevival() : 10;
                LocalDateTime dueDateOfRevival = now.plusDays(daysOfRevival);

                // 4. Create TS-UNC suspension record
                SuspendedNotice suspension = SuspendedNotice.builder()
                    .noticeNo(noticeNo)
                    .dateOfSuspension(now)
                    .srNo(nextSrNo)
                    .suspensionSource("OCMS")
                    .suspensionType("TS")  // Temporary Suspension
                    .reasonOfSuspension("UNC")  // Unclaimed
                    .officerAuthorisingSupension("SYSTEM")  // TODO: Get from security context
                    .dueDateOfRevival(dueDateOfRevival)
                    .suspensionRemarks(record.getSuspensionRemarks())
                    .build();

                // Set audit fields
                suspension.setCreUserId("SYSTEM");  // TODO: Get from security context

                // 5. Save suspension
                suspendedNoticeRepository.save(suspension);

                log.info("Successfully applied TS-UNC suspension to notice: {} (srNo: {})", noticeNo, nextSrNo);

                // 6. Update RDP tables with date_of_return and unclaimed_reason
                updateRdpTables(noticeNo, record.getDateOfReturn(), record.getReasonForUnclaim());

                // 7. Update DN tables with date_of_return and reason_for_unclaim
                updateDnTables(noticeNo, record.getDateOfReturn(), record.getReasonForUnclaim());

                results.add(new UnclaimedProcessingResult(
                    noticeNo,
                    true,
                    "TS-UNC suspension applied successfully"
                ));

            } catch (Exception e) {
                log.error("Error processing unclaimed reminder for notice: {}", record.getNoticeNo(), e);
                results.add(new UnclaimedProcessingResult(
                    record.getNoticeNo(),
                    false,
                    "Failed to process",
                    e.getMessage()
                ));
            }
        }

        log.info("Completed processing {} unclaimed reminders", unclaimedRecords.size());
        return results;
    }

    /**
     * Update RDP tables with date_of_return and unclaimed_reason
     * Based on OCMS 20 Spec requirement
     */
    private void updateRdpTables(String noticeNo, LocalDateTime dateOfReturn, String unclaimedReason) {
        try {
            List<OcmsRequestDriverParticulars> rdpRecords = rdpRepository.findByNoticeNo(noticeNo);

            if (rdpRecords.isEmpty()) {
                log.debug("No RDP records found for notice: {}", noticeNo);
                return;
            }

            for (OcmsRequestDriverParticulars rdp : rdpRecords) {
                rdp.setDateOfReturn(dateOfReturn);
                rdp.setUnclaimedReason(unclaimedReason);
                rdp.setUpdUserId("SYSTEM"); // TODO: Get from security context
                rdpRepository.save(rdp);
            }

            log.info("Updated {} RDP records for notice: {}", rdpRecords.size(), noticeNo);

        } catch (Exception e) {
            log.error("Failed to update RDP tables for notice: {}", noticeNo, e);
            // Don't throw - this is supplementary update
        }
    }

    /**
     * Update DN tables with date_of_return and reason_for_unclaim
     * Based on OCMS 20 Spec requirement
     */
    private void updateDnTables(String noticeNo, LocalDateTime dateOfReturn, String reasonForUnclaim) {
        try {
            List<OcmsDriverNotice> dnRecords = dnRepository.findByNoticeNo(noticeNo);

            if (dnRecords.isEmpty()) {
                log.debug("No DN records found for notice: {}", noticeNo);
                return;
            }

            for (OcmsDriverNotice dn : dnRecords) {
                dn.setDateOfReturn(dateOfReturn);
                dn.setReasonForUnclaim(reasonForUnclaim);
                dn.setUpdUserId("SYSTEM"); // TODO: Get from security context
                dnRepository.save(dn);
            }

            log.info("Updated {} DN records for notice: {}", dnRecords.size(), noticeNo);

        } catch (Exception e) {
            log.error("Failed to update DN tables for notice: {}", noticeNo, e);
            // Don't throw - this is supplementary update
        }
    }
}
