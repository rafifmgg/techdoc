package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;

import java.util.List;
import java.util.Map;

public interface OcmsOffenceNoticeOwnerDriverService extends BaseService<OcmsOffenceNoticeOwnerDriver, OcmsOffenceNoticeOwnerDriverId> {
    // Custom combined response with flattened address fields
    FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto> getAllWithAddresses(Map<String, String[]> params);

    /**
     * Find all ONOD records by notice number
     * @param noticeNo Notice number
     * @return List of ONOD records
     */
    List<OcmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo);

    /**
     * Find all ONOD records by ID number
     * @param idNo ID number
     * @return List of ONOD records
     */
    List<OcmsOffenceNoticeOwnerDriver> findByIdNo(String idNo);
}
