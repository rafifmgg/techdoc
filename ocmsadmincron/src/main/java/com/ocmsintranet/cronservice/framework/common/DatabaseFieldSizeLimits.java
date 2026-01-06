package com.ocmsintranet.cronservice.framework.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized repository for database field size limits
 * Contains predefined maps of field size limits for different entity types
 */
public class DatabaseFieldSizeLimits {

    /**
     * Field size limits for LTA records
     */
    private static final Map<String, Integer> LTA_FIELD_SIZE_LIMITS = new HashMap<>();
    
    /**
     * Field size limits for MHA records
     */
    private static final Map<String, Integer> MHA_FIELD_SIZE_LIMITS = new HashMap<>();
    
    /**
     * Initialize field size limits
     */
    static {
        // LTA field size limits
        // Notice and vehicle information
        LTA_FIELD_SIZE_LIMITS.put("offenceNoticeNumber", 20);  // ocms_offence_notice_detail.notice_no (10), but allowing extra space
        LTA_FIELD_SIZE_LIMITS.put("vehicleNumber", 20);        // No direct column, used in processing
        LTA_FIELD_SIZE_LIMITS.put("chassisNumber", 25);        // ocms_offence_notice_detail.lta_chassis_number (25)
        
        // Owner information
        LTA_FIELD_SIZE_LIMITS.put("ownerId", 12);              // ocms_offence_notice_owner_driver.id_no (12)
        LTA_FIELD_SIZE_LIMITS.put("ownerName", 66);            // ocms_offence_notice_owner_driver.name (66)
        LTA_FIELD_SIZE_LIMITS.put("ownerIdType", 1);           // ocms_offence_notice_owner_driver.id_type (1)
        
        // Address information
        LTA_FIELD_SIZE_LIMITS.put("blockHouseNo", 10);         // ocms_offence_notice_owner_driver_addr.blk_hse_no (10)
        LTA_FIELD_SIZE_LIMITS.put("streetName", 32);           // ocms_offence_notice_owner_driver_addr.street_name (32)
        LTA_FIELD_SIZE_LIMITS.put("buildingName", 65);         // ocms_offence_notice_owner_driver_addr.bldg_name (65)
        LTA_FIELD_SIZE_LIMITS.put("postalCode", 6);            // ocms_offence_notice_owner_driver_addr.postal_code (6)
        
        // Vehicle details
        LTA_FIELD_SIZE_LIMITS.put("vehicleMake", 50);          // ocms_offence_notice_detail.vehicle_make (50)
        LTA_FIELD_SIZE_LIMITS.put("primaryColour", 100);       // ocms_offence_notice_detail.lta_primary_colour (100)
        LTA_FIELD_SIZE_LIMITS.put("secondaryColour", 100);     // ocms_offence_notice_detail.lta_secondary_colour (100)
        
        // MHA field size limits based on MhaFileFormatHelper.RESPONSE_FIELD_DEFINITIONS
        // Personal information
        MHA_FIELD_SIZE_LIMITS.put("uin", 9);                // UIN (1-9)
        MHA_FIELD_SIZE_LIMITS.put("name", 66);              // Name (10-75)
        MHA_FIELD_SIZE_LIMITS.put("dateOfBirth", 8);        // Date of birth (76-83)
        MHA_FIELD_SIZE_LIMITS.put("dateOfDeath", 8);        // Date of Death (174-181)
        MHA_FIELD_SIZE_LIMITS.put("lifeStatus", 1);         // Life Status (182)
        
        // Address information
        MHA_FIELD_SIZE_LIMITS.put("addressType", 1);        // MHA_Address_type (84)
        MHA_FIELD_SIZE_LIMITS.put("blockHouseNo", 10);      // MHA_Block No (85-94)
        MHA_FIELD_SIZE_LIMITS.put("streetName", 32);        // MHA_Street Name (95-126)
        MHA_FIELD_SIZE_LIMITS.put("floorNo", 2);            // MHA_Floor No (127-128)
        MHA_FIELD_SIZE_LIMITS.put("unitNo", 5);             // MHA_Unit No (129-133)
        MHA_FIELD_SIZE_LIMITS.put("buildingName", 30);       // MHA_Building Name (134-163)
        MHA_FIELD_SIZE_LIMITS.put("postalCode", 6);          // MHA_New_Postal_Code (168-173)
        MHA_FIELD_SIZE_LIMITS.put("invalidAddressTag", 1);   // Invalid Address Tag (183)
        
        // Reference information
        MHA_FIELD_SIZE_LIMITS.put("uraReferenceNo", 10);     // URA Reference No. (184-193)
        MHA_FIELD_SIZE_LIMITS.put("batchDateTime", 14);      // Batch Date Time (194-207)
        MHA_FIELD_SIZE_LIMITS.put("lastChangeAddressDate", 8); // Date of Address Change (208-215)
        MHA_FIELD_SIZE_LIMITS.put("timestamp", 23);          // Timestamp (216-238)
    }
    
    /**
     * Get field size limits for LTA records
     * 
     * @return Unmodifiable map of field names to maximum allowed lengths
     */
    public static Map<String, Integer> getLtaFieldSizeLimits() {
        return Collections.unmodifiableMap(LTA_FIELD_SIZE_LIMITS);
    }
    
    /**
     * Get field size limits for MHA records
     * 
     * @return Unmodifiable map of field names to maximum allowed lengths
     */
    public static Map<String, Integer> getMhaFieldSizeLimits() {
        return Collections.unmodifiableMap(MHA_FIELD_SIZE_LIMITS);
    }
    
    /**
     * Add additional field size limits to an existing map
     * 
     * @param existingLimits The existing map of field size limits
     * @param fieldName The field name
     * @param maxLength The maximum allowed length
     */
    public static void addFieldSizeLimit(Map<String, Integer> existingLimits, String fieldName, int maxLength) {
        existingLimits.put(fieldName, maxLength);
    }
}
