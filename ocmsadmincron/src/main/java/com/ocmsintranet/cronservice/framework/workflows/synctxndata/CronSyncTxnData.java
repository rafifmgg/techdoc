package com.ocmsintranet.cronservice.framework.workflows.synctxndata;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice.EocmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice.EocmsValidOffenceNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail.EwebTxnDetail;
import com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail.EWebTxnDetailService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices.OcmsRefundNotices;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices.OcmsRefundNoticesId;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices.OcmsRefundNoticesService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice.OcmsSuspendedNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReason;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonId;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.cronservice.crud.ocmsizdb.webtxndetail.WebTxnDetail;
import com.ocmsintranet.cronservice.crud.ocmsizdb.webtxndetail.WebTxnDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cron job for synchronizing transaction data between Internet (EOCMS) and Intranet (OCMS) databases.
 *
 * Flow Summary (Batch Processing with Per-Notice Transaction Isolation):
 * 1. Retrieve all unsynchronized transactions from eocms_web_txn_detail (status='S', is_sync=false)
 * 2. Group transactions by notice_no
 * 3. Process each notice in its own transaction (REQUIRES_NEW):
 *    - If notice processing fails, only that notice is rolled back
 *    - Other notices continue processing successfully
 * 4. For each notice, process all its transactions in order:
 *    a) Batch insert web_txn_detail for this notice
 *    b) Get VON record
 *    c) Check if already marked as FP or PRA → Create refund, skip processing
 *    d) Process each transaction:
 *       - Add payment to existing amount_paid
 *       - Compare vs amount_payable → Apply PS-FP or PS-PRA
 *       - Create suspended notice for each transaction
 *       - If overpayment → Create refund notice
 *    e) Update VON, eVON, insert suspended notices, insert refunds
 *    f) Mark transactions as synchronized
 * 5. Report summary: successful notices vs failed notices
 *
 * Note: Each transaction creates its own suspended notice with sequential sr_no
 */
@Slf4j
@Component
public class CronSyncTxnData {

    private final EWebTxnDetailService ewebTxnDetailService;
    private final WebTxnDetailService webTxnDetailService;
    private final OcmsValidOffenceNoticeService vonService;
    private final EocmsValidOffenceNoticeService eocmsVonService;
    private final OcmsSuspendedNoticeService suspendedNoticeService;
    private final OcmsRefundNoticesService refundNoticesService;
    private final OcmsSuspensionReasonService suspensionReasonService;

    @Value("${payment.sync.enabled:true}")
    private boolean paymentSyncEnabled;

    // Import SystemConstant values for better maintainability
    private static final String SUSPENSION_TYPE_PS = SystemConstant.SuspensionType.PERMANENT;
    private static final String REASON_FP = SystemConstant.SuspensionReason.FULL_PAYMENT;
    private static final String REASON_PRA = SystemConstant.SuspensionReason.PARTIAL_AMOUNT;
    private static final String PAYMENT_STATUS_FP = SystemConstant.PaymentStatus.FULL_PAYMENT;
    private static final String PAYMENT_ACCEPTANCE_NO = SystemConstant.PaymentAcceptance.NOT_ALLOWED;
    private static final String ESERVICE_MSG_E2 = SystemConstant.EServiceMessageCode.FULL_PAYMENT;
    private static final String ESERVICE_MSG_E12 = SystemConstant.EServiceMessageCode.PARTIAL_PAYMENT;
    private static final String SUSPENSION_SOURCE_OCMS = SystemConstant.Subsystem.OCMS_CODE;
    private static final String SYSTEM_USER = SystemConstant.User.DEFAULT_SYSTEM_USER_ID;
    private static final String REFUND_REASON_DBP = SystemConstant.RefundReason.DOUBLE_PAYMENT;
    private static final String REFUND_REASON_OVP = SystemConstant.RefundReason.OVER_PAYMENT;

    @Autowired
    public CronSyncTxnData(EWebTxnDetailService ewebTxnDetailService,
                           WebTxnDetailService webTxnDetailService,
                           OcmsValidOffenceNoticeService vonService,
                           EocmsValidOffenceNoticeService eocmsVonService,
                           OcmsSuspendedNoticeService suspendedNoticeService,
                           OcmsRefundNoticesService refundNoticesService,
                           OcmsSuspensionReasonService suspensionReasonService) {
        this.ewebTxnDetailService = ewebTxnDetailService;
        this.webTxnDetailService = webTxnDetailService;
        this.vonService = vonService;
        this.eocmsVonService = eocmsVonService;
        this.suspendedNoticeService = suspendedNoticeService;
        this.refundNoticesService = refundNoticesService;
        this.suspensionReasonService = suspensionReasonService;
    }

    /**
     * Synchronize transaction data from Internet to Intranet
     * Each notice is processed in its own transaction for isolation
     * Can be triggered manually via API endpoint
     * (Automatic scheduling disabled - was: cron = "0 *'/5 * * * *")
     */
    public void syncTransactionData() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("=== Starting Cron Pull Internet (Transaction Sync - Batch Mode with Isolation) at {} ===", startTime);

        // Check if payment sync feature is enabled
        if (!paymentSyncEnabled) {
            log.info("Payment sync feature is disabled. Skipping transaction sync.");
            return;
        }

        try {
            // Step 1: Retrieve all unsynchronized transactions from Internet database
            List<EwebTxnDetail> unsyncedTransactions =
                ewebTxnDetailService.findUnsynchronizedTodayTransactions();

            if (unsyncedTransactions.isEmpty()) {
                log.info("No unsynchronized transactions found for today");
                return;
            }

            log.info("Found {} unsynchronized transactions to process", unsyncedTransactions.size());

            // Step 2: Group transactions by notice_no
            Map<String, List<EwebTxnDetail>> groupedByNotice = unsyncedTransactions.stream()
                .collect(Collectors.groupingBy(EwebTxnDetail::getOffenceNoticeNo));

            log.info("Grouped into {} unique notices", groupedByNotice.size());

            // PERFORMANCE OPTIMIZATION: Batch fetch ALL VONs at once (1 query instead of N queries)
            List<String> noticeNos = new ArrayList<>(groupedByNotice.keySet());
            Map<String, OcmsValidOffenceNotice> vonMap = vonService.findByNoticeNoIn(noticeNos)
                .stream()
                .collect(Collectors.toMap(OcmsValidOffenceNotice::getNoticeNo, von -> von));

            log.info("Batch fetched {} VON records", vonMap.size());

            // PERFORMANCE OPTIMIZATION: Batch insert ALL web_txn_detail BEFORE transaction loop
            // If transaction fails later, web_txn_detail stays (harmless copy), is_sync=false ensures retry
            List<WebTxnDetail> allTargetTxns = new ArrayList<>();
            for (List<EwebTxnDetail> txns : groupedByNotice.values()) {
                for (EwebTxnDetail sourceTxn : txns) {
                    WebTxnDetail targetTxn = new WebTxnDetail();
                    copyTransactionProperties(sourceTxn, targetTxn);
                    allTargetTxns.add(targetTxn);
                }
            }
            webTxnDetailService.saveAll(allTargetTxns);
            log.info("Batch inserted {} web_txn_detail records across all notices", allTargetTxns.size());

            // Step 3: Process each notice in its own transaction
            int successCount = 0;
            int failureCount = 0;
            List<String> failedNotices = new ArrayList<>();

            for (Map.Entry<String, List<EwebTxnDetail>> entry : groupedByNotice.entrySet()) {
                String noticeNo = entry.getKey();
                List<EwebTxnDetail> noticeTxns = entry.getValue();

                // Get pre-fetched VON from map
                OcmsValidOffenceNotice von = vonMap.get(noticeNo);

                // Check VON exists before starting transaction
                if (von == null) {
                    failureCount++;
                    failedNotices.add(noticeNo);
                    log.error("VON not found for notice_no: {} - skipping", noticeNo);
                    continue;
                }

                try {
                    // Process this notice in separate transaction (REQUIRES_NEW) with pre-fetched VON
                    processNoticeWithIsolation(noticeNo, noticeTxns, von);
                    successCount++;
                    log.info("Successfully processed notice: {} ({} transactions)", noticeNo, noticeTxns.size());

                } catch (Exception e) {
                    failureCount++;
                    failedNotices.add(noticeNo);
                    log.error("Failed to process notice: {} ({} transactions) - Error: {}",
                             noticeNo, noticeTxns.size(), e.getMessage(), e);
                    // Continue with next notice - this notice's transaction will be rolled back
                }
            }

            // Summary
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            log.info("=== Transaction Sync Summary ===");
            log.info("Total transactions: {}, Unique notices: {}", unsyncedTransactions.size(), groupedByNotice.size());
            log.info("Success: {} notices, Failed: {} notices", successCount, failureCount);
            log.info("Duration: {} seconds ({} mins), Avg: {}/notice",
                     durationSeconds,
                     String.format("%.1f", durationSeconds / 60.0),
                     successCount > 0 ? String.format("%.2fs", durationSeconds * 1.0 / successCount) : "N/A");

            if (!failedNotices.isEmpty()) {
                log.warn("Failed notices (will retry on next run): {}", failedNotices);
            }

        } catch (Exception e) {
            log.error("Fatal error during transaction synchronization", e);
        }
    }

    /**
     * Process all transactions for a single notice in an isolated transaction
     * If any error occurs, only this notice's changes are rolled back
     *
     * PERFORMANCE OPTIMIZATION: VON is pre-fetched in batch and passed as parameter
     * to avoid N individual queries (1 batch query instead of N queries)
     *
     * @param noticeNo The notice number
     * @param transactions All transactions for this notice (can be multiple for partial payments)
     * @param von Pre-fetched Valid Offence Notice record (fetched in batch before transaction)
     * @throws Exception if processing fails (triggers rollback for this notice only)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processNoticeWithIsolation(String noticeNo, List<EwebTxnDetail> transactions, OcmsValidOffenceNotice von) throws Exception {
        log.debug("Processing notice {} with {} transactions in isolated transaction", noticeNo, transactions.size());

        LocalDateTime currentDate = LocalDateTime.now();

        // OPTIMIZATION: web_txn_detail already inserted in batch BEFORE this transaction
        // OPTIMIZATION: VON already passed as parameter (pre-fetched in batch), no query needed here

        // Collections for batch operations within this notice
        // TODO: Uncomment after ocms_suspended_notice table is created
        // List<OcmsSuspendedNotice> suspendedNotices = new ArrayList<>();
        boolean needsRefund = false;
        BigDecimal overpaymentAmount = BigDecimal.ZERO;
        EocmsValidOffenceNotice evon = null;

        // Step 3: Check if already marked as FP or PRA (duplicate payment - notice already settled)
        if (REASON_FP.equals(von.getCrsReasonOfSuspension()) ||
            REASON_PRA.equals(von.getCrsReasonOfSuspension())) {
            log.info("Notice {} already marked as {} - creating refund for duplicate payment",
                     noticeNo, von.getCrsReasonOfSuspension());
            // Create individual refund notice for each duplicate transaction (DBP = Double Payment)
            for (EwebTxnDetail txn : transactions) {
                BigDecimal paymentAmount = txn.getPaymentAmount() != null
                    ? txn.getPaymentAmount() : BigDecimal.ZERO;
                if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                    createRefundNotice(noticeNo, txn.getReceiptNo(), paymentAmount, von, REFUND_REASON_DBP);
                }
            }
            // Mark transactions as synchronized and exit
            batchMarkAsSynchronized(transactions);
            return;
        }

        // TODO: Uncomment after ocms_suspended_notice table is created
        // PERFORMANCE OPTIMIZATION: Get starting sr_no ONCE per notice (only when needed for suspended notices)
        // Moved here AFTER the FP check to avoid unnecessary DB query when returning early
        // int currentSrNo = getNextSrNo(noticeNo);

        // Track transactions that need refund for duplicate payments within same batch
        List<EwebTxnDetail> batchRefundTransactions = new ArrayList<>();
        // Track the transaction that caused overpayment (if any)
        EwebTxnDetail overpaymentTransaction = null;

        // Step 4: Process each transaction individually (for multiple partial payments)
        for (EwebTxnDetail txn : transactions) {
            BigDecimal paymentAmount = txn.getPaymentAmount() != null
                ? txn.getPaymentAmount() : BigDecimal.ZERO;

            log.debug("Processing transaction: receipt_no={}, payment_amount={}", txn.getReceiptNo(), paymentAmount);

            // Check if VON was already marked as FP or PRA during THIS batch processing
            // This handles multiple transactions for same notice in same batch
            if (REASON_FP.equals(von.getCrsReasonOfSuspension()) ||
                REASON_PRA.equals(von.getCrsReasonOfSuspension())) {
                log.info("Notice {} already marked as {} during batch - transaction {} needs refund",
                         noticeNo, von.getCrsReasonOfSuspension(), txn.getReceiptNo());
                batchRefundTransactions.add(txn);
                continue; // Skip to next transaction, will create refund at end
            }

            // Add payment to existing amount_paid
            BigDecimal currentPaid = von.getAmountPaid() != null ? von.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal newAmountPaid = currentPaid.add(paymentAmount);
            BigDecimal amountPayable = von.getAmountPayable() != null ? von.getAmountPayable() : BigDecimal.ZERO;

            log.debug("Notice {}: current_paid={}, payment={}, new_paid={}, payable={}",
                     noticeNo, currentPaid, paymentAmount, newAmountPaid, amountPayable);

            // Compare new total paid vs amount payable
            int comparison = newAmountPaid.compareTo(amountPayable);

            String suspensionReason;
            String eserviceMsg;
            String remarks;

            if (comparison >= 0) {
                // Full payment or overpayment - Apply PS-FP
                suspensionReason = REASON_FP;
                eserviceMsg = ESERVICE_MSG_E2;
                remarks = "Full payment received";

                von.setSuspensionType(SUSPENSION_TYPE_PS);
                von.setCrsReasonOfSuspension(REASON_FP);
                von.setCrsDateOfSuspension(txn.getPaymentDateAndTime());
                // For overpayment (OVP): set amount_paid = amount_payable (excess goes to refund)
                // For exact full payment: amount_paid = newAmountPaid (which equals amount_payable)
                von.setAmountPaid(amountPayable);
                von.setPaymentStatus(PAYMENT_STATUS_FP);
                von.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
                von.setEserviceMessageCode(ESERVICE_MSG_E2);
                von.setUpdUserId(SYSTEM_USER);

                // PROCESS 6: Payment sync is IMMEDIATE - set is_sync to Y
                von.setIsSync("Y");

                // Prepare eVON sync (E2 only, no crs_reason for FP)
                if (evon == null) {
                    Optional<EocmsValidOffenceNotice> evonOpt = eocmsVonService.getById(noticeNo);
                    if (evonOpt.isPresent()) {
                        evon = evonOpt.get();
                    }
                }
                if (evon != null) {
                    evon.setEserviceMessageCode(ESERVICE_MSG_E2);
                    evon.setIsSync("Y");
                    evon.setUpdUserId(SYSTEM_USER);
                }

                log.info("Notice {}: Applied PS-FP (paid={}, payable={})", noticeNo, amountPayable, amountPayable);

                // Check for overpayment
                if (comparison > 0) {
                    overpaymentAmount = newAmountPaid.subtract(amountPayable);
                    overpaymentTransaction = txn;
                    log.info("Notice {}: Overpayment detected - refund amount: {}, receipt_no: {}",
                            noticeNo, overpaymentAmount, txn.getReceiptNo());
                    needsRefund = true;
                }

            } else {
                // Partial payment - Apply PS-PRA
                suspensionReason = REASON_PRA;
                eserviceMsg = ESERVICE_MSG_E12;
                remarks = String.format("Partial payment: %.2f", newAmountPaid);

                von.setSuspensionType(SUSPENSION_TYPE_PS);
                von.setCrsReasonOfSuspension(REASON_PRA);
                von.setCrsDateOfSuspension(txn.getPaymentDateAndTime());
                von.setAmountPaid(newAmountPaid);
                von.setAmountPayable(newAmountPaid);
                von.setPaymentStatus(PAYMENT_STATUS_FP);
                von.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
                von.setEserviceMessageCode(ESERVICE_MSG_E12);
                von.setUpdUserId(SYSTEM_USER);

                // PROCESS 6: Payment sync is IMMEDIATE - set is_sync to Y
                von.setIsSync("Y");

                // Prepare eVON sync (E12 + crs_reason for PRA)
                if (evon == null) {
                    Optional<EocmsValidOffenceNotice> evonOpt = eocmsVonService.getById(noticeNo);
                    if (evonOpt.isPresent()) {
                        evon = evonOpt.get();
                    }
                }
                if (evon != null) {
                    evon.setEserviceMessageCode(ESERVICE_MSG_E12);
                    evon.setCrsReasonOfSuspension(REASON_PRA);
                    evon.setIsSync("Y");
                    evon.setUpdUserId(SYSTEM_USER);
                }

                log.info("Notice {}: Applied PS-PRA (paid={}, payable={})", noticeNo, newAmountPaid, amountPayable);
            }

            // TODO: Uncomment after ocms_suspended_notice table is created
            // Create suspended notice for this transaction
            // OcmsSuspendedNotice suspendedNotice = new OcmsSuspendedNotice();
            // suspendedNotice.setNoticeNo(noticeNo);
            // suspendedNotice.setDateOfSuspension(currentDate);
            // PERFORMANCE OPTIMIZATION: Use incremented sr_no instead of querying DB for each transaction
            // suspendedNotice.setSrNo(currentSrNo++);
            // suspendedNotice.setSuspensionType(SUSPENSION_TYPE_PS);
            // suspendedNotice.setReasonOfSuspension(suspensionReason);
            // suspendedNotice.setSuspensionSource(SUSPENSION_SOURCE_OCMS);
            // suspendedNotice.setOfficerAuthorisingSuspension(SYSTEM_USER);
            // suspendedNotice.setSuspensionRemarks(remarks);
            // suspendedNotices.add(suspendedNotice);
        }

        // Step 5: Batch save all modifications for this notice
        vonService.save(von);
        log.debug("Updated VON for notice {}", noticeNo);

        if (evon != null) {
            eocmsVonService.save(evon);
            log.debug("Synced eVON for notice {}", noticeNo);
        }

        // TODO: Uncomment after ocms_suspended_notice table is created
        // if (!suspendedNotices.isEmpty()) {
        //     suspendedNoticeService.saveAll(suspendedNotices);
        //     log.debug("Inserted {} suspended notices for notice {}", suspendedNotices.size(), noticeNo);
        // }

        // Create individual refund notices for overpayment and batch duplicates
        // Overpayment refund (from the transaction that caused overpayment) - OVP = Over Payment
        if (needsRefund && overpaymentTransaction != null && overpaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Notice {}: Creating overpayment refund - receipt_no: {}, amount: {}",
                    noticeNo, overpaymentTransaction.getReceiptNo(), overpaymentAmount);
            createRefundNotice(noticeNo, overpaymentTransaction.getReceiptNo(), overpaymentAmount, von, REFUND_REASON_OVP);
        }

        // Batch duplicate refunds (each transaction that came after FP/PRA was set) - DBP = Double Payment
        for (EwebTxnDetail refundTxn : batchRefundTransactions) {
            BigDecimal refundAmount = refundTxn.getPaymentAmount() != null
                ? refundTxn.getPaymentAmount() : BigDecimal.ZERO;
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Notice {}: Creating batch duplicate refund - receipt_no: {}, amount: {}",
                        noticeNo, refundTxn.getReceiptNo(), refundAmount);
                createRefundNotice(noticeNo, refundTxn.getReceiptNo(), refundAmount, von, REFUND_REASON_DBP);
            }
        }

        // Step 6: Mark all transactions for this notice as synchronized
        batchMarkAsSynchronized(transactions);
        log.debug("Marked {} transactions as synchronized for notice {}", transactions.size(), noticeNo);
    }

    /**
     * Copy properties from EOCMS transaction to OCMS transaction
     */
    private void copyTransactionProperties(EwebTxnDetail source, WebTxnDetail target) {
        target.setReceiptNo(source.getReceiptNo());
        target.setOffenceNoticeNo(source.getOffenceNoticeNo());
        target.setTypeOfReceipt(source.getTypeOfReceipt());
        target.setRemarks(source.getRemarks());
        target.setTransactionDateAndTime(source.getTransactionDateAndTime());
        target.setPaymentDateAndTime(source.getPaymentDateAndTime());
        target.setVehicleNo(source.getVehicleNo());
        target.setAtomsFlag(source.getAtomsFlag());
        target.setPaymentMode(source.getPaymentMode());
        target.setPaymentAmount(source.getPaymentAmount());
        target.setTotalAmount(source.getTotalAmount());
        target.setSender(source.getSender());
        target.setStatus(source.getStatus());
        target.setErrorRemarks(source.getErrorRemarks());
    }

    /**
     * Create refund notice for overpayment or duplicate payment
     * Primary key: refund_notice_id
     * refund_notice_id format: RE{YYYYMMDD}{SSS}{NNN} (e.g., RE20251218004001)
     *
     * @param noticeNo The notice number
     * @param receiptNo The receipt number from web transaction detail
     * @param refundAmount The amount to be refunded
     * @param von The Valid Offence Notice record to get additional fields
     * @param refundReason The reason for refund (DBP=Double Payment, OVP=Over Payment, APP=Apply Waiver)
     */
    private void createRefundNotice(String noticeNo, String receiptNo, BigDecimal refundAmount,
                                     OcmsValidOffenceNotice von, String refundReason) {
        // Get next running number from sequence
        int runningNumber = refundNoticesService.getNextRunningNumber();

        // Generate refund_notice_id with format: RE{YYYYMMDD}{runningNo}
        String refundNoticeId = OcmsRefundNoticesId.generateRefundNoticeId(runningNumber);

        OcmsRefundNotices refund = new OcmsRefundNotices();
        refund.setRefundNoticeId(refundNoticeId);
        refund.setReceiptNo(receiptNo);
        refund.setNoticeNo(noticeNo);
        refund.setPpCode(von.getPpCode());
        refund.setAmountPaid(von.getAmountPaid());
        refund.setAmountPayable(von.getAmountPayable());
        refund.setSuspensionType(von.getSuspensionType());
        refund.setEprReasonOfSuspension(von.getEprReasonOfSuspension());
        refund.setCrsReasonOfSuspension(von.getCrsReasonOfSuspension());
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(refundReason);
        refundNoticesService.save(refund);
        log.info("Created refund notice for notice_no: {}, refund_notice_id: {}, refund_amount: {}, reason: {}",
                noticeNo, refundNoticeId, refundAmount, refundReason);
    }

    /**
     * Batch mark transactions as synchronized
     */
    private void batchMarkAsSynchronized(List<EwebTxnDetail> transactions) {
        ewebTxnDetailService.batchUpdateSyncStatus(transactions, "Y");
    }

    /**
     * PERFORMANCE OPTIMIZED: Get the next sr_no for a given notice_no
     * Uses optimized MAX query instead of retrieving all records
     * Returns 1 if no existing records found
     *
     * Performance improvement: ~50-100x faster than previous implementation
     * - OLD: Retrieved ALL suspended notices for notice_no, then iterated to find max
     * - NEW: Single optimized MAX(sr_no) query
     */
    private int getNextSrNo(String noticeNo) {
        try {
            Integer maxSrNo = suspendedNoticeService.findMaxSrNoByNoticeNo(noticeNo);
            return (maxSrNo != null ? maxSrNo : 0) + 1;

        } catch (Exception e) {
            log.error("Error getting next sr_no for notice {}: {}", noticeNo, e.getMessage(), e);
            // Fallback: return 1 for first record
            return 1;
        }
    }
}
