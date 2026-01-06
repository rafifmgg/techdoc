package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsTemplateStore;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_template_store table.
 * Stores metadata about templates stored in Azure Blob Storage.
 */
@Entity
@Table(name = "ocms_template_store", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsTemplateStore extends BaseEntity {

    @Id
    @Column(name = "template_name", length = 64, nullable = false)
    private String templateName;

    @Column(name = "remarks", length = 50)
    private String remarks;

    @Column(name = "template_type", length = 32)
    private String templateType;
}
