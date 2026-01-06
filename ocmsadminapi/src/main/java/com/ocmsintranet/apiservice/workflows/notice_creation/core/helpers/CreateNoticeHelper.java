package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail.OcmsOffenceNoticeDetailService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@Slf4j
public class CreateNoticeHelper {
    
    @Autowired
    private NoticeValidationHelper noticeValidationHelper;
    
    @Autowired
    private NoticeProcessingHelper noticeProcessingHelper;
    
    @Autowired
    private NoticeNumberHelper noticeNumberHelper;
    
    @Autowired
    private NoticeRecordHelper noticeRecordHelper;

    @Autowired
    private AdvisoryNoticeHelper advisoryNoticeHelper;

    // Delegate methods to NoticeValidationHelper
    public Map<String, Object> createErrorResponse(String statusCode, String message) {
        return noticeValidationHelper.createErrorResponse(statusCode, message);
    }
    
    public List<OffenceNoticeDto> extractDtoList(Map<String, Object> mappedData) {
        return noticeValidationHelper.extractDtoList(mappedData);
    }

    public List<OffenceNoticeDto> validateMandatoryFieldsForBatch(List<OffenceNoticeDto> dtoList) {
        return noticeValidationHelper.validateMandatoryFieldsForBatch(dtoList);
    }

    public Map<String, Object> prepareSingleDtoMap(Map<String, Object> mappedData, OffenceNoticeDto dto) {
        return noticeValidationHelper.prepareSingleDtoMap(mappedData, dto);
    }
    
    public boolean checkDuplicateNoticeNumber(Map<String, Object> data, OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService) {
        return noticeValidationHelper.checkDuplicateNoticeNumber(data, ocmsValidOffenceNoticeService);
    }

    public String getOffenseTypeFromRuleCode(Map<String, Object> data) {
        return noticeValidationHelper.getOffenseTypeFromRuleCode(data);
    }

    public boolean checkDuplicateOffenseDetails(Map<String, Object> data, OcmsValidOffenceNoticeService validOffenceNoticeService) {
        return noticeValidationHelper.checkDuplicateOffenseDetails(data, validOffenceNoticeService);
    }

    // Delegate methods to NoticeProcessingHelper
    public boolean checkANSForEHT(Map<String, Object> data) {
        return noticeProcessingHelper.checkANSForEHT(data);
    }
    
    public boolean checkANSForOthers(Map<String, Object> data) {
        return noticeProcessingHelper.checkANSForOthers(data);
    }
    
    public void processPS_ANS(Map<String, Object> data,
                          SuspendedNoticeService ocmsSuspendedNoticeService,
                          OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        noticeProcessingHelper.processPS_ANS(data, ocmsSuspendedNoticeService, ocmsSuspensionReasonService);
    }

    public void setProcessingStagesForX(Map<String, Object> data) {
        noticeProcessingHelper.setProcessingStagesForX(data);
    }

    public void setProcessingStagesForVSD(Map<String, Object> data) {
        noticeProcessingHelper.setProcessingStagesForVSD(data);
    }

    public void processForeignVehicle(Map<String, Object> data,
                                        SuspendedNoticeService ocmsSuspendedNoticeService,
                                        OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        noticeProcessingHelper.processForeignVehicle(data, ocmsSuspendedNoticeService, ocmsSuspensionReasonService);
    }

    /**
     * PHASE 1: Set foreign vehicle settings only (no notice creation, no suspension)
     * This is Step 5a in the new flow - settings applied BEFORE notice creation
     */
    public void setForeignVehicleSettings(Map<String, Object> data) {
        noticeProcessingHelper.setForeignVehicleSettings(data);
    }

    /**
     * PHASE 1: Create PS-FOR suspension AFTER notice creation
     * This is Step 5e in the new flow - suspension created AFTER notice exists
     */
    public void createForeignVehicleSuspension(String noticeNumber,
                                                Map<String, Object> data,
                                                SuspendedNoticeService ocmsSuspendedNoticeService,
                                                OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
        if (dto == null) {
            log.error("No DTO found for creating foreign vehicle suspension");
            return;
        }

        java.time.LocalDateTime currentDate = java.time.LocalDateTime.now();

        // Delegate to the existing private method in NoticeProcessingHelper
        // We need to call it via reflection or make it public - for Phase 1, we'll recreate the logic here
        log.info("Creating PS-FOR suspension for foreign vehicle notice {}", noticeNumber);

        try {
            com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice suspendedNotice =
                new com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice();

            // Get next SR number
            Integer maxSrNo = ocmsSuspendedNoticeService.getMaxSrNoForNotice(noticeNumber);
            suspendedNotice.setSrNo(maxSrNo != null ? maxSrNo + 1 : 1);

            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSuspensionSource("ocmsiz_app_conn");
            suspendedNotice.setSuspensionType("PS");
            suspendedNotice.setReasonOfSuspension("FOR");
            suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");
            suspendedNotice.setDateOfRevival(null);
            suspendedNotice.setDueDateOfRevival(null);
            suspendedNotice.setSuspensionRemarks("Foreign vehicle detected by system");

            ocmsSuspendedNoticeService.save(suspendedNotice);

            log.info("PS-FOR suspension created successfully for notice {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error creating PS-FOR suspension: {}", e.getMessage(), e);
        }
    }

    /**
     * OCMS 14: Create TS-OLD suspension AFTER notice creation for VIP vehicles
     * This is Step 5f in the VIP flow - suspension created AFTER notice exists
     *
     * Documentation reference:
     * - Section 7.2.2: "Once a notice is created for a VIP vehicle, it is immediately suspended
     *   under the temporary suspension code 'OLD' for 21 days"
     * - Suspension type: TS-OLD (Temporary Suspension - Under Investigation)
     * - Duration: 21 days (configurable in Suspension_Reason table)
     */
    public void createVipVehicleSuspension(String noticeNumber,
                                           Map<String, Object> data,
                                           SuspendedNoticeService ocmsSuspendedNoticeService,
                                           OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
        if (dto == null) {
            log.error("No DTO found for creating VIP vehicle suspension");
            return;
        }

        java.time.LocalDateTime currentDate = java.time.LocalDateTime.now();

        log.info("Creating TS-OLD suspension for VIP vehicle notice {}", noticeNumber);

        try {
            com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice suspendedNotice =
                new com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice();

            // Get next SR number
            Integer maxSrNo = ocmsSuspendedNoticeService.getMaxSrNoForNotice(noticeNumber);
            suspendedNotice.setSrNo(maxSrNo != null ? maxSrNo + 1 : 1);

            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSuspensionSource("ocmsiz_app_conn");
            suspendedNotice.setSuspensionType("TS");
            suspendedNotice.setReasonOfSuspension("OLD");
            suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");
            suspendedNotice.setDateOfRevival(null);

            // Set due date of revival to 21 days from suspension date
            // (Duration configurable in Suspension_Reason table)
            suspendedNotice.setDueDateOfRevival(currentDate.plusDays(21));

            suspendedNotice.setSuspensionRemarks("VIP vehicle detected - under OIC investigation");

            ocmsSuspendedNoticeService.save(suspendedNotice);

            log.info("TS-OLD suspension created successfully for VIP notice {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error creating TS-OLD suspension for VIP vehicle: {}", e.getMessage(), e);
        }
    }

    public void processMilitaryVehicle(Map<String, Object> data) {
        noticeProcessingHelper.processMilitaryVehicle(data);
    }

    public String processDuplicateNotice(Map<String, Object> data, 
                                       OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService,
                                       SuspendedNoticeService ocmsSuspendedNoticeService,
                                       OcmsSuspensionReasonService ocmsSuspensionReasonService) {
        return noticeProcessingHelper.processDuplicateNotice(data, ocmsValidOffenceNoticeService, 
                                                           ocmsSuspendedNoticeService, ocmsSuspensionReasonService,
                                                           noticeNumberHelper, noticeRecordHelper);
    }

    // insertIntoSuspendedNotice method removed as it's not needed
    // Suspended notices are created directly in the specific processing methods

    public void populateAddressForMilitary(Map<String, Object> data) {
        noticeProcessingHelper.populateAddressForMilitary(data);
    }

    public NoticeResponse prepareSuccessAndFailureResponse(List<String> processedNoticeNumbers, 
                                                       Map<String, Object> failedNotices,
                                                       List<String> duplicateNotices) {
        return noticeProcessingHelper.prepareSuccessAndFailureResponse(processedNoticeNumbers, failedNotices, duplicateNotices);
    }

    // Delegate methods to NoticeNumberHelper
    public String processNoticeNumber(Map<String, Object> singleDtoMap) {
        return noticeNumberHelper.processNoticeNumber(singleDtoMap);
    }

    // Delegate methods to NoticeRecordHelper
    public void createNoticeRecord(Map<String, Object> data,
                                  OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService,
                                  OcmsOffenceNoticeDetailService passedDetailService) {
        noticeRecordHelper.createNoticeRecord(data, ocmsValidOffenceNoticeService, passedDetailService);
    }

    // Delegate methods to AdvisoryNoticeHelper
    public AdvisoryNoticeHelper.AdvisoryNoticeResult checkAnQualification(Map<String, Object> data) {
        return advisoryNoticeHelper.checkQualification(data);
    }

    public void updateNoticeWithAnFlags(String noticeNumber) {
        advisoryNoticeHelper.updateNoticeWithAnFlags(noticeNumber);
    }
}