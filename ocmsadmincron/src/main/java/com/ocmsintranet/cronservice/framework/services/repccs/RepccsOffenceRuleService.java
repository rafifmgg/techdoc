package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;

public interface RepccsOffenceRuleService {

    OcmsBatchJob executeUpdatedOffenceRuleFunction(OcmsBatchJob batchJob);
    
    /**
     * Get the count of updated offence rules that would be processed
     * 
     * @return count of records, or -1 if an error occurs
     */
    int getUpdatedOffenceRuleCount();
}
