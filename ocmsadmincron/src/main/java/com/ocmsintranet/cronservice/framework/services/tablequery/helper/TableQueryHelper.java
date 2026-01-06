package com.ocmsintranet.cronservice.framework.services.tablequery.helper;

import com.ocmsintranet.cronservice.crud.BaseService;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Helper class for TableQueryService operations
 * Provides utility methods for entity manipulation, conversion, and reflection operations
 */
@Slf4j
public class TableQueryHelper {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    };

    // Pattern to match SQL Server datetime with timezone offset
    private static final Pattern SQL_SERVER_DATETIME_PATTERN = Pattern.compile(
        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,7}([+-]\\d{4})?");

    /**
     * Check if class is primitive or wrapper type
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type.equals(Boolean.class) || 
               type.equals(Byte.class) || 
               type.equals(Character.class) || 
               type.equals(Short.class) || 
               type.equals(Integer.class) || 
               type.equals(Long.class) || 
               type.equals(Float.class) || 
               type.equals(Double.class);
    }

    /**
     * Get field value using reflection with proper error handling
     */
    public static Object getFieldValue(Object entity, String fieldName) {
        if (entity == null) {
            return null;
        }

        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(entity);
            }
        } catch (Exception e) {
            log.error("Error getting field value for {}.{}: {}", entity.getClass().getSimpleName(), fieldName, e.getMessage());
        }
        
        return null;
    }

    /**
     * Set field value using reflection with proper error handling
     */
    public static void setFieldValue(Object entity, String fieldName, Object value) {
        if (entity == null) {
            return;
        }

        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object convertedValue = convertValueToFieldType(value, field.getType());
                field.set(entity, convertedValue);
            } else {
                log.warn("Field '{}' not found in entity class {}", fieldName, entity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Error setting field {}.{}: {}", entity.getClass().getSimpleName(), fieldName, e.getMessage());
            // Don't throw exception, just log and continue
        }
    }

    /**
     * Check if entity has composite key
     */
    public static boolean hasCompositeKey(Class<?> entityClass) {
        return entityClass.getAnnotation(IdClass.class) != null;
    }

    /**
     * Get all ID field names from entity
     */
    public static List<String> getIdFieldNames(Class<?> entityClass) {
        List<String> idFields = new ArrayList<>();
        
        for (Field field : getAllFields(entityClass)) {
            if (field.isAnnotationPresent(Id.class)) {
                idFields.add(field.getName());
            }
        }
        
        return idFields;
    }

    /**
     * Create a map containing only ID fields and their values
     */
    public static Map<String, Object> extractIdFieldsAsMap(Object entity) {
        Map<String, Object> idMap = new HashMap<>();
        
        if (entity == null) {
            return idMap;
        }
        
        try {
            for (Field field : getAllFields(entity.getClass())) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    idMap.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting ID fields from entity: {}", e.getMessage(), e);
        }
        
        return idMap;
    }

    /**
     * Validate that required fields are present in the field map
     */
    public static void validateRequiredFields(Class<?> entityClass, Map<String, Object> fields, List<String> requiredFields) {
        List<String> missingFields = new ArrayList<>();
        
        for (String requiredField : requiredFields) {
            if (!fields.containsKey(requiredField) || fields.get(requiredField) == null) {
                missingFields.add(requiredField);
            }
        }
        
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing required fields for " + entityClass.getSimpleName() + ": " + missingFields);
        }
    }

    /**
     * Get the actual field type considering generics and collections
     */
    public static Class<?> getFieldType(Class<?> entityClass, String fieldName) {
        Field field = findField(entityClass, fieldName);
        return field != null ? field.getType() : null;
    }

    /**
     * Check if a field exists in the entity class
     */
    public static boolean hasField(Class<?> entityClass, String fieldName) {
        return findField(entityClass, fieldName) != null;
    }

    /**
     * Copy non-null fields from source to target entity
     */
    public static void copyNonNullFields(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();
        
        try {
            for (Field sourceField : getAllFields(sourceClass)) {
                sourceField.setAccessible(true);
                Object value = sourceField.get(source);
                
                if (value != null) {
                    Field targetField = findField(targetClass, sourceField.getName());
                    if (targetField != null && targetField.getType().equals(sourceField.getType())) {
                        targetField.setAccessible(true);
                        targetField.set(target, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error copying fields from {} to {}: {}", sourceClass.getSimpleName(), targetClass.getSimpleName(), e.getMessage());
            throw new RuntimeException("Failed to copy fields", e);
        }
    }

    /**
     * Create a deep copy of an entity (useful for testing)
     */
    public static Object deepCopyEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            Class<?> entityClass = entity.getClass();
            Object copy = entityClass.getDeclaredConstructor().newInstance();
            
            for (Field field : getAllFields(entityClass)) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                if (value != null) {
                    // For simple types, just copy the value
                    if (isPrimitiveOrWrapper(field.getType()) || 
                        field.getType().equals(String.class) || 
                        field.getType().equals(BigDecimal.class) ||
                        field.getType().equals(LocalDate.class) ||
                        field.getType().equals(LocalDateTime.class) ||
                        field.getType().isEnum()) {
                        field.set(copy, value);
                    } else {
                        // For complex types, you might want to implement deeper copying
                        // For now, just reference copy
                        field.set(copy, value);
                    }
                }
            }
            
            return copy;
        } catch (Exception e) {
            log.error("Error creating deep copy of entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create deep copy", e);
        }
    }

    /**
     * Convert entity to a string representation for debugging
     */
    public static String entityToString(Object entity) {
        if (entity == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(entity.getClass().getSimpleName()).append("{");
        
        try {
            List<Field> fields = getAllFields(entity.getClass());
            boolean first = true;
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                if (!first) {
                    sb.append(", ");
                }
                
                sb.append(field.getName()).append("=");
                if (value instanceof String) {
                    sb.append("'").append(value).append("'");
                } else {
                    sb.append(value);
                }
                
                first = false;
            }
        } catch (Exception e) {
            sb.append("Error reading fields: ").append(e.getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert entity object to Map representation
     * Handles all field types including nested objects, dates, and collections
     */
    public static Map<String, Object> convertEntityToMap(Object entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get all fields from the entity class hierarchy
            List<Field> allFields = getAllFields(entity.getClass());
            
            for (Field field : allFields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                if (value != null) {
                    result.put(field.getName(), convertValueForSerialization(value));
                } else {
                    result.put(field.getName(), null);
                }
            }
        } catch (Exception e) {
            log.error("Error converting entity to map: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert entity to map", e);
        }
        
        return result;
    }

    /**
     * Create a partial entity with specified fields set
     * Used for PATCH operations and entity creation
     */
    public static Object createPartialEntity(Class<?> entityClass, Map<String, Object> fields) {
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            
            // Don't set audit fields here - let JPA @PrePersist/@PreUpdate handle them
            // This avoids conflicts with @NonEditable annotations on BaseEntity fields
            
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    Field field = findField(entityClass, fieldName);
                    if (field != null) {
                        // Skip @NonEditable fields to avoid constraint violations
                        if (field.isAnnotationPresent(com.ocmsintranet.cronservice.crud.annotations.NonEditable.class)) {
                            log.debug("Skipping @NonEditable field {}.{}", entityClass.getSimpleName(), fieldName);
                            continue;
                        }
                        
                        field.setAccessible(true);
                        
                        Object convertedValue = convertValueToFieldType(value, field.getType());
                        field.set(entity, convertedValue);
                        log.debug("Set field {}.{} = {} (converted from {})", 
                                entityClass.getSimpleName(), fieldName, convertedValue, value);
                    } else {
                        log.warn("Field '{}' not found in entity class {}", fieldName, entityClass.getSimpleName());
                    }
                } catch (Exception e) {
                    log.error("Error setting field {}.{}: {}", entityClass.getSimpleName(), fieldName, e.getMessage());
                    // Continue with other fields even if one fails
                }
            }
            
            return entity;
        } catch (Exception e) {
            log.error("Error creating partial entity for class {}: {}", entityClass.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to create partial entity", e);
        }
    }

    /**
     * Patch an entity using BaseService with proper ID handling
     * Supports both simple and composite keys
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object patchEntity(BaseService service, Object existingEntity, Object partialEntity) {
        try {
            // Extract the ID from the existing entity
            Object id = extractEntityId(existingEntity);
            
            if (id == null) {
                throw new RuntimeException("Could not extract ID from entity: " + existingEntity.getClass().getSimpleName());
            }
            
            // Use BaseService patch method
            return service.patch((Serializable) id, partialEntity);
        } catch (Exception e) {
            log.error("Error patching entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to patch entity", e);
        }
    }

    /**
     * Save a new entity using BaseService
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object saveEntity(BaseService service, Object entity) {
        try {
            return service.save(entity);
        } catch (Exception e) {
            log.error("Error saving entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    /**
     * Extract ID value from entity (handles both simple and composite keys)
     */
    public static Object extractEntityId(Object entity) {
        Class<?> entityClass = entity.getClass();
        
        try {
            // Check if entity has composite key
            IdClass idClassAnnotation = entityClass.getAnnotation(IdClass.class);
            if (idClassAnnotation != null) {
                // Handle composite key
                Class<?> idClass = idClassAnnotation.value();
                Object compositeId = idClass.getDeclaredConstructor().newInstance();
                
                // Find all @Id fields and copy their values to composite ID object
                for (Field field : getAllFields(entityClass)) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        
                        // Set corresponding field in composite ID
                        Field idField = findField(idClass, field.getName());
                        if (idField != null) {
                            idField.setAccessible(true);
                            idField.set(compositeId, value);
                        }
                    }
                }
                
                return compositeId;
            } else {
                // Handle simple key
                for (Field field : getAllFields(entityClass)) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting ID from entity: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Convert value for JSON serialization
     * Handles dates, enums, and other special types
     */
    private static Object convertValueForSerialization(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof LocalDate) {
            return ((LocalDate) value).toString();
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toString();
        } else if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toString();
        } else if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toString();
        } else if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        } else if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            List<Object> converted = new ArrayList<>();
            for (Object item : collection) {
                converted.add(convertValueForSerialization(item));
            }
            return converted;
        } else if (value instanceof byte[]) {
            // Handle byte arrays specially - convert to Base64 string for JSON serialization
            byte[] byteArray = (byte[]) value;
            return java.util.Base64.getEncoder().encodeToString(byteArray);
        } else if (value.getClass().isArray()) {
            // Handle other array types
            Class<?> componentType = value.getClass().getComponentType();
            if (componentType.isPrimitive()) {
                // Handle primitive arrays
                if (componentType == int.class) {
                    int[] array = (int[]) value;
                    return Arrays.stream(array).boxed().collect(java.util.stream.Collectors.toList());
                } else if (componentType == long.class) {
                    long[] array = (long[]) value;
                    return Arrays.stream(array).boxed().collect(java.util.stream.Collectors.toList());
                } else if (componentType == double.class) {
                    double[] array = (double[]) value;
                    return Arrays.stream(array).boxed().collect(java.util.stream.Collectors.toList());
                } else if (componentType == float.class) {
                    float[] array = (float[]) value;
                    List<Float> converted = new ArrayList<>();
                    for (float f : array) {
                        converted.add(f);
                    }
                    return converted;
                } else if (componentType == boolean.class) {
                    boolean[] array = (boolean[]) value;
                    List<Boolean> converted = new ArrayList<>();
                    for (boolean b : array) {
                        converted.add(b);
                    }
                    return converted;
                } else {
                    // For other primitive types, convert to string
                    return value.toString();
                }
            } else {
                // Handle object arrays
                Object[] array = (Object[]) value;
                List<Object> converted = new ArrayList<>();
                for (Object item : array) {
                    converted.add(convertValueForSerialization(item));
                }
                return converted;
            }
        } else if (isPrimitiveOrWrapper(value.getClass()) || value instanceof String || value instanceof BigDecimal) {
            return value;
        } else {
            // For complex objects, convert to map
            return convertEntityToMap(value);
        }
    }

    /**
     * Get all fields from class hierarchy
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return allFields;
    }

    /**
     * Find field in class hierarchy
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Utility method to log entity changes for debugging
     */
    public static void logEntityChanges(Object originalEntity, Object updatedEntity, String operation) {
        if (!log.isDebugEnabled()) {
            return;
        }
        
        log.debug("=== {} Operation ===", operation);
        log.debug("Original: {}", entityToString(originalEntity));
        log.debug("Updated:  {}", entityToString(updatedEntity));
        
        if (originalEntity != null && updatedEntity != null) {
            try {
                List<Field> fields = getAllFields(originalEntity.getClass());
                List<String> changedFields = new ArrayList<>();
                
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object originalValue = field.get(originalEntity);
                    Object updatedValue = field.get(updatedEntity);
                    
                    if (!Objects.equals(originalValue, updatedValue)) {
                        changedFields.add(field.getName() + ": " + originalValue + " -> " + updatedValue);
                    }
                }
                
                if (!changedFields.isEmpty()) {
                    log.debug("Changed fields: {}", changedFields);
                } else {
                    log.debug("No fields changed");
                }
            } catch (Exception e) {
                log.debug("Error comparing entities: {}", e.getMessage());
            }
        }
        log.debug("==================");
    }

    /**
     * Check if two entities are equal based on their ID fields
     */
    public static boolean entitiesHaveSameId(Object entity1, Object entity2) {
        if (entity1 == null || entity2 == null) {
            return false;
        }
        
        if (!entity1.getClass().equals(entity2.getClass())) {
            return false;
        }
        
        try {
            Object id1 = extractEntityId(entity1);
            Object id2 = extractEntityId(entity2);
            
            return Objects.equals(id1, id2);
        } catch (Exception e) {
            log.error("Error comparing entity IDs: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get human-readable field names for error messages
     */
    public static List<String> getFieldNames(Class<?> entityClass) {
        return getAllFields(entityClass).stream()
                .map(Field::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Validate field types in the input map against entity class
     */
    public static Map<String, String> validateFieldTypes(Class<?> entityClass, Map<String, Object> fields) {
        Map<String, String> errors = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue; // Null values are generally acceptable
            }
            
            Field field = findField(entityClass, fieldName);
            if (field == null) {
                errors.put(fieldName, "Field does not exist in entity " + entityClass.getSimpleName());
                continue;
            }
            
            try {
                // Try to convert the value to see if it's compatible
                convertValueToFieldType(value, field.getType());
            } catch (Exception e) {
                errors.put(fieldName, "Cannot convert value '" + value + "' to type " + field.getType().getSimpleName() + ": " + e.getMessage());
            }
        }
        
        return errors;
    }
    
    /**
     * Convert value to the appropriate field type
     * Handles all common field types including enums, dates, primitives, and byte arrays
     * Enhanced to handle SQL Server datetime formats with timezone offsets
     */
    private static Object convertValueToFieldType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If already the correct type, return as-is
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        try {
            // Handle Date to LocalDateTime/LocalDate conversions
            if (value instanceof java.util.Date) {
                java.util.Date dateValue = (java.util.Date) value;
                if (targetType.equals(LocalDateTime.class)) {
                    return LocalDateTime.ofInstant(dateValue.toInstant(), java.time.ZoneId.systemDefault());
                } else if (targetType.equals(LocalDate.class)) {
                    return LocalDateTime.ofInstant(dateValue.toInstant(), java.time.ZoneId.systemDefault()).toLocalDate();
                } else if (targetType.equals(OffsetDateTime.class)) {
                    return OffsetDateTime.ofInstant(dateValue.toInstant(), java.time.ZoneId.systemDefault());
                } else if (targetType.equals(ZonedDateTime.class)) {
                    return ZonedDateTime.ofInstant(dateValue.toInstant(), java.time.ZoneId.systemDefault());
                }
            }
            
            // Handle enum types
            if (targetType.isEnum()) {
                String stringValue = value.toString();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object enumValue = Enum.valueOf((Class<Enum>) targetType, stringValue);
                return enumValue;
            }

            // Handle string conversion
            if (targetType.equals(String.class)) {
                return value.toString();
            }
            
            // Handle byte array conversion
            if (targetType.equals(byte[].class)) {
                // If the value is already a byte array, return it
                if (value instanceof byte[]) {
                    return value;
                }
                // If the value is a String, convert it to a byte array using UTF-8 encoding
                if (value instanceof String) {
                    return ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                // If value is Base64 encoded string, decode it
                if (value instanceof String && ((String) value).matches("^[A-Za-z0-9+/=]+$")) {
                    try {
                        return java.util.Base64.getDecoder().decode((String) value);
                    } catch (IllegalArgumentException e) {
                        // Not valid Base64, fall through to default conversion
                    }
                }
            }

            String stringValue = value.toString().trim();

            // Handle numeric types
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return Boolean.parseBoolean(stringValue);
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(stringValue);
            }

            // Handle date types with enhanced SQL Server support
            if (targetType.equals(LocalDate.class)) {
                return parseLocalDate(stringValue);
            } else if (targetType.equals(LocalDateTime.class)) {
                return parseLocalDateTime(stringValue);
            } else if (targetType.equals(OffsetDateTime.class)) {
                return parseOffsetDateTime(stringValue);
            } else if (targetType.equals(ZonedDateTime.class)) {
                return parseZonedDateTime(stringValue);
            }

            log.warn("Unsupported field type conversion: {} -> {}", value.getClass().getSimpleName(), targetType.getSimpleName());
            return value;

        } catch (Exception e) {
            log.error("Error converting value '{}' to type {}: {}", value, targetType.getSimpleName(), e.getMessage());
            throw new RuntimeException("Type conversion failed", e);
        }
    }
    
    /**
     * Parse LocalDate with multiple format support
     */
    private static LocalDate parseLocalDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        // Try parsing with different formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME ||
                    formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME ||
                    formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                    // For datetime strings, extract just the date part
                    if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                        OffsetDateTime dateTime = OffsetDateTime.parse(dateString, formatter);
                        return dateTime.toLocalDate();
                    } else if (formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                        ZonedDateTime dateTime = ZonedDateTime.parse(dateString, formatter);
                        return dateTime.toLocalDate();
                    } else {
                        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                        return dateTime.toLocalDate();
                    }
                } else {
                    return LocalDate.parse(dateString, formatter);
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new RuntimeException("Unable to parse date: " + dateString);
    }
    
    /**
     * Parse LocalDateTime with multiple format support and SQL Server compatibility
     */
    private static LocalDateTime parseLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        // Check if it's a Unix timestamp (number with optional decimal)
        if (dateTimeString.matches("^\\d+(\\.\\d+)?$")) {
            try {
                // Handle Unix timestamps
                if (dateTimeString.contains(".")) {
                    // Timestamp with fractional seconds
                    double timestampDouble = Double.parseDouble(dateTimeString);
                    long seconds = (long) timestampDouble;
                    int nanos = (int) ((timestampDouble - seconds) * 1_000_000_000);
                    return LocalDateTime.ofEpochSecond(seconds, nanos, java.time.ZoneOffset.UTC);
                } else {
                    // Simple Unix timestamp
                    long timestamp = Long.parseLong(dateTimeString);
                    // If timestamp is more than 10 digits, it's likely milliseconds
                    if (dateTimeString.length() > 10) {
                        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneOffset.UTC);
                    } else {
                        return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC);
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("Failed to parse as Unix timestamp: {}", dateTimeString);
            }
        }

        // If only date is provided, add default time
        if (dateTimeString.length() <= 10) {
            LocalDate date = parseLocalDate(dateTimeString);
            return date != null ? date.atStartOfDay() : null;
        }

        // Handle SQL Server datetime format with timezone offset
        if (SQL_SERVER_DATETIME_PATTERN.matcher(dateTimeString).matches()) {
            try {
                // Remove timezone offset for LocalDateTime parsing
                String cleanedString = dateTimeString.replaceAll("([+-]\\d{4})$", "");
                
                // Handle microseconds (SQL Server can have up to 7 decimal places)
                if (cleanedString.contains(".")) {
                    int dotIndex = cleanedString.indexOf('.');
                    String beforeDot = cleanedString.substring(0, dotIndex + 1);
                    String afterDot = cleanedString.substring(dotIndex + 1);
                    
                    // Truncate to 6 digits for nanoseconds (LocalDateTime max precision)
                    if (afterDot.length() > 6) {
                        afterDot = afterDot.substring(0, 6);
                    }
                    // Pad with zeros if less than 6 digits
                    while (afterDot.length() < 6) {
                        afterDot += "0";
                    }
                    
                    cleanedString = beforeDot + afterDot;
                }
                
                log.debug("Cleaned SQL Server datetime string: '{}' -> '{}'", dateTimeString, cleanedString);
                
                // Try parsing the cleaned string
                DateTimeFormatter sqlServerFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                return LocalDateTime.parse(cleanedString, sqlServerFormatter);
                
            } catch (Exception e) {
                log.debug("Failed to parse SQL Server format, trying other formats: {}", e.getMessage());
                // Fall through to try other formatters
            }
        }

        // Try parsing with different formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    // Skip date-only formatter for datetime parsing
                    continue;
                } else if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    // Parse as OffsetDateTime and convert to LocalDateTime
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString, formatter);
                    return offsetDateTime.toLocalDateTime();
                } else if (formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                    // Parse as ZonedDateTime and convert to LocalDateTime
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, formatter);
                    return zonedDateTime.toLocalDateTime();
                } else {
                    return LocalDateTime.parse(dateTimeString, formatter);
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new RuntimeException("Unable to parse datetime: " + dateTimeString);
    }
    
    /**
     * Parse OffsetDateTime with multiple format support
     */
    private static OffsetDateTime parseOffsetDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        // Try parsing with different formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    return OffsetDateTime.parse(dateTimeString, formatter);
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If no specific offset format works, try to parse as LocalDateTime and add system offset
        try {
            LocalDateTime localDateTime = parseLocalDateTime(dateTimeString);
            if (localDateTime != null) {
                return localDateTime.atOffset(java.time.ZoneOffset.systemDefault().getRules().getOffset(localDateTime));
            }
        } catch (Exception e) {
            // Ignore and throw error below
        }

        throw new RuntimeException("Unable to parse offset datetime: " + dateTimeString);
    }
    
    /**
     * Parse ZonedDateTime with multiple format support
     */
    private static ZonedDateTime parseZonedDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        // Try parsing with different formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                    return ZonedDateTime.parse(dateTimeString, formatter);
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If no specific zoned format works, try to parse as LocalDateTime and add system zone
        try {
            LocalDateTime localDateTime = parseLocalDateTime(dateTimeString);
            if (localDateTime != null) {
                return localDateTime.atZone(java.time.ZoneId.systemDefault());
            }
        } catch (Exception e) {
            // Ignore and throw error below
        }

        throw new RuntimeException("Unable to parse zoned datetime: " + dateTimeString);
    }
}
