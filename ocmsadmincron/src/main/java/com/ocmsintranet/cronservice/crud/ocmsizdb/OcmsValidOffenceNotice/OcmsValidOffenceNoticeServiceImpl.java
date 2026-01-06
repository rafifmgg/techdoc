package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for OcmsValidOffenceNotice entities
 */
@Service
public class OcmsValidOffenceNoticeServiceImpl extends BaseImplement<OcmsValidOffenceNotice, String, OcmsValidOffenceNoticeRepository>
        implements OcmsValidOffenceNoticeService {

    /**
     * Constructor with required dependencies
     */
    public OcmsValidOffenceNoticeServiceImpl(OcmsValidOffenceNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    public List<OcmsValidOffenceNotice> findByNoticeNoIn(List<String> noticeNumbers) {
        return repository.findByNoticeNoIn(noticeNumbers);
    }

    @Override
    public List<OcmsValidOffenceNotice> findByIsSync(String isSync) {
        return repository.findByIsSync(isSync);
    }
}