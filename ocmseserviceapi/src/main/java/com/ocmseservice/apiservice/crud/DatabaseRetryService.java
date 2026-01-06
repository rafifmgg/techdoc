package com.ocmseservice.apiservice.crud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.function.Supplier;

@Service
public class DatabaseRetryService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryService.class);
    
    private final RetryTemplate retryTemplate;
    
    public DatabaseRetryService(@Qualifier("dataSourceRetryTemplate") RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
        
        // Add listener for logging retry attempts
        this.retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                if (context.getRetryCount() > 0) {
                    logger.info("Retrying database operation (attempt {})", context.getRetryCount());
                }
                return true;
            }
            
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                if (throwable == null && context.getRetryCount() > 0) {
                    logger.info("Database operation succeeded after {} attempts", context.getRetryCount());
                }
            }
            
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                logger.warn("Database operation failed (attempt {}): {}", 
                           context.getRetryCount(), 
                           throwable.getMessage());
            }
        });
    }
    
    /**
     * Execute a database operation with retry capability
     * 
     * @param operation The database operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return retryTemplate.execute(context -> {
            try {
                return operation.get();
            } catch (Exception e) {
                logger.error("Database operation failed: {}", e.getMessage(), e);
                throw new RuntimeException("Database operation failed", e);
            }

            // try {
            //     return operation.get();
            // } catch (Exception e) {
            //     if (isRetryableException(e)) {
            //         logger.warn("Encountered retryable exception: {}", e.getMessage());
            //         throw e;
            //     } else {
            //         logger.error("Encountered non-retryable exception: {}", e.getMessage());
            //         throw new RuntimeException("Database operation failed", e);
            //     }
            // }
        });
    }
    
    /**
     * Execute a database operation that doesn't return a value with retry capability
     * 
     * @param operation The database operation to execute
     */
    public void executeWithRetry(Runnable operation) {
        retryTemplate.execute(context -> {
            try {
                operation.run();
                return null;
            } catch (Exception e) {
                if (isRetryableException(e)) {
                    logger.warn("Encountered retryable exception: {}", e.getMessage());
                    throw e;
                } else {
                    logger.error("Encountered non-retryable exception: {}", e.getMessage());
                    throw new RuntimeException("Database operation failed", e);
                }
            }
        });
    }
    
    /**
     * Determine if an exception is retryable
     * 
     * @param e The exception to check
     * @return True if the exception is retryable, false otherwise
     */
    private boolean isRetryableException(Exception e) {
        // Check for specific retryable exceptions
        if (e instanceof SQLException) {
            SQLException sqlException = (SQLException) e;
            int errorCode = sqlException.getErrorCode();
            
            // Timeout errors: typically 8001, -2, etc. in SQL Server
            if (errorCode == 8001 || errorCode == -2 || 
                sqlException.getMessage().toLowerCase().contains("timeout") ||
                sqlException.getMessage().toLowerCase().contains("connection")) {
                return true;
            }
            
            // Deadlock errors: 1205 in SQL Server
            if (errorCode == 1205) {
                return true;
            }
            
            // Resource errors: 40197, 40143, 40613, etc. in SQL Server
            if (errorCode == 40197 || errorCode == 40143 || errorCode == 40613) {
                return true;
            }
        }
        
        // Check for common connection exceptions
        String message = e.getMessage().toLowerCase();
        return message.contains("connection") ||
               message.contains("timeout") ||
               message.contains("not open") ||
               message.contains("broken pipe") ||
               message.contains("reset") ||
               message.contains("refused") ||
               message.contains("unavailable");
    }
}