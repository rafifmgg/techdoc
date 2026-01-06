package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.mappers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.upload.dto.LtaUploadData;

// Removed unused import
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper class to convert database query results to LtaUploadData DTOs
 */
public class LtaUploadDataMapper {

    /**
     * Maps a list of Object[] from native query to LtaUploadData objects
     * 
     * @param results List of Object[] from native query
     * @return List of LtaUploadData objects
     */
    public static List<LtaUploadData> mapFromObjectArray(List<Object[]> results) {
        List<LtaUploadData> dataList = new ArrayList<>();
        
        if (results == null || results.isEmpty()) {
            return dataList;
        }
        
        for (Object[] row : results) {
            LtaUploadData data = new LtaUploadData();
            
            // Map fields from the query result to the DTO
            // For Appendix E format, we only need these 5 fields
            int i = 0;
            
            // From ocms_valid_offence_notice - only the fields returned by the repository query
            data.setNoticeNo(getString(row[i++]));         // 0: notice_no
            data.setVehicleNo(getString(row[i++]));        // 1: vehicle_no
            data.setNoticeDateAndTime(getLocalDateTime(row[i++])); // 2: notice_date_and_time
            data.setNextProcessingStage(getString(row[i++])); // 3: next_processing_stage
            data.setNextProcessingDate(getLocalDateTime(row[i++])); // 4: next_processing_date
            data.setSuspensionType(getString(row[i++])); // 5: suspension_type
            
            // All other fields will remain null/empty as they're not needed for Appendix E format
            
            dataList.add(data);
        }
        
        return dataList;
    }
    
    private static String getString(Object value) {
        return value == null ? "" : value.toString();
    }
    
    private static LocalDateTime getLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return null;
    }
}
