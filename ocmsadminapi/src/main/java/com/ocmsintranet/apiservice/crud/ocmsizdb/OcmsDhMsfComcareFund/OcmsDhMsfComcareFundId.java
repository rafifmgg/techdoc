package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDhMsfComcareFund;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OcmsDhMsfComcareFund entity.
 * Primary Key: (id_no, notice_no)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsDhMsfComcareFundId implements Serializable {
    private String idNo;
    private String noticeNo;
}