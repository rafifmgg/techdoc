package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.apiservice.crud.BaseService;

import java.util.List;

/**
 * Service interface for OcmsOffenceNoticeOwnerDriverAddr operations
 */
public interface OcmsOffenceNoticeOwnerDriverAddrService extends BaseService<OcmsOffenceNoticeOwnerDriverAddr, OcmsOffenceNoticeOwnerDriverAddrId> {

    /**
     * Find all addresses for a notice number
     * @param noticeNo Notice number
     * @return List of addresses
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo);

    /**
     * Bulk find addresses for a set of notice numbers, owner/driver indicators and address types.
     * Used for efficient batch loading when fetching multiple owner/driver records with addresses.
     *
     * @param noticeNos List of notice numbers
     * @param ownerDriverIndicators List of owner/driver indicators (O/H/D)
     * @param typeOfAddress List of address types (lta_reg, lta_mail, mha_reg, furnished_mail)
     * @return List of matching addresses
     */
    List<OcmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
            List<String> noticeNos,
            List<String> ownerDriverIndicators,
            List<String> typeOfAddress);
}
