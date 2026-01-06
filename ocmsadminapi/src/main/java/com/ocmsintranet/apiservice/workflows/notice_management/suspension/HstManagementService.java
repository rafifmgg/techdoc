package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import java.util.List;

/**
 * Service interface for HST (House Tenant) Management
 */
public interface HstManagementService {

    /**
     * Check if HST ID exists in ocms_hst table
     * @param idNo Offender ID number
     * @return true if exists, false otherwise
     */
    boolean hstExists(String idNo);

    /**
     * Apply HST suspension to all outstanding notices for an offender
     * @param request HST apply request with offender details
     * @return List of processing results per notice
     * @throws HstAlreadyExistsException if HST ID already exists
     */
    List<HstProcessingResult> applyHstSuspension(HstApplyDto request) throws HstAlreadyExistsException;

    /**
     * Update HST record and all TS-HST suspended notices
     * @param idNo Offender ID number
     * @param request Update details
     * @return List of processing results per notice
     */
    List<HstProcessingResult> updateHst(String idNo, HstUpdateDto request);

    /**
     * Revive all TS-HST suspensions and remove HST record
     * @param request Revive request with new address details
     * @return List of processing results per notice
     */
    List<HstProcessingResult> reviveHst(HstReviveDto request);

    /**
     * Exception thrown when HST ID already exists
     */
    class HstAlreadyExistsException extends Exception {
        public HstAlreadyExistsException(String message) {
            super(message);
        }
    }
}
