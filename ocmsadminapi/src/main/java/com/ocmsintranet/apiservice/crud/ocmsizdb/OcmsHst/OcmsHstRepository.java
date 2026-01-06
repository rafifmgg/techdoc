package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHst;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsHstRepository extends BaseRepository<OcmsHst, String> {
    // No custom methods - using only methods from BaseRepository
}
