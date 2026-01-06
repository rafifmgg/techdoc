package com.ocmsintranet.cronservice.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * Base repository interface with common CRUD operations
 * This interface is intended to be extended by all repositories in the application
 * 
 * @param <E> Entity type
 * @param <ID> Entity ID type
 */
@NoRepositoryBean
public interface BaseRepository<E, ID extends Serializable> 
    extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {
    
    // The base interfaces provide all the necessary methods
    // Custom methods can be added in specific repository implementations
}