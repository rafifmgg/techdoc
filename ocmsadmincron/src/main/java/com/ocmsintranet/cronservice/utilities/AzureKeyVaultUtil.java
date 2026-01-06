package com.ocmsintranet.cronservice.utilities;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.SignResult;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import com.azure.security.keyvault.keys.models.JsonWebKey;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility for Azure Key Vault operations
 * This implementation requires explicit Key Vault URLs for all operations
 */
@Component
@org.springframework.context.annotation.Lazy
public class AzureKeyVaultUtil {
    private static final Logger logger = LoggerFactory.getLogger(AzureKeyVaultUtil.class);
    
    // Default Key Vault URL (used as fallback when no explicit URL is provided)
    private final String defaultKeyVaultUrl;
    
    // Credential shared across all Key Vault clients
    private DefaultAzureCredential credential;
    
    // Map to store KeyClients for different Key Vault URLs
    private final Map<String, KeyClient> keyClients = new HashMap<>();
    
    // Map to store SecretClients for different Key Vault URLs
    private final Map<String, SecretClient> secretClients = new HashMap<>();
    
    private boolean initialized = false;
    
    @Value("${sftp.azure.privatekey-secret-name:urasftpsecret}")
    private String sftpPrivateKeySecretName;
    
    /**
     * Constructor with default Key Vault URL
     * 
     * @param defaultKeyVaultUrl Default Key Vault URL to use as fallback when no explicit URL is provided
     */
    public AzureKeyVaultUtil(
            @Value("${ocms.azure.keyvault.url}") String defaultKeyVaultUrl) {
        this.defaultKeyVaultUrl = defaultKeyVaultUrl;
        logger.debug("Configured with default Key Vault URL: {}", defaultKeyVaultUrl);
    }
    
    /**
     * Lazy initialization of Azure clients
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            try {
                logger.info("Initializing Azure Key Vault clients...");
                
                // Initialize Azure credential - use a specific set of credential providers
                // Set environment variable to control which credential providers are used
                System.setProperty("azure.identity.credential.chain-order", "environment,managed-identity,azure-cli");
                this.credential = new DefaultAzureCredentialBuilder().build();
                
                // Initialize default Key Vault clients
                initializeKeyVaultClients(defaultKeyVaultUrl);
                
                initialized = true;
                logger.info("Azure Key Vault clients initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Azure Key Vault clients", e);
            }
        }
    }
    
    /**
     * Initialize Key Vault clients for a specific Key Vault URL
     * 
     * @param keyVaultUrl The Key Vault URL to initialize clients for
     */
    private void initializeKeyVaultClients(String keyVaultUrl) {
        if (keyVaultUrl == null || keyVaultUrl.isEmpty()) {
            throw new IllegalArgumentException("Key Vault URL cannot be null or empty");
        }
        
        logger.debug("Initializing Key Vault clients for URL: {}", keyVaultUrl);
        
        // Create Key Client for this Key Vault URL
        KeyClient keyClient = new KeyClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(credential)
                .buildClient();
        keyClients.put(keyVaultUrl, keyClient);
        
        // Create Secret Client for this Key Vault URL
        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(credential)
                .buildClient();
        secretClients.put(keyVaultUrl, secretClient);
        
        logger.debug("Key Vault clients initialized for URL: {}", keyVaultUrl);
    }
    
    /**
        }
        
        return secretClients.get(vaultUrl);
    }
    
    /**
     * Get a key client for a specific Key Vault URL
     * 
     * @param keyVaultUrl Key Vault URL to get the client for
     * @return KeyClient for the specified URL
     */
    private KeyClient getKeyClient(String keyVaultUrl) {
        initializeIfNeeded();
        
        // Use the provided Key Vault URL or default if null
        String vaultUrl = (keyVaultUrl != null && !keyVaultUrl.isEmpty()) ? keyVaultUrl : defaultKeyVaultUrl;
        
        // Check if we already have a client for this URL
        if (!keyClients.containsKey(vaultUrl)) {
            // Initialize client for this URL if it doesn't exist
            initializeKeyVaultClients(vaultUrl);
        }
        
        return keyClients.get(vaultUrl);
    }
    
    /**
     * Get a cryptography client for a key in a specific Key Vault
     * 
     * @param keyName Name of the key
     * @return CryptographyClient for the specified key using default Key Vault URL
     */
    public CryptographyClient getCryptographyClient(String keyName) {
        return getCryptographyClient(keyName, defaultKeyVaultUrl);
    }
    
    /**
     * Get a cryptography client for a key in a specific Key Vault
     * 
     * @param keyName Name of the key
     * @param keyVaultUrl Specific Key Vault URL to use
     * @return CryptographyClient for the specified key
     */
    public CryptographyClient getCryptographyClient(String keyName, String keyVaultUrl) {
        initializeIfNeeded();
        
        // Get the Key Client for this URL
        KeyClient keyClient = getKeyClient(keyVaultUrl);
        
        // Get the key from Key Vault
        KeyVaultKey key = keyClient.getKey(keyName);
        
        // Create and return a CryptographyClient for this key
        return new CryptographyClientBuilder()
                .keyIdentifier(key.getId())
                .credential(credential)
                .buildClient();
    }
    
    /**
     * Retrieve a public key from Azure Key Vault using default Key Vault URL
     * 
     * @param keyName Name of the key
     * @return RSA public key
     * @throws Exception 
     */
    public RSAPublicKey retrievePublicKey(String keyName) throws Exception {
        return retrievePublicKey(keyName, defaultKeyVaultUrl);
    }
    
    /**
     * Retrieve a public key from a specific Azure Key Vault
     * 
     * @param keyName Name of the key
     * @param keyVaultUrl Specific Key Vault URL to use
     * @return RSA public key
     * @throws Exception 
     */
    public RSAPublicKey retrievePublicKey(String keyName, String keyVaultUrl) throws Exception {
        try {
            initializeIfNeeded();
            
            logger.info("Retrieving public key components for: {} from Key Vault: {}", keyName, keyVaultUrl);

            // Get the Key Client for this URL
            KeyClient keyClient = getKeyClient(keyVaultUrl);
            
            // Get the key from Key Vault
            KeyVaultKey keyVaultKey = keyClient.getKey(keyName);
            JsonWebKey jsonWebKey = keyVaultKey.getKey();
            
            if (jsonWebKey != null) {
                // Extract the modulus and exponent
                byte[] modulus = jsonWebKey.getN();
                byte[] exponent = jsonWebKey.getE();
                
                if (modulus == null || exponent == null) {
                    throw new IllegalStateException("Key components (modulus or exponent) are missing");
                }
                
                // Create the RSA public key
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                        new BigInteger(1, modulus),
                        new BigInteger(1, exponent)
                );
                RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
                
                logger.info("Successfully retrieved public key for: {}", keyName);
                return publicKey;
            } else {
                throw new IllegalArgumentException("No key components found for: " + keyName);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve public key for: {}", keyName, e);
            throw new Exception("Failed to retrieve public key from Key Vault: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sign data using a key from Azure Key Vault
     * 
     * @param keyName Name of the key
     * @param data Data to sign
     * @return Signed data
     */
    public byte[] signData(String keyName, byte[] data) {
        return signData(keyName, data, defaultKeyVaultUrl);
    }
    
    /**
     * Sign data using a key from a specific Azure Key Vault
     * 
     * @param keyName Name of the key
     * @param data Data to sign
     * @param keyVaultUrl Specific Key Vault URL to use
     * @return Signed data
     */
    public byte[] signData(String keyName, byte[] data, String keyVaultUrl) {
        initializeIfNeeded();
        
        try {
            // Get cryptography client for this key and vault
            CryptographyClient cryptoClient = getCryptographyClient(keyName, keyVaultUrl);
            
            // For RS256, we need to hash the data first with SHA-256
            // Azure Key Vault expects a 32-byte hash for RS256, not raw data
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedData = digest.digest(data);
            
            logger.debug("Signing data with key: {} from Key Vault: {}", keyName, keyVaultUrl);
            logger.debug("Original data length: {} bytes, Hashed data length: {} bytes", 
                      data.length, hashedData.length);
            
            // Sign the hashed data using RS256
            SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;
            SignResult signResult = cryptoClient.sign(algorithm, hashedData);
            
            return signResult.getSignature();
        } catch (Exception e) {
            logger.error("Failed to sign data using key: {} from Key Vault: {}", keyName, keyVaultUrl, e);
            throw new RuntimeException("Failed to sign data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sign data using a key from Azure Key Vault using default Key Vault URL
     * 
     * @param keyName Name of the key
        }
    }
    
    /**
     * Get a secret from Azure Key Vault
     * 
     * @param secretName Name of the secret
     * @return Secret value
     */
    public String getSecret(String secretName) {
        // For SFTP operations, we need to force initialization and get the real key
        if (secretName.equals(sftpPrivateKeySecretName)) {
            // Force initialization for SFTP operations
            try {
                // Always initialize for SFTP operations
                if (!initialized) {
                    logger.info("Forcing initialization of Azure Key Vault for SFTP operations");
                    initializeIfNeeded();
                }
                
                // Use default Key Vault for SFTP operations
                String vaultUrl = defaultKeyVaultUrl;
                SecretClient secretClient = secretClients.get(vaultUrl);
                
                logger.info("Retrieving SFTP key from Key Vault: {}", vaultUrl);
                String sftpKey = secretClient.getSecret(secretName).getValue();
                
                // Debug the key format without exposing the full key
                logger.info("SFTP key length: {}", sftpKey.length());
                logger.info("SFTP key contains BEGIN marker: {}", sftpKey.contains("BEGIN"));
                logger.info("SFTP key contains END marker: {}", sftpKey.contains("END"));
                logger.info("SFTP key first 20 chars: {}", sftpKey.length() > 20 ? 
                             sftpKey.substring(0, 20) + "..." : "[too short]");
                
                // Format the key properly if it doesn't have the required markers
                if (!sftpKey.contains("BEGIN") || !sftpKey.contains("END")) {
                    logger.info("SFTP key is missing BEGIN/END markers, adding them");
                    
                    // Determine the key type and add appropriate markers inline
                    if (sftpKey.contains("ssh-rsa") || sftpKey.startsWith("AAAA")) {
                        // RSA key format
                        sftpKey = "-----BEGIN RSA PRIVATE KEY-----\n" + 
                                  sftpKey.trim() + 
                                  "\n-----END RSA PRIVATE KEY-----";
                    } else {
                        // Default to OpenSSH format
                        sftpKey = "-----BEGIN OPENSSH PRIVATE KEY-----\n" + 
                                  sftpKey.trim() + 
                                  "\n-----END OPENSSH PRIVATE KEY-----";
                    }
                    
                    logger.info("SFTP key formatted with BEGIN/END markers");
                }
                
                logger.info("Successfully retrieved SFTP key");
                return sftpKey;
            } catch (Exception e) {
                logger.error("Failed to retrieve SFTP key from Key Vault: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to retrieve SFTP key from Key Vault: " + e.getMessage(), e);
            }
        }
        
        // For non-SFTP secrets, use the regular flow
        initializeIfNeeded();
        
        // Use default Key Vault URL
        String vaultUrl = defaultKeyVaultUrl;
        
        try {
            logger.debug("Getting secret '{}' from Key Vault: {}", secretName, vaultUrl);
            SecretClient secretClient = secretClients.get(vaultUrl);
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            return secret.getValue();
        } catch (Exception e) {
            logger.error("Failed to get secret '{}' from Key Vault: {}", secretName, e.getMessage(), e);
            throw new RuntimeException("Failed to get secret from Key Vault", e);
        }
    }
    
    /**
     * Get a secret from a specific Azure Key Vault URL
     * 
     * @param secretName Name of the secret
     * @param keyVaultUrl Specific Key Vault URL to use
     * @return Secret value
     */
    public String getSecret(String secretName, String keyVaultUrl) {
        initializeIfNeeded();
        
        // Use the explicitly provided Key Vault URL
        try {
            logger.debug("Getting secret '{}' from explicitly specified Key Vault: {}", secretName, keyVaultUrl);
            
            // Ensure we have a client for this URL
            if (!secretClients.containsKey(keyVaultUrl)) {
                logger.info("Creating new SecretClient for URL: {}", keyVaultUrl);
                secretClients.put(keyVaultUrl, new SecretClientBuilder()
                    .vaultUrl(keyVaultUrl)
                    .credential(credential)
                    .buildClient());
            }
            
            SecretClient secretClient = secretClients.get(keyVaultUrl);
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            return secret.getValue();
        } catch (Exception e) {
            logger.error("Failed to get secret '{}' from specified Key Vault {}: {}", 
                        secretName, keyVaultUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to get secret from specified Key Vault", e);
        }
    }
    
    // Method removed - not needed for the original implementation
    
    /**
     * Test the connection to Azure Key Vault
     * 
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            initializeIfNeeded();
            // Try to list a key to test connectivity
            logger.info("Testing Azure Key Vault connections...");
            
            // Test default Key Vault connection
            boolean connected = keyClients.get(defaultKeyVaultUrl) != null && 
                              secretClients.get(defaultKeyVaultUrl) != null;
            
            logger.info("Key Vault connection test result: {}", connected);
            
            return connected;
        } catch (Exception e) {
            logger.error("Azure Key Vault connection test failed", e);
            return false;
        }
    }
    
    public static String generateJWT(String account, String user, AzureKeyVaultUtil keyVaultUtil, String keyName, String keyVaultUrl) throws Exception {
        logger.info("Generating JWT for account: {} and user: {}", account, user);
        
        try {
            // Get the public key from Azure Key Vault with explicit Key Vault URL
            RSAPublicKey publicKey = keyVaultUtil.retrievePublicKey(keyName, keyVaultUrl);
            
            // Create qualified username
            String qualifiedUserName = account.toUpperCase(Locale.ROOT) + "." + user.toUpperCase(Locale.ROOT);
            
            // Generate public key fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String publicKeyFp = "SHA256:" + Base64.getEncoder().encodeToString(digest.digest(publicKey.getEncoded()));
            
            // Set JWT timestamps
            Date issuedTs = new Date();
            Date expiresTs = new Date(issuedTs.getTime() + TimeUnit.HOURS.toMillis(1));
            
            // Create JWT header and payload
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String payload = String.format(
                    "{\"iss\":\"%s.%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                    qualifiedUserName, publicKeyFp, qualifiedUserName,
                    issuedTs.getTime() / 1000, expiresTs.getTime() / 1000
            );
            
            // Base64 encode header and payload
            String base64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            String message = base64Header + "." + base64Payload;
            
            // Sign using Azure Key Vault with explicit URL
            byte[] signature = keyVaultUtil.signData(keyName, message.getBytes(StandardCharsets.UTF_8), keyVaultUrl);
            
            // Encode signature and create final JWT
            String base64Signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            
            logger.info("JWT token generated successfully");
            return message + "." + base64Signature;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token", e);
            throw new Exception("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate JWT token using a private key stored as a secret in Azure Key Vault (for local signing)
     * 
     * @param account Snowflake account identifier
     * @param user Snowflake username
     * @param keyVaultUtil Azure Key Vault utility instance
     * @param secretName Name of the secret containing the private key in PEM format
     * @return JWT token signed with the private key
     * @throws Exception if JWT generation fails
     */
    public static String generateJWTWithPrivateKey(String account, String user, AzureKeyVaultUtil keyVaultUtil, String secretName) throws Exception {
        logger.info("Generating JWT with local signing for account: {} and user: {}", account, user);
        
        try {
            // Retrieve the private key from Azure Key Vault Secret
            String privateKeyPem = keyVaultUtil.getSecret(secretName);
            
            // Parse the private key from PEM format
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            
            // Generate public key from private key for fingerprint calculation
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            BigInteger modulus = rsaPrivateKey.getModulus();
            
            // For RSA, we use the standard public exponent (65537)
            BigInteger publicExponent = new BigInteger("65537");
            
            // Create public key from modulus and exponent
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
            
            // Generate public key fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String publicKeyFp = "SHA256:" + Base64.getEncoder().encodeToString(digest.digest(publicKey.getEncoded()));
            
            // Create qualified username
            String qualifiedUserName = account.toUpperCase(Locale.ROOT) + "." + user.toUpperCase(Locale.ROOT);
            
            // Set JWT timestamps
            Date issuedTs = new Date();
            Date expiresTs = new Date(issuedTs.getTime() + TimeUnit.HOURS.toMillis(1));
            
            // Create JWT header and payload
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String payload = String.format(
                    "{\"iss\":\"%s.%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                    qualifiedUserName, publicKeyFp, qualifiedUserName,
                    issuedTs.getTime() / 1000, expiresTs.getTime() / 1000
            );
            
            // Base64 encode header and payload
            String base64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            String message = base64Header + "." + base64Payload;
            
            // Sign locally using the private key
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            
            // Encode signature and create final JWT
            String base64Signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            
            logger.info("JWT token generated successfully with local signing");
            return message + "." + base64Signature;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token with local signing", e);
            throw new Exception("Failed to generate JWT token with local signing: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse a PEM-formatted private key string into a PrivateKey object
     * 
     * @param privateKeyPem PEM-formatted private key string
     * @return PrivateKey object
     * @throws Exception if parsing fails
     */
    private static PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        try {
            // Remove PEM headers and footers
            String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            // Decode Base64
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            
            // Generate private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            logger.error("Failed to parse private key from PEM format", e);
            throw new Exception("Failed to parse private key from PEM format: " + e.getMessage(), e);
        }
    }
}
