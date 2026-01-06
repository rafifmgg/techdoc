package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceAttachment;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for OcmsOffenceAttachment entities
 */
@Service
public class OcmsOffenceAttachmentServiceImpl extends BaseImplement<OcmsOffenceAttachment, Integer, OcmsOffenceAttachmentRepository> 
        implements OcmsOffenceAttachmentService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsOffenceAttachmentServiceImpl(OcmsOffenceAttachmentRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    // You can add custom methods or override base methods if needed
}
