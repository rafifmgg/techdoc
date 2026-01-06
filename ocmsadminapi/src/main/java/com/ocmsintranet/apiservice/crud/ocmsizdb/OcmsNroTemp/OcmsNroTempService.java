package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.apiservice.crud.BaseService;

import java.util.List;

public interface OcmsNroTempService extends BaseService<OcmsNroTemp, Long> {

    /**
     * Find all unprocessed records by query reason
     * @param queryReason 'UNC' or 'HST'
     * @return List of unprocessed OcmsNroTemp records
     */
    List<OcmsNroTemp> findUnprocessedByQueryReason(String queryReason);

    /**
     * Find all records by query reason
     * @param queryReason 'UNC' or 'HST'
     * @return List of OcmsNroTemp records
     */
    List<OcmsNroTemp> findByQueryReason(String queryReason);
}
