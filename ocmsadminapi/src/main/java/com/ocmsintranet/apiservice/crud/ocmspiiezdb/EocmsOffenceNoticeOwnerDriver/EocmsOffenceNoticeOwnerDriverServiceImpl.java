package com.ocmsintranet.apiservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.beans.SymmetricKeyConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for EocmsOffenceNoticeOwnerDriver entity.
 * Handles encryption/decryption of PII data using SQL Server symmetric key.
 */
@Service
@Transactional(value = "piiTransactionManager")
public class EocmsOffenceNoticeOwnerDriverServiceImpl
        extends BaseImplement<EocmsOffenceNoticeOwnerDriver, String, EocmsOffenceNoticeOwnerDriverRepository>
        implements EocmsOffenceNoticeOwnerDriverService {

    @Autowired
    private SymmetricKeyConstants symmetricKeyConstants;

    @PersistenceContext(unitName = "pii")
    private EntityManager entityManager;

    /**
     * Constructor with required dependencies
     */
    public EocmsOffenceNoticeOwnerDriverServiceImpl(
            EocmsOffenceNoticeOwnerDriverRepository repository,
            DatabaseRetryService retryService) {
        super(repository, retryService);
    }

    private void openSymmetricKey() {
        Query query = entityManager.createNativeQuery(symmetricKeyConstants.openSymmetricKey);
        query.executeUpdate();
    }

    private void closeSymmetricKey() {
        Query query = entityManager.createNativeQuery(symmetricKeyConstants.closeSymmetricKey);
        query.executeUpdate();
    }

    @Override
    @Transactional(value = "piiTransactionManager")
    public EocmsOffenceNoticeOwnerDriver save(EocmsOffenceNoticeOwnerDriver entity) {
        openSymmetricKey();
        try {
            return super.save(entity);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public Optional<EocmsOffenceNoticeOwnerDriver> getById(String id) {
        openSymmetricKey();
        try {
            return super.getById(id);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public FindAllResponse<EocmsOffenceNoticeOwnerDriver> getAll(Map<String, String[]> requestParams) {
        openSymmetricKey();
        try {
            return super.getAll(requestParams);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager")
    public EocmsOffenceNoticeOwnerDriver patch(String id, EocmsOffenceNoticeOwnerDriver entity) {
        openSymmetricKey();
        try {
            return super.patch(id, entity);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager")
    public List<EocmsOffenceNoticeOwnerDriver> saveAll(Iterable<EocmsOffenceNoticeOwnerDriver> entities) {
        openSymmetricKey();
        try {
            return super.saveAll(entities);
        } finally {
            closeSymmetricKey();
        }
    }
}