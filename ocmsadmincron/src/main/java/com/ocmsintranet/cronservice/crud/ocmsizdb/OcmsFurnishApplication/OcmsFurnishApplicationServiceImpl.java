package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for OcmsFurnishApplication entities (Intranet DB)
 */
@Service
public class OcmsFurnishApplicationServiceImpl
        extends BaseImplement<OcmsFurnishApplication, String, OcmsFurnishApplicationRepository>
        implements OcmsFurnishApplicationService {

    /**
     * Constructor with required dependencies
     */
    public OcmsFurnishApplicationServiceImpl(OcmsFurnishApplicationRepository repository,
                                              DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsFurnishApplication> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }

    @Override
    public List<OcmsFurnishApplication> findByStatus(String status) {
        return repository.findByStatus(status);
    }
}
