package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EocmsRequestDriverParticularsRepository extends JpaRepository<EocmsRequestDriverParticulars, EocmsRequestDriverParticularsId>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsRequestDriverParticulars> {
}
