package com.ocmseservice.apiservice.utilities;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import com.azure.security.keyvault.keys.models.JsonWebKey;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@Lazy // Add lazy initialization to prevent startup initialization
public class AzureKeyVaultUtil {
    private static final Logger logger = LoggerFactory.getLogger(AzureKeyVaultUtil.class);
    
    private final String keyVaultUrl;
    private DefaultAzureCredential credential;
    private KeyClient keyClient;
    private SecretClient secretClient;
    
    public AzureKeyVaultUtil(@Value("${ocms.azure.keyvault.url}") String keyVaultUrl) {
        this.keyVaultUrl = keyVaultUrl;
        logger.info("AzureKeyVaultUtil created with vault URL: {}, but clients not yet initialized", keyVaultUrl);
    }
    
    // Lazy initialization methods
    private synchronized DefaultAzureCredential getCredential() {
        if (credential == null) {
            logger.info("Initializing Azure credential");
            
            // Check if running in Azure (App Service sets this environment variable)
            if (System.getenv("WEBSITE_SITE_NAME") != null) {
                logger.info("Running in Azure App Service, configuring managed identity with longer timeout");
                
                // Create an HTTP client with longer timeouts
                HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                        .connectTimeout(Duration.ofSeconds(60))
                        .responseTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(60))
                        .build();
                
                // We don't need to explicitly create a ManagedIdentityCredential
                // as DefaultAzureCredential will use it automatically in App Service
                
                // Build DefaultAzureCredential with the custom HTTP client
                credential = new DefaultAzureCredentialBuilder()
                        .httpClient(httpClient)
                        .build();
                
                logger.info("Azure managed identity credential initialized with extended timeout");
            } else {
                logger.info("Running locally, using default credential chain");
                credential = new DefaultAzureCredentialBuilder().build();
            }
            
            logger.info("Azure credential initialized successfully");
        }
        return credential;
    }
    
    private synchronized KeyClient getKeyClient() {
        if (keyClient == null) {
            logger.info("Initializing Key Vault Key Client");
            keyClient = new KeyClientBuilder()
                    .vaultUrl(keyVaultUrl)
                    .credential(getCredential())
                    .buildClient();
        }
        return keyClient;
    }
    
    private synchronized SecretClient getSecretClient() {
        if (secretClient == null) {
            logger.info("Initializing Key Vault Secret Client");
            secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUrl)
                    .credential(getCredential())
                    .buildClient();
        }
        return secretClient;
    }
    
    /**
     * Validates the APIM subscription key from the request header
     * 
     * @param request The HTTP request
     * @param headerName The name of the header containing the subscription key
     * @return true if the key is valid, false otherwise
     */
    public boolean validateSubscriptionKey(HttpServletRequest request, String headerName) {
        try {
            logger.info("Validating APIM subscription key using header: {}", headerName);
            
            // Extract subscription key from request header
            String subscriptionKey = request.getHeader(headerName);
            
            if (subscriptionKey == null || subscriptionKey.isEmpty()) {
                logger.error("Missing APIM subscription key in header");
                return false;
            }
            
            logger.debug("Retrieved header subscription key: [MASKED]");
            logger.debug("Getting expected key from Azure Key Vault using secret name: {}", headerName);
            
            // Get the expected key from Azure Key Vault using lazy-initialized client
            String expectedKey = getSecret(headerName);
            logger.debug("Retrieved APIM secret value from Key Vault: [MASKED]");
            
            if (!subscriptionKey.equals(expectedKey)) {
                logger.error("Invalid APIM subscription key provided");
                return false;
            }
            
            logger.info("APIM subscription key validated successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error validating APIM subscription key", e);
            return false;
        }
    }
    
    public CryptographyClient getCryptographyClient(String keyName) {
        String keyIdentifier = keyVaultUrl + "/keys/" + keyName;
        logger.info("Creating CryptographyClient for key: {}", keyIdentifier);
        
        return new CryptographyClientBuilder()
                .keyIdentifier(keyIdentifier)
                .credential(getCredential()) // Use the lazy-initialized credential
                .buildClient();
    }
    
    public RSAPublicKey retrievePublicKey(String keyName) throws Exception {
        try {
            logger.info("Retrieving public key components for: {} from Key Vault: {}", keyName, keyVaultUrl);
            
            // Get the key from Key Vault using lazy-initialized client
            KeyVaultKey keyVaultKey = getKeyClient().getKey(keyName);
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
    
    public byte[] signData(String keyName, byte[] data) {
        try {
            logger.info("Signing data with key: {}", keyName);
            
            // Get a cryptography client for the key
            CryptographyClient cryptographyClient = getCryptographyClient(keyName);
            
            // Hash the data with SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            // Sign the hash with the key in Key Vault
            byte[] signature = cryptographyClient.sign(SignatureAlgorithm.RS256, hash).getSignature();
            
            logger.info("Data signed successfully with key: {}", keyName);
            return signature;
        } catch (Exception e) {
            logger.error("Failed to sign data with key: {}", keyName, e);
            throw new RuntimeException("Failed to sign data with Key Vault", e);
        }
    }
    
    public String getSecret(String secretName) {
        try {
            logger.info("Retrieving secret: {} from Key Vault: {}", secretName, keyVaultUrl);
            
            KeyVaultSecret secret = getSecretClient().getSecret(secretName);
            
            if (secret != null && secret.getValue() != null) {
                logger.info("Successfully retrieved secret: {}", secretName);
                return secret.getValue();
            } else {
                logger.error("Secret not found or has null value: {}", secretName);
                throw new IllegalArgumentException("Secret not found or has null value: " + secretName);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve secret: {}", secretName, e);
            throw new RuntimeException("Failed to retrieve secret from Key Vault: " + e.getMessage(), e);
        }
    }
    
    public static String generateJWT(String account, String user, AzureKeyVaultUtil keyVaultUtil, String keyName) throws Exception {
        logger.info("Generating JWT for account: {} and user: {}", account, user);
        
        try {
            // Get the public key from Azure Key Vault
            RSAPublicKey publicKey = keyVaultUtil.retrievePublicKey(keyName);
            
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
            
            // Sign using Azure Key Vault
            byte[] signature = keyVaultUtil.signData(keyName, message.getBytes(StandardCharsets.UTF_8));
            
            // Encode signature and create final JWT
            String base64Signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            
            logger.info("JWT token generated successfully");
            return message + "." + base64Signature;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token", e);
            throw new Exception("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }
}