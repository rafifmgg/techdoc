package com.ocmsintranet.cronservice.testing.agencies.toppan.helpers;

import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Toppan test data operations
 */
@Component
@Slf4j
public class ToppanTestDataHelper {

    private static final String TEST_USER = "TOPPAN_TEST";
    private static final DateTimeFormatter DB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TableQueryService tableQueryService;
    
    /**
     * Clean up existing test data
     */
    public void cleanupTestData() {
        try {
            // Delete from ocms_offence_notice_owner_driver
            Map<String, Object> filter = new HashMap<>();
            filter.put("notice_no[$like]", "TPTEST%");
            tableQueryService.delete("ocms_offence_notice_owner_driver", filter);
            log.info("Cleaned up ocms_offence_notice_owner_driver");
            
            // Delete from ocms_valid_offence_notice
            tableQueryService.delete("ocms_valid_offence_notice", filter);
            log.info("Cleaned up ocms_valid_offence_notice");
            
            // Delete from ocms_nro_temp
            tableQueryService.delete("ocms_nro_temp", filter);
            log.info("Cleaned up ocms_nro_temp");
            
            // Delete from ocms_hst
            Map<String, Object> hstFilter = new HashMap<>();
            hstFilter.put("id_no[$like]", "TPTEST%");
            tableQueryService.delete("ocms_hst", hstFilter);
            log.info("Cleaned up ocms_hst");
            
            log.info("Successfully cleaned up all existing test data");
        } catch (Exception e) {
            log.error("Error cleaning up test data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clean up test data", e);
        }
    }
    
    /**
     * Insert test records into ocms_valid_offence_notice
     */
    public void insertValidOffenceNotices(String stage) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Valid records that should be processed
            insertValidOffenceNotice("TPTEST001", "SXX1234A", stage, now, null, "O");
            insertValidOffenceNotice("TPTEST002", "SXX5678B", stage, now, null, "O");
            insertValidOffenceNotice("TPTEST005", "SXX7890E", stage, now, null, "O");
            
            // Invalid records with future processing date
            insertValidOffenceNotice("TPTEST003", "SXX9012C", stage, now.plusDays(30), null, "O");
            
            // Invalid records with wrong processing stage
            insertValidOffenceNotice("TPTEST004", "SXX3456D", "XX", now, null, "O");
            
            // Invalid records with suspension type
            insertValidOffenceNotice("TPTEST006", "SXX1111F", stage, now, "S", "O");
            
            // Will be excluded by NRO temp
            insertValidOffenceNotice("TPTEST007", "SXX2222G", stage, now, null, "O");
            
            // Will be excluded by HST
            insertValidOffenceNotice("TPTEST008", "SXX3333H", stage, now, null, "O");
            
            // Will be excluded by wrong offender_indicator
            insertValidOffenceNotice("TPTEST009", "SXX4444I", stage, now, null, "D");
            
            // Will be excluded by wrong id_type
            insertValidOffenceNotice("TPTEST010", "SXX5555J", stage, now, null, "O");
            
            log.info("Successfully inserted test records into ocms_valid_offence_notice");
        } catch (Exception e) {
            log.error("Error inserting test records into ocms_valid_offence_notice: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert test records", e);
        }
    }
    
    /**
     * Insert a single record into ocms_valid_offence_notice
     */
    private void insertValidOffenceNotice(String noticeNo, String vehicleNo, String stage, 
                                         LocalDateTime processingDate, String suspensionType, String offenderIndicator) {
        Map<String, Object> record = new HashMap<>();
        record.put("notice_no", noticeNo);
        record.put("vehicle_no", vehicleNo);
        record.put("last_processing_stage", stage);
        record.put("next_processing_stage", stage);
        record.put("last_processing_date", processingDate.format(DB_DATE_FORMAT));
        record.put("offence_date", LocalDateTime.now().minusDays(30).format(DB_DATE_FORMAT));
        record.put("pp_code", "PP001");
        record.put("pp_name", "PARKING OFFENCE"); // Mandatory field
        record.put("offender_indicator", offenderIndicator); // O for owner, D for driver
        record.put("cre_user_id", TEST_USER);
        record.put("cre_date", LocalDateTime.now().format(DB_DATE_FORMAT));
        record.put("upd_user_id", TEST_USER);
        record.put("upd_date", LocalDateTime.now().format(DB_DATE_FORMAT));
        
        if (suspensionType != null) {
            record.put("suspension_type", suspensionType);
        }
        
        tableQueryService.post("ocms_valid_offence_notice", record);
    }
    
    /**
     * Insert test records into ocms_offence_notice_owner_driver
     */
    public void insertOwnerDriverRecords() {
        try {
            // For TPTEST001, TPTEST002, TPTEST005 - Valid records
            insertOwnerDriverRecord("TPTEST001", "NRIC", "S1234567A", "P", "John Doe");
            insertOwnerDriverRecord("TPTEST002", "NRIC", "S7654321B", "P", "Jane Smith");
            insertOwnerDriverRecord("TPTEST005", "NRIC", "S9876543C", "P", "Alice Johnson");
            
            // For TPTEST003 - Future processing date
            insertOwnerDriverRecord("TPTEST003", "NRIC", "S2468135D", "P", "Bob Williams");
            
            // For TPTEST004 - Wrong processing stage
            insertOwnerDriverRecord("TPTEST004", "NRIC", "S1357924E", "P", "Charlie Brown");
            
            // For TPTEST006 - Suspension type
            insertOwnerDriverRecord("TPTEST006", "NRIC", "S9753124F", "P", "David Miller");
            
            // For TPTEST007 - Will be excluded by NRO temp
            insertOwnerDriverRecord("TPTEST007", "NRIC", "S3692581G", "P", "Eva Davis");
            
            // For TPTEST008 - Will be excluded by HST
            insertOwnerDriverRecord("TPTEST008", "NRIC", "S1472583H", "P", "Frank Wilson");
            
            // For TPTEST009 - Wrong offender_indicator
            insertOwnerDriverRecord("TPTEST009", "NRIC", "S2583691I", "P", "Grace Taylor");
            
            // For TPTEST010 - Wrong id_type
            insertOwnerDriverRecord("TPTEST010", "UEN", "TPTEST010", "C", "Test Company");
            
            log.info("Successfully inserted test records into ocms_offence_notice_owner_driver");
        } catch (Exception e) {
            log.error("Error inserting test records into ocms_offence_notice_owner_driver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert owner/driver records", e);
        }
    }
    
    /**
     * Insert a single record into ocms_offence_notice_owner_driver
     */
    private void insertOwnerDriverRecord(String noticeNo, String idType, String idNo, String entityType, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("notice_no", noticeNo);
        record.put("id_type", idType);
        record.put("id_no", idNo);
        record.put("entity_type", entityType); // P for person, C for company
        record.put("name", name);
        record.put("cre_user_id", TEST_USER);
        record.put("cre_date", LocalDateTime.now().format(DB_DATE_FORMAT));
        record.put("upd_user_id", TEST_USER);
        record.put("upd_date", LocalDateTime.now().format(DB_DATE_FORMAT));
        
        tableQueryService.post("ocms_offence_notice_owner_driver", record);
    }
    
    /**
     * Insert exclusion records
     */
    public void insertExclusionRecords() {
        try {
            // Insert into ocms_nro_temp to exclude TPTEST007
            Map<String, Object> nroRecord = new HashMap<>();
            nroRecord.put("notice_no", "TPTEST007");
            nroRecord.put("cre_user_id", TEST_USER);
            nroRecord.put("cre_date", LocalDateTime.now().format(DB_DATE_FORMAT));
            tableQueryService.post("ocms_nro_temp", nroRecord);
            
            // Insert into ocms_hst to exclude TPTEST008
            Map<String, Object> hstRecord = new HashMap<>();
            hstRecord.put("id_no", "S1472583H"); // ID for TPTEST008
            hstRecord.put("id_type", "NRIC");
            hstRecord.put("cre_user_id", TEST_USER);
            hstRecord.put("cre_date", LocalDateTime.now().format(DB_DATE_FORMAT));
            tableQueryService.post("ocms_hst", hstRecord);
            
            log.info("Successfully inserted exclusion records");
        } catch (Exception e) {
            log.error("Error inserting exclusion records: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert exclusion records", e);
        }
    }
    
    /**
     * Query test notices
     */
    public List<Map<String, Object>> queryTestNotices() {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("notice_no[$like]", "TPTEST%");
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_valid_offence_notice", filter);
            log.info("Found {} test notices", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error querying test notices: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Query test owner/driver records
     */
    public List<Map<String, Object>> queryTestOwnerDrivers() {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("notice_no[$like]", "TPTEST%");
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_offence_notice_owner_driver", filter);
            log.info("Found {} test owner/driver records", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error querying test owner/driver records: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Query processed notices after Toppan upload
     */
    public List<Map<String, Object>> queryProcessedNotices(String stage) {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("notice_no[$like]", "TPTEST%");
            filter.put("last_processing_stage", stage);
            
            List<Map<String, Object>> results = tableQueryService.query("ocms_valid_offence_notice", filter);
            log.info("Found {} processed notices for stage {}", results.size(), stage);
            return results;
        } catch (Exception e) {
            log.error("Error querying processed notices: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
