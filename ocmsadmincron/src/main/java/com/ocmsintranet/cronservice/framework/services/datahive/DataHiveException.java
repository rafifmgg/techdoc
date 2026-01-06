package com.ocmsintranet.cronservice.framework.services.datahive;

/**
 * Exception thrown when DataHive operations fail
 */
public class DataHiveException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new DataHive exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public DataHiveException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new DataHive exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public DataHiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
