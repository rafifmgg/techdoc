package com.ocmsintranet.cronservice.testing.agencies.lta.controllers;

import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaDiplomatNormalFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaErrorCodeFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaLocalNormalFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaStandardExclusionListFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaStandardPassportFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaStandardSnowflakeEnaFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaStandardSnowflakeRd1FlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaSuspendedVehicleFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaUenSnowflakeEnaFlowService;
import com.ocmsintranet.cronservice.testing.agencies.lta.services.LtaUenSnowflakeRd1FlowService;
import com.ocmsintranet.cronservice.testing.common.TestStepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for LTA test scenarios
 * Implements test endpoints for various LTA workflow scenarios
 */
@RestController
@RequestMapping("/test/lta")
public class LtaScenarioController {

    @Value("${test.endpoints.enabled:true}")
    private boolean testEndpointsEnabled;

    @Autowired
    private LtaDiplomatNormalFlowService ltaDiplomatNormalFlowService;

    @Autowired
    private LtaLocalNormalFlowService ltaLocalNormalFlowService;

    @Autowired
    private LtaErrorCodeFlowService ltaErrorCodeFlowService;

    @Autowired
    private LtaStandardExclusionListFlowService ltaStandardExclusionListFlowService;

    @Autowired
    private LtaStandardPassportFlowService ltaStandardPassportFlowService;

    @Autowired
    private LtaStandardSnowflakeEnaFlowService ltaStandardSnowflakeEnaFlowService;

    @Autowired
    private LtaStandardSnowflakeRd1FlowService ltaStandardSnowflakeRd1FlowService;

    @Autowired
    private LtaSuspendedVehicleFlowService ltaSuspendedVehicleFlowService;

    @Autowired
    private LtaUenSnowflakeEnaFlowService ltaUenSnowflakeEnaFlowService;

    @Autowired
    private LtaUenSnowflakeRd1FlowService ltaUenSnowflakeRd1FlowService;

    /**
     * Test Flow: Diplomat Normal Flow
     *
     * Input: File with diplomaticFlag = 'Y'
     * Process: Detect diplomat vehicle → Patch Diplomat → Update to RD1
     * Verification:
     *   - Status updated from NPA to RD1
     *   - Owner data correctly updated
     *   - No errors logged
     */
    @PostMapping("/lta-diplomat-normal-flow")
    public ResponseEntity<?> runDiplomatNormalFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaDiplomatNormalFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Local Normal Flow
     *
     * Input: Notice with vehicle_registration_type = 'S'
     * Process: Detect local vehicle → Process locally → Update status
     * Verification:
     *   - Status is updated from NPA to ENA
     *   - Owner data is updated correctly
     *   - No errors are logged
     */
    @PostMapping("/lta-local-normal-flow")
    public ResponseEntity<?> runLocalNormalFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaLocalNormalFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Error Code A Flow (Total count does not match)
     *
     * Input: None
     * Process: Generate file with Error Code A → Upload file
     * Verification:
     *   - File is generated with correct error code
     *   - File is uploaded successfully
     *   - Error is detected correctly
     */
    @PostMapping("/lta-error-a-flow")
    public ResponseEntity<?> runErrorCodeAFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaErrorCodeFlowService.runErrorCodeATest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Error Code B Flow (Missing Trailer)
     *
     * Input: None
     * Process: Generate file with Error Code B → Upload file
     * Verification:
     *   - File is generated with correct error code
     *   - File is uploaded successfully
     *   - Error is detected correctly
     */
    @PostMapping("/lta-error-b-flow")
    public ResponseEntity<?> runErrorCodeBFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaErrorCodeFlowService.runErrorCodeBTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Error Code C Flow (Missing Header)
     *
     * Input: None
     * Process: Generate file with Error Code C → Upload file
     * Verification:
     *   - File is generated with correct error code
     *   - File is uploaded successfully
     *   - Error is detected correctly
     */
    @PostMapping("/lta-error-c-flow")
    public ResponseEntity<?> runErrorCodeCFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaErrorCodeFlowService.runErrorCodeCTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Standard Vehicle with Owner in Exclusion List
     *
     * Input: Standard vehicle (not suspended) with owner in exclusion list
     * Process: Process vehicle → Detect owner in exclusion list → Update to RD1
     * Verification:
     *   - Status is updated from NPA to RD1 (skipping ENA)
     *   - Owner data is updated correctly
     *   - Exclusion list entry remains intact
     */
    @PostMapping("/lta-standard-exclusion-list-flow")
    public ResponseEntity<?> runStandardExclusionListFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaStandardExclusionListFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Standard Vehicle with Passport Holder (Not in Exclusion List)
     *
     * Input: Standard vehicle (not suspended) with passport holder owner not in exclusion list
     * Process: Process vehicle → Verify passport holder data → Update to RD1
     * Verification:
     *   - Status is updated from NPA to RD1
     *   - Owner data is updated correctly with passport information
     *   - Owner is not added to exclusion list
     */
    @PostMapping("/lta-standard-passport-flow")
    public ResponseEntity<?> runStandardPassportFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaStandardPassportFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Standard vehicle with Singapore NRIC (Snowflake ENA)
     *
     * Input: Standard vehicle (not suspended) with Singapore NRIC owner not in exclusion list
     * Process: Process vehicle → Verify Singapore NRIC → Update to ENA (with STAGEDAYS)
     * Verification:
     *   - Status is updated from ROV to ENA
     *   - Owner data is updated correctly with Singapore NRIC
     *   - ENA STAGEDAYS parameter is used
     *   - Owner is not in exclusion list
     */
    @PostMapping("/lta-standard-snowflake-ena-flow")
    public ResponseEntity<?> runStandardSnowflakeEnaFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaStandardSnowflakeEnaFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Standard vehicle with Singapore NRIC (Snowflake RD1)
     *
     * Input: Standard vehicle (not suspended) with Singapore NRIC owner not in exclusion list
     * Process: Process vehicle → Call Snowflake → NO mobile/email found → Update to RD1 (with STAGEDAYS)
     * Verification:
     *   - Status is updated from ROV to RD1
     *   - Owner data is updated correctly with Singapore NRIC
     *   - RD1 STAGEDAYS parameter is used
     *   - No mobile/email found in Snowflake
     *   - Owner is not in exclusion list
     */
    @PostMapping("/lta-standard-snowflake-rd1-flow")
    public ResponseEntity<?> runStandardSnowflakeRd1FlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaStandardSnowflakeRd1FlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: Suspended vehicle with TS-HST but REVIVED
     *
     * Input: Standard vehicle that is suspended (TS-HST) but has been revived
     * Process: Process vehicle → Update basic owner/vehicle data → Check suspension (revived) → END early
     * Verification:
     *   - Processing stages remain unchanged (NPA->ROV)
     *   - Owner and vehicle data is updated with LTA data
     *   - Suspension configuration remains intact (TS-HST REVIVED)
     *   - Processing ends early after suspension check
     */
    @PostMapping("/lta-suspended-vehicle-flow")
    public ResponseEntity<?> runSuspendedVehicleFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaSuspendedVehicleFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: UEN Snowflake ENA Flow
     *
     * Input: Standard vehicle with UEN (ID Type 4) owner not in exclusion list
     * Process: Process vehicle → Call Snowflake → Email found → Update to ENA (with STAGEDAYS)
     * Verification:
     *   - Status is updated from ROV to ENA
     *   - Owner data is updated correctly with UEN (ID Type 4)
     *   - Snowflake is called (datahive_processing_datetime populated)
     *   - Email address is found and populated
     *   - ENA STAGEDAYS parameter is used
     */
    @PostMapping("/lta-uen-snowflake-ena-flow")
    public ResponseEntity<?> runUenSnowflakeEnaFlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaUenSnowflakeEnaFlowService.runTest();
        return ResponseEntity.ok(results);
    }

    /**
     * Test Flow: UEN Snowflake RD1 Flow
     *
     * Input: UEN entity without email in Snowflake
     * Process: UEN → Call Snowflake → No email found → Patch to RD1 stage
     * Verification:
     *   - Processing stage updated from ROV to RD1
     *   - No email populated (remains null/empty)
     *   - Snowflake call timestamp recorded
     *   - Audit fields updated correctly
     */
    @PostMapping("/lta-uen-snowflake-rd1-flow")
    public ResponseEntity<?> runUenSnowflakeRd1FlowTest() {
        if (!testEndpointsEnabled) {
            return ResponseEntity.badRequest().body("Test endpoints are disabled in this environment");
        }
        List<TestStepResult> results = ltaUenSnowflakeRd1FlowService.runTest();
        return ResponseEntity.ok(results);
    }
}
