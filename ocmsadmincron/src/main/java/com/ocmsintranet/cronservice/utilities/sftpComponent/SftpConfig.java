package com.ocmsintranet.cronservice.utilities.sftpComponent;

import com.ocmsintranet.cronservice.utilities.AzureKeyVaultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for SFTP server connections.
 * Loads environment-specific SFTP properties and initializes the SftpUtil with server configurations.
 */
@Configuration
@PropertySource(value = "classpath:sftp/sftp-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
public class SftpConfig {
    private static final Logger logger = LoggerFactory.getLogger(SftpConfig.class);

    private SftpUtil sftpUtil;
    private final AzureKeyVaultUtil azureKeyVaultUtil;

    // Azure Blob SFTP server properties
    @Value("${sftp.servers.azureblob.host:}")
    private String azureblobHost;

    @Value("${sftp.servers.azureblob.port:22}")
    private int azureblobPort;

    @Value("${sftp.servers.azureblob.username:}")
    private String azureblobUsername;

    @Value("${sftp.servers.azureblob.secretname:}")
    private String azureblobSecretName;

    // CES SFTP server properties
    @Value("${sftp.servers.ces.host:}")
    private String cesHost;

    @Value("${sftp.servers.ces.port:22}")
    private int cesPort;

    @Value("${sftp.servers.ces.username:}")
    private String cesUsername;

    @Value("${sftp.servers.ces.secretname:}")
    private String cesSecretName;

    // MHA SFTP server properties
    @Value("${sftp.servers.mha.host:}")
    private String mhaHost;

    @Value("${sftp.servers.mha.port:22}")
    private int mhaPort;

    @Value("${sftp.servers.mha.username:}")
    private String mhaUsername;

    @Value("${sftp.servers.mha.secretname:}")
    private String mhaSecretName;

    // Toppan SFTP server properties
    @Value("${sftp.servers.toppan.host:}")
    private String toppanHost;

    @Value("${sftp.servers.toppan.port:22}")
    private int toppanPort;

    @Value("${sftp.servers.toppan.username:}")
    private String toppanUsername;

    @Value("${sftp.servers.toppan.secretname:}")
    private String toppanSecretName;

    // ICMS SFTP server properties
    @Value("${sftp.servers.icms.host:}")
    private String icmsHost;

    @Value("${sftp.servers.icms.port:22}")
    private int icmsPort;

    @Value("${sftp.servers.icms.username:}")
    private String icmsUsername;

    @Value("${sftp.servers.icms.secretname:}")
    private String icmsSecretName;

    // REPCCS SFTP server properties
    @Value("${sftp.servers.repccs.host:}")
    private String repCcsHost;

    @Value("${sftp.servers.repccs.port:22}")
    private int repCcsPort;

    @Value("${sftp.servers.repccs.username:}")
    private String repCcsUsername;

    @Value("${sftp.servers.repccs.secretname:}")
    private String repCcsSecretName;

    // LTA SFTP server properties
    @Value("${sftp.servers.lta.host:}")
    private String ltaHost;

    @Value("${sftp.servers.lta.port:22}")
    private int ltaPort;

    @Value("${sftp.servers.lta.username:}")
    private String ltaUsername;

    @Value("${sftp.servers.lta.secretname:}")
    private String ltaSecretName;

    @Value("${ocms.azure.keyvault.sftp.url:${ocms.azure.keyvault.url}}")
    private String sftpKeyVaultUrl;

    /**
     * Constructor that initializes dependencies
     */
    public SftpConfig(AzureKeyVaultUtil azureKeyVaultUtil) {
        this.azureKeyVaultUtil = azureKeyVaultUtil;
        // SftpUtil will be initialized in the @PostConstruct method
        // after all properties have been injected
    }
    
    /**
     * Initialize server configurations after all properties have been injected
     * This method runs after Spring has completed dependency injection
     */
    @PostConstruct
    private void initServerConfigs() {
        logger.info("Initializing SFTP server configurations");
        
        // Initialize SftpUtil here after all properties have been injected
        logger.info("Initializing SftpUtil with Key Vault URL: {}", sftpKeyVaultUrl);
        this.sftpUtil = new SftpUtil(azureKeyVaultUtil, sftpKeyVaultUrl);
        
        List<ServerConfig> serverConfigs = createServerConfigurations();
        int configuredCount = 0;
        
        // Configure each server
        for (ServerConfig config : serverConfigs) {
            if (isValidServerConfig(config)) {
                logger.info("Adding {} SFTP server configuration: {}@{}:{} (secret: {})", 
                        config.name.toUpperCase(), config.username, config.host, config.port, config.secretName);
                sftpUtil.addServerConfig(config.name, config.host, config.port, config.username, config.secretName);
                configuredCount++;
            } else {
                logger.warn("{} SFTP server not configured - missing required properties (host: '{}', username: '{}', secretName: '{}')", 
                        config.name.toUpperCase(), config.host, config.username, config.secretName);
            }
        }
        
        // Log the number of configured servers
        logger.info("Successfully initialized {} SFTP server configurations", configuredCount);
        
        if (configuredCount == 0) {
            logger.error("No SFTP servers were configured! Check your application properties.");
        }
        
        // Test Azure Key Vault connectivity
        testAzureKeyVaultConnectivity();
    }

    /**
     * Create list of server configurations from injected properties
     */
    private List<ServerConfig> createServerConfigurations() {
        List<ServerConfig> configs = new ArrayList<>();
        
        configs.add(new ServerConfig("azureblob", azureblobHost, azureblobPort, azureblobUsername, azureblobSecretName));
        configs.add(new ServerConfig("ces", cesHost, cesPort, cesUsername, cesSecretName));
        configs.add(new ServerConfig("mha", mhaHost, mhaPort, mhaUsername, mhaSecretName));
        configs.add(new ServerConfig("toppan", toppanHost, toppanPort, toppanUsername, toppanSecretName));
        configs.add(new ServerConfig("icms", icmsHost, icmsPort, icmsUsername, icmsSecretName));
        configs.add(new ServerConfig("repccs", repCcsHost, repCcsPort, repCcsUsername, repCcsSecretName));
        configs.add(new ServerConfig("lta", ltaHost, ltaPort, ltaUsername, ltaSecretName));
        
        return configs;
    }

    /**
     * Validate that a server configuration has all required properties
     */
    private boolean isValidServerConfig(ServerConfig config) {
        return config.host != null && !config.host.trim().isEmpty() &&
               config.username != null && !config.username.trim().isEmpty() &&
               config.secretName != null && !config.secretName.trim().isEmpty();
    }

    /**
     * Test Azure Key Vault connectivity with proper exception handling
     */
    private void testAzureKeyVaultConnectivity() {
        try {
            logger.info("Testing Azure Key Vault connectivity...");
            boolean kvConnected = azureKeyVaultUtil.testConnection();
            logger.info("Azure Key Vault connectivity test: {}", kvConnected ? "SUCCESS" : "FAILED");
            
            if (!kvConnected) {
                logger.error("Azure Key Vault is not accessible. SFTP operations may fail due to inability to retrieve secrets.");
            }
        } catch (Exception e) {
            logger.error("Azure Key Vault connectivity test failed with exception: {}", e.getMessage(), e);
            logger.error("SFTP operations may fail due to Key Vault connectivity issues");
        }
    }

    @Bean
    public SftpUtil sftpUtil() {
        return this.sftpUtil;
    }

    /**
     * Internal class to hold server configuration data
     */
    private static class ServerConfig {
        final String name;
        final String host;
        final int port;
        final String username;
        final String secretName;

        ServerConfig(String name, String host, int port, String username, String secretName) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.username = username;
            this.secretName = secretName;
        }
    }
}
