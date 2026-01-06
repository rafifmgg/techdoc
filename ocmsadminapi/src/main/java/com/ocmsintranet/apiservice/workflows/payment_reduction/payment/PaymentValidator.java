package com.ocmsintranet.apiservice.workflows.payment_reduction.payment;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionDetail.OcmsWebTransactionDetail;
import com.ocmsintranet.apiservice.workflows.payment_reduction.payment.dto.PaymentValidationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Standalone payment validation component.
 *
 * This component checks payment logic and determines the payment status:
 * - DOUBLE_PAYMENT: Notice already fully paid
 * - FULL_PAYMENT: Payment amount equals amount payable
 * - PARTIAL_PAYMENT: Payment amount less than amount payable
 * - OVER_PAYMENT: Payment amount exceeds amount payable
 *
 * STANDALONE DESIGN: This validator is decoupled from the main payment flow.
 * To disable validation, simply skip calling this validator in PaymentUpdateServiceImpl.
 */
@Slf4j
@Component
public class PaymentValidator {

    private static final String REASON_FP = SystemConstant.SuspensionReason.FULL_PAYMENT;

    /**
     * Validate payment based on web transaction detail and VON record.
     *
     * @param webTxnDetail The web transaction detail (already inserted)
     * @param von The valid offence notice record
     * @return PaymentValidationContext containing the result and amounts
     */
    public PaymentValidationContext validatePayment(OcmsWebTransactionDetail webTxnDetail, OcmsValidOffenceNotice von) {
        String noticeNo = webTxnDetail.getOffenceNoticeNo();

        // Get amounts
        BigDecimal amountPaid = webTxnDetail.getPaymentAmount() != null ? webTxnDetail.getPaymentAmount() : BigDecimal.ZERO;
        BigDecimal amountPayable = von.getAmountPayable() != null ? von.getAmountPayable() : BigDecimal.ZERO;
        BigDecimal currentAmountPaid = von.getAmountPaid() != null ? von.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newTotalPaid = currentAmountPaid.add(amountPaid);

        log.info("Validating payment for notice {}: paid={}, payable={}, currentPaid={}, newTotal={}",
                noticeNo, amountPaid, amountPayable, currentAmountPaid, newTotalPaid);

        // Check 1: Double payment - notice already marked as FP
        if (isAlreadyFullyPaid(von)) {
            log.warn("Notice {} already fully paid - double payment detected", noticeNo);
            return PaymentValidationContext.builder()
                    .result(PaymentValidationResult.DOUBLE_PAYMENT)
                    .amountPaid(amountPaid)
                    .amountPayable(amountPayable)
                    .currentAmountPaid(currentAmountPaid)
                    .newTotalPaid(newTotalPaid)
                    .overpaymentAmount(amountPaid) // Full payment amount is overpayment
                    .build();
        }

        // Check 2: Compare new total paid vs amount payable
        int comparison = newTotalPaid.compareTo(amountPayable);

        if (comparison > 0) {
            // Over payment
            BigDecimal overpayment = newTotalPaid.subtract(amountPayable);
            log.info("Notice {}: Over payment detected - overpayment amount={}", noticeNo, overpayment);
            return PaymentValidationContext.builder()
                    .result(PaymentValidationResult.OVER_PAYMENT)
                    .amountPaid(amountPaid)
                    .amountPayable(amountPayable)
                    .currentAmountPaid(currentAmountPaid)
                    .newTotalPaid(newTotalPaid)
                    .overpaymentAmount(overpayment)
                    .build();

        } else if (comparison == 0) {
            // Full payment (exact)
            log.info("Notice {}: Full payment - exact amount", noticeNo);
            return PaymentValidationContext.builder()
                    .result(PaymentValidationResult.FULL_PAYMENT)
                    .amountPaid(amountPaid)
                    .amountPayable(amountPayable)
                    .currentAmountPaid(currentAmountPaid)
                    .newTotalPaid(newTotalPaid)
                    .overpaymentAmount(BigDecimal.ZERO)
                    .build();

        } else {
            // Partial payment
            log.info("Notice {}: Partial payment - remaining={}", noticeNo, amountPayable.subtract(newTotalPaid));
            return PaymentValidationContext.builder()
                    .result(PaymentValidationResult.PARTIAL_PAYMENT)
                    .amountPaid(amountPaid)
                    .amountPayable(amountPayable)
                    .currentAmountPaid(currentAmountPaid)
                    .newTotalPaid(newTotalPaid)
                    .overpaymentAmount(BigDecimal.ZERO)
                    .build();
        }
    }

    /**
     * Check if notice is already fully paid
     */
    private boolean isAlreadyFullyPaid(OcmsValidOffenceNotice von) {
        return REASON_FP.equals(von.getCrsReasonOfSuspension());
    }
}
