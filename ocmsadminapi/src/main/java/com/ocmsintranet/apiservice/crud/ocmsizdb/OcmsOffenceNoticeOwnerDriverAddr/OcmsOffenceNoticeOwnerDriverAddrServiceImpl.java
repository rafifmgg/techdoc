package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;

import java.util.List;

/**
 * Service implementation for OcmsOffenceNoticeOwnerDriverAddr operations
 */
@Service
@Transactional
public class OcmsOffenceNoticeOwnerDriverAddrServiceImpl
        extends BaseImplement<OcmsOffenceNoticeOwnerDriverAddr, OcmsOffenceNoticeOwnerDriverAddrId, OcmsOffenceNoticeOwnerDriverAddrRepository>
        implements OcmsOffenceNoticeOwnerDriverAddrService {

    public OcmsOffenceNoticeOwnerDriverAddrServiceImpl(OcmsOffenceNoticeOwnerDriverAddrRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
            List<String> noticeNos,
            List<String> ownerDriverIndicators,
            List<String> typeOfAddress) {
        return repository.findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
                noticeNos, ownerDriverIndicators, typeOfAddress);
    }
}
