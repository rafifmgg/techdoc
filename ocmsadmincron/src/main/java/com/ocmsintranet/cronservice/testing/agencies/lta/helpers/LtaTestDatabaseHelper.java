package com.ocmsintranet.cronservice.testing.agencies.lta.helpers;

import com.ocmsintranet.cronservice.testing.agencies.lta.models.TestStepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for LTA test database operations.
 * Contains methods for creating and verifying test records in the database.
 */
@Component
public class LtaTestDatabaseHelper {

    private final JdbcTemplate jdbcTemplate;
    private String schema;
    private String diplomatNoticeNumber;
    private String diplomatVehicleNumber;
    private String vipNoticeNumber;
    private String vipVehicleNumber;
    private String localNoticeNumber;
    private String localVehicleNumber;

    private String testFilesPath;
    private boolean createPhysicalFiles = true;
    private String datFile;
    private String encryptedFile;

    /**
     * Constructor for LtaTestDatabaseHelper
     *
     * @param jdbcTemplate JdbcTemplate for database operations
     * @param schema Database schema name
     * @param dipNoticeNumber Diplomat notice number
     * @param dipVehicleNumber Diplomat vehicle number
     * @param vipNoticeNumber VIP notice number
     * @param vipVehicleNumber VIP vehicle number
     */
    @Autowired
    public LtaTestDatabaseHelper(
            JdbcTemplate jdbcTemplate,
            @Value("${lta.test.schema:ocmsizmgr}") String schema,
            @Value("${lta.test.diplomat.notice:DIP1000001}") String dipNoticeNumber,
            @Value("${lta.test.diplomat.vehicle:FBD870K}") String dipVehicleNumber,
            @Value("${lta.test.vip.notice:VIP1000001}") String vipNoticeNumber,
            @Value("${lta.test.vip.vehicle:VXY5678}") String vipVehicleNumber,
            @Value("${lta.test.local.notice:LOC1000001}") String localNoticeNumber,
            @Value("${lta.test.local.vehicle:E1830X}") String localVehicleNumber) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
        this.diplomatNoticeNumber = dipNoticeNumber;
        this.diplomatVehicleNumber = dipVehicleNumber;
        this.vipNoticeNumber = vipNoticeNumber;
        this.vipVehicleNumber = vipVehicleNumber;
        this.localNoticeNumber = localNoticeNumber;
        this.localVehicleNumber = localVehicleNumber;
    }

    /**
     * Set the test files path
     *
     * @param testFilesPath Path to test files directory
     */
    public void setTestFilesPath(String testFilesPath) {
        this.testFilesPath = testFilesPath;
    }

    /**
     * Set the flag to create physical files
     *
     * @param createPhysicalFiles True if physical files should be created
     */
    public void setCreatePhysicalFiles(boolean createPhysicalFiles) {
        this.createPhysicalFiles = createPhysicalFiles;
    }

    /**
     * Set the DAT file path
     *
     * @param datFile Path to DAT file
     */
    public void setDatFile(String datFile) {
        this.datFile = datFile;
    }

    /**
     * Set the encrypted file path
     *
     * @param encryptedFile Path to encrypted file
     */
    public void setEncryptedFile(String encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    /**
     * Clean up existing test records from all tables
     */
    public void cleanupExistingTestRecords() {
        try {
            // Delete from ocms_offence_notice_owner_driver
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_offence_notice_owner_driver WHERE notice_no IN (?, ?)",
                diplomatNoticeNumber, vipNoticeNumber
            );
            System.out.println("Successfully cleaned up ocms_offence_notice_owner_driver records");
        } catch (Exception e) {
            System.err.println("Error cleaning up ocms_offence_notice_owner_driver records: " + e.getMessage());
            // Continue with next table cleanup
        }

        try {
            // Delete from ocms_offence_notice_detail
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_offence_notice_detail WHERE notice_no IN (?, ?)",
                diplomatNoticeNumber, vipNoticeNumber
            );
            System.out.println("Successfully cleaned up ocms_offence_notice_detail records");
        } catch (Exception e) {
            System.err.println("Error cleaning up ocms_offence_notice_detail records: " + e.getMessage());
            // Continue with next table cleanup
        }

        try {
            // Delete from ocms_valid_offence_notice
            jdbcTemplate.update(
                "DELETE FROM " + schema + ".ocms_valid_offence_notice WHERE notice_no IN (?, ?)",
                diplomatNoticeNumber, vipNoticeNumber
            );
            System.out.println("Successfully cleaned up ocms_valid_offence_notice records");
        } catch (Exception e) {
            System.err.println("Error cleaning up ocms_valid_offence_notice records: " + e.getMessage());
            // Continue with next operation
        }
    }

    /**
     * Cleans up all test files in the test directory if physical file creation is enabled
     */
    public void cleanupTestFiles() {
        if (createPhysicalFiles) {
            try {
                if (datFile != null) {
                    Files.deleteIfExists(Paths.get(datFile));
                }
                if (encryptedFile != null) {
                    Files.deleteIfExists(Paths.get(encryptedFile));
                }
                File directory = new File(testFilesPath);
                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && (file.getName().startsWith("LTA_") ||
                                                 file.getName().startsWith("VRL-URA-OFFREPLY-D2-") ||
                                                 file.getName().endsWith(".json") ||
                                                 file.getName().endsWith(".uploaded") ||
                                                 file.getName().endsWith(".dat") ||
                                                 file.getName().endsWith(".p7"))) {
                                boolean deleted = file.delete();
                                if (deleted) {
                                    System.out.println("Deleted test file: " + file.getAbsolutePath());
                                } else {
                                    System.err.println("Failed to delete test file: " + file.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error cleaning up test files: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to create a properly formatted Appendix F record (691 characters).
     * Based on the shell script's create_appendix_f_record function.
     */
    public String createAppendixFRecord(
            String vehicleNumber, String chassisNumber, String diplomaticFlag,
            String noticeNumber, String ownerIdType, String passportPlace,
            String ownerId, String ownerName, String addressType,
            String blockHouse, String streetName, String floorNumber,
            String unitNumber, String buildingName, String postalCode,
            String makeDescription, String primaryColour, String secondaryColour,
            String roadTaxExpiry, String unladenWeight, String maxLadenWeight,
            String effectiveOwnership, String deregistrationDate, String errorCode,
            String processingDate, String processingTime, String iuNumber,
            String regAddressEffectiveDate, String mailingBlockHouse,
            String mailingStreetName, String mailingFloorNumber, String mailingUnitNumber,
            String mailingBuildingName, String mailingPostalCode, String mailingAddressEffectiveDate) {

        StringBuilder record = new StringBuilder();

        // Position 1: Record Type (A1)
        record.append(String.format("%-1.1s", "D"));

        // Position 2-13: Vehicle Registration Number (A12)
        record.append(String.format("%-12.12s", vehicleNumber));

        // Position 14-38: Chassis Number (A25)
        record.append(String.format("%-25.25s", chassisNumber));

        // Position 39: Diplomatic Flag (A1)
        record.append(String.format("%-1.1s", diplomaticFlag));

        // Position 40-49: Notice Number (A10)
        record.append(String.format("%-10.10s", noticeNumber));

        // Position 50: Owner ID Type (A1)
        record.append(String.format("%-1.1s", ownerIdType));

        // Position 51-53: Owner's Passport Place of Issue (A3)
        record.append(String.format("%-3.3s", passportPlace));

        // Position 54-73: Owner ID (A20)
        record.append(String.format("%-20.20s", ownerId));

        // Position 74-139: Owner Name (A66)
        record.append(String.format("%-66.66s", ownerName));

        // Position 140: Address Type (A1)
        record.append(String.format("%-1.1s", addressType));

        // Position 141-150: Block/House Number (A10)
        record.append(String.format("%-10.10s", blockHouse));

        // Position 151-182: Street Name (A32)
        record.append(String.format("%-32.32s", streetName));

        // Position 183-184: Floor Number (A2)
        record.append(String.format("%-2.2s", floorNumber));

        // Position 185-189: Unit Number (A5)
        record.append(String.format("%-5.5s", unitNumber));

        // Position 190-219: Building Name (A30)
        record.append(String.format("%-30.30s", buildingName));

        // Position 220-227: Postal Code (A8)
        record.append(String.format("%-8.8s", postalCode));

        // Position 228-327: Make Description (A100)
        record.append(String.format("%-100.100s", makeDescription));

        // Position 328-427: Primary Colour (A100)
        record.append(String.format("%-100.100s", primaryColour));

        // Position 428-527: Secondary Colour (A100)
        record.append(String.format("%-100.100s", secondaryColour));

        // Position 528-535: Road Tax Expiry Date (N8)
        record.append(String.format("%8.8s", roadTaxExpiry));

        // Position 536-542: Unladen Weight (N7)
        record.append(String.format("%7.7s", unladenWeight));

        // Position 543-549: Maximum Laden Weight (N7)
        record.append(String.format("%7.7s", maxLadenWeight));

        // Position 550-557: Effective Ownership Date (N8)
        record.append(String.format("%8.8s", effectiveOwnership));

        // Position 558-565: Deregistration Date (N8)
        record.append(String.format("%8.8s", deregistrationDate));

        // Position 566: Return Error Code (A1)
        record.append(String.format("%-1.1s", errorCode));

        // Position 567-574: Processing Date (N8)
        record.append(String.format("%8.8s", processingDate));

        // Position 575-578: Processing Time (N4)
        record.append(String.format("%4.4s", processingTime));

        // Position 579-588: IU/OBU Label Number (A10)
        record.append(String.format("%-10.10s", iuNumber));

        // Position 589-596: Registered Address Effective Date (N8)
        record.append(String.format("%8.8s", regAddressEffectiveDate));

        // Position 597-606: Mailing Block/House Number (A10)
        record.append(String.format("%-10.10s", mailingBlockHouse));

        // Position 607-638: Mailing Street Name (A32)
        record.append(String.format("%-32.32s", mailingStreetName));

        // Position 639-640: Mailing Floor Number (A2)
        record.append(String.format("%-2.2s", mailingFloorNumber));

        // Position 641-645: Mailing Unit Number (A5)
        record.append(String.format("%-5.5s", mailingUnitNumber));

        // Position 646-675: Mailing Building Name (A30)
        record.append(String.format("%-30.30s", mailingBuildingName));

        // Position 676-683: Mailing Postal Code (A8)
        record.append(String.format("%-8.8s", mailingPostalCode));

        // Position 684-691: Mailing Address Effective Date (N8)
        record.append(String.format("%8.8s", mailingAddressEffectiveDate));

        // Verify length is exactly 691
        if (record.length() != 691) {
            throw new IllegalStateException("Record length is " + record.length() + ", expected 691");
        }

        return record.toString();
    }

    /**
     * Display joined table data for a specific notice number and vehicle number
     *
     * @param noticeNumber The notice number to query
     * @param vehicleNumber The vehicle number to query
     * @return A formatted string with the query results
     */
    public String displayJoinedTableData(String noticeNumber, String vehicleNumber) {
        StringBuilder result = new StringBuilder();
        try {
            String query = "SELECT von.notice_no, von.vehicle_no, von.offence_notice_type, " +
                    "von.notice_date_and_time, von.composition_amount, von.computer_rule_code, " +
                    "von.pp_code, von.parking_lot_no, von.parking_fee, von.surcharge, " +
                    "von.administration_fee, von.amount_payable, " +
                    "von.warden_no, von.last_processing_stage, von.next_processing_stage, " +
                    "von.last_processing_date, von.next_processing_date, von.cre_user_id, " +
                    "von.cre_date, von.suspension_type, von.epr_reason_suspension, " +
                    "von.subsystem_label, " +
                    "ond.notice_no, ond.offence_date, ond.offence_time, ond.offence_location, " +
                    "ond.offence_code, ond.offence_description, ond.offence_type, " +
                    "onod.notice_no, onod.owner_id_type, onod.owner_id, onod.owner_name, " +
                    "onod.owner_address_type, onod.owner_block_house_no, onod.owner_street_name, " +
                    "onod.owner_floor_no, onod.owner_unit_no, onod.owner_building_name, " +
                    "onod.owner_postal_code, onod.driver_id_type, onod.driver_id, " +
                    "onod.driver_name, onod.driver_address_type, onod.driver_block_house_no, " +
                    "onod.driver_street_name, onod.driver_floor_no, onod.driver_unit_no, " +
                    "onod.driver_building_name, onod.driver_postal_code " +
                    "FROM " + schema + ".ocms_valid_offence_notice von " +
                    "LEFT JOIN " + schema + ".ocms_offence_notice_detail ond ON von.notice_no = ond.notice_no " +
                    "LEFT JOIN " + schema + ".ocms_offence_notice_owner_driver onod ON von.notice_no = onod.notice_no " +
                    "WHERE von.notice_no = ? AND von.vehicle_no = ?";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, noticeNumber, vehicleNumber);

            if (rows.isEmpty()) {
                result.append("No records found for notice number: ").append(noticeNumber)
                      .append(" and vehicle number: ").append(vehicleNumber);
            } else {
                result.append("Found ").append(rows.size()).append(" record(s) for notice number: ")
                      .append(noticeNumber).append(" and vehicle number: ").append(vehicleNumber).append("\n\n");

                for (Map<String, Object> row : rows) {
                    result.append("=== VALID OFFENCE NOTICE ===\n");
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getKey().startsWith("von.")) {
                            result.append(entry.getKey().substring(4)).append(": ")
                                  .append(entry.getValue()).append("\n");
                        }
                    }

                    result.append("\n=== OFFENCE NOTICE DETAIL ===\n");
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getKey().startsWith("ond.")) {
                            result.append(entry.getKey().substring(4)).append(": ")
                                  .append(entry.getValue()).append("\n");
                        }
                    }

                    result.append("\n=== OFFENCE NOTICE OWNER DRIVER ===\n");
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getKey().startsWith("onod.")) {
                            result.append(entry.getKey().substring(5)).append(": ")
                                  .append(entry.getValue()).append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.append("Error querying database: ").append(e.getMessage())
                  .append("\n").append(sw.toString());
        }

        return result.toString();
    }

    /**
     * Verify test results by checking database records for diplomat, VIP, and LOCAL notices
     *
     * @param result TestStepResult to update with verification results
     * @return TestStepResult containing verification status and details
     */
    public TestStepResult verifyResults(TestStepResult result) {
        try {
            // Check for diplomat records
            int diplomatCount = 0;
            try {
                String diplomatQuery = "SELECT COUNT(*) FROM " + schema + ".ocms_valid_offence_notice " +
                        "WHERE notice_no = ?";
                diplomatCount = jdbcTemplate.queryForObject(diplomatQuery, Integer.class,
                        diplomatNoticeNumber);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                result.addDetail("⚠️ Error querying diplomat records: " + e.getMessage());
                // Continue with next check
            }

            StringBuilder diplomatDetails = new StringBuilder("Diplomat records check: ");
            if (diplomatCount > 0) {
                diplomatDetails.append("PASSED (Found ").append(diplomatCount).append(" records)\n");
                diplomatDetails.append(displayJoinedTableData(diplomatNoticeNumber, diplomatVehicleNumber)).append("\n\n");
            } else {
                diplomatDetails.append("FAILED (No records found)\n");
                result.setStatus("FAILED");
            }
            result.addDetail(diplomatDetails.toString());
            
            // Check for VIP records
            int vipCount = 0;
            try {
                String vipQuery = "SELECT COUNT(*) FROM " + schema + ".ocms_valid_offence_notice " +
                        "WHERE notice_no = ?";
                vipCount = jdbcTemplate.queryForObject(vipQuery, Integer.class, vipNoticeNumber);
            } catch (Exception e) {
                result.addDetail("⚠️ Error querying VIP records: " + e.getMessage());
                // Continue with next check
            }
            
            StringBuilder vipDetails = new StringBuilder("VIP records check: ");
            if (vipCount > 0) {
                vipDetails.append("PASSED (Found ").append(vipCount).append(" records)\n");
                vipDetails.append(displayJoinedTableData(vipNoticeNumber, vipVehicleNumber)).append("\n\n");
            } else {
                vipDetails.append("FAILED (No records found)\n");
                result.setStatus("FAILED");
            }
            result.addDetail(vipDetails.toString());

            // Check for LOCAL records
            int localCount = 0;
            try {
                String localQuery = "SELECT COUNT(*) FROM " + schema + ".ocms_valid_offence_notice " +
                        "WHERE notice_no = ?";
                localCount = jdbcTemplate.queryForObject(localQuery, Integer.class,
                        localNoticeNumber);
            } catch (Exception e) {
                result.addDetail("⚠️ Error querying LOCAL records: " + e.getMessage());
                // Continue with next check
            }

            StringBuilder localDetails = new StringBuilder("LOCAL records check: ");
            if (localCount > 0) {
                localDetails.append("PASSED (Found ").append(localCount).append(" records)\n");
                localDetails.append(displayJoinedTableData(localNoticeNumber, localVehicleNumber));
            } else {
                localDetails.append("FAILED (No records found)\n");
                result.setStatus("FAILED");
            }
            result.addDetail(localDetails.toString());

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String errorDetails = "Error verifying results: " + e.getMessage() + "\n" + sw.toString();
            result.addDetail(errorDetails);
            result.setStatus("FAILED");
        }

        return result;
    }

    /**
     * Create test records for a specific vehicle type
     *
     * @param noticeNumber Notice number to use
     * @param vehicleNumber Vehicle number to use
     * @param registrationType Vehicle registration type (D for Diplomat, V for VIP, S for LOCAL)
     */
    public void createTestRecords(String noticeNumber, String vehicleNumber, String registrationType) {
        try {
            // 1. Insert ocms_valid_offence_notice
            insertValidOffenceNotice(
                noticeNumber,
                vehicleNumber,
                registrationType,
                "O", // Offence notice type
                70.00, // Composition amount
                10300, // Computer rule code
                "A0045", // PP code
                "ROV", // Last processing stage
                "ENA"  // Next processing stage
            );
            System.out.println("Successfully inserted valid offence notice: " + noticeNumber);
        } catch (Exception e) {
            System.err.println("Error inserting valid offence notice: " + e.getMessage());
            // Continue with next operation
        }

        try {
            // 2. Insert ocms_offence_notice_detail
            insertOffenceNoticeDetail(noticeNumber);
            System.out.println("Successfully inserted offence notice detail: " + noticeNumber);
        } catch (Exception e) {
            System.err.println("Error inserting offence notice detail: " + e.getMessage());
            // Continue with next operation
        }

        try {
            // 3. Insert ocms_offence_notice_owner_driver
            insertOffenceNoticeOwnerDriver(noticeNumber);
            System.out.println("Successfully inserted offence notice owner driver: " + noticeNumber);
        } catch (Exception e) {
            System.err.println("Error inserting offence notice owner driver: " + e.getMessage());
            // Continue with next operation
        }
    }

    /**
     * Create test records for Diplomat vehicle
     */
    public void createDiplomatTestRecords() {
        createTestRecords(diplomatNoticeNumber, diplomatVehicleNumber, "D");
    }

    /**
     * Create test records for VIP vehicle
     */
    public void createVipTestRecords() {
        createTestRecords(vipNoticeNumber, vipVehicleNumber, "V");
    }

    /**
     * Create test records for LOCAL vehicle
     */
    public void createLocalTestRecords() {
        createTestRecords(localNoticeNumber, localVehicleNumber, "S");
    }

    /**
     * Update existing test records
     *
     * @param noticeNumber Notice number to update
     * @param registrationType Vehicle registration type (D for Diplomat, V for VIP, S for LOCAL)
     */
    public void updateTestRecords(String noticeNumber, String registrationType) {
        try {
            String updateNoticeSql = "UPDATE " + schema + ".ocms_valid_offence_notice " +
                                    "SET vehicle_registration_type = ?, " +
                                    "offence_notice_type = 'O', " +
                                    "composition_amount = 70.00, " +
                                    "computer_rule_code = 10300, " +
                                    "pp_code = 'A0045', " +
                                    "last_processing_stage = 'ROV', " +
                                    "next_processing_stage = 'ENA', " +
                                    "upd_user_id = 'SYSTEM', " +
                                    "upd_date = GETDATE() " +
                                    "WHERE notice_no = ?";

            jdbcTemplate.update(updateNoticeSql, registrationType, noticeNumber);
            System.out.println("Successfully updated valid offence notice: " + noticeNumber);
        } catch (Exception e) {
            System.err.println("Error updating valid offence notice: " + e.getMessage());
            // Continue with next operation
        }

        try {
            // Check if record exists in ocms_offence_notice_detail
            String checkDetailSql = "SELECT COUNT(*) FROM " + schema + ".ocms_offence_notice_detail WHERE notice_no = ?";
            int detailCount = 0;
            try {
                detailCount = jdbcTemplate.queryForObject(checkDetailSql, Integer.class, noticeNumber);
            } catch (Exception e) {
                System.err.println("Error checking offence notice detail: " + e.getMessage());
                // Default to insert if check fails
                detailCount = 0;
            }

            if (detailCount > 0) {
                // Update ocms_offence_notice_detail
                String updateDetailSql = "UPDATE " + schema + ".ocms_offence_notice_detail " +
                                        "SET lta_chassis_number = NULL, " +
                                        "lta_make_description = NULL, " +
                                        "lta_primary_colour = NULL, " +
                                        "lta_unladen_weight = NULL, " +
                                        "lta_max_laden_weight = NULL, " +
                                        "lta_diplomatic_flag = 'N', " +
                                        "upd_user_id = 'SYSTEM', " +
                                        "upd_date = GETDATE() " +
                                        "WHERE notice_no = ?";

                jdbcTemplate.update(updateDetailSql, noticeNumber);
                System.out.println("Successfully updated offence notice detail: " + noticeNumber);
            } else {
                // Insert into ocms_offence_notice_detail
                insertOffenceNoticeDetail(noticeNumber);
            }
        } catch (Exception e) {
            System.err.println("Error updating/inserting offence notice detail: " + e.getMessage());
            // Continue with next operation
        }

        try {
            // Check if record exists in ocms_offence_notice_owner_driver
            String checkOwnerSql = "SELECT COUNT(*) FROM " + schema + ".ocms_offence_notice_owner_driver WHERE notice_no = ?";
            int ownerCount = 0;
            try {
                ownerCount = jdbcTemplate.queryForObject(checkOwnerSql, Integer.class, noticeNumber);
            } catch (Exception e) {
                System.err.println("Error checking offence notice owner driver: " + e.getMessage());
                // Default to insert if check fails
                ownerCount = 0;
            }

            if (ownerCount > 0) {
                // Update ocms_offence_notice_owner_driver
                String updateOwnerSql = "UPDATE " + schema + ".ocms_offence_notice_owner_driver " +
                                       "SET owner_driver_indicator = 'O', " +
                                    //    "life_status = 'A', " +
                                    //    "mha_processing_date_time = GETDATE(), " +
                                       "id_type = '1', " +
                                       "id_no = '', " +
                                       "name = NULL, " +
                                       "mail_blk_hse_no = NULL, " +
                                       "mail_street = NULL, " +
                                       "mail_postal_code = NULL, " +
                                       "mail_floor = NULL, " +
                                       "mail_unit = NULL, " +
                                       "mail_bldg = NULL, " +
                                       "lta_mailing_block_house_number = NULL, " +
                                       "lta_mailing_street_name = NULL, " +
                                       "lta_mailing_floor_number = NULL, " +
                                       "lta_mailing_unit_number = NULL, " +
                                       "lta_mailing_building_name = NULL, " +
                                       "lta_mailing_postal_code = NULL, " +
                                       "lta_mailing_address_effective_date = NULL, " +
                                       "offender_indicator = 'Y', " +
                                       "upd_user_id = 'SYSTEM', " +
                                       "upd_date = GETDATE() " +
                                       "WHERE notice_no = ?";

                jdbcTemplate.update(updateOwnerSql, noticeNumber);
                System.out.println("Successfully updated offence notice owner driver: " + noticeNumber);
            } else {
                // Insert into ocms_offence_notice_owner_driver
                insertOffenceNoticeOwnerDriver(noticeNumber);
            }
        } catch (Exception e) {
            System.err.println("Error updating/inserting offence notice owner driver: " + e.getMessage());
            // Continue with next operation
        }
    }

    /**
     * Update existing diplomat test records
     */
    public void updateDiplomatTestRecords() {
        updateTestRecords(diplomatNoticeNumber, "D");
    }

    /**
     * Update existing VIP test records
     */
    public void updateVipTestRecords() {
        updateTestRecords(vipNoticeNumber, "V");
    }

    /**
     * Update existing LOCAL test records
     */
    public void updateLocalTestRecords() {
        updateTestRecords(localNoticeNumber, "S");
    }

    /**
     * Insert record into ocms_valid_offence_notice table
     */
    private void insertValidOffenceNotice(
        String noticeNo,
        String vehicleNo,
        String vehicleRegistrationType,
        String offenceNoticeType,
        double compositionAmount,
        int computerRuleCode,
        String ppCode,
        String lastProcessingStage,
        String nextProcessingStage) {

        String insertNoticeSql = "INSERT INTO " + schema + ".ocms_valid_offence_notice "
            + "(notice_no, vehicle_no, vehicle_category, vehicle_registration_type, offence_notice_type, notice_date_and_time, "
            + "composition_amount, computer_rule_code, pp_code, "
            + "prev_processing_stage, prev_processing_date, "
            + "last_processing_stage, last_processing_date, "
            + "next_processing_stage, next_processing_date, "
            + "subsystem_label, suspension_type, epr_reason_of_suspension, "
            + "cre_user_id, cre_date) "
            + "VALUES (?, ?, 'C', ?, ?, GETDATE(), ?, ?, ?, "
            + "NULL, NULL, ?, GETDATE(), ?, GETDATE(), 'OCMS', NULL, NULL, "
            + "'SYSTEM', GETDATE())";

        jdbcTemplate.update(
            insertNoticeSql,
            noticeNo,
            vehicleNo,
            vehicleRegistrationType,
            offenceNoticeType,
            compositionAmount,
            computerRuleCode,
            ppCode,
            lastProcessingStage,
            nextProcessingStage
        );
    }

    /**
     * Insert record into ocms_offence_notice_detail table
     */
    private void insertOffenceNoticeDetail(String noticeNo) {
        String insertDetailSql = "INSERT INTO " + schema + ".ocms_offence_notice_detail "
            + "(notice_no, cre_date, cre_user_id, "
            + "lta_chassis_number, lta_make_description, lta_primary_colour, lta_diplomatic_flag, "
            + "lta_road_tax_expiry_date, lta_eff_ownership_date, lta_deregistration_date, "
            + "lta_unladen_weight, lta_max_laden_weight) "
            + "VALUES (?, GETDATE(), 'SYSTEM', "
            + "NULL, NULL, NULL, NULL, "
            + "NULL, NULL, NULL, NULL, NULL)";

        jdbcTemplate.update(insertDetailSql, noticeNo);
    }

    /**
     * Insert record into ocms_offence_notice_owner_driver table
     */
    private void insertOffenceNoticeOwnerDriver(String noticeNo) {
        String insertOwnerSql = "INSERT INTO " + schema + ".ocms_offence_notice_owner_driver "
            + "(notice_no, owner_driver_indicator, "
            + "cre_date, cre_user_id, "
            // + "life_status, "
            // + "mha_processing_date_time, "
            + "id_type, id_no, name, offender_indicator, "
            // + "reg_blk_hse_no, reg_street, reg_floor, reg_unit, reg_bldg, reg_postal_code, "
            + "mail_blk_hse_no, mail_street, mail_floor, mail_unit, mail_bldg, mail_postal_code, "
            + "lta_error_code, passport_place_of_issue, "
            + "lta_reg_address_effective_date, lta_mailing_address_effective_date) "
            + "VALUES "
            + "(?, 'O', "
            + "GETDATE(), 'SYSTEM', "
            // + "'A', "
            + "GETDATE(), "
            // + "GETDATE(), "
            + "'1', '', NULL, 'Y', "
            // + "NULL, NULL, NULL, NULL, NULL, NULL, "
            + "NULL, NULL, NULL, NULL, NULL, NULL, "
            + "NULL, NULL, NULL, NULL)";

        jdbcTemplate.update(insertOwnerSql, noticeNo);
    }

    /**
     * Verify that all required records were created successfully
     *
     * @param result TestStepResult to add verification details to
     */
    public void verifyRecordsCreated(TestStepResult result) {
        try {
            String verifyRecordsSql = "SELECT 'CREATED RECORDS - ocms_valid_offence_notice' as table_name, COUNT(*) as count "
                + "FROM " + schema + ".ocms_valid_offence_notice "
                + "WHERE notice_no IN (?, ?, ?) "
                + "UNION ALL "
                + "SELECT 'CREATED RECORDS - ocms_offence_notice_detail' as table_name, COUNT(*) as count "
                + "FROM " + schema + ".ocms_offence_notice_detail "
                + "WHERE notice_no IN (?, ?, ?) "
                + "UNION ALL "
                + "SELECT 'CREATED RECORDS - ocms_offence_notice_owner_driver' as table_name, COUNT(*) as count "
                + "FROM " + schema + ".ocms_offence_notice_owner_driver "
                + "WHERE notice_no IN (?, ?, ?)";

            List<Map<String, Object>> verificationResults = jdbcTemplate.queryForList(verifyRecordsSql,
                diplomatNoticeNumber, vipNoticeNumber, localNoticeNumber,
                diplomatNoticeNumber, vipNoticeNumber, localNoticeNumber,
                diplomatNoticeNumber, vipNoticeNumber, localNoticeNumber);

            // Add verification results to the step result
            for (Map<String, Object> row : verificationResults) {
                result.addDetail(row.get("table_name") + ": " + row.get("count"));
            }
            
            result.addDetail("✅ Record verification completed successfully");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result.addDetail("⚠️ Error verifying records: " + e.getMessage());
            result.addDetail("Stack trace: " + sw.toString());
            // Continue with test execution
        }
        // Method is void, no return value
    }

    /**
    }

    /**
     * Check if diplomat test records exist in database
     *
     * @return true if records exist, false otherwise
     */
    public boolean diplomatTestRecordsExist() {
        return testRecordsExist(diplomatNoticeNumber);
    }

    /**
     * Check if VIP test records exist in database
     *
     * @return true if records exist, false otherwise
     */
    public boolean vipTestRecordsExist() {
        return testRecordsExist(vipNoticeNumber);
    }

    /**
     * Check if LOCAL test records exist in database
     *
     * @return true if records exist, false otherwise
     */
    public boolean localTestRecordsExist() {
        return testRecordsExist(localNoticeNumber);
    }
    
    /**
     * Get notice details from database for a specific notice number
     *
     * @param noticeNumber Notice number to retrieve details for
     * @return Map containing notice details or empty map if not found/error
     */
    public Map<String, Object> getNoticeDetails(String noticeNumber) {
        try {
            String sql = "SELECT * FROM " + schema + ".ocms_valid_offence_notice " +
                         "WHERE notice_no = ?";

            return jdbcTemplate.queryForMap(sql, noticeNumber);
        } catch (Exception e) {
            System.err.println("Error retrieving notice details for notice " + noticeNumber + ": " + e.getMessage());
            // Return empty map instead of throwing exception
            return new HashMap<>();
        }
    }
    
    /**
     * Check if test records exist in database for a specific notice number
     *
     * @param noticeNumber Notice number to check
     * @return true if records exist, false otherwise
     */
    public boolean testRecordsExist(String noticeNumber) {
        try {
            String checkSql = "SELECT COUNT(*) FROM " + schema + ".ocms_valid_offence_notice " +
                              "WHERE notice_no = ?";

            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, noticeNumber);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("Error checking if test records exist for notice " + noticeNumber + ": " + e.getMessage());
            // Return false if there's an error checking the database
            return false;
        }
    }

    /**
     * Create or update test records based on environment
     *
     * @param cleanup true if environment is local or SIT, false for DEV
     * @param result TestStepResult to add details to
     */
    public void createOrUpdateTestRecords(boolean cleanup, TestStepResult result) {
        if (cleanup) {
            // For local and SIT environments, use the original flow (delete then insert)
            cleanupExistingTestRecords();
            result.addDetail("✅ Cleaned up existing test records (local/SIT environment)");

            createDiplomatTestRecords();
            createVipTestRecords();
            createLocalTestRecords();
            result.addDetail("✅ Created new test records for Diplomat and VIP");
        } else {
            // For DEV environment, use select-update-insert flow
            result.addDetail("⚠️ Running in DEV environment - using select-update-insert flow");

            // Check and handle Diplomat records
            if (diplomatTestRecordsExist()) {
                updateDiplomatTestRecords();
                result.addDetail("✅ Updated existing Diplomat test records");
            } else {
                createDiplomatTestRecords();
                result.addDetail("✅ Created new Diplomat test records");
            }

            // Check and handle VIP records
            if (vipTestRecordsExist()) {
                updateVipTestRecords();
                result.addDetail("✅ Updated existing VIP test records");
            } else {
                createVipTestRecords();
                result.addDetail("✅ Created new VIP test records");
            }

            // Check and handle LOCAL records
            if (localTestRecordsExist()) {
                updateLocalTestRecords();
                result.addDetail("✅ Updated existing LOCAL test records");
            } else {
                createLocalTestRecords();
                result.addDetail("✅ Created new LOCAL test records");
            }
        }
    }
}
