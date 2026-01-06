package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "eocms_request_driver_particulars", schema = "ocmsezmgr")
@Getter
@Setter
@IdClass(EocmsRequestDriverParticularsId.class)
public class EocmsRequestDriverParticulars extends BaseEntity {
    @Id
    @Column(name = "date_of_processing", nullable = false)
    private LocalDateTime dateOfProcessing;
    
    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;
    
    @Column(name = "date_of_rdp", nullable = false)
    private LocalDateTime dateOfRdp;
    
    @Column(name = "date_of_return")
    private LocalDateTime dateOfReturn;
    
    @Column(name = "owner_bldg", length = 65)
    private String ownerBldg;
    
    @Column(name = "owner_blk_hse_no", length = 10)
    private String ownerBlkHseNo;
    
    @Column(name = "owner_floor", length = 3)
    private String ownerFloor;
    
    @Column(name = "owner_id_type", length = 1, nullable = false)
    private String ownerIdType;
    
    @Column(name = "owner_name", length = 66)
    private String ownerName;
    
    @Column(name = "owner_nric_no", length = 20, nullable = false)
    private String ownerNricNo;
    
    @Column(name = "owner_postal_code", length = 6)
    private String ownerPostalCode;
    
    @Column(name = "owner_street", length = 32)
    private String ownerStreet;
    
    @Column(name = "owner_unit", length = 5)
    private String ownerUnit;
    
    @Column(name = "postal_regn_no", length = 15)
    private String postalRegnNo;
    
    @Column(name = "processing_stage", length = 3, nullable = false)
    private String processingStage;
    
    @Column(name = "reminder_flag", length = 1)
    private String reminderFlag;
    
    @Column(name = "unclaimed_reason", length = 3)
    private String unclaimedReason;
}
