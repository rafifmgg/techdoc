package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSuspensionReason.OcmsSuspensionReasonService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.sequence.SequenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AutoSuspensionHelper for triggering suspension logic
 * OCMS 10 - Advisory Notice suspension integration
 * OCMS 17 - Auto TS triggers (ACR, CLV, NRO, PAM, PDP, ROV, SYS)
 */
@Slf4j
@Component
public class AutoSuspensionHelper {

    @Autowired
    private NoticeProcessingHelper noticeProcessingHelper;

    @Autowired
    private SuspendedNoticeService ocmsSuspendedNoticeService;

    @Autowired
    private OcmsSuspensionReasonService ocmsSuspensionReasonService;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private TemporarySuspensionHelper temporarySuspensionHelper;

    /**
     * Trigger advisory notice suspension (PS-ANS)
     * This is called AFTER the notice is already created, so we only create the suspension record
     * @param noticeNumber Notice number
     * @param reason Suspension reason
     */
    public void triggerAdvisoryNotice(String noticeNumber, String reason) {
        log.info("Triggering PS-ANS suspension for notice {}, reason: {}", noticeNumber, reason);

        try {
            // Create PS-ANS suspension record directly
            // Note: The notice is already created by this point, we just need to add the suspension
            java.time.LocalDateTime currentDate = java.time.LocalDateTime.now();

            com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice suspendedNotice =
                new com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNotice();

            // Set the fields for suspended notice
            suspendedNotice.setNoticeNo(noticeNumber);
            suspendedNotice.setDateOfSuspension(currentDate);
            suspendedNotice.setSrNo(getNextSrNo()); // Get next sequence
            suspendedNotice.setSuspensionSource("OCMS");
            suspendedNotice.setSuspensionType("PS");
            suspendedNotice.setReasonOfSuspension("ANS");

            // No revival dates for PS-ANS
            suspendedNotice.setDateOfRevival(null);
            suspendedNotice.setDueDateOfRevival(null);

            suspendedNotice.setOfficerAuthorisingSupension("SYSTEM");
            suspendedNotice.setSuspensionRemarks("ANS detected - " + reason);

            // Save the suspended notice record
            ocmsSuspendedNoticeService.save(suspendedNotice);

            log.info("Successfully triggered PS-ANS suspension for notice {}", noticeNumber);
        } catch (Exception e) {
            log.error("Error triggering PS-ANS suspension for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger ACR (ACRA) suspension when company is deregistered or director/shareholder cannot be identified
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param reason Detailed reason for ACR suspension
     */
    public void triggerAcrSuspension(String noticeNumber, String reason) {
        log.info("Triggering TS-ACR suspension for notice {}, reason: {}", noticeNumber, reason);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "ACR";
            String suspensionRemarks = "Auto-suspended: " + reason;
            String officerAuthorisingSuspension = "SYSTEM";

            // Call TS helper to apply suspension
            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, // daysToRevive (will use default from suspension_reason table)
                suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-ACR for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-ACR for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-ACR for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger CLV (Classified Vehicle) suspension for VIP vehicles at RR3/DR3 stage
     * OCMS 17 - Auto TS trigger with looping
     * @param noticeNumber Notice number
     * @param reason Detailed reason for CLV suspension
     */
    public void triggerClvSuspension(String noticeNumber, String reason) {
        log.info("Triggering TS-CLV suspension for notice {}, reason: {}", noticeNumber, reason);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "CLV";
            String suspensionRemarks = "Auto-suspended: " + reason;
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-CLV for notice {} (looping enabled)", noticeNumber);
            } else {
                log.error("Failed to trigger TS-CLV for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-CLV for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger NRO (MHA/DataHive Exception) suspension when MHA address validation fails
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param reason Detailed reason for NRO suspension
     */
    public void triggerNroSuspension(String noticeNumber, String reason) {
        log.info("Triggering TS-NRO suspension for notice {}, reason: {}", noticeNumber, reason);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "NRO";
            String suspensionRemarks = "Auto-suspended: " + reason;
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-NRO for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-NRO for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-NRO for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger PAM (Payment Mismatch) suspension when payment vehicle doesn't match notice vehicle
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param paymentVehicle Vehicle number in payment
     * @param noticeVehicle Vehicle number in notice
     */
    public void triggerPamSuspension(String noticeNumber, String paymentVehicle, String noticeVehicle) {
        log.info("Triggering TS-PAM suspension for notice {}, payment vehicle: {}, notice vehicle: {}",
            noticeNumber, paymentVehicle, noticeVehicle);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "PAM";
            String suspensionRemarks = String.format("Auto-suspended: Payment vehicle %s does not match notice vehicle %s",
                paymentVehicle, noticeVehicle);
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-PAM for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-PAM for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-PAM for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger PDP (Pending Driver Particulars) suspension when furnish requires approval
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param reason Detailed reason for PDP suspension
     */
    public void triggerPdpSuspension(String noticeNumber, String reason) {
        log.info("Triggering TS-PDP suspension for notice {}, reason: {}", noticeNumber, reason);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "PDP";
            String suspensionRemarks = "Auto-suspended: " + reason;
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-PDP for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-PDP for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-PDP for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger ROV (LTA Exception) suspension when LTA returns error codes
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param ltaErrorCode LTA error code (A/B/C or 2/3/4)
     */
    public void triggerRovSuspension(String noticeNumber, String ltaErrorCode) {
        log.info("Triggering TS-ROV suspension for notice {}, LTA error code: {}", noticeNumber, ltaErrorCode);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "ROV";
            String suspensionRemarks = "Auto-suspended: LTA error code " + ltaErrorCode;
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-ROV for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-ROV for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-ROV for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Trigger SYS (System Error) suspension when system/interface errors occur
     * OCMS 17 - Auto TS trigger
     * @param noticeNumber Notice number
     * @param errorDetails Details of the system error
     */
    public void triggerSysSuspension(String noticeNumber, String errorDetails) {
        log.info("Triggering TS-SYS suspension for notice {}, error: {}", noticeNumber, errorDetails);

        try {
            String srNo = String.valueOf(getNextSrNo());
            String suspensionSource = "OCMS";
            String reasonOfSuspension = "SYS";
            String suspensionRemarks = "Auto-suspended: " + errorDetails;
            String officerAuthorisingSuspension = "SYSTEM";

            Map<String, Object> result = temporarySuspensionHelper.processTS(
                noticeNumber, suspensionSource, reasonOfSuspension,
                null, suspensionRemarks, officerAuthorisingSuspension, srNo, ""
            );

            if (result != null && "OCMS-2000".equals(result.get("appCode"))) {
                log.info("Successfully triggered TS-SYS for notice {}", noticeNumber);
            } else {
                log.error("Failed to trigger TS-SYS for notice {}: {}", noticeNumber,
                    result != null ? result.get("message") : "Unknown error");
            }

        } catch (Exception e) {
            log.error("Error triggering TS-SYS for notice {}: {}", noticeNumber, e.getMessage(), e);
        }
    }

    /**
     * Get next sequence number for SR_NO using SequenceService
     */
    private Integer getNextSrNo() {
        return sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");
    }
}
