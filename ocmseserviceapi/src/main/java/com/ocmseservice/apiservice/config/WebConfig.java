package com.ocmseservice.apiservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Web configuration for the application.
 * Single source of CORS configuration that reads all settings from application properties.
 */
@Configuration
public class WebConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsProperty;
    
    @Value("${cors.allowed-methods:}")
    private String allowedMethodsProperty;
    
    @Value("${cors.allowed-headers:}")
    private String allowedHeadersProperty;
    
    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;
    
    @Value("${cors.max-age:0}")
    private long maxAge;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    /**
     * Configures CORS for the application.
     * This ensures that frontend applications can access the API from different origins.
     * All settings are read from application properties files for each environment.
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer webConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                // Validate ALL required properties - no hardcoded defaults
                if (allowedMethodsProperty == null || allowedMethodsProperty.isEmpty()) {
                    logger.error("cors.allowed-methods not defined in properties, CORS configuration skipped!");
                    return;
                }
                
                if (allowedHeadersProperty == null || allowedHeadersProperty.isEmpty()) {
                    logger.error("cors.allowed-headers not defined in properties, CORS configuration skipped!");
                    return;
                }
                
                if (allowedOriginsProperty == null || allowedOriginsProperty.isEmpty()) {
                    logger.error("cors.allowed-origins property is not defined in application properties - CORS configuration skipped!");
                    return;
                }
                
                // Split the comma-separated lists
                String[] allowedOrigins = allowedOriginsProperty.split(",");
                String[] allowedMethods = allowedMethodsProperty.split(",");
                String[] allowedHeaders = allowedHeadersProperty.split(",");
                
                // Trim whitespace from all values
                for (int i = 0; i < allowedOrigins.length; i++) {
                    allowedOrigins[i] = allowedOrigins[i].trim();
                }
                for (int i = 0; i < allowedMethods.length; i++) {
                    allowedMethods[i] = allowedMethods[i].trim();
                }
                for (int i = 0; i < allowedHeaders.length; i++) {
                    allowedHeaders[i] = allowedHeaders[i].trim();
                }
                
                // Log the configured settings for debugging
                logger.info("CORS configured with:\n" + 
                         " - allowed origins: {}\n" + 
                         " - allowed methods: {}\n" + 
                         " - allowed headers: {}\n" + 
                         " - allow credentials: {}\n" + 
                         " - max age: {}\n" + 
                         " - for active profile: {}", 
                         Arrays.toString(allowedOrigins),
                         Arrays.toString(allowedMethods),
                         Arrays.toString(allowedHeaders),
                         allowCredentials,
                         maxAge,
                         activeProfiles);
                
                // Apply the CORS configuration
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders)
                        .allowCredentials(allowCredentials)
                        .maxAge(maxAge);
            }
            
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SecurityHeadersInterceptor());
            }
        };
    }
    
    /**
     * Interceptor to add security headers to all API responses.
     * Implements Content Security Policy (CSP) and other security headers.
     */
    private class SecurityHeadersInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
            // Content Security Policy (CSP)
            // Build connect-src directive from allowed origins
            StringBuilder cspConnectSrc = new StringBuilder("'self'");
            
            if (allowedOriginsProperty != null && !allowedOriginsProperty.isEmpty()) {
                String[] allowedOrigins = allowedOriginsProperty.split(",");
                for (int i = 0; i < allowedOrigins.length; i++) {
                    String origin = allowedOrigins[i].trim();
                    if (!origin.isEmpty() && !origin.equals("*")) {
                        cspConnectSrc.append(" ").append(origin);
                    }
                }
            }
            
            response.setHeader("Content-Security-Policy", 
                    "default-src 'none'; connect-src " + cspConnectSrc.toString() + "; frame-ancestors 'none'; form-action 'none'; base-uri 'none'; block-all-mixed-content");
            
            // Prevent browsers from MIME-sniffing
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // Prevent clickjacking
            response.setHeader("X-Frame-Options", "DENY");
            
            // Enable browser XSS protection
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Prevent all caching for API responses
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            
            // Prevent information leakage
            response.setHeader("Referrer-Policy", "no-referrer");
            
            // HSTS (HTTP Strict Transport Security) - force HTTPS
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // Allow JWT tokens to be used
            // The 'Authorization' header is handled by CORS configuration
            
            return true;
        }
    }
}