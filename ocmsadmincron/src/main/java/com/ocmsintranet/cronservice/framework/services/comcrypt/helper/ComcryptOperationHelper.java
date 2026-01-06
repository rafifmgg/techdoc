package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for COMCRYPT operations
 * Provides database operations for COMCRYPT tokens and workflow management
 */
@Slf4j
@Service
public class ComcryptOperationHelper {

    private final OcmsComcryptOperationService ocmsComcryptOperationService;
    private int maxQueryLimit = 1000;

    @Autowired
    public ComcryptOperationHelper(OcmsComcryptOperationService ocmsComcryptOperationService) {
        this.ocmsComcryptOperationService = ocmsComcryptOperationService;
    }

    /**
     * Find operation by requestId
     */
    @Transactional(readOnly = true)
    public OcmsComcryptOperation findOperationByRequestId(String requestId) {
        log.info("[COMCRYPT-FIND] Searching for operation with requestId: {}", requestId);
        
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("[COMCRYPT-FIND] requestId is null or empty");
            return null;
        }
        
        Map<String, String[]> params = new HashMap<>();
        params.put("requestId", new String[]{requestId.trim()});
        params.put("$limit", new String[]{"1"});
        
        List<OcmsComcryptOperation> operations = ocmsComcryptOperationService.getAll(params).getItems();
        
        if (operations.isEmpty()) {
            log.warn("[COMCRYPT-FIND] No operation found for requestId: {}", requestId);
            return null;
        }
        
        log.info("[COMCRYPT-FIND] Found operation for requestId: {}", requestId);
        return operations.get(0);
    }

    /**
     * Find all operations (logging only - limited by OcmsComcryptOperation fields)
     */
    @Transactional(readOnly = true)
    public List<OcmsComcryptOperation> findAllOperations() {
        log.info("[COMCRYPT-FIND-ALL] Searching for all operations");
                
        Map<String, String[]> params = new HashMap<>();
        params.put("$limit", new String[]{String.valueOf(maxQueryLimit)});
        
        List<OcmsComcryptOperation> operations = ocmsComcryptOperationService.getAll(params).getItems();
        log.info("[COMCRYPT-FIND-ALL] Found {} total operations", operations.size());
        
        return operations;
    }

    /**
     * Handle timed out operations (logging only - no status field in OcmsComcryptOperation)
     */
    @Transactional
    public int handleTimedOutOperations(int timeoutMinutes) {
        log.info("[COMCRYPT-TIMEOUT-CHECK] Checking for timed out operations (timeout: {} minutes)", timeoutMinutes);
        
        // Note: OcmsComcryptOperation doesn't have status or created_at fields
        // This method will log the limitation but cannot identify timed out operations
        log.warn("[COMCRYPT-LIMITATION] OcmsComcryptOperation lacks status and created_at fields - cannot identify timed out operations");
        
        // Return 0 since we cannot identify timed out operations
        log.info("[COMCRYPT-TIMEOUT-CHECK] No timed out operations identified due to table limitations");
        return 0;
    }

    /**
     * Clean up old operations (logging only - limited by OcmsComcryptOperation fields)
     */
    @Transactional
    public int cleanupOldOperations(int retentionDays) {
        log.info("[COMCRYPT-CLEANUP] Checking for old operations (retention: {} days)", retentionDays);
        
        // Note: OcmsComcryptOperation doesn't have status or completed_at fields
        // This method will log the limitation but cannot identify old operations
        log.warn("[COMCRYPT-LIMITATION] OcmsComcryptOperation lacks status and completed_at fields - cannot identify old operations for cleanup");
        
        // Return 0 since we cannot identify old operations
        log.info("[COMCRYPT-CLEANUP] No old operations identified for cleanup due to table limitations");
        return 0;
    }

    /**
     * Save operation
     */
    @Transactional
    public OcmsComcryptOperation saveOperation(OcmsComcryptOperation operation) {
        log.info("[COMCRYPT-SAVE] Saving operation - requestId: {}, operationType: {}, fileName: {}", 
                operation.getRequestId(), operation.getOperationType(), operation.getFileName());
        
        OcmsComcryptOperation saved = ocmsComcryptOperationService.save(operation);
        log.info("[COMCRYPT-SAVE] Operation saved successfully - requestId: {}", saved.getRequestId());
        return saved;
    }

    /**
     * Check if status indicates completion (string-based status)
     */
    private boolean isCompletedStatus(String status) {
        return "COMPLETED".equals(status) || 
               "FAILED".equals(status) || 
               "COMPLETED_WITH_ERRORS".equals(status) ||
               "TIMEOUT".equals(status);
    }
    
    /**
     * Delete an operation record by requestId (for cleanup after successful completion)
     * 
     * This method implements the "remove record from comcrypt operation table" 
     * step shown in the COMCRYPT flow diagram.
     * 
     * @param requestId The request ID of the operation to delete
     * @return True if operation was deleted, false if not found or deletion failed
     */
    @Transactional
    public boolean deleteOperationByRequestId(String requestId) {
        log.info("[COMCRYPT-CLEANUP] Deleting operation record for requestId: {}", requestId);
        
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("[COMCRYPT-CLEANUP] requestId is null or empty");
            return false;
        }
        
        try {
            boolean deleted = ocmsComcryptOperationService.delete(requestId.trim());
            if (deleted) {
                log.info("[COMCRYPT-CLEANUP] Successfully deleted operation record for requestId: {}", requestId);
                return true;
            } else {
                log.warn("[COMCRYPT-CLEANUP] Operation record not found for requestId: {}", requestId);
                return false;
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-CLEANUP] Failed to delete operation record for requestId: {}", requestId, e);
            return false;
        }
    }
}
