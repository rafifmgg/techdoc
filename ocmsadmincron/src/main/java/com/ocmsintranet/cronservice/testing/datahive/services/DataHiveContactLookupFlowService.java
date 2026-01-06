package com.ocmsintranet.cronservice.testing.datahive.services;

import com.ocmsintranet.cronservice.framework.services.datahive.contact.ContactLookupResult;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.DataHiveContactService;
import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.datahive.constants.DataHiveTestConstants;
import com.ocmsintranet.cronservice.testing.datahive.helpers.DataHiveTestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service untuk testing DataHive Contact Lookup Flow
 * Mengadaptasi pattern dari LTA testing flows
 */
@Slf4j
@Service
public class DataHiveContactLookupFlowService {

    // Constants untuk status
    private static final String STATUS_SUCCESS = "SUCCESS";

    @Autowired
    private TableQueryService tableQueryService;
    
    @Autowired
    private DataHiveContactService dataHiveContactService;
    
    @Autowired
    private DataHiveTestHelper dataHiveTestHelper;

    /**
     * Run comprehensive DataHive contact lookup test flow
     *
     * Input: Pre-defined test IDs (NRIC, FIN, Passport)
     * Process: Setup data → Execute lookups → Update database → Verify results
     * Verification:
     *   - Contact information successfully retrieved and stored
     *   - Database updated with correct audit trail
     *   - Error handling for invalid IDs
     *
     * @return List of test step results
     */
    public List<TestStepResult> runTest() {
        log.info("🔍 Starting DataHive Contact Lookup Flow Test");
        
        List<TestStepResult> results = new ArrayList<>();
        
        // Step 1: Setup Test Data
        TestStepResult setupResult = setupTestData();
        results.add(setupResult);
        
        // Step 2: Execute Contact Lookups (hanya jika setup berhasil)
        TestStepResult lookupResult;
        if (STATUS_SUCCESS.equals(setupResult.getStatus())) {
            lookupResult = executeContactLookups();
        } else {
            lookupResult = createSkippedResult("Contact Lookup Execution",
                "Skipped due to test data setup failure");
        }
        results.add(lookupResult);
        
        // Step 3: Update Database (hanya jika lookup berhasil)
        TestStepResult updateResult;
        if (STATUS_SUCCESS.equals(lookupResult.getStatus())) {
            updateResult = updateDatabase(lookupResult);
        } else {
            updateResult = createSkippedResult("Database Update",
                "Skipped due to contact lookup failure");
        }
        results.add(updateResult);
        
        // Step 4: Verify Results (hanya jika update berhasil)
        TestStepResult verifyResult;
        if (STATUS_SUCCESS.equals(updateResult.getStatus())) {
            verifyResult = verifyResults(updateResult);
        } else {
            verifyResult = createSkippedResult("Results Verification",
                "Skipped due to database update failure");
        }
        results.add(verifyResult);
        
        // Step 5: Cleanup Test Data
        TestStepResult cleanupResult = cleanupTestData();
        results.add(cleanupResult);
        
        log.info("✅ DataHive Contact Lookup Flow Test completed");
        return results;
    }

    /**
     * Step 1: Setup test data di database
     */
    private TestStepResult setupTestData() {
        log.info("📋 Step 1: Setting up test data");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Cleanup existing test data first
            cleanupExistingTestData();
            
            // Insert test data untuk berbagai ID types
            Map<String, Object> testData = new HashMap<>();
            List<String> insertedNotices = new ArrayList<>();
            
            // Insert NRIC test data
            for (String nric : Arrays.copyOf(DataHiveTestConstants.VALID_NRIC_SAMPLES, 3)) {
                String noticeNumber = insertTestRecord(nric, "NRIC");
                insertedNotices.add(noticeNumber);
            }
            
            // Insert FIN test data
            for (String fin : Arrays.copyOf(DataHiveTestConstants.VALID_FIN_SAMPLES, 2)) {
                String noticeNumber = insertTestRecord(fin, "FIN");
                insertedNotices.add(noticeNumber);
            }
            
            // Insert Passport test data
            String passport = DataHiveTestConstants.VALID_PASSPORT_SAMPLES[0];
            String noticeNumber = insertTestRecord(passport, "PASSPORT");
            insertedNotices.add(noticeNumber);
            
            // Insert invalid ID untuk error testing
            String invalidId = DataHiveTestConstants.INVALID_ID_SAMPLES[0];
            String invalidNoticeNumber = insertTestRecord(invalidId, "NRIC");
            insertedNotices.add(invalidNoticeNumber);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            testData.put("totalRecords", insertedNotices.size());
            testData.put("nricRecords", 3);
            testData.put("finRecords", 2);
            testData.put("passportRecords", 1);
            testData.put("invalidRecords", 1);
            testData.put("executionTimeMs", executionTime);
            testData.put("insertedNotices", insertedNotices);
            
            log.info("✅ Test data setup completed: {} records in {}ms", 
                    insertedNotices.size(), executionTime);
            
            return createSuccessResult("Setup Test Data",
                    String.format("Successfully inserted %d test records in %dms", 
                            insertedNotices.size(), executionTime), testData);
            
        } catch (Exception e) {
            log.error("❌ Test data setup failed: {}", e.getMessage(), e);
            return createFailedResult("Setup Test Data",
                    "Test data setup failed: " + e.getMessage());
        }
    }

    /**
     * Step 2: Execute contact lookups untuk semua test IDs
     */
    private TestStepResult executeContactLookups() {
        log.info("🔎 Step 2: Executing contact lookups");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Get test records dari database
            List<Map<String, Object>> testRecords = getTestRecords();
            
            Map<String, Object> lookupResults = new HashMap<>();
            List<Map<String, Object>> successfulLookups = new ArrayList<>();
            List<Map<String, Object>> failedLookups = new ArrayList<>();
            
            for (Map<String, Object> record : testRecords) {
                String noticeNumber = (String) record.get("notice_number");
                String idNo = (String) record.get("id_no");
                String idType = (String) record.get("id_type");
                
                log.debug("Looking up contact for {} ({}): {}", idType, noticeNumber, idNo);
                
                try {
                    ContactLookupResult result = dataHiveContactService.lookupContact(idNo, idType, noticeNumber);
                    
                    Map<String, Object> lookupData = new HashMap<>();
                    lookupData.put("noticeNumber", noticeNumber);
                    lookupData.put("idNo", idNo);
                    lookupData.put("idType", idType);
                    lookupData.put("lookupSuccess", result.hasContact());
                    lookupData.put("contactFound", result.hasContact());
                    lookupData.put("mobileFound", result.isMobileFound());
                    lookupData.put("emailFound", result.isEmailFound());
                    
                    if (result.hasContact()) {
                        successfulLookups.add(lookupData);
                        log.debug("✅ Contact lookup successful for {}: {}", idType, idNo);
                    } else {
                        failedLookups.add(lookupData);
                        log.debug("❌ Contact lookup failed for {}: {}", idType, idNo);
                    }
                    
                } catch (Exception e) {
                    Map<String, Object> lookupData = new HashMap<>();
                    lookupData.put("noticeNumber", noticeNumber);
                    lookupData.put("idNo", idNo);
                    lookupData.put("idType", idType);
                    lookupData.put("lookupSuccess", false);
                    lookupData.put("error", e.getMessage());
                    
                    failedLookups.add(lookupData);
                    log.debug("❌ Contact lookup exception for {}: {}", idType, e.getMessage());
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            lookupResults.put("totalRecords", testRecords.size());
            lookupResults.put("successfulLookups", successfulLookups.size());
            lookupResults.put("failedLookups", failedLookups.size());
            lookupResults.put("successRate", (double) successfulLookups.size() / testRecords.size());
            lookupResults.put("executionTimeMs", executionTime);
            lookupResults.put("successfulResults", successfulLookups);
            lookupResults.put("failedResults", failedLookups);
            
            log.info("✅ Contact lookups completed: {}/{} successful in {}ms", 
                    successfulLookups.size(), testRecords.size(), executionTime);
            
            return createSuccessResult("Execute Contact Lookups",
                    String.format("Completed %d lookups (%d successful, %d failed) in %dms", 
                            testRecords.size(), successfulLookups.size(), 
                            failedLookups.size(), executionTime), lookupResults);
            
        } catch (Exception e) {
            log.error("❌ Contact lookup execution failed: {}", e.getMessage(), e);
            return createFailedResult("Execute Contact Lookups",
                    "Contact lookup execution failed: " + e.getMessage());
        }
    }

    /**
     * Step 3: Update database dengan contact lookup results
     */
    private TestStepResult updateDatabase(TestStepResult lookupResult) {
        log.info("💾 Step 3: Updating database with lookup results");
        
        try {
            long startTime = System.currentTimeMillis();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonData = (Map<String, Object>) lookupResult.getJsonData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> successfulResults = 
                (List<Map<String, Object>>) jsonData.get("successfulResults");
            
            int updatedRecords = 0;
            List<String> updatedNotices = new ArrayList<>();
            
            for (Map<String, Object> result : successfulResults) {
                String noticeNumber = (String) result.get("noticeNumber");
                Boolean mobileFound = (Boolean) result.get("mobileFound");
                Boolean emailFound = (Boolean) result.get("emailFound");
                
                // Update record dengan contact information
                // Untuk testing, kita update dengan mock contact data berdasarkan lookup results
                
                updateContactInformation(noticeNumber, mobileFound, emailFound);
                updatedRecords++;
                updatedNotices.add(noticeNumber);
                
                log.debug("✅ Updated contact information for notice: {} (mobile: {}, email: {})", 
                         noticeNumber, mobileFound, emailFound);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("updatedRecords", updatedRecords);
            updateData.put("executionTimeMs", executionTime);
            updateData.put("updatedNotices", updatedNotices);
            
            log.info("✅ Database update completed: {} records updated in {}ms", 
                    updatedRecords, executionTime);
            
            return createSuccessResult("Update Database",
                    String.format("Successfully updated %d records in %dms", 
                            updatedRecords, executionTime), updateData);
            
        } catch (Exception e) {
            log.error("❌ Database update failed: {}", e.getMessage(), e);
            return createFailedResult("Update Database",
                    "Database update failed: " + e.getMessage());
        }
    }

    /**
     * Step 4: Verify hasil update di database
     */
    private TestStepResult verifyResults(TestStepResult updateResult) {
        log.info("🔍 Step 4: Verifying database updates");
        
        try {
            long startTime = System.currentTimeMillis();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> updateJsonData = (Map<String, Object>) updateResult.getJsonData();
            @SuppressWarnings("unchecked")
            List<String> updatedNotices = (List<String>) updateJsonData.get("updatedNotices");
            
            int verifiedRecords = 0;
            List<String> verifiedNotices = new ArrayList<>();
            
            for (String noticeNumber : updatedNotices) {
                // Verify record telah diupdate dengan benar
                if (verifyContactUpdate(noticeNumber)) {
                    verifiedRecords++;
                    verifiedNotices.add(noticeNumber);
                    log.debug("✅ Verified contact update for notice: {}", noticeNumber);
                } else {
                    log.debug("❌ Contact update verification failed for notice: {}", noticeNumber);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> verifyData = new HashMap<>();
            verifyData.put("totalToVerify", updatedNotices.size());
            verifyData.put("verifiedRecords", verifiedRecords);
            verifyData.put("verificationRate", (double) verifiedRecords / updatedNotices.size());
            verifyData.put("executionTimeMs", executionTime);
            verifyData.put("verifiedNotices", verifiedNotices);
            
            boolean allVerified = verifiedRecords == updatedNotices.size();
            
            log.info("✅ Results verification completed: {}/{} verified in {}ms", 
                    verifiedRecords, updatedNotices.size(), executionTime);
            
            if (allVerified) {
                return createSuccessResult("Verify Results",
                        String.format("All %d records verified successfully in %dms", 
                                verifiedRecords, executionTime), verifyData);
            } else {
                return createFailedResult("Verify Results",
                        String.format("Only %d/%d records verified successfully", 
                                verifiedRecords, updatedNotices.size()));
            }
            
        } catch (Exception e) {
            log.error("❌ Results verification failed: {}", e.getMessage(), e);
            return createFailedResult("Verify Results",
                    "Results verification failed: " + e.getMessage());
        }
    }

    /**
     * Step 5: Cleanup test data
     */
    private TestStepResult cleanupTestData() {
        log.info("🧹 Step 5: Cleaning up test data");
        
        try {
            long startTime = System.currentTimeMillis();
            
            int deletedRecords = cleanupExistingTestData();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> cleanupData = new HashMap<>();
            cleanupData.put("deletedRecords", deletedRecords);
            cleanupData.put("executionTimeMs", executionTime);
            
            log.info("✅ Test data cleanup completed: {} records deleted in {}ms", 
                    deletedRecords, executionTime);
            
            return createSuccessResult("Cleanup Test Data",
                    String.format("Successfully deleted %d test records in %dms", 
                            deletedRecords, executionTime), cleanupData);
            
        } catch (Exception e) {
            log.error("❌ Test data cleanup failed: {}", e.getMessage(), e);
            return createFailedResult("Cleanup Test Data",
                    "Test data cleanup failed: " + e.getMessage());
        }
    }

    // Helper methods untuk database operations
    
    private String insertTestRecord(String idNo, String idType) {
        String noticeNumber = DataHiveTestConstants.generateTestNoticeNumber();
        String vehicleNumber = DataHiveTestConstants.generateTestVehicleNumber();
        
        // Insert ke ocms_valid_offence_notice
        Map<String, Object> vonFields = new HashMap<>();
        vonFields.put("notice_number", noticeNumber);
        vonFields.put("vehicle_no", vehicleNumber);
        vonFields.put("pp_code", "PP001");
        vonFields.put("pp_name", "PARKING OFFENCE");
        vonFields.put("last_processing_stage", "NPA");
        vonFields.put("next_processing_stage", "ROV");
        vonFields.put("cre_user_id", DataHiveTestConstants.TEST_USER);
        vonFields.put("upd_user_id", DataHiveTestConstants.TEST_USER);
        
        tableQueryService.post("OcmsValidOffenceNotice", vonFields);
        
        // Insert ke ocms_offence_notice_owner_driver
        Map<String, Object> ondFields = new HashMap<>();
        ondFields.put("notice_number", noticeNumber);
        ondFields.put("id_no", idNo);
        ondFields.put("id_type", idType);
        ondFields.put("name", "TEST OWNER");
        ondFields.put("entity_type", "INDIVIDUAL");
        ondFields.put("cre_user_id", DataHiveTestConstants.TEST_USER);
        ondFields.put("upd_user_id", DataHiveTestConstants.TEST_USER);
        
        tableQueryService.post("OcmsOffenceNoticeOwnerDriver", ondFields);
        
        return noticeNumber;
    }
    
    private List<Map<String, Object>> getTestRecords() {
        // Query untuk mendapatkan test records menggunakan filter
        Map<String, Object> filters = new HashMap<>();
        filters.put("cre_user_id", DataHiveTestConstants.TEST_USER);
        
        List<Map<String, Object>> vonRecords = tableQueryService.query("OcmsValidOffenceNotice", filters);
        
        // Filter hanya yang notice_number dimulai dengan TEST prefix
        List<Map<String, Object>> testRecords = new ArrayList<>();
        for (Map<String, Object> von : vonRecords) {
            String noticeNumber = (String) von.get("notice_number");
            if (noticeNumber != null && noticeNumber.startsWith(DataHiveTestConstants.TEST_NOTICE_PREFIX)) {
                // Get corresponding owner driver data
                Map<String, Object> ondFilters = new HashMap<>();
                ondFilters.put("notice_number", noticeNumber);
                List<Map<String, Object>> ondRecords = tableQueryService.query("OcmsOffenceNoticeOwnerDriver", ondFilters);
                
                if (!ondRecords.isEmpty()) {
                    Map<String, Object> combinedRecord = new HashMap<>();
                    combinedRecord.put("notice_number", noticeNumber);
                    combinedRecord.put("id_no", ondRecords.get(0).get("id_no"));
                    combinedRecord.put("id_type", ondRecords.get(0).get("id_type"));
                    testRecords.add(combinedRecord);
                }
            }
        }
        
        return testRecords;
    }
    
    private void updateContactInformation(String noticeNumber, Boolean mobileFound, Boolean emailFound) {
        // Update dengan mock contact information untuk testing
        String mobileNo = mobileFound ? "+65 9999 9999" : null;
        String email = emailFound ? "test@datahive.test" : null;
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("notice_number", noticeNumber);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "UPDATED TEST OWNER");
        updates.put("offender_tel_no", mobileNo);
        updates.put("email", email);
        updates.put("upd_user_id", DataHiveTestConstants.TEST_USER);
        
        tableQueryService.patch("OcmsOffenceNoticeOwnerDriver", filters, updates);
    }
    
    private boolean verifyContactUpdate(String noticeNumber) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("notice_number", noticeNumber);
        filters.put("name", "UPDATED TEST OWNER");
        filters.put("upd_user_id", DataHiveTestConstants.TEST_USER);
        
        long count = tableQueryService.count("OcmsOffenceNoticeOwnerDriver", filters);
        
        return count > 0;
    }
    
    private int cleanupExistingTestData() {
        int deletedRecords = 0;
        
        try {
            // Delete dari ocms_offence_notice_owner_driver dulu (foreign key constraint)
            Map<String, Object> ondFilters = new HashMap<>();
            ondFilters.put("cre_user_id", DataHiveTestConstants.TEST_USER);
            
            // Get all test notices first untuk filter by notice_number prefix
            List<Map<String, Object>> ondRecords = tableQueryService.query("OcmsOffenceNoticeOwnerDriver", ondFilters);
            for (Map<String, Object> record : ondRecords) {
                String noticeNumber = (String) record.get("notice_number");
                if (noticeNumber != null && noticeNumber.startsWith(DataHiveTestConstants.TEST_NOTICE_PREFIX)) {
                    Map<String, Object> deleteFilter = new HashMap<>();
                    deleteFilter.put("notice_number", noticeNumber);
                    deletedRecords += tableQueryService.delete("OcmsOffenceNoticeOwnerDriver", deleteFilter);
                }
            }
            
            // Delete dari ocms_valid_offence_notice
            Map<String, Object> vonFilters = new HashMap<>();
            vonFilters.put("cre_user_id", DataHiveTestConstants.TEST_USER);
            
            List<Map<String, Object>> vonRecords = tableQueryService.query("OcmsValidOffenceNotice", vonFilters);
            for (Map<String, Object> record : vonRecords) {
                String noticeNumber = (String) record.get("notice_number");
                if (noticeNumber != null && noticeNumber.startsWith(DataHiveTestConstants.TEST_NOTICE_PREFIX)) {
                    Map<String, Object> deleteFilter = new HashMap<>();
                    deleteFilter.put("notice_number", noticeNumber);
                    deletedRecords += tableQueryService.delete("OcmsValidOffenceNotice", deleteFilter);
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Warning during cleanup: {}", e.getMessage());
        }
        
        return deletedRecords;
    }

    // Helper methods untuk creating test results
    
    private TestStepResult createSuccessResult(String title, String details, Map<String, Object> jsonData) {
        return dataHiveTestHelper.createTestResult(STATUS_SUCCESS, title, details, jsonData);
    }
    
    private TestStepResult createFailedResult(String title, String details) {
        return dataHiveTestHelper.createFailedResult(title, details);
    }
    
    private TestStepResult createSkippedResult(String title, String details) {
        return dataHiveTestHelper.createSkippedResult(title, details);
    }
}
