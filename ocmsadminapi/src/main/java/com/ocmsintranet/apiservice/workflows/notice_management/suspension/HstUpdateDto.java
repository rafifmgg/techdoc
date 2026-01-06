package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.Data;

/**
 * DTO for updating HST record
 * Maps to POST /v1/update-hst/{id} request payload
 */
@Data
public class HstUpdateDto {

    private String name;
    private String streetName;
    private String blkHseNo;
    private String floorNo;
    private String unitNo;
    private String bldgName;
    private String postalCode;
}
