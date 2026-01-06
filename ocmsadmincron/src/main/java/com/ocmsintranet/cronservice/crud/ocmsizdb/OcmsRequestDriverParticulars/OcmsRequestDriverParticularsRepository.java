package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OcmsRequestDriverParticulars entity
 */
@Repository
public interface OcmsRequestDriverParticularsRepository extends BaseRepository<OcmsRequestDriverParticulars, OcmsRequestDriverParticularsId> {
    
    /**
     * Find request driver particulars by composite primary key
     */
    Optional<OcmsRequestDriverParticulars> findById(OcmsRequestDriverParticularsId id);
    
    /**
     * Find all request driver particulars by notice number
     */
    List<OcmsRequestDriverParticulars> findByNoticeNo(String noticeNo);
    
    /**
     * Find all request driver particulars by processing stage
     */
    List<OcmsRequestDriverParticulars> findByProcessingStage(String processingStage);
    
    /**
     * Find all request driver particulars by date of processing
     */
    List<OcmsRequestDriverParticulars> findByDateOfProcessing(LocalDateTime dateOfProcessing);
    
    /**
     * Find all request driver particulars by owner NRIC number
     */
    List<OcmsRequestDriverParticulars> findByOwnerNricNo(String ownerNricNo);
    
    /**
     * Find all request driver particulars with reminder flag
     */
    List<OcmsRequestDriverParticulars> findByReminderFlag(String reminderFlag);
    
    /**
     * Find request driver particulars by processing stage and date range
     */
    @Query("SELECT rdp FROM OcmsRequestDriverParticulars rdp WHERE rdp.processingStage = :stage " +
           "AND rdp.dateOfProcessing BETWEEN :startDate AND :endDate")
    List<OcmsRequestDriverParticulars> findByProcessingStageAndDateRange(
            @Param("stage") String stage,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find unclaimed request driver particulars
     */
    @Query("SELECT rdp FROM OcmsRequestDriverParticulars rdp WHERE rdp.unclaimedReason IS NOT NULL")
    List<OcmsRequestDriverParticulars> findUnclaimedRequests();
    
    /**
     * Find request driver particulars by date of RDP
     */
    List<OcmsRequestDriverParticulars> findByDateOfRdp(LocalDateTime dateOfRdp);
    
    /**
     * Find request driver particulars that have been returned
     */
    @Query("SELECT rdp FROM OcmsRequestDriverParticulars rdp WHERE rdp.dateOfReturn IS NOT NULL")
    List<OcmsRequestDriverParticulars> findReturnedRequests();
}