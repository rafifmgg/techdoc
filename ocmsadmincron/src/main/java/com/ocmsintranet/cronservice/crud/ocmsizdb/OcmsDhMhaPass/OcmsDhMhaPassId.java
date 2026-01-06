package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMhaPass;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsDhMhaPass entity
 * Primary key: (id_no, notice_no)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhMhaPassId implements Serializable {
    
    private String idNo;
    private String noticeNo;
}
