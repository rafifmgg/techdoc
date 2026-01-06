package com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MhaReportResponseDto {

    private List<MhaRecord> data;
    private int total;
    private int skip;
    private int limit;
    private Map<String, Integer> metrics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MhaRecord {
        // Row 0: notice_no
        private String noticeNo;

        // Row 1: data_type ('SUCCESS' or 'ERROR')
        private String dataType;

        // Row 2-3: Processing stage details
        private String lastProcessingStage;
        private String nextProcessingStage;

        // Row 4: next_processing_date
        private LocalDateTime nextProcessingDate;

        // Row 5: notice_date_and_time
        private LocalDateTime noticeDateAndTime;

        // Row 6: processing_date_time
        private LocalDateTime processingDateTime;

        // Row 7: effective_date
        private LocalDateTime effectiveDate;

        // Row 8: invalid_addr_tag
        private String invalidAddrTag;

        // Row 9-13: Identity fields
        private String idType;
        private String idNo;
        private String name;
        private LocalDateTime dateOfBirth;
        private LocalDateTime dateOfDeath;

        // Row 14-19: Address fields
        private String bldgName;
        private String blkHseNo;
        private String streetName;
        private String postalCode;
        private String floorNo;
        private String unitNo;

        // Row 20-22: Suspension fields (only populated for ERROR records)
        private String suspensionType;
        private LocalDateTime dueDateOfRevival;
        private String eprReasonOfSuspension;
    }
}