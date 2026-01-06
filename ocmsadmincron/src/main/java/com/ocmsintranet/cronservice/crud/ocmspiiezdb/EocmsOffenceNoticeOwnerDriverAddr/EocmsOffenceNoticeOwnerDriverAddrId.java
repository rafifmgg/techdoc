package com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for EocmsOffenceNoticeOwnerDriverAddr entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsOffenceNoticeOwnerDriverAddrId implements Serializable {

    private String noticeNo;
    private String ownerDriverIndicator;
    private String typeOfAddress;
}