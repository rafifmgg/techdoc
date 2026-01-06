package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsReduction;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for OcmsReducedOffenceAmount entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsReducedOffenceAmountId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String noticeNo;
    private Integer srNo;
}
