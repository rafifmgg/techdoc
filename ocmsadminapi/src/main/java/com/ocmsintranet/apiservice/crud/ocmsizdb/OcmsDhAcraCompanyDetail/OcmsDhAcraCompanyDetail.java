package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhAcraCompanyDetail;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_acra_company_detail table.
 * Stores company details from ACRA for businesses identified by UEN and notice number.
 * 
 * Primary Key: composite key (uen, notice_no)
 */
@Entity
@Table(name = "ocms_dh_acra_company_detail", schema = "ocmsizmgr")
@IdClass(OcmsDhAcraCompanyDetailId.class)
@Getter
@Setter
public class OcmsDhAcraCompanyDetail extends BaseEntity {

    @Id
    @Column(name = "uen", length = 10, nullable = false)
    private String uen;

    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "entity_name", length = 65)
    private String entityName;

    @Column(name = "entity_type", length = 7)
    private String entityType;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "deregistration_date")
    private LocalDateTime deregistrationDate;

    @Column(name = "entity_status_code", length = 2)
    private String entityStatusCode;

    @Column(name = "company_type_code", length = 2)
    private String companyTypeCode;
}