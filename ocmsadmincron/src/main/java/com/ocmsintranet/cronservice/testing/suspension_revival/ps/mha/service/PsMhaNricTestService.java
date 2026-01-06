package com.ocmsintranet.cronservice.testing.suspension_revival.ps.mha.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface PsMhaNricTestService {
    SuspensionRevivalTestResponse executePsMhaNricTest(SuspensionRevivalTestRequest request);
}
