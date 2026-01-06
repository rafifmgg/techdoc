package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcmsNroTempRepository extends BaseRepository<OcmsNroTemp, Long> {

    /**
     * Find all unprocessed records by query reason
     * @param queryReason 'UNC' or 'HST'
     * @param processed false for unprocessed records
     * @return List of unprocessed OcmsNroTemp records
     */
    List<OcmsNroTemp> findByQueryReasonAndProcessed(String queryReason, Boolean processed);

    /**
     * Find all records by query reason
     * @param queryReason 'UNC' or 'HST'
     * @return List of OcmsNroTemp records
     */
    List<OcmsNroTemp> findByQueryReason(String queryReason);
}
