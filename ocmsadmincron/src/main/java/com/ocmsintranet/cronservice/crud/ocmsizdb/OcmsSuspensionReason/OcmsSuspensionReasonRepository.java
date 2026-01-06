package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason;

import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.ocmsintranet.cronservice.crud.BaseRepository;

@Repository
public interface OcmsSuspensionReasonRepository extends BaseRepository<OcmsSuspensionReason, OcmsSuspensionReasonId> {
    Optional<OcmsSuspensionReason> findById(OcmsSuspensionReasonId id);
}
