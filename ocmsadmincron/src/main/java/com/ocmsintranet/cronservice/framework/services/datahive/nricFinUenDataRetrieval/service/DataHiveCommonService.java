package com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.service;

import com.ocmsintranet.cronservice.framework.services.datahive.nricFinUenDataRetrieval.dto.CommonDataResult;

import java.util.List;
import java.util.Map;

/**
 * Shared service for retrieving prison/custody data that's common
 * across NRIC and FIN ID types (not applicable for UEN)
 */
public interface DataHiveCommonService {

    /**
     * Retrieve prison and custody data including:
     * - Custody status from V_DH_SPS_CUSTODY_STATUS
     * - Release date from V_DH_SPS_RELEASE_DATE
     *
     * Handles update vs insert logic based on existing records
     *
     * @param idType The type of ID ("NRIC" or "FIN")
     * @param idNumber The ID number to look up
     * @param noticeNumber The offence notice number for tracking
     * @return CommonDataResult containing custody and incarceration data
     */
    CommonDataResult getPrisonCustodyData(String idType, String idNumber, String noticeNumber);

    /**
     * BATCH VERSION: Retrieve prison and custody data for multiple IDs
     * Queries DataHive in batches to avoid rate limiting
     *
     * Benefits:
     * - Reduces API calls from N to N/100 per data source (custody, release)
     * - Avoids DataHive rate limiting issues
     * - Improves performance through batch queries
     *
     * @param idType The type of ID ("NRIC" or "FIN")
     * @param idNumbers List of ID numbers to look up
     * @return Map of idNumber -> CommonDataResult
     */
    Map<String, CommonDataResult> batchGetPrisonCustodyData(String idType, List<String> idNumbers);
}