package com.ocmsintranet.apiservice.crud.casdb.vipvehicle;

import com.ocmsintranet.apiservice.crud.dao.casdb.repositories.VipVehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementation for VIP vehicle detection
 * Queries CAS VIP_VEHICLE database to identify vehicles with active VIP parking labels
 *
 * Implementation based on OCMS 14 requirements:
 * - Section 2.3: VIP Vehicle Detection Flow
 * - Section 7: VIP Vehicle Processing
 * - Query pattern: SELECT vehicle_no FROM VIP_VEHICLE WHERE vehicle_no = '<vehNo>' AND status = 'A'
 */
@Service
@Slf4j
public class VipVehicleService implements VipVehicleServiceInterface {

    @Autowired
    private VipVehicleRepository vipVehicleRepository;

    /**
     * Check if vehicle has an active VIP parking label by querying CAS database
     *
     * @param vehNo Vehicle registration number (will be converted to uppercase)
     * @return true if vehicle has active VIP status in CAS database, false otherwise
     */
    @Override
    public boolean isVipVehicle(String vehNo) {
        if (vehNo == null || vehNo.trim().isEmpty()) {
            log.debug("VIP check: Vehicle number is null or empty, returning false");
            return false;
        }

        try {
            // Convert to uppercase for consistent querying
            String vehicleNumber = vehNo.trim().toUpperCase();

            log.debug("Querying CAS VIP_VEHICLE table for vehicle: {}", vehicleNumber);
            boolean isVip = vipVehicleRepository.isVipVehicle(vehicleNumber);

            log.info("VIP check for vehicle {}: {}", vehicleNumber, isVip ? "VIP" : "NOT VIP");
            return isVip;

        } catch (Exception e) {
            log.error("Error querying CAS VIP_VEHICLE table for vehicle {}: {}",
                     vehNo, e.getMessage(), e);
            // If CAS database query fails, default to false (non-VIP)
            // This allows the system to continue functioning if CAS is temporarily unavailable
            return false;
        }
    }
}
