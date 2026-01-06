package com.ocmsintranet.apiservice.workflows.operational.cronproxy;

import java.util.Map;

import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for proxying requests to various cron jobs
 * Acts as a bridge between adminui and cron endpoints
 */
@RestController
@RequestMapping("/${api.version}/cron")
@RequiredArgsConstructor
@Slf4j
public class CronProxyController {

    private final CronProxyService cronProxyService;

    /**
     * Trigger the LTA enquiry process in the cron tier
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/lta-enquiry/trigger")
    public ResponseEntity<Map<String, Object>> triggerLtaEnquiry() {
        log.info("Received request to trigger LTA enquiry process");
        return cronProxyService.triggerLtaEnquiry();
    }

    /**
     * Trigger the LTA result process in the cron tier
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/lta-result/trigger")
    public ResponseEntity<Map<String, Object>> triggerLtaResult() {
        log.info("Received request to trigger LTA result process");
        return cronProxyService.triggerLtaResult();
    }

    /**
     * Trigger the MHA enquiry process in the cron tier
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/mha-enquiry/trigger")
    public ResponseEntity<Map<String, Object>> triggerMhaEnquiry() {
        log.info("Received request to trigger MHA enquiry process");
        return cronProxyService.triggerMhaEnquiry();
    }

    /**
     * Trigger the MHA result process in the cron tier
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/mha-result/trigger")
    public ResponseEntity<Map<String, Object>> triggerMhaResult() {
        log.info("Received request to trigger MHA result process");
        return cronProxyService.triggerMhaResult();
    }

    /**
     * Generate daily job execution report
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/job-report/trigger")
    public ResponseEntity<Map<String, Object>> generateDailyJobReport() {
        log.info("Received request to generate daily job execution report");
        return cronProxyService.generateDailyJobReport();
    }

    /**
     * Execute ENA reminder
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/ena-reminder/trigger")
    public ResponseEntity<Map<String, Object>> executeEnaReminder() {
        log.info("Received request to execute ENA reminder");
        return cronProxyService.executeEnaReminder();
    }

    /**
     * Retry failed ENA reminders
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/ena-retry/trigger")
    public ResponseEntity<Map<String, Object>> retryEnaReminder() {
        log.info("Received request to retry failed ENA reminders");
        return cronProxyService.retryEnaReminder();
    }
    
    /**
     * Trigger LTA daily report generation
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/lta-report-daily/trigger")
    public ResponseEntity<Map<String, Object>> triggerLtaReportDaily() {
        log.info("Received request to generate LTA daily report");
        return cronProxyService.triggerLtaReportDaily();
    }
    
    /**
     * Trigger MHA daily report generation
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/mha-report-daily/trigger")
    public ResponseEntity<Map<String, Object>> triggerMhaReportDaily() {
        log.info("Received request to generate MHA daily report");
        return cronProxyService.triggerMhaReportDaily();
    }
    
    /**
     * Trigger eNotification report generation
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/enotif-report/trigger")
    public ResponseEntity<Map<String, Object>> triggerEnotifReport() {
        log.info("Received request to trigger eNotification report");
        return cronProxyService.triggerEnotifReportDaily();
    }
    
    /**
     * Trigger Datahive contact info report generation
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/dhcontactinfo-report/trigger")
    public ResponseEntity<Map<String, Object>> triggerDhContactInfoReport() {
        log.info("Received request to trigger Datahive contact info report");
        return cronProxyService.triggerDhContactInfoReportDaily();
    }

    /**
     * Trigger Toppan download process
     *
     * @return Response from the cron tier
     */
    @PostMapping("/toppan-result/trigger")
    public ResponseEntity<Map<String, Object>> triggerToppanDownload() {
        log.info("Received request to trigger Toppan download process");
        return cronProxyService.triggerToppanDownload();
    }

    /**
     * Trigger Toppan letters generation process
     *
     * @return Response from the cron tier
     */
    @PostMapping("/toppan-enquiry/trigger")
    public ResponseEntity<Map<String, Object>> triggerToppanUpload() {
        log.info("Received request to trigger Toppan letters generation process");
        return cronProxyService.triggerToppanUpload();
    }
    
    /**
     * Trigger DataHive UEN/FIN sync process
     * 
     * @return Response from the cron tier
     */
    @PostMapping("/datahive-uen-fin/trigger")
    public ResponseEntity<Map<String, Object>> triggerDataHiveUenFin() {
        log.info("Received request to trigger DataHive UEN/FIN sync process");
        return cronProxyService.triggerDataHiveUenFin();
    }
}
