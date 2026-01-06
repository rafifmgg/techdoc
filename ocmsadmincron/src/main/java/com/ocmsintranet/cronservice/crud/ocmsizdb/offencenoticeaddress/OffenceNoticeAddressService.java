package com.ocmsintranet.cronservice.crud.ocmsizdb.offencenoticeaddress;

import com.ocmsintranet.cronservice.crud.BaseService;
import java.util.List;

/**
 * Service interface for OffenceNoticeAddress entity
 */
public interface OffenceNoticeAddressService extends BaseService<OffenceNoticeAddress, OffenceNoticeAddressId> {
    
    /**
     * Find all addresses by notice number
     * 
     * @param noticeNo the notice number
     * @return list of addresses for the notice
     */
    List<OffenceNoticeAddress> findByNoticeNo(String noticeNo);
    
    /**
     * Find all addresses by notice number and owner/driver indicator
     * 
     * @param noticeNo the notice number
     * @param ownerDriverIndicator the owner/driver indicator
     * @return list of addresses for the notice and owner/driver
     */
    List<OffenceNoticeAddress> findByNoticeNoAndOwnerDriverIndicator(String noticeNo, String ownerDriverIndicator);
    
    /**
     * Find all addresses by notice number, owner/driver indicator and type of address
     * 
     * @param noticeNo the notice number
     * @param ownerDriverIndicator the owner/driver indicator
     * @param typeOfAddress the type of address (lta_reg/lta_mail/mha_reg/furnished_mail)
     * @return list of addresses matching the criteria
     */
    List<OffenceNoticeAddress> findByNoticeNoAndOwnerDriverIndicatorAndTypeOfAddress(
            String noticeNo, String ownerDriverIndicator, String typeOfAddress);
    
    // Save and delete methods are inherited from BaseService
}
