package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;

public interface RepccsListedVehOcmsToRepccsService {

    OcmsBatchJob executeListedVehOcmsToRepccsFunction(OcmsBatchJob batchJob);
}
