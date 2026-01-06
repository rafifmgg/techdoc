package com.ocmsintranet.cronservice.framework.services.datahive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for DataHive operations
 * Simplified to only include health check and query execution endpoints
 */
@RestController
@RequestMapping("/{apiVersion}/datahive")
public class DataHiveController {
    private static final Logger logger = LoggerFactory.getLogger(DataHiveController.class);
    
    @Autowired
    private DataHiveUtil dataHiveUtil;
    
    @Autowired
    private DataHiveService dataHiveService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Health check endpoint
     * 
     * @return Health status
     */
    @PostMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        logger.info("Health check requested");
        boolean isAvailable = dataHiveUtil.isServiceAvailable();
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", isAvailable ? "UP" : "DOWN");
        response.put("service", "DataHive");
        
        return ResponseEntity
                .status(isAvailable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
    
    /**
     * Execute SQL query with default database context
     * 
     * @param requestBody Request body containing SQL statement
     * @return Query result
     */
    @PostMapping("/query")
    public ResponseEntity<Object> executeQuery(@RequestBody Map<String, String> requestBody) {
        String sql = requestBody.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("SQL statement is required");
        }
        
        logger.info("Executing SQL query: {}", sql);
        try {
            JsonNode result = dataHiveUtil.executeQuery(sql);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error executing query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing query: " + e.getMessage());
        }
    }
    
    /**
     * Execute SQL query with custom database context
     * 
     * @param requestBody Request body containing SQL statement and database context
     * @return Query result
     */
    @PostMapping("/query/custom")
    public ResponseEntity<Object> executeQueryCustom(@RequestBody Map<String, String> requestBody) {
        String sql = requestBody.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("SQL statement is required");
        }
        
        String database = requestBody.getOrDefault("database", dataHiveUtil.getDefaultDatabase());
        String schema = requestBody.getOrDefault("schema", dataHiveUtil.getDefaultSchema());
        String warehouse = requestBody.getOrDefault("warehouse", dataHiveUtil.getDefaultWarehouse());
        String role = requestBody.getOrDefault("role", dataHiveUtil.getDefaultRole());
        
        logger.info("Executing SQL query with custom context: {}", sql);
        logger.debug("Database context: db={}, schema={}, warehouse={}, role={}", 
                database, schema, warehouse, role);
        
        try {
            JsonNode result = dataHiveUtil.executeQuery(sql, database, schema, warehouse, role);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error executing query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing query: " + e.getMessage());
        }
    }
    
    /**
     * Execute SQL query asynchronously with default database context and wait for the result
     * Returns only the data array from the query result
     * 
     * @param requestBody Request body containing SQL statement
     * @return The data array from the query result
     */
    @PostMapping("/query/async")
    public ResponseEntity<Object> executeQueryAsync(@PathVariable String apiVersion, @RequestBody Map<String, String> requestBody) {
        String sql = requestBody.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("SQL statement is required");
        }
        
        logger.info("Executing SQL query asynchronously: {}", sql);
        try {
            // Use the service to execute the query with default database context
            JsonNode result = dataHiveService.executeQueryAsyncCustom(
                sql, 
                dataHiveUtil.getDefaultDatabase(), 
                dataHiveUtil.getDefaultSchema(), 
                dataHiveUtil.getDefaultWarehouse(), 
                dataHiveUtil.getDefaultRole()
            );
            logger.info("Async query completed successfully");
            
            return ResponseEntity.ok().body(result);
        } catch (DataHiveException e) {
            logger.error("Error executing async query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing async query: " + e.getMessage());
        }
    }
    
    /**
     * Execute SQL query asynchronously with custom database context and wait for the result
     * Returns only the data array from the query result
     * 
     * @param requestBody Request body containing SQL statement and database context
     * @return The data array from the query result
     */
    @PostMapping("/query/async/custom")
    public ResponseEntity<Object> executeQueryAsyncCustom(@PathVariable String apiVersion, @RequestBody Map<String, String> requestBody) {
        String sql = requestBody.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("SQL statement is required");
        }
        
        String database = requestBody.getOrDefault("database", dataHiveUtil.getDefaultDatabase());
        String schema = requestBody.getOrDefault("schema", dataHiveUtil.getDefaultSchema());
        String warehouse = requestBody.getOrDefault("warehouse", dataHiveUtil.getDefaultWarehouse());
        String role = requestBody.getOrDefault("role", dataHiveUtil.getDefaultRole());
        
        logger.info("Executing SQL query asynchronously with custom context: {}", sql);
        logger.debug("Database context: db={}, schema={}, warehouse={}, role={}", 
                database, schema, warehouse, role);
        
        try {
            // Use the service to execute the query and get just the data array
            JsonNode result = dataHiveService.executeQueryAsyncCustom(sql, database, schema, warehouse, role);
            logger.info("Async query completed successfully");
            
            return ResponseEntity.ok().body(result);
        } catch (DataHiveException e) {
            logger.error("Error executing async query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing async query: " + e.getMessage());
        }
    }
}
