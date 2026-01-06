package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEnotificationExclusionList;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsEnotificationExclusionList entities
 */
@Service
public class OcmsEnotificationExclusionListServiceImpl extends BaseImplement<OcmsEnotificationExclusionList, String, OcmsEnotificationExclusionListRepository>
        implements OcmsEnotificationExclusionListService {

    /**
     * Constructor with required dependencies
     */
    public OcmsEnotificationExclusionListServiceImpl(OcmsEnotificationExclusionListRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
