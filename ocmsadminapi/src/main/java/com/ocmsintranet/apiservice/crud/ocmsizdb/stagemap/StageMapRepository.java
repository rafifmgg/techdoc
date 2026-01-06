package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for StageMap entity
 * Based on OCMS Data Dictionary
 */
@Repository
public interface StageMapRepository extends JpaRepository<StageMap, StageMapId>, JpaSpecificationExecutor<StageMap> {

    /**
     * Check if stage transition exists
     * Query: SELECT count(*) FROM ocms_stage_map
     * WHERE last_processing_stage = :lastStage
     * AND next_processing_stage LIKE '%:nextStage%'
     *
     * @param lastStage Last processing stage
     * @param nextStage Next processing stage
     * @return count of matching records
     */
    @Query("SELECT COUNT(s) FROM StageMap s " +
           "WHERE s.lastProcessingStage = :lastStage " +
           "AND s.nextProcessingStage LIKE CONCAT('%', :nextStage, '%')")
    long countByLastStageAndNextStageLike(
        @Param("lastStage") String lastStage,
        @Param("nextStage") String nextStage
    );

    /**
     * Find all stage maps by last processing stage
     * Query: SELECT * FROM ocms_stage_map
     * WHERE last_processing_stage = :lastStage
     *
     * @param lastStage Last processing stage
     * @return List of StageMap records
     */
    @Query("SELECT s FROM StageMap s " +
           "WHERE s.lastProcessingStage = :lastStage")
    java.util.List<StageMap> findByLastProcessingStage(
        @Param("lastStage") String lastStage
    );
}
