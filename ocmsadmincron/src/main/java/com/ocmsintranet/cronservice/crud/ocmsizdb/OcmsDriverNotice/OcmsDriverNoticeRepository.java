package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsDriverNotice;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OcmsDriverNotice entity
 */
@Repository
public interface OcmsDriverNoticeRepository extends BaseRepository<OcmsDriverNotice, OcmsDriverNoticeId> {
    
    /**
     * Find driver notice by composite primary key
     */
    Optional<OcmsDriverNotice> findById(OcmsDriverNoticeId id);
    
    /**
     * Find all driver notices by notice number
     */
    List<OcmsDriverNotice> findByNoticeNo(String noticeNo);
    
    /**
     * Find all driver notices by processing stage
     */
    List<OcmsDriverNotice> findByProcessingStage(String processingStage);
    
    /**
     * Find all driver notices by date of processing
     */
    List<OcmsDriverNotice> findByDateOfProcessing(LocalDateTime dateOfProcessing);
    
    /**
     * Find all driver notices by driver NRIC number
     */
    List<OcmsDriverNotice> findByDriverNricNo(String driverNricNo);
    
    /**
     * Find all driver notices with reminder flag
     */
    List<OcmsDriverNotice> findByReminderFlag(String reminderFlag);
    
    /**
     * Find driver notices by processing stage and date range
     */
    @Query("SELECT dn FROM OcmsDriverNotice dn WHERE dn.processingStage = :stage " +
           "AND dn.dateOfProcessing BETWEEN :startDate AND :endDate")
    List<OcmsDriverNotice> findByProcessingStageAndDateRange(
            @Param("stage") String stage,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find unclaimed driver notices
     */
    @Query("SELECT dn FROM OcmsDriverNotice dn WHERE dn.reasonForUnclaim IS NOT NULL")
    List<OcmsDriverNotice> findUnclaimedNotices();
}