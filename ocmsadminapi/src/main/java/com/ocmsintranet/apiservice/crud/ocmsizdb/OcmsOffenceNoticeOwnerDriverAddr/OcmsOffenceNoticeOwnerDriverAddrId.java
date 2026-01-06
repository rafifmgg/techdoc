package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for OcmsOffenceNoticeOwnerDriverAddr entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsOffenceNoticeOwnerDriverAddrId implements Serializable {
    private String noticeNo;
    private String ownerDriverIndicator;
    private String typeOfAddress;
}
