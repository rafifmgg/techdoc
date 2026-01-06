package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocms_suspension_reason", schema = "ocmsizmgr")
@Data
public class OcmsSuspensionReason {
    
    @EmbeddedId
    private OcmsSuspensionReasonId id;
    
    @Column(name = "cre_date", nullable = false)
    private LocalDateTime creDate;
    
    @Column(name = "cre_user_id", length = 50, nullable = false)
    private String creUserId;
    
    @Column(name = "description", length = 200, nullable = false)
    private String description;
    
    @Column(name = "no_of_days_for_revival")
    private Integer noOfDaysForRevival;
    
    @Column(name = "status", length = 1, nullable = false)
    private String status = "A";
    
    @Column(name = "upd_date")
    private LocalDateTime updDate;
    
    @Column(name = "upd_user_id", length = 50)
    private String updUserId;
}
