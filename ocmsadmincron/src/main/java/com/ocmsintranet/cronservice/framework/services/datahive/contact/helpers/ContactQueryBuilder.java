package com.ocmsintranet.cronservice.framework.services.datahive.contact.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper class to build SQL queries for DataHive contact lookups
 * Constructs appropriate queries based on owner ID type
 */
@Slf4j
@Component
public class ContactQueryBuilder {
    
    // Table names
    private static final String SINGPASS_TABLE = "V_DH_SNDGO_SINGPASSCONTACT_MASTER";
    private static final String CORPPASS_TABLE = "V_DH_GOVTECH_CORPPASS_DELTA";
    
    /**
     * Build SQL query for Singpass (NRIC/FIN) contact lookup
     * Note: CONTACT column contains mobile number (no separate email column)
     * 
     * @param ownerId The NRIC or FIN to look up
     * @return SQL query string
     */
    public String buildSingpassQuery(String ownerId) {
        String sql = String.format(
            "SELECT UIN, CONTACT FROM %s WHERE UIN = '%s'",
            SINGPASS_TABLE, ownerId
        );
        
        log.debug("Built Singpass query");
        return sql;
    }
    
    /**
     * Build SQL query for Corppass (UEN) contact lookup
     * 
     * @param ownerId The UEN to look up
     * @return SQL query string
     */
    public String buildCorppassQuery(String ownerId) {
        String sql = String.format(
            "SELECT ENTITY_ID, CP_ACCOUNT_EMAIL, ACCOUNT_STATUS FROM %s WHERE ENTITY_ID = '%s' AND ACCOUNT_STATUS = 'ACTIVE'",
            CORPPASS_TABLE, ownerId
        );
        
        log.debug("Built Corppass query");
        return sql;
    }
}