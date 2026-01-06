package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsNroTemp;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsNroTempRepository extends BaseRepository<OcmsNroTemp, String> {
    // No custom methods - using only methods from BaseRepository
}