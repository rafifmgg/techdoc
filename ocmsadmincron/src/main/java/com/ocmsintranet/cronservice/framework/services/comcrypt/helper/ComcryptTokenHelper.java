package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation.OcmsComcryptOperation;
import com.ocmsintranet.cronservice.utilities.comcrypt.ComcryptUtils;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper class for COMCRYPT token operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComcryptTokenHelper {

    private final ComcryptUtils comcryptUtils;
    private final ComcryptOperationHelper operationHelper;

    /**
     * Request a COMCRYPT token
     */
    public String requestToken(String appCode, String operationType, String fileName, String metadata) {
        log.info("[COMCRYPT-TOKEN-REQUEST] operation: {}, fileName: {}", operationType, fileName);
        
        String requestId = generateRequestId(appCode);
        
        // Create operation using OcmsComcryptOperation
        OcmsComcryptOperation operation = new OcmsComcryptOperation();
        operation.setRequestId(requestId);
        operation.setOperationType(operationType);
        operation.setFileName(fileName);
        
        operationHelper.saveOperation(operation);
        log.info("[COMCRYPT-TOKEN-OPERATION-CREATED] requestId: {}, operationType: {}", requestId, operationType);
        
        // Request token from COMCRYPT service with single retry
        boolean tokenRequested = false;
        int maxRetries = 1;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            tokenRequested = comcryptUtils.requestToken(appCode, requestId);
            
            if (tokenRequested) {
                log.info("[COMCRYPT-TOKEN-SENT] COMCRYPT token requested successfully for requestId: {} on attempt {}", requestId, attempt);
                break;
            } else if (attempt <= maxRetries) {
                log.warn("[COMCRYPT-TOKEN-RETRY] Token request failed for requestId: {}, retrying (attempt {}/{})", requestId, attempt, maxRetries + 1);
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[COMCRYPT-TOKEN-INTERRUPTED] Retry interrupted for requestId: {}", requestId);
                    break;
                }
            }
        }
        
        if (!tokenRequested) {
            log.error("[COMCRYPT-TOKEN-FAILED] Failed to request COMCRYPT token after {} attempts for requestId: {}", maxRetries + 1, requestId);
            return null;
        }
        
        return requestId;
    }

    /**
     * Get token for a request ID
     */
    public Optional<String> getToken(String requestId) {
        log.debug("[COMCRYPT-TOKEN-CHECK] Checking if token has been received for requestId: {}", requestId);
        
        OcmsComcryptOperation operation = operationHelper.findOperationByRequestId(requestId);
        
        if (operation != null && operation.getToken() != null) {
            log.debug("[COMCRYPT-TOKEN-FOUND] Token found for requestId: {}", requestId);
            return Optional.of(operation.getToken());
        }
        
        log.debug("[COMCRYPT-TOKEN-NOT-FOUND] No token found yet for requestId: {}", requestId);
        return Optional.empty();        
    }

    /**
     * Wait for token with timeout
     */
    public Optional<String> waitForToken(String requestId, long maxWaitTimeMs, long checkIntervalMs) {
        log.info("[COMCRYPT-TOKEN-WAIT] Waiting for COMCRYPT token for requestId: {}, maxWaitTime: {} ms", requestId, maxWaitTimeMs);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // ✅ Multi-server compatible: Use ComcryptUtils.waitForToken with requestId
            String token = comcryptUtils.waitForToken(maxWaitTimeMs, checkIntervalMs, requestId);
            
            if (token != null) {
                long waitTime = System.currentTimeMillis() - startTime;
                log.info("[COMCRYPT-TOKEN-RECEIVED] Token received for requestId: {} after {} ms", requestId, waitTime);
                return Optional.of(token);
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-TOKEN-ERROR] Error waiting for token for requestId: {}", requestId, e);
        }
        
        log.warn("[COMCRYPT-TOKEN-TIMEOUT] Timeout waiting for token for requestId: {}", requestId);
        return Optional.empty();
    }

    /**
     * Wait for token indefinitely (no timeout)
     */
    public Optional<String> waitForToken(String requestId) {
        log.info("[COMCRYPT-TOKEN-WAIT] Waiting for COMCRYPT token for requestId: {} (no timeout)", requestId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Wait indefinitely for token
            String token = comcryptUtils.waitForToken(requestId);
            
            if (token != null) {
                long waitTime = System.currentTimeMillis() - startTime;
                log.info("[COMCRYPT-TOKEN-RECEIVED] Token received for requestId: {} after {} ms", requestId, waitTime);
                return Optional.of(token);
            }
        } catch (Exception e) {
            log.error("[COMCRYPT-TOKEN-ERROR] Error waiting for token for requestId: {}", requestId, e);
        }
        
        log.warn("[COMCRYPT-TOKEN-FAILED] Failed to get token for requestId: {}", requestId);
        return Optional.empty();
    }

    /**
     * Generate unique request ID
     */
    private String generateRequestId(String appCode) {
        return appCode + "_REQ_" + System.currentTimeMillis();
    }
}
