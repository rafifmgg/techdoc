package com.ocmsintranet.apiservice.testing.furnish_rejection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FurnishRejectionTestRequest {
    private String scenarioName;
    @Builder.Default
    private boolean setupData = true;
    @Builder.Default
    private boolean cleanupAfterTest = false;
}
