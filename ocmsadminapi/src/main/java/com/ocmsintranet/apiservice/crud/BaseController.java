package com.ocmsintranet.apiservice.crud;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Id;

import org.springframework.beans.factory.annotation.Value;

/**
 * Base controller with common CRUD operations
 * This class is intended to be extended by all controllers in the application
 * 
 * @param <E> Entity type
 * @param <ID> Entity ID type
 * @param <S> Service type
 */
@Slf4j
public abstract class BaseController<E, ID extends Serializable, S extends BaseService<E, ID>> {

    @Value("${api.version}")
    private String apiVersion;
    
    @ModelAttribute("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }
    
    protected final S service;

    public BaseController(S service) {
        this.service = service;
    }

    /**
     * Create a new entity
     * This method handles both simple and composite primary keys
     * Returns a standardized response format
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object payload) {
        if (payload instanceof List<?>) {
            // Handle array of entity objects
            try {
                List<E> entities = convertToEntities(payload);
                service.saveAll(entities);
                
                // Return standardized success response
                CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } catch (Exception e) {
                // Return standardized error response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
                responseData.setMessage("Error processing request: " + e.getMessage());
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
        } else {
            // Handle single entity object
            try {
                E entity = convertToEntity(payload);
                service.save(entity);
                
                // Return standardized success response
                CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } catch (Exception e) {
                // Return standardized error response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
                responseData.setMessage("Error processing request: " + e.getMessage());
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
        }
    }
    
    /**
     * Convert a payload object to an entity instance
     * This handles the case where the payload is a Map (from JSON deserialization)
     * and ensures audit fields are properly set for all entities
     */
    @SuppressWarnings("unchecked")
    protected E convertToEntity(Object payload) {
        // Get the entity class from the generic type parameter
        Class<E> entityClass = getEntityClass();
        
        // If payload is already the correct entity type, return it
        if (entityClass.isInstance(payload)) {
            return (E) payload;
        }
        
        // If payload is a Map (from JSON deserialization), convert it to an entity
        if (payload instanceof Map) {
            try {
                // Create a new instance of the entity class
                E entity = entityClass.getDeclaredConstructor().newInstance();
                
                // Get all fields from the entity class and its superclasses
                List<Field> allFields = new ArrayList<>();
                Class<?> currentClass = entityClass;
                
                while (currentClass != null && currentClass != Object.class) {
                    allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                    currentClass = currentClass.getSuperclass();
                }
                
                // Copy values from the map to the entity
                Map<String, Object> map = (Map<String, Object>) payload;
                
                // Process all fields
                for (Field field : allFields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    
                    if (map.containsKey(fieldName)) {
                        Object value = map.get(fieldName);
                        
                        // Handle type conversions for common types
                        if (value != null) {
                            Class<?> fieldType = field.getType();
                            
                            // Handle LocalDateTime
                            if (fieldType == LocalDateTime.class && value instanceof String) {
                                field.set(entity, LocalDateTime.parse((String) value));
                            }
                            // Handle BigDecimal
                            else if (fieldType == BigDecimal.class && value instanceof Number) {
                                field.set(entity, BigDecimal.valueOf(((Number) value).doubleValue()));
                            }
                            // Handle BigDecimal from String
                            else if (fieldType == BigDecimal.class && value instanceof String) {
                                field.set(entity, new BigDecimal((String) value));
                            }
                            // Handle primitive numbers
                            else if (Number.class.isAssignableFrom(fieldType) && value instanceof Number) {
                                if (fieldType == Integer.class) {
                                    field.set(entity, ((Number) value).intValue());
                                } else if (fieldType == Long.class) {
                                    field.set(entity, ((Number) value).longValue());
                                } else if (fieldType == Double.class) {
                                    field.set(entity, ((Number) value).doubleValue());
                                } else if (fieldType == Float.class) {
                                    field.set(entity, ((Number) value).floatValue());
                                } else {
                                    field.set(entity, value);
                                }
                            }
                            // Handle other types
                            else {
                                field.set(entity, value);
                            }
                        }
                    }
                }
                
                // Audit fields will be handled by JPA lifecycle callbacks (@PrePersist and @PreUpdate)
                
                return entity;
            } catch (Exception e) {
                throw new RuntimeException("Error converting Map to entity: " + e.getMessage(), e);
            }
        }
        
        // If we can't convert, try a simple cast
        return (E) payload;
    }
    
    /**
     * Extract the ID from the entity (works for both simple and composite keys)
     */
    @SuppressWarnings("unchecked")
    protected ID extractIdFromEntity(E entity) {
        try {
            Class<E> entityClass = getEntityClass();
            Class<ID> idClass = getIdClass();

            // If simple ID (entity and ID class are the same or ID is primitive/well-known)
            if (idClass.equals(entityClass) || idClass.equals(String.class) || idClass.equals(Integer.class) || idClass.equals(Long.class) || idClass.equals(java.util.UUID.class)) {
                // Try to find field named 'id' or annotated with @Id
                for (Field field : entityClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getName().equalsIgnoreCase("id") || field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                        return (ID) field.get(entity);
                    }
                }
                // Fallback: try first field
                Field field = entityClass.getDeclaredFields()[0];
                field.setAccessible(true);
                return (ID) field.get(entity);
            }

            // Composite key: build ID object from entity fields
            ID idInstance = idClass.getDeclaredConstructor().newInstance();
            for (Field idField : idClass.getDeclaredFields()) {
                idField.setAccessible(true);
                String idFieldName = idField.getName();
                // Find matching field in entity
                Field entityField = null;
                try {
                    entityField = entityClass.getDeclaredField(idFieldName);
                } catch (NoSuchFieldException nsfe) {
                    // Try superclasses
                    Class<?> superClass = entityClass.getSuperclass();
                    while (superClass != null && superClass != Object.class) {
                        try {
                            entityField = superClass.getDeclaredField(idFieldName);
                            break;
                        } catch (NoSuchFieldException ignore) {
                        }
                        superClass = superClass.getSuperclass();
                    }
                }
                if (entityField != null) {
                    entityField.setAccessible(true);
                    Object value = entityField.get(entity);
                    idField.set(idInstance, value);
                }
            }
            return idInstance;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting ID from entity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a clean response with only appCode and message
     * This returns a JSON structure like: {"data":{"appCode":"OCMS-2000","message":"Save Success."}}
     */
    protected ResponseEntity<?> createCleanResponse(String appCode, String message, HttpStatus status) {
        // Create a simple map structure for the response
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("appCode", appCode);
        data.put("message", message);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("data", data);
        
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * Convert a payload object to a list of entity instances
     */
    @SuppressWarnings("unchecked")
    protected List<E> convertToEntities(Object payload) {
        if (payload instanceof List) {
            List<?> list = (List<?>) payload;
            List<E> entities = new ArrayList<>();
            
            for (Object item : list) {
                entities.add(convertToEntity(item));
            }
            
            return entities;
        }
        
        throw new IllegalArgumentException("Payload is not a list");
    }
    
    /**
     * Get the entity class from the generic type parameter
     */
    @SuppressWarnings("unchecked")
    protected Class<E> getEntityClass() {
        // Get the generic type parameters
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<E>) type.getActualTypeArguments()[0];
    }

    /**
     * Get the ID class from the generic type parameter
     */
    @SuppressWarnings("unchecked")
    protected Class<ID> getIdClass() {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<ID>) type.getActualTypeArguments()[1];
    }

    /**
     * Get entity by ID with path variables for composite keys with 2 parts
     * Returns the entity if found
     * 
     * This method handles composite primary keys by extracting the key parts from the URL path
     * and mapping them to the appropriate fields in the entity's ID class.
     */
    @GetMapping("/{id1}/{id2}")
    public ResponseEntity<?> getByCompositeId(
            @PathVariable String id1,
            @PathVariable String id2) {
        return getByCompositeIdInternal(new String[]{id1, id2});
    }
    
    /**
     * Get entity by ID with path variables for composite keys with 3 parts
     * Returns the entity if found
     */
    @GetMapping("/{id1}/{id2}/{id3}")
    public ResponseEntity<?> getByCompositeId(
            @PathVariable String id1,
            @PathVariable String id2,
            @PathVariable String id3) {
        return getByCompositeIdInternal(new String[]{id1, id2, id3});
    }
    
    /**
     * Internal method to handle composite key retrieval with variable number of parts
     * This method supports composite keys with 2 or more fields
     */
    private ResponseEntity<?> getByCompositeIdInternal(String[] idParts) {
        try {
            // Get entity class and ID class information for better handling
            Class<E> entityClass = getEntityClass();
            Class<ID> idClass = getIdClass();
            String entityName = entityClass.getSimpleName();
            
            // Create a new entity instance
            E entity = entityClass.getDeclaredConstructor().newInstance();
            
            // Try to directly create the ID object for any entity with a composite key
            // This is a more reliable approach than trying to set fields on the entity first
            if (!idClass.equals(entityClass) && !idClass.equals(Serializable.class)) {
                
                try {
                    // Get the fields of the ID class
                    Field[] idFields = idClass.getDeclaredFields();
                    
                    // Check if we have the right number of fields and path variables
                    if (idFields.length == idParts.length) {
                        // Create a new instance of the ID class
                        Constructor<ID> constructor = idClass.getDeclaredConstructor();
                        ID id = constructor.newInstance();
                        
                        // Build a description for debugging and error messages
                        StringBuilder fieldDetails = new StringBuilder();
                        
                        // Set each field with the corresponding path variable
                        for (int i = 0; i < idFields.length; i++) {
                            Field field = idFields[i];
                            field.setAccessible(true);
                            Class<?> fieldType = field.getType();
                            Object value = idParts[i];
                            if (fieldType == Integer.class || fieldType == int.class) {
                                value = Integer.valueOf(idParts[i]);
                            } else if (fieldType == LocalDateTime.class) {
                                value = LocalDateTime.parse(idParts[i]);
                            } // else leave as String
                            field.set(id, value);
                            
                            if (i > 0) fieldDetails.append(" and ");
                            fieldDetails.append(field.getName()).append("=").append(idParts[i]);
                        }
                        
                        // Get the entity using the ID
                        Optional<E> entityOpt = service.getById(id);
                        
                        if (entityOpt.isPresent()) {
                            return new ResponseEntity<>(entityOpt.get(), HttpStatus.OK);
                        } else {
                            // Return standardized not found response
                            CrudResponse<?> errorResponse = new CrudResponse<>();
                            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                            responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                            
                            // Create a more informative error message with the actual field names
                            String errorMessage = String.format("%s with %s not found",
                                    entityName, fieldDetails.toString());
                            responseData.setMessage(errorMessage);
                            errorResponse.setData(responseData);
                            
                            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
                        }
                    } else {
                        // Continue with normal flow if ID class doesn't match path variable count
                    }
                } catch (Exception e) {
                    // Continue with normal flow if direct ID creation fails
                }
            }
            
            // Fallback approach: try to set fields on the entity and extract ID
            if (idParts.length == 2) {
                // Set the ID fields based on path variables (for backward compatibility)
                setIdFieldsFromPathVariables(entity, idParts[0], idParts[1]);
            } else {
                // For more than 2 parts, we need a different approach
                
                // Get all fields from the entity class and its superclasses
                List<Field> allFields = new ArrayList<>();
                Class<?> currentClass = entityClass;
                
                while (currentClass != null && currentClass != Object.class) {
                    allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                    currentClass = currentClass.getSuperclass();
                }
                
                // Look for ID fields or fields with @Id annotation
                List<Field> potentialIdFields = new ArrayList<>();
                
                for (Field field : allFields) {
                    field.setAccessible(true);
                    String fieldName = field.getName().toLowerCase();
                    
                    // Check if field is likely an ID field
                    if (fieldName.contains("id") || 
                        fieldName.contains("code") || 
                        fieldName.contains("key") ||
                        fieldName.contains("number")) {
                        potentialIdFields.add(field);
                    }
                }
                
                // Set values on potential ID fields, up to the number of path variables
                int fieldsToSet = Math.min(potentialIdFields.size(), idParts.length);
                for (int i = 0; i < fieldsToSet; i++) {
                    Field field = potentialIdFields.get(i);
                    try {
                        setFieldValue(entity, field, idParts[i]);
                    } catch (Exception e) {
                        // Ignore errors setting fields
                    }
                }
            }
            
            // Extract ID from the entity using the entity's metadata
            ID id = extractIdFromEntity(entity);
            
            // Get the entity from the service
            Optional<E> entityOpt = service.getById(id);
            
            if (entityOpt.isPresent()) {
                return new ResponseEntity<>(entityOpt.get(), HttpStatus.OK);
            } else {
                // Return standardized not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                
                // Include entity information in the error message for better context
                String errorMessage = String.format("%s with ID %s not found", 
                        entityName, 
                        id != null ? id.toString() : "unknown");
                responseData.setMessage(errorMessage);
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Get entity by single ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            // For simple IDs, convert the path variable to the appropriate type
            ID entityId = convertPathVariableToId(id);
            
            // Get the entity
            Optional<E> entityOpt = service.getById(entityId);
            
            if (entityOpt.isPresent()) {
                return new ResponseEntity<>(entityOpt.get(), HttpStatus.OK);
            } else {
                // Return not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                responseData.setMessage(CrudResponse.Messages.RECORD_NOT_FOUND);
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Convert a path variable to the appropriate ID type
     */
    @SuppressWarnings("unchecked")
    protected ID convertPathVariableToId(String pathVariable) {
        try {
            // Get the ID class
            Class<ID> idClass = (Class<ID>) ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[1];
            
            // Handle common ID types
            if (idClass == String.class) {
                return (ID) pathVariable;
            } else if (idClass == Integer.class || idClass == int.class) {
                return (ID) Integer.valueOf(pathVariable);
            } else if (idClass == Long.class || idClass == long.class) {
                return (ID) Long.valueOf(pathVariable);
            } else if (idClass == UUID.class) {
                return (ID) UUID.fromString(pathVariable);
            }
            
            // If we can't convert, return null
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting path variable to ID: " + e.getMessage(), e);
        }
    }

    /**
     * Update entity (full update) with single ID path variable
     * This method handles simple primary keys
     * Returns a standardized response format
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWithPathVariable(
            @PathVariable String id,
            @RequestBody Object payload) {
        try {
            // Convert the payload to an entity
            E entity = convertToEntity(payload);
            
            // Convert path variable to ID
            ID entityId = convertPathVariableToId(id);
            
            // Update the entity
            service.update(entityId, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Update entity (full update) with path variables (2-part composite key)
     * This method handles composite primary keys with 2 parts
     * Returns a standardized response format
     */
    @PutMapping("/{id1}/{id2}")
    public ResponseEntity<?> updateWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @RequestBody Object payload) {
        return updateWithPathVariablesInternal(new String[]{id1, id2}, payload);
    }
    
    /**
     * Update entity (full update) with path variables (3-part composite key)
     * This method handles composite primary keys with 3 parts
     * Returns a standardized response format
     */
    @PutMapping("/{id1}/{id2}/{id3}")
    public ResponseEntity<?> updateWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @PathVariable String id3,
            @RequestBody Object payload) {
        return updateWithPathVariablesInternal(new String[]{id1, id2, id3}, payload);
    }
    
    /**
     * Internal method to handle update operations with variable number of ID parts
     * This method supports composite keys with 2 or more fields
     */
    private ResponseEntity<?> updateWithPathVariablesInternal(String[] idParts, Object payload) {
        try {
            // Convert the payload to an entity
            E entity = convertToEntity(payload);
            
            // Get entity class and ID class information for better handling
            Class<E> entityClass = getEntityClass();
            Class<ID> idClass = getIdClass();
            
            // Try to directly create the ID object for any entity with a composite key
            if (!idClass.equals(entityClass) && !idClass.equals(Serializable.class)) {
                try {
                    // Get the fields of the ID class
                    Field[] idFields = idClass.getDeclaredFields();
                    
                    // Check if we have the right number of fields and path variables
                    if (idFields.length == idParts.length) {
                        // Create a new instance of the ID class
                        Constructor<ID> constructor = idClass.getDeclaredConstructor();
                        ID id = constructor.newInstance();
                        
                        // Set each field with the corresponding path variable
                        for (int i = 0; i < idFields.length; i++) {
                            Field field = idFields[i];
                            field.setAccessible(true);
                            field.set(id, idParts[i]);
                        }
                        
                        // Update the entity
                        service.update(id, entity);
                        
                        // Return standardized success response
                        CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                } catch (Exception e) {
                    // Continue with fallback approach if direct ID creation fails
                }
            }
            
            // Fallback: use the old approach for 2-part keys
            if (idParts.length == 2) {
                // Set the ID fields based on path variables
                setIdFieldsFromPathVariables(entity, idParts[0], idParts[1]);
            } else {
                // For more than 2 parts, use a generic approach
                // Get all fields from the entity class and its superclasses
                List<Field> allFields = new ArrayList<>();
                Class<?> currentClass = entityClass;
                
                while (currentClass != null && currentClass != Object.class) {
                    allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                    currentClass = currentClass.getSuperclass();
                }
                
                // Look for ID fields or fields with @Id annotation
                List<Field> potentialIdFields = new ArrayList<>();
                
                for (Field field : allFields) {
                    field.setAccessible(true);
                    String fieldName = field.getName().toLowerCase();
                    
                    // Check if field is likely an ID field
                    if (fieldName.contains("id") || 
                        fieldName.contains("code") || 
                        fieldName.contains("key") ||
                        fieldName.contains("number")) {
                        potentialIdFields.add(field);
                    }
                }
                
                // Set values on potential ID fields, up to the number of path variables
                int fieldsToSet = Math.min(potentialIdFields.size(), idParts.length);
                for (int i = 0; i < fieldsToSet; i++) {
                    Field field = potentialIdFields.get(i);
                    try {
                        setFieldValue(entity, field, idParts[i]);
                    } catch (Exception e) {
                        // Ignore errors in field setting
                    }
                }
            }
            
            // Extract ID from the entity
            ID id = extractIdFromEntity(entity);
            
            // Update the entity
            service.update(id, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Update entity (full update) with request body only
     * This method handles both simple and composite primary keys
     * Returns a standardized response format
     */
    @PutMapping
    public ResponseEntity<?> update(@RequestBody Object payload) {
        try {
            // For entities with composite keys, we'll use the entity's ID fields from the payload
            E entity = convertToEntity(payload);
            
            // Extract ID from the entity (works for both simple and composite keys)
            ID id = extractIdFromEntity(entity);
            
            // Update the entity
            service.update(id, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Set ID fields on the entity based on path variables
     */
    protected void setIdFieldsFromPathVariables(E entity, String id1, String id2) {
        // Get entity class and ID class information for better handling
        Class<E> entityClass = getEntityClass();
        Class<ID> idClass = getIdClass();
        
        // Try to directly set ID fields using ID class field names
        try {
            // Get the fields of the ID class
            Field[] idFields = idClass.getDeclaredFields();
            
            // Check if we have the right number of fields and path variables
            if (idFields.length == 2) {
                // Set each field with the corresponding path variable
                for (int i = 0; i < idFields.length; i++) {
                    Field field = idFields[i];
                    field.setAccessible(true);
                    if (i == 0) {
                        setFieldValue(entity, field, id1);
                    } else {
                        setFieldValue(entity, field, id2);
                    }
                }
            }
        } catch (Exception e) {
            // Continue with fallback approach if direct ID field setting fails
        }
        
        // Fallback: use naming conventions to set ID fields
        // Get all fields from the entity class and its superclasses
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = entityClass;
        
        while (currentClass != null && currentClass != Object.class) {
            allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        
        // Look for ID fields or fields with @Id annotation
        List<Field> potentialIdFields = new ArrayList<>();
        
        for (Field field : allFields) {
            field.setAccessible(true);
            String fieldName = field.getName().toLowerCase();
            
            // Check if field is likely an ID field
            if (fieldName.contains("id") || 
                fieldName.contains("code") || 
                fieldName.contains("key") ||
                fieldName.contains("number")) {
                potentialIdFields.add(field);
            }
        }
        
        // Set values on potential ID fields, up to the number of path variables
        int fieldsToSet = Math.min(potentialIdFields.size(), 2);
        for (int i = 0; i < fieldsToSet; i++) {
            Field field = potentialIdFields.get(i);
            try {
                if (i == 0) {
                    setFieldValue(entity, field, id1);
                } else {
                    setFieldValue(entity, field, id2);
                }
            } catch (Exception e) {
                // Ignore errors in field setting
            }
        }
    }

    /**
     * Set a field value with appropriate type conversion
     */
    private void setFieldValue(E entity, Field field, String value) throws Exception {
        Class<?> fieldType = field.getType();
        field.setAccessible(true);
        if (fieldType == String.class) {
            field.set(entity, value);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            field.set(entity, Integer.parseInt(value));
        } else if (fieldType == Long.class || fieldType == long.class) {
            field.set(entity, Long.parseLong(value));
        } else if (fieldType == Double.class || fieldType == double.class) {
            field.set(entity, Double.parseDouble(value));
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            field.set(entity, Boolean.parseBoolean(value));
        } else if (fieldType == java.time.LocalDate.class) {
            field.set(entity, java.time.LocalDate.parse(value));
        } else if (fieldType == java.time.LocalDateTime.class) {
            field.set(entity, java.time.LocalDateTime.parse(value));
        } else {
            // Fallback: try to set as String
            field.set(entity, value);
        }
    }

    /**
     * Patch entity (partial update) with single ID path variable
     * This method handles simple primary keys
     * Returns a standardized response format
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchWithPathVariable(
            @PathVariable String id,
            @RequestBody Object payload) {
        try {
            // Convert the payload to an entity
            E entity = convertToEntity(payload);
            
            // Convert path variable to ID
            ID entityId = convertPathVariableToId(id);
            
            // Patch the entity
            service.patch(entityId, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Patch entity (partial update) with path variables (2-part composite key)
     * This method handles composite primary keys with 2 parts
     * Returns a standardized response format
     */
    @PatchMapping("/{id1}/{id2}")
    public ResponseEntity<?> patchWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @RequestBody Object payload) {
        return patchWithPathVariablesInternal(new String[]{id1, id2}, payload);
    }
    
    /**
     * Patch entity (partial update) with path variables (3-part composite key)
     * This method handles composite primary keys with 3 parts
     * Returns a standardized response format
     */
    @PatchMapping("/{id1}/{id2}/{id3}")
    public ResponseEntity<?> patchWithPathVariables(
            @PathVariable String id1,
            @PathVariable String id2,
            @PathVariable String id3,
            @RequestBody Object payload) {
        return patchWithPathVariablesInternal(new String[]{id1, id2, id3}, payload);
    }
    
    /**
     * Internal method to handle patch operations with variable number of ID parts
     * This method supports composite keys with 2 or more fields
     */
    private ResponseEntity<?> patchWithPathVariablesInternal(String[] idParts, Object payload) {
        try {
            log.debug("PATCH: payload = {}", payload);
            // Convert the payload to an entity
            E entity = convertToEntity(payload);
            log.debug("PATCH: new entity = {}", entity);
            
            // Get entity class and ID class information for better handling
            Class<E> entityClass = getEntityClass();
            Class<ID> idClass = getIdClass();
            
            // Try to directly create the ID object for any entity with a composite key
            if (!idClass.equals(entityClass) && !idClass.equals(Serializable.class)) {
                try {
                    // Get the fields of the ID class
                    Field[] idFields = idClass.getDeclaredFields();
                    
                    // Check if we have the right number of fields and path variables
                    if (idFields.length == idParts.length) {
                        // Create a new instance of the ID class
                        Constructor<ID> constructor = idClass.getDeclaredConstructor();
                        ID id = constructor.newInstance();
                        
                        // Set each field with the corresponding path variable
                        for (int i = 0; i < idFields.length; i++) {
                            Field field = idFields[i];
                            field.setAccessible(true);
                            Class<?> fieldType = field.getType();
                            Object value = idParts[i];
                            if (fieldType == Integer.class || fieldType == int.class) {
                                value = Integer.valueOf(idParts[i]);
                            } else if (fieldType == LocalDateTime.class) {
                                value = LocalDateTime.parse(idParts[i]);
                            } // else leave as String
                            field.set(id, value);
                        }
                        
                        // Patch the entity
                        service.patch(id, entity);
                        
                        // Return standardized success response
                        CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                } catch (Exception e) {
                    // Continue with fallback approach if direct ID creation fails
                }
            }
            
            // Fallback: use the old approach for 2-part keys
            if (idParts.length == 2) {
                // Set the ID fields based on path variables
                setIdFieldsFromPathVariables(entity, idParts[0], idParts[1]);
            } else {
                // For more than 2 parts, use a generic approach
                // Get all fields from the entity class and its superclasses
                List<Field> allFields = new ArrayList<>();
                Class<?> currentClass = entityClass;
                
                while (currentClass != null && currentClass != Object.class) {
                    allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                    currentClass = currentClass.getSuperclass();
                }
                
                // Look for ID fields or fields with @Id annotation
                List<Field> potentialIdFields = new ArrayList<>();
                
                for (Field field : allFields) {
                    field.setAccessible(true);
                    String fieldName = field.getName().toLowerCase();
                    
                    // Check if field is likely an ID field
                    if (fieldName.contains("id") || 
                        fieldName.contains("code") || 
                        fieldName.contains("key") ||
                        fieldName.contains("number")) {
                        potentialIdFields.add(field);
                    }
                }
                
                // Set values on potential ID fields, up to the number of path variables
                int fieldsToSet = Math.min(potentialIdFields.size(), idParts.length);
                for (int i = 0; i < fieldsToSet; i++) {
                    Field field = potentialIdFields.get(i);
                    try {
                        setFieldValue(entity, field, idParts[i]);
                    } catch (Exception e) {
                        // Ignore errors in field setting
                    }
                }
            }
            
            // Extract ID from the entity
            ID id = extractIdFromEntity(entity);
            
            // Patch the entity
            service.patch(id, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Patch entity (partial update) with request body only
     * This method handles both simple and composite primary keys
     * Returns a standardized response format
     */
    @PatchMapping
    public ResponseEntity<?> patch(@RequestBody Object payload) {
        try {
            // For entities with composite keys, we'll use the entity's ID fields from the payload
            E entity = convertToEntity(payload);
            
            // Extract ID from the entity (works for both simple and composite keys)
            ID id = extractIdFromEntity(entity);
            
            // Patch the entity
            service.patch(id, entity);
            
            // Return standardized success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    

    
    /**
     * Delete entity with 2-part composite key
     * Returns a standardized response format
     */
    @DeleteMapping("/{id1}/{id2}")
    public ResponseEntity<?> deleteWithCompositeKey(
            @PathVariable String id1,
            @PathVariable String id2) {
        return deleteWithCompositeKeyInternal(new String[]{id1, id2});
    }
    
    /**
     * Delete entity with 3-part composite key
     * Returns a standardized response format
     */
    @DeleteMapping("/{id1}/{id2}/{id3}")
    public ResponseEntity<?> deleteWithCompositeKey(
            @PathVariable String id1,
            @PathVariable String id2,
            @PathVariable String id3) {
        return deleteWithCompositeKeyInternal(new String[]{id1, id2, id3});
    }
    
    /**
     * Internal method to handle delete operations with variable number of ID parts
     * This method supports composite keys with 2 or more fields
     */
    private ResponseEntity<?> deleteWithCompositeKeyInternal(String[] idParts) {
        try {
            // Get entity class and ID class information for better handling
            Class<E> entityClass = getEntityClass();
            Class<ID> idClass = getIdClass();
            
            // Try to directly create the ID object for any entity with a composite key
            if (!idClass.equals(entityClass) && !idClass.equals(Serializable.class)) {
                try {
                    // Get the fields of the ID class
                    Field[] idFields = idClass.getDeclaredFields();
                    
                    // Check if we have the right number of fields and path variables
                    if (idFields.length == idParts.length) {
                        // Create a new instance of the ID class
                        Constructor<ID> constructor = idClass.getDeclaredConstructor();
                        ID id = constructor.newInstance();
                        
                        // Set each field with the corresponding path variable
                        for (int i = 0; i < idFields.length; i++) {
                            Field field = idFields[i];
                            field.setAccessible(true);
                            field.set(id, idParts[i]);
                        }
                        
                        // Delete the entity
                        service.delete(id);
                        
                        // Return standardized success response
                        CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.DELETE_SUCCESS);
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                } catch (Exception e) {
                    // Return error response if direct ID creation fails
                    CrudResponse<?> errorResponse = new CrudResponse<>();
                    CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                    responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
                    responseData.setMessage("Error creating composite key: " + e.getMessage());
                    errorResponse.setData(responseData);
                    
                    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                }
            }
            
            // If we get here, something went wrong with ID creation
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Unable to create composite key for delete operation");
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete entity
     * Returns a standardized response format
     * 
     * This method handles both simple and composite primary keys via path variable
     * For simple IDs, it will convert the String path variable to the appropriate ID type
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            // Convert path variable to ID (works for simple IDs)
            ID entityId = convertPathVariableToId(id);
            
            // Delete the entity
            boolean deleted = service.delete(entityId);
            
            if (deleted) {
                // Return standardized success response
                CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.DELETE_SUCCESS);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                // Return not found response
                CrudResponse<?> errorResponse = new CrudResponse<>();
                CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
                responseData.setAppCode(CrudResponse.AppCodes.NOT_FOUND);
                responseData.setMessage(CrudResponse.Messages.RECORD_NOT_FOUND);
                errorResponse.setData(responseData);
                
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Return standardized error response
            CrudResponse<?> errorResponse = new CrudResponse<>();
            CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
            responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
            responseData.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setData(responseData);
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all entities with filtering, sorting, and pagination
     * 
     * Example query:
     * GET /api/demo?name[$regex]=test&status=1&$sort=createdAt:desc&$limit=10&$skip=0
     */
    @GetMapping
    public ResponseEntity<FindAllResponse<E>> getAll(@RequestParam MultiValueMap<String, String> params) {
        // Convert MultiValueMap to the format expected by BaseService
        Map<String, String[]> normalizedParams = new HashMap<>();
        
        params.forEach((key, values) -> {
            normalizedParams.put(key, values.toArray(new String[0]));
        });
        
        FindAllResponse<E> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get distinct values for a field
     * 
     * Example query:
     * GET /api/demo/distinct/status?createdAt[$gte]=2023-01-01
     */
    @GetMapping("/distinct/{fieldName}")
    public ResponseEntity<List<String>> getDistinct(
            @PathVariable String fieldName,
            @RequestParam Map<String, String[]> params) {
        List<String> values = service.getStringFieldAll(fieldName, params);
        return new ResponseEntity<>(values, HttpStatus.OK);
    }
}