package com.ocmsintranet.cronservice.testing.suspension_revival.regression.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface RegressionTestService {
    SuspensionRevivalTestResponse executeRegressionTest(SuspensionRevivalTestRequest request);
}
