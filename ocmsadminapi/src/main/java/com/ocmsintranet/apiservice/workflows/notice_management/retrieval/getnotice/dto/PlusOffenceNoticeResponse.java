package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Response DTO for the PLUS API /plus-offence-notice endpoint
 */
@Data
public class PlusOffenceNoticeResponse {
    private Long total;
    private Integer limit;
    private Integer skip;
    private List<Map<String, Object>> data;
}
