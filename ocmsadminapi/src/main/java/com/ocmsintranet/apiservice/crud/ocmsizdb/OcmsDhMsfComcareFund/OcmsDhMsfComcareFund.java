package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMsfComcareFund;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_dh_msf_comcare_fund table.
 * This table contains data on ComCare assistance administered through Community Care Committees (CCC) 
 * in Singapore. It includes details about beneficiaries, assistance periods, payment dates, and 
 * relevant metadata regarding record creation and updates.
 * 
 * Primary Key: (id_no, notice_no)
 */
@Entity
@Table(name = "ocms_dh_msf_comcare_fund", schema = "ocmsizmgr")
@Getter
@Setter
@IdClass(OcmsDhMsfComcareFundId.class)
public class OcmsDhMsfComcareFund extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 9, nullable = false)
    private String idNo;
    
    @Id
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;

    @Column(name = "assistance_start")
    private LocalDateTime assistanceStart;
    
    @Column(name = "assistance_end")
    private LocalDateTime assistanceEnd;
    
    @Column(name = "beneficiary_name", length = 66)
    private String beneficiaryName;
    
    @Column(name = "data_date")
    private LocalDateTime dataDate;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "reference_period", nullable = false)
    private LocalDateTime referencePeriod;
    
    @Column(name = "source", length = 3, nullable = false)
    private String source;
}