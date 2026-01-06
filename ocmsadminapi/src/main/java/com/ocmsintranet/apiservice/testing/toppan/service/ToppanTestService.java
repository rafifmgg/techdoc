package com.ocmsintranet.apiservice.testing.toppan.service;

import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadResponse;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadResponse;

/**
 * Service interface for Toppan testing operations
 */
public interface ToppanTestService {

    /**
     * Execute Toppan upload test with multiple steps
     *
     * @param request ToppanUploadRequest containing test parameters
     * @return ToppanUploadResponse with results from all steps
     */
    ToppanUploadResponse executeToppanUpload(ToppanUploadRequest request);

    /**
     * Execute Toppan download test with multiple steps
     *
     * @param request ToppanDownloadRequest containing test parameters
     * @return ToppanDownloadResponse with results from all steps
     */
    ToppanDownloadResponse executeToppanDownload(ToppanDownloadRequest request);
}
