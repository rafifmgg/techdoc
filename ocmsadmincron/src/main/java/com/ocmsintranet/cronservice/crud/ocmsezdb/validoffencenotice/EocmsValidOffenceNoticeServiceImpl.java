package com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for EocmsValidOffenceNotice entities (eVON)
 */
@Service
public class EocmsValidOffenceNoticeServiceImpl
        extends BaseImplement<EocmsValidOffenceNotice, String, EocmsValidOffenceNoticeRepository>
        implements EocmsValidOffenceNoticeService {

    /**
     * Constructor with required dependencies
     */
    public EocmsValidOffenceNoticeServiceImpl(EocmsValidOffenceNoticeRepository repository,
                                               DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
