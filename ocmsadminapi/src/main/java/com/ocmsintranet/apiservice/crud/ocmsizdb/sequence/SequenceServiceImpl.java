package com.ocmsintranet.apiservice.crud.ocmsizdb.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SequenceServiceImpl implements SequenceService {
    
    @Autowired
    private SequenceRepository sequenceRepository;
    
    @Override
    public Long getNextNoticeNumber(String subsystem) {
        try {
            Long nextVal = sequenceRepository.getNextNoticeNumber(subsystem);
            log.info("Retrieved next notice number for subsystem {}: {}", subsystem, nextVal);
            return nextVal;
        } catch (Exception e) {
            log.error("Error getting next notice number for subsystem {}: {}", subsystem, e.getMessage());
            throw new RuntimeException("Failed to get next notice number for subsystem: " + subsystem, e);
        }
    }
    
    @Override
    public Integer getNextSequence(String sequenceName) {
        try {
            Integer nextVal = sequenceRepository.getNextSequence(sequenceName);
            log.info("Retrieved next sequence number for {}: {}", sequenceName, nextVal);
            return nextVal;
        } catch (Exception e) {
            log.error("Error getting next sequence number for {}: {}", sequenceName, e.getMessage());
            throw new RuntimeException("Failed to get next sequence number for: " + sequenceName, e);
        }
    }

    @Override
    public Integer getNextTxnRefNumber() {
        return getNextSequence("TXN_REF_NUMBER_SEQ");
    }
}