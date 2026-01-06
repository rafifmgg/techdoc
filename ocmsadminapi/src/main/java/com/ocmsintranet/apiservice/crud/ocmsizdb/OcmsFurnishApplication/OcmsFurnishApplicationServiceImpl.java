package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OcmsFurnishApplicationServiceImpl extends BaseImplement<OcmsFurnishApplication, String, OcmsFurnishApplicationRepository>
        implements OcmsFurnishApplicationService {

    public OcmsFurnishApplicationServiceImpl(OcmsFurnishApplicationRepository repository, DatabaseRetryService retryService) {
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

    @Override
    public List<OcmsFurnishApplication> findByStatusIn(List<String> statuses) {
        return repository.findByStatusIn(statuses);
    }
}
