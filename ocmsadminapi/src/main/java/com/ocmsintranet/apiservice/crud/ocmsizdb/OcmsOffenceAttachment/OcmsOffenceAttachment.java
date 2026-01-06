package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_offence_attachment table.
 * This table stores offence-related attachments in the OCMS system including metadata
 * about attached files such as file names, categories, sizes, MIME types, and their
 * relationship to an offence notice or vehicle.
 * 
 * Note: The file_name column stores both the name of the file and the path to Azure Blob storage.
 */
@Entity
@Table(name = "ocms_offence_attachment", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsOffenceAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ocms_offence_attachment")
    @SequenceGenerator(name = "seq_ocms_offence_attachment", sequenceName = "seq_ocms_offence_attachment", schema = "ocmsizmgr", allocationSize = 1)
    @Column(name = "attachment_id", nullable = false)
    private Integer attachmentId;

    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    @Column(name = "file_name", nullable = false, length = 2000)
    private String fileName;

    @Column(name = "mime", length = 100)
    private String mime;

    @Column(name = "size")
    private Long size;
}

