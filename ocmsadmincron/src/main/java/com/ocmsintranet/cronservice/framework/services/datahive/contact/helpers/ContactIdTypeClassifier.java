package com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers.LtaIdTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper class to classify owner ID types for DataHive queries
 * Determines whether to query Singpass or Corppass tables
 */
@Slf4j
@Component
public class ContactIdTypeClassifier {
    
    /**
     * Enum for ID type classification
     */
    public enum IdTypeCategory {
        SINGPASS,   // NRIC or FIN
        CORPPASS,   // UEN types
        UNKNOWN     // Unrecognized ID type
    }
    
    /**
     * Classify the owner ID type to determine which DataHive table to query
     * 
     * @param ownerIdType The owner ID type code
     * @return IdTypeCategory indicating Singpass, Corppass, or Unknown
     */
    public IdTypeCategory classifyIdType(String ownerIdType) {
        if (ownerIdType == null || ownerIdType.trim().isEmpty()) {
            log.warn("Null or empty owner ID type provided");
            return IdTypeCategory.UNKNOWN;
        }
        
        // Use existing LtaIdTypeUtil for classification
        if (LtaIdTypeUtil.isSingpassIdType(ownerIdType)) {
            log.debug("ID type {} classified as Singpass (NRIC/FIN)", ownerIdType);
            return IdTypeCategory.SINGPASS;
        } else if (LtaIdTypeUtil.isCorppassIdType(ownerIdType)) {
            log.debug("ID type {} classified as Corppass (UEN)", ownerIdType);
            return IdTypeCategory.CORPPASS;
        } else {
            log.warn("Unknown ID type: {}", ownerIdType);
            return IdTypeCategory.UNKNOWN;
        }
    }
}