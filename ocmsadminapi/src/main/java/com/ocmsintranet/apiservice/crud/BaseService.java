package com.ocmsintranet.apiservice.crud;

import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base service interface with common CRUD operations and advanced querying
 * 
 * @param <E> Entity type
 * @param <K> Primary key type (can be a composite key class)
 */
public interface BaseService<E, K extends Serializable> {

    /**
     * Create a new entity
     * 
     * @param entity The entity to save
     * @return The saved entity with ID
     */
    E save(E entity);
    
    /**
     * Save multiple entities
     * 
     * @param entities Collection of entities to save
     * @return List of saved entities with IDs
     */
    List<E> saveAll(Iterable<E> entities);
    
    /**
     * Update an entity completely
     * 
     * @param id The entity ID
     * @param entity The updated entity
     * @return The updated entity
     */
    E update(K id, E entity);
    
    /**
     * Partially update an entity (patch)
     * 
     * @param id The entity ID
     * @param partialEntity The partial entity with only fields to update
     * @return The updated entity
     */
    E patch(K id, E partialEntity);
    
    /**
     * Get entity by ID
     * 
     * @param id The entity ID
     * @return Optional containing the entity if found
     */
    Optional<E> getById(K id);
    
    /**
     * Delete entity by ID
     * 
     * @param id The entity ID
     * @return True if entity was deleted, false if not found
     */
    boolean delete(K id);
    
    /**
     * Get all entities with advanced filtering, sorting, and pagination
     * 
     * @param params Query parameters for filtering, sorting, and pagination
     * @return Paginated response with items and metadata
     */
    FindAllResponse<E> getAll(Map<String, String[]> params);
    
    /**
     * Get all values for a specific field with filtering
     * 
     * @param fieldName The field name to get values for
     * @param params Query parameters for filtering
     * @return List of distinct string values for the field
     */
    List<String> getStringFieldAll(String fieldName, Map<String, String[]> params);
}