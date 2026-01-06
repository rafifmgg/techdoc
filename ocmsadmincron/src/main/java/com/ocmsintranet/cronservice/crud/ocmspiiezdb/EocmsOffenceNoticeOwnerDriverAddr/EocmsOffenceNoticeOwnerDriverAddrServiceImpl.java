package com.ocmsintranet.cronservice.crud.ocmspiiezdb.EocmsOffenceNoticeOwnerDriverAddr;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import com.ocmsintranet.cronservice.crud.beans.FindAllResponse;
import com.ocmsintranet.cronservice.crud.beans.SymmetricKeyConstants;

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
 * Service implementation for EocmsOffenceNoticeOwnerDriverAddr entity.
 * Handles encryption/decryption of PII address data using SQL Server symmetric key.
 */
@Service
@Transactional(value = "piiTransactionManager")
public class EocmsOffenceNoticeOwnerDriverAddrServiceImpl
        extends BaseImplement<EocmsOffenceNoticeOwnerDriverAddr, EocmsOffenceNoticeOwnerDriverAddrId, EocmsOffenceNoticeOwnerDriverAddrRepository>
        implements EocmsOffenceNoticeOwnerDriverAddrService {

    @Autowired
    private SymmetricKeyConstants symmetricKeyConstants;

    @PersistenceContext(unitName = "pii")
    private EntityManager entityManager;

    /**
     * Constructor with required dependencies
     */
    public EocmsOffenceNoticeOwnerDriverAddrServiceImpl(
            EocmsOffenceNoticeOwnerDriverAddrRepository repository,
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
    public EocmsOffenceNoticeOwnerDriverAddr save(EocmsOffenceNoticeOwnerDriverAddr entity) {
        openSymmetricKey();
        try {
            return super.save(entity);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public Optional<EocmsOffenceNoticeOwnerDriverAddr> getById(EocmsOffenceNoticeOwnerDriverAddrId id) {
        openSymmetricKey();
        try {
            return super.getById(id);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public FindAllResponse<EocmsOffenceNoticeOwnerDriverAddr> getAll(Map<String, String[]> requestParams) {
        openSymmetricKey();
        try {
            return super.getAll(requestParams);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager")
    public EocmsOffenceNoticeOwnerDriverAddr patch(EocmsOffenceNoticeOwnerDriverAddrId id, EocmsOffenceNoticeOwnerDriverAddr entity) {
        openSymmetricKey();
        try {
            return super.patch(id, entity);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager")
    public List<EocmsOffenceNoticeOwnerDriverAddr> saveAll(Iterable<EocmsOffenceNoticeOwnerDriverAddr> entities) {
        openSymmetricKey();
        try {
            return super.saveAll(entities);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNo(String noticeNo) {
        openSymmetricKey();
        try {
            return repository.findByNoticeNo(noticeNo);
        } finally {
            closeSymmetricKey();
        }
    }

    @Override
    @Transactional(value = "piiTransactionManager", readOnly = true)
    public List<EocmsOffenceNoticeOwnerDriverAddr> findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(
            List<String> noticeNos, List<String> indicators, List<String> types) {
        openSymmetricKey();
        try {
            return repository.findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(noticeNos, indicators, types);
        } finally {
            closeSymmetricKey();
        }
    }
}