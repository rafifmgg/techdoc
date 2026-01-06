package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service implementation for OcmsSuspendedNotice entities
 */
@Service
public class OcmsSuspendedNoticeServiceImpl extends BaseImplement<OcmsSuspendedNotice, OcmsSuspendedNoticeId, OcmsSuspendedNoticeRepository>
        implements OcmsSuspendedNoticeService {

    private final OcmsSuspendedNoticeRepository suspendedNoticeRepository;

    /**
     * Constructor with required dependencies
     */
    public OcmsSuspendedNoticeServiceImpl(OcmsSuspendedNoticeRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
        this.suspendedNoticeRepository = repository;
    }
    
    @Override
    public List<Map<String, Object>> getSuccessRecordsForReport(String reportDate) {
        return suspendedNoticeRepository.findSuccessRecordsForReport(reportDate);
    }
    
    @Override
    public List<Map<String, Object>> getErrorRecordsForReport(String reportDate) {
        return suspendedNoticeRepository.findErrorRecordsForReport(reportDate);
    }
    
    @Override
    public List<Map<String, Object>> getSuspendedNoticesForReport(String reportDate) {
        return suspendedNoticeRepository.findSuspendedNoticesForReport(reportDate);
    }

    @Override
    public Integer findMaxSrNoByNoticeNo(String noticeNo) {
        return suspendedNoticeRepository.findMaxSrNoByNoticeNo(noticeNo);
    }

    @Override
    public List<OcmsSuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType) {
        return suspendedNoticeRepository.findByNoticeNoAndSuspensionType(noticeNo, suspensionType);
    }
}
