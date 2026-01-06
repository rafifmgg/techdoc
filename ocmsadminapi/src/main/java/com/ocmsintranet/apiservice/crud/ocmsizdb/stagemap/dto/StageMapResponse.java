package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stage map data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageMapResponse {
    private String lastProcessingStage;
    private String nextProcessingStage;
}
