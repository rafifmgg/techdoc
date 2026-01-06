package com.ocmsintranet.cronservice.crud.ocmsezdb.furnishapplicationdoc;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the eocms_furnish_application_doc table (Internet DB).
 * Stores supporting documents/attachments for furnish applications.
 * Multiple documents can be associated with a single furnish application (txn_no).
 *
 * Primary Key: (txn_no, attachment_id)
 *
 * Note: Actual files are stored in Azure Blob Storage (Internet container).
 * This table only stores metadata (filename, size, MIME type).
 */
@Entity
@Table(name = "eocms_furnish_application_doc", schema = "ocmsezmgr")
@Getter
@Setter
@NoArgsConstructor
@IdClass(EocmsFurnishApplicationDocId.class)
public class EocmsFurnishApplicationDoc extends BaseEntity {

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

    @Column(name = "is_sync", length = 1)
    private String isSync = "N";
}
