package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import com.ocmsintranet.apiservice.crud.BaseService;

public interface OcmsSuspensionReasonService extends BaseService<OcmsSuspensionReason, OcmsSuspensionReasonId> {
    
    /**
     * Get the number of days for revival based on suspension type and reason
     * 
     * @param suspensionType The suspension type (e.g., "PS")
     * @param reasonOfSuspension The reason of suspension (e.g., "FOR", "ANS")
     * @return The number of days for revival, defaults to 7 if not found
     */
    Integer getNoOfDaysForRevival(String suspensionType, String reasonOfSuspension);
}
