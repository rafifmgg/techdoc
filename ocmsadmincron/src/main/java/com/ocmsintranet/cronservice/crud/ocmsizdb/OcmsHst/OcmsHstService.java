package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.cronservice.crud.BaseService;

public interface OcmsHstService extends BaseService<OcmsHst, String> {

    /**
     * Check if an ID number exists in the HST (High Security Threat) database
     * Used by OCMS 41 auto-approval to check if furnished ID is flagged as HST
     *
     * @param idNo ID number to check
     * @return true if ID exists in HST database, false otherwise
     */
    boolean existsByIdNo(String idNo);
}