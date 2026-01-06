package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsVipVehicle;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_vip_vehicle")
@Setter
@Getter
@NoArgsConstructor
public class OcmsVipVehicle {
    
    public static final String C_VEHICLE_NO = "vehicle_no";
    public static final String C_STATUS = "status";
    
    @Column(name = "vehicle_no", length = 14, nullable=false)
    @Id
    @NotBlank
    private String vehicleNo;

    @Column(name = "parking_label_id", length = 50, nullable=false)
    @NotBlank
    private String parkingLabelId;
    
    @Column(name = "type", length = 30, nullable=false)
    @NotBlank
    private String type;
    
    @Column(name = "iu_no", length = 10)
    private String iuNo;
    
    @Column(name = "vehicle_owner_name", length = 66)
    private String vehicleOwnerName;
    
    @Column(name = "spouse_name", length = 66)
    private String spouseName;

    @Column(name = "parking_place_code", length = 5, nullable=false)
    @NotBlank
    private String parkingPlaceCode;
    
    @Column(name = "parking_place", length = 50, nullable=false)
    @NotBlank
    private String parkingPlace;
    
    @Column(name = "label_no", length = 30, nullable=false)
    @NotBlank
    private String labelNo;
    
    @Column(name = "label_type", length = 30, nullable=false)
    @NotBlank
    private String labelType;
    
    @Column(name = "label_desc", length = 30, nullable=false)
    @NotBlank
    private String labelDesc;
    
    @Column(name = "label_holder_name", length = 66, nullable=false)
    @NotBlank
    private String labelHolderName;
    
    @Column(name = "issued_dt", nullable=false)
    @NotBlank
    private LocalDateTime issuedDt;
    
    @Column(name = "issuing_authority", length = 50, nullable=false)
    @NotBlank
    private String issuingAuthority;
    
    @Column(name = "section", length = 30, nullable=false)
    @NotBlank
    private String section;
    
    @Column(name = "designation", length = 30, nullable=false)
    @NotBlank
    private String designation;
    
    @Column(name = "staff_name", length = 66, nullable=false)
    @NotBlank
    private String staffName;
    
    @Column(name = "validity_start_dt", nullable=false)
    @NotBlank
    private LocalDateTime validityStartDt;

    @Column(name = "validity_end_dt", nullable=false)
    @NotBlank
    private LocalDateTime validityEndDt;
    
    @Column(name = "management_status", length = 200, nullable=false)
    @NotBlank
    private String managementStatus;
    
    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "status", length = 1, nullable=false)
    @NotBlank
    private String status;
    
    @Column(name = "received_dt", nullable=false)
    @NotBlank
    private LocalDateTime receivedDt;
    
    @Column(name = "cre_date")
    @NotBlank
    private LocalDateTime creDate;

    @Column(name = "cre_user_id", length = 50)
    @NotBlank
    private String creUserId;

    @Column(name = "upd_date")
    private LocalDateTime updDate;

    @Column(name = "upd_user_id", length = 50)
    private String updUserId;
    
    @Version // Optimistic locking
    private Long version;
}
