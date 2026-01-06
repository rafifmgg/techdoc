package com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction.OcmsReducedOffenceAmount;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction.OcmsReducedOffenceAmountRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.workflows.payment_reduction.reduction.dto.ReductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for performing database updates during reduction processing.
 *
 * All updates are wrapped in a single @Transactional method to ensure atomicity.
 * If any update fails, the entire transaction is rolled back.
 *
 * Updates performed:
 * 1. Update intranet ocms_valid_offence_notice (set TS/RED, update amounts/dates)
 * 2. Insert into intranet ocms_suspended_notice (record suspension)
 * 3. Insert into intranet ocms_reduced_offence_amount (record reduction details)
 * 4. Update internet eocms_valid_offence_notice (mirror intranet changes)
 *
 * NOTE: If intranet and internet are on different datasources, this would require
 * a distributed transaction manager or an alternative pattern (e.g., outbox pattern,
 * eventual consistency with compensating actions). Currently assumes single datasource.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReductionPersistenceService {

    private final OcmsValidOffenceNoticeRepository ocmsValidOffenceNoticeRepository;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsReducedOffenceAmountRepository reducedOffenceAmountRepository;
    private final EocmsValidOffenceNoticeService eocmsValidOffenceNoticeService;

    /**
     * Apply reduction updates to all relevant tables in a single transaction.
     *
     * This method must be atomic - if any step fails, all changes are rolled back.
     *
     * @param context The reduction context containing all necessary data
     * @throws RuntimeException if any database operation fails
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyReduction(ReductionContext context) {
        String noticeNo = context.getNotice().getNoticeNo();
        log.info("Starting transactional reduction updates for notice {}", noticeNo);

        try {
            // Step 1: Update intranet ocms_valid_offence_notice
            updateIntranetValidOffenceNotice(context);

            // Step 2: Insert into ocms_suspended_notice
            insertSuspendedNotice(context);

            // Step 3: Insert into ocms_reduced_offence_amount
            insertReducedOffenceAmount(context);

            // Step 4: Update internet eocms_valid_offence_notice
            updateInternetValidOffenceNotice(context);

            log.info("Successfully completed all reduction updates for notice {}", noticeNo);

        } catch (Exception e) {
            log.error("Error during reduction persistence for notice {}: {}",
                    noticeNo, e.getMessage(), e);
            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Failed to persist reduction changes: " + e.getMessage(), e);
        }
    }

    /**
     * Update the intranet ocms_valid_offence_notice table.
     *
     * Sets:
     * - suspension_type = "TS" (Temporary Suspension)
     * - epr_reason_of_suspension = "RED" (Reduction)
     * - amount_payable = new reduced amount
     * - epr_reason_suspension_date = date of reduction
     * - due_date_of_revival = expiry date of reduction
     */
    private void updateIntranetValidOffenceNotice(ReductionContext context) {
        log.info("Step 1: Updating intranet ocms_valid_offence_notice for notice {}",
                context.getNotice().getNoticeNo());

        OcmsValidOffenceNotice notice = context.getNotice();

        // Set suspension fields
        notice.setSuspensionType("TS");
        notice.setEprReasonOfSuspension("RED");
        notice.setEprDateOfSuspension(context.getRequest().getDateOfReduction());
        notice.setDueDateOfRevival(context.getRequest().getExpiryDateOfReduction());

        // Update amount payable
        notice.setAmountPayable(context.getRequest().getAmountPayable());

        // Save the updated entity
        ocmsValidOffenceNoticeRepository.save(notice);

        log.info("Successfully updated intranet notice {} with TS-RED suspension",
                notice.getNoticeNo());
    }

    /**
     * Insert a new record into ocms_suspended_notice.
     *
     * This records the suspension due to reduction.
     */
    private void insertSuspendedNotice(ReductionContext context) {
        log.info("Step 2: Inserting into ocms_suspended_notice for notice {}",
                context.getNotice().getNoticeNo());

        SuspendedNotice suspendedNotice = SuspendedNotice.builder()
                .noticeNo(context.getNotice().getNoticeNo())
                .dateOfSuspension(context.getRequest().getDateOfReduction())
                .srNo(context.getSrNo())
                .suspensionSource(context.getRequest().getSuspensionSource())
                .suspensionType("TS")
                .reasonOfSuspension("RED")
                .officerAuthorisingSupension(context.getRequest().getAuthorisedOfficer())
                .dueDateOfRevival(context.getRequest().getExpiryDateOfReduction())
                .suspensionRemarks("Reduction applied - amount reduced: " +
                        context.getRequest().getAmountReduced())
                .build();

        suspendedNoticeRepository.save(suspendedNotice);

        log.info("Successfully inserted suspended notice record for notice {}",
                context.getNotice().getNoticeNo());
    }

    /**
     * Insert a new record into ocms_reduced_offence_amount.
     *
     * This records the reduction details including amounts and dates.
     */
    private void insertReducedOffenceAmount(ReductionContext context) {
        log.info("Step 3: Inserting into ocms_reduced_offence_amount for notice {}",
                context.getNotice().getNoticeNo());

        // Use the same sr_no as suspended_notice
        OcmsReducedOffenceAmount reducedAmount = OcmsReducedOffenceAmount.builder()
                .noticeNo(context.getNotice().getNoticeNo())
                .srNo(context.getSrNo())
                .dateOfReduction(context.getRequest().getDateOfReduction())
                .amountReduced(context.getRequest().getAmountReduced())
                .amountPayable(context.getRequest().getAmountPayable())
                .reasonOfReduction(context.getRequest().getReasonOfReduction())
                .expiryDate(context.getRequest().getExpiryDateOfReduction())
                .authorisedOfficer(context.getRequest().getAuthorisedOfficer())
                .remarks(context.getRequest().getRemarks())
                .build();

        reducedOffenceAmountRepository.save(reducedAmount);

        log.info("Successfully inserted reduced offence amount record for notice {}",
                context.getNotice().getNoticeNo());
    }

    /**
     * Update the internet eocms_valid_offence_notice table.
     *
     * Mirrors the changes made to the intranet table.
     *
     * NOTE: If intranet and internet are on different datasources:
     * - Option 1: Use JTA/XA distributed transactions (performance overhead)
     * - Option 2: Use Saga pattern with compensating transactions
     * - Option 3: Use outbox pattern for eventual consistency
     * - Option 4: Best-effort update with manual reconciliation process
     *
     * IMPORTANT: The internet DB schema does not include dueDateOfRevival field.
     * Only 4 fields are synced: suspensionType, eprReasonOfSuspension, eprDateOfSuspension, amountPayable
     */
    private void updateInternetValidOffenceNotice(ReductionContext context) {
        log.info("Step 4: Updating internet eocms_valid_offence_notice for notice {}",
                context.getNotice().getNoticeNo());

        String noticeNo = context.getNotice().getNoticeNo();
        Optional<EocmsValidOffenceNotice> eNoticeOpt =
                eocmsValidOffenceNoticeService.getById(noticeNo);

        if (eNoticeOpt.isPresent()) {
            EocmsValidOffenceNotice eNotice = eNoticeOpt.get();

            // Mirror intranet changes (4 fields only - dueDateOfRevival does not exist in internet DB schema)
            eNotice.setSuspensionType("TS");
            eNotice.setEprReasonOfSuspension("RED");
            eNotice.setEprDateOfSuspension(context.getRequest().getDateOfReduction());
            eNotice.setAmountPayable(context.getRequest().getAmountPayable());

            eocmsValidOffenceNoticeService.save(eNotice);

            log.info("Successfully updated internet notice {} with TS-RED suspension",
                    context.getNotice().getNoticeNo());
        } else {
            log.warn("Notice {} not found in internet database, skipping update",
                    context.getNotice().getNoticeNo());
            // Note: This does NOT fail the transaction
            // Internet record might not exist if notice was never synced from intranet
        }
    }

    /**
     * Check if a reduction has already been applied (idempotency check).
     *
     * A notice is considered already reduced if:
     * - suspension_type = "TS"
     * - epr_reason_of_suspension = "RED"
     *
     * @param notice The notice to check
     * @return true if reduction has already been applied
     */
    public boolean isReductionAlreadyApplied(OcmsValidOffenceNotice notice) {
        boolean isAlreadyReduced = "TS".equals(notice.getSuspensionType()) &&
                "RED".equals(notice.getEprReasonOfSuspension());

        if (isAlreadyReduced) {
            log.info("Reduction already applied to notice {} (TS-RED status detected)",
                    notice.getNoticeNo());
        }

        return isAlreadyReduced;
    }

    /**
     * Get the next serial number for suspended_notice (and reduced_offence_amount).
     * Both tables share the same sr_no for a given reduction transaction.
     * This queries the max sr_no from suspended_notice and returns max + 1.
     *
     * @param noticeNo The notice number
     * @return The next serial number to be used in both tables
     */
    public Integer getNextSrNo(String noticeNo) {
        Integer maxSrNo = suspendedNoticeRepository
                .findTopByNoticeNoOrderBySrNoDesc(noticeNo)
                .map(SuspendedNotice::getSrNo)
                .orElse(0);

        Integer nextSrNo = maxSrNo + 1;
        log.debug("Next sr_no for notice {} = {}", noticeNo, nextSrNo);
        return nextSrNo;
    }
}
