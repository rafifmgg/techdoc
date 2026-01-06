package com.ocmsintranet.cronservice.framework.workflows.jobreport.services;

import java.util.List;
import java.util.Map;

/**
 * Service interface for job execution report generation
 */
public interface JobExecutionReportService {
    
    /**
     * Generate a report of job executions for the last 24 hours
     * 
     * @param excludedJobNames List of job names to exclude from the report
     * @return Map containing the report data for template rendering
     */
    Map<String, Object> generateDailyJobReport(List<String> excludedJobNames);
}
