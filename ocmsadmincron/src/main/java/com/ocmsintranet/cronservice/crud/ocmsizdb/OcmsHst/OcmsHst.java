package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_hst table.
 * This table stores information regarding the history of address changes and updates for individuals.
 * It maintains detailed records of individual identification, notice information, vehicle details,
 * and processing history with audit trail information.
 */
@Entity
@Table(name = "ocms_hst", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsHst extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 20, nullable = false)  // Correct: primary key is id_no
    private String idNo;

    // @Column(name = "notice_no", length = 10, nullable = false)
    // private String noticeNo;

    @Column(name = "id_type", length = 1, nullable = false)
    private String idType;

    @Column(name = "vehicle_no", length = 12, nullable = false)
    private String vehicleNo;

    @Column(name = "processing_date", nullable = false)
    private LocalDateTime processingDate;
}
