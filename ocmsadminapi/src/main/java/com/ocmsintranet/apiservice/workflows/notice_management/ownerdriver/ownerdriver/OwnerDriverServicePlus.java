package com.ocmsintranet.apiservice.workflows.notice_management.ownerdriver;

import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface OwnerDriverServicePlus {
    ResponseEntity<?> processPlusOffenceNoticeOwnerDriver(Map<String, Object> requestBody);
    ResponseEntity<?> processPlusUpdateOffenceNoticeOwnerDriver(Map<String, Object> payload);
}
