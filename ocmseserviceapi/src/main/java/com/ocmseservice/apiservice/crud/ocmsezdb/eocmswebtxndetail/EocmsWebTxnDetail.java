package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "eocms_web_txn_detail", schema = "ocmsezmgr")
@IdClass(EocmsWebTxnDetailId.class)
@Getter
@Setter
public class EocmsWebTxnDetail extends BaseEntity {

    @Id
    @Column(name = "receipt_no", length = 16, nullable = false)
    private String receiptNo;

    @Id
    @Column(name = "offence_notice_no", length = 10)
    private String offenceNoticeNo;
    
    @Column(name = "type_of_receipt", length = 2)
    private String typeOfReceipt;
    
    @Column(name = "remarks", length = 100)
    private String remarks;
    
    @Column(name = "transaction_date_and_time")
    private LocalDateTime transactionDateAndTime;

    @Column(name = "vehicle_no", length = 12)
    private String vehicleNo;
    
    @Column(name = "atoms_flag", length = 1)
    private String atomsFlag;
    
    @Column(name = "payment_mode", length = 6)
    private String paymentMode;
    
    @Column(name = "total_amount", length = 10)
    private String totalAmount;
    
    @Column(name = "payment_amount", length = 10)
    private String paymentAmount;
    
    @Column(name = "sender", length = 10)
    private String sender;
    
    @Column(name = "status", length = 1)
    private String status;//sync hanya yang success

    @Column(name = "error_remarks", length = 50)
    private String errorRemarks;
    
    @Column(name = "is_sync", nullable = false)
    private Boolean isSync;

    @Column(name = "merchant_ref_no",length = 16, nullable = false)
    private String merchantRefNo;

}
