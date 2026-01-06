package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.casdb.vipvehicle.VipVehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// LTA Vehicle Number Validation Library
import validateGenerateVehicleNoSuffix.ValidateRegistrationNo;
import validateGenerateVehicleNoSuffix.VehicleNoException;

/**
 * Utility class for vehicle registration type checking
 * Replicates the logic from the old system excluding vehicle weight factor check
 */
@Slf4j
@Component
public class SpecialVehUtils {

    @Autowired
    private VipVehicleService vipVehicleService;

    // Constructor allows Spring autowiring
    public SpecialVehUtils() {
        // Dependencies injected by Spring
    }

    /**
     * Check vehicle registration type
     * Returns a single-character code representing the vehicle type:
     * X - UPL vehicle
     * D - Diplomat vehicle
     * I - MID vehicle
     * V - VIP vehicle
     * S - Singapore vehicle
     * F - Foreign vehicle
     *
     * @param vehNo Vehicle registration number
     * @param sourceProvidedType Vehicle registration type provided by source (e.g., REPCCS, EHT)
     * @return Vehicle registration type code
     */
    public String checkVehregistration(String vehNo, String sourceProvidedType) {
        // Step 1: Honor source-provided registration type (e.g., REPCCS/EHT marking as Foreign)
        if ("F".equals(sourceProvidedType)) {
            log.debug("Source provided registration type is F, returning F (Foreign vehicle)");
            return "F";
        }

        // Step 2: Check for blank vehicle number or "N.A" string (UPL vehicle)
        if (vehNo == null || vehNo.isEmpty() || vehNo.trim().isEmpty() || "N.A".equalsIgnoreCase(vehNo.trim())) {
            log.debug("Vehicle number is blank/null/N.A, returning X (UPL vehicle)");
            return "X";
        }

        // Check for UNLICENSED_PARKING as UPL vehicle (standardized format for unlicensed parking)
        String trimmedVehNo = vehNo.trim().toUpperCase();
        if (SystemConstant.VehicleNumber.UNLICENSED_PARKING.equals(trimmedVehNo)) {
            log.debug("Vehicle number is {}, returning X (UPL vehicle)",
                SystemConstant.VehicleNumber.UNLICENSED_PARKING);
            return "X";
        }

        vehNo = trimmedVehNo;
        log.debug("Checking vehicle registration type for: {}", vehNo);
        if (isSg(vehNo)) {
            log.debug("Vehicle {} identified as Singapore vehicle", vehNo);
            return "S";
        }
        if (isDip(vehNo)) {
            log.debug("Vehicle {} identified as Diplomat vehicle", vehNo);
            return "D";
        }
        if (isMid(vehNo)) {
            log.debug("Vehicle {} identified as MID vehicle", vehNo);
            return "I";
        }
        if (isVip(vehNo)) {
            log.debug("Vehicle {} identified as VIP vehicle", vehNo);
            return "V";
        }

        // Step 7: Check if last character is alphabet (edge case for Singapore vehicles)
        if (vehNo.length() > 0 && Character.isLetter(vehNo.charAt(vehNo.length() - 1))) {
            log.debug("Vehicle {} has alphabet as last character, returning S (Local)", vehNo);
            return "S";
        }

        // Step 8: Default to Foreign vehicle if no other type matches
        log.debug("Vehicle {} does not match any specific type, defaulting to Foreign vehicle", vehNo);
        return "F";
    }

    /**
     * Check if vehicle is a UPL vehicle
     * @param vehNo Vehicle registration number
     * @return true if UPL vehicle
     */
    private boolean inUplVehNo(String vehNo) {
        // UPL vehicle check - already handled in checkVehregistration for blank vehicle numbers
        // Keeping this method for future extensibility
        return false;
    }

    /**
     * Check if vehicle is a Diplomat vehicle
     * @param vehNo Vehicle registration number
     * @return true if Diplomat vehicle
     */
    private boolean isDip(String vehNo) {
        // Diplomat vehicle check: prefix "S" AND suffix with values "CC", "CD", "TC", "TE" or "CC"
        return vehNo.matches("^S.*CC$") || 
               vehNo.matches("^S.*CD$") || 
               vehNo.matches("^S.*TC$") || 
               vehNo.matches("^S.*TE$");
    }

    /**
     * Check if vehicle is a MID vehicle
     * @param vehNo Vehicle registration number
     * @return true if MID vehicle
     */
    private boolean isMid(String vehNo) {
        // MID vehicle check based on prefix "MID" or suffix "MID"
        // Also check for MINDEF prefix or suffix as per new requirements
        return vehNo.startsWith("MID") || vehNo.endsWith("MID") ||
               vehNo.startsWith("MINDEF") || vehNo.endsWith("MINDEF");
    }

    /**
     * Check if vehicle is a VIP vehicle by querying the CAS database
     * @param vehNo Vehicle registration number
     * @return true if VIP vehicle
     */
    private boolean isVip(String vehNo) {
        try {
            // Use VipVehicleService to check if vehicle is VIP
            // Queries CAS VIP_VEHICLE table: SELECT vehicle_no FROM VIP_VEHICLE WHERE vehicle_no = '<vehNo>' AND status = 'A'
            boolean isVip = vipVehicleService.isVipVehicle(vehNo);
            log.debug("VIP check for vehicle {}: {}", vehNo, isVip);
            return isVip;
        } catch (Exception e) {
            log.error("Error checking VIP status for vehicle {}: {}", vehNo, e.getMessage(), e);
            // If CAS query fails, default to false (non-VIP) to allow processing to continue
            return false;
        }
    }

    /**
     * Check if vehicle is a Singapore vehicle using LTA official validation library
     * @param vehNo Vehicle registration number
     * @return true if Singapore vehicle (valid checksum)
     */
    private boolean isSg(String vehNo) {
        // Use LTA official validation library
        // This handles all Singapore vehicle formats including:
        // - Single letter prefix (E, F, G, Q, S, W, X, Y)
        // - Two letter prefix (QA, QB, PA, XA, SBA, SKM, etc.)
        // - Three letter special prefixes (SAG, SPF, SBS, CSS, LTA, etc.)
        try {
            boolean isValid = ValidateRegistrationNo.validate(vehNo);
            log.debug("LTA validation for vehicle {}: {}", vehNo, isValid ? "VALID" : "INVALID");
            return isValid;
        } catch (VehicleNoException e) {
            // LTA library throws exception for invalid vehicle numbers
            log.debug("LTA validation failed for vehicle {}: code {}, message: {}",
                     vehNo, e.getReturnCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            log.warn("Unexpected error during LTA validation for vehicle {}: {}",
                    vehNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if vehicle is a Foreign vehicle
     * @param vehNo Vehicle registration number
     * @return true if Foreign vehicle
     */
    private boolean isForeign(String vehNo) {
        // Foreign vehicle check - any vehicle that doesn't match other patterns
        // This is a catch-all method and will return true for anything that doesn't match other patterns
        return true;
    }
}
