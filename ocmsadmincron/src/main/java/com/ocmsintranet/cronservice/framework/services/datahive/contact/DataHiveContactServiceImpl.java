package com.ocmsintranet.cronservice.framework.services.datahive.contact;

import com.fasterxml.jackson.databind.JsonNode;
import com.ocmsintranet.cronservice.framework.services.datahive.DataHiveUtil;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers.ContactIdTypeClassifier;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers.ContactQueryBuilder;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers.ContactResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of DataHiveContactService
 * Handles contact lookups from DataHive with retry logic and audit logging
 * 
 * Main flow:
 * Step 1: Validate input parameters (ownerId, ownerIdType, offenceNoticeNumber)
 * Step 2: Classify ID type using ContactIdTypeClassifier (NRIC/FIN vs UEN)
 * Step 3: Build appropriate SQL query using ContactQueryBuilder
 * Step 4: Execute query using existing DataHiveUtil.executeQueryAsyncDataOnly() with retry
 * Step 5: Parse JsonNode response using ContactResponseParser
 * Step 6: Log audit trail with results
 * Step 7: Return ContactLookupResult
 */
@Slf4j
@Service
public class DataHiveContactServiceImpl implements DataHiveContactService {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second
    
    private final DataHiveUtil dataHiveUtil;
    private final ContactIdTypeClassifier idTypeClassifier;
    private final ContactQueryBuilder queryBuilder;
    private final ContactResponseParser responseParser;
    
    public DataHiveContactServiceImpl(
            DataHiveUtil dataHiveUtil,
            ContactIdTypeClassifier idTypeClassifier,
            ContactQueryBuilder queryBuilder,
            ContactResponseParser responseParser) {
        this.dataHiveUtil = dataHiveUtil;
        this.idTypeClassifier = idTypeClassifier;
        this.queryBuilder = queryBuilder;
        this.responseParser = responseParser;
    }
    
    @Override
    public ContactLookupResult lookupContact(String ownerId, String ownerIdType, String offenceNoticeNumber) {
        log.info("Starting DataHive contact lookup for notice {} (Type: {})",
                offenceNoticeNumber, ownerIdType);

        // Step 1: Validate input parameters (PREPARATION STAGE)
        if (!validateInputs(ownerId, ownerIdType, offenceNoticeNumber)) {
            log.error("Invalid input parameters provided for contact lookup");
            return createPreparationErrorResult("Invalid input parameters: ownerId, ownerIdType, or offenceNoticeNumber is null/empty");
        }

        // Step 2: Classify ID type (PREPARATION STAGE)
        ContactIdTypeClassifier.IdTypeCategory idCategory;
        try {
            idCategory = idTypeClassifier.classifyIdType(ownerIdType);

            if (idCategory == ContactIdTypeClassifier.IdTypeCategory.UNKNOWN) {
                log.warn("Unknown ID type {} for notice {}, returning preparation error",
                        ownerIdType, offenceNoticeNumber);
                return createPreparationErrorResult("Unknown ID type: " + ownerIdType);
            }
        } catch (Exception e) {
            log.error("Error classifying ID type {} for notice {}: {}",
                    ownerIdType, offenceNoticeNumber, e.getMessage());
            return createPreparationErrorResult("ID type classification failed: " + e.getMessage());
        }

        // Step 3: Build SQL query (PREPARATION STAGE)
        String sql;
        try {
            sql = buildQuery(idCategory, ownerId);
            log.info("Built DataHive query for ID category {}", idCategory);
        } catch (Exception e) {
            log.error("Error building query for notice {}: {}", offenceNoticeNumber, e.getMessage());
            return createPreparationErrorResult("Query build failed: " + e.getMessage());
        }

        // Step 4: Execute query with retry (QUERY EXECUTION STAGE)
        JsonNode resultData;
        try {
            resultData = executeQueryWithRetry(sql, offenceNoticeNumber);

            if (resultData == null) {
                log.warn("No data returned from DataHive for notice {}", offenceNoticeNumber);
                return createQueryErrorResult("DataHive query failed - all retry attempts exhausted");
            }
        } catch (Exception e) {
            log.error("Error executing DataHive query for notice {}: {}",
                    offenceNoticeNumber, e.getMessage(), e);
            return createQueryErrorResult("DataHive query execution error: " + e.getMessage());
        }

        // Step 5: Parse response (SUCCESS PATH)
        try {
            log.info("Parsing DataHive response for ID category {}", idCategory);
            ContactLookupResult result = parseResponse(idCategory, resultData, ownerId);
            log.info("Parsed result - Mobile found: {}, Email found: {}, Table: {}",
                    result.isMobileFound(), result.isEmailFound(), result.getQueryTable());

            // Step 6: Audit log results
            logAuditTrail(offenceNoticeNumber, ownerId, ownerIdType, result);

            // Step 7: Return result
            return result;

        } catch (Exception e) {
            log.error("Error parsing DataHive response for notice {}: {}",
                    offenceNoticeNumber, e.getMessage(), e);
            return createQueryErrorResult("Response parsing failed: " + e.getMessage());
        }
    }
    
    /**
     * Validate input parameters
     */
    private boolean validateInputs(String ownerId, String ownerIdType, String offenceNoticeNumber) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            log.error("Owner ID is null or empty");
            return false;
        }
        
        if (ownerIdType == null || ownerIdType.trim().isEmpty()) {
            log.error("Owner ID type is null or empty");
            return false;
        }
        
        if (offenceNoticeNumber == null || offenceNoticeNumber.trim().isEmpty()) {
            log.error("Offence notice number is null or empty");
            return false;
        }
        
        return true;
    }
    
    /**
     * Build SQL query based on ID type category
     */
    private String buildQuery(ContactIdTypeClassifier.IdTypeCategory idCategory, String ownerId) {
        switch (idCategory) {
            case SINGPASS:
                return queryBuilder.buildSingpassQuery(ownerId);
            case CORPPASS:
                return queryBuilder.buildCorppassQuery(ownerId);
            default:
                throw new IllegalStateException("Unexpected ID category: " + idCategory);
        }
    }
    
    /**
     * Execute DataHive query with retry logic
     */
    private JsonNode executeQueryWithRetry(String sql, String offenceNoticeNumber) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("Executing DataHive query (attempt {}/{})", attempt, MAX_RETRY_ATTEMPTS);
                
                CompletableFuture<JsonNode> future = dataHiveUtil.executeQueryAsyncDataOnly(sql);
                JsonNode result = future.get(30, TimeUnit.SECONDS);
                
                log.info("DataHive query successful on attempt {}. Response size: {} records", 
                        attempt, result != null && result.isArray() ? result.size() : 0);
                
                if (result != null && result.size() > 0) {
                    log.debug("DataHive raw response: {}", result.toString());
                } else {
                    log.warn("DataHive returned empty result set");
                }
                
                return result;
                
            } catch (Exception e) {
                log.warn("DataHive query failed on attempt {}/{} for notice {}: {}", 
                        attempt, MAX_RETRY_ATTEMPTS, offenceNoticeNumber, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        break;
                    }
                }
            }
        }
        
        log.error("All retry attempts exhausted for notice {}", offenceNoticeNumber);
        return null;
    }
    
    /**
     * Parse response based on ID type category
     */
    private ContactLookupResult parseResponse(ContactIdTypeClassifier.IdTypeCategory idCategory, 
                                            JsonNode resultData, String ownerId) {
        switch (idCategory) {
            case SINGPASS:
                return responseParser.parseSingpassResponse(resultData, ownerId);
            case CORPPASS:
                return responseParser.parseCorppassResponse(resultData, ownerId);
            default:
                throw new IllegalStateException("Unexpected ID category: " + idCategory);
        }
    }
    
    /**
     * Log audit trail for the contact lookup
     */
    private void logAuditTrail(String offenceNoticeNumber, String ownerId, 
                               String ownerIdType, ContactLookupResult result) {
        log.info("AUDIT: DataHive contact lookup completed - Notice: {}, ID: {}, Type: {}, " +
                "Table: {}, Mobile: {}, Email: {}, HasContact: {}",
                offenceNoticeNumber, ownerId, ownerIdType, result.getQueryTable(),
                result.isMobileFound(), result.isEmailFound(), result.hasContact());
    }
    
    /**
     * Create empty result for error cases
     */
    private ContactLookupResult createEmptyResult() {
        return ContactLookupResult.builder()
                .mobileFound(false)
                .emailFound(false)
                .queryTable("NONE")
                .build();
    }

    /**
     * Create result for preparation errors (validation, ID type classification, query building)
     * These errors occur BEFORE DataHive query execution
     */
    private ContactLookupResult createPreparationErrorResult(String errorMessage) {
        log.error("Preparation error: {}", errorMessage);
        return ContactLookupResult.builder()
                .mobileFound(false)
                .emailFound(false)
                .queryTable("NONE")
                .preparationError(true)
                .queryError(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create result for query execution errors (connection timeout, query failures, parsing errors)
     * These errors occur DURING or AFTER DataHive query execution
     */
    private ContactLookupResult createQueryErrorResult(String errorMessage) {
        log.error("Query error: {}", errorMessage);
        return ContactLookupResult.builder()
                .mobileFound(false)
                .emailFound(false)
                .queryTable("NONE")
                .preparationError(false)
                .queryError(true)
                .errorMessage(errorMessage)
                .build();
    }
}