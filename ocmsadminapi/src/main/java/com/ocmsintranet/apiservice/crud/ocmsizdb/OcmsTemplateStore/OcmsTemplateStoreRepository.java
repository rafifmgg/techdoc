package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsTemplateStore;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for OcmsTemplateStore entity
 */
@Repository
public interface OcmsTemplateStoreRepository extends BaseRepository<OcmsTemplateStore, String> {
    // The base repository provides all necessary methods
    // template_name is the primary key (String type)
}
