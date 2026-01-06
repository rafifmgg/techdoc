package com.ocmsintranet.apiservice.workflows.furnish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for furnishable check response (OCMS41.43).
 * Checks if notice is at a furnishable stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishableCheckResponse {

    private String noticeNo;
    private boolean isFurnishable;
    private String currentProcessingStage;
    private String reason; // Reason if not furnishable
}
