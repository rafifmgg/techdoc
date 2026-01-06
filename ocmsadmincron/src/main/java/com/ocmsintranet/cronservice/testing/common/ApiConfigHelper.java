package com.ocmsintranet.cronservice.testing.common;

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
public class ApiConfigHelper {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private String apiBaseUrl;

    @PostConstruct
    private void initializeApiConfiguration() {
        setApiBaseUrl();
    }

    /**
     * Set API base URL based on active spring profile
     */
    private void setApiBaseUrl() {
        switch (activeProfile) {
            case "local":
                apiBaseUrl = "http://localhost:8083/ocms";
                break;
            case "sit":
                // apiBaseUrl = "https://ocmsapisitintraapp.azurewebsites.net/ocms";
                apiBaseUrl = "http://localhost:8083/ocms";
                break;
            case "dev":
                apiBaseUrl = "https://ocmsapidevintraapp.appsvc-ura-deviptapp-ase3-cps-fe.appserviceenvironment.net/ocms";
                break;
            case "uat":
                apiBaseUrl = "https://ocmsapiuatintraapp.appsvc-ura-uatiptapp-ase3-cps-fe.appserviceenvironment.net/ocms";
                break;
            default:
                apiBaseUrl = "http://localhost:8083/ocms";
                log.warn("⚠️  Unknown profile '{}', defaulting to local configuration", activeProfile);
                break;
        }
        log.info("🔗 API Base URL configured for profile '{}': {}", activeProfile, apiBaseUrl);
    }

    /**
     * Get the configured API base URL
     * @return API base URL for current environment
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
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
            return apiBaseUrl;
        }

        String cleanEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return apiBaseUrl + cleanEndpoint;
    }
}
