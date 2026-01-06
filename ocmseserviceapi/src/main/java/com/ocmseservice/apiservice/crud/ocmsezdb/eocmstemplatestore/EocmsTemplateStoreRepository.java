package com.ocmseservice.apiservice.crud.ocmsezdb.eocmstemplatestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EocmsTemplateStoreRepository extends JpaRepository<EocmsTemplateStore, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsTemplateStore> {
}
