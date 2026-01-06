package com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eocms_offence_notice_owner_driver", schema = "ocmspiiezmgr")
@Getter
@Setter
@IdClass(EocmsOffenceNoticeOwnerDriverId.class)
public class EocmsOffenceNoticeOwnerDriver extends BaseEntity {
    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;
    
    @Id
    @Column(name = "owner_driver_indicator", length = 1, nullable = false)
    private String ownerDriverIndicator;
    
    @Column(name = "id_no", length = 20, nullable = false)
    private String idNo;
    
    @Column(name = "offender_indicator", length = 1)
    private String offenderIndicator;
    
    @Column(name = "email_addr", length = 320)
    private String emailAddr;
}
