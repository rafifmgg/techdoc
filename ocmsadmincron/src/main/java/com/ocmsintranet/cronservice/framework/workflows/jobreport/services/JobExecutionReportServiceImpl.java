package com.ocmsintranet.cronservice.framework.workflows.jobreport.services;

import com.ocmsintranet.cronservice.crud.beans.FindAllResponse;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of JobExecutionReportService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobExecutionReportServiceImpl implements JobExecutionReportService {

    private final OcmsBatchJobService ocmsBatchJobService;
    
    @Value("${spring.profiles.active:unknown}")
    private String environment;
    
    @Value("${server.name:unknown}")
    private String serverName;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter REPORT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
    
    /**
     * Generate a report of job executions for the last 24 hours
     * 
     * @param excludedJobNames List of job names to exclude from the report
     * @return Map containing the report data for template rendering
     */
    @Override
    public Map<String, Object> generateDailyJobReport(List<String> excludedJobNames) {
        log.info("Generating daily job execution report");
        
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        
        // Set end time to 5 PM today
        LocalDateTime endTime = now.withHour(17).withMinute(0).withSecond(0).withNano(0);
        if (now.isAfter(endTime)) {
            // If current time is after 5 PM, use current time as end time
            endTime = now;
        }
        
        // Set start time to 5 PM previous day
        LocalDateTime startTime = endTime.minusDays(1);
        
        // Create parameters for the query
        Map<String, String[]> params = new HashMap<>();
        params.put("$limit", new String[]{"9999"});
        params.put("creDate[$gte]", new String[]{startTime.toString()});
        params.put("creDate[$lte]", new String[]{endTime.toString()});
        
        log.info("Fetching job executions from {} to {}", startTime, endTime);
        
        // Get all job executions using the standard BaseService method
        FindAllResponse<com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob> response = 
            ocmsBatchJobService.getAll(params);
            
        List<com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob> jobExecutions = 
            response.getItems();
            
        // Apply additional filtering for excluded job names in memory
        if (excludedJobNames != null && !excludedJobNames.isEmpty()) {
            jobExecutions = jobExecutions.stream()
                .filter(job -> !excludedJobNames.contains(job.getName()))
                .collect(Collectors.toList());
        }
        
        log.info("Found {} job executions between {} and {}", jobExecutions.size(), startTime, endTime);
        
        // Count jobs by status
        long completedJobs = jobExecutions.stream()
            .filter(job -> "S".equals(job.getRunStatus()))  // Assuming 'S' is for completed
            .count();
            
        long failedJobs = jobExecutions.stream()
            .filter(job -> "F".equals(job.getRunStatus()))  // Assuming 'F' is for failed
            .count();
            
        long runningJobs = jobExecutions.stream()
            .filter(job -> "R".equals(job.getRunStatus()) || job.getEndRun() == null)  // Running if status is 'R' or end_run is null
            .count();
        
        // Format job executions for the report
        List<Map<String, String>> formattedJobs = jobExecutions.stream()
            .map(this::formatJobExecution)
            .collect(Collectors.toList());
        
        // Create the report data
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportDate", now.format(DATE_FORMATTER));
        reportData.put("startDate", startTime.format(REPORT_DATETIME_FORMATTER));
        reportData.put("endDate", endTime.format(REPORT_DATETIME_FORMATTER));
        reportData.put("totalJobs", jobExecutions.size());
        reportData.put("completedJobs", completedJobs);
        reportData.put("failedJobs", failedJobs);
        reportData.put("runningJobs", runningJobs);
        reportData.put("jobExecutions", formattedJobs);
        reportData.put("generatedTimestamp", now.format(TIME_FORMATTER));
        reportData.put("environment", environment);
        reportData.put("serverName", serverName);
        
        return reportData;
    }
    
    /**
     * Format a job execution for the report
     * 
     * @param job The job execution to format
     * @return Map containing formatted job execution data
     */
    private Map<String, String> formatJobExecution(com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob job) {
        Map<String, String> formattedJob = new HashMap<>();
        
        formattedJob.put("jobName", job.getName() != null ? job.getName() : "N/A");
        
        // Map status codes to user-friendly names
        String status = job.getRunStatus();
        String statusDisplay = "UNKNOWN";
        if (status != null) {
            switch (status) {
                case "S":
                    statusDisplay = "Success";
                    break;
                case "F":
                    statusDisplay = "Failed";
                    break;
                case "R":
                    statusDisplay = "Running";
                    break;
                default:
                    statusDisplay = status; // Return the code if not recognized
            }
        }
        formattedJob.put("status", statusDisplay);
        formattedJob.put("message", job.getLogText());
        formattedJob.put("startTime", job.getStartRun() != null ? job.getStartRun().format(TIME_FORMATTER) : "N/A");
        formattedJob.put("endTime", job.getEndRun() != null ? job.getEndRun().format(TIME_FORMATTER) : "N/A");
        
        // Calculate duration if both start and end times are available
        if (job.getStartRun() != null && job.getEndRun() != null) {
            Duration duration = Duration.between(job.getStartRun(), job.getEndRun());
            formattedJob.put("duration", String.format("%d min %d sec", 
                duration.toMinutes(), 
                duration.minusMinutes(duration.toMinutes()).getSeconds()));
        } else if (job.getStartRun() != null) {
            // If job is still running, calculate duration from start until now
            Duration duration = Duration.between(job.getStartRun(), LocalDateTime.now());
            formattedJob.put("duration", String.format("Running: %d min %d sec", 
                duration.toMinutes(), 
                duration.minusMinutes(duration.toMinutes()).getSeconds()));
        } else {
            formattedJob.put("duration", "N/A");
        }
        
        return formattedJob;
    }
}
