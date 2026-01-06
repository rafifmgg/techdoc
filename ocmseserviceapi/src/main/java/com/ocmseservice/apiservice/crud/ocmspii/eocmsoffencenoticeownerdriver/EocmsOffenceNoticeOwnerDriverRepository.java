package com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EocmsOffenceNoticeOwnerDriverRepository extends JpaRepository<EocmsOffenceNoticeOwnerDriver, EocmsOffenceNoticeOwnerDriverId>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsOffenceNoticeOwnerDriver> {
    
    /**
     * Find notice numbers by ID number where offender_indicator = 'Y'
     * 
     * @param idNo The ID number to search for
     * @return List of notice numbers
     */
    @Query(value = "SELECT notice_no FROM ocmspiiezmgr.eocms_offence_notice_owner_driver WHERE id_no = :idNo AND offender_indicator = 'Y'", nativeQuery = true)
    List<String> findNoticeNosByIdNoAndOffenderIndicator(@Param("idNo") String idNo);
    
    /**
     * Find owner driver records by notice number
     * 
     * @param noticeNo The notice number to search for
     * @return List of owner driver records
     */
    @Query(value = "SELECT * FROM ocmspiiezmgr.eocms_offence_notice_owner_driver WHERE notice_no = :noticeNo", nativeQuery = true)
    List<EocmsOffenceNoticeOwnerDriver> findByNoticeNo(@Param("noticeNo") String noticeNo);
}
