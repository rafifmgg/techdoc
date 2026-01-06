package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.FINDataResult;
import java.util.Date;

/**
 * Service interface for retrieving data for Foreign ID holders (FIN) 
 * from various DataHive (Snowflake) tables
 */
public interface DataHiveFINService {
    
    /**
     * Retrieve comprehensive data for a FIN holder including:
     * - Death status
     * - PR/SC conversion status
     * - Work permit information (EP/WP)
     * - Pass information (LTVP/STP)
     * - Prison/custody data (via CommonService)
     * 
     * Also handles transaction codes (PS-RIP, PS-RP2, TS-OLD) and work item generation
     * 
     * @param fin The FIN to look up
     * @param noticeNumber The offence notice number for tracking
     * @param ownerDriverIndicator The owner/driver indicator ('O', 'D', 'H')
     * @param offenceDate The date of the offence for death date comparison
     * @return FINDataResult containing all retrieved data
     */
    FINDataResult retrieveFINData(String fin, String noticeNumber, 
                                  String ownerDriverIndicator, Date offenceDate);
    
    /**
     * Get the common service instance for direct access
     * @return DataHiveCommonService instance
     */
    DataHiveCommonService getCommonService();
}