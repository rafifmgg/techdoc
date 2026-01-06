package com.ocmsintranet.apiservice.workflows.plus.evalidnotice;

import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface EvalidNoticeService {
    ResponseEntity<?> processPlusEValidOffenceNotice(Map<String, Object> requestBody);
}
