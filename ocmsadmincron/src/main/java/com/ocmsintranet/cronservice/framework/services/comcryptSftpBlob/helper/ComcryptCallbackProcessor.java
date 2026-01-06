package com.ocmsintranet.cronservice.framework.services.comcryptSftpBlob.helper;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for managing COMCRYPT token callbacks.
 * This class handles:
 * - Registration of callback handlers for specific request IDs
 * - Processing of token callbacks and routing to the correct handler
 * - Cleanup of expired callback handlers
 * 
 * Updated to use only requestId-based routing without workflow type dependencies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptCallbackProcessor {

    private final TableQueryService tableQueryService;
    
    // Map to store callback handlers by request ID
    private final Map<String, CallbackHandler> callbackHandlers = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(2);
    
    // Default cleanup interval (in minutes)
    private static final long CLEANUP_INTERVAL_MINUTES = 60;
    
    // Default handler expiry time (in minutes)
    private static final long HANDLER_EXPIRY_MINUTES = 120;
    
    /**
     * Inner class to track callback handlers with timestamps
     */
    private static class CallbackHandler {
        private final Runnable handler;
        private final LocalDateTime creDate;
        
        public CallbackHandler(Runnable handler) {
            this.handler = handler;
            this.creDate = LocalDateTime.now();
        }
        
        public Runnable getHandler() { return handler; }
        public LocalDateTime getCreDate() { return creDate; }
        
        public boolean isExpired(long expiryMinutes) {
            return LocalDateTime.now().isAfter(creDate.plusMinutes(expiryMinutes));
        }
    }
    
    /**
     * Initialize cleanup task
     */
    public void init() {
        // Schedule periodic cleanup of expired callback handlers
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredHandlers,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        log.info("Initialized ComcryptCallbackProcessor with periodic cleanup every {} minutes", CLEANUP_INTERVAL_MINUTES);
    }
    
    /**
     * Register a callback handler for a specific request ID
     * 
     * @param requestId The request ID
     * @param handler The callback handler
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void registerCallbackHandler(String requestId, Runnable handler) {
        if (requestId == null || requestId.isEmpty()) {
            log.error("Cannot register callback handler with null or empty request ID");
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        
        if (handler == null) {
            log.error("Cannot register null callback handler for request ID: {}", requestId);
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        CallbackHandler callbackHandler = new CallbackHandler(handler);
        callbackHandlers.put(requestId, callbackHandler);
        log.info("Registered callback handler for request ID: {}", requestId);
    }
    
    /**
     * Remove a callback handler for a specific request ID
     * 
     * @param requestId The request ID
     * @return true if a handler was removed, false if no handler was registered
     */
    public boolean removeCallbackHandler(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            log.error("Cannot remove callback handler with null or empty request ID");
            return false;
        }
        
        CallbackHandler removed = callbackHandlers.remove(requestId);
        if (removed != null) {
            log.info("Removed callback handler for request ID: {}", requestId);
            return true;
        } else {
            log.debug("No callback handler found to remove for request ID: {}", requestId);
            return false;
        }
    }
    
    /**
     * Process a token callback for a specific request ID
     * Updated to use only requestId-based routing without workflow type dependencies.
     * 
     * @param requestId The request ID
     * @param token The COMCRYPT token
     * @return true if the callback was processed successfully, false otherwise
     */
    public boolean processTokenCallback(String requestId, String token) {
        log.info("Processing token callback for request ID: {}", requestId);
        
        if (requestId == null || requestId.isEmpty()) {
            log.error("Cannot process token callback with null or empty request ID");
            return false;
        }
        
        if (token == null || token.isEmpty()) {
            log.error("Cannot process token callback with null or empty token for request ID: {}", requestId);
            return false;
        }
        
        try {
            // Get the operation from the database using TableQueryService
            Map<String, Object> filters = new HashMap<>();
            filters.put("requestId", requestId);
            
            List<Map<String, Object>> operations = tableQueryService.query("ocms_comcrypt_operation", filters);
            if (operations.isEmpty()) {
                log.error("No COMCRYPT operation found for request ID: {}", requestId);
                return false;
            }
            
            Map<String, Object> operation = operations.get(0);
            
            // Check the current status of the operation
            Object statusObj = operation.get("status");
            String currentStatus = statusObj != null ? statusObj.toString() : null;
            if (currentStatus != null && 
                !SystemConstant.CryptOperation.Status.REQUESTED.equals(currentStatus) &&
                !SystemConstant.CryptOperation.Status.IN_PROGRESS.equals(currentStatus)) {
                log.warn("Received token callback for operation in unexpected status: {} for request ID: {}", 
                    currentStatus, requestId);
            }
            
            // Update the operation with the token
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("token", token);            
            List<Map<String, Object>> updatedOperations = tableQueryService.patch("ocms_comcrypt_operation", filters, updateData);
            
            if (updatedOperations == null || updatedOperations.isEmpty()) {
                log.error("Failed to update COMCRYPT operation with token for request ID: {}", requestId);
                return false;
            }
            
            log.info("Updated COMCRYPT operation with token for request ID: {}", requestId);
            
            // Check if there's a registered callback handler for this request ID
            CallbackHandler callbackHandler = callbackHandlers.get(requestId);
            if (callbackHandler != null) {
                log.info("Executing registered callback handler for request ID: {}", requestId);
                try {
                    callbackHandler.getHandler().run();
                    callbackHandlers.remove(requestId); // Remove the handler after successful execution
                    log.info("Successfully executed callback handler for request ID: {}", requestId);
                    return true;
                } catch (Exception e) {
                    log.error("Error executing callback handler for request ID: {}: {}", requestId, e.getMessage(), e);
                                        
                    callbackHandlers.remove(requestId); // Remove the handler even if it failed
                    return false;
                }
            }
            
            // No callback handler registered - this is expected in some scenarios
            log.warn("No callback handler registered for request ID: {} - token stored but no action taken", requestId);
            return true;
            
        } catch (Exception e) {
            log.error("Error processing token callback for request ID: {}: {}", requestId, e.getMessage(), e);
                        
            return false;
        }
    }
    
    /**
     * Get the count of registered callback handlers
     * 
     * @return The number of registered callback handlers
     */
    public int getCallbackHandlerCount() {
        return callbackHandlers.size();
    }
    
    /**
     * Check if a callback handler is registered for a specific request ID
     * 
     * @param requestId The request ID
     * @return true if a handler is registered, false otherwise
     */
    public boolean hasCallbackHandler(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return false;
        }
        return callbackHandlers.containsKey(requestId);
    }
    
    /**
     * Get statistics about callback processing
     * Updated to show only requestId-based handler statistics.
     * 
     * @return A string containing statistics
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Callback Processor Statistics:\n");
        stats.append("- Active Callback Handlers: ").append(callbackHandlers.size()).append("\n");
        
        // Group handlers by age
        long lessThan10Min = 0;
        long lessThan30Min = 0;
        long lessThan60Min = 0;
        long moreThan60Min = 0;
        
        LocalDateTime now = LocalDateTime.now();
        for (CallbackHandler handler : callbackHandlers.values()) {
            long ageMinutes = java.time.Duration.between(handler.getCreDate(), now).toMinutes();
            if (ageMinutes < 10) {
                lessThan10Min++;
            } else if (ageMinutes < 30) {
                lessThan30Min++;
            } else if (ageMinutes < 60) {
                lessThan60Min++;
            } else {
                moreThan60Min++;
            }
        }
        
        stats.append("- Handler Age Distribution:\n");
        stats.append("  - < 10 minutes: ").append(lessThan10Min).append("\n");
        stats.append("  - 10-30 minutes: ").append(lessThan30Min).append("\n");
        stats.append("  - 30-60 minutes: ").append(lessThan60Min).append("\n");
        stats.append("  - > 60 minutes: ").append(moreThan60Min).append("\n");
        stats.append("- Cleanup Interval: ").append(CLEANUP_INTERVAL_MINUTES).append(" minutes\n");
        stats.append("- Handler Expiry: ").append(HANDLER_EXPIRY_MINUTES).append(" minutes");
        
        return stats.toString();
    }
    
    /**
     * Get a list of all active request IDs with callback handlers
     * 
     * @return A list of request IDs
     */
    public java.util.Set<String> getActiveRequestIds() {
        return new java.util.HashSet<>(callbackHandlers.keySet());
    }
    
    /**
     * Clean up expired callback handlers
     */
    private void cleanupExpiredHandlers() {
        try {
            int initialCount = callbackHandlers.size();
            if (initialCount == 0) {
                return; // No handlers to clean up
            }
            
            // Find expired handlers
            List<String> expiredRequestIds = callbackHandlers.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(HANDLER_EXPIRY_MINUTES))
                .map(Map.Entry::getKey)
                .toList();
            
            // Remove expired handlers
            for (String requestId : expiredRequestIds) {
                callbackHandlers.remove(requestId);
                log.warn("Removed expired callback handler for request ID: {}", requestId);
                
                // Update operation status to reflect the timeout
                try {
                    Map<String, Object> filters = new HashMap<>();
                    filters.put("requestId", requestId);
                    
                    Map<String, Object> updateData = new HashMap<>();                    
                    tableQueryService.patch("ocms_comcrypt_operation", filters, updateData);
                } catch (Exception e) {
                    log.error("Failed to update operation status for expired handler (request ID: {}): {}", 
                        requestId, e.getMessage());
                }
            }
            
            if (!expiredRequestIds.isEmpty()) {
                log.info("Cleaned up {} expired callback handlers (had {} total)", 
                    expiredRequestIds.size(), initialCount);
            }
            
        } catch (Exception e) {
            log.error("Error during callback handler cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shutdown the cleanup executor
     */
    public void shutdown() {
        try {
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
            log.info("ComcryptCallbackProcessor cleanup executor has been shut down");
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("ComcryptCallbackProcessor cleanup executor shutdown was interrupted");
        }
    }
}
