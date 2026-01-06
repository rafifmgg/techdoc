package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsHstRepository extends BaseRepository<OcmsHst, String> {

    /**
     * Check if an ID number exists in the HST database
     * Used by OCMS 41 auto-approval to check if furnished ID is flagged as HST
     *
     * @param idNo ID number to check
     * @return true if ID exists, false otherwise
     */
    boolean existsByIdNo(String idNo);
}