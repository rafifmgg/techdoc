package com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for EocmsValidOffenceNotice entities (Internet Database)
 */
@Service
public class EocmsValidOffenceNoticeServiceImpl extends BaseImplement<EocmsValidOffenceNotice, String, EocmsValidOffenceNoticeRepository>
        implements EocmsValidOffenceNoticeService {

    /**
     * Constructor with required dependencies
     */
    public EocmsValidOffenceNoticeServiceImpl(EocmsValidOffenceNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<EocmsValidOffenceNotice> findByVehicleNo(String vehicleNo) {
        return retryService.executeWithRetry(() -> repository.findByVehicleNo(vehicleNo));
    }

    @Override
    public List<EocmsValidOffenceNotice> findByPaymentStatus(String paymentStatus) {
        return retryService.executeWithRetry(() -> repository.findByPaymentStatus(paymentStatus));
    }

    @Override
    public List<EocmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage) {
        return retryService.executeWithRetry(() -> repository.findByLastProcessingStage(lastProcessingStage));
    }

    @Override
    public List<EocmsValidOffenceNotice> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return retryService.executeWithRetry(() -> repository.findByDateRange(startDate, endDate));
    }

    @Override
    public List<EocmsValidOffenceNotice> findUnpaidNotices() {
        return retryService.executeWithRetry(() -> repository.findUnpaidNotices());
    }

    @Override
    public FindAllResponse<EocmsValidOffenceNotice> getAllWithFilters(Map<String, String[]> params) {
        // Enhanced filtering logic can be implemented here similar to OCMS version
        // For now, use the base implementation
        return super.getAll(params);
    }
}
