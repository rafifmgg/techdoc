package com.ocmsintranet.cronservice.framework.services.tablequery.helper;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for entity-specific services
 * Provides lookup functionality to find the appropriate BaseService
 * implementation for a given entity class
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityServiceRegistry {
    private final ApplicationContext applicationContext;
    private final Map<Class<?>, BaseService<?, ?>> serviceCache = new HashMap<>();
    
    /**
     * Get the appropriate service for an entity class
     * 
     * @param entityClass The entity class
     * @return The service that handles this entity type
     */
    @SuppressWarnings("unchecked")
    public <E, K extends Serializable> BaseService<E, K> getServiceForEntity(Class<E> entityClass) {
        // Check cache first
        if (serviceCache.containsKey(entityClass)) {
            return (BaseService<E, K>) serviceCache.get(entityClass);
        }
        
        // Find appropriate service bean
        for (Object bean : applicationContext.getBeansOfType(BaseService.class).values()) {
            BaseService<?, ?> baseService = (BaseService<?, ?>) bean;
            
            if (baseService instanceof BaseImplement) {
                BaseImplement<?, ?, ?> baseImplement = (BaseImplement<?, ?, ?>) baseService;
                
                try {
                    Class<?> serviceEntityClass = baseImplement.getEntityClass();
                    if (serviceEntityClass.equals(entityClass)) {
                        // Found matching service
                        serviceCache.put(entityClass, baseService);
                        return (BaseService<E, K>) baseService;
                    }
                } catch (Exception e) {
                    // Skip services that don't properly implement getEntityClass
                    log.debug("Skipping service: {}", baseService.getClass().getName());
                }
            }
        }
        
        log.warn("No service found for entity: {}", entityClass.getSimpleName());
        throw new IllegalArgumentException("No service found for entity: " + entityClass.getSimpleName());
    }
}
