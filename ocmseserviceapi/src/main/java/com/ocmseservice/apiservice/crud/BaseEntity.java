package com.ocmseservice.apiservice.crud;

import com.ocmseservice.apiservice.crud.annotations.NonEditable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base entity with audit fields
 * This class is intended to be extended by all entities in the application
 */
@MappedSuperclass
@Data
public abstract class BaseEntity {

    @NonEditable
    @Column(name = "cre_date")
    private LocalDateTime creDate;
    
    @NonEditable
    // @NonNull
    @Column(name = "cre_user_id", nullable = false, length = 50)
    private String creUserId;
    
    @Column(name = "upd_date")
    private LocalDateTime updDate;
    
    @Column(name = "upd_user_id", length = 50)
    private String updUserId;
    
    /**
     * Set creation audit fields before persisting
     */
    @PrePersist
    protected void onCreate() {
        creDate = LocalDateTime.now();
        if (creUserId == null || creUserId.trim().isEmpty()) {
            creUserId = "SYSTEM";
        }
//         if (creUserId == null) {
//             creUserId = "SYSTEM";
//         }
    }
    
    /**
     * Set update audit fields before updating
     * Also ensure creDate is set to avoid NULL constraint violations
     */
    @PreUpdate
    protected void onUpdate() {
        // Set update date and user
        updDate = LocalDateTime.now();
        if (updUserId == null) {
            updUserId = "SYSTEM";
        }
        
        // Ensure creDate is set even during updates
        // This prevents NULL constraint violations when updating existing entities
        if (creDate == null) {
            creDate = LocalDateTime.now();
        }
        
         //Ensure creUserId is set
         if (creUserId == null) {
             creUserId = "SYSTEM";
         }
    }
}