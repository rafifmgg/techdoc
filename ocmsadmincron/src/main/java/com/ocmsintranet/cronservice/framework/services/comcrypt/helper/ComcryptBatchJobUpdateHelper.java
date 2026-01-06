package com.ocmsintranet.cronservice.framework.services.comcrypt.helper;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper class for updating batch job status from COMCRYPT callbacks.
 * Used to update batch job log_text and run_status when encryption succeeds or fails.
 */
@Slf4j
@Component
public class ComcryptBatchJobUpdateHelper {

    private final OcmsBatchJobRepository batchJobRepository;

    @Value("${cron.ces.upload.shedlock.name}")
    private String cesGenerateFilesJobName;

    public ComcryptBatchJobUpdateHelper(OcmsBatchJobRepository batchJobRepository) {
        this.batchJobRepository = batchJobRepository;
    }

    /**
     * Update batch job when encryption fails.
     * Determines batch job name based on requestId pattern and updates log_text.
     *
     * @param requestId The request ID (e.g., OCMSCES002_REQ_xxx)
     * @param encryptType The encryption type (PGP/SLIFT)
     */
    public void updateOnEncryptionFailure(String requestId, String encryptType) {
        if (requestId == null || requestId.isEmpty()) {
            log.warn("[BATCH-JOB-UPDATE] RequestId is null or empty, skipping update");
            return;
        }

        String batchJobName = resolveBatchJobName(requestId);
        if (batchJobName == null) {
            log.debug("[BATCH-JOB-UPDATE] No batch job mapping for requestId: {}, skipping update", requestId);
            return;
        }

        try {
            log.info("[BATCH-JOB-UPDATE] Updating batch job on encryption failure for requestId: {}", requestId);

            // Query latest batch job by name (DESC LIMIT 1)
            OcmsBatchJob batchJob = batchJobRepository.findLatestByNameToday(batchJobName);

            if (batchJob == null) {
                log.warn("[BATCH-JOB-UPDATE] No batch job found for today with name: {}", batchJobName);
                return;
            }

            log.info("[BATCH-JOB-UPDATE] Found batch job - id: {}, current log_text length: {}",
                    batchJob.getBatchJobId(),
                    batchJob.getLogText() != null ? batchJob.getLogText().length() : 0);

            // Append failure message to log_text
            String currentLogText = batchJob.getLogText() != null ? batchJob.getLogText() : "";
            String failureMessage = "\nencrypting " + encryptType + " : failed (requestId: " + requestId + ")";
            batchJob.setLogText(currentLogText + failureMessage);

            // Set run_status to F (Failed)
            batchJob.setRunStatus(SystemConstant.BatchJob.STATUS_FAILED);

            // Save updated batch job
            batchJobRepository.save(batchJob);

            log.info("[BATCH-JOB-UPDATE] Successfully updated batch job {} - appended: '{}', run_status: F",
                    batchJob.getBatchJobId(), failureMessage.trim());

        } catch (Exception e) {
            log.error("[BATCH-JOB-UPDATE] Error updating batch job for requestId: {}: {}", requestId, e.getMessage(), e);
        }
    }

    /**
     * Resolve batch job name based on requestId pattern.
     *
     * @param requestId The request ID (e.g., OCMSCES002_REQ_xxx)
     * @return Batch job name or null if no mapping found
     */
    private String resolveBatchJobName(String requestId) {
        String upperRequestId = requestId.toUpperCase();

        // CES workflow
        if (upperRequestId.contains("CES")) {
            return cesGenerateFilesJobName;
        }

        // Add more mappings here as needed (e.g., REPCCS, etc.)

        return null;
    }
}