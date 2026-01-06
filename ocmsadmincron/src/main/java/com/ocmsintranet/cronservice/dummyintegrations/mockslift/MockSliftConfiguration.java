package com.ocmsintranet.cronservice.dummyintegrations.mockslift;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for the mock SLIFT server
 * 
 * This configuration is only active in the local profile and provides
 * necessary beans for the mock SLIFT server to function.
 */
@Slf4j
@Configuration
@Profile({"local", "sit"})
public class MockSliftConfiguration {

    /**
     * Initialize the mock SLIFT server when the application starts in local profile
     */
    @Value("${ocms.APIM.baseurl:http://localhost:8083}")
    private String baseUrl;
    
    @Value("${ocms.comcrypt.token:/crypt/token/get/v1}")
    private String tokenPath;
    
    @Value("${ocms.comcrypt.encrypting:/crypt/sliftencrypt/OCMSLTA001/v1}")
    private String encryptPath;
    
    @Value("${ocms.comcrypt.decrypt.slift:/crypt/sliftdecrypt/OCMSLTA001/v1}")
    private String decryptPath;
    
    /**
     * Initialize the mock SLIFT server and log available endpoints
     * 
     * @return A string identifier for the bean
     */
    @Bean
    public String initializeMockSlift() {
        log.info("Initializing Mock SLIFT Server for development/testing");
        log.info("Mock SLIFT endpoints available at:");
        log.info("  - Token Request: {}{}", baseUrl, tokenPath);
        log.info("  - Encryption: {}{}", baseUrl, encryptPath);
        log.info("  - Decryption: {}{}", baseUrl, decryptPath);
        
        return "mockSliftInitializer";
    }
    
    /**
     * Provide a RestTemplate for the mock SLIFT controller if not already available
     */
    @Bean
    @Profile({"local", "sit"})
    public RestTemplate mockSliftRestTemplate() {
        return new RestTemplate();
    }
}
