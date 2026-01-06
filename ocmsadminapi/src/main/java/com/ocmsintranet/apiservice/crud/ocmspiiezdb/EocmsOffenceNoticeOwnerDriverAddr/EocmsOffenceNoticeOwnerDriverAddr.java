package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the eocms_offence_notice_owner_driver_addr table in PII database.
 * This table stores encrypted address information for offence notice owners/drivers.
 *
 * Primary Key: (notice_no, owner_driver_indicator, type_of_address)
 *
 * All PII address fields are encrypted using SQL Server column-level encryption.
 */
@Entity
@Table(name = "eocms_offence_notice_owner_driver_addr", schema = "ocmspiiezmgr")
@IdClass(EocmsOffenceNoticeOwnerDriverAddrId.class)
@Getter
@Setter
public class EocmsOffenceNoticeOwnerDriverAddr extends BaseEntity {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    @Id
    @Column(name = "owner_driver_indicator", nullable = false, length = 1)
    private String ownerDriverIndicator;

    @Id
    @Column(name = "type_of_address", nullable = false, length = 20)
    private String typeOfAddress;

    // Encrypted Building Name
    @Column(name = "bldg_name", length = 65, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(65), DecryptByKey(bldg_name))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(65), ?))"
    )
    private String bldgName;

    // Encrypted Block/House Number
    @Column(name = "blk_hse_no", length = 10, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(10), DecryptByKey(blk_hse_no))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(10), ?))"
    )
    private String blkHseNo;

    // Encrypted Floor Number
    @Column(name = "floor_no", length = 3, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(3), DecryptByKey(floor_no))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(3), ?))"
    )
    private String floorNo;

    // Encrypted Postal Code
    @Column(name = "postal_code", length = 6, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(6), DecryptByKey(postal_code))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(6), ?))"
    )
    private String postalCode;

    // Encrypted Street Name
    @Column(name = "street_name", length = 32, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(32), DecryptByKey(street_name))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(32), ?))"
    )
    private String streetName;

    // Encrypted Unit Number
    @Column(name = "unit_no", length = 5, columnDefinition = "varbinary(max)")
    @ColumnTransformer(
        read = "CONVERT(varchar(5), DecryptByKey(unit_no))",
        write = "EncryptByKey(Key_GUID('ocmspii_symmetric_key'), CONVERT(varchar(5), ?))"
    )
    private String unitNo;
}