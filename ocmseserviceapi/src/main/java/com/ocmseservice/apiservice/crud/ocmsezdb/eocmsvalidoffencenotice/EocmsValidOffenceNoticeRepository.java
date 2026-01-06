package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EocmsValidOffenceNoticeRepository extends JpaRepository<EocmsValidOffenceNotice, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsValidOffenceNotice> {


    @Query(value = "select * from eocms_valid_offence_notice where notice_no =:noticeNumber  and crs_reason_of_suspension IS NULL", nativeQuery = true)
    EocmsValidOffenceNotice findByNoticeNo(@Param("noticeNumber") String noticeNumber);

    @Query(value = "select * from eocms_valid_offence_notice where vehicle_no=:vehicleNo and crs_reason_of_suspension IS NULL", nativeQuery = true)
    List<EocmsValidOffenceNotice> findAllByVehicleNoIn(@Param("vehicleNo") String vehicleNo);

    @Query(value = "select * from eocms_valid_offence_notice where notice_no IN (:noticeNumbers) ", nativeQuery = true)
    List<EocmsValidOffenceNotice> findAllByNoticeNoIn(@Param("noticeNumbers") List<String> noticeNumbers);
}
