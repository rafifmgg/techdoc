package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsFurnishApplicationDoc entity
 * Primary key: (txn_no, attachment_id)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsFurnishApplicationDocId implements Serializable {

    private String txnNo;
    private Integer attachmentId;
}
