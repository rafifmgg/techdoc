package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsUplVehNo;

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
@Table(name = "ocms_upl_veh_no")
@Setter
@Getter
@NoArgsConstructor
public class OcmsUplVehNo {

    @Column(name = "vehicle_no", length = 14, nullable = false)
    @Id
    @NotBlank
    private String vehicleNo;

    @Column(name = "cre_date")
    private LocalDateTime creDate;

    @Column(name = "cre_user_id", length = 50)
    private String creUserId;

    @Column(name = "upd_date")
    private LocalDateTime updDate;

    @Column(name = "upd_user_id", length = 50)
    private String updUserId;

    @Column(name = "veh_registration", length = 1, nullable = false)
    @NotBlank
    private String vehRegistration;
}
