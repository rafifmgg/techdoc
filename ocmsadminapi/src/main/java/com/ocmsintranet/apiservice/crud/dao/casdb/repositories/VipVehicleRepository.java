package com.ocmsintranet.apiservice.crud.dao.casdb.repositories;

import com.ocmsintranet.apiservice.crud.dao.casdb.models.VipVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for querying VIP_VEHICLE table in CAS database
 * Used for VIP vehicle detection as per OCMS 14 requirements
 *
 * Query pattern from documentation:
 * SELECT vehicle_no FROM VIP_VEHICLE WHERE vehicle_no = '<vehicle number>' AND status = 'A'
 */
@Repository
public interface VipVehicleRepository extends JpaRepository<VipVehicle, String> {

    /**
     * Find active VIP vehicle by vehicle number
     * @param vehicleNo Vehicle registration number
     * @return Optional containing VipVehicle if found with status 'A', empty otherwise
     */
    @Query("SELECT v FROM VipVehicle v WHERE v.vehicleNo = :vehicleNo AND v.status = 'A'")
    Optional<VipVehicle> findActiveVipVehicle(@Param("vehicleNo") String vehicleNo);

    /**
     * Check if vehicle has active VIP status
     * Convenience method for boolean check
     * @param vehicleNo Vehicle registration number
     * @return true if vehicle exists in VIP_VEHICLE with status 'A'
     */
    default boolean isVipVehicle(String vehicleNo) {
        return findActiveVipVehicle(vehicleNo).isPresent();
    }
}
