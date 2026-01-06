package com.ocmsintranet.apiservice.testing.furnish_manual.helpers;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class TestContext {
    private String scenarioName;
    private String noticeNo;
    private Map<String, Object> scenarioData;
    private Map<String, Object> apiResponse;
    private Map<String, Object> verificationData = new HashMap<>();
}
