package com.ocmsintranet.cronservice.testing.suspension_revival.auto_revival.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface AutoRevivalTestService {
    SuspensionRevivalTestResponse executeAutoRevivalTest(SuspensionRevivalTestRequest request);
}
