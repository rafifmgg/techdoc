package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite ID class for EocmsOffenceNoticeOwnerDriver entity
 * Represents the composite primary key: (notice_no, owner_driver_indicator)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EocmsOffenceNoticeOwnerDriverId implements Serializable {

    private String noticeNo;
    private String ownerDriverIndicator;
}
