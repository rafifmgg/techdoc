package com.ocmsintranet.cronservice.framework.services.datahive;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service interface for DataHive operations
 */
public interface DataHiveService {
    
    /**
     * Execute a SQL query asynchronously with custom database context
     * Returns only the data array from the query result
     * 
     * @param sql SQL query to execute
     * @param database Database name
     * @param schema Schema name
     * @param warehouse Warehouse name
     * @param role Role name
     * @return Data array from query result as JsonNode
     * @throws DataHiveException if query execution fails
     */
    JsonNode executeQueryAsyncCustom(String sql, String database, String schema, String warehouse, String role) throws DataHiveException;
}
