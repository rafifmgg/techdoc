package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVehicleWeightFactor;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_vehicle_weight_factor")
@Setter
@Getter
@NoArgsConstructor
public class OcmsVehicleWeightFactor {

    @Column(name = "vehicle_series", length = 3, nullable = false)
    @Id
    @NotBlank
    private String vehicleSeries;

    @Column(name = "cre_date")
    private LocalDateTime creDate;

    @Column(name = "cre_user_id", length = 50)
    private String creUserId;

    @Column(name = "upd_date")
    private LocalDateTime updDate;

    @Column(name = "upd_user_id", length = 50)
    private String updUserId;

    @Column(name = "weight_factor", nullable = false)
    private Integer weightFactor;

    @Column(name = "status", length = 1, nullable = false)
    @NotBlank
    private String status;
}
