package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for OcmsChangeOfProcessing entity
 * Based on OCMS CPS Spec §4.6
 *
 * Uses composite primary key (notice_no, date_of_change, new_processing_stage)
 */
@Repository
public interface OcmsChangeOfProcessingRepository extends JpaRepository<OcmsChangeOfProcessing, OcmsChangeOfProcessing.CompositeKey> {

    /**
     * Check if a change already exists for the same notice, new stage, and date
     * Used for idempotency checks (per SGT-day)
     * Based on OCMS CPS Spec §6
     *
     * @param noticeNo Notice number
     * @param newProcessingStage New processing stage
     * @param dateOfChange Date of change (SGT date)
     * @return true if exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM OcmsChangeOfProcessing c " +
           "WHERE c.noticeNo = :noticeNo " +
           "AND c.newProcessingStage = :newProcessingStage " +
           "AND c.dateOfChange = :dateOfChange")
    boolean existsByNoticeNoAndNewStageAndDate(
        @Param("noticeNo") String noticeNo,
        @Param("newProcessingStage") String newProcessingStage,
        @Param("dateOfChange") LocalDate dateOfChange
    );

    /**
     * Find all change records for a specific date
     * Used for report generation
     */
    java.util.List<OcmsChangeOfProcessing> findByDateOfChange(LocalDate dateOfChange);

    /**
     * Find all change records between date range
     * Used for report retrieval (BE-007)
     */
    @Query("SELECT c FROM OcmsChangeOfProcessing c " +
           "WHERE c.creDate >= :startDate AND c.creDate < :endDate " +
           "ORDER BY c.creDate DESC")
    java.util.List<OcmsChangeOfProcessing> findByCreDateBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
}
