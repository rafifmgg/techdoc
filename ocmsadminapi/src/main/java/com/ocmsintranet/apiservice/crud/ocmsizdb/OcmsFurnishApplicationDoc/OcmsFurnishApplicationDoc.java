package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_furnish_application_doc table.
 * Stores supporting documents/attachments for furnish applications.
 * Multiple documents can be associated with a single furnish application (txn_no).
 *
 * Primary Key: (txn_no, attachment_id)
 */
@Entity
@Table(name = "ocms_furnish_application_doc", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsFurnishApplicationDocId.class)
public class OcmsFurnishApplicationDoc extends BaseEntity {

    @Id
    @Column(name = "txn_no", length = 20, nullable = false)
    private String txnNo;

    @Id
    @Column(name = "attachment_id", nullable = false)
    private Integer attachmentId;

    @Column(name = "doc_name", length = 2000, nullable = false)
    private String docName;

    @Column(name = "mime", length = 100)
    private String mime;

    @Column(name = "size")
    private Long size;
}
