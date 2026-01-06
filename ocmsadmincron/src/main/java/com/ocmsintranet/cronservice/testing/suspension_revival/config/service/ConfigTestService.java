package com.ocmsintranet.cronservice.testing.suspension_revival.config.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface ConfigTestService {
    SuspensionRevivalTestResponse executeConfigTest(SuspensionRevivalTestRequest request);
}
