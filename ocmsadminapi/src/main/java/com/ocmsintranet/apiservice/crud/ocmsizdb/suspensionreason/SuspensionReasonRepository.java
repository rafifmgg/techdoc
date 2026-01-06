package com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface SuspensionReasonRepository extends BaseRepository<SuspensionReason, SuspensionReasonId>{
    Optional<SuspensionReason> findById(SuspensionReasonId id);
    
    @Query("SELECT DISTINCT s.suspensionType FROM SuspensionReason s ORDER BY s.suspensionType")
    List<String> findDistinctSuspensionTypes();
}
