package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing the ocms_template_store table.
 * Stores templates for SMS and Email notifications in the database.
 * Templates contain placeholders (e.g., <Name>, <noticeno>) that are replaced with actual values at runtime.
 * Supports versioning - multiple versions of the same template can exist, with one marked as active.
 */
@Entity
@Table(name = "ocms_template_store", schema = "ocmsizmgr")
@IdClass(OcmsTemplateStoreId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcmsTemplateStore {

    @Id
    @Column(name = "template_name", length = 64, nullable = false)
    private String templateName;

    @Id
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "template_type", length = 32)
    private String templateType;

    /**
     * Template content containing placeholders that will be replaced with actual values.
     * For SMS: Plain text with placeholders (e.g., "Dear <Name>, you have...")
     * For Email: Subject and body separated by ---SUBJECT--- and ---BODY--- delimiters
     */
    @Lob
    @Column(name = "template_content")
    private String templateContent;

    // Audit fields
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
