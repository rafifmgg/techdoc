package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDhMhaPass;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsDhMhaPassRepository extends BaseRepository<OcmsDhMhaPass, OcmsDhMhaPassId> {
    // No custom methods - using only methods from BaseRepository
}