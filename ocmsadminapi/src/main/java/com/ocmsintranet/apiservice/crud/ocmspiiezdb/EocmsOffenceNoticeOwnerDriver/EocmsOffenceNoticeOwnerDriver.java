package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the eocms_offence_notice_owner_driver table in PII database.
 * This table stores encrypted personal information about the owners or drivers associated with an offence notice.
 *
 * Primary Key: notice_no
 *
 * All PII fields are encrypted using SQL Server column-level encryption.
 */
@Entity
@Table(name = "eocms_offence_notice_owner_driver", schema = "ocmspiiezmgr")
@IdClass(EocmsOffenceNoticeOwnerDriverId.class)
@Getter
@Setter
public class EocmsOffenceNoticeOwnerDriver extends BaseEntity {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    @Id
    @Column(name = "owner_driver_indicator", nullable = false, length = 1)
    private String ownerDriverIndicator;

    // Encrypted ID Number (NRIC/FIN)
    @Column(name = "id_no", nullable = false, length = 12, columnDefinition = "varbinary(max)")
    @NotBlank(message = "ID number is required")
    @ColumnTransformer(
        read = "CONVERT(varchar(12), DecryptByKey(id_no))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(12), ?))"
    )
    private String idNo;

    @Column(name = "offender_indicator", length = 1)
    private String offenderIndicator;

    // Encrypted Name
    @Column(name = "name", length = 66, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(66), DecryptByKey(name))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(66), ?))"
    )
    private String name;

    @Column(name = "id_type", length = 1)
    private String idType;
}