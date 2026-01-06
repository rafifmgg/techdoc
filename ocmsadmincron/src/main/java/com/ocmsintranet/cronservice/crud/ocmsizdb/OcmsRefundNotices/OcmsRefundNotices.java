package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRefundNotices;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing the ocms_refund_notice table.
 * Stores refund transactions raised against paid notices
 * (overpayment or duplicate payment scenarios).
 *
 * Primary key: refund_notice_id
 * refund_notice_id format: RE{receipt_no} (e.g., RE123456789)
 */
@Entity
@Table(name = "ocms_refund_notice", schema = "ocmsizmgr")
@Getter
@Setter
@NoArgsConstructor
public class OcmsRefundNotices extends BaseEntity {

    @Id
    @Column(name = "refund_notice_id", length = 16, nullable = false)
    private String refundNoticeId;

    @Column(name = "receipt_no", length = 16, nullable = false)
    private String receiptNo;

    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;

    @Column(name = "amount_paid", precision = 19, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "amount_payable", precision = 19, scale = 2, nullable = false)
    private BigDecimal amountPayable;

    @Column(name = "suspension_type", length = 2, nullable = false)
    private String suspensionType;

    @Column(name = "epr_reason_of_suspension", length = 3)
    private String eprReasonOfSuspension;

    @Column(name = "crs_reason_of_suspension", length = 3)
    private String crsReasonOfSuspension;

    @Column(name = "refund_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 3, nullable = false)
    private String refundReason;

    // All audit fields (cre_date, cre_user_id, upd_date, upd_user_id)
    // are inherited from BaseEntity
}
