package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason;

import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface OcmsSuspensionReasonRepository extends BaseRepository<OcmsSuspensionReason, OcmsSuspensionReasonId> {
    Optional<OcmsSuspensionReason> findById(OcmsSuspensionReasonId id);
}
