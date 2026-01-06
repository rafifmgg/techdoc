package com.ocmsintranet.cronservice.framework.workflows.jobreport.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ocmsintranet.cronservice.framework.workflows.jobreport.jobs.JobExecutionReportJob;
import com.ocmsintranet.cronservice.framework.workflows.jobreport.jobs.JobExecutionReportJob.JobReportResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for job execution report operations
 */
@RestController
@RequestMapping("/${api.version}/job-execution-report")
@Slf4j
@RequiredArgsConstructor
public class JobExecutionReportController {

    private final JobExecutionReportJob jobExecutionReportJob;
    
    /**
     * Generate and return a job execution report for the last 24 hours
     * 
     * @return ResponseEntity containing the report data and email status
     */
    @PostMapping("/generate-daily-report")
    public ResponseEntity<Map<String, Object>> getDailyJobReport() {
        log.info("REST request to get daily job execution report");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Execute the job to generate and send the report
            JobReportResult result = jobExecutionReportJob.sendJobExecutionReport();
            
            // Prepare response with email status and report data
            response.put("emailSent", result.isSuccess());
            response.put("message", result.getMessage());
            
            // Include the report data in the response
            response.put("reportData", result.getReportData());
            
            return result.isSuccess() 
                ? ResponseEntity.ok(response) 
                : ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            log.error("Error generating daily job execution report", e);
            response.put("error", "Error generating report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
