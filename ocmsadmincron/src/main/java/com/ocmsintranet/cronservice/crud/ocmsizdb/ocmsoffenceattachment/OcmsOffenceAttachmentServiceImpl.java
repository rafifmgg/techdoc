package com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsoffenceattachment;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

@Service
public class OcmsOffenceAttachmentServiceImpl
        extends BaseImplement<OcmsOffenceAttachment, Integer, OcmsOffenceAttachmentRepository>
        implements OcmsOffenceAttachmentService {

    public OcmsOffenceAttachmentServiceImpl(OcmsOffenceAttachmentRepository repository,
                                            DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}
