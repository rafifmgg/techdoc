package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraShareholderInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsDhAcraShareholderInfo entity
 * Primary key: (company_uen, person_id_no, notice_no)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhAcraShareholderInfoId implements Serializable {
    
    private String companyUen;
    private String personIdNo;
    private String noticeNo;
}