package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhAcraBoardInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsDhAcraBoardInfo entity
 * Primary key: (entity_uen, person_id_no, notice_no)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhAcraBoardInfoId implements Serializable {
    
    private String entityUen;
    private String personIdNo;
    private String noticeNo;
}