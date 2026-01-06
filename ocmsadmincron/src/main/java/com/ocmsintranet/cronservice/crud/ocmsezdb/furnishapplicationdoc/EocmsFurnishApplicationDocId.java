package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for EocmsFurnishApplicationDoc entity
 * Primary key: (txn_no, attachment_id)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsFurnishApplicationDocId implements Serializable {

    private String txnNo;
    private Integer attachmentId;
}
