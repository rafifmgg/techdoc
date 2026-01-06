package com.ocmsintranet.cronservice.testing.suspension_revival.ts.datahive.single.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface TsDatahiveSingleTestService {
    SuspensionRevivalTestResponse executeTsDatahiveSingleTest(SuspensionRevivalTestRequest request);
}
