package com.ocmsintranet.cronservice.testing.suspension_revival.ts.datahive.batch.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface TsDatahiveBatchTestService {
    SuspensionRevivalTestResponse executeTsDatahiveBatchTest(SuspensionRevivalTestRequest request);
}
