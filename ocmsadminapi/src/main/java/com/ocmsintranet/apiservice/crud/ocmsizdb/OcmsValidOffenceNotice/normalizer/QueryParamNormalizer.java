package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.normalizer;

import java.util.Map;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.NoticeQueryRequest;

public interface QueryParamNormalizer {
    Map<String, String[]> toNormalizedParamMap(NoticeQueryRequest req);
}