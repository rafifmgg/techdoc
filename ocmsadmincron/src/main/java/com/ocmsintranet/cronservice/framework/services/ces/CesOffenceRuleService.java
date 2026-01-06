package com.ocmsintranet.cronservice.framework.services.ces;

import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;

public interface CesOffenceRuleService {

    OcmsBatchJob executeCesOffenceRuleFunction(OcmsBatchJob batchJob);



}
