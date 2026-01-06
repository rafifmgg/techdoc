package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraBoardInfo;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_acra_board_info table.
 * Stores board member information for companies from ACRA, identified by entity UEN, person ID and notice number.
 * 
 * Primary Key: (entity_uen, person_id_no, notice_no)
 */
@Entity
@Table(name = "ocms_dh_acra_board_info", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsDhAcraBoardInfoId.class)
public class OcmsDhAcraBoardInfo extends BaseEntity {

    @Id
    @Column(name = "entity_uen", length = 10, nullable = false)
    private String entityUen;

    @Id
    @Column(name = "person_id_no", length = 12, nullable = false)
    private String personIdNo;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "position_appointment_date")
    private LocalDateTime positionAppointmentDate;

    @Column(name = "position_withdrawn_date")
    private LocalDateTime positionWithdrawnDate;

    @Column(name = "position_held_code", length = 2)
    private String positionHeldCode;

    @Column(name = "reference_period")
    private LocalDateTime referencePeriod;
}