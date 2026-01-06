package com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToppanReportResponseDto {

    private List<FileInfo> data;
    private int total;
    private String source;
    private String reportDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * File name
         */
        private String fileName;

        /**
         * Download URL for the file
         */
        private String downloadUrl;

        /**
         * File size in bytes
         */
        private Long fileSize;
    }
}
