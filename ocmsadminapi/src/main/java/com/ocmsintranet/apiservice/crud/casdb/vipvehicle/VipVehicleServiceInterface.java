package com.ocmsintranet.apiservice.crud.casdb.vipvehicle;

/**
 * Service interface for VIP vehicle detection
 * Queries CAS VIP_VEHICLE database to identify vehicles with active VIP parking labels
 *
 * Implementation based on OCMS 14 requirements:
 * - Section 2.3: VIP Vehicle Detection Flow
 * - Section 7: VIP Vehicle Processing
 */
public interface VipVehicleServiceInterface {

    /**
     * Check if vehicle has an active VIP parking label
     * Queries CAS VIP_VEHICLE table with: vehicle_no = '<vehNo>' AND status = 'A'
     *
     * @param vehNo Vehicle registration number
     * @return true if vehicle has active VIP status in CAS database, false otherwise
     */
    boolean isVipVehicle(String vehNo);
}
