package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.OffenceNoticeWithOwnerDto;

public interface OcmsValidOffenceNoticeService extends BaseService<OcmsValidOffenceNotice, String> {
    /**
     * Get all offence notices with owner information
     * @param params Query parameters
     * @return Paginated response with offence notices and owner details
     */
    FindAllResponse<OffenceNoticeWithOwnerDto> getAllWithOwnerInfo(Map<String, String[]> params);

    OcmsValidOffenceNotice patchWithFiles(String noticeNo, OcmsValidOffenceNotice partialEntity, Map<String, Object> payload);

    /**
     * Find notices by vehicle number
     * @param vehicleNo Vehicle number
     * @return List of notices
     */
    List<OcmsValidOffenceNotice> findByVehicleNo(String vehicleNo);

    /**
     * Find notices by last processing stage
     * @param lastProcessingStage Last processing stage
     * @return List of notices
     */
    List<OcmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage);

    /**
     * Find notices by last processing stage and date
     * @param lastProcessingStage Last processing stage
     * @param date Date
     * @return List of notices
     */
    List<OcmsValidOffenceNotice> findByLastProcessingStageAndDate(String lastProcessingStage, LocalDate date);

}