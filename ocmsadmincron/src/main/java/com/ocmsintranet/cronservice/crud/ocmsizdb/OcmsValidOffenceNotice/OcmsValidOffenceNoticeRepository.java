package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for querying valid offence notices for various workflows
 */

@Repository
public interface OcmsValidOffenceNoticeRepository extends BaseRepository<OcmsValidOffenceNotice, String> {

    /**
     * Find comprehensive LTA upload data by joining multiple tables to get all required fields
     * Hardcoded to look for next_processing_stage = 'ROV' and next_processing_date <= current date
     * 
     * @return List of LtaUploadData DTOs containing all required fields for LTA file generation
     */
    @Query(value = """
        SELECT 
            v.notice_no as noticeNo,
            v.vehicle_no as vehicleNo,
            v.notice_date_and_time as noticeDateAndTime,
            v.next_processing_stage as nextProcessingStage,
            v.next_processing_date as nextProcessingDate,
            v.suspension_type as suspensionType
        FROM ocmsizmgr.ocms_valid_offence_notice v
        WHERE v.next_processing_stage = 'ROV' 
        AND CAST(v.next_processing_date AS DATE) <= CAST(GETDATE() AS DATE)
        AND v.offence_notice_type IN ('O', 'E')
        AND (
            v.suspension_type IS NULL 
            OR v.suspension_type != 'PS'
            OR (v.suspension_type = 'PS' AND ISNULL(v.epr_reason_of_suspension, '') != 'FOR')
        )
        AND (
            v.suspension_type IS NULL 
            OR v.suspension_type != 'PS'
            OR ISNULL(v.epr_reason_of_suspension, '') != 'ANS'
            OR v.subsystem_label != '001'
        )
        AND NOT (
            v.suspension_type = 'PS'
            AND ISNULL(v.crs_reason_of_suspension, '') IN ('FP', 'PRA')
        )
        ORDER BY v.next_processing_date ASC
        """, nativeQuery = true)
    List<Object[]> findComprehensiveLtaUploadData();
    
    /**
     * Find records that need initial SMS/email notifications
     * Joins ocms_valid_offence_notice with ocms_offence_notice_owner_driver to get contact information
     * Filters for records where:
     * - last_processing_stage = 'ENA'
     * - last_processing_date <= current date
     * - Either suspension_type IS NULL or (suspension_type = 'TS' AND epr_reason_of_suspension = 'HST')
     * - nric_no not in the exclusion list
     * - offender_indicator = 'Y'
     * 
     * @return List of records for initial SMS/email notifications with contact information
     */
    /**
     * Find records that need initial SMS/email notifications
     * CORRECTED: Using id_no as per official data dictionary
     */
    @Query(value = """
        SELECT 
            von.notice_no as noticeNo,
            von.vehicle_no as vehicleNo,
            von.last_processing_stage as lastProcessingStage,
            von.last_processing_date as lastProcessingDate,
            von.next_processing_stage as nextProcessingStage,
            von.next_processing_date as nextProcessingDate,
            onod.id_no as nricNo,
            onod.name as name,
            onod.offender_tel_no as phoneNo
        FROM ocmsizmgr.ocms_valid_offence_notice von
        JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod ON onod.notice_no = von.notice_no
        WHERE von.last_processing_stage = 'ENA'
        AND CAST(von.last_processing_date AS DATE) <= CAST(GETDATE() AS DATE)
        AND (
            von.suspension_type IS NULL
            OR (
                von.suspension_type = 'TS'
                AND von.epr_reason_of_suspension = 'HST'
            )
        )
        AND onod.id_no NOT IN (
            SELECT id_no FROM ocmsizmgr.ocms_enotification_exclusion_list
        )
        AND onod.offender_indicator = 'Y'
        ORDER BY von.last_processing_date ASC
        """, nativeQuery = true)
    List<Object[]> findRecordsForInitialNotification();

    @Query(value = """
        SELECT von.notice_no
        FROM ocmsizmgr.ocms_valid_offence_notice von
        WHERE von.sent_to_rep_date IS NULL
          AND von.subsystem_label = :repccsCode
        ORDER BY von.create_date ASC
        """, nativeQuery = true)
    List<String> findNoticesForRepArc(@org.springframework.data.repository.query.Param("repccsCode") String repccsCode);

    @Query(value = """
             SELECT
                von.notice_no
            FROM
                ocms_valid_offence_notice von
            WHERE
                 von.suspension_type = 'PS'
                AND (
                    von.epr_reason_of_suspension IN (
                        SELECT TRIM(value)
                        FROM STRING_SPLIT(
                            (SELECT value
                             FROM ocms_parameter
                             WHERE parameter_id = 'EPRARC' AND code = 'ARCHIVAL'),
                            ','
                        )
                    )
                    OR von.crs_reason_of_suspension IN (
                        SELECT TRIM(value)
                        FROM STRING_SPLIT(
                            (SELECT value
                             FROM ocms_parameter
                             WHERE parameter_id = 'CRSARC' AND code = 'ARCHIVAL'),
                            ','
                        )
                    )
                )
                AND von.subsystem_label = '002'
                AND (
                    von.epr_date_of_suspension < DATEADD(YEAR,
                        CAST((SELECT value FROM ocms_parameter WHERE parameter_id = 'PERIOD' AND code = 'ARCHIVAL') AS INT),\s
                        GETDATE()
                    )
                    OR von.crs_date_of_suspension < DATEADD(YEAR,
                        CAST((SELECT value FROM ocms_parameter WHERE parameter_id = 'PERIOD' AND code = 'ARCHIVAL') AS INT),\s
                        GETDATE()
                    )
                );
        """, nativeQuery = true)
    List<String> findNoticesForNopoarc();

    @Query(value = """
             SELECT
                von.notice_no
            FROM\s
                ocms_valid_offence_notice von
            WHERE
                von.sent_to_rep_date IS NULL
                AND von.suspension_type = 'PS'
                AND (
                    von.epr_reason_of_suspension IN (
                        SELECT TRIM(value)
                        FROM STRING_SPLIT(
                            (SELECT value\s
                             FROM ocms_parameter\s
                             WHERE parameter_id = 'EPRARC' AND code = 'ARCHIVAL'),\s
                            ','
                        )
                    )
                    OR von.crs_reason_of_suspension IN (
                        SELECT TRIM(value)
                        FROM STRING_SPLIT(
                            (SELECT value\s
                             FROM ocms_parameter\s
                             WHERE parameter_id = 'CRSARC' AND code = 'ARCHIVAL'),\s
                            ','
                        )
                    )
                )
                AND von.subsystem_label = '001'
                AND (
                    von.epr_date_of_suspension < DATEADD(YEAR,\s
                        CAST((SELECT value FROM ocms_parameter WHERE parameter_id = 'PERIOD' AND code = 'ARCHIVAL') AS INT),\s
                        GETDATE()
                    )
                    OR von.crs_date_of_suspension < DATEADD(YEAR,\s
                        CAST((SELECT value FROM ocms_parameter WHERE parameter_id = 'PERIOD' AND code = 'ARCHIVAL') AS INT),\s
                        GETDATE()
                    )
                );
        """, nativeQuery = true)
    List<String> findCertisNoticesForNopoarc();

    @Query(value = """
             SELECT
             distinct(von.vehicle_no),
               'Outstanding notices. Call URA 632935400 during office hours' AS message
            FROM
              ocmsizmgr.ocms_valid_offence_notice von
            WHERE
             von.vehicle_registration_type = 'F'
             AND von.subsystem_label ='002'
             AND von.suspension_type ='PS'
             AND von.epr_reason_of_suspension='FOR'
             AND (von.amount_paid =0 OR von.amount_paid IS NULL)
            AND CAST(von.notice_date_and_time AS DATE) = CAST(
                DATEADD(DAY, -(
               SELECT CAST(value AS INT)
               FROM ocmsizmgr.ocms_parameter op
                WHERE op.code = 'FOR' AND op.parameter_id = 'PERIOD'
               ), GETDATE()
            ) AS DATE);
            """, nativeQuery = true)
    List<Object[]> findForeignVehicleNotices();


    @Query(value = """
             SELECT
             distinct(von.vehicle_no),
               'Outstanding notices. Call URA 632935400 during office hours' AS message
            FROM
              ocmsizmgr.ocms_valid_offence_notice von
            WHERE
             von.vehicle_registration_type = 'F'
             AND von.suspension_type ='PS'
             AND von.epr_reason_of_suspension='FOR'
             AND (von.amount_paid =0 OR von.amount_paid IS NULL)
            AND CAST(von.notice_date_and_time AS DATE) = CAST(
                DATEADD(DAY, -(
               SELECT CAST(value AS INT)
               FROM ocmsizmgr.ocms_parameter op
                WHERE op.code = 'FOR' AND op.parameter_id = 'PERIOD'
               ), GETDATE()
            ) AS DATE);
            """, nativeQuery = true)
    List<Object[]> findCertisForeignVehicleNotices();
    

    @Query(value = """
        SELECT distinct(vehicle_no)
        FROM ocmsizmgr.ocms_valid_offence_notice
        WHERE (an_flag = 'N' OR an_flag IS NULL)
        AND offence_notice_type = 'O'
        AND vehicle_registration_type IN ('S', 'D', 'V', 'I')
        AND CAST(notice_date_and_time AS DATE) >= CAST(DATEADD(YEAR, -(
            SELECT TOP 1 CAST(value AS INT)
            FROM ocmsizmgr.ocms_parameter
            WHERE code = 'ANS'
            AND parameter_id = 'PERIOD'
        ), GETDATE()) AS DATE)
        AND subsystem_label = '002'
        ORDER BY vehicle_no ASC
        """, nativeQuery = true)
    List<String> findVehiclesForCasinansveh();

    @Query(value = """
        SELECT distinct(vehicle_no)
        FROM ocmsizmgr.ocms_valid_offence_notice
        WHERE (an_flag = 'N' OR an_flag IS NULL)
        AND offence_notice_type = 'O'
        AND vehicle_registration_type IN ('S', 'D', 'V', 'I')
        AND CAST(notice_date_and_time AS DATE) >= CAST(DATEADD(YEAR, -(
            SELECT TOP 1 CAST(value AS INT)
            FROM ocmsizmgr.ocms_parameter
            WHERE code = 'ANS'
            AND parameter_id = 'PERIOD'
        ), GETDATE()) AS DATE)
        AND subsystem_label = '001'
    ORDER BY vehicle_no ASC
    """, nativeQuery = true)
    List<String> findCertisVehiclesForCasinansveh();
    
    /**
     * Update sent_to_rep_date for processed notices
     * Sets sent_to_rep_date to current date and updates audit fields
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = """
        UPDATE ocmsizmgr.ocms_valid_offence_notice 
        SET sent_to_rep_date = GETDATE(),
            upd_date = GETDATE(),
            upd_user_id = 'SYSTEM'
        WHERE notice_no IN (:noticeNumbers)
        """, nativeQuery = true)
    int updateSentToRepDate(@org.springframework.data.repository.query.Param("noticeNumbers") java.util.List<String> noticeNumbers);
    
    /**
     * Update sent_to_rep_date for processed vehicles by vehicle number
     * Sets sent_to_rep_date to current date and updates audit fields
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = """
        UPDATE ocmsizmgr.ocms_valid_offence_notice 
        SET sent_to_rep_date = GETDATE(),
            upd_date = GETDATE(),
            upd_user_id = 'SYSTEM'
        WHERE vehicle_no IN (:vehicleNumbers)
          AND suspension_type = 'PS'
          AND epr_reason_of_suspension = 'ANS'
        """, nativeQuery = true)
    int updateSentToRepDateForVehicles(@org.springframework.data.repository.query.Param("vehicleNumbers") java.util.List<String> vehicleNumbers);
    
    /**
     * Update sent_to_rep_date for processed foreign vehicles by vehicle number
     * Sets sent_to_rep_date to current date and updates audit fields
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = """
        UPDATE ocmsizmgr.ocms_valid_offence_notice 
        SET sent_to_rep_date = GETDATE(),
            upd_date = GETDATE(),
            upd_user_id = 'SYSTEM'
        WHERE vehicle_no IN (:vehicleNumbers)
          AND vehicle_registration_type = 'F'
          AND suspension_type = 'PS'
          AND epr_reason_of_suspension = 'FOR'
        """, nativeQuery = true)
    int updateSentToRepDateForForeignVehicles(@org.springframework.data.repository.query.Param("vehicleNumbers") java.util.List<String> vehicleNumbers);

    /**
     * Find notices that need DataHive FIN/UEN enrichment
     * Optimized query with database-side filtering for exclusions
     *
     * @return List of notice data for DataHive enrichment [noticeNo, idNo, idType, ownerDriverIndicator, nextProcessingDate]
     */
    @Query(value = """
        SELECT DISTINCT
            von.notice_no AS noticeNo,
            onod.id_no AS idNo,
            onod.id_type AS idType,
            onod.owner_driver_indicator AS ownerDriverIndicator,
            von.next_processing_date AS nextProcessingDate
        FROM ocmsizmgr.ocms_valid_offence_notice von
        INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod
            ON onod.notice_no = von.notice_no
        WHERE von.next_processing_stage IN ('RD1', 'RD2', 'RR3', 'DN2', 'DR3')
            AND CAST(von.next_processing_date AS DATE) <= CAST(DATEADD(DAY, 1, GETDATE()) AS DATE)
            AND von.suspension_type IS NULL
            AND onod.id_type IN ('F', 'B')
            AND onod.offender_indicator = 'Y'
            AND von.notice_no NOT IN (
                SELECT notice_no
                FROM ocmsizmgr.ocms_nro_temp
            )
            AND onod.id_no NOT IN (
                SELECT id_no
                FROM ocmsizmgr.ocms_hst
            )
        ORDER BY von.next_processing_date ASC
        """, nativeQuery = true)
    List<Object[]> findNoticesForDataHiveEnrichment();

    /**
     * Find Toppan notices for a specific stage with all required data
     * Optimized comprehensive query with database-side filtering
     *
     * @param stage The processing stage (RD1, RD2, RR3, DN1, DN2, DR3)
     * @return List of comprehensive notice data for Toppan file generation
     */
    @Query(value = """
        SELECT
            von.notice_no AS noticeNo,
            von.vehicle_no AS vehicleNo,
            von.notice_date_and_time AS noticeDateAndTime,
            von.pp_name AS ppName,
            von.composition_amount AS compositionAmount,
            von.next_processing_stage AS nextProcessingStage,
            von.next_processing_date AS nextProcessingDate,
            ond.rule_desc AS ruleDesc,
            ond.rule_no AS ruleNo,
            von.computer_rule_code AS computerRuleCode,
            onod.name AS name,
            onod.id_no AS idNo,
            onod.id_type AS idType,
            onod.offender_indicator AS offenderIndicator,
            onod.owner_driver_indicator AS ownerDriverIndicator,
            von.vehicle_registration_type AS vehicleRegistrationType
        FROM ocmsizmgr.ocms_valid_offence_notice von
        INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod
            ON onod.notice_no = von.notice_no
            AND onod.offender_indicator = 'Y'
        LEFT JOIN ocmsizmgr.ocms_offence_notice_detail ond
            ON ond.notice_no = von.notice_no
        WHERE von.next_processing_stage = :stage
            AND CAST(von.next_processing_date AS DATE) <= CAST(GETDATE() AS DATE)
            AND von.suspension_type IS NULL
        ORDER BY von.next_processing_date ASC
        """, nativeQuery = true)
    List<Object[]> findToppanNoticesForStage(@org.springframework.data.repository.query.Param("stage") String stage);

    /**
     * Find addresses for Toppan notices with priority ordering
     * Returns all addresses for given notices, ordered by priority
     *
     * @param noticeNumbers List of notice numbers
     * @return List of address data ordered by priority [noticeNo, ownerDriverIndicator, addressType, blkHseNo, streetName, floorNo, unitNo, buildingName, postalCode]
     */
    @Query(value = """
        SELECT
            onoda.notice_no AS noticeNo,
            onoda.owner_driver_indicator AS ownerDriverIndicator,
            onoda.type_of_address AS addressType,
            onoda.blk_hse_no AS blkHseNo,
            onoda.street_name AS streetName,
            onoda.floor_no AS floorNo,
            onoda.unit_no AS unitNo,
            onoda.bldg_name AS buildingName,
            onoda.postal_code AS postalCode
        FROM ocmsizmgr.ocms_offence_notice_owner_driver_addr onoda
        WHERE onoda.notice_no IN (:noticeNumbers)
        ORDER BY
            onoda.notice_no,
            onoda.owner_driver_indicator,
            CASE onoda.type_of_address
                WHEN 'mha_reg' THEN 1
                WHEN 'lta_reg' THEN 2
                WHEN 'lta_mail' THEN 3
                WHEN 'furnished_mail' THEN 4
                ELSE 5
            END
        """, nativeQuery = true)
    List<Object[]> findAddressesForNoticesWithPriority(@org.springframework.data.repository.query.Param("noticeNumbers") java.util.List<String> noticeNumbers);

    /**
     * Batch query VON records by notice numbers using IN clause
     * Used by transaction sync workflow for efficient batch processing
     *
     * @param noticeNumbers List of notice numbers to query
     * @return List of VON records
     */
    List<OcmsValidOffenceNotice> findByNoticeNoIn(List<String> noticeNumbers);

    /**
     * Find VON records by is_sync flag
     * Used by Process 7 (Batch Cron Sync) to find records that need syncing to internet database
     *
     * @param isSync The sync status ("N" = needs syncing, "Y" = already synced)
     * @return List of VON records with the specified sync status
     */
    List<OcmsValidOffenceNotice> findByIsSync(String isSync);

    /**
     * Find Advisory Notice (AN) letters that need to be sent to Toppan
     * OCMS 10: Query for notices where:
     * - an_flag = 'Y' (marked as Advisory Notice)
     * - suspension_type IS NULL OR (suspension_type != 'PS' OR epr_reason_of_suspension != 'ANS')
     *   (not yet suspended with PS-ANS - we only suspend AFTER letter is sent to Toppan)
     * - Get all required data for Toppan file generation
     *
     * @return List of comprehensive notice data for AN Letter generation
     */
    @Query(value = """
        SELECT
            von.notice_no AS noticeNo,
            von.vehicle_no AS vehicleNo,
            von.notice_date_and_time AS noticeDateAndTime,
            von.pp_name AS ppName,
            von.composition_amount AS compositionAmount,
            von.last_processing_stage AS lastProcessingStage,
            von.next_processing_stage AS nextProcessingStage,
            von.next_processing_date AS nextProcessingDate,
            ond.rule_desc AS ruleDesc,
            ond.rule_no AS ruleNo,
            von.computer_rule_code AS computerRuleCode,
            onod.name AS name,
            onod.id_no AS idNo,
            onod.id_type AS idType,
            onod.offender_indicator AS offenderIndicator,
            onod.owner_driver_indicator AS ownerDriverIndicator,
            von.vehicle_registration_type AS vehicleRegistrationType
        FROM ocmsizmgr.ocms_valid_offence_notice von
        INNER JOIN ocmsizmgr.ocms_offence_notice_owner_driver onod
            ON onod.notice_no = von.notice_no
            AND onod.offender_indicator = 'Y'
        LEFT JOIN ocmsizmgr.ocms_offence_notice_detail ond
            ON ond.notice_no = von.notice_no
        WHERE von.an_flag = 'Y'
            AND (
                von.suspension_type IS NULL
                OR von.suspension_type != 'PS'
                OR von.epr_reason_of_suspension != 'ANS'
            )
            AND von.notice_no NOT IN (
                SELECT notice_no
                FROM ocmsizmgr.ocms_an_letter
            )
        ORDER BY von.create_date ASC
        """, nativeQuery = true)
    List<Object[]> findAdvisoryNoticeLetters();

    /**
     * OCMS 14: Find all Type V (VIP) notices for Classified Vehicle Report
     * Daily Classified Vehicle Notices Report
     *
     * @return List of maps containing Type V notice data with payment status
     */
    @Query(value = """
        SELECT
            von.notice_no AS noticeNo,
            von.vehicle_no AS vehicleNo,
            von.vehicle_registration_type AS vehicleRegistrationType,
            von.notice_date_and_time AS noticeDate,
            von.computer_rule_code AS offenceRuleCode,
            von.composition_amount AS compositionAmount,
            von.amount_payable AS amountPayable,
            von.suspension_type AS suspensionType,
            von.epr_reason_of_suspension AS suspensionReason,
            von.epr_date_of_suspension AS suspensionDate,
            von.pp_name AS ppName,
            von.pp_code AS ppCode,
            CASE
                WHEN von.amount_payable > 0 THEN 'Outstanding'
                ELSE 'Settled'
            END AS paymentStatus
        FROM ocmsizmgr.ocms_valid_offence_notice von
        WHERE von.vehicle_registration_type = 'V'
        ORDER BY von.notice_date_and_time DESC
        """, nativeQuery = true)
    java.util.List<java.util.Map<String, Object>> findAllTypeVNotices();

    /**
     * OCMS 14: Find Type V notices amended to Type S (TS-CLV revived)
     * Daily Classified Vehicle Notices Report - Amended Notices Sheet
     *
     * @return List of maps containing amended notice data
     */
    @Query(value = """
        SELECT
            sn.notice_no AS noticeNo,
            sn.date_of_suspension AS suspensionDate,
            sn.date_of_revival AS revivalDate,
            von.vehicle_no AS vehicleNo,
            von.vehicle_registration_type AS currentType,
            von.composition_amount AS compositionAmount,
            von.amount_payable AS amountPayable,
            von.notice_date_and_time AS noticeDate,
            von.pp_name AS ppName,
            von.pp_code AS ppCode
        FROM ocmsizmgr.ocms_suspended_notice sn
        INNER JOIN ocmsizmgr.ocms_valid_offence_notice von
            ON von.notice_no = sn.notice_no
        WHERE sn.suspension_type = 'TS'
          AND sn.reason_of_suspension = 'CLV'
          AND sn.date_of_revival IS NOT NULL
          AND von.vehicle_registration_type = 'S'
        ORDER BY sn.date_of_revival DESC
        """, nativeQuery = true)
    java.util.List<java.util.Map<String, Object>> findTypeVAmendedToS();
}
