package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for EocmsOffenceNoticeOwnerDriverAddr entity.
 * Handles database operations for encrypted PII address data.
 */
@Repository
public interface EocmsOffenceNoticeOwnerDriverAddrRepository
    extends BaseRepository<EocmsOffenceNoticeOwnerDriverAddr, EocmsOffenceNoticeOwnerDriverAddrId> {

    /**
     * Find all addresses for a specific notice
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo);

    /**
     * Find all addresses for a specific notice and person
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNoAndOwnerDriverIndicator(
        String noticeNo, String ownerDriverIndicator);

    /**
     * Find specific type of address for a notice
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNoAndTypeOfAddress(
        String noticeNo, String typeOfAddress);

    /**
     * Find addresses by multiple criteria (bulk fetch)
     */
    List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
        List<String> noticeNos, List<String> ownerDriverIndicators, List<String> typeOfAddresses);
}