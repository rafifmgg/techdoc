package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraCompanyDetail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsDhAcraCompanyDetail entity
 * Primary key: (uen, notice_no)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhAcraCompanyDetailId implements Serializable {
    
    private String uen;
    private String noticeNo;
}
