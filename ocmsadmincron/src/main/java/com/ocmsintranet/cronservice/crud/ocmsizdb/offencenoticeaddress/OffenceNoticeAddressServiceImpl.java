package com.ocmsintranet.cronservice.crud.ocmsizdb.offencenoticeaddress;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for OffenceNoticeAddress entity
 */
@Service
@Transactional
public class OffenceNoticeAddressServiceImpl extends BaseImplement<OffenceNoticeAddress, OffenceNoticeAddressId, OffenceNoticeAddressRepository>
        implements OffenceNoticeAddressService {
    
    private final OffenceNoticeAddressRepository offenceNoticeAddressRepository;
    
    public OffenceNoticeAddressServiceImpl(OffenceNoticeAddressRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
        this.offenceNoticeAddressRepository = repository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OffenceNoticeAddress> findByNoticeNo(String noticeNo) {
        return offenceNoticeAddressRepository.findByNoticeNo(noticeNo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OffenceNoticeAddress> findByNoticeNoAndOwnerDriverIndicator(String noticeNo, String ownerDriverIndicator) {
        return offenceNoticeAddressRepository.findByNoticeNoAndOwnerDriverIndicator(noticeNo, ownerDriverIndicator);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OffenceNoticeAddress> findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(
            String noticeNo, String ownerDriverIndicator, String typeOfAddress) {
        return offenceNoticeAddressRepository.findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(
                noticeNo, ownerDriverIndicator, typeOfAddress);
    }
    
    // BaseImplement already provides save and delete methods
}
