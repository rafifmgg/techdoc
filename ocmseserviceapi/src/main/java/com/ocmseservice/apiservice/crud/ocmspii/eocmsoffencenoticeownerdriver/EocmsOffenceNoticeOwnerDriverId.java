package com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsOffenceNoticeOwnerDriverId implements Serializable {
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;
    
    @Column(name = "owner_driver_indicator", length = 1, nullable = false)
    private String ownerDriverIndicator;
}
