package com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail;

import com.ocmsintranet.cronservice.crud.BaseImplement;
import com.ocmsintranet.cronservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.ocmsintranet.cronservice.crud.beans.SystemConstant;

/**
 * Service implementation for EwebTxnDetail entities
 */
@Service
public class EWebTxnDetailServiceImpl extends BaseImplement<EwebTxnDetail, EwebTxnDetailId, EWebTxnDetailRepository>
        implements EWebTxnDetailService {

    /**
     * Constructor with required dependencies
     */
    public EWebTxnDetailServiceImpl(EWebTxnDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<EwebTxnDetail> findUnsynchronizedTodayTransactions() {
        return repository.findUnsynchronizedTodayTransactions();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public EwebTxnDetail updateSyncStatus(String receiptNo, String offenceNoticeNo, String isSync) {
        EwebTxnDetailId id = new EwebTxnDetailId(receiptNo, offenceNoticeNo);
        Optional<EwebTxnDetail> optionalTxn = getById(id);
        if (optionalTxn.isPresent()) {
            EwebTxnDetail txn = optionalTxn.get();
            txn.setIsSync(isSync);
            txn.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            txn.setUpdDate(LocalDateTime.now());
            return save(txn);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void batchUpdateSyncStatus(List<EwebTxnDetail> transactions, String isSync) {
        LocalDateTime now = LocalDateTime.now();
        for (EwebTxnDetail txn : transactions) {
            txn.setIsSync(isSync);
            txn.setUpdUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            txn.setUpdDate(now);
        }
        saveAll(transactions);
    }
}
