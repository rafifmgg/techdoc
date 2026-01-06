package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.UENDataResult;

/**
 * Service interface for retrieving business entity data using UEN
 * from various DataHive (Snowflake) ACRA tables
 */
public interface DataHiveUENService {
    
    /**
     * Retrieve comprehensive data for a business entity including:
     * - Company registration data (from registered or de-registered tables)
     * - Shareholder information
     * - Board member information
     * 
     * Also handles:
     * - TS-ACR transaction code application
     * - Email notifications for not found UENs
     * - Gazetted flag updates for Listed Companies
     * 
     * @param uen The UEN to look up
     * @param noticeNumber The offence notice number for tracking
     * @return UENDataResult containing all retrieved data
     */
    UENDataResult retrieveUENData(String uen, String noticeNumber);
}