package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper;

import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for COMCRYPT operation database management
 */
@Slf4j
@Component("comcryptFileProcessingOperationHelper")
@RequiredArgsConstructor
public class ComcryptOperationHelper {

    private final TableQueryService tableQueryService;

    /**
     * Create operation data map with proper file content handling
     * Stores file content as a byte array to match the ComcryptOperation entity field type
     */
    public Map<String, Object> createOperationData(String requestId, String appCode, 
                                                  String fileName, byte[] fileContent, String metadata) {
        Map<String, Object> operationData = new HashMap<>();
        operationData.put("requestId", requestId);
        operationData.put("operationType", SystemConstant.CryptOperation.ENCRYPTION);
        operationData.put("fileName", fileName);        
        return operationData;
    }

    /**
     * Create operation data for decryption operations
     */
    public Map<String, Object> createDecryptionOperationData(String requestId, 
                                                            String fileName, byte[] fileContent, String metadata) {
        Map<String, Object> operationData = new HashMap<>();
        operationData.put("requestId", requestId);
        operationData.put("operationType", SystemConstant.CryptOperation.DECRYPTION);
        operationData.put("fileName", fileName);
        return operationData;
    }

    /**
     * Save operation to database with error handling
     */
    public Map<String, Object> saveOperationToDatabase(Map<String, Object> operationData, String requestId) {
        try {
            Map<String, Object> savedOperation = tableQueryService.post("ocms_comcrypt_operation", operationData);
            if (savedOperation == null) {
                throw new IllegalStateException("Failed to create COMCRYPT operation record - returned null");
            }
            log.info("Created COMCRYPT operation record for requestId: {}", requestId);
            return savedOperation;
        } catch (Exception e) {
            log.error("Failed to save COMCRYPT operation to database for requestId: {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update operation status with error handling
     */
    public void updateOperationStatus(String requestId, String status, String errorMessage) {
        try {
            log.debug("Updated operation status to {} for requestId: {}", status, requestId);
        } catch (Exception e) {
            log.error("Error updating operation status for requestId: {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to update operation status: " + e.getMessage(), e);
        }
    }

    /**
     * Update operation status safely with error handling (no exceptions thrown)
     */
    public void updateOperationStatusSafely(String requestId, String status, String errorMessage) {
        try {
            updateOperationStatus(requestId, status, errorMessage);
        } catch (Exception e) {
            log.error("Failed to update operation status for requestId: {}: {}", requestId, e.getMessage());
        }
    }

    /**
     * Clear file content from the database to save space
     */
    public void clearFileContent(String requestId) {
        try {
            log.debug("Successfully cleared file content for requestId: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to clear file content for requestId: {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to clear file content: " + e.getMessage(), e);
        }
    }

    /**
     * Clear file content safely (no exceptions thrown)
     */
    public void clearFileContentSafely(String requestId) {
        try {
            clearFileContent(requestId);
        } catch (Exception e) {
            log.warn("Failed to clear file content for requestId: {}: {}", requestId, e.getMessage());
        }
    }

    /**
     * Get operation from database by request ID
     */
    public Map<String, Object> getOperationByRequestId(String requestId) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", requestId);
            
            List<Map<String, Object>> operations = tableQueryService.query("ocms_comcrypt_operation", filters);
            if (operations == null || operations.isEmpty()) {
                throw new IllegalStateException("No operation found for requestId: " + requestId);
            }
            
            return operations.get(0);
        } catch (Exception e) {
            log.error("Error retrieving operation for requestId: {}: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve operation: " + e.getMessage(), e);
        }
    }

    /**
     * Get file content from operation, handling both String and byte[] formats
     */
    public byte[] getFileContentFromOperation(Map<String, Object> operation, String requestId) {
        Object fileContentObj = operation.get("fileContent");
        
        if (fileContentObj == null) {
            throw new IllegalStateException("File content not found for requestId: " + requestId);
        }
        
        // Handle different types of file content storage
        if (fileContentObj instanceof String) {
            // File content stored as String (preferred format)
            log.debug("Converting String file content to bytes for requestId: {}", requestId);
            return ((String) fileContentObj).getBytes(StandardCharsets.UTF_8);
        } else if (fileContentObj instanceof byte[]) {
            // For backward compatibility with existing data
            log.warn("File content is stored as byte array for requestId: {}. This format is deprecated.", requestId);
            return (byte[]) fileContentObj;
        } else {
            throw new IllegalStateException("Unsupported file content format for requestId: " + requestId + 
                ". Expected String or byte[], got: " + fileContentObj.getClass().getName());
        }
    }

    /**
     * Get token from operation
     */
    public String getTokenFromOperation(Map<String, Object> operation, String requestId) {
        String token = (String) operation.get("token");
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Token is null or empty for requestId: " + requestId);
        }
        return token;
    }

    /**
     * Generate a unique request ID for COMCRYPT operations
     */
    public String generateRequestId(String appCode) {
        String actualAppCode = (appCode != null && !appCode.trim().isEmpty()) ? appCode.trim() : "OCMSLTA001";
        return actualAppCode + "-" + UUID.randomUUID().toString();
    }

    /**
     * Get operation statistics by status
     */
    public Map<String, Long> getOperationStatusCounts() {
        Map<String, Long> statusCounts = new HashMap<>();
        
        // Use local array to avoid IDE resolution issues with nested static classes
        String[] allStatuses = {
            SystemConstant.CryptOperation.Status.REQUESTED,
            SystemConstant.CryptOperation.Status.IN_PROGRESS,
            SystemConstant.CryptOperation.Status.PROCESSED,
            SystemConstant.CryptOperation.Status.UPLOADED,
            SystemConstant.CryptOperation.Status.COMPLETED,
            SystemConstant.CryptOperation.Status.COMPLETED_WITH_ERRORS,
            SystemConstant.CryptOperation.Status.FAILED,
            SystemConstant.CryptOperation.Status.TIMEOUT
        };
        
        for (String status : allStatuses) {
            try {
                Map<String, Object> filters = new HashMap<>();
                filters.put("status", status);
                long count = tableQueryService.count("ocms_comcrypt_operation", filters);
                statusCounts.put(status, count);
            } catch (Exception e) {
                log.warn("Error counting operations for status {}: {}", status, e.getMessage());
                statusCounts.put(status, 0L);
            }
        }
        
        return statusCounts;
    }

    /**
     * Get recent operation count (last N hours)
     */
    public long getRecentOperationCount(int hours) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("creDate", Map.of("$gte", LocalDateTime.now().minusHours(hours)));
            return tableQueryService.count("ocms_comcrypt_operation", filters);
        } catch (Exception e) {
            log.warn("Error counting recent operations: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get recent failed operation count (last N hours)
     */
    public long getRecentFailedOperationCount(int hours) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("creDate", Map.of("$gte", LocalDateTime.now().minusHours(hours)));
            return tableQueryService.count("ocms_comcrypt_operation", filters);
        } catch (Exception e) {
            log.warn("Error counting recent failed operations: {}", e.getMessage());
            return 0;
        }
    }
}