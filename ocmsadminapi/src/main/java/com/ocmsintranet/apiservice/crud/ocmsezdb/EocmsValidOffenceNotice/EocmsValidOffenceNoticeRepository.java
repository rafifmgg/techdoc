package com.ocmsintranet.apiservice.crud.ocmsezdb.EocmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EocmsValidOffenceNotice entity (Internet Database)
 */
@Repository
public interface EocmsValidOffenceNoticeRepository extends BaseRepository<EocmsValidOffenceNotice, String> {

    /**
     * Find notices by vehicle number
     */
    List<EocmsValidOffenceNotice> findByVehicleNo(String vehicleNo);

    /**
     * Find notices by payment status
     */
    List<EocmsValidOffenceNotice> findByPaymentStatus(String paymentStatus);

    /**
     * Find notices by processing stage
     */
    List<EocmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage);

    /**
     * Find notices within date range
     */
    @Query("SELECT e FROM EocmsValidOffenceNotice e WHERE e.noticeDateAndTime BETWEEN :startDate AND :endDate")
    List<EocmsValidOffenceNotice> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find unpaid notices
     */
    @Query("SELECT e FROM EocmsValidOffenceNotice e WHERE e.paymentStatus IS NULL OR e.paymentStatus != 'PD'")
    List<EocmsValidOffenceNotice> findUnpaidNotices();
}
