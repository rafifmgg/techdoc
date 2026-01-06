package com.ocmsintranet.cronservice.crud.beans;

/**
 * Centralized system constants for the OCMS Cron Service application.
 * This class organizes all constants into logical categories to provide a
 * single source of truth
 * for constant values used throughout the cron service application.
 *
 * Each inner class represents a specific category of constants related to
 * a particular domain or functionality within the system.
 *
 * @see User For user-related constants
 * @see CronJob For cron job-related constants
 * @see BatchJob For batch job-related constants
 * @see Notification For notification-related constants
 * @see SuspensionType For suspension type-related constants
 * @see SuspensionReason For suspension reason-related constants
 * @see Agency For agency-related constants
 * @see IdType For identification type-related constants
 *      <li>CronJob - Job status tracking constants</li>
 *      <li>Notification - Notification-related constants</li>
 *      <li>Database - Database-related constants</li>
 *      <li>BatchJob - Batch job status constants</li>
 *      <li>SuspensionType - Suspension type constants</li>
 *      <li>SuspensionReason - Suspension reason codes</li>
 *      <li>Agency - Agency-related constants</li>
 *      <li>IdType - Identification type constants</li>
 *      </ul>
 */
public class SystemConstant {

    /**
     * System User Constants.
     * Contains constants related to system users and default user identifiers.
     */
    public static class User {
        /**
         * Default system user identifier used for automated operations.
         * This ID is used when operations are performed by the system rather than a
         * specific user.
         */
        public static final String DEFAULT_SYSTEM_USER_ID = "ocmsiz_app_conn";
    }

    /**
     * Cron Job Constants.
     * Contains constants specific to cron job operations.
     */
    public static class CronJob {
        /**
         * Status indicating a cron job is currently running.
         */
        public static final String STATUS_RUNNING = "RUNNING";

        /**
         * Status indicating a cron job has completed successfully.
         */
        public static final String STATUS_COMPLETED = "COMPLETED";

        /**
         * Status indicating a cron job has failed.
         */
        public static final String STATUS_FAILED = "FAILED";

        /**
         * Status indicating a cron job is scheduled but not yet started.
         */
        public static final String STATUS_SCHEDULED = "SCHEDULED";

        /**
         * Status indicating a cron job has been cancelled.
         */
        public static final String STATUS_CANCELLED = "CANCELLED";

        /**
         * Status indicating a cron job is paused.
         */
        public static final String STATUS_PAUSED = "PAUSED";

        /**
         * Status indicating a cron job is waiting for a dependency.
         */
        public static final String STATUS_WAITING = "WAITING";

        /**
         * Status indicating no data was found for processing.
         */
        public static final String STATUS_NO_DATA = "NO_DATA";

        /**
         * Default log text for job start.
         */
        public static final String LOG_JOB_START = "Job Start";

        /**
         * Default log text for no data found.
         */
        public static final String LOG_NO_DATA_FOUND = "NO DATA FOUND";

        /**
         * Default log prefix for error messages.
         */
        public static final String LOG_ERROR_PREFIX = "Error: ";
    }

    /**
     * Batch Job Constants.
     * Contains constants related to batch job database records.
     */
    public static class BatchJob {
        /**
         * Status indicating a batch job is running.
         */
        public static final String STATUS_RUNNING = "R";

        /**
         * Status indicating a batch job completed successfully.
         */
        public static final String STATUS_COMPLETED = "C";

        /**
         * Status indicating a batch job failed.
         */
        public static final String STATUS_FAILED = "F";

        /**
         * Status indicating no data was found for processing.
         */
        public static final String STATUS_NO_DATA = "N";
    }

    /**
     * Notification Constants.
     * Contains constants related to notification types in the system.
     */
    public static class Notification {
        /**
         * Success status.
         */
        public static final String STATUS_SENT = "S";

        /**
         * Error status.
         */
        public static final String STATUS_FAILED = "E";

    }

    /**
     * Suspension Type Constants.
     * Contains constants related to suspension types in the system.
     */
    public static class SuspensionType {
        /**
         * Temporary suspension type.
         */
        public static final String TEMPORARY = "TS";

        /**
         * Permanent suspension type.
         */
        public static final String PERMANENT = "PS";
    }

    /**
     * Suspension Reason Constants.
     * Contains constants related to suspension reason codes in the system.
     */
    public static class SuspensionReason {
        /**
         * Full Payment - used when full payment is received.
         */
        public static final String FULL_PAYMENT = "FP";

        /**
         * Partial Amount - used when partial payment is received.
         */
        public static final String PARTIAL_AMOUNT = "PRA";

        /**
         * Rest in Peace - used for deceased individuals.
         */
        public static final String RIP = "RIP";

        /**
         * Rest in Peace 2 - used when date of death is before offence date.
         */
        public static final String RP2 = "RP2";

        /**
         * No Registered Owner - used for invalid addresses.
         */
        public static final String NRO = "NRO";

        /**
         * Registered Owner Verification - used for LTA ROV process.
         */
        public static final String ROV = "ROV";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String NPA = "NPA";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String ENA = "ENA";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String RD1 = "RD1";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String RD2 = "RD2";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String RD3 = "RD3";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String RR3 = "RR3";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String DN1 = "DN1";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String DN2 = "DN2";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String DR3 = "DR3";

        /**
         * No Processing Allowed - used for invalid addresses.
         */
        public static final String ACR = "ACR";

        /**
         * System suspension reason.
         */
        public static final String SYS = "SYS";

        /**
         * HST House Tenants (looping)
         */
        public static final String HST = "HST";

        /**
         * Payment Amount Mismatch - used for TS-PAM exceptions.
         */
        public static final String PAYMENT_AMOUNT_MISMATCH = "PAM";

        /**
         * UNC Returned as unclaimed.
         */
        public static final String UNC = "UNC";

        /**
         * OLD Under Investigation - used for VIP vehicles during investigation period (21 days).
         */
        public static final String OLD = "OLD";

        /**
         * CLV VIP Clearance (Looping) - used for VIP vehicles at RR3/DR3 stages.
         */
        public static final String CLV = "CLV";
    }

    /**
     * Subsystem Identification Constants.
     * Contains constants related to subsystem identification and mapping.
     */
    public static class Subsystem {
        /**
         * Code for CES subsystem.
         */
        public static final String CES_CODE = "001";

        /**
         * Code for EEPS subsystem.
         */
        public static final String EEPS_CODE = "003";

        /**
         * Code for OCMS subsystem.
         */
        public static final String OCMS_CODE = "004";

        /**
         * Code for PLUS subsystem.
         */
        public static final String PLUS_CODE = "005";

        /**
         * Code for MEV vehicle subsystem (006-010).
         */
        public static final String MEV_CODE_006 = "006";
        public static final String MEV_CODE_007 = "007";
        public static final String MEV_CODE_008 = "008";
        public static final String MEV_CODE_009 = "009";
        public static final String MEV_CODE_010 = "010";

        /**
         * Code for REPCCS ENC subsystem.
         */
        public static final String REPCCS_ENC_CODE_020 = "020";
        public static final String REPCCS_ENC_CODE_021 = "021";

        /**
         * Code for REPCCS EHT subsystem (part of MEV vehicle).
         */
        public static final String REPCCS_EHT_CODE = "023";

        /**
         * Name for CES subsystem.
         */
        public static final String CES_NAME = "CES";

        /**
         * Name for EEPS subsystem.
         */
        public static final String EEPS_NAME = "EEPS";

        /**
         * Name for OCMS subsystem.
         */
        public static final String OCMS_NAME = "OCMS";

        /**
         * Name for PLUS subsystem.
         */
        public static final String PLUS_NAME = "PLUS";

        /**
         * Name for MEV vehicle subsystem.
         */
        public static final String MEV_NAME = "MEV";

        /**
         * Name for REPCCS ENC subsystem.
         */
        public static final String REPCCS_ENC_NAME = "REPCCS_ENC";

        /**
         * Name for REPCCS EHT subsystem.
         */
        public static final String REPCCS_EHT_NAME = "REPCCS_EHT";

    }

    /**
     * Agency Constants.
     * Contains constants related to agency sources in the system.
     */
    public static class Agency {
        /**
         * OCMS system identifier.
         */
        public static final String OCMS = "OCMS";

        /**
         * Ministry of Home Affairs identifier.
         */
        public static final String MHA = "MHA";

        /**
         * Land Transport Authority identifier.
         */
        public static final String LTA = "LTA";
    }

    /**
     * ID Type Constants.
     * Contains constants related to identification types in the system.
     */
    public static class IdType {
        /**
         * National Registration Identity Card (NRIC) type.
         */
        public static final String NRIC = "N";

        /**
         * Foreign Identification Number (FIN) type.
         */
        public static final String FIN = "F";

        /**
         * Unique Entity Number (UEN) type for businesses.
         */
        public static final String UEN = "U";
    }

    /**
     * Address Type Constants.
     * Contains constants related to address types in the system.
     *
     * These constants are used to identify different types of addresses stored in
     * the database,
     * particularly for LTA (Land Transport Authority) related addresses. Using
     * these constants
     * instead of hardcoded strings ensures consistency across the application and
     * makes
     * the code more maintainable.
     */
    public static class AddressType {
        /**
         * LTA Registration address type.
         */
        public static final String LTA_REGISTRATION = "lta_reg";

        /**
         * LTA Mailing address type.
         */
        public static final String LTA_MAILING = "lta_mail";
    }

    /**
     * Owner Driver Indicator Constants.
     * Contains constants related to owner/driver indicators in the system.
     *
     * These constants are used to distinguish between vehicle owners and drivers
     * in the offence notice processing system. Using these constants instead of
     * hardcoded values improves code readability and maintainability, especially
     * when working with database records that store this information.
     */
    public static class OwnerDriverIndicator {
        /**
         * Owner indicator.
         */
        public static final String OWNER = "O";

        /**
         * Driver indicator.
         */
        public static final String DRIVER = "D";
    }

    /**
     * Offender Indicator Constants.
     * Contains constants related to offender indicators in the system.
     *
     * These constants are used to indicate whether an individual is currently
     * considered an offender in the context of an offence notice. Using these
     * constants instead of hardcoded values improves code readability and
     * maintainability, especially when working with database records that
     * track offender status.
     */
    public static class OffenderIndicator {
        /**
         * Current offender indicator.
         */
        public static final String CURRENT = "Y";

        /**
         * Not current offender indicator.
         */
        public static final String NOT_CURRENT = "N";
    }

    /**
     * Cryptographic Operation Constants.
     * Contains constants related to cryptographic operations in the system.
     *
     * These constants are used to specify the type of cryptographic operation
     * being performed, particularly for encryption and decryption processes.
     * Using these constants instead of hardcoded values improves code readability
     * and maintainability.
     */
    /**
     * Cryptographic Operation Constants.
     * Contains constants for COMCRYPT cryptographic operations.
     */
    public static class CryptOperation {
        /**
         * Encryption operation type.
         */
        public static final String ENCRYPTION = "ENCRYPTION";
        
        /**
         * Decryption operation type.
         */
        public static final String DECRYPTION = "DECRYPTION";
        
        /**
         * type.
         */
        public static final String SLIFT = "SLIFT";
        public static final String PGP = "PGP";
        
        /**
         * Operation status constants
         */
        public static class Status {
            public static final String REQUESTED = "REQUESTED";
            public static final String IN_PROGRESS = "IN_PROGRESS";
            public static final String PROCESSED = "PROCESSED";
            public static final String UPLOADED = "UPLOADED";
            public static final String COMPLETED = "COMPLETED";
            public static final String COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS";
            public static final String FAILED = "FAILED";
            public static final String TIMEOUT = "TIMEOUT";            
        }
    }

    /**
     * Payment Status Constants.
     * Contains constants related to payment status in the system.
     */
    public static class PaymentStatus {
        /**
         * Full Payment status - indicates notice has been fully paid.
         */
        public static final String FULL_PAYMENT = "FP";
    }

    /**
     * Payment Acceptance Constants.
     * Contains constants related to payment acceptance flags in the system.
     */
    public static class PaymentAcceptance {
        /**
         * Payment acceptance not allowed.
         */
        public static final String NOT_ALLOWED = "N";

        /**
         * Payment acceptance allowed.
         */
        public static final String ALLOWED = "Y";
    }

    /**
     * eService Message Code Constants.
     * Contains constants related to eService message codes used for Internet portal.
     */
    public static class EServiceMessageCode {
        /**
         * Full Payment message code - displayed when notice is fully paid.
         */
        public static final String FULL_PAYMENT = "E2";

        /**
         * Partial Payment message code - displayed when partial payment is received.
         */
        public static final String PARTIAL_PAYMENT = "E12";
    }

    /**
     * Refund Reason Constants.
     * Contains constants related to refund reason codes in the system.
     */
    public static class RefundReason {
        /**
         * Double Payment - refund due to duplicate payment on same notice.
         */
        public static final String DOUBLE_PAYMENT = "DBP";

        /**
         * Over Payment - refund due to payment exceeding amount payable.
         */
        public static final String OVER_PAYMENT = "OVP";

        /**
         * Apply Waiver - refund due to waiver application.
         */
        public static final String APPLY_WAIVER = "APP";
    }

}
