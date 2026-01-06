package com.ocmsintranet.cronservice.framework.services.tablequery;

import com.ocmsintranet.cronservice.crud.BaseService;
import com.ocmsintranet.cronservice.crud.beans.FindAllResponse;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.helper.TableQueryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of TableQueryService with dynamic entity resolution
 * Provides simplified table querying capabilities for workflows
 * Uses dynamic discovery to find entity classes and their services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableQueryServiceImpl implements TableQueryService {

    private final DynamicEntityResolver entityResolver;
    
    // Registry of predefined queries
    private final Map<String, PredefinedQuery> predefinedQueries = new HashMap<>();
    
    /**
     * Initialize predefined queries
     */
    @PostConstruct
    public void init() {
        // LTA Enquiry query
        registerPredefinedQuery("ltaEnquiryQuery", (tableName, params) -> {
            Map<String, Object> filters = new HashMap<>();
            filters.put("nextProcessingStage", SystemConstant.SuspensionReason.ROV);
            
            LocalDateTime currentDate = (LocalDateTime) params.getOrDefault("currentDate", LocalDateTime.now());
            filters.put("nextProcessingDate.lte", currentDate);
            filters.put("offenceNoticeType.in", Arrays.asList("O", "E"));
            
            return filters;
        });

        // MHA NRIC query for RD1 stage
        registerPredefinedQuery("mhaNricQuery", (tableName, params) -> {
            Map<String, Object> filters = new HashMap<>();
            filters.put("nextProcessingStage", SystemConstant.SuspensionReason.RD1);
            
            LocalDateTime currentDate = (LocalDateTime) params.getOrDefault("currentDate", LocalDateTime.now());
            filters.put("nextProcessingDate.lte", currentDate);
            filters.put("suspensionType.null", true);
            
            return filters;
        });
    }
    
    @Override
    public List<Map<String, Object>> query(String tableName, Map<String, Object> filters) {
        log.debug("Querying table: {} with filters: {}", tableName, filters.keySet());
        
        try {
            // Get entity class for table name using dynamic resolver
            Class<?> entityClass = entityResolver.getEntityClassForTable(tableName);
            
            // Get appropriate service for this entity
            BaseService<?, ? extends Serializable> service = entityResolver.getServiceForEntity(entityClass);
            
            // Convert simple filters to BaseService format
            Map<String, String[]> baseServiceFilters = convertToBaseServiceFilters(filters);
            
            // Use the service's getAll method
            FindAllResponse<?> response = service.getAll(baseServiceFilters);
            return response.getItems().stream()
                .map(entity -> TableQueryHelper.convertEntityToMap(entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error querying table {}: {}", tableName, e.getMessage(), e);
            
            // Provide helpful error message with available tables
            if (e.getMessage().contains("No entity found for table")) {
                Set<String> availableTables = entityResolver.getAvailableTableNames();
                throw new IllegalArgumentException("Table '" + tableName + "' not found. Available tables: " + availableTables, e);
            }
            
            throw new RuntimeException("Failed to query table: " + tableName, e);
        }
    }
    
    @Override
    public List<Map<String, Object>> queryWithPredefined(String tableName, String queryName, Map<String, Object> parameters) {
        log.debug("Querying table: {} with predefined query: {}", tableName, queryName);
        
        try {
            // Validate table exists first
            if (!entityResolver.isTableSupported(tableName)) {
                Set<String> availableTables = entityResolver.getAvailableTableNames();
                throw new IllegalArgumentException("Table '" + tableName + "' not found. Available tables: " + availableTables);
            }
            
            // Get predefined query
            PredefinedQuery predefinedQuery = predefinedQueries.get(queryName);
            if (predefinedQuery == null) {
                throw new IllegalArgumentException("Predefined query not found: " + queryName + 
                    ". Available queries: " + predefinedQueries.keySet());
            }
            
            // Generate filters using predefined query
            Map<String, Object> filters = predefinedQuery.generateFilters(tableName, parameters);
            
            // Execute query with generated filters
            return query(tableName, filters);
        } catch (Exception e) {
            log.error("Error executing predefined query {} for table {}: {}", 
                     queryName, tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to execute predefined query: " + queryName, e);
        }
    }
    
    @Override
    public long count(String tableName, Map<String, Object> filters) {
        log.debug("Counting rows in table: {} with filters: {}", tableName, filters.keySet());
        
        try {
            // Get entity class for table name using dynamic resolver
            Class<?> entityClass = entityResolver.getEntityClassForTable(tableName);
            
            // Get appropriate service for this entity
            BaseService<?, ? extends Serializable> service = entityResolver.getServiceForEntity(entityClass);
            
            // Convert simple filters to BaseService format
            Map<String, String[]> baseServiceFilters = convertToBaseServiceFilters(filters);
            
            // Add limit=0 to just get count
            baseServiceFilters.put("$limit", new String[]{"0"});
            
            // Use the service's getAll method to get count
            FindAllResponse<?> response = service.getAll(baseServiceFilters);
            return response.getTotal();
        } catch (Exception e) {
            log.error("Error counting rows in table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to count rows in table: " + tableName, e);
        }
    }
    
    @Override
    public List<Map<String, Object>> patch(String tableName, Map<String, Object> filters, Map<String, Object> fields) {
        log.debug("Patching entities in table: {} with filters: {} and fields: {}", 
                tableName, filters.keySet(), fields.keySet());
        
        try {
            // Get entity class for table name using dynamic resolver
            Class<?> entityClass = entityResolver.getEntityClassForTable(tableName);
            
            // Get appropriate service for this entity
            BaseService<?, ? extends Serializable> service = entityResolver.getServiceForEntity(entityClass);
            
            // Convert simple filters to BaseService format
            Map<String, String[]> baseServiceFilters = convertToBaseServiceFilters(filters);
            
            // First, query to find all entities that match the filters
            FindAllResponse<?> response = service.getAll(baseServiceFilters);
            List<?> entities = response.getItems();
            
            if (entities.isEmpty()) {
                log.info("No entities found to patch in table: {} with filters: {}", tableName, filters.keySet());
                return Collections.emptyList();
            }
            
            List<Object> updatedEntities = new ArrayList<>();
            
            // For each matching entity, apply the patch
            for (Object entity : entities) {
                try {
                    // Create a partial entity with the fields to update
                    Object partialEntity = TableQueryHelper.createPartialEntity(entityClass, fields);
                    
                    // Use the enhanced helper method that handles composite keys
                    Object updatedEntity = TableQueryHelper.patchEntity(service, entity, partialEntity);
                    updatedEntities.add(updatedEntity);
                } catch (Exception e) {
                    log.error("Error patching entity in table {}: {}", tableName, e.getMessage(), e);
                    // Continue with other entities even if one fails
                }
            }
            
            // Convert updated entities to maps
            return updatedEntities.stream()
                .map(entity -> TableQueryHelper.convertEntityToMap(entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error patching entities in table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to patch entities in table: " + tableName, e);
        }
    }

    @Override
    public Map<String, Object> post(String tableName, Map<String, Object> fields) {
        log.debug("Creating new entity in table: {} with fields: {}", tableName, fields.keySet());
        
        try {
            // Get entity class for table name using dynamic resolver
            Class<?> entityClass = entityResolver.getEntityClassForTable(tableName);
            
            // Get appropriate service for this entity
            BaseService<?, ? extends Serializable> service = entityResolver.getServiceForEntity(entityClass);
            
            // Create a new entity with the provided fields
            Object newEntity = TableQueryHelper.createPartialEntity(entityClass, fields);
            
            // Save the entity using the service's save method
            Object savedEntity = TableQueryHelper.saveEntity(service, newEntity);
            
            // Convert the saved entity to a map and return it
            return TableQueryHelper.convertEntityToMap(savedEntity);
        } catch (Exception e) {
            log.error("Error creating entity in table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to create entity in table: " + tableName, e);
        }
    }
    
    @Override
    public int delete(String tableName, Map<String, Object> filters) {
        log.debug("Deleting entities from table: {} with filters: {}", tableName, filters.keySet());
        
        try {
            // Get entity class for table name using dynamic resolver
            Class<?> entityClass = entityResolver.getEntityClassForTable(tableName);
            
            // Get appropriate service for this entity
            BaseService<?, ? extends Serializable> service = entityResolver.getServiceForEntity(entityClass);
            
            // Convert simple filters to BaseService format
            Map<String, String[]> baseServiceFilters = convertToBaseServiceFilters(filters);
            
            // First, query to find all entities that match the filters
            FindAllResponse<?> response = service.getAll(baseServiceFilters);
            List<?> entities = response.getItems();
            
            if (entities.isEmpty()) {
                log.info("No entities found to delete in table: {} with filters: {}", tableName, filters.keySet());
                return 0;
            }
            
            int deletedCount = 0;
            
            // For each matching entity, delete it
            for (Object entity : entities) {
                try {
                    // Extract the ID from the entity
                    Object idValue = TableQueryHelper.extractEntityId(entity);
                    
                    if (idValue == null) {
                        log.error("Could not extract ID from entity of type: {}", entityClass.getName());
                        continue;
                    }
                    
                    // Delete the entity
                    @SuppressWarnings("unchecked")
                    BaseService<Object, Serializable> typedService = (BaseService<Object, Serializable>) service;
                    boolean deleted = typedService.delete((Serializable) idValue);
                    
                    if (deleted) {
                        deletedCount++;
                        log.debug("Deleted entity with ID: {} from table: {}", idValue, tableName);
                    } else {
                        log.warn("Failed to delete entity with ID: {} from table: {}", idValue, tableName);
                    }
                } catch (Exception e) {
                    log.error("Error deleting entity from table {}: {}", tableName, e.getMessage(), e);
                    // Continue with other entities even if one fails
                }
            }
            
            log.info("Deleted {} entities from table: {}", deletedCount, tableName);
            return deletedCount;
        } catch (Exception e) {
            log.error("Error deleting entities from table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete entities from table: " + tableName, e);
        }
    }
    
    /**
     * Get all available table names - useful for debugging or UI
     */
    public Set<String> getAvailableTableNames() {
        return entityResolver.getAvailableTableNames();
    }
    
    /**
     * Check if a table is supported
     */
    public boolean isTableSupported(String tableName) {
        return entityResolver.isTableSupported(tableName);
    }
    
    /**
     * Refresh entity mappings - useful for testing or when new entities are added
     */
    public void refreshEntityMappings() {
        entityResolver.refreshEntityMappings();
    }
    
    /**
     * Convert simple filters to the format expected by BaseService
     * This is the key method that ensures proper formatting for BaseImplement
     */
    private Map<String, String[]> convertToBaseServiceFilters(Map<String, Object> filters) {
        Map<String, String[]> result = new HashMap<>();
        
        if (filters == null || filters.isEmpty()) {
            return result;
        }
        
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Handle explicit null values
            if (value == null) {
                result.put(key + "[$null]", new String[]{"true"});
                continue;
            }
            
            // Handle bracket notation that's already provided (e.g., "field[$in]")
            if (key.contains("[$") && key.contains("]")) {
                log.debug("Using pre-formatted bracket notation: {}", key);
                result.put(key, convertValueToStringArray(value));
            } else if (key.contains(".")) {
                // Handle dot notation conversion (e.g., "field.in" -> "field[$in]")
                if (key.endsWith(".in")) {
                    // Special handling for .in operator
                    String fieldName = key.replace(".in", "");
                    String[] inValues = convertCollectionToCommaSeparatedArray(value);
                    result.put(fieldName + "[$in]", inValues);
                    log.debug("Converted .in operator: {} -> {}[$in] with values: {}", 
                             key, fieldName, Arrays.toString(inValues));
                } else if (key.endsWith(".null") && Boolean.TRUE.equals(value)) {
                    String fieldName = key.replace(".null", "");
                    result.put(fieldName + "[$null]", new String[]{"true"});
                } else {
                    String convertedKey = convertOperatorKey(key);
                    result.put(convertedKey, convertValueToStringArray(value));
                }
            } else {
                // Simple equality
                result.put(key, convertValueToStringArray(value));
            }
        }
        
        log.debug("Filter conversion summary - Input: {} -> Output: {}", 
                 filters.keySet(), result.keySet());
        return result;
    }
    
    /**
     * Convert collection/array values to the format expected by BaseImplement's $in operator
     * BaseImplement expects: ["O,E"] (single string element with comma-separated values)
     */
    private String[] convertCollectionToCommaSeparatedArray(Object value) {
        if (value == null) {
            return new String[0];
        }
        
        List<String> stringValues = new ArrayList<>();
        
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            for (Object item : collection) {
                if (item != null) {
                    stringValues.add(item.toString());
                }
            }
        } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object item : array) {
                if (item != null) {
                    stringValues.add(item.toString());
                }
            }
        } else {
            String stringValue = value.toString().trim();
            
            // Handle array-like strings: ['O', 'E'] or ["O", "E"] or [O, E]
            if (stringValue.startsWith("[") && stringValue.endsWith("]")) {
                stringValue = stringValue.substring(1, stringValue.length() - 1);
                String[] parts = stringValue.split(",");
                for (String part : parts) {
                    String cleanPart = part.trim().replaceAll("(^['\"])|(['\"]$)", "");
                    if (!cleanPart.isEmpty()) {
                        stringValues.add(cleanPart);
                    }
                }
            } else if (stringValue.contains(",")) {
                // Handle comma-separated values
                String[] parts = stringValue.split(",");
                for (String part : parts) {
                    String cleanPart = part.trim();
                    if (!cleanPart.isEmpty()) {
                        stringValues.add(cleanPart);
                    }
                }
            } else {
                stringValues.add(stringValue);
            }
        }
        
        // KEY FIX: Return single element array with comma-separated values
        // This is what BaseImplement's $in operator expects
        if (stringValues.isEmpty()) {
            return new String[0];
        } else {
            String commaSeparated = String.join(",", stringValues);
            log.debug("Converted $in values to: '{}'", commaSeparated);
            return new String[]{commaSeparated};
        }
    }
    
    /**
     * Convert operator keys to the format expected by BaseService (bracket notation)
     */
    private String convertOperatorKey(String key) {
        // Map of simple operators to BaseService bracket operators
        Map<String, String> operatorMapping = new HashMap<>();
        operatorMapping.put(".lte", "[$lte]");
        operatorMapping.put(".gte", "[$gte]");
        operatorMapping.put(".lt", "[$before]");
        operatorMapping.put(".gt", "[$after]");
        operatorMapping.put(".eq", "");
        operatorMapping.put(".ne", "[$ne]");
        operatorMapping.put(".in", "[$in]");
        operatorMapping.put(".nin", "[$nin]");
        operatorMapping.put(".like", "[$contains]");
        operatorMapping.put(".null", "[$null]");
        operatorMapping.put(".notnull", "[$null]");
        
        // Check if key ends with any known operator
        for (Map.Entry<String, String> mapping : operatorMapping.entrySet()) {
            if (key.endsWith(mapping.getKey())) {
                String fieldName = key.substring(0, key.length() - mapping.getKey().length());
                return fieldName + mapping.getValue();
            }
        }
        
        return key;
    }
    
    /**
     * Convert a value to string array format used by BaseService
     * This is for non-$in operators
     */
    private String[] convertValueToStringArray(Object value) {
        if (value == null) {
            return new String[0];
        }
        
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                .map(Object::toString)
                .toArray(String[]::new);
        }
        
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.stream(array)
                .map(Object::toString)
                .toArray(String[]::new);
        }
        
        String stringValue = value.toString();
        if (stringValue.contains(",")) {
            return stringValue.split(",");
        }
        
        return new String[]{stringValue};
    }
    
    /**
     * Register a predefined query
     */
    private void registerPredefinedQuery(String name, PredefinedQuery query) {
        predefinedQueries.put(name, query);
        log.info("Registered predefined query: {}", name);
    }
    
    /**
     * Interface for predefined queries
     */
    @FunctionalInterface
    public interface PredefinedQuery {
        Map<String, Object> generateFilters(String tableName, Map<String, Object> parameters);
    }
}