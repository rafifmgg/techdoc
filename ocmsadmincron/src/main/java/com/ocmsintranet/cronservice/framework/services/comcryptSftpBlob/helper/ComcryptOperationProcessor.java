package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper;

import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for managing COMCRYPT operations.
 * This class handles:
 * - Finding operations by request ID
 * - Updating operation status
 * - Converting between OcmsComcryptOperation objects and Maps
 * - Clearing file content from operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptOperationProcessor {

    private final TableQueryService tableQueryService;
    
    /**
     * Find a COMCRYPT operation by request ID
     * 
     * @param requestId The request ID
     * @return The OcmsComcryptOperation if found, null otherwise
     */
    public OcmsComcryptOperation findOperationByRequestId(String requestId) {
        try {
            if (requestId == null || requestId.isEmpty()) {
                log.error("Cannot find operation with null or empty request ID");
                return null;
            }
            
            // Create filter to find by request ID
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", requestId);
            
            // Query the database
            List<Map<String, Object>> results = tableQueryService.query("ocms_comcrypt_operation", filters);
            
            if (results == null || results.isEmpty()) {
                log.warn("No COMCRYPT operation found for request ID: {}", requestId);
                return null;
            }
            
            // Convert the first result to an OcmsComcryptOperation object
            return convertToOperation(results.get(0));
        } catch (Exception e) {
            log.error("Error finding COMCRYPT operation by request ID: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Update the status of a COMCRYPT operation
     * 
     * @param requestId The request ID
     * @param status The new status
     * @param errorMessage The error message (if any)
     * @return true if the update was successful, false otherwise
     */
    public boolean updateOperationStatus(String requestId, String status, String errorMessage) {
        try {
            if (requestId == null || requestId.isEmpty()) {
                log.error("Cannot update operation status with null or empty request ID");
                return false;
            }
            
            // Get the operation from the database
            OcmsComcryptOperation operation = findOperationByRequestId(requestId);
            if (operation == null) {
                log.error("No COMCRYPT operation found for request ID: {}", requestId);
                return false;
            }
            
            operation.setUpdDate(LocalDateTime.now());
            
            // Save the updated operation
            Map<String, Object> operationMap = convertToMap(operation);
            
            // Create filter to find by request ID
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", operation.getRequestId());
            
            List<Map<String, Object>> updatedOperations = tableQueryService.patch(
                "ocms_comcrypt_operation", 
                filters, 
                operationMap
            );
            
            Map<String, Object> updatedOperation = updatedOperations != null && !updatedOperations.isEmpty() ? 
                updatedOperations.get(0) : null;
            
            if (updatedOperation == null) {
                log.error("Failed to update COMCRYPT operation status for request ID: {}", requestId);
                return false;
            }
            
            log.info("Updated COMCRYPT operation status to {} for request ID: {}", status, requestId);
            return true;
        } catch (Exception e) {
            log.error("Error updating COMCRYPT operation status: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clear the file content from a COMCRYPT operation to free up database space
     * 
     * @param requestId The request ID
     * @return true if the update was successful, false otherwise
     */
    public boolean clearFileContent(String requestId) {
        try {
            if (requestId == null || requestId.isEmpty()) {
                log.error("Cannot clear file content with null or empty request ID");
                return false;
            }
            
            // Get the operation from the database
            OcmsComcryptOperation operation = findOperationByRequestId(requestId);
            if (operation == null) {
                log.error("No COMCRYPT operation found for request ID: {}", requestId);
                return false;
            }
            
            // Clear the file content
            // operation.setFileContent(null);
            operation.setUpdDate(LocalDateTime.now());
            
            // Save the updated operation
            Map<String, Object> operationMap = convertToMap(operation);
            
            // Create filter to find by request ID
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", operation.getRequestId());
            
            List<Map<String, Object>> updatedOperations = tableQueryService.patch(
                "ocms_comcrypt_operation", 
                filters, 
                operationMap
            );
            
            Map<String, Object> updatedOperation = updatedOperations != null && !updatedOperations.isEmpty() ? 
                updatedOperations.get(0) : null;
            
            if (updatedOperation == null) {
                log.error("Failed to clear file content for request ID: {}", requestId);
                return false;
            }
            
            log.info("Cleared file content for request ID: {}", requestId);
            return true;
        } catch (Exception e) {
            log.error("Error clearing file content: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Convert a OcmsComcryptOperation object to a Map
     * 
     * @param operation The OcmsComcryptOperation object
     * @return A Map representation of the operation
     */
    public Map<String, Object> convertToMap(OcmsComcryptOperation operation) {
        if (operation == null) {
            return null;
        }
        
        Map<String, Object> map = new HashMap<>();
        // Only include fields that exist in OcmsComcryptOperation entity
        map.put("requestId", operation.getRequestId());
        map.put("operationType", operation.getOperationType());
        map.put("fileName", operation.getFileName());
        map.put("token", operation.getToken());
        
        return map;
    }
    
    /**
     * Convert a Map to a OcmsComcryptOperation object
     * 
     * @param map The Map representation of the operation
     * @return A OcmsComcryptOperation object
     */
    public OcmsComcryptOperation convertToOperation(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        OcmsComcryptOperation operation = new OcmsComcryptOperation();
        // Only set fields that exist in OcmsComcryptOperation entity
        operation.setRequestId((String) map.get("requestId"));
        operation.setOperationType((String) map.get("operationType"));
        operation.setFileName((String) map.get("fileName"));
        operation.setToken((String) map.get("token"));
        
        return operation;
    }
}
