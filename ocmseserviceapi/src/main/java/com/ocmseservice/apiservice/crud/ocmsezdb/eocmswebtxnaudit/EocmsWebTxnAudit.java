package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "eocms_web_txn_audit", schema = "ocmsezmgr")
@Getter
@Setter
public class EocmsWebTxnAudit extends BaseEntity {
    @Id
    @Column(name = "web_txn_id", length = 30, nullable = false)
    private String webTxnId;
    
    @Column(name = "msg_error", length = 250)
    private String msgError;
    //buat apa?
    @Column(name = "record_counter", nullable = false)
    private Integer recordCounter;
    
    @Column(name = "send_date", nullable = false)
    private LocalDateTime sendDate;
    
    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;
    
    @Column(name = "sender", length = 5, nullable = false)
    private String sender;
    
    //need check perlu hapus atau tidak
    @Column(name = "status_num", length = 1, nullable = false)
    private String statusNum;
    //buat apa?
    @Column(name = "target_receiver", length = 5, nullable = false)
    private String targetReceiver;
    
    @Column(name = "txn_detail", length = 4000, nullable = false)
    private String txnDetail;
}
