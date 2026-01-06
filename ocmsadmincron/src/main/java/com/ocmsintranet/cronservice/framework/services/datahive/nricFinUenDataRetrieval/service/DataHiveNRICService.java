package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.NRICDataResult;

/**
 * Service interface for retrieving data for Singapore Citizens and Permanent Residents using NRIC
 * from various DataHive (Snowflake) tables
 */
public interface DataHiveNRICService {
    
    /**
     * Retrieve comprehensive data for an NRIC holder including:
     * - Comcare fund data (FSC and CCC)
     * - Prison/custody data (via CommonService)
     * - Contact information (via ContactService)
     * 
     * @param nric The NRIC to look up
     * @param noticeNumber The offence notice number for tracking
     * @return NRICDataResult containing all retrieved data
     */
    NRICDataResult retrieveNRICData(String nric, String noticeNumber);
    
    /**
     * Get the common service instance for direct access
     * @return DataHiveCommonService instance
     */
    DataHiveCommonService getCommonService();
}