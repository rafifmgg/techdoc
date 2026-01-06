package com.ocmsintranet.apiservice.crud.ocmsizdb.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterRepository;

import java.util.Optional;

@Repository
@Slf4j
public class SequenceRepository {
    
    @PersistenceContext(unitName = "intranetEntityManagerFactory")
    private EntityManager entityManager;
    
    @Autowired
    private ParameterRepository parameterRepository;
    
    private static final String SEQUENCE_PARAM_CODE = "SEQUENCE";
    
    public Long getNextNoticeNumber(String subsystem) {
        try {
            String sequenceName = getSequenceName(subsystem);
            String sql = "SELECT NEXT VALUE FOR " + sequenceName;
            
            log.info("Executing sequence query: {}", sql);
            Query query = entityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();
            log.info("Query result type: {}", result.getClass().getName());
            log.info("Query result value: {}", result);
            
            Long nextVal = ((Number) result).longValue();
            log.info("Successfully retrieved next notice number from sequence {} : {}", sequenceName, nextVal);
            return nextVal;
        } catch (Exception e) {
            log.error("Error executing sequence query for subsystem: {}", subsystem, e);
            throw e;
        }
    }
    
    /**
     * Gets the next value from a specified sequence
     * 
     * @param sequenceName The name of the sequence (e.g., "SUSPENDED_NOTICE_SEQ")
     * @return The next sequence value as an Integer
     */
    public Integer getNextSequence(String sequenceName) {
        try {
            String fullSequenceName;
            
            // Map sequence name to actual database sequence
            switch (sequenceName.toUpperCase()) {
                case "SUSPENDED_NOTICE_SEQ":
                    fullSequenceName = "ocmsizmgr.seq_suspended_notice_sr_no";
                    break;
                case "TXN_REF_NUMBER_SEQ":
                    fullSequenceName = "ocmsizmgr.seq_ocms_txn_ref_number";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown sequence name: " + sequenceName);
            }
            
            String sql = "SELECT NEXT VALUE FOR " + fullSequenceName;
            
            log.info("Executing sequence query: {}", sql);
            Query query = entityManager.createNativeQuery(sql);
            Object result = query.getSingleResult();
            log.info("Query result type: {}", result.getClass().getName());
            log.info("Query result value: {}", result);
            
            Integer nextVal = ((Number) result).intValue();
            log.info("Successfully retrieved next sequence number from {} : {}", fullSequenceName, nextVal);
            return nextVal;
        } catch (Exception e) {
            log.error("Error executing sequence query for {}: {}", sequenceName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Gets the database sequence name for a given subsystem code.
     * Uses parameter table with code='SEQUENCE' to map subsystem codes to sequences.
     * Constructs the full sequence name as: ocmsizmgr.seq_notice_no_{parameter_id_lowercase}
     * 
     * @param subsystem The subsystem code (e.g., "001", "CES", "003")
     * @return The full database sequence name (e.g., "ocmsizmgr.seq_notice_no_ces")
     * @throws IllegalArgumentException if subsystem is not found in parameter table
     */
    private String getSequenceName(String subsystem) {
        // Trim and convert to uppercase to handle case-insensitive matching
        String normalizedSubsystem = subsystem != null ? subsystem.trim().toUpperCase() : "";
        log.info("Getting sequence name for normalized subsystem: '{}'", normalizedSubsystem);
        
        // First, try to find the subsystem code directly in parameter table
        Optional<Parameter> paramOpt = findParameterBySubsystem(normalizedSubsystem);
        
        if (paramOpt.isPresent()) {
            String parameterId = paramOpt.get().getParameterId();
            String sequenceName = buildSequenceName(parameterId);
            log.info("Found sequence mapping: subsystem='{}' -> parameter_id='{}' -> sequence='{}'", 
                normalizedSubsystem, parameterId, sequenceName);
            return sequenceName;
        }
        
        // If not found directly, check if this subsystem code is listed in any parameter's value field
        // (e.g., parameter_id='CES' with value='001,CES' means both codes map to same sequence)
        paramOpt = findParameterBySubsystemInValue(normalizedSubsystem);
        
        if (paramOpt.isPresent()) {
            String parameterId = paramOpt.get().getParameterId();
            String sequenceName = buildSequenceName(parameterId);
            log.info("Found sequence mapping via value search: subsystem='{}' -> parameter_id='{}' -> sequence='{}'", 
                normalizedSubsystem, parameterId, sequenceName);
            return sequenceName;
        }
        
        log.error("No sequence configuration found for subsystem: '{}'", normalizedSubsystem);
        throw new IllegalArgumentException(
            "Unknown subsystem: '" + normalizedSubsystem + "'. " +
            "Please ensure the subsystem is configured in ocms_parameter table with code='SEQUENCE'."
        );
    }
    
    /**
     * Builds the full sequence name from parameter_id.
     * Format: ocmsizmgr.seq_notice_no_{parameter_id_lowercase}
     * 
     * @param parameterId The parameter_id (e.g., "CES", "EEPS", "MEV")
     * @return Full sequence name (e.g., "ocmsizmgr.seq_notice_no_ces")
     */
    private String buildSequenceName(String parameterId) {
        String lowerCaseId = parameterId.toLowerCase();
        return "ocmsizmgr.seq_notice_no_" + lowerCaseId;
    }
    
    /**
     * Find parameter by subsystem code as parameter_id.
     */
    private Optional<Parameter> findParameterBySubsystem(String subsystem) {
        ParameterId parameterId = new ParameterId();
        parameterId.setParameterId(subsystem);
        parameterId.setCode(SEQUENCE_PARAM_CODE);
        return parameterRepository.findById(parameterId);
    }
    
    /**
     * Find parameter where subsystem code appears in the value field.
     * This handles cases where we look up by codes listed in the value field.
     * For example: parameter_id='CES', code='SEQUENCE', value='001,CES'
     * Both '001' and 'CES' will map to the same sequence.
     * 
     * Uses database-level query for optimal performance instead of Java loops.
     */
    private Optional<Parameter> findParameterBySubsystemInValue(String subsystem) {
        // Use database query to search for subsystem in value field
        // This is much more efficient than loading all records and looping in Java
        return parameterRepository.findByCodeAndValueContaining(SEQUENCE_PARAM_CODE, subsystem);
    }
}