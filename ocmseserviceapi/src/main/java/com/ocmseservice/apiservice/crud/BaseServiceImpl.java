package com.ocmseservice.apiservice.crud;

import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseServiceImpl<E, ID extends Serializable> implements BaseService<E, ID> {

    protected final JpaRepository<E, ID> repository;

    public BaseServiceImpl(JpaRepository<E, ID> repository) {
        this.repository = repository;
    }

    @Override
    public E save(E entity) {
        return repository.save(entity);
    }

    @Override
    public List<E> saveAll(Iterable<E> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public Optional<E> getById(ID id) {
        return repository.findById(id);
    }

    @Override
    public FindAllResponse<E> getAll(Map<String, String[]> params) {
        // Extract pagination parameters
        int limit = getIntParam(params, "$limit", 10);
        int skip = getIntParam(params, "$skip", 0);
        
        // Extract sorting parameters
        String[] sortParams = params.getOrDefault("$sort", new String[]{});
        List<Order> orders = new ArrayList<>();
        for (String sortParam : sortParams) {
            String[] parts = sortParam.split(":");
            String field = parts[0];
            Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? 
                Direction.DESC : Direction.ASC;
            orders.add(new Order(direction, field));
        }
        
        // Create pageable object
        PageRequest pageRequest = PageRequest.of(skip / limit, limit, 
            orders.isEmpty() ? Sort.unsorted() : Sort.by(orders));
        
        // Get total count and data
        Page<E> page = repository.findAll(pageRequest);
        
        // Create response
        FindAllResponse<E> response = new FindAllResponse<>();
        response.setTotal(page.getTotalElements());
        response.setData(page.getContent());
        
        return response;
    }

    @Override
    public E update(ID id, E entity) {
        if (repository.existsById(id)) {
            return repository.save(entity);
        }
        throw new RuntimeException("Entity not found with id: " + id);
    }

    @Override
    public E patch(ID id, E partialEntity) {
        Optional<E> existingEntity = repository.findById(id);
        if (existingEntity.isPresent()) {
            // For now, treat patch same as update
            // In a real implementation, you would merge the partial entity with the existing one
            return repository.save(partialEntity);
        }
        throw new RuntimeException("Entity not found with id: " + id);
    }

    @Override
    public boolean delete(ID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    private int getIntParam(Map<String, String[]> params, String key, int defaultValue) {
        String[] values = params.get(key);
        if (values != null && values.length > 0) {
            try {
                return Integer.parseInt(values[0]);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public List<String> getStringFieldAll(String fieldName, Map<String, String[]> params) {
        // Get all entities and extract distinct values for the specified field
        try {
            List<E> allEntities = repository.findAll();
            return allEntities.stream()
                .map(entity -> {
                    try {
                        // Use reflection to get the field value
                        java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        return value != null ? value.toString() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error and return an empty list
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
