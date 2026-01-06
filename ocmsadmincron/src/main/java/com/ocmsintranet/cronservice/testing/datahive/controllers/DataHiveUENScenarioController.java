package com.ocmsintranet.cronservice.testing.datahive.controllers;

import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenCompleteSuccessFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenRegistrationOnlyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenDeregisteredOnlyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenShareholderOnlyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenBoardOnlyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenMixedCompanyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenMixedCorporateFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenNotFoundFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenUpdateCompanyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenUpdateAddressFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenUpdateShareholderFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenUpdateBoardFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenListedCompanyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenNonListedCompanyFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenTsAcrSuccessFlowService;
import com.ocmsintranet.cronservice.testing.datahive.services.DataHiveUenTsAcrFailureFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test/datahive/uen")
public class DataHiveUENScenarioController {

    @Value("${test.endpoints.enabled:true}")
    private boolean testEndpointsEnabled;

    private final DataHiveUenCompleteSuccessFlowService uenCompleteFlowService;
    private final DataHiveUenRegistrationOnlyFlowService uenRegistrationOnlyFlowService;
    private final DataHiveUenDeregisteredOnlyFlowService uenDeregisteredOnlyFlowService;
    private final DataHiveUenShareholderOnlyFlowService uenShareholderOnlyFlowService;
    private final DataHiveUenBoardOnlyFlowService uenBoardOnlyFlowService;
    private final DataHiveUenMixedCompanyFlowService uenMixedCompanyFlowService;
    private final DataHiveUenMixedCorporateFlowService uenMixedCorporateFlowService;
    private final DataHiveUenNotFoundFlowService uenNotFoundFlowService;
    private final DataHiveUenUpdateCompanyFlowService uenUpdateCompanyFlowService;
    private final DataHiveUenUpdateAddressFlowService uenUpdateAddressFlowService;
    private final DataHiveUenUpdateShareholderFlowService uenUpdateShareholderFlowService;
    private final DataHiveUenUpdateBoardFlowService uenUpdateBoardFlowService;
    private final DataHiveUenListedCompanyFlowService uenListedCompanyFlowService;
    private final DataHiveUenNonListedCompanyFlowService uenNonListedCompanyFlowService;
    private final DataHiveUenTsAcrSuccessFlowService uenTsAcrSuccessFlowService;
    private final DataHiveUenTsAcrFailureFlowService uenTsAcrFailureFlowService;

    public DataHiveUENScenarioController(DataHiveUenCompleteSuccessFlowService uenCompleteFlowService,
                                        DataHiveUenRegistrationOnlyFlowService uenRegistrationOnlyFlowService,
                                        DataHiveUenDeregisteredOnlyFlowService uenDeregisteredOnlyFlowService,
                                        DataHiveUenShareholderOnlyFlowService uenShareholderOnlyFlowService,
                                        DataHiveUenBoardOnlyFlowService uenBoardOnlyFlowService,
                                        DataHiveUenMixedCompanyFlowService uenMixedCompanyFlowService,
                                        DataHiveUenMixedCorporateFlowService uenMixedCorporateFlowService,
                                        DataHiveUenNotFoundFlowService uenNotFoundFlowService,
                                        DataHiveUenUpdateCompanyFlowService uenUpdateCompanyFlowService,
                                        DataHiveUenUpdateAddressFlowService uenUpdateAddressFlowService,
                                        DataHiveUenUpdateShareholderFlowService uenUpdateShareholderFlowService,
                                        DataHiveUenUpdateBoardFlowService uenUpdateBoardFlowService,
                                        DataHiveUenListedCompanyFlowService uenListedCompanyFlowService,
                                        DataHiveUenNonListedCompanyFlowService uenNonListedCompanyFlowService,
                                        DataHiveUenTsAcrSuccessFlowService uenTsAcrSuccessFlowService,
                                        DataHiveUenTsAcrFailureFlowService uenTsAcrFailureFlowService) {
        this.uenCompleteFlowService = uenCompleteFlowService;
        this.uenRegistrationOnlyFlowService = uenRegistrationOnlyFlowService;
        this.uenDeregisteredOnlyFlowService = uenDeregisteredOnlyFlowService;
        this.uenShareholderOnlyFlowService = uenShareholderOnlyFlowService;
        this.uenBoardOnlyFlowService = uenBoardOnlyFlowService;
        this.uenMixedCompanyFlowService = uenMixedCompanyFlowService;
        this.uenMixedCorporateFlowService = uenMixedCorporateFlowService;
        this.uenNotFoundFlowService = uenNotFoundFlowService;
        this.uenUpdateCompanyFlowService = uenUpdateCompanyFlowService;
        this.uenUpdateAddressFlowService = uenUpdateAddressFlowService;
        this.uenUpdateShareholderFlowService = uenUpdateShareholderFlowService;
        this.uenUpdateBoardFlowService = uenUpdateBoardFlowService;
        this.uenListedCompanyFlowService = uenListedCompanyFlowService;
        this.uenNonListedCompanyFlowService = uenNonListedCompanyFlowService;
        this.uenTsAcrSuccessFlowService = uenTsAcrSuccessFlowService;
        this.uenTsAcrFailureFlowService = uenTsAcrFailureFlowService;
    }

    /**
     * Test Flow: DataHive Complete UEN Success Flow
     *
     * Input: UEN T14UF7629E dengan complete ACRA records
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - All DataHive views return data untuk comprehensive company profile
     *   - ocms_dh_acra_company_detail: 1 record created dengan company details
     *   - ocms_dh_acra_shareholder_info: Multiple records created untuk each shareholder
     *   - ocms_dh_acra_board_info: Multiple records created untuk each board member
     *   - Snowflake vs Database exact field match validation
     */
    @PostMapping("/complete-success-flow")
    public ResponseEntity<Map<String, Object>> executeUENCompleteSuccessFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🚀 Starting DataHive Complete UEN Success Flow Test");

        try {
            List<TestStepResult> results = uenCompleteFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive Complete UEN Success Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive Complete UEN Success Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive UEN Registration Only Flow
     *
     * Input: UEN 201234568B dengan registration record only
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify only company registration data
     * Verification:
     *   - ocms_dh_acra_company_detail: 1 record created dengan company details
     *   - ocms_offence_notice_owner_driver_addr: 1 record created dengan registered address
     *   - Shareholder/Board tables: NO CHANGES expected
     *   - DataHive queries return: Registration=✅, Deregistered=❌, Shareholder=❌, Board=❌
     */
    @PostMapping("/registration-only-flow")
    public ResponseEntity<Map<String, Object>> executeUENRegistrationOnlyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏢 Starting DataHive UEN Registration Only Flow Test");

        try {
            List<TestStepResult> results = uenRegistrationOnlyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Registration Only Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Registration Only Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Deregistered Only UEN Flow
     *
     * Input: UEN 201234569C dengan deregistered record only
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - DataHive views: Registration=❌, Deregistered=✅, Shareholder=❌, Board=❌
     *   - ocms_dh_acra_company_detail: 1 record created dengan deregistration info
     *   - ocms_offence_notice_owner_driver_addr: 1 record created
     *   - Shareholder/Board tables: NO CHANGES expected
     *   - Business logic tables: Limited changes expected
     */
    @PostMapping("/deregistered-only-flow")
    public ResponseEntity<Map<String, Object>> executeUENDeregisteredOnlyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏢 Starting DataHive UEN Deregistered Only Flow Test");

        try {
            List<TestStepResult> results = uenDeregisteredOnlyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Deregistered Only Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Deregistered Only Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Shareholder Only UEN Flow
     *
     * Input: UEN 201234570D dengan shareholder records only
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - DataHive views: Registration=❌, Deregistered=❌, Shareholder=✅, Board=❌
     *   - ocms_shareholder_info: Multiple records created untuk each shareholder
     *   - Other tables: NO CHANGES expected
     *   - Negative testing untuk tables yang tidak seharusnya diupdate
     */
    @PostMapping("/shareholder-only-flow")
    public ResponseEntity<Map<String, Object>> executeUENShareholderOnlyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("👥 Starting DataHive UEN Shareholder Only Flow Test");

        try {
            List<TestStepResult> results = uenShareholderOnlyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Shareholder Only Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Shareholder Only Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Board Only UEN Flow
     *
     * Input: UEN 201234571E dengan board member records only
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - DataHive views: Registration=❌, Deregistered=❌, Shareholder=❌, Board=✅
     *   - ocms_dh_acra_board_info: Multiple records created untuk each board member
     *   - Other tables: NO CHANGES expected
     *   - Board position and appointment date handling
     */
    @PostMapping("/board-only-flow")
    public ResponseEntity<Map<String, Object>> executeUENBoardOnlyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("👔 Starting DataHive UEN Board Only Flow Test");

        try {
            List<TestStepResult> results = uenBoardOnlyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Board Only Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Board Only Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Mixed Company UEN Flow
     *
     * Input: UEN 201234572F dengan both active dan deregistered records
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - DataHive views: Registration=✅, Deregistered=✅, Shareholder=❌, Board=❌
     *   - ocms_dh_acra_company_detail: 1 record created dengan merged data
     *   - Business rule validation: Handle conflicting registration status
     *   - Conflict resolution logic testing
     */
    @PostMapping("/mixed-company-flow")
    public ResponseEntity<Map<String, Object>> executeUENMixedCompanyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏢 Starting DataHive UEN Mixed Company Flow Test");

        try {
            List<TestStepResult> results = uenMixedCompanyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Mixed Company Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Mixed Company Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Mixed Corporate UEN Flow
     *
     * Input: UEN 201234573G dengan complete corporate structure
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify exact match
     * Verification:
     *   - DataHive views: Registration=❌, Deregistered=❌, Shareholder=✅, Board=✅
     *   - ocms_shareholder_info: Multiple records created
     *   - ocms_dh_acra_board_info: Multiple records created
     *   - Complete corporate structure verification
     */
    @PostMapping("/mixed-corporate-flow")
    public ResponseEntity<Map<String, Object>> executeUENMixedCorporateFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏛️ Starting DataHive UEN Mixed Corporate Flow Test");

        try {
            List<TestStepResult> results = uenMixedCorporateFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Mixed Corporate Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Mixed Corporate Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Not Found UEN Flow
     *
     * Input: UEN 201234574H yang clean, tidak ada history
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify graceful handling
     * Verification:
     *   - DataHive views: All views return empty result sets
     *   - No database updates, semua tables unchanged
     *   - Graceful handling of empty result sets
     *   - No exceptions thrown, proper error resilience
     */
    @PostMapping("/not-found-flow")
    public ResponseEntity<Map<String, Object>> executeUENNotFoundFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🔍 Starting DataHive UEN Not Found Flow Test");

        try {
            List<TestStepResult> results = uenNotFoundFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Not Found Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Not Found Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Update Company UEN Flow
     *
     * Input: UEN 201234575I dengan existing company record to update
     * Process: Create existing record → Call DataHive API → Update verification
     * Verification:
     *   - ocms_dh_acra_company_detail: Existing record updated, bukan create new
     *   - Record count remains same, updated fields verified
     *   - UPSERT logic testing
     *   - Audit fields (upd_date) updated properly
     */
    @PostMapping("/update-company-flow")
    public ResponseEntity<Map<String, Object>> executeUENUpdateCompanyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🔄 Starting DataHive UEN Update Company Flow Test");

        try {
            List<TestStepResult> results = uenUpdateCompanyFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Update Company Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Update Company Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Update Address UEN Flow
     *
     * Input: UEN 201234576J dengan existing address record to update
     * Process: Create existing address → Call DataHive API → Update verification
     * Verification:
     *   - ocms_offence_notice_owner_driver_addr: Existing record updated
     *   - Audit fields (upd_date) updated properly
     *   - Address data change verification
     *   - Address type consistency maintained
     */
    @PostMapping("/update-address-flow")
    public ResponseEntity<Map<String, Object>> executeUENUpdateAddressFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏠 Starting DataHive UEN Update Address Flow Test");

        try {
            List<TestStepResult> results = uenUpdateAddressFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Update Address Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Update Address Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Update Shareholder UEN Flow
     *
     * Input: UEN 201234577K dengan existing shareholder record to update
     * Process: Create existing shareholder → Call DataHive API → Update verification
     * Verification:
     *   - ocms_shareholder_info: Existing record updated dengan new share info
     *   - UPSERT logic working properly
     *   - Share allotment number changes verified
     *   - No duplicate shareholder records created
     */
    @PostMapping("/update-shareholder-flow")
    public ResponseEntity<Map<String, Object>> executeUENUpdateShareholderFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("👥 Starting DataHive UEN Update Shareholder Flow Test");

        try {
            List<TestStepResult> results = uenUpdateShareholderFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Update Shareholder Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Update Shareholder Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Update Board UEN Flow
     *
     * Input: UEN 201234578L dengan existing board record to update
     * Process: Create existing board member → Call DataHive API → Update verification
     * Verification:
     *   - ocms_dh_acra_board_info: Existing record updated dengan new position info
     *   - Date fields properly handled
     *   - Position withdrawal date updates
     *   - Position code validation working
     */
    @PostMapping("/update-board-flow")
    public ResponseEntity<Map<String, Object>> executeUENUpdateBoardFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("👔 Starting DataHive UEN Update Board Flow Test");

        try {
            List<TestStepResult> results = uenUpdateBoardFlowService.executeFlow();

            // Calculate summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("summary", calculateSummary(results));

            log.info("✅ DataHive UEN Update Board Flow Test completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Update Board Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Listed Company UEN Flow
     *
     * Input: UEN 201234579M dengan EntityType=LC (Listed Company)
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify business logic
     * Verification:
     *   - EntityType=LC business rules validation
     *   - Gazetted flag logic (should be 1 for Listed Companies)
     *   - Listed Company specific requirements
     *   - Complete corporate information requirements
     */
    @PostMapping("/listed-company-flow")
    public ResponseEntity<Map<String, Object>> executeUENListedCompanyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏛️ Starting DataHive UEN Listed Company Flow Test");

        try {
            Map<String, Object> results = uenListedCompanyFlowService.executeListedCompanyFlow();

            log.info("✅ DataHive UEN Listed Company Flow Test completed");
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Listed Company Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive Non-Listed Company UEN Flow
     *
     * Input: UEN 201234580N dengan EntityType=NLC (Non-Listed Company)
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify business logic
     * Verification:
     *   - EntityType=NLC business rules validation
     *   - Gazetted flag logic (should be 0 for Non-Listed Companies)
     *   - Simplified data requirements vs Listed Company
     *   - Reduced disclosure requirements validation
     */
    @PostMapping("/non-listed-company-flow")
    public ResponseEntity<Map<String, Object>> executeUENNonListedCompanyFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("🏢 Starting DataHive UEN Non-Listed Company Flow Test");

        try {
            Map<String, Object> results = uenNonListedCompanyFlowService.executeNonListedCompanyFlow();

            log.info("✅ DataHive UEN Non-Listed Company Flow Test completed");
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ DataHive UEN Non-Listed Company Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive TS-ACR Success UEN Flow
     *
     * Input: UEN 201234581O dengan successful TS-ACR application workflow
     * Process: Setup test data → Call DataHive API → Query Snowflake directly → Verify success workflow
     * Verification:
     *   - Application status SUCCESS validation
     *   - Approval workflow completion
     *   - Data consistency post-approval
     *   - Audit trail completeness
     *   - Business rule compliance for approved applications
     */
    @PostMapping("/ts-acr-success-flow")
    public ResponseEntity<Map<String, Object>> executeUENTsAcrSuccessFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("✅ Starting DataHive UEN TS-ACR Success Flow Test");

        try {
            Map<String, Object> results = uenTsAcrSuccessFlowService.executeTsAcrSuccessFlow();

            log.info("✅ DataHive UEN TS-ACR Success Flow Test completed");
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ DataHive UEN TS-ACR Success Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Test Flow: DataHive TS-ACR Failure UEN Flow
     *
     * Input: UEN 201234582P dengan failed TS-ACR application workflow
     * Process: Setup test data → Simulate API failure → Query Snowflake directly → Verify failure handling
     * Verification:
     *   - Application status FAILED validation
     *   - Failure handling workflow
     *   - Error logging and audit trail
     *   - Retry mechanism validation
     *   - Data cleanup on failure
     *   - Business rule enforcement on failure
     */
    @PostMapping("/ts-acr-failure-flow")
    public ResponseEntity<Map<String, Object>> executeUENTsAcrFailureFlow() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Test endpoints are disabled"));
        }

        log.info("❌ Starting DataHive UEN TS-ACR Failure Flow Test");

        try {
            Map<String, Object> results = uenTsAcrFailureFlowService.executeTsAcrFailureFlow();

            log.info("✅ DataHive UEN TS-ACR Failure Flow Test completed");
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ DataHive UEN TS-ACR Failure Flow Test failed with error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Test execution failed: " + e.getMessage()));
        }
    }

    /**
     * Calculate summary statistics dari test results
     *
     * @param results List hasil test steps
     * @return Map containing summary statistics
     */
    private Map<String, Object> calculateSummary(List<TestStepResult> results) {
        Map<String, Object> summary = new HashMap<>();

        long successCount = results.stream()
                .mapToLong(r -> "SUCCESS".equals(r.getStatus()) ? 1 : 0)
                .sum();

        long failedCount = results.stream()
                .mapToLong(r -> "FAILED".equals(r.getStatus()) ? 1 : 0)
                .sum();

        long skippedCount = results.stream()
                .mapToLong(r -> "SKIPPED".equals(r.getStatus()) ? 1 : 0)
                .sum();

        summary.put("totalSteps", results.size());
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("skippedCount", skippedCount);
        summary.put("overallStatus", failedCount > 0 ? "FAILED" : "SUCCESS");

        return summary;
    }
}
