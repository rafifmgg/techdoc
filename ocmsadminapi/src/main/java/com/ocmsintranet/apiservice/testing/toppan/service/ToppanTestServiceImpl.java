package com.ocmsintranet.apiservice.testing.toppan.service;

import com.ocmsintranet.apiservice.testing.main.StepResult;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanDownloadResponse;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadRequest;
import com.ocmsintranet.apiservice.testing.toppan.dto.ToppanUploadResponse;
import com.ocmsintranet.apiservice.testing.toppan.helpers.StepExecutionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for Toppan testing operations
 * Executes multiple steps and always returns results for all steps
 */
@Service
@Slf4j
public class ToppanTestServiceImpl implements ToppanTestService {

    private final StepExecutionHelper stepExecutionHelper;

    public ToppanTestServiceImpl(StepExecutionHelper stepExecutionHelper) {
        this.stepExecutionHelper = stepExecutionHelper;
    }

    /**
     * Execute Toppan upload test with 4 steps
     * - Step 1: Create notices from scenario.json
     * - Step 2: Execute if Step 1 succeeds, otherwise skip
     * - Step 3: Execute if Step 2 succeeds, otherwise skip
     * - Step 4: Execute if Step 3 succeeds, otherwise skip
     *
     * @param request ToppanUploadRequest containing preload flag
     * @return ToppanUploadResponse with results from all steps (never throws exception)
     */
    @Override
    public ToppanUploadResponse executeToppanUpload(ToppanUploadRequest request) {
        log.info("Starting Toppan upload test with preload={}", request.getPreload());

        ToppanUploadResponse response = new ToppanUploadResponse();

        // Execute Step 1 (get scenario)
        StepResult step1Result = stepExecutionHelper.executeStep1();
        response.addStep(step1Result);

        // Execute Step 2 based on Step 1 result
        StepResult step2Result;
        if ("success".equals(step1Result.getStatus())) {
            // Step 1 succeeded, execute Step 2
            step2Result = stepExecutionHelper.executeStep2(request.getTriggerCron());
        } else {
            // Step 1 failed, skip Step 2
            step2Result = stepExecutionHelper.createSkippedStep(
                "Step 2",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step2Result);

        // Execute Step 3 based on Step 2 result
        StepResult step3Result;
        if ("success".equals(step2Result.getStatus())) {
            // Step 2 succeeded, execute Step 3
            // Extract notice numbers from Step 1 result
            List<String> noticeNumbers = extractNoticeNumbers(step1Result);
            step3Result = stepExecutionHelper.executeStep3(noticeNumbers);
        } else {
            // Step 2 failed or skipped, skip Step 3
            step3Result = stepExecutionHelper.createSkippedStep(
                "Step 3",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step3Result);

        // Execute Step 4 based on Step 3 result
        StepResult step4Result;
        if ("success".equals(step3Result.getStatus())) {
            // Step 3 succeeded, execute Step 4
            // Step 4 will read data from StepExecutionHelper's private field
            step4Result = stepExecutionHelper.executeStep4(request.getDetails());
        } else {
            // Step 3 failed or skipped, skip Step 4
            step4Result = stepExecutionHelper.createSkippedStep(
                "Step 4",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step4Result);

        log.info("Toppan upload test completed. Step 1: {}, Step 2: {}, Step 3: {}, Step 4: {}",
                step1Result.getStatus(), step2Result.getStatus(), step3Result.getStatus(), step4Result.getStatus());

        return response;
    }

    /**
     * Extract notice numbers from Step 1 result
     *
     * @param step1Result Step 1 result containing notice_no in json field
     * @return List of notice numbers, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    private List<String> extractNoticeNumbers(StepResult step1Result) {
        try {
            Object json = step1Result.getJson();
            if (json instanceof Map) {
                Map<String, Object> jsonMap = (Map<String, Object>) json;
                Object noticeNoObj = jsonMap.get("notice_no");
                if (noticeNoObj instanceof List) {
                    return (List<String>) noticeNoObj;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract notice numbers from Step 1: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Execute Toppan download test with 4 steps
     * - Step 1: Get download data from scenario.json
     * - Step 2: Execute if Step 1 succeeds, trigger cron
     * - Step 3: Execute if Step 2 succeeds, get valid offence notice data
     * - Step 4: Execute if Step 3 succeeds, verify download data
     *
     * @param request ToppanDownloadRequest containing triggerCron and details flags
     * @return ToppanDownloadResponse with results from all steps (never throws exception)
     */
    @Override
    public ToppanDownloadResponse executeToppanDownload(ToppanDownloadRequest request) {
        log.info("Starting Toppan download test with triggerCron={}, details={}",
                request.getTriggerCron(), request.getDetails());

        ToppanDownloadResponse response = new ToppanDownloadResponse();

        // Execute Step 1 (get download data)
        StepResult step1Result = stepExecutionHelper.executeDownloadStep1();
        response.addStep(step1Result);

        // Execute Step 2 based on Step 1 result
        StepResult step2Result;
        if ("success".equals(step1Result.getStatus())) {
            // Step 1 succeeded, execute Step 2
            step2Result = stepExecutionHelper.executeDownloadStep2(request.getTriggerCron());
        } else {
            // Step 1 failed, skip Step 2
            step2Result = stepExecutionHelper.createSkippedStep(
                "Step 2",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step2Result);

        // Execute Step 3 based on Step 2 result
        StepResult step3Result;
        if ("success".equals(step2Result.getStatus())) {
            // Step 2 succeeded, execute Step 3
            // Extract notice numbers from Step 1 result
            List<String> noticeNumbers = extractNoticeNumbers(step1Result);
            step3Result = stepExecutionHelper.executeDownloadStep3(noticeNumbers);
        } else {
            // Step 2 failed or skipped, skip Step 3
            step3Result = stepExecutionHelper.createSkippedStep(
                "Step 3",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step3Result);

        // Execute Step 4 based on Step 3 result
        StepResult step4Result;
        if ("success".equals(step3Result.getStatus())) {
            // Step 3 succeeded, execute Step 4
            step4Result = stepExecutionHelper.executeDownloadStep4(request.getDetails());
        } else {
            // Step 3 failed or skipped, skip Step 4
            step4Result = stepExecutionHelper.createSkippedStep(
                "Step 4",
                "skipped due to previous step failure"
            );
        }
        response.addStep(step4Result);

        log.info("Toppan download test completed. Step 1: {}, Step 2: {}, Step 3: {}, Step 4: {}",
                step1Result.getStatus(), step2Result.getStatus(), step3Result.getStatus(), step4Result.getStatus());

        return response;
    }
}
