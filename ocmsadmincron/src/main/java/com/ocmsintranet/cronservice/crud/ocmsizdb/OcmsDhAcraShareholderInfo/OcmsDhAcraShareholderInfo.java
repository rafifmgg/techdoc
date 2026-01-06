package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraShareholderInfo;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_acra_shareholder_info table.
 * Stores shareholder information for companies from ACRA, identified by company UEN, person ID and notice number.
 * 
 * Primary Key: (company_uen, person_id_no, notice_no)
 */
@Entity
@Table(name = "ocms_dh_acra_shareholder_info", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsDhAcraShareholderInfoId.class)
public class OcmsDhAcraShareholderInfo extends BaseEntity {

    @Id
    @Column(name = "company_uen", length = 10, nullable = false)
    private String companyUen;

    @Id
    @Column(name = "person_id_no", length = 12, nullable = false)
    private String personIdNo;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "share_allotted_no")
    private Integer shareAllottedNo;

    @Column(name = "company_profile_uen", length = 10)
    private String companyProfileUen;

    @Column(name = "category", length = 1)
    private String category;
}