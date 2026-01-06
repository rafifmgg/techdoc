package com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsoffenceattachment;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsOffenceAttachmentRepository extends BaseRepository<OcmsOffenceAttachment, Integer> {
    boolean existsByNoticeNoAndFileName(String noticeNo, String fileName);
}
