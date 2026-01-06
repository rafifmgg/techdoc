package com.ocmsintranet.apiservice.workflows.notice_management.suspension;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Unclaimed Reminder submission
 * Maps to the payload in POST /v1/submit-unclaimed
 */
@Data
public class UnclaimedReminderDto {

    private String noticeNo;
    private LocalDateTime dateOfLetter;
    private String lastProcessingStage;
    private String idNumber;
    private String idType;
    private String ownerHirerIndicator;
    private LocalDateTime dateOfReturn;
    private String reasonForUnclaim;
    private String unclaimRemarks;
    private String reasonOfSuspension;  // Should be "UNC"
    private Integer daysOfRevival;
    private String suspensionRemarks;
}
