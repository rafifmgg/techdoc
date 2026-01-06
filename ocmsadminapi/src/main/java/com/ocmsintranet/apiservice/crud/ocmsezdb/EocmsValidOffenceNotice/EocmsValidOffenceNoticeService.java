package com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for EocmsValidOffenceNotice entities (Internet Database)
 */
public interface EocmsValidOffenceNoticeService extends BaseService<EocmsValidOffenceNotice, String> {

    /**
     * Find notices by vehicle number
     */
    List<EocmsValidOffenceNotice> findByVehicleNo(String vehicleNo);

    /**
     * Find notices by payment status
     */
    List<EocmsValidOffenceNotice> findByPaymentStatus(String paymentStatus);

    /**
     * Find notices by processing stage
     */
    List<EocmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage);

    /**
     * Find notices within date range
     */
    List<EocmsValidOffenceNotice> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find unpaid notices
     */
    List<EocmsValidOffenceNotice> findUnpaidNotices();

    /**
     * Get all with enhanced filtering and pagination
     */
    FindAllResponse<EocmsValidOffenceNotice> getAllWithFilters(Map<String, String[]> params);
}
