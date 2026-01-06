// ============================================================================
// LtaIdTypeUtil.java - Owner ID Type Classification Utility
// ============================================================================
package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

/**
 * Utility class for LTA owner ID type classification and mapping
 */
public class LtaIdTypeUtil {
    
    // OCMS ID type constants
    public static final String NRIC_TYPE = "N";
    public static final String FIN_TYPE = "F";
    public static final String PASSPORT_TYPE = "P";
    public static final String BUSINESS_TYPE = "B";
    
    /**
     * Map LTA owner ID types to OCMS ID types
     * 
     * @param ltaOwnerIdType The LTA owner ID type
     * @return The OCMS ID type (N/F/B/P)
     */
    public static String mapLtaOwnerIdTypeToOcms(String ltaOwnerIdType) {
        if (ltaOwnerIdType == null || ltaOwnerIdType.trim().isEmpty()) {
            return NRIC_TYPE; // Default to NRIC if null or empty
        }
        
        if (isSingpassNricIdType(ltaOwnerIdType)) {
            return NRIC_TYPE;
        } else if (isSingpassFinIdType(ltaOwnerIdType)) {
            return FIN_TYPE;
        } else if (isPassportIdType(ltaOwnerIdType)) {
            return PASSPORT_TYPE;
        } else if (isCorppassIdType(ltaOwnerIdType)) {
            return BUSINESS_TYPE;
        } else {
            return NRIC_TYPE; // Default to NRIC for unknown types
        }
    }
    
    /**
     * Check if owner ID type is NRIC
     */
    public static boolean isSingpassNricIdType(String ownerIdType) {
        return "1".equals(ownerIdType);
    }
    
    /**
     * Check if owner ID type is FIN
     */
    public static boolean isSingpassFinIdType(String ownerIdType) {
        return "D".equals(ownerIdType);
    }
    
    /**
     * Check if owner ID type is Singpass (NRIC or FIN)
     */
    public static boolean isSingpassIdType(String ownerIdType) {
        return isSingpassNricIdType(ownerIdType) || isSingpassFinIdType(ownerIdType);
    }

    /**
     * Check if owner ID type is Corppass (UEN)
     */
    public static boolean isCorppassIdType(String ownerIdType) {
        if (ownerIdType == null) {
            return false;
        }
        
        switch (ownerIdType) {
            case "4": // Government
            case "5": // Statutory Board
            case "6": // Local Business
            case "7": // Local Company
            case "8": // Club/Association/Organisation
            case "9": // Professional
            case "A": // Foreign Company ID
            case "B": // Limited Liability Partnership
            case "C": // Limited Partnership
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if ID type is passport
     */
    public static boolean isPassportIdType(String ownerIdType) {
        return "2".equals(ownerIdType) || "3".equals(ownerIdType); // 2 = Malaysia NRIC, 3 = Foreign Passport
    }
}
