package com.ocmsintranet.apiservice.crud.ocmsizdb.standardcode;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface StandardCodeRepository extends BaseRepository<StandardCode, StandardCodeId>{
    Optional<StandardCode> findById(StandardCodeId id);

}
