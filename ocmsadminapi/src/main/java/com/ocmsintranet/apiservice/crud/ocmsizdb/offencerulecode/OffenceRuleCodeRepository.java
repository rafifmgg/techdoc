package com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for OffenceRuleCode entity
 */
@Repository
public interface OffenceRuleCodeRepository extends BaseRepository<OffenceRuleCode, OffenceRuleCodeId> {
    // The base repository provides all necessary methods
    
    /**
     * Find the offence type for a given computer rule code and vehicle category
     * Only returns records where current date is between effective start and end dates
     * 
     * @param computerRuleCode The computer rule code to search for
     * @param vehicleCategory The vehicle category to search for
     * @param currentDate The current date to check against effective dates
     * @return Optional containing the offence type if found
     */
    @Query(value = "SELECT a.offence_type FROM ocmsizmgr.ocms_offence_rule_code a WHERE a.computer_rule_code = :computerRuleCode AND a.vehicle_category = :vehicleCategory AND :currentDate BETWEEN a.effective_start_date AND a.effective_end_date", nativeQuery = true)
    Optional<String> findOffenceTypeByRuleCodeAndVehicleCategory(@Param("computerRuleCode") Integer computerRuleCode, @Param("vehicleCategory") String vehicleCategory, @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find OffenceRuleCode by computer rule code, vehicle category, and offence type
     * Only returns records where current date is between effective start and end dates
     *
     * @param computerRuleCode The computer rule code to search for
     * @param vehicleCategory The vehicle category to search for
     * @param offenceType The offence type to search for
     * @param currentDate The current date to check against effective dates
     * @return Optional containing the OffenceRuleCode if found
     */
    @Query("SELECT a FROM OffenceRuleCode a WHERE a.computerRuleCode = :computerRuleCode AND a.vehicleCategory = :vehicleCategory AND a.offenceType = :offenceType AND :currentDate BETWEEN a.effectiveStartDate AND a.effectiveEndDate")
    Optional<OffenceRuleCode> findByRuleCodeVehicleCategoryAndOffenceType(
        @Param("computerRuleCode") Integer computerRuleCode,
        @Param("vehicleCategory") String vehicleCategory,
        @Param("offenceType") String offenceType,
        @Param("currentDate") LocalDateTime currentDate
    );
}