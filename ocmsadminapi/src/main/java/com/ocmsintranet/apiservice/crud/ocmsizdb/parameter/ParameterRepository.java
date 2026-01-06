package com.ocmsintranet.apiservice.crud.ocmsizdb.parameter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ocmsintranet.apiservice.crud.BaseRepository;

@Repository
public interface ParameterRepository extends BaseRepository<Parameter, ParameterId>{
    Optional<Parameter> findById(ParameterId id);
    
    /**
     * Find all parameters by code (e.g., all parameters with code='SEQUENCE')
     */
    List<Parameter> findByCode(String code);
    
    /**
     * Find parameter where the value field contains the subsystem code.
     * Uses database-level search for better performance.
     * Searches for exact match in comma-separated values.
     * 
     * @param code The parameter code (e.g., "SEQUENCE")
     * @param subsystem The subsystem code to search for in value field
     * @return Optional containing the matching parameter
     */
    @Query("SELECT p FROM Parameter p WHERE p.code = :code AND " +
           "(p.value = :subsystem OR " +
           "p.value LIKE CONCAT(:subsystem, ',%') OR " +
           "p.value LIKE CONCAT('%,', :subsystem, ',%') OR " +
           "p.value LIKE CONCAT('%,', :subsystem))")
    Optional<Parameter> findByCodeAndValueContaining(@Param("code") String code, 
                                                      @Param("subsystem") String subsystem);
}