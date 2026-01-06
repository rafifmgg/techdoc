package com.ocmsintranet.apiservice.crud.dao.casdb.models;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

/**
 * Entity mapping for VIP_VEHICLE table in CAS database
 * Used for detecting vehicles with active VIP parking labels
 *
 * Based on OCMS 14 documentation:
 * - Query: SELECT vehicle_no FROM VIP_VEHICLE WHERE vehicle_no = '<vehicle number>' AND status = 'A'
 * - Source: v1.7_OCMS 14 Functional Document, Section 2.3
 */
@Entity
@Table(name = "VIP_VEHICLE")
@Data
public class VipVehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "VEHICLE_NO", nullable = false, length = 50)
    private String vehicleNo;

    @Column(name = "STATUS", length = 1)
    private String status;

    /**
     * Check if this VIP vehicle record is active
     * @return true if status is 'A' (Active)
     */
    public boolean isActive() {
        return "A".equals(status);
    }
}
