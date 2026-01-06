package com.ocmsintranet.apiservice.crud.beans;

/**
 * Centralized system constants for the OCMS application.
 * This class organizes all constants into logical categories to provide a single source of truth
 * for constant values used throughout the application.
 *
 * <p>Categories include:</p>
 * <ul>
 *   <li>User - User-related constants</li>
 *   <li>Status - Status codes for various entities</li>
 *   <li>AppCode - Application codes for responses</li>
 *   <li>Message - Standard messages for user feedback</li>
 *   <li>Subsystem - Subsystem identification and utility methods</li>
 *   <li>Error - Standardized error messages</li>
 *   <li>EntityStatus - Entity-specific status values</li>
 *   <li>ValidationMessage - Validation-related messages</li>
 *   <li>VehicleCategory - Vehicle category codes used in database operations</li>
 *   <li>OffenceType - Offence type codes used in database operations</li>
 *   <li>SuspensionType - Suspension type and source codes</li>
 * </ul>
 */
public class SystemConstant {

    /**
     * System User Constants.
     * Contains constants related to system users and default user identifiers.
     */
    public static class User {
        /**
         * Default system user identifier used for automated operations.
         * This ID is used when operations are performed by the system rather than a specific user.
         */
        public static final String DEFAULT_SYSTEM_USER_ID = "ocmsiz_app_conn";
    }

    /**
     * Status Code Constants.
     * Contains status codes for various entities in the system including transactions,
     * notices, and suspensions.
     */
    public static class Status {
        /**
         * Indicates a transaction is currently being processed.
         */
        public static final String TRANSACTION_PROCESSING = "P";
        
        /**
         * Indicates a transaction has been successfully completed.
         */
        public static final String TRANSACTION_COMPLETED = "C";
        
        /**
         * Indicates a transaction encountered an error during processing.
         */
        public static final String TRANSACTION_ERROR = "E";
        
        /**
         * Indicates a notice is currently active in the system.
         */
        public static final String NOTICE_ACTIVE = "A";
        
        /**
         * Indicates a notice is currently suspended.
         */
        public static final String NOTICE_SUSPENDED = "S";
        
        /**
         * Indicates a notice has been cancelled.
         */
        public static final String NOTICE_CANCELLED = "C";
        
        /**
         * Indicates a permanent suspension.
         */
        public static final String SUSPENSION_TYPE_PERMANENT = "PS";
        
        /**
         * Indicates a temporary suspension.
         */
        public static final String SUSPENSION_TYPE_TEMPORARY = "TS";
    }


    /**
     * Application Code Constants.
     * Contains standard application response codes used across the system.
     */
    public static class AppCode {
        /**
         * Indicates a successful operation with no errors.
         */
        public static final String SUCCESS = "0";
        
        /**
         * Indicates a bad request with invalid parameters.
         */
        public static final String BAD_REQUEST = "400";
        
        /**
         * Indicates a resource was not found.
         */
        public static final String NOT_FOUND = "404";
        
        /**
         * Indicates a server error occurred during processing.
         */
        public static final String SERVER_ERROR = "500";
    }

    /**
     * Subsystem Identification Constants.
     * Contains constants related to subsystem identification and mapping.
     */
    public static class Subsystem {
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
     * Entity Status Constants.
     * Contains common status values used across entity classes.
     */
    public static class EntityStatus {
        /**
         * Status indicating an entity is active.
         */
        public static final String ACTIVE = "A";
        
        /**
         * Status indicating an entity is inactive.
         */
        public static final String INACTIVE = "I";
        
        /**
         * Status indicating an entity is deleted (soft delete).
         */
        public static final String DELETED = "D";
        
        /**
         * Status indicating an entity is pending approval.
         */
        public static final String PENDING = "P";
        
        /**
         * Status indicating an entity is rejected.
         */
        public static final String REJECTED = "R";
    }
        
    /**
     * Vehicle Category Constants.
     * Contains standardized vehicle category codes used in database operations.
     */
    public static class VehicleCategory {
        /**
         * Category code for cars.
         */
        public static final String CAR = "C";
        
        /**
         * Category code for motorcycles.
         */
        public static final String MOTORCYCLE = "M";
        
        /**
         * Category code for heavy vehicles.
         */
        public static final String HEAVY_VEHICLE = "H";
        
        /**
         * Category code for buses.
         */
        public static final String BUS = "B";
        
        /**
         * Category code for all vehicle types.
         */
        public static final String ALL = "A";
    }
    
    /**
     * Offence Type Constants.
     * Contains standardized offence type codes used in database operations.
     */
    public static class OffenceType {
        /**
         * Type code for parking offences.
         */
        public static final String PARKING = "P";
        
        /**
         * Type code for traffic offences.
         */
        public static final String TRAFFIC = "T";
        
        /**
         * Type code for regulatory offences.
         */
        public static final String REGULATORY = "R";
    }
    
    /**
     * Suspension Type Constants.
     * Contains standardized suspension type and source codes used in database operations.
     */
    public static class SuspensionType {
        /**
         * Code for permanent suspension.
         */
        public static final String PERMANENT = "PS";
        
        /**
         * Code for temporary suspension.
         */
        public static final String TEMPORARY = "TS";
        
        /**
         * Code for suspension source from court.
         */
        public static final String SOURCE_COURT = "COURT";
        
        /**
         * Code for suspension source from police.
         */
        public static final String SOURCE_POLICE = "POLICE";
    }
    
    /**
     * Suspension Reason Constants.
     * Contains standardized suspension reason codes used in database operations.
     */
    public static class SuspensionReason {
        /**
         * Reason code for Automated Number Selection suspension.
         */
        public static final String ANS = "ANS";

        /**
         * Reason code for Foreign Vehicle suspension.
         */
        public static final String FOREIGN = "FOR";

        /**
         * Reason code for Duplicate Notice suspension.
         */
        public static final String DUPLICATE = "DBB";

        /**
         * Reason code for Full Payment suspension.
         */
        public static final String FULL_PAYMENT = "FP";

        /**
         * Reason code for Partial Amount suspension.
         */
        public static final String PARTIAL_AMOUNT = "PRA";
    }

    /**
     * Payment Status Constants.
     * Contains standardized payment status codes used in database operations.
     */
    public static class PaymentStatus {
        /**
         * Status code for Full Payment.
         */
        public static final String FULL_PAYMENT = "FP";

        /**
         * Status code for Partial Payment.
         */
        public static final String PARTIAL_PAYMENT = "PP";

        /**
         * Status code for No Payment.
         */
        public static final String NO_PAYMENT = "NP";
    }

    /**
     * Payment Acceptance Constants.
     * Contains standardized payment acceptance codes used in database operations.
     */
    public static class PaymentAcceptance {
        /**
         * Code indicating payment is allowed.
         */
        public static final String ALLOWED = "Y";

        /**
         * Code indicating payment is not allowed.
         */
        public static final String NOT_ALLOWED = "N";
    }

    /**
     * E-Service Message Code Constants.
     * Contains standardized e-service message codes used for eVON synchronization.
     */
    public static class EServiceMessageCode {
        /**
         * Message code for Full Payment (E2).
         */
        public static final String FULL_PAYMENT = "E2";

        /**
         * Message code for Partial Payment (E12).
         */
        public static final String PARTIAL_PAYMENT = "E12";

        /**
         * Message code for payment allowed (E1).
         * Used when creating notices where payment is accepted.
         */
        public static final String ALLOW_PAYMENT = "E1";

        /**
         * Message code for ANS (Automated Number Selection) - E5.
         * Used when an_flag = Y, indicating ANS suspension with payment not allowed.
         */
        public static final String ANS = "E5";

        /**
         * Message code for foreign vehicles (F type) - E6.
         * Used when creating notices for foreign registered vehicles.
         */
        public static final String FOREIGN_VEHICLE = "E6";
    }
    
    /**
     * Offence Notice Type Constants.
     * Contains standardized offence notice type codes used in the system.
     */
    public static class OffenceNoticeType {
        /**
         * Type code for electronic notices.
         */
        public static final String ELECTRONIC = "E";
        
        /**
         * Type code for manual notices.
         */
        public static final String MANUAL = "M";
    }
    
    /**
     * Processing Stage Constants.
     * Contains standardized processing stage codes used in workflow operations.
     */
    public static class ProcessingStage {
        /**
         * Stage code for New Processing Application.
         */
        public static final String NEW_PROCESSING_APPLICATION = "NPA";
        
        /**
         * Stage code for Registration of Vehicle.
         */
        public static final String REGISTRATION_OF_VEHICLE = "ROV";
        
        /**
         * Stage code for Demand Note 1.
         */
        public static final String DEMAND_NOTE_1 = "DN1";
        
        /**
         * Stage code for Registration Document 1.
         */
        public static final String REGISTRATION_DOCUMENT_1 = "RD1";
    }
    
    /**
     * Military Entity Constants.
     * Contains standardized military entity and address information used across the system.
     */
    public static class Military {
        /**
         * Military Owner Constants.
         * Contains constants related to military owner information.
         */
        public static class Owner {
            /**
             * Name of the military entity (MINDEF).
             */
            public static final String NAME = "MINDEF";
            
            /**
             * ID type for the military entity.
             */
            public static final String ID_TYPE = "B";
            
            /**
             * ID number for the military entity.
             */
            public static final String ID_NO = "T08GA0011B";
            
            /**
             * Offender indicator for the military entity.
             */
            public static final String OFFENDER_INDICATOR = "Y";
            
            /**
             * Driver indicator for the military entity (Owner).
             */
            public static final String DRIVER_INDICATOR = "O";
        }
        
        /**
         * Military Address Constants.
         * Contains constants related to military address information.
         */
        public static class Address {
            /**
             * Address type for military addresses.
             */
            public static final String TYPE = "mha_reg";
            
            /**
             * Block/house number for the military address.
             */
            public static final String BLK_HSE_NO = "151";
            
            /**
             * Street name for the military address.
             */
            public static final String STREET_NAME = "Choa Chu Kang Way";
            
            /**
             * Building name for the military address.
             */
            public static final String BLDG_NAME = "Kranji Camp 3";
            
            /**
             * Postal code for the military address.
             */
            public static final String POSTAL_CODE = "688248";
        }
    }
    public static class Rep{

        public static final String SENDER = "REP";
        public static final String RRECEIVER = "URA";
        public static final String SERVICE_CODE = "REPCCSNOPO";
        public static final String REP_TXN_CODE ="REPCCSNOPO";

        public static final String INVALID_INPUT = "Invalid input format or failed validation";
        public static final String SUCCESS_MESSAGE ="OK";
        public static final String UNAUTHORIZED_MESSAGE="Unauthorized Access";
        public static final String DUPLICATED_MESSAGE ="Notice no Already Exist";
        public static final String ERROR_5000 ="Something went wrong. Please try again later";

        public static final String STEP_1="STEP 1: Subscription key is NULL for transactionId=";
        public static final String STEP_2="STEP 2: Validating payload length for transactionId=";
        public static final String STEP_3="STEP 3: Payload validation successful for transactionId=";
        public static final String STEP_4="STEP 4: Mapping and serializing data for transactionId=";
        public static final String STEP_5="STEP 5: Failed Subscription key validation for transactionId=";
        public static final String STEP_6="STEP 6: Subscription key validation successful for transactionId=";
        public static final String STEP_7="STEP 7: Notice no already Exist for transactionId={}";
        public static final String STEP_8="STEP 8: Runtime exception occurred for transactionId=";
        public static final String STEP_9="STEP 9: HTTP client error for transactionId=";
        public static final String STEP_10="STEP 10: HTTP client error for transactionId=";
        public static final String STEP_11="API response received with status: SUCCESS for transactionId=";

    }

    /**
     * Vehicle Number Constants.
     * Contains standardized vehicle number values used for unlicensed parking vehicles.
     */
    public static class VehicleNumber {
        /**
         * Standard format for unlicensed parking vehicle number (without dot).
         */
        public static final String UNLICENSED_PARKING = "N.A";
    }

}
