// ============================================================================
// LtaBusinessRulesChecker.java - Business Rules and Validation
// ============================================================================
package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LtaBusinessRulesChecker {

    private final TableQueryService tableQueryService;

    public LtaBusinessRulesChecker(TableQueryService tableQueryService) {
        this.tableQueryService = tableQueryService;
    }

    /**
     * Check if notice is suspended
     * According to flow diagram, a notice is NOT suspended if:
     * - suspension_type is blank or null, AND
     * - epr_reason is blank or null, AND
     * - crs_reason is blank or null
     */
    public boolean isNoticeSuspended(Map<String, Object> record) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_valid_offence_notice", filters);
            
            if (results == null || results.isEmpty()) {
                log.warn("Notice {} not found in ocms_valid_offence_notice", offenceNoticeNumber);
                return false;
            }
            
            Map<String, Object> notice = results.get(0);
            
            // Check suspension fields
            String suspensionType = (String) notice.get("suspensionType");
            String eprReason = (String) notice.get("eprReasonOfSuspension");
            String crsReason = (String) notice.get("crsReasonOfSuspension");
            
            // Notice is suspended if any of the fields has a value
            boolean isSuspended = !StringUtils.isEmpty(suspensionType) || 
                                  !StringUtils.isEmpty(eprReason) || 
                                  !StringUtils.isEmpty(crsReason);
            
            log.info("Notice {} suspension check: suspension_type={}, epr_reason={}, crs_reason={}, isSuspended={}",
                    offenceNoticeNumber, suspensionType, eprReason, crsReason, isSuspended);
            
            return isSuspended;
        } catch (Exception e) {
            log.error("Error checking if notice is suspended: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if notice is under TS-HST
     */
    public boolean isNoticeUnderTsHst(Map<String, Object> record) {
        // Comment first because hst table not implementated (for reference):
        // try {
        //     String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        //     
        //     Map<String, Object> filters = new HashMap<>();
        //     filters.put("noticeNo", offenceNoticeNumber);
        //     filters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
        //     filters.put("reasonOfSuspension", SystemConstant.SuspensionReason.HST);
        //     filters.put("dateOfRevival[$null]", true); // Check for null
        //     
        //     long count = tableQueryService.count("ocms_suspended_notice", filters);
        //     
        //     log.debug("TS-HST check for notice {}: count={}", offenceNoticeNumber, count);
        //     return count > 0;
        // } catch (Exception e) {
        //     log.error("Error checking if notice is under TS-HST: {}", e.getMessage(), e);
        //     return false;
        // }

        // temp implementation: Query ocms_valid_offence_notice where 
        //   suspension_type = 'TS' AND epr_reason_of_suspension = 'HST' AND notice_no = offenceNoticeNumber
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");

            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            filters.put("suspensionType", SystemConstant.SuspensionType.TEMPORARY);
            filters.put("eprReasonOfSuspension", SystemConstant.SuspensionReason.HST);

            long count = tableQueryService.count("ocms_valid_offence_notice", filters);

            log.debug("TS-HST check (ocms_valid_offence_notice) for notice {}: count={}", offenceNoticeNumber, count);
            return count > 0;
        } catch (Exception e) {
            log.error("Error checking if notice is under TS-HST (ocms_valid_offence_notice): {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if offender is in exclusion list
     */
    public boolean isOffenderInExclusionList(Map<String, Object> record) {
        try {
            String ownerId = (String) record.get("ownerId");
            
            if (ownerId == null || ownerId.isEmpty()) {
                log.warn("Owner ID is null or empty, cannot check exclusion list");
                return false;
            }
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("idNo", ownerId);  // ✅ CHANGED FROM "nricNo" TO "idNo"
            
            long count = tableQueryService.count("ocms_enotification_exclusion_list", filters);
            
            log.debug("Exclusion list check for owner ID {}: count={}", ownerId, count);
            return count > 0;
        } catch (Exception e) {
            log.error("Error checking exclusion list: {}", e.getMessage(), e);
            return false;
        }
    }
}