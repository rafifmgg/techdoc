package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.cronservice.crud.BaseService;

import java.util.List;

public interface OcmsValidOffenceNoticeService extends BaseService<OcmsValidOffenceNotice, String> {
    /**
     * Find all VON records by notice numbers (batch query using IN clause)
     *
     * @param noticeNumbers List of notice numbers
     * @return List of VON records
     */
    List<OcmsValidOffenceNotice> findByNoticeNoIn(List<String> noticeNumbers);

    /**
     * Find VON records by is_sync flag
     * Used by Process 7 (Batch Cron Sync) to find records that need syncing to internet database
     *
     * @param isSync The sync status ("N" = needs syncing, "Y" = already synced)
     * @return List of VON records with the specified sync status
     */
    List<OcmsValidOffenceNotice> findByIsSync(String isSync);
}