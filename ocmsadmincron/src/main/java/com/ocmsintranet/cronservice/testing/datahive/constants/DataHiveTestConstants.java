package com.ocmsintranet.cronservice.testing.datahive.constants;

/**
 * Constants untuk DataHive testing scenarios
 * Berdasarkan pattern LTA testing flows
 */
public class DataHiveTestConstants {

    // Test User untuk audit trails
    public static final String TEST_USER = "DATAHIVE_TEST";

    // Test NRIC samples (valid format untuk Singapore)
    public static final String[] VALID_NRIC_SAMPLES = {
        "S1234567A", "S2345678B", "S3456789C", "S4567890D", "S5678901E"
    };

    // Test FIN samples (valid format untuk Singapore)
    public static final String[] VALID_FIN_SAMPLES = {
        "G1234567A", "G2345678B", "G3456789C", "F4567890D", "F5678901E"
    };

    // Test Passport samples
    public static final String[] VALID_PASSPORT_SAMPLES = {
        "E1234567", "E2345678", "E3456789", "A4567890", "A5678901"
    };

    // Invalid ID samples untuk error testing
    public static final String[] INVALID_ID_SAMPLES = {
        "INVALID123", "ABC", "12345", "", "TOOLONG1234567890"
    };

    // Test address data untuk address verification
    public static final TestAddressData[] ADDRESS_TEST_DATA = {
        new TestAddressData("123", "Orchard Road", "04", "01", "Orchard Plaza", "238858"),
        new TestAddressData("456", "Marina Bay", "10", "05", "Marina Bay Sands", "018956"),
        new TestAddressData("789", "Sentosa Island", "", "", "", "098000"),
        new TestAddressData("", "Incomplete Street", "", "", "", ""),
        new TestAddressData("999", "Invalid Address Name", "99", "99", "Non Existent", "000000")
    };

    // Batch processing configurations
    public static final int DEFAULT_BATCH_SIZE = 50;
    public static final int DEFAULT_CHUNK_SIZE = 10;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long TIMEOUT_SECONDS = 30;

    // Success criteria thresholds
    public static final double MINIMUM_SUCCESS_RATE = 0.95;
    public static final double EXPECTED_BATCH_SUCCESS_RATE = 0.90;
    public static final long MAX_RESPONSE_TIME_MS = 5000;

    // Database table names
    public static final String TABLE_OFFENCE_NOTICE_OWNER_DRIVER = "ocms_offence_notice_owner_driver";
    public static final String TABLE_OFFENCE_NOTICE_OWNER_DRIVER_ADDR = "ocms_offence_notice_owner_driver_addr";
    public static final String TABLE_VALID_OFFENCE_NOTICE = "ocms_valid_offence_notice";

    // Test data prefixes
    public static final String TEST_NOTICE_PREFIX = "DHTEST";
    public static final String TEST_VEHICLE_PREFIX = "DHTEST";

    // Contact lookup result statuses
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_NOT_FOUND = "NOT_FOUND";
    public static final String STATUS_INVALID = "INVALID";

    // Data quality audit metrics
    public static final String[] QUALITY_METRICS = {
        "ACCURACY", "COMPLETENESS", "CONSISTENCY", "TIMELINESS", "VALIDITY"
    };

    // Error scenario types
    public static final String[] ERROR_SCENARIOS = {
        "NETWORK_TIMEOUT", "INVALID_CREDENTIALS", "RATE_LIMIT_EXCEEDED", 
        "SERVICE_UNAVAILABLE", "MALFORMED_RESPONSE"
    };

    /**
     * Test address data structure
     */
    public static class TestAddressData {
        public final String blkHseNo;
        public final String streetName;
        public final String floorNo;
        public final String unitNo;
        public final String bldgName;
        public final String postalCode;

        public TestAddressData(String blkHseNo, String streetName, String floorNo, 
                              String unitNo, String bldgName, String postalCode) {
            this.blkHseNo = blkHseNo;
            this.streetName = streetName;
            this.floorNo = floorNo;
            this.unitNo = unitNo;
            this.bldgName = bldgName;
            this.postalCode = postalCode;
        }
    }

    /**
     * Generate test notice number dengan timestamp
     */
    public static String generateTestNoticeNumber() {
        return TEST_NOTICE_PREFIX + String.format("%04d", (int) (Math.random() * 9999));
    }

    /**
     * Generate test vehicle number
     */
    public static String generateTestVehicleNumber() {
        return TEST_VEHICLE_PREFIX + String.format("%04d", (int) (Math.random() * 9999));
    }

    /**
     * Get random valid NRIC untuk testing
     */
    public static String getRandomValidNric() {
        return VALID_NRIC_SAMPLES[(int) (Math.random() * VALID_NRIC_SAMPLES.length)];
    }

    /**
     * Get random valid FIN untuk testing
     */
    public static String getRandomValidFin() {
        return VALID_FIN_SAMPLES[(int) (Math.random() * VALID_FIN_SAMPLES.length)];
    }

    /**
     * Get random valid Passport untuk testing
     */
    public static String getRandomValidPassport() {
        return VALID_PASSPORT_SAMPLES[(int) (Math.random() * VALID_PASSPORT_SAMPLES.length)];
    }

    /**
     * Get random invalid ID untuk error testing
     */
    public static String getRandomInvalidId() {
        return INVALID_ID_SAMPLES[(int) (Math.random() * INVALID_ID_SAMPLES.length)];
    }
}
