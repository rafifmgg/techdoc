package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_driver_notice table.
 * This table stores information about driver notices, including details related to 
 * notice processing, the driver's information, and any claims or reminders.
 * 
 * Primary Key: (date_of_processing, notice_no)
 */
@Entity
@Table(name = "ocms_driver_notice", schema = "ocmsizmgr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(OcmsDriverNoticeId.class)
public class OcmsDriverNotice extends BaseEntity {

    @Id
    @Column(name = "date_of_processing", nullable = false)
    private LocalDateTime dateOfProcessing;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "date_of_dn", nullable = false)
    private LocalDateTime dateOfDn;

    @Column(name = "date_of_return")
    private LocalDateTime dateOfReturn;

    @Column(name = "driver_bldg", length = 65)
    private String driverBldg;

    @Column(name = "driver_blk_hse_no", length = 10)
    private String driverBlkHseNo;

    @Column(name = "driver_floor", length = 3)
    private String driverFloor;

    @Column(name = "driver_id_type", length = 1, nullable = false)
    private String driverIdType;

    @Column(name = "driver_name", length = 66)
    private String driverName;

    @Column(name = "driver_nric_no", length = 20, nullable = false)
    private String driverNricNo;

    @Column(name = "driver_postal_code", length = 6)
    private String driverPostalCode;

    @Column(name = "driver_street", length = 32)
    private String driverStreet;

    @Column(name = "driver_unit", length = 5)
    private String driverUnit;

    @Column(name = "postal_regn_no", length = 15)
    private String postalRegnNo;

    @Column(name = "processing_stage", length = 3, nullable = false)
    private String processingStage;

    @Column(name = "reason_for_unclaim", length = 3)
    private String reasonForUnclaim;

    @Column(name = "reminder_flag", length = 1)
    private String reminderFlag;

}