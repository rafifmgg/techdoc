package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_hst table.
 * Stores HST (House Tenant) suspension records for offenders whose addresses are invalid.
 * Used to track offenders for monthly MHA/DataHive address checks and to automatically
 * suspend new notices under the same offender ID.
 *
 * Primary Key: id_no
 */
@Entity
@Table(name = "ocms_hst", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsHst extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 20, nullable = false)
    private String idNo;

    @Column(name = "id_type", length = 10, nullable = false)
    private String idType;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "street_name", length = 255)
    private String streetName;

    @Column(name = "blk_hse_no", length = 20)
    private String blkHseNo;

    @Column(name = "floor_no", length = 10)
    private String floorNo;

    @Column(name = "unit_no", length = 10)
    private String unitNo;

    @Column(name = "bldg_name", length = 255)
    private String bldgName;

    @Column(name = "postal_code", length = 10)
    private String postalCode;
}
