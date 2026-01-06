package com.ocmsintranet.cronservice.testing.suspension_revival.ps.toppan.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface PsToppanTestService {
    SuspensionRevivalTestResponse executePsToppanTest(SuspensionRevivalTestRequest request);
}
