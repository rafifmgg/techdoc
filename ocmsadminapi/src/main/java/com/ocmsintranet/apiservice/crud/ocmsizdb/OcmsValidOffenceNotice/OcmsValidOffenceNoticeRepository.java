package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OcmsValidOffenceNoticeRepository extends BaseRepository<OcmsValidOffenceNotice, String> {

    /**
     * Find a valid offence notice by notice number.
     * Since noticeNo is the primary key, this is equivalent to findById.
     *
     * @param noticeNo The notice number to search for
     * @return Optional containing the notice if found
     */
    default Optional<OcmsValidOffenceNotice> findByNoticeNo(String noticeNo) {
        return findById(noticeNo);
    }
    
    /**
     * Find valid offence notices by joining with owner/driver information
     * This allows filtering by fields from the OcmsOffenceNoticeOwnerDriver table
     */
    @Query("SELECT DISTINCT v FROM OcmsValidOffenceNotice v " +
           "JOIN com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver o ON v.noticeNo = o.id.noticeNo " +
           "WHERE (:offenderIndicator IS NULL OR o.offenderIndicator = :offenderIndicator) " +
           "AND (:ownerDriverIndicator IS NULL OR o.id.ownerDriverIndicator = :ownerDriverIndicator) " +
           "AND (:idNo IS NULL OR o.idNo = :idNo) " +
           "AND (:idType IS NULL OR o.idType = :idType) " +
           "AND (:name IS NULL OR o.name LIKE CONCAT('%', :name, '%'))")
    List<OcmsValidOffenceNotice> findByOwnerDriverInfo(
        @Param("offenderIndicator") String offenderIndicator,
        @Param("ownerDriverIndicator") String ownerDriverIndicator,
        @Param("idNo") String idNo,
        @Param("idType") String idType,
        @Param("name") String name
    );

    @Query(value = """
    SELECT count(*)
    FROM ocmsizmgr.ocms_valid_offence_notice
    WHERE vehicle_no = :vehicleNo
      AND notice_date_and_time >= DATEADD(YEAR, -2, GETDATE())
    """, nativeQuery = true)
    int existsByVehicleNoAndNoticeDateTimeWithinLast2Years(@Param("vehicleNo") String vehicleNo);

    /**
     * Find notices by vehicle number
     * @param vehicleNo Vehicle number
     * @return List of notices
     */
    List<OcmsValidOffenceNotice> findByVehicleNo(String vehicleNo);

    /**
     * Find notices by current processing stage
     * @param lastProcessingStage Last processing stage
     * @return List of notices
     */
    List<OcmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage);

    /**
     * Find notices by stage and date
     * @param lastProcessingStage Last processing stage
     * @param lastProcessingDate Last processing date
     * @return List of notices
     */
    @Query("SELECT v FROM OcmsValidOffenceNotice v WHERE v.lastProcessingStage = :stage " +
           "AND CAST(v.lastProcessingDate AS LocalDate) = :date")
    List<OcmsValidOffenceNotice> findByLastProcessingStageAndDate(
        @Param("stage") String lastProcessingStage,
        @Param("date") java.time.LocalDate lastProcessingDate
    );
}