package com.ocmsintranet.cronservice.crud;

import com.ocmsintranet.cronservice.crud.beans.FindAllResponse;
import com.ocmsintranet.cronservice.crud.annotations.NonEditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

/**
 * Base implementation for services with common CRUD and query functionality
 * 
 * @param <E> Entity type
 * @param <K> Primary key type (can be a composite key class)
 * @param <R> Repository type
 */
public abstract class BaseImplement<E, K extends Serializable, R extends JpaRepository<E, K> & JpaSpecificationExecutor<E>> 
        implements BaseService<E, K> {

    protected final R repository;
    protected final DatabaseRetryService retryService;
    
    @PersistenceContext
    protected EntityManager entityManager;

    private final Class<E> entityClass;
    
    // Query operators
    private static final String REGEX_OPERATOR = "$regex";
    private static final String IN_OPERATOR = "$in";
    private static final String GREATER_THAN_EQUAL_OPERATOR = "$gte";
    private static final String LESS_THAN_EQUAL_OPERATOR = "$lte";
    private static final String BETWEEN_OPERATOR = "$between";
    private static final String NULL_OPERATOR = "$null";
    private static final String NOT_EQUAL_OPERATOR = "$ne";
    private static final String CONTAINS_OPERATOR = "$contains";
    private static final String STARTS_WITH_OPERATOR = "$startsWith";
    private static final String ENDS_WITH_OPERATOR = "$endsWith";
    private static final String BEFORE_OPERATOR = "$before";
    private static final String AFTER_OPERATOR = "$after";
    private static final String CASE_INSENSITIVE_OPERATOR = "$caseInsensitive";
    
    // Special parameter names
    private static final String SORT_PARAM = "$sort";
    private static final String ORDER_PARAM = "$order";
    private static final String LIMIT_PARAM = "$limit";
    private static final String SKIP_PARAM = "$skip";
    private static final String FIELDS_PARAM = "$fields";
    private static final String SEARCH_PARAM = "$q";
    
    // Default pagination values
    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_SKIP = 0;
    
    // Maximum limit to prevent performance issues
    private static final int MAX_LIMIT = 1000;

    @SuppressWarnings("unchecked")
    public BaseImplement(R repository, DatabaseRetryService retryService) {
        this.repository = repository;
        this.retryService = retryService;
        
        // Get entity type from generic parameters
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        this.entityClass = (Class<E>) genericSuperclass.getActualTypeArguments()[0];
    }

    /**
     * Create a new entity
     */
    @Override
    @Transactional
    public E save(E entity) {
        return retryService.executeWithRetry(() -> repository.save(entity));
    }

    /**
     * Save multiple entities
     */
    @Override
    @Transactional
    public List<E> saveAll(Iterable<E> entities) {
        return retryService.executeWithRetry(() -> repository.saveAll(entities));
    }

    /**
     * Update an entity completely
     */
    @Override
    @Transactional
    public E update(K id, E entity) {
        return retryService.executeWithRetry(() -> {
            if (!repository.existsById(id)) {
                throw new RuntimeException("Entity not found with id: " + id);
            }
            
            E existingEntity = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
            
            // Check non-editable fields and prevent their modification
            validateNonEditableFields(existingEntity, entity);
            
            // Transfer ID from existing entity to new entity for update
            transferIdFields(existingEntity, entity);
            
            return repository.save(entity);
        });
    }

    /**
     * Partially update an entity (patch)
     */
    @Override
    @Transactional
    public E patch(K id, E partialEntity) {
        return retryService.executeWithRetry(() -> {
            E existingEntity = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
            
            // Check non-editable fields
            validateNonEditableFields(existingEntity, partialEntity);
            
            // Update only non-null fields
            updateNonNullFields(existingEntity, partialEntity);
            
            // Make sure ID is preserved
            transferIdFields(existingEntity, existingEntity);
            
            return repository.save(existingEntity);
        });
    }

    /**
     * Get entity by ID
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<E> getById(K id) {
        return retryService.executeWithRetry(() -> repository.findById(id));
    }

    /**
     * Delete entity by ID
     */
    @Override
    @Transactional
    public boolean delete(K id) {
        return retryService.executeWithRetry(() -> {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                return true;
            }
            return false;
        });
    }

   /**
     * Get all entities with advanced filtering, sorting, and pagination
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public FindAllResponse<E> getAll(Map<String, String[]> params) {
        return retryService.executeWithRetry(() -> {
            // Parse pagination parameters
            int limit = parseLimit(params);
            int skip = parseSkip(params);
            
            // Create pageable with sort
            Pageable pageable = createPageable(params, skip, limit);
            
            // Create specifications for filtering
            Specification<E> spec = createSpecification(params);
            
            // Get field selection if any
            String[] fields = params.getOrDefault(FIELDS_PARAM, new String[0]);
            
            // Execute query
            Page<E> page = repository.findAll(spec, pageable);
            
            // No field filtering - return all fields
            if (fields.length == 0) {
                List<E> entities = page.getContent();
                
                return new FindAllResponse<>(page.getTotalElements(), limit, skip, entities);
            }
            
            // With field filtering - create map-based objects with only requested fields
            Set<String> includedFields = new HashSet<>(Arrays.asList(fields));
            
            // Create a list of maps containing only the requested fields
            List<Map<String, Object>> filteredItems = page.getContent().stream()
                .map(entity -> {
                    Map<String, Object> filteredMap = new HashMap<>();
                    
                    for (String fieldName : includedFields) {
                        try {
                            Field field = findField(entityClass, fieldName);
                            if (field != null) {
                                field.setAccessible(true);
                                Object value = field.get(entity);
                                filteredMap.put(fieldName, value);
                            }
                        } catch (Exception e) {
                            // Ignore field if it can't be accessed
                        }
                    }
                    
                    return filteredMap;
                })
                .collect(Collectors.toList());
            
            // Cast is safe since maps will be serialized to JSON with the same structure
            return new FindAllResponse<>(
                page.getTotalElements(), 
                limit, 
                skip, 
                (List<E>)(List<?>)filteredItems
            );
        });
    }
        
    /**
     * Get all values for a specific field with filtering
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getStringFieldAll(String fieldName, Map<String, String[]> params) {
        return retryService.executeWithRetry(() -> {
            // Create specifications for filtering
            Specification<E> spec = createSpecification(params);
            
            // Execute query with distinct field selection
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<String> query = cb.createQuery(String.class);
            Root<E> root = query.from(entityClass);
            
            // Select the requested field
            Path<String> fieldPath = getNestedPath(root, fieldName);
            query.select(fieldPath).distinct(true);
            
            // Apply specification if present
            if (spec != null) {
                query.where(spec.toPredicate(root, query, cb));
            }
            
            // Execute query
            return entityManager.createQuery(query).getResultList();
        });
    }

    /**
     * Create specification for filtering based on query parameters
     */
    protected Specification<E> createSpecification(Map<String, String[]> params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            params.forEach((key, values) -> {
                // Skip special parameters
                if (isSpecialParameter(key)) {
                    return;
                }
                
                // Handle search parameter
                if (key.equals(SEARCH_PARAM) && values.length > 0) {
                    predicates.add(createSearchPredicate(root, query, criteriaBuilder, values[0]));
                    return;
                }
                
                // Regular field filtering
                if (values.length > 0) {
                    String value = values[0];
                    addFieldPredicate(key, value, root, criteriaBuilder, predicates);
                }
            });
            
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Add predicate for field filtering based on operator
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addFieldPredicate(String key, String value, Root<E> root, CriteriaBuilder cb, List<Predicate> predicates) {
        // Check if key contains an operator
        if (key.contains("[") && key.contains("]")) {
            String fieldName = key.substring(0, key.indexOf("["));
            String operator = key.substring(key.indexOf("[") + 1, key.indexOf("]"));
            Path path = getNestedPath(root, fieldName);
            
            switch (operator) {
                case REGEX_OPERATOR:
                    // First, decode any URL-encoded characters in the pattern
                    String decodedValue;
                    try {
                        decodedValue = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        decodedValue = value; // Fallback if decoding fails
                    }
                    
                    // Handle different regex patterns
                    if (decodedValue.startsWith("^") && decodedValue.endsWith("$")) {
                        // Exact match pattern (^pattern$)
                        String exactPattern = decodedValue.substring(1, decodedValue.length() - 1)
                                            .replace("\\s", " ")
                                            .replace(".*", "%")
                                            .replace(".", "_");
                        predicates.add(cb.equal(path.as(String.class), exactPattern));
                    } else if (decodedValue.startsWith("^")) {
                        // Starts with pattern (^pattern)
                        String startsWithPattern = decodedValue.substring(1)
                                                .replace("\\s", " ")
                                                .replace(".*", "%")
                                                .replace(".", "_");
                        predicates.add(cb.like(path.as(String.class), startsWithPattern + "%"));
                    } else if (decodedValue.endsWith("$")) {
                        // Ends with pattern (pattern$)
                        String endsWithPattern = decodedValue.substring(0, decodedValue.length() - 1)
                                                .replace("\\s", " ")
                                                .replace(".*", "%")
                                                .replace(".", "_");
                        predicates.add(cb.like(path.as(String.class), "%" + endsWithPattern));
                    } else {
                        // Contains pattern (pattern)
                        String containsPattern = decodedValue.replace("\\s", " ")
                                                .replace(".*", "%")
                                                .replace(".", "_");
                        predicates.add(cb.like(path.as(String.class), "%" + containsPattern + "%"));
                    }
                    break;
                case IN_OPERATOR:
                    predicates.add(path.in((Object[]) value.split(",")));
                    break;
                case GREATER_THAN_EQUAL_OPERATOR:
                    addComparisonPredicate(path, value, cb, predicates, (p, v) -> cb.greaterThanOrEqualTo(p, v));
                    break;
                case LESS_THAN_EQUAL_OPERATOR:
                    addComparisonPredicate(path, value, cb, predicates, (p, v) -> cb.lessThanOrEqualTo(p, v));
                    break;
                case BETWEEN_OPERATOR:
                    addBetweenPredicate(path, value, cb, predicates);
                    break;
                case NULL_OPERATOR:
                    if (Boolean.parseBoolean(value)) {
                        predicates.add(cb.isNull(path));
                    } else {
                        predicates.add(cb.isNotNull(path));
                    }
                    break;
                case NOT_EQUAL_OPERATOR:
                    predicates.add(cb.or(cb.notEqual(path, value), cb.isNull(path)));
                    break;
                case CONTAINS_OPERATOR:
                    predicates.add(cb.like(path.as(String.class), "%" + value + "%"));
                    break;
                case STARTS_WITH_OPERATOR:
                    predicates.add(cb.like(path.as(String.class), value + "%"));
                    break;
                case ENDS_WITH_OPERATOR:
                    predicates.add(cb.like(path.as(String.class), "%" + value));
                    break;
                case BEFORE_OPERATOR:
                    addDateComparisonPredicate(path, value, cb, predicates, (p, v) -> cb.lessThan(p, v));
                    break;
                case AFTER_OPERATOR:
                    addDateComparisonPredicate(path, value, cb, predicates, (p, v) -> cb.greaterThan(p, v));
                    break;
                case CASE_INSENSITIVE_OPERATOR:
                    predicates.add(cb.equal(cb.lower(path.as(String.class)), value.toLowerCase()));
                    break;
                default:
                    // Unknown operator, ignore
            }
        } else {
            // Simple equality
            Path path = getNestedPath(root, key);
            
            // Handle empty string case
            if (value.isEmpty()) {
                predicates.add(cb.or(cb.equal(path, ""), cb.isNull(path)));
            } else {
                Object typedValue = convertValueToFieldType(path, value);
                predicates.add(cb.equal(path, typedValue));
            }
        }
    }

    /**
     * Add between predicate for range queries
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addBetweenPredicate(Path path, String value, CriteriaBuilder cb, List<Predicate> predicates) {
        try {
            // Parse between values (format should be [min,max])
            if (value.startsWith("[") && value.endsWith("]")) {
                String[] range = value.substring(1, value.length() - 1).split(",");
                if (range.length == 2) {
                    Class<?> fieldType = path.getJavaType();
                    
                    if (Number.class.isAssignableFrom(fieldType) || fieldType.equals(LocalDate.class) 
                            || fieldType.equals(LocalDateTime.class)) {
                        Object min = convertToFieldType(range[0], fieldType);
                        Object max = convertToFieldType(range[1], fieldType);
                        
                        if (fieldType.equals(LocalDate.class)) {
                            predicates.add(cb.between(path.as(LocalDate.class), (LocalDate) min, (LocalDate) max));
                        } else if (fieldType.equals(LocalDateTime.class)) {
                            predicates.add(cb.between(path.as(LocalDateTime.class), (LocalDateTime) min, (LocalDateTime) max));
                        } else {
                            // Handle numeric types
                            predicates.add(cb.between(path, (Comparable) min, (Comparable) max));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore invalid format
        }
    }

    /**
     * Add comparison predicate with type conversion
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addComparisonPredicate(Path path, String value, CriteriaBuilder cb, List<Predicate> predicates,
                                       BiFunction<Path, Comparable, Predicate> operation) {
        try {
            Class<?> fieldType = path.getJavaType();
            Object typedValue = convertToFieldType(value, fieldType);
            
            if (typedValue instanceof Comparable) {
                predicates.add(operation.apply(path, (Comparable) typedValue));
            }
        } catch (Exception e) {
            // Ignore invalid format
        }
    }

    /**
     * Add date-specific comparison predicate
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addDateComparisonPredicate(Path path, String value, CriteriaBuilder cb, List<Predicate> predicates,
                                          BiFunction<Path, Comparable, Predicate> operation) {
        try {
            Class<?> fieldType = path.getJavaType();
            
            if (fieldType.equals(LocalDate.class)) {
                LocalDate date = LocalDate.parse(value);
                predicates.add(operation.apply(path, date));
            } else if (fieldType.equals(LocalDateTime.class)) {
                LocalDateTime dateTime;
                if (value.length() <= 10) {
                    // If only date is provided, use start/end of day based on operation
                    LocalDate date = LocalDate.parse(value);
                    if (operation.apply(path, LocalDateTime.MIN).equals(cb.lessThan(path, LocalDateTime.MIN))) {
                        // It's a "before" operation, use end of day
                        dateTime = date.atTime(23, 59, 59);
                    } else {
                        // It's an "after" operation, use start of day
                        dateTime = date.atStartOfDay();
                    }
                } else {
                    dateTime = LocalDateTime.parse(value);
                }
                predicates.add(operation.apply(path, dateTime));
            }
        } catch (Exception e) {
            // Ignore invalid format
        }
    }

    /**
     * Create full-text search predicate across all searchable fields
     */
    private Predicate createSearchPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb, String searchTerm) {
        List<Predicate> searchPredicates = new ArrayList<>();
        
        // Get all string fields from entity
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getType().equals(String.class)) {
                try {
                    Path<String> path = root.get(field.getName());
                    searchPredicates.add(cb.like(cb.lower(path), "%" + searchTerm.toLowerCase() + "%"));
                } catch (Exception e) {
                    // Ignore if field doesn't exist in entity
                }
            }
        }
        
        return cb.or(searchPredicates.toArray(new Predicate[0]));
    }

    /**
     * Create pageable object with sorting
     */
    private Pageable createPageable(Map<String, String[]> params, int skip, int limit) {
        Sort sort = createSort(params);
        
        // Calculate the page number from skip and limit
        int page = limit > 0 ? skip / limit : 0;
        
        return PageRequest.of(page, limit, sort);
    }

    /**
     * Create sort object from parameters
     */
    private Sort createSort(Map<String, String[]> params) {
        List<Sort.Order> orders = new ArrayList<>();
        
        // Handle regular sort parameters
        if (params.containsKey(SORT_PARAM) || params.containsKey(SORT_PARAM + "[")) {
            // Extract sort fields and directions
            params.forEach((key, values) -> {
                if (key.startsWith(SORT_PARAM + "[") && key.endsWith("]") && values.length > 0) {
                    String field = key.substring((SORT_PARAM + "[").length(), key.length() - 1);
                    int direction = Integer.parseInt(values[0]);
                    
                    if (direction >= 0) {
                        orders.add(Sort.Order.asc(field));
                    } else {
                        orders.add(Sort.Order.desc(field));
                    }
                } else if (key.equals(SORT_PARAM) && values.length > 0) {
                    // Handle comma-separated sort fields with direction (field:asc,anotherField:desc)
                    String[] sortFields = values[0].split(",");
                    for (String field : sortFields) {
                        String[] parts = field.split(":");
                        if (parts.length == 2) {
                            if ("desc".equalsIgnoreCase(parts[1])) {
                                orders.add(Sort.Order.desc(parts[0]));
                            } else {
                                orders.add(Sort.Order.asc(parts[0]));
                            }
                        } else if (parts.length == 1) {
                            orders.add(Sort.Order.asc(parts[0]));
                        }
                    }
                }
            });
        }
        
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * Parse limit parameter with validation
     */
    private int parseLimit(Map<String, String[]> params) {
        if (params.containsKey(LIMIT_PARAM) && params.get(LIMIT_PARAM).length > 0) {
            try {
                int limit = Integer.parseInt(params.get(LIMIT_PARAM)[0]);
                if (limit > 0) {
                    return Math.min(limit, MAX_LIMIT);
                }
            } catch (NumberFormatException e) {
                // Use default if invalid
            }
        }
        return DEFAULT_LIMIT;
    }

    /**
     * Parse skip parameter with validation
     */
    private int parseSkip(Map<String, String[]> params) {
        if (params.containsKey(SKIP_PARAM) && params.get(SKIP_PARAM).length > 0) {
            try {
                int skip = Integer.parseInt(params.get(SKIP_PARAM)[0]);
                if (skip >= 0) {
                    return skip;
                }
            } catch (NumberFormatException e) {
                // Use default if invalid
            }
        }
        return DEFAULT_SKIP;
    }

    /**
     * Check if parameter is a special parameter (not a field filter)
     */
    private boolean isSpecialParameter(String param) {
        return param.equals(LIMIT_PARAM) || param.equals(SKIP_PARAM) || 
               param.equals(FIELDS_PARAM) || param.startsWith(SORT_PARAM) || 
               param.startsWith(ORDER_PARAM) || param.equals(SEARCH_PARAM);
    }

    /**
     * Convert string value to appropriate field type
     */
    private Object convertValueToFieldType(Path<?> path, String value) {
        Class<?> targetType = path.getJavaType();
        return convertToFieldType(value, targetType);
    }

    /**
     * Convert string value to specified type
     */
    private Object convertToFieldType(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        if (targetType.equals(String.class)) {
            return value;
        } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.parseLong(value);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.parseDouble(value);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (targetType.equals(LocalDate.class)) {
            return LocalDate.parse(value);
        } else if (targetType.equals(LocalDateTime.class)) {
            if (value.length() <= 10) {
                // If only date part is provided
                return LocalDate.parse(value).atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } else if (targetType.equals(BigDecimal.class)) {
            return new BigDecimal(value);
        }
        
        // Default to string if type is not supported
        return value;
    }

    /**
     * Get path to nested field (supports dot notation like "address.city")
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Path getNestedPath(Root<E> root, String fieldName) {
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            Path path = root.get(parts[0]);
            
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            
            return path;
        } else {
            return root.get(fieldName);
        }
    }

    /**
     * Helper method to find a field in the class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) {
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
     * Update non-null fields from source to target
     */
    private void updateNonNullFields(E target, E source) {
        try {
            // Get all fields in the class hierarchy
            List<Field> allFields = new ArrayList<>();
            Class<?> currentClass = entityClass;
            
            while (currentClass != null && currentClass != Object.class) {
                allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                currentClass = currentClass.getSuperclass();
            }
            
            for (Field field : allFields) {
                field.setAccessible(true);
                
                // Skip @NonEditable fields to avoid overwriting audit fields with NULL
                if (field.isAnnotationPresent(NonEditable.class)) {
                    continue;
                }
                
                Object value = field.get(source);
                
                if (value != null) {
                    field.set(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating fields", e);
        }
    }

    /**
     * Transfer ID fields from source entity to target entity (FIXED for composite keys)
     */
    private void transferIdFields(E source, E target) {
        try {
            // Check if entity has composite key
            IdClass idClassAnnotation = entityClass.getAnnotation(IdClass.class);
            if (idClassAnnotation != null) {
                // Handle composite key - transfer ALL @Id fields
                for (Field field : entityClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        Object value = field.get(source);
                        field.set(target, value);
                    }
                }
            } else {
                // Handle simple key
                for (Field field : entityClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Id.class)) {
                        Object value = field.get(source);
                        field.set(target, value);
                        break; // Only one @Id field for simple keys
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error transferring ID fields", e);
        }
    }

    /**
     * Validate that non-editable fields aren't being modified
     */
    private void validateNonEditableFields(E existingEntity, E newEntity) {
        try {
            // Get all fields in the class hierarchy
            List<Field> allFields = new ArrayList<>();
            Class<?> currentClass = entityClass;
            
            while (currentClass != null && currentClass != Object.class) {
                allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                currentClass = currentClass.getSuperclass();
            }
            
            // Check each field
            for (Field field : allFields) {
                field.setAccessible(true);
                
                // Check if field is marked as non-editable
                if (field.isAnnotationPresent(NonEditable.class)) {
                    // Get values
                    Object existingValue = field.get(existingEntity);
                    Object newValue = field.get(newEntity);
                    
                    // If new value is non-null and different from existing value, throw exception
                    if (newValue != null && !Objects.equals(existingValue, newValue)) {
                        throw new RuntimeException("Field '" + field.getName() + "' is marked as non-editable and cannot be modified");
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error validating non-editable fields", e);
        }
    }

    /**
     * Get the entity class type
     * 
     * @return The entity class
     */
    public Class<E> getEntityClass() {
        return this.entityClass;
    }
}
