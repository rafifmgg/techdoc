package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsdrivernotice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EocmsDriverNoticeRepository extends JpaRepository<EocmsDriverNotice, EocmsDriverNoticeId>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsDriverNotice> {
}
