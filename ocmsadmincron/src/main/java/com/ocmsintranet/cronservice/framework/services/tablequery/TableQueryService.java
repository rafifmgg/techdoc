package com.ocmsintranet.cronservice.framework.services.tablequery;

import java.util.List;
import java.util.Map;

/**
 * Service interface for simplified table query operations
 * Allows workflows to query any entity table using simple filters
 * without needing to know the entity class
 */
public interface TableQueryService {
    
    /**
     * Query a table with simple filters
     * 
     * @param tableName The name of the entity table (e.g., "OcmsValidOffenceNotice")
     * @param filters A map of simple filter conditions
     * @return List of rows as maps
     */
    List<Map<String, Object>> query(String tableName, Map<String, Object> filters);
    
    /**
     * Query a table with a predefined query name
     * 
     * @param tableName The name of the entity table
     * @param queryName A predefined query name (e.g., "ltaEnquiryQuery")
     * @param parameters Optional parameters to customize the query
     * @return List of rows as maps
     */
    List<Map<String, Object>> queryWithPredefined(String tableName, String queryName, Map<String, Object> parameters);
    
    /**
     * Get count of rows that match filters
     * 
     * @param tableName The name of the entity table
     * @param filters A map of simple filter conditions
     * @return Count of matching rows
     */
    long count(String tableName, Map<String, Object> filters);
    
    /**
     * Patch (partially update) entities that match the given filters
     * 
     * @param tableName The name of the entity table
     * @param filters A map of simple filter conditions to identify entities to update
     * @param fields A map of field names and values to update
     * @return List of updated entities as maps
     */
    List<Map<String, Object>> patch(String tableName, Map<String, Object> filters, Map<String, Object> fields);
    
    /**
     * Create a new entity in the specified table
     * 
     * @param tableName The name of the entity table
     * @param fields A map of field names and values for the new entity
     * @return The created entity as a map
     */
    Map<String, Object> post(String tableName, Map<String, Object> fields);
    
    /**
     * Delete entities that match the given filters
     * 
     * @param tableName The name of the entity table
     * @param filters A map of simple filter conditions to identify entities to delete
     * @return Number of entities deleted
     */
    int delete(String tableName, Map<String, Object> filters);
}
