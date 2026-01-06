package com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

/**
 * Service interface for EocmsOffenceNoticeOwnerDriverAddr entity.
 * Handles encrypted PII address data operations.
 */
public interface EocmsOffenceNoticeOwnerDriverAddrService
        extends BaseService<EocmsOffenceNoticeOwnerDriverAddr, EocmsOffenceNoticeOwnerDriverAddrId> {

    /**
     * Find all addresses for a specific notice
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo);

    /**
     * Find all addresses for specific notices, indicators, and types (for bulk fetching)
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
        List<String> noticeNos, List<String> indicators, List<String> types);
}