package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspendedNotice;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface OcmsSuspendedNoticeRepository extends BaseRepository<OcmsSuspendedNotice, OcmsSuspendedNoticeId> {
    
    /**
     * Custom query to fetch successful records for the Success sheet.
     * Focuses ONLY on valid_offence_notice data with valid ID information.
     * This query does NOT reference the suspended_notice table at all.
     *
     * Filter criteria:
     * - suspension_type IS NULL, OR
     * - suspension_type = 'TS' AND epr_reason_of_suspension IN ('HST', 'UNC')
     *
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for successful records
     */
    @Query(value = """
            SELECT
                von.notice_no,
                von.last_processing_stage,
                von.next_processing_stage,
                von.next_processing_date,
                addr.processing_date_time,
                von.notice_date_and_time,
                von.epr_reason_of_suspension,
                nod.id_type,
                nod.id_no,
                nod.name,
                COALESCE(addr.bldg_name, '')   AS bldg_name,
                COALESCE(addr.blk_hse_no, '')  AS blk_hse_no,
                COALESCE(addr.street_name, '') AS street_name,
                COALESCE(addr.postal_code, '') AS postal_code,
                COALESCE(addr.floor_no, '')    AS floor_no,
                COALESCE(addr.unit_no, '')     AS unit_no
            FROM ocmsizmgr.ocms_offence_notice_owner_driver_addr AS addr
            JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS nod
                ON nod.notice_no = addr.notice_no
              AND nod.owner_driver_indicator = addr.owner_driver_indicator
            JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON von.notice_no = nod.notice_no
            WHERE addr.type_of_address = 'mha_reg'
              AND CONVERT(date, addr.processing_date_time) = :reportDate
              AND nod.id_no IS NOT NULL
              AND addr.address_type IS NOT NULL
              AND von.next_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3')
              AND (
                    von.suspension_type IS NULL
                    OR (von.suspension_type = 'TS' AND von.epr_reason_of_suspension IN ('HST', 'UNC'))
                  )
            ORDER BY von.notice_no
        """, nativeQuery = true)
    List<Map<String, Object>> findSuccessRecordsForReport(@Param("reportDate") String reportDate);
    
    /**
     * Custom query to fetch error records for the Error sheet.
     * Focuses on suspended_notice data with missing or invalid information.
     * 
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for error records
     */
    @Query(value =  """
            SELECT
                von.notice_no,
                von.suspension_type,
                von.due_date_of_revival,
                von.last_processing_stage,
                von.next_processing_stage,
                von.next_processing_date,
                von.notice_date_and_time,
                addr.processing_date_time,
                von.epr_reason_of_suspension,
                COALESCE(nod.id_type, '') AS id_type,
                COALESCE(nod.id_no, '')   AS id_no,
                COALESCE(nod.name, '')    AS name
            FROM ocmsizmgr.ocms_offence_notice_owner_driver_addr AS addr
            JOIN ocmsizmgr.ocms_offence_notice_owner_driver AS nod
                ON nod.notice_no = addr.notice_no
              AND nod.owner_driver_indicator = addr.owner_driver_indicator
            JOIN ocmsizmgr.ocms_valid_offence_notice AS von
                ON von.notice_no = nod.notice_no
            WHERE addr.type_of_address = 'mha_reg'
              AND nod.id_no IS NOT NULL
              AND addr.address_type IS NOT NULL
              AND CONVERT(date, addr.processing_date_time) = :reportDate
              AND (
                    (von.suspension_type = 'TS' AND von.epr_reason_of_suspension = 'NRO')
                OR (von.suspension_type = 'PS' AND von.epr_reason_of_suspension IN ('RIP', 'RP2'))
                  )
            ORDER BY von.notice_no
            """, nativeQuery = true)
    List<Map<String, Object>> findErrorRecordsForReport(@Param("reportDate") String reportDate);
    
    /**
     * Original combined query to fetch all suspended notices.
     * Kept for backward compatibility.
     * 
     * @param reportDate The date to filter by (format: yyyy-MM-dd)
     * @return List of maps containing the joined data for TS/PS notices with NRO/RP2 reason
     */
    @Query(value =
            "SELECT sn.notice_no, sn.sr_no, sn.suspension_type, sn.reason_of_suspension, " +
            "sn.date_of_suspension, sn.due_date_of_revival, sn.suspension_remarks, " +
            "von.next_processing_stage, von.next_processing_date, " +
            "von.epr_date_of_suspension, von.epr_reason_of_suspension, " +
            "nod.id_type, nod.id_no, nod.name, " +
            "addr.bldg_name, addr.blk_hse_no, addr.street_name, " +
            "addr.postal_code, addr.floor_no, addr.unit_no " +
            "FROM ocms_suspended_notice sn " +
            "JOIN ocms_valid_offence_notice von ON sn.notice_no = von.notice_no " +
            "LEFT JOIN ocms_offence_notice_owner_driver nod ON von.notice_no = nod.notice_no " +
            "LEFT JOIN ocms_offence_notice_owner_driver_addr addr ON nod.notice_no = addr.notice_no " +
            "AND nod.owner_driver_indicator = addr.owner_driver_indicator " +
            "WHERE sn.suspension_type IN ('TS', 'PS') AND sn.reason_of_suspension IN ('NRO', 'RP2') " +
            "AND CONVERT(date, sn.date_of_suspension) = :reportDate " +
            "ORDER BY sn.notice_no",
            nativeQuery = true)
    List<Map<String, Object>> findSuspendedNoticesForReport(@Param("reportDate") String reportDate);

    /**
     * PERFORMANCE OPTIMIZATION: Get maximum sr_no for a given notice_no
     * Uses optimized MAX query instead of retrieving all records
     *
     * @param noticeNo The notice number to query
     * @return Maximum sr_no for the notice, or null if no records exist
     */
    @Query("SELECT MAX(s.srNo) FROM OcmsSuspendedNotice s WHERE s.noticeNo = :noticeNo")
    Integer findMaxSrNoByNoticeNo(@Param("noticeNo") String noticeNo);

    /**
     * OCMS 14: Find PS-RP2 suspended notices where offender is Hirer/Driver
     * Daily RIP Hirer/Driver Furnished Report
     *
     * Query joins:
     * - ocms_suspended_notice (PS-RP2 suspensions)
     * - ocms_offence_notice_owner_driver (offender details - Hirer or Driver)
     * - ocms_valid_offence_notice (notice details)
     *
     * @param suspensionDate Date of suspension (format: yyyy-MM-dd)
     * @return List of maps containing report data
     */
    @Query(value = """
        SELECT
            sn.notice_no AS noticeNo,
            sn.date_of_suspension AS suspensionDate,
            sn.sr_no AS srNo,
            ond.name AS offenderName,
            ond.id_type AS idType,
            ond.id_no AS idNo,
            ond.owner_driver_indicator AS ownerDriverIndicator,
            von.notice_date_and_time AS noticeDate,
            von.computer_rule_code AS offenceRuleCode,
            von.composition_amount AS compositionAmount,
            von.amount_payable AS amountPayable,
            von.vehicle_no AS vehicleNo,
            von.pp_name AS ppName
        FROM ocmsizmgr.ocms_suspended_notice sn
        INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver ond
            ON ond.notice_no = sn.notice_no
            AND ond.offender_indicator = 'Y'
        INNER JOIN ocmsizmgr.ocms_valid_offence_notice von
            ON von.notice_no = sn.notice_no
        WHERE sn.suspension_type = 'PS'
          AND sn.reason_of_suspension = 'RP2'
          AND CAST(sn.date_of_suspension AS DATE) = CAST(:suspensionDate AS DATE)
          AND ond.owner_driver_indicator IN ('H', 'D')
        ORDER BY sn.notice_no ASC
        """, nativeQuery = true)
    List<Map<String, Object>> findPsRp2WithHirerDriverOffender(@Param("suspensionDate") String suspensionDate);

    /**
     * Find suspended notices by notice number and suspension type
     * Used by OCMS 41 auto-approval to check for active PS (Permanent Suspension)
     *
     * @param noticeNo Notice number
     * @param suspensionType Suspension type (PS, TS, etc.)
     * @return List of suspended notice records
     */
    List<OcmsSuspendedNotice> findByNoticeNoAndSuspensionType(String noticeNo, String suspensionType);
}
