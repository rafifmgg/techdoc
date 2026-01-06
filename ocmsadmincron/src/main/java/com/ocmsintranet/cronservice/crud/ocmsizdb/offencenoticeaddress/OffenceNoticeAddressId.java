package com.ocmsintranet.cronservice.crud.ocmsizdb.offencenoticeaddress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for OffenceNoticeAddress entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffenceNoticeAddressId implements Serializable {
    private String noticeNo;
    private String ownerDriverIndicator;
    private String typeOfAddress;
}
