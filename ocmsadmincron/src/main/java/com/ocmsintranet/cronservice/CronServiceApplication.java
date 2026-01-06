package com.ocmsintranet.cronservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class CronServiceApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CronServiceApplication.class);
    }

    public static void main(String[] args) {
        // Get active profile
        String activeProfile = System.getProperty("spring.profiles.active", "local");
        System.out.println("Active profile: " + activeProfile);
        
        // Read properties from application properties files
        Properties appProps = new Properties();
        Properties profileProps = new Properties();
        
        try {
            // Load default properties
            InputStream appInputStream = CronServiceApplication.class.getClassLoader().getResourceAsStream("application.properties");
            if (appInputStream != null) {
                appProps.load(appInputStream);
                appInputStream.close();
            }
            
            // Load profile-specific properties
            InputStream profileInputStream = CronServiceApplication.class.getClassLoader().getResourceAsStream("application-" + activeProfile + ".properties");
            if (profileInputStream != null) {
                profileProps.load(profileInputStream);
                profileInputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error loading properties: " + e.getMessage());
        }
        
        // CRITICAL: Set system properties for Azure Blob logging from application properties
        System.out.println("Setting Azure Blob logging properties from application properties");
        
        // Get properties with defaults if not found
        String endpoint = getProperty(appProps, profileProps, "azure.log.endpoint", "https://uraproject.blob.core.windows.net");
        String container = getProperty(appProps, profileProps, "azure.log.container", "ocmsiz-applog-test");
        String prefix1 = getProperty(appProps, profileProps, "azure.log.prefix1", "ocmsadmincron");
        
        // Set system properties for Log4j2
        System.setProperty("azure.log.endpoint", endpoint);
        System.setProperty("azure.log.container", container);
        System.setProperty("azure.log.prefix1", prefix1);
        
        System.out.println("Using " + activeProfile.toUpperCase() + " Azure Blob logging settings");
        
        // Print the system properties to verify they are set correctly
        System.out.println("Azure Blob logging properties set:");
        System.out.println("azure.log.endpoint: " + endpoint);
        System.out.println("azure.log.container: " + container);
        System.out.println("azure.log.prefix1: " + prefix1);
        
        // Add try-catch to capture the actual Spring Boot exception
        try {
            SpringApplication.run(CronServiceApplication.class, args);
        } catch (Exception e) {
            System.err.println("=== SPRING BOOT STARTUP FAILED ===");
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("=== ROOT CAUSE ===");
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.err.println("Root Cause: " + rootCause.getClass().getName());
            System.err.println("Root Message: " + rootCause.getMessage());
            System.err.println("=== FULL STACK TRACE ===");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Helper method to get a property value from profile-specific properties first,
     * then from default properties, and finally using the default value if not found.
     */
    private static String getProperty(Properties defaultProps, Properties profileProps, String key, String defaultValue) {
        // Check profile-specific properties first
        String value = profileProps.getProperty(key);
        
        // If not found, check default properties
        if (value == null) {
            value = defaultProps.getProperty(key);
        }
        
        // If still not found, use the default value
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
}