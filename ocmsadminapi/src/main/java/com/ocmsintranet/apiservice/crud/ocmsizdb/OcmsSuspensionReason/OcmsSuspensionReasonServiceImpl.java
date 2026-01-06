package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Service
@Slf4j
public class OcmsSuspensionReasonServiceImpl 
    extends BaseImplement<OcmsSuspensionReason, OcmsSuspensionReasonId, OcmsSuspensionReasonRepository>
    implements OcmsSuspensionReasonService {
    
    public OcmsSuspensionReasonServiceImpl(OcmsSuspensionReasonRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    /**
     * Get the number of days for revival based on suspension type and reason
     * 
     * @param suspensionType The suspension type (e.g., "PS")
     * @param reasonOfSuspension The reason of suspension (e.g., "FOR", "ANS")
     * @return The number of days for revival, defaults to 7 if not found
     */
    @Override
    public Integer getNoOfDaysForRevival(String suspensionType, String reasonOfSuspension) {
        OcmsSuspensionReasonId id = new OcmsSuspensionReasonId();
        id.setSuspensionType(suspensionType);
        id.setReasonOfSuspension(reasonOfSuspension);
        
        Optional<OcmsSuspensionReason> reasonRecord = getById(id);
        return reasonRecord.map(OcmsSuspensionReason::getNoOfDaysForRevival).orElse(7); // Default to 7 days if not found
    }
}
