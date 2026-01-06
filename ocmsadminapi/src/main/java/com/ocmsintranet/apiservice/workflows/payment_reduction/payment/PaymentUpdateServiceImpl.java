package com.ocmsintranet.apiservice.workflows.payment_reduction.payment;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail.OcmsWebTransactionDetail;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail.OcmsWebTransactionDetailService;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRefundNotice.OcmsRefundNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.sequence.SequenceService;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice.EocmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto.PaymentUpdateRequest;
import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto.PaymentValidationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service implementation for payment update operations
 *
 * Flow:
 * 1. Insert web_txn_detail first
 * 2. Validate payment using standalone PaymentValidator
 * 3. Apply suspension logic based on validation result
 */
@Service
@Slf4j
public class PaymentUpdateServiceImpl implements PaymentUpdateService {

    private final OcmsValidOffenceNoticeService vonService;
    private final EocmsValidOffenceNoticeService evonService;
    private final SuspendedNoticeService suspendedNoticeService;
    private final SuspendedNoticeRepository suspendedNoticeRepository;
    private final OcmsWebTransactionDetailService webTxnDetailService;
    private final PaymentValidator paymentValidator;
    private final OcmsRefundNoticeService refundNoticeService;
    private final SequenceService sequenceService;

    // Constants for payment update
    private static final String SUSPENSION_TYPE_PS = SystemConstant.SuspensionType.PERMANENT;
    private static final String REASON_FP = SystemConstant.SuspensionReason.FULL_PAYMENT;
    private static final String REASON_PRA = SystemConstant.SuspensionReason.PARTIAL_AMOUNT;
    private static final String PAYMENT_STATUS_FP = SystemConstant.PaymentStatus.FULL_PAYMENT;
    private static final String SUSPENSION_SOURCE_OCMS = SystemConstant.Subsystem.OCMS_NAME;
    private static final String SENDER_OCMS = SystemConstant.Subsystem.OCMS_NAME;
    private static final String PAYMENT_ACCEPTANCE_NO = SystemConstant.PaymentAcceptance.NOT_ALLOWED;
    private static final String ESERVICE_MSG_E2 = SystemConstant.EServiceMessageCode.FULL_PAYMENT;
    private static final String ESERVICE_MSG_E12 = SystemConstant.EServiceMessageCode.PARTIAL_PAYMENT;

    public PaymentUpdateServiceImpl(
            OcmsValidOffenceNoticeService vonService,
            EocmsValidOffenceNoticeService evonService,
            SuspendedNoticeService suspendedNoticeService,
            SuspendedNoticeRepository suspendedNoticeRepository,
            OcmsWebTransactionDetailService webTxnDetailService,
            PaymentValidator paymentValidator,
            OcmsRefundNoticeService refundNoticeService,
            SequenceService sequenceService) {
        this.vonService = vonService;
        this.evonService = evonService;
        this.suspendedNoticeService = suspendedNoticeService;
        this.suspendedNoticeRepository = suspendedNoticeRepository;
        this.webTxnDetailService = webTxnDetailService;
        this.paymentValidator = paymentValidator;
        this.refundNoticeService = refundNoticeService;
        this.sequenceService = sequenceService;
    }

    @Override
    @Transactional
    public void processPaymentUpdate(PaymentUpdateRequest request) {
        log.info("Processing payment update for notice: {}", request.getNoticeNo());

        LocalDateTime currentDateTime = LocalDateTime.now();

        try {
            // Step 1: Get VON record first (needed for web_txn_detail)
            OcmsValidOffenceNotice von = getVON(request.getNoticeNo());

            // Step 2: Insert web_txn_detail FIRST
            OcmsWebTransactionDetail webTxnDetail = createWebTransactionDetail(request, von, currentDateTime);
            log.info("Web transaction detail created: receiptNo={}", webTxnDetail.getReceiptNo());

            // Step 3: Validate payment using standalone validator
            PaymentValidationContext validationContext = paymentValidator.validatePayment(webTxnDetail, von);
            log.info("Payment validation result for notice {}: {}", request.getNoticeNo(), validationContext.getResult());

            // Step 4: Process based on validation result
            processValidationResult(request, von, validationContext, currentDateTime, webTxnDetail.getReceiptNo());

            log.info("Payment update completed successfully for notice: {}", request.getNoticeNo());

        } catch (Exception e) {
            log.error("Error processing payment update for notice: {}", request.getNoticeNo(), e);
            throw new RuntimeException("Failed to process payment update: " + e.getMessage(), e);
        }
    }

    /**
     * Get VON record
     */
    private OcmsValidOffenceNotice getVON(String noticeNo) {
        Optional<OcmsValidOffenceNotice> vonOptional = vonService.getById(noticeNo);
        if (vonOptional.isEmpty()) {
            throw new RuntimeException("Notice not found: " + noticeNo);
        }
        return vonOptional.get();
    }

    /**
     * Create web transaction detail record FIRST
     * @return The created web transaction detail
     */
    private OcmsWebTransactionDetail createWebTransactionDetail(PaymentUpdateRequest request, OcmsValidOffenceNotice von, LocalDateTime currentDateTime) {
        String receiptNo = TransactionIdGenerator.generate(request.getPaymentMode(), sequenceService);

        OcmsWebTransactionDetail webTxnDetail = new OcmsWebTransactionDetail();
        webTxnDetail.setReceiptNo(receiptNo);
        webTxnDetail.setOffenceNoticeNo(request.getNoticeNo());
        webTxnDetail.setTypeOfReceipt("PF");
        webTxnDetail.setRemarks(request.getRemarks());
        webTxnDetail.setTransactionDateAndTime(request.getTransactionDateAndTime());
        webTxnDetail.setPaymentDateAndTime(currentDateTime);
        webTxnDetail.setVehicleNo(von.getVehicleNo());
        webTxnDetail.setAtomsFlag("N");
        webTxnDetail.setPaymentMode(request.getPaymentMode());
        webTxnDetail.setPaymentAmount(request.getAmountPaid());
        webTxnDetail.setTotalAmount(request.getAmountPaid());
        webTxnDetail.setSender(SENDER_OCMS);
        webTxnDetail.setStatus("S");
        webTxnDetail.setErrorRemarks(null);

        webTxnDetailService.save(webTxnDetail);
        return webTxnDetail;
    }

    /**
     * Inner class for generating transaction IDs (receipt numbers).
     *
     * Format: [PREFIX] + [9-digit sequence] + [checksum digit]
     *
     * Prefix mapping based on payment method:
     * - CASH   -> CASH
     * - CHEQUE -> CHQ
     * - Default -> TXN
     *
     * Checksum calculation (weighted sum modulo 10):
     * Checksum = [(1×d1) + (2×d2) + (3×d3) + (4×d4) + (10×d5) + (9×d6) + (8×d7) + (7×d8) + (6×d9)] % 10
     */
    static class TransactionIdGenerator {

        private static final int[] WEIGHTS = {1, 2, 3, 4, 10, 9, 8, 7, 6};

        /**
         * Generates a transaction ID based on payment method.
         *
         * @param paymentMethod The payment method (e.g., CASH, CHEQUE, OTHER)
         * @param sequenceService The sequence service to retrieve the next sequence number
         * @return The generated transaction ID
         */
        public static String generate(String paymentMethod, SequenceService sequenceService) {
            String prefix = getPrefix(paymentMethod);
            Integer sequenceNumber = sequenceService.getNextTxnRefNumber();
            String paddedSequence = String.format("%09d", sequenceNumber);
            int checksum = calculateChecksum(paddedSequence);

            return prefix + paddedSequence + checksum;
        }

        /**
         * Gets the prefix based on payment method.
         *
         * @param paymentMethod The payment method
         * @return The corresponding prefix
         */
        private static String getPrefix(String paymentMethod) {
            if (paymentMethod == null) {
                return "TXN";
            }

            switch (paymentMethod.toUpperCase()) {
                case "CASH":
                    return "CASH";
                case "CHEQUE":
                    return "CHQ";
                default:
                    return "TXN";
            }
        }

        /**
         * Calculates the checksum digit using weighted sum modulo 10.
         * Formula: [(1×d1) + (2×d2) + (3×d3) + (4×d4) + (10×d5) + (9×d6) + (8×d7) + (7×d8) + (6×d9)] % 10
         *
         * @param paddedSequence The 9-digit padded sequence number
         * @return The checksum digit (0-9)
         */
        private static int calculateChecksum(String paddedSequence) {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                int digit = Character.getNumericValue(paddedSequence.charAt(i));
                sum += WEIGHTS[i] * digit;
            }
            return sum % 10;
        }
    }

    /**
     * Process payment based on validation result
     * This method applies the appropriate suspension logic
     */
    private void processValidationResult(PaymentUpdateRequest request, OcmsValidOffenceNotice von,
                                         PaymentValidationContext context, LocalDateTime currentDateTime,
                                         String receiptNo) {
        String noticeNo = request.getNoticeNo();

        switch (context.getResult()) {
            case DOUBLE_PAYMENT:
                handleDoublePayment(noticeNo, context, receiptNo, von);
                break;

            case FULL_PAYMENT:
                handleFullPayment(request, von, context, currentDateTime);
                break;

            case PARTIAL_PAYMENT:
                handlePartialPayment(request, von, context, currentDateTime);
                break;

            case OVER_PAYMENT:
                handleOverPayment(request, von, context, currentDateTime, receiptNo);
                break;

            default:
                throw new RuntimeException("Unknown validation result: " + context.getResult());
        }
    }

    /**
     * Handle double payment - notice already fully paid
     */
    private void handleDoublePayment(String noticeNo, PaymentValidationContext context,
                                      String receiptNo, OcmsValidOffenceNotice von) {
        log.warn("Double payment detected for notice {}. Amount={}, creating refund notice.",
                noticeNo, context.getAmountPaid());

        // Create refund notice for the full payment amount (entire amount is refundable)
        boolean created = refundNoticeService.createIfNotExists(receiptNo, noticeNo,
                context.getAmountPaid(), context.getAmountPaid(), von);
        if (created) {
            log.info("Refund notice created for double payment on notice {}: refundAmount={}",
                    noticeNo, context.getAmountPaid());
        } else {
            log.info("Refund notice already exists for receipt {}", receiptNo);
        }
    }

    /**
     * Handle full payment - exact amount paid
     */
    private void handleFullPayment(PaymentUpdateRequest request, OcmsValidOffenceNotice von,
                                   PaymentValidationContext context, LocalDateTime currentDateTime) {
        String noticeNo = request.getNoticeNo();

        // Update VON with full payment suspension
        updateVONForFullPayment(von, context, currentDateTime);
        vonService.save(von);
        log.info("VON updated with PS-FP for notice: {}", noticeNo);

        // Update eVON
        updateEVONForFullPayment(noticeNo, currentDateTime);

        // Create suspended notice
        createSuspendedNotice(noticeNo, REASON_FP, "Full payment received", currentDateTime);
        log.info("Suspended notice created for notice: {}", noticeNo);
    }

    /**
     * Handle partial payment - less than amount payable
     */
    private void handlePartialPayment(PaymentUpdateRequest request, OcmsValidOffenceNotice von,
                                      PaymentValidationContext context, LocalDateTime currentDateTime) {
        String noticeNo = request.getNoticeNo();

        // Update VON with partial payment suspension
        updateVONForPartialPayment(von, context, currentDateTime);
        vonService.save(von);
        log.info("VON updated with PS-PRA for notice: {}", noticeNo);

        // Update eVON
        updateEVONForPartialPayment(noticeNo, context, currentDateTime);

        // Create suspended notice
        String remarks = String.format("Partial payment: %.2f of %.2f",
                context.getNewTotalPaid(), context.getAmountPayable());
        createSuspendedNotice(noticeNo, REASON_PRA, remarks, currentDateTime);
        log.info("Suspended notice created for notice: {}", noticeNo);
    }

    /**
     * Handle over payment - more than amount payable
     */
    private void handleOverPayment(PaymentUpdateRequest request, OcmsValidOffenceNotice von,
                                   PaymentValidationContext context, LocalDateTime currentDateTime,
                                   String receiptNo) {
        String noticeNo = request.getNoticeNo();

        // Update VON with full payment suspension (overpayment counts as full)
        updateVONForFullPayment(von, context, currentDateTime);
        vonService.save(von);
        log.info("VON updated with PS-FP (overpayment) for notice: {}", noticeNo);

        // Update eVON
        updateEVONForFullPayment(noticeNo, currentDateTime);

        // Create suspended notice
        String remarks = String.format("Full payment received (overpayment: %.2f)", context.getOverpaymentAmount());
        createSuspendedNotice(noticeNo, REASON_FP, remarks, currentDateTime);
        log.info("Suspended notice created for notice: {}", noticeNo);

        // Create refund notice for the overpayment amount
        boolean created = refundNoticeService.createIfNotExists(receiptNo, noticeNo,
                context.getOverpaymentAmount(), context.getAmountPaid(), von);
        if (created) {
            log.info("Refund notice created for overpayment on notice {}: refundAmount={}",
                    noticeNo, context.getOverpaymentAmount());
        } else {
            log.info("Refund notice already exists for receipt {}", receiptNo);
        }
    }

    /**
     * Update VON for full payment (FP or overpayment)
     */
    private void updateVONForFullPayment(OcmsValidOffenceNotice von, PaymentValidationContext context,
                                         LocalDateTime currentDateTime) {
        von.setSuspensionType(SUSPENSION_TYPE_PS);
        von.setCrsReasonOfSuspension(REASON_FP);
        von.setCrsDateOfSuspension(currentDateTime);
        von.setAmountPaid(context.getNewTotalPaid());
        von.setPaymentStatus(PAYMENT_STATUS_FP);
        von.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
        von.setEserviceMessageCode(ESERVICE_MSG_E2);
    }

    /**
     * Update VON for partial payment
     */
    private void updateVONForPartialPayment(OcmsValidOffenceNotice von, PaymentValidationContext context,
                                            LocalDateTime currentDateTime) {
        von.setSuspensionType(SUSPENSION_TYPE_PS);
        von.setCrsReasonOfSuspension(REASON_PRA);
        von.setCrsDateOfSuspension(currentDateTime);
        von.setAmountPaid(context.getNewTotalPaid());
        von.setPaymentStatus(PAYMENT_STATUS_FP);
        von.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
        von.setEserviceMessageCode(ESERVICE_MSG_E12);
    }

    /**
     * Update eVON for full payment
     */
    private void updateEVONForFullPayment(String noticeNo, LocalDateTime currentDateTime) {
        Optional<EocmsValidOffenceNotice> evonOptional = evonService.getById(noticeNo);
        if (evonOptional.isEmpty()) {
            log.warn("eVON not found for notice: {}. Skipping eVON update.", noticeNo);
            return;
        }

        EocmsValidOffenceNotice evon = evonOptional.get();
        evon.setSuspensionType(SUSPENSION_TYPE_PS);
        evon.setCrsReasonOfSuspension(REASON_FP);
        evon.setCrsDateOfSuspension(currentDateTime);
        evon.setPaymentStatus(PAYMENT_STATUS_FP);
        evon.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
        evon.setEserviceMessageCode(ESERVICE_MSG_E2);
        evonService.save(evon);
        log.info("eVON updated with PS-FP for notice: {}", noticeNo);
    }

    /**
     * Update eVON for partial payment
     */
    private void updateEVONForPartialPayment(String noticeNo, PaymentValidationContext context,
                                             LocalDateTime currentDateTime) {
        Optional<EocmsValidOffenceNotice> evonOptional = evonService.getById(noticeNo);
        if (evonOptional.isEmpty()) {
            log.warn("eVON not found for notice: {}. Skipping eVON update.", noticeNo);
            return;
        }

        EocmsValidOffenceNotice evon = evonOptional.get();
        evon.setSuspensionType(SUSPENSION_TYPE_PS);
        evon.setCrsReasonOfSuspension(REASON_PRA);
        evon.setCrsDateOfSuspension(currentDateTime);
        evon.setPaymentStatus(PAYMENT_STATUS_FP);
        evon.setPaymentAcceptanceAllowed(PAYMENT_ACCEPTANCE_NO);
        evon.setEserviceMessageCode(ESERVICE_MSG_E12);
        evonService.save(evon);
        log.info("eVON updated with PS-PRA for notice: {}", noticeNo);
    }

    /**
     * Create suspended notice record
     */
    private void createSuspendedNotice(String noticeNo, String reason, String remarks,
                                       LocalDateTime currentDateTime) {
        Integer maxSrNo = suspendedNoticeRepository.findMaxSrNoByNoticeNo(noticeNo);
        Integer nextSrNo = (maxSrNo == null ? 0 : maxSrNo) + 1;

        SuspendedNotice suspendedNotice = SuspendedNotice.builder()
                .noticeNo(noticeNo)
                .dateOfSuspension(currentDateTime)
                .srNo(nextSrNo)
                .suspensionType(SUSPENSION_TYPE_PS)
                .reasonOfSuspension(reason)
                .suspensionRemarks(remarks)
                .suspensionSource(SUSPENSION_SOURCE_OCMS)
                .officerAuthorisingSupension(SystemConstant.User.DEFAULT_SYSTEM_USER_ID)
                .build();

        suspendedNoticeService.save(suspendedNotice);
    }
}
