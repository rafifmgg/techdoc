package com.ocmsintranet.cronservice.crud;

import com.ocmsintranet.cronservice.crud.annotations.NonEditable;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
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
    @Column(name = "cre_date", nullable = false, updatable = false)
    private LocalDateTime creDate;
    
    @NonEditable
    @Column(name = "cre_user_id", nullable = false, length = 50, updatable = false)
    private String creUserId;
    
    @NonEditable
    @Column(name = "upd_date", nullable = true)
    private LocalDateTime updDate;
    
    @NonEditable
    @Column(name = "upd_user_id", nullable = true, length = 50)
    private String updUserId;
    
    /**
     * Set creation audit fields before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (creDate == null) {
            creDate = LocalDateTime.now();
        }
        if (creUserId == null) {
            creUserId = SystemConstant.User.DEFAULT_SYSTEM_USER_ID;
        }
    }
    
    /**
     * Set update audit fields before updating
     */
    @PreUpdate
    protected void onUpdate() {
        if (updDate == null) {
            updDate = LocalDateTime.now();
        }
        if (updUserId == null) {
            updUserId = SystemConstant.User.DEFAULT_SYSTEM_USER_ID;
        }
    }
}
