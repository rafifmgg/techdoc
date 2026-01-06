package com.ocmseservice.apiservice.crud.ocmsezdb.eocmstemplatestore;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eocms_template_store", schema = "ocmsezmgr")
@Getter
@Setter
public class EocmsTemplateStore extends BaseEntity {
    @Id
    @Column(name = "template_name", length = 64, nullable = false)
    private String templateName;
    
    @Lob
    @Column(name = "bin_data")
    private byte[] binData;
    
    @Column(name = "remarks", length = 50)
    private String remarks;
    
    @Column(name = "template_type", length = 32)
    private String templateType;
}
