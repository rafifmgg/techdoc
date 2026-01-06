package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionAudit;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ocms_web_txn_audit", schema = "ocmsizmgr")
public class OcmsWebTransactionAudit extends BaseEntity {

    @Id
    @Column(name = "web_txn_id", nullable = false, length = 30)
    private String webTxnId;
    
    @Column(name = "sender", nullable = false, length = 5)
    private String sender;
    
    @Column(name = "target_receiver", nullable = false, length = 5)
    private String targetReceiver;

    @Column(name = "msg_error", length = 250)
    private String msgError;

    @Column(name = "record_counter")
    private Integer recordCounter;

    @Column(name = "send_date")
    private LocalDate sendDate;

    @Column(name = "send_time")
    private LocalTime sendTime;

    @Column(name = "status_num", length = 1)
    private String statusNum;

    @Column(name = "txn_detail", length = 4000)
    private String txnDetail;

    @Column(name = "service_code", length = 50)
    private String serviceCode;
}