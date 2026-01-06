package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsDriverNotice entities
 */
@Service
public class OcmsDriverNoticeServiceImpl extends BaseImplement<OcmsDriverNotice, OcmsDriverNoticeId, OcmsDriverNoticeRepository> 
        implements OcmsDriverNoticeService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsDriverNoticeServiceImpl(OcmsDriverNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}