package com.ocmsintranet.cronservice.utilities.comcrypt;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for COMCRYPT (Secure Lightweight File Transfer)
 * Manages configuration settings and token storage for COMCRYPT operations
 */
@Slf4j
@Component
public class ComcryptConfig {    
    @Getter
    @Setter
    private String comcryptAppToken;

    /**
     * Check if a valid token exists
     * 
     * @return true if a non-empty token exists, false otherwise
     */
    public boolean hasValidToken() {
        return comcryptAppToken != null && !comcryptAppToken.isEmpty();
    }
    
    /**
     * Clear the current token
     */
    public void clearToken() {
        log.info("Clearing COMCRYPT token");
        comcryptAppToken = null;
    }
}
