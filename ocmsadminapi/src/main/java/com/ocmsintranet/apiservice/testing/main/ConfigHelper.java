package com.ocmsintranet.apiservice.testing.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Helper class for API configuration based on environment profiles.
 * Provides dynamic API base URL configuration for different environments.
 */
@Slf4j
@Component
public class ConfigHelper {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private String baseUrl;

    @PostConstruct
    private void initializeApiConfiguration() {
        setBaseUrl();
    }

    /**
     * Set API base URL based on active spring profile
     */
    private void setBaseUrl() {
        switch (activeProfile) {
            case "local":
                baseUrl = "http://localhost:8085";
                break;
            case "sit":
                baseUrl = "https://ocmssitintraapp.azurewebsites.net/ocms";
                break;
            case "dev":
                baseUrl = "https://ocmsdevintraapp.appsvc-ura-deviptapp-ase3-cps-fe.appserviceenvironment.net/ocms";
                break;
            case "uat":
                baseUrl = "https://ocmsuatintraapp.appsvc-ura-uatiptapp-ase3-cps-fe.appserviceenvironment.net/ocms";
                break;
            default:
                baseUrl = "http://localhost:8083/ocms";
                log.warn("⚠️  Unknown profile '{}', defaulting to local configuration", activeProfile);
                break;
        }
        log.info("🔗 API Base URL configured for profile '{}': {}", activeProfile, baseUrl);
    }

    /**
     * Get the configured API base URL
     * @return API base URL for current environment
     */
    public String getApiBaseUrl() {
        return baseUrl;
    }

    /**
     * Get the active spring profile
     * @return current active profile
     */
    public String getActiveProfile() {
        return activeProfile;
    }

    /**
     * Build full API endpoint URL
     * @param endpoint the specific endpoint path (e.g., "/api/datahive/test/nric")
     * @return full URL combining base URL and endpoint
     */
    public String buildApiUrl(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return baseUrl;
        }

        String cleanEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return baseUrl + cleanEndpoint;
    }
}
