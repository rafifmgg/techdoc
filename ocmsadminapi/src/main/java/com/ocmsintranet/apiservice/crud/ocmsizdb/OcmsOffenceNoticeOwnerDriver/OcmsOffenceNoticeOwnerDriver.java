package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_offence_notice_owner_driver table.
 * This table stores information about the owners or drivers associated with an offence notice,
 * including details like personal information, ownership, and contact information.
 * 
 * Primary Key: (notice_no, owner_driver_indicator)
 * 
 * Note: This entity has been updated to match the actual database schema based on
 * the CREATE TABLE definition.
 */
@Entity
@Table(name = "ocms_offence_notice_owner_driver", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsOffenceNoticeOwnerDriverId.class)
public class OcmsOffenceNoticeOwnerDriver extends BaseEntity {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo;

    @Id
    @Column(name = "owner_driver_indicator", nullable = false, length = 1)
    private String ownerDriverIndicator;
        
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    
    @Column(name = "life_status", length = 1)
    private String lifeStatus;
    
    @Column(name = "date_of_death")
    private LocalDateTime dateOfDeath;
    
    @Column(name = "email_addr", length = 320)
    private String emailAddr;
    
    @Column(name = "email_alt_addr", length = 320)
    private String emailAltAddr;
    
    @Column(name = "lta_entity_type", length = 1)
    private String entityType;
    
    @Column(name = "id_type", length = 1)
    private String idType;
        

    @Column(name = "name", length = 66)
    private String name;

    @Column(name = "id_no", length = 12, nullable = false)
    private String idNo;

    @Column(name = "offender_indicator", length = 1)
    private String offenderIndicator;
    
    @Column(name = "offender_tel_code", length = 3)
    private String offenderTelCode;
    
    @Column(name = "offender_tel_no", length = 12)
    private String offenderTelNo;
        
    @Column(name = "passport_place_of_issue", length = 3)
    private String passportPlaceOfIssue;

    @Column(name = "dh_mha_check_allow", length = 1)
    private String dhMhaCheckAllow;

    // Address fields
    @Column(name = "street_name", length = 200)
    private String streetName;

    @Column(name = "blk_hse_no", length = 50)
    private String blkHseNo;

    @Column(name = "floor_no", length = 10)
    private String floorNo;

    @Column(name = "unit_no", length = 10)
    private String unitNo;

    @Column(name = "bldg_name", length = 200)
    private String bldgName;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    // Helper method to get composite ID
    public OcmsOffenceNoticeOwnerDriverId getId() {
        return new OcmsOffenceNoticeOwnerDriverId(this.noticeNo, this.ownerDriverIndicator);
    }

}
