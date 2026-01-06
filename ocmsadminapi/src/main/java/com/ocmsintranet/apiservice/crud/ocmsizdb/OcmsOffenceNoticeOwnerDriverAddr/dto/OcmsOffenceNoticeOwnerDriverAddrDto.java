package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;

/**
 * DTO for OcmsOffenceNoticeOwnerDriverAddr entity
 * Used to control data flow without JPA relationships
 * Updated to match normalized schema
 */
@Data
@NoArgsConstructor
public class OcmsOffenceNoticeOwnerDriverAddrDto {

    // Primary key fields
    private String noticeNo; // The unique identifier for the offence notice
    private String ownerDriverIndicator; // Indicator whether the record relates to an owner or driver
    private String typeOfAddress; // type of the source address (lta_reg/lta_mail/mha_reg/furnished_mail)
    
    // Address fields based on schema
    private String bldgName; // The building name of the source address
    private String blkHseNo; // Block or house number of the source address
    private String streetName; // Street name of the source address
    private String floorNo; // Floor number of the source address
    private String unitNo; // Unit number of the source address
    private String postalCode; // Postal code of the source address
    private String addressType; // Address type return from MHA result file
    private String invalidAddrTag; // Flag to indicate if the address is invalid from MHA result file
    
    // Error code and effective date fields
    private String errorCode; // LTA error code related to the offence notice
    private LocalDateTime effectiveDate; // Date when the source address was last updated
    private LocalDateTime processingDateTime; // Date time when the record process by the LTA & MHA
    
    // Audit fields
    private LocalDateTime creDate; // Date when the record was created
    private String creUserId; // The user ID who created the record
    private LocalDateTime updDate; // Date when the record was last updated
    private String updUserId; // The user ID who last updated the record
    
    /**
     * Convert entity to DTO
     * @param entity OcmsOffenceNoticeOwnerDriverAddr entity
     * @return DTO representation
     */
    public static OcmsOffenceNoticeOwnerDriverAddrDto fromEntity(OcmsOffenceNoticeOwnerDriverAddr entity) {
        if (entity == null) {
            return null;
        }
        
        OcmsOffenceNoticeOwnerDriverAddrDto dto = new OcmsOffenceNoticeOwnerDriverAddrDto();
        
        // Set primary key fields
        dto.setNoticeNo(entity.getNoticeNo());
        dto.setOwnerDriverIndicator(entity.getOwnerDriverIndicator());
        dto.setTypeOfAddress(entity.getTypeOfAddress());
        
        // Set address fields
        dto.setBldgName(entity.getBldgName());
        dto.setBlkHseNo(entity.getBlkHseNo());
        dto.setStreetName(entity.getStreetName());
        dto.setFloorNo(entity.getFloorNo());
        dto.setUnitNo(entity.getUnitNo());
        dto.setPostalCode(entity.getPostalCode());
        dto.setAddressType(entity.getAddressType());
        dto.setInvalidAddrTag(entity.getInvalidAddrTag());
        
        // Set error code and effective date fields
        dto.setErrorCode(entity.getErrorCode());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setProcessingDateTime(entity.getProcessingDateTime());
        
        // Set audit fields
        dto.setCreDate(entity.getCreDate());
        dto.setCreUserId(entity.getCreUserId());
        dto.setUpdDate(entity.getUpdDate());
        dto.setUpdUserId(entity.getUpdUserId());
        
        return dto;
    }
    
    /**
     * Convert DTO to entity
     * @return entity representation
     */
    public OcmsOffenceNoticeOwnerDriverAddr toEntity() {
        OcmsOffenceNoticeOwnerDriverAddr entity = new OcmsOffenceNoticeOwnerDriverAddr();
        
        // Set primary key fields
        entity.setNoticeNo(this.noticeNo);
        entity.setOwnerDriverIndicator(this.ownerDriverIndicator);
        entity.setTypeOfAddress(this.typeOfAddress);
        
        // Set address fields
        entity.setBldgName(this.bldgName);
        entity.setBlkHseNo(this.blkHseNo);
        entity.setStreetName(this.streetName);
        entity.setFloorNo(this.floorNo);
        entity.setUnitNo(this.unitNo);
        entity.setPostalCode(this.postalCode);
        entity.setAddressType(this.addressType);
        entity.setInvalidAddrTag(this.invalidAddrTag);
        
        // Set error code and effective date fields
        entity.setErrorCode(this.errorCode);
        entity.setEffectiveDate(this.effectiveDate);
        entity.setProcessingDateTime(this.processingDateTime);
        
        // Set audit fields
        entity.setCreDate(this.creDate);
        entity.setCreUserId(this.creUserId);
        entity.setUpdDate(this.updDate);
        entity.setUpdUserId(this.updUserId);
        
        return entity;
    }
}
