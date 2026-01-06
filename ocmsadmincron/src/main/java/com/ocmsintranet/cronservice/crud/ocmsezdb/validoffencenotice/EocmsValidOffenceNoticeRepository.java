package com.ocmsintranet.cronservice.crud.ocmsezdb.validoffencenotice;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for EocmsValidOffenceNotice entity (eVON)
 */
@Repository
public interface EocmsValidOffenceNoticeRepository extends BaseRepository<EocmsValidOffenceNotice, String> {
    // The base repository provides all necessary methods
    // Add custom query methods here if needed
}
