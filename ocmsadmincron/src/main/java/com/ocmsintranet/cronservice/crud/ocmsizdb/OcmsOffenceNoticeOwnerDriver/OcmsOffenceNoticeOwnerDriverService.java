package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

public interface OcmsOffenceNoticeOwnerDriverService extends BaseService<OcmsOffenceNoticeOwnerDriver, OcmsOffenceNoticeOwnerDriverId> {

    /**
     * Find ONOD records by is_sync flag
     * Used by Process 7 (Batch Cron Sync) to find records that need syncing to internet PII database
     *
     * @param isSync The sync status ("N" = needs syncing, "Y" = already synced)
     * @return List of ONOD records with the specified sync status
     */
    List<OcmsOffenceNoticeOwnerDriver> findByIsSync(String isSync);

    /**
     * Find all offender records for a given notice number
     * Used by OCMS 41 auto-approval to check existing hirers/drivers
     *
     * @param noticeNo Notice number
     * @return List of offender records for the notice
     */
    List<OcmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo);
}