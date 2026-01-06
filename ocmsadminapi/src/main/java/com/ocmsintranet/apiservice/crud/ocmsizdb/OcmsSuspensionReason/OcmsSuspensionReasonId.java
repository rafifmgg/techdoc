package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;

@Embeddable
@Data
public class OcmsSuspensionReasonId implements Serializable {
    
    @Column(name = "suspension_type", length = 2, nullable = false)
    private String suspensionType;
    
    @Column(name = "reason_of_suspension", length = 3, nullable = false)
    private String reasonOfSuspension;
}
