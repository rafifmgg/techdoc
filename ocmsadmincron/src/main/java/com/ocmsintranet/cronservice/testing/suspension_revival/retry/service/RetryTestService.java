package com.ocmsintranet.cronservice.testing.suspension_revival.retry.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface RetryTestService {
    SuspensionRevivalTestResponse executeRetryTest(SuspensionRevivalTestRequest request);
}
