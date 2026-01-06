package com.ocmsintranet.cronservice.framework.dto.repccs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListNoticeDTO {

    private String noticeNo;

    private LocalDateTime sentToRepDate;

    private String suspensionType;

    private String eprReasonSuspension;

    private String crsReasonSuspension;

    private String subsystemLabel;

    private LocalDateTime eprDateOfSuspension;

    private LocalDateTime crsDateOfSuspension;
}

