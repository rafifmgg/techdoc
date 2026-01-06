package com.ocmsintranet.apiservice.crud.ocmsizdb.sequence;

public interface SequenceService {
    Long getNextNoticeNumber(String subsystem);
    
    /**
     * Gets the next sequence number for suspended notice sr_no
     * @return The next sequence number
     */
    Integer getNextSequence(String sequenceName);

    // Add these later when needed
    // Long getNextTransactionId();
    // Long getNextBatchId();

    /**
     * Gets the next transaction reference number from seq_ocms_txn_ref_number
     * @return The next transaction reference number
     */
    Integer getNextTxnRefNumber();
}