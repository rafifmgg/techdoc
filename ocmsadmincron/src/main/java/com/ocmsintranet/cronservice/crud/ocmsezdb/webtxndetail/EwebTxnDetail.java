package com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the eocms_web_txn_detail table.
 * Stores information related to web transaction details in EOCMS.
 */
@Entity
@Table(name = "eocms_web_txn_detail", schema = "ocmsezmgr")
@IdClass(EwebTxnDetailId.class)
@Getter
@Setter
@NoArgsConstructor
public class EwebTxnDetail extends BaseEntity {

    @Id
    @Column(name = "receipt_no", nullable = false, length = 16)
    private String receiptNo;

    @Column(name = "type_of_receipt", length = 2)
    private String typeOfReceipt;

    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "transaction_date_and_time")
    private LocalDateTime transactionDateAndTime;

    @Id
    @Column(name = "offence_notice_no", nullable = false, length = 10)
    private String offenceNoticeNo;

    @Column(name = "vehicle_no", length = 14)
    private String vehicleNo;

    @Column(name = "atoms_flag", length = 1)
    private String atomsFlag;

    @Column(name = "payment_mode", length = 6)
    private String paymentMode;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_amount", precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "sender", length = 10)
    private String sender;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "error_remarks", length = 250)
    private String errorRemarks;

    @Column(name = "is_sync", nullable = false, length = 1)
    private String isSync = "N";

    @Column(name = "payment_date_and_time")
    private LocalDateTime paymentDateAndTime;
}
