package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_furnish_application table (Intranet DB).
 * Stores furnish hirer/driver particulars synced from Internet DB.
 *
 * Note: PII fields are NOT encrypted in this table (Intranet DB).
 * The cron sync process decrypts PII from Internet DB when copying here.
 */
@Entity
@Table(name = "ocms_furnish_application", schema = "ocmsizmgr")
@Getter
@Setter
@NoArgsConstructor
public class OcmsFurnishApplication extends BaseEntity {

    @Id
    @Column(name = "txn_no", length = 20, nullable = false)
    private String txnNo;

    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "vehicle_no", length = 14, nullable = false)
    private String vehicleNo;

    @Column(name = "offence_date", nullable = false)
    private LocalDateTime offenceDate;

    @Column(name = "pp_code", length = 5, nullable = false)
    private String ppCode;

    @Column(name = "pp_name", length = 100, nullable = false)
    private String ppName;

    @Column(name = "last_processing_stage", length = 3, nullable = false)
    private String lastProcessingStage;

    @Column(name = "furnish_name", length = 66, nullable = false)
    private String furnishName;

    @Column(name = "furnish_id_type", length = 1, nullable = false)
    private String furnishIdType;

    @Column(name = "furnish_id_no", length = 12, nullable = false)
    private String furnishIdNo;

    @Column(name = "furnish_mail_blk_no", length = 10, nullable = false)
    private String furnishMailBlkNo;

    @Column(name = "furnish_mail_floor", length = 3)
    private String furnishMailFloor;

    @Column(name = "furnish_mail_street_name", length = 32, nullable = false)
    private String furnishMailStreetName;

    @Column(name = "furnish_mail_unit_no", length = 5)
    private String furnishMailUnitNo;

    @Column(name = "furnish_mail_bldg_name", length = 65)
    private String furnishMailBldgName;

    @Column(name = "furnish_mail_postal_code", length = 6, nullable = false)
    private String furnishMailPostalCode;

    @Column(name = "furnish_tel_code", length = 4)
    private String furnishTelCode;

    @Column(name = "furnish_tel_no", length = 12)
    private String furnishTelNo;

    @Column(name = "furnish_email_addr", length = 320)
    private String furnishEmailAddr;

    @Column(name = "owner_driver_indicator", length = 1, nullable = false)
    private String ownerDriverIndicator;

    @Column(name = "hirer_owner_relationship", length = 1, nullable = false)
    private String hirerOwnerRelationship;

    @Column(name = "others_relationship_desc", length = 15)
    private String othersRelationshipDesc;

    @Column(name = "ques_one_ans", length = 32, nullable = false)
    private String quesOneAns;

    @Column(name = "ques_two_ans", length = 32, nullable = false)
    private String quesTwoAns;

    @Column(name = "ques_three_ans", length = 32)
    private String quesThreeAns;

    @Column(name = "rental_period_to")
    private LocalDateTime rentalPeriodTo;

    @Column(name = "rental_period_from")
    private LocalDateTime rentalPeriodFrom;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "owner_name", length = 66, nullable = false)
    private String ownerName;

    @Column(name = "owner_id_no", length = 12, nullable = false)
    private String ownerIdNo;

    @Column(name = "owner_tel_code", length = 4)
    private String ownerTelCode;

    @Column(name = "owner_tel_no", length = 20)
    private String ownerTelNo;

    @Column(name = "owner_email_addr", length = 320)
    private String ownerEmailAddr;

    @Column(name = "corppass_staff_name", length = 66)
    private String corppassStaffName;

    @Column(name = "reason_for_review", length = 255)
    private String reasonForReview;

    @Column(name = "remarks", length = 200)
    private String remarks;
}
