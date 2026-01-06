package com.ocmsintranet.cronservice.config;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.net.SocketException;

/**
 * Exception handler to catch and suppress specific exceptions
 * like ClientAbortException that occur when clients disconnect prematurely.
 */
@ControllerAdvice
public class ClientDisconnectionExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientDisconnectionExceptionHandler.class);

    /**
     * Handles ClientAbortException which occurs when a client disconnects before
     * the server finishes sending the response.
     * 
     * @param ex The ClientAbortException
     */
    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.OK) // Return 200 OK to avoid error logs
    public void handleClientAbortException(ClientAbortException ex) {
        // Intentionally empty - we don't want to log anything for this exception
    }
    
    /**
     * Handles SocketException which often occurs with "Broken pipe" when clients disconnect.
     * 
     * @param ex The SocketException
     */
    @ExceptionHandler(SocketException.class)
    @ResponseStatus(HttpStatus.OK) // Return 200 OK to avoid error logs
    public void handleSocketException(SocketException ex) {
        // Only log at trace level if the message contains "Broken pipe"
        if (ex.getMessage() != null && ex.getMessage().contains("Broken pipe")) {
            // Intentionally empty - we don't want to log anything for this exception
        } else {
            // For other socket exceptions, log at debug level
            if (logger.isDebugEnabled()) {
                logger.debug("Socket exception occurred", ex);
            }
        }
    }
    
    /**
     * Handles IOException which may be related to client disconnections.
     * 
     * @param ex The IOException
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.OK) // Return 200 OK to avoid error logs
    public void handleIOException(IOException ex) {
        // Check if this is a broken pipe or connection reset
        if (ex.getMessage() != null && 
            (ex.getMessage().contains("Broken pipe") || 
             ex.getMessage().contains("Connection reset") ||
             ex.getMessage().contains("Connection abort"))) {
            // Intentionally empty - we don't want to log anything for these exceptions
        } else {
            // For other IO exceptions, log at debug level
            if (logger.isDebugEnabled()) {
                logger.debug("IO exception occurred", ex);
            }
        }
    }
}
