package com.ocmsintranet.cronservice.framework.services.datahive;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of the DataHive service
 */
@Service
public class DataHiveServiceImpl implements DataHiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataHiveServiceImpl.class);
    
    @Autowired
    private DataHiveUtil dataHiveUtil;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public JsonNode executeQueryAsyncCustom(String sql, String database, String schema, String warehouse, String role) throws DataHiveException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new DataHiveException("SQL statement is required");
        }
        
        logger.info("Executing SQL query asynchronously with custom context: {}", sql);
        logger.debug("Database context: db={}, schema={}, warehouse={}, role={}", 
                database, schema, warehouse, role);
        
        try {
            // Start the async query execution and wait for the result
            // Use the data-only method to get just the data array
            return dataHiveUtil.executeQueryAsyncDataOnly(sql, database, schema, warehouse, role).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Async query execution was interrupted: {}", e.getMessage(), e);
            throw new DataHiveException("Async query execution was interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Error executing async query: {}", e.getCause().getMessage(), e.getCause());
            throw new DataHiveException("Error executing async query: " + e.getCause().getMessage(), e.getCause());
        } catch (Exception e) {
            logger.error("Error starting async query: {}", e.getMessage(), e);
            throw new DataHiveException("Error starting async query: " + e.getMessage(), e);
        }
    }
}
