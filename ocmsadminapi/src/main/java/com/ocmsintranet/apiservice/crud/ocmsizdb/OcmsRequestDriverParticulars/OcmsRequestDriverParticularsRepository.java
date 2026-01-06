package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OcmsRequestDriverParticulars entity
 */
 @Repository
 public interface OcmsRequestDriverParticularsRepository extends BaseRepository<OcmsRequestDriverParticulars, OcmsRequestDriverParticularsId> {
     /**
      * Find all RDP records for a given notice number
      * @param noticeNo Notice number
      * @return List of RDP records
      */
     List<OcmsRequestDriverParticulars> findByNoticeNo(String noticeNo);
 }