package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.helpers;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.dto.LtaUploadData;
import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.mappers.LtaUploadDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Removed unused import
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// Removed unused import
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for LTA upload operations
 * Uses direct repository access for queries (avoids [$in] issues)
 * Uses TableQueryService for updates (since it was working)
 */
@Slf4j
@Component
public class LtaUploadHelper {

    private final OcmsValidOffenceNoticeRepository repository;
    private final TableQueryService tableQueryService;

    @Autowired
    public LtaUploadHelper(OcmsValidOffenceNoticeRepository repository, TableQueryService tableQueryService) {
        this.repository = repository;
        this.tableQueryService = tableQueryService;
    }

    /**
     * Query offence notice data for LTA upload using direct repository access
     * This uses native SQL queries and bypasses TableQueryService filtering issues
     * 
     * @return List of offence notices ready for LTA upload
     */
    public List<Map<String, Object>> queryLtaUploadData() {
        log.info("Querying LTA upload data for current date and ROV processing stage");
        log.info("===== REPOSITORY QUERY PARAMETERS =====");
        log.info("nextProcessingStage: ROV (hardcoded in SQL)");
        log.info("nextProcessingDate <= GETDATE() (hardcoded in SQL)");
        log.info("offenceNoticeType IN: ['O', 'E'] (hardcoded in SQL)");
        log.info("=====================================");
        
        try {
            // Use the repository's comprehensive query (no parameters needed)
            List<Object[]> queryResults = repository.findComprehensiveLtaUploadData();
            
            log.info("===== REPOSITORY QUERY RESULTS =====");
            log.info("Retrieved {} records from database", queryResults.size());
            
            // Convert query results to DTOs
            List<LtaUploadData> ltaUploadDataList = LtaUploadDataMapper.mapFromObjectArray(queryResults);
            
            log.info("===== FINAL RESULTS =====");
            log.info("Records to be processed: {}", ltaUploadDataList.size());
            
            if (!ltaUploadDataList.isEmpty()) {
                log.info("Records to be processed:");
                for (int i = 0; i < ltaUploadDataList.size(); i++) {
                    LtaUploadData data = ltaUploadDataList.get(i);
                    log.info("  #{}: noticeNo={}, vehicleNo={}, noticeDateAndTime={}", 
                            i + 1, data.getNoticeNo(), data.getVehicleNo(), data.getNoticeDateAndTime());
                
                    log.info("  Processing info for notice {}: stage={}, date={}",
                            data.getNoticeNo(),
                            data.getNextProcessingStage(),
                            data.getNextProcessingDate());
                }
            }
            log.info("===================================");
            
            // Convert DTOs to maps for compatibility with existing code
            return ltaUploadDataList.stream()
                .map(this::convertDtoToMap)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error querying LTA upload data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query LTA upload data", e);
        }
    }
    
    /**
     * Convert DTO to map for compatibility with existing code
     * Include all fields required for the LTA file format
     * 
     * @param dto The LtaUploadData DTO to convert
     * @return Map representation of the DTO with all required fields for LTA file
     */
    private Map<String, Object> convertDtoToMap(LtaUploadData dto) {
        Map<String, Object> map = new HashMap<>();
        // DateTimeFormatter for time only, date formatter removed as it's no longer needed
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");
        
        // For Appendix E format, we only need these basic fields
        // Basic notice information
        map.put("noticeNo", dto.getNoticeNo());
        // Include suspensionType so downstream updateProcessingStage can use it
        map.put("suspensionType", dto.getSuspensionType());
        map.put("nextProcessingStage", dto.getNextProcessingStage());
        map.put("nextProcessingDate", dto.getNextProcessingDate());
        
        // Vehicle information
        map.put("vehicleNo", dto.getVehicleNo());
        
        // Offence information
        LocalDateTime noticeDateAndTime = dto.getNoticeDateAndTime() != null ? dto.getNoticeDateAndTime() : LocalDateTime.now();
        map.put("noticeDateAndTime", noticeDateAndTime);
        map.put("offenceDate", noticeDateAndTime.toLocalDate());
        map.put("offenceTime", noticeDateAndTime.format(timeFormatter));
        
        // For Appendix E format, we don't need additional fields
        
        return map;
    }
    
    /**
     * Update processing stage for processed offence notices
     * Uses TableQueryService to update the database (since this was working)
     * 
     * @param processedNotices List of processed offence notices
     */
    public void updateProcessingStage(List<Map<String, Object>> processedNotices) {
        log.info("Updating processing stage for {} processed notices using TableQueryService", processedNotices.size());
        
        try {
            for (Map<String, Object> notice : processedNotices) {
                String noticeNo = (String) notice.get("noticeNo");
                
                try {
                    // Create filter to find the specific record
                    Map<String, Object> filters = new HashMap<>();
                    filters.put("noticeNo", noticeNo);
                    
                    // Create update fields with specific values as requested
                    Map<String, Object> updateFields = new HashMap<>();
                    LocalDateTime currentDate = LocalDateTime.now();
                    
                    // First, query the current record to retrieve lastProcessingDate
                    List<Map<String, Object>> currentRecords = tableQueryService.query(
                        "ocms_valid_offence_notice", 
                        filters
                    );
                    
                    Object oldLastProcessingDate = currentDate; // Default to current date
                    if (!currentRecords.isEmpty()) {
                        Map<String, Object> currentRecord = currentRecords.get(0);
                        if (currentRecord.get("lastProcessingDate") != null) {
                            oldLastProcessingDate = currentRecord.get("lastProcessingDate");
                            log.info("Retrieved old lastProcessingDate: {} for notice {}", oldLastProcessingDate, noticeNo);
                        }
                    }
                    
                    // Use the specific field values provided
                    updateFields.put("prevProcessingStage", SystemConstant.SuspensionReason.NPA);
                    
                    // Set prevProcessingDate to the old value of lastProcessingDate
                    updateFields.put("prevProcessingDate", oldLastProcessingDate);
                    
                    // Set other fields as required
                    updateFields.put("lastProcessingStage", SystemConstant.SuspensionReason.ROV);

                    updateFields.put("lastProcessingDate", currentDate);
                    updateFields.put("nextProcessingDate", currentDate);

                    // Normalize suspensionType: handle non-String values, trim whitespace, and log for debugging
                    Object stObj = notice.get("suspensionType");
                    String suspensionType = null;
                    if (stObj != null) {
                        suspensionType = stObj.toString().trim();
                        log.debug("suspensionType for notice {} = '{}' (class={})", noticeNo, suspensionType, stObj.getClass().getName());
                    } else {
                        log.debug("suspensionType is null for notice {}", noticeNo);
                    }

                    // Constant-first equals with normalized string avoids NPEs and handles non-String map values
                    if (SystemConstant.SuspensionType.TEMPORARY.equals(suspensionType)) {
                        updateFields.put("nextProcessingStage", SystemConstant.SuspensionReason.RD1);
                    } else {
                        updateFields.put("nextProcessingStage", SystemConstant.SuspensionReason.ENA);
                    }
                                        
                    log.info("Updating notice {} with specific processing stage values: prev=NPA, last=ROV, next=ENA", noticeNo);
                    
                    // Update using TableQueryService (this was working)
                    List<Map<String, Object>> updated = tableQueryService.patch(
                        "ocms_valid_offence_notice", 
                        filters, 
                        updateFields
                    );
                    
                    if (updated.isEmpty()) {
                        log.warn("No records updated for noticeNo: {}", noticeNo);
                    } else {
                        log.debug("Successfully updated processing stage for noticeNo: {}", noticeNo);
                    }
                    
                } catch (Exception e) {
                    log.error("Error updating processing stage for noticeNo {}: {}", noticeNo, e.getMessage(), e);
                    // Continue with other records even if one fails
                }
            }
            
            log.info("Completed updating processing stage for processed notices using TableQueryService");
            
        } catch (Exception e) {
            log.error("Error updating processing stages: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update processing stages", e);
        }
    }
}
