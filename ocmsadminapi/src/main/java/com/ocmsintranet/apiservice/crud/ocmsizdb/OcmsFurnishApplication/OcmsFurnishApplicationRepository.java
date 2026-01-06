package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface OcmsFurnishApplicationRepository extends BaseRepository<OcmsFurnishApplication, String> {

    /**
     * Find all furnish applications by notice number
     */
    List<OcmsFurnishApplication> findByNoticeNo(String noticeNo);

    /**
     * Find all furnish applications by status
     */
    List<OcmsFurnishApplication> findByStatus(String status);

    /**
     * Find all furnish applications by status (for pending approval workflow)
     */
    List<OcmsFurnishApplication> findByStatusIn(List<String> statuses);

    /**
     * OCMS 20: Ad-hoc HST Hirer/Driver Furnished Report (Section 3.6.3)
     * Query furnish applications where offender is Hirer (H) or Driver (D) and is in HST table
     *
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return List of records with furnish application data + HST address data
     */
    @Query(value = """
        SELECT
            f.txn_no AS txnNo,
            f.notice_no AS noticeNo,
            f.vehicle_no AS vehicleNo,
            f.offence_date AS offenceDate,
            f.pp_code AS ppCode,
            f.pp_name AS ppName,
            f.last_processing_stage AS lastProcessingStage,
            f.furnish_name AS furnishName,
            f.furnish_id_type AS furnishIdType,
            f.furnish_id_no AS furnishIdNo,
            f.furnish_mail_blk_no AS furnishBlkNo,
            f.furnish_mail_floor AS furnishFloor,
            f.furnish_mail_street_name AS furnishStreetName,
            f.furnish_mail_unit_no AS furnishUnitNo,
            f.furnish_mail_bldg_name AS furnishBldgName,
            f.furnish_mail_postal_code AS furnishPostalCode,
            f.furnish_tel_code AS furnishTelCode,
            f.furnish_tel_no AS furnishTelNo,
            f.furnish_email_addr AS furnishEmail,
            f.owner_driver_indicator AS ownerDriverIndicator,
            f.hirer_owner_relationship AS hirerOwnerRelationship,
            f.created_date AS furnishedDate,
            h.id_no AS hstIdNo,
            h.id_type AS hstIdType,
            h.name AS hstName,
            h.street_name AS hstStreetName,
            h.blk_hse_no AS hstBlkNo,
            h.floor_no AS hstFloorNo,
            h.unit_no AS hstUnitNo,
            h.bldg_name AS hstBldgName,
            h.postal_code AS hstPostalCode
        FROM ocmsizmgr.ocms_furnish_application f
        INNER JOIN ocmsizmgr.ocms_hst h ON h.id_no = f.furnish_id_no
        WHERE f.owner_driver_indicator IN ('H', 'D')
            AND (:startDate IS NULL OR CAST(f.created_date AS DATE) >= :startDate)
            AND (:endDate IS NULL OR CAST(f.created_date AS DATE) <= :endDate)
        ORDER BY f.created_date DESC
        """, nativeQuery = true)
    List<Map<String, Object>> findHirerDriverFurnishedByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * OCMS 20: Ad-hoc Alternative Address Furnished Report (Section 3.6.4)
     * Query furnish applications where address source is eService (ES) or LTA
     *
     * Note: The specification mentions eService/LTA sources. We need to determine
     * how to identify these sources in the data. For now, we filter by:
     * - Records that have valid furnish data (non-null postal code)
     * - Join with HST table to show comparison
     *
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return List of records with furnish application data + HST address data
     */
    @Query(value = """
        SELECT
            f.txn_no AS txnNo,
            f.notice_no AS noticeNo,
            f.vehicle_no AS vehicleNo,
            f.offence_date AS offenceDate,
            f.pp_code AS ppCode,
            f.pp_name AS ppName,
            f.last_processing_stage AS lastProcessingStage,
            f.furnish_name AS furnishName,
            f.furnish_id_type AS furnishIdType,
            f.furnish_id_no AS furnishIdNo,
            f.furnish_mail_blk_no AS furnishBlkNo,
            f.furnish_mail_floor AS furnishFloor,
            f.furnish_mail_street_name AS furnishStreetName,
            f.furnish_mail_unit_no AS furnishUnitNo,
            f.furnish_mail_bldg_name AS furnishBldgName,
            f.furnish_mail_postal_code AS furnishPostalCode,
            f.furnish_tel_code AS furnishTelCode,
            f.furnish_tel_no AS furnishTelNo,
            f.furnish_email_addr AS furnishEmail,
            f.owner_driver_indicator AS ownerDriverIndicator,
            f.created_date AS furnishedDate,
            h.id_no AS hstIdNo,
            h.id_type AS hstIdType,
            h.name AS hstName,
            h.street_name AS hstStreetName,
            h.blk_hse_no AS hstBlkNo,
            h.floor_no AS hstFloorNo,
            h.unit_no AS hstUnitNo,
            h.bldg_name AS hstBldgName,
            h.postal_code AS hstPostalCode
        FROM ocmsizmgr.ocms_furnish_application f
        INNER JOIN ocmsizmgr.ocms_hst h ON h.id_no = f.furnish_id_no
        WHERE f.furnish_mail_postal_code IS NOT NULL
            AND (:startDate IS NULL OR CAST(f.created_date AS DATE) >= :startDate)
            AND (:endDate IS NULL OR CAST(f.created_date AS DATE) <= :endDate)
        ORDER BY f.created_date DESC
        """, nativeQuery = true)
    List<Map<String, Object>> findAlternativeAddressByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
