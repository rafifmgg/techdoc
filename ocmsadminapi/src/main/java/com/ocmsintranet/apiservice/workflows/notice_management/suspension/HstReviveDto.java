package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.Data;

/**
 * DTO for reviving HST suspensions
 * Maps to POST /v1/revive-hst request payload
 */
@Data
public class HstReviveDto {

    private String idNo;
    private String name;
    private String streetName;
    private String blkHseNo;
    private String floorNo;
    private String unitNo;
    private String bldgName;
    private String postalCode;
}
