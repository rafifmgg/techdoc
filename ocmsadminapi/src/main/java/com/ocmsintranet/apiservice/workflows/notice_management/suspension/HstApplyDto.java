package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.Data;

/**
 * DTO for applying HST suspension
 * Maps to POST /v1/apply-hst request payload
 */
@Data
public class HstApplyDto {

    private String idNo;
    private String name;
    private String streetName;
    private String blkHseNo;
    private String floorNo;
    private String unitNo;
    private String bldgName;
    private String postalCode;
}
