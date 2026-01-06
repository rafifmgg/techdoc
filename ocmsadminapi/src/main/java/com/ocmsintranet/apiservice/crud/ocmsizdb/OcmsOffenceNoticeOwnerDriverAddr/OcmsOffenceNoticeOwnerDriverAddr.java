package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity class for ocms_offence_notice_owner_driver_addr table
 * Stores normalized address records associated with an offence notice, separated by address type
 */
@Entity
@Table(name = "ocms_offence_notice_owner_driver_addr", schema = "ocmsizmgr")
@Getter
@Setter
@NoArgsConstructor
@IdClass(OcmsOffenceNoticeOwnerDriverAddrId.class)
public class OcmsOffenceNoticeOwnerDriverAddr extends BaseEntity {

    @Id
    @Column(name = "notice_no", nullable = false, length = 10)
    private String noticeNo; // The unique identifier for the offence notice
    
    @Id
    @Column(name = "owner_driver_indicator", nullable = false, length = 1)
    private String ownerDriverIndicator; // Indicator whether the record relates to an owner or driver
    
    @Id
    @Column(name = "type_of_address", nullable = false, length = 20)
    private String typeOfAddress; // type of the source address (lta_reg/lta_mail/mha_reg/furnished_mail)

    // Address fields based on schema
    @Column(name = "bldg_name", length = 65)
    private String bldgName; // The building name of the source address
    
    @Column(name = "blk_hse_no", length = 10)
    private String blkHseNo; // Block or house number of the source address
    
    @Column(name = "street_name", length = 32)
    private String streetName; // Street name of the source address
    
    @Column(name = "floor_no", length = 3)
    private String floorNo; // Floor number of the source address
    
    @Column(name = "unit_no", length = 5)
    private String unitNo; // Unit number of the source address
    
    @Column(name = "postal_code", length = 6)
    private String postalCode; // Postal code of the source address
    
    @Column(name = "address_type", length = 1)
    private String addressType; // Address type return from MHA result file
    
    @Column(name = "invalid_addr_tag", length = 1)
    private String invalidAddrTag; // Flag to indicate if the address is invalid from MHA result file

    // Error code and effective date fields
    @Column(name = "error_code", length = 10)
    private String errorCode; // LTA error code related to the offence notice
    
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate; // Date when the source address was last updated
    
    @Column(name = "processing_date_time")
    private LocalDateTime processingDateTime; // Date time when the record process by the LTA & MHA

    @Column(name = "id_no", length = 12)
    private String idNo; // ID number of the owner/driver

    // Audit fields inherited from BaseEntity

    // Helper methods - aliases for compatibility
    public String getBlkNo() {
        return this.blkHseNo;
    }

    public void setBlkNo(String blkNo) {
        this.blkHseNo = blkNo;
    }

    public String getFloor() {
        return this.floorNo;
    }

    public void setFloor(String floor) {
        this.floorNo = floor;
    }

    public String getAddrType() {
        return this.typeOfAddress;
    }

    public void setAddrType(String addrType) {
        this.typeOfAddress = addrType;
    }
}
