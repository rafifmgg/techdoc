package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EocmsWebTxnAuditRepository extends JpaRepository<EocmsWebTxnAudit, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsWebTxnAudit> {
    
   
}
