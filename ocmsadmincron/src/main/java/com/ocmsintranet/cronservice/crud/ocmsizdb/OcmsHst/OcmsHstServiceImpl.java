package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsHst entities
 */
@Service
public class OcmsHstServiceImpl extends BaseImplement<OcmsHst, String, OcmsHstRepository>
        implements OcmsHstService {

    private final OcmsHstRepository hstRepository;

    /**
     * Constructor with required dependencies
     */
    public OcmsHstServiceImpl(OcmsHstRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
        this.hstRepository = repository;
    }

    @Override
    public boolean existsByIdNo(String idNo) {
        return hstRepository.existsByIdNo(idNo);
    }
}