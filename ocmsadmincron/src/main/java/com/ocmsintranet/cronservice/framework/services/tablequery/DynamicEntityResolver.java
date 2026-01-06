package com.ocmsintranet.cronservice.framework.services.tablequery;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.BaseService;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic entity class resolver that automatically discovers entity mappings
 * without requiring hardcoded table-to-entity mappings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicEntityResolver {
    
    private final ApplicationContext applicationContext;
    
    // Cache for table name to entity class mappings
    private final Map<String, Class<?>> tableToEntityCache = new ConcurrentHashMap<>();
    
    // Cache for entity class to service mappings
    private final Map<Class<?>, BaseService<?, ?>> entityToServiceCache = new ConcurrentHashMap<>();
    
    // Flag to track if discovery has been performed
    private volatile boolean discoveryCompleted = false;
    
    /**
     * Get entity class for table name using dynamic discovery
     */
    public Class<?> getEntityClassForTable(String tableName) {
        ensureDiscoveryCompleted();
        
        Class<?> entityClass = tableToEntityCache.get(tableName);
        if (entityClass == null) {
            throw new IllegalArgumentException("No entity found for table: " + tableName + 
                ". Available tables: " + tableToEntityCache.keySet());
        }
        
        return entityClass;
    }
    
    /**
     * Get service for entity class
     */
    @SuppressWarnings("unchecked")
    public <E, K extends Serializable> BaseService<E, K> getServiceForEntity(Class<E> entityClass) {
        ensureDiscoveryCompleted();
        
        BaseService<?, ?> service = entityToServiceCache.get(entityClass);
        if (service == null) {
            throw new IllegalArgumentException("No service found for entity: " + entityClass.getSimpleName());
        }
        
        return (BaseService<E, K>) service;
    }
    
    /**
     * Get all available table names
     */
    public Set<String> getAvailableTableNames() {
        ensureDiscoveryCompleted();
        return tableToEntityCache.keySet();
    }
    
    /**
     * Ensure entity discovery has been completed
     */
    private void ensureDiscoveryCompleted() {
        if (!discoveryCompleted) {
            synchronized (this) {
                if (!discoveryCompleted) {
                    performEntityDiscovery();
                    discoveryCompleted = true;
                }
            }
        }
    }
    
    /**
     * Perform dynamic discovery of entities and their corresponding services
     */
    private void performEntityDiscovery() {
        log.info("Starting dynamic entity discovery...");
        
        // Step 1: Discover all entity classes and their table names
        discoverEntityClasses();
        
        // Step 2: Discover all services and map them to entity classes
        discoverEntityServices();
        
        log.info("Entity discovery completed. Found {} entities with {} services", 
                tableToEntityCache.size(), entityToServiceCache.size());
        log.debug("Available tables: {}", tableToEntityCache.keySet());
    }
    
    /**
     * Discover all entity classes by scanning services
     */
    private void discoverEntityClasses() {
        // Get all BaseService implementations from Spring context
        Map<String, BaseService> services = applicationContext.getBeansOfType(BaseService.class);
        
        for (BaseService<?, ?> service : services.values()) {
            try {
                if (service instanceof BaseImplement) {
                    BaseImplement<?, ?, ?> baseImplement = (BaseImplement<?, ?, ?>) service;
                    Class<?> entityClass = baseImplement.getEntityClass();
                    
                    if (entityClass != null) {
                        String tableName = extractTableName(entityClass);
                        if (tableName != null) {
                            tableToEntityCache.put(tableName, entityClass);
                            entityToServiceCache.put(entityClass, service);
                            log.debug("Discovered entity: {} -> table: {}", entityClass.getSimpleName(), tableName);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract entity class from service: {}, error: {}", 
                         service.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * Alternative method: Discover entities by scanning generic type parameters
     */
    private void discoverEntityServices() {
        Map<String, BaseService> services = applicationContext.getBeansOfType(BaseService.class);
        
        for (BaseService<?, ?> service : services.values()) {
            try {
                Class<?> entityClass = extractEntityClassFromGenericType(service);
                if (entityClass != null && !entityToServiceCache.containsKey(entityClass)) {
                    String tableName = extractTableName(entityClass);
                    if (tableName != null) {
                        tableToEntityCache.put(tableName, entityClass);
                        entityToServiceCache.put(entityClass, service);
                        log.debug("Discovered entity via generics: {} -> table: {}", 
                                entityClass.getSimpleName(), tableName);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract entity class from generic type for service: {}, error: {}", 
                         service.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * Extract table name from entity class using @Table annotation or class name
     * IMPROVED: Better handling of table name mapping
     */
    private String extractTableName(Class<?> entityClass) {
        // First try @Table annotation
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        
        // If no @Table annotation, check if it's an @Entity
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation != null) {
            // Use entity name if specified, otherwise use class name
            String entityName = entityAnnotation.name();
            if (!entityName.isEmpty()) {
                return entityName;
            }
        }
        
        // Convert CamelCase class name to table name format
        String className = entityClass.getSimpleName();
        
        // Handle specific mappings for better discoverability
        if ("OcmsOffenceNoticeOwnerDriver".equals(className)) {
            return "ocms_offence_notice_owner_driver";
        }
        if ("OcmsValidOffenceNotice".equals(className)) {
            return "ocms_valid_offence_notice";
        }
        if ("OcmsHst".equals(className)) {
            return "ocms_hst";
        }
        if ("OcmsNroTemp".equals(className)) {
            return "ocms_nro_temp";
        }
        if ("OcmsSmsNotificationRecords".equals(className)) {
            return "ocms_sms_notification_records";
        }
        if ("OcmsEmailNotificationRecords".equals(className)) {
            return "ocms_email_notification_records";
        }
        if ("OcmsEnotificationExclusionList".equals(className)) {
            return "ocms_enotification_exclusion_list";
        }
        if ("OcmsSuspendedNotice".equals(className)) {
            return "ocms_suspended_notice";
        }
        if ("OcmsOffenceNoticeDetail".equals(className)) {
            return "ocms_offence_notice_detail";
        }
        if ("OcmsSuspensionReason".equals(className)) {
            return "ocms_suspension_reason";
        }
        if ("OffenceNoticeAddress".equals(className)) {
            return "ocms_offence_notice_owner_driver_addr";
        }
        if ("OffenceRuleCode".equals(className)) {
            return "ocms_offence_rule_code";
        }
        if ("Parameter".equals(className)) {
            return "ocms_parameter";
        }
        if ("OcmsComcryptOperation".equals(className)) {
            return "ocms_comcrypt_operation";
        }
        if ("JobExecutionHistory".equals(className)) {
            return "job_execution_history";
        }
        if ("WebTransactionAudit".equals(className)) {
            return "ocms_web_txn_audit";
        }
        
        // Fallback: convert CamelCase to snake_case
        return camelToSnakeCase(className);
    }

    /**
     * Convert CamelCase to snake_case
     */
    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Extract entity class from generic type parameters of BaseService
     */
    private Class<?> extractEntityClassFromGenericType(BaseService<?, ?> service) {
        try {
            // Get the generic superclass or interfaces
            Type[] interfaces = service.getClass().getGenericInterfaces();
            for (Type interfaceType : interfaces) {
                if (interfaceType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) interfaceType;
                    if (paramType.getRawType().equals(BaseService.class)) {
                        Type[] typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            return (Class<?>) typeArgs[0];
                        }
                    }
                }
            }
            
            // Try generic superclass
            Type genericSuperclass = service.getClass().getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericSuperclass;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting entity class from generic type: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Force refresh of entity mappings (useful for testing or dynamic updates)
     */
    public void refreshEntityMappings() {
        synchronized (this) {
            tableToEntityCache.clear();
            entityToServiceCache.clear();
            discoveryCompleted = false;
        }
        ensureDiscoveryCompleted();
    }
    
    /**
     * Check if a table exists in the discovered mappings
     */
    public boolean isTableSupported(String tableName) {
        ensureDiscoveryCompleted();
        return tableToEntityCache.containsKey(tableName);
    }
}