package com.ocmsintranet.apiservice.testing.furnish_manual.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualFurnishTestRequest {
    private String scenarioName;
    @Builder.Default
    private boolean setupData = true;
    @Builder.Default
    private boolean cleanupAfterTest = false;
}
