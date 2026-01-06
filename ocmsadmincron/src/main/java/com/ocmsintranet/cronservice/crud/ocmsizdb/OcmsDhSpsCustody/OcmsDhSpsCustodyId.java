package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhSpsCustody;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Composite primary key for OcmsDhSpsCustody entity
 * Primary key: (id_no, notice_no, adm_date)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhSpsCustodyId implements Serializable {
    
    private String idNo;
    private String noticeNo;
    private LocalDateTime admDate;
}