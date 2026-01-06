package com.ocmsintranet.cronservice.testing.suspension_revival.notification.service;

import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestRequest;
import com.ocmsintranet.cronservice.testing.suspension_revival.dto.SuspensionRevivalTestResponse;

public interface NotificationTestService {
    SuspensionRevivalTestResponse executeNotificationTest(SuspensionRevivalTestRequest request);
}
