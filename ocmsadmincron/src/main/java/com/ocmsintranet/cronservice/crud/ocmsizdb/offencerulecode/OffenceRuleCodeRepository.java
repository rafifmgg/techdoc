package com.ocmsintranet.cronservice.crud.ocmsizdb.offencerulecode;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OffenceRuleCode entity
 */
@Repository
public interface OffenceRuleCodeRepository extends BaseRepository<OffenceRuleCode, OffenceRuleCodeId> {
    
    /**
     * Find active offence rules for specific vehicle categories and offence types
     * Used for generating offence rule data files for government systems
     * 
     * @return List of Object arrays containing offence rule data
     */
    @Query(value = """
        SELECT
            orc.computer_rule_code as offence_code,
            orc.rule_no as offence_rule,
            orc.description as offence_description,
            orc.effective_start_date,
            orc.effective_end_date,
            orc.composition_amount as default_fine_amount,
            orc.secondary_fine_amount,
            CASE WHEN orc.vehicle_category = 'H' THEN 'HV'
            ELSE orc.vehicle_category
            END AS vehicle_type
        FROM ocms_offence_rule_code orc
        WHERE 
            orc.vehicle_category IN ('C', 'H', 'M')
            AND orc.effective_start_date <= GETDATE()
            AND orc.effective_end_date >= GETDATE()
            AND orc.offence_type IN ('O', 'U')
        ORDER BY orc.computer_rule_code ASC
        """, nativeQuery = true)
    List<Object[]> findActiveOffenceRules();

    @Query(value = """
        SELECT
            orc.computer_rule_code as offence_code,
            orc.rule_no as offence_rule,
            orc.description as offence_description,
            orc.effective_start_date,
            orc.effective_end_date,
            orc.composition_amount as default_fine_amount,
            orc.secondary_fine_amount,
            orc.vehicle_category as vehicle_type
        FROM ocms_offence_rule_code orc
        WHERE 
            orc.vehicle_category IN ('C', 'H', 'M','Y')
            AND orc.effective_start_date <= GETDATE()
            AND orc.effective_end_date >= GETDATE()
            AND orc.offence_type IN ('O', 'U')
        ORDER BY orc.computer_rule_code ASC
        """, nativeQuery = true)
    List<Object[]> findActiveRepOffenceRules();


}