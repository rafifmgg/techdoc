package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for OcmsOffenceNoticeOwnerDriver entities
 */
@Service
public class OcmsOffenceNoticeOwnerDriverServiceImpl extends BaseImplement<OcmsOffenceNoticeOwnerDriver, OcmsOffenceNoticeOwnerDriverId, OcmsOffenceNoticeOwnerDriverRepository>
        implements OcmsOffenceNoticeOwnerDriverService {

    /**
     * Constructor with required dependencies
     */
    public OcmsOffenceNoticeOwnerDriverServiceImpl(OcmsOffenceNoticeOwnerDriverRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsOffenceNoticeOwnerDriver> findByIsSync(String isSync) {
        return repository.findByIsSync(isSync);
    }

    @Override
    public List<OcmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }
}