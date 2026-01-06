package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OcmsOffenceNoticeOwnerDriverAddr entity
 */
@Repository
public interface OcmsOffenceNoticeOwnerDriverAddrRepository extends BaseRepository<OcmsOffenceNoticeOwnerDriverAddr, OcmsOffenceNoticeOwnerDriverAddrId> {
    /**
     * Bulk find addresses for a set of notice numbers, owner/driver indicators and address types.
     * Note: This may return a superset due to separate IN clauses; callers should filter by exact key if needed.
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
            List<String> noticeNos,
            List<String> ownerDriverIndicators,
            List<String> typeOfAddress);

    /**
     * Find a specific address by notice number, owner/driver indicator, and address type
     * @param noticeNo Notice number
     * @param ownerDriverIndicator Owner/Driver indicator (O/H/D)
     * @param typeOfAddress Type of address (furnished_mail, etc.)
     * @return Optional containing the address if found
     */
    Optional<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(
            String noticeNo,
            String ownerDriverIndicator,
            String typeOfAddress);

    /**
     * Find all addresses for a notice number
     * @param noticeNo Notice number
     * @return List of addresses
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo);

    /**
     * Find addresses by notice number and owner/driver indicator
     * Used by ManualFurnishServiceImpl
     * @param noticeNo Notice number
     * @param ownerDriverIndicator Owner/Driver indicator
     * @return List of addresses
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoAndOwnerDriverIndicator(
            String noticeNo,
            String ownerDriverIndicator);

    /**
     * Find addresses by notice number and ID number
     * Used by ManualFurnishValidationService
     * @param noticeNo Notice number
     * @param idNo ID number
     * @return List of addresses
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoAndIdNo(
            String noticeNo,
            String idNo);
}
