package com.ocmsintranet.cronservice.crud.repositories.ocmsizdb;

import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsoffenceattachment.OcmsOffenceAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsOffenceAttachmentRepository extends JpaRepository<OcmsOffenceAttachment, Integer> {
}
