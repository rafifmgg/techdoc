package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeRepository;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.RepWebHookPayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RepWebHookPayloadValidator {

    @Autowired
    private OffenceRuleCodeRepository offenceRuleCodeRepository;

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern DECIMAL_PATTERN_4_2 = Pattern.compile("^\\d{1,4}\\.\\d{2}$");
    private static final Pattern DECIMAL_PATTERN_1_6 = Pattern.compile("^\\d\\.\\d{6}$");
    private static final Pattern DECIMAL_PATTERN_3_6 = Pattern.compile("^\\d{1,3}\\.\\d{6}$");

    // SubSystemLabel validation - valid prefixes (020, 021, 023 only)
    private static final Set<Integer> VALID_SUBSYSTEM_PREFIXES = Set.of(20, 21, 23);

    public void validateMandatoryFields(RepWebHookPayloadDto payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        // 1. Mandatory Field Validations (1-5)
        validateTransactionID(payload);
        validateCaptureDateTime(payload);
        validateOffenceCode(payload);
        validateEnforcementCarParkID(payload);
        validateEnforcementCarParkName(payload);

        // 2. Vehicle Identification Validations (5-8)
        boolean isUnidentifiedVehicle = detectUnidentifiedVehicle(payload);
        validateVehicleRegistration(payload, isUnidentifiedVehicle);
        validateLicencePlate(payload);
        validateVehicleCategory(payload, isUnidentifiedVehicle);

        // 3. ANS Flag Validation (9-10)
        validateANSFlag(payload);
        validateSubSystemLabel(payload);

        // 4. Fine Amount Validation (11)
        validateFineAmount(payload);

        // 5. DateTime Future Validations (12-14)
        validateCreatedDateTime(payload);
        validateParkingEntryDateTime(payload);
        validateParkingEndDateTime(payload);

        // 6. Parking DateTime Relationship Validation (15)
        validateParkingDateTimeRelationship(payload);

        // 7. Conditional Field Validation (16)
        validateViolationCodeAndChargeAmount(payload);

        // 8. Length & Format Validations (17-38)
        validateLengthAndFormat(payload);

        // 9. Offence Type Validation
        validateOffenceType(payload);
    }

    // ==================== Mandatory Field Validations ====================

    private static void validateTransactionID(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getTransactionId())) {
            throw new IllegalArgumentException("Missing Mandatory Field: TransactionID");
        }
    }

    private static void validateCaptureDateTime(RepWebHookPayloadDto payload) {
        if (payload.getCaptureDateTime() == null) {
            throw new IllegalArgumentException("Missing Mandatory Field: CaptureDateTime");
        }
        if (payload.getCaptureDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("CaptureDateTime cannot be in the future");
        }
    }

    private static void validateOffenceCode(RepWebHookPayloadDto payload) {
        if (payload.getOffenceCode() == null) {
            throw new IllegalArgumentException("Missing Mandatory Field: OffenceCode");
        }
    }

    private static void validateEnforcementCarParkID(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getEnforcementCarParkId())) {
            throw new IllegalArgumentException("Missing Mandatory Field: EnforcementCarParkID");
        }
    }

    private static void validateEnforcementCarParkName(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getEnforcementCarParkName())) {
            throw new IllegalArgumentException("Missing Mandatory Field: EnforcementCarParkName");
        }
    }

    // ==================== Vehicle Identification Validations ====================

    private static boolean detectUnidentifiedVehicle(RepWebHookPayloadDto payload) {
        return "0".equals(payload.getVehicleCategory()) &&
               "X".equals(payload.getVehicleRegistration()) &&
               "N.A.".equals(payload.getLicencePlate());
    }

    private static void validateVehicleRegistration(RepWebHookPayloadDto payload, boolean isUnidentifiedVehicle) {
        if (isEmpty(payload.getVehicleRegistration())) {
            throw new IllegalArgumentException("Missing Mandatory Field: VehicleRegistration");
        }

        String vehicleReg = payload.getVehicleRegistration();
        if (isUnidentifiedVehicle) {
            if (!"X".equals(vehicleReg)) {
                throw new IllegalArgumentException("VehicleRegistration must be 'X' for unidentified vehicles");
            }
        } else {
            if (!"S".equals(vehicleReg) && !"F".equals(vehicleReg)) {
                throw new IllegalArgumentException("VehicleRegistration must be 'S' or 'F' (or 'X' for unidentified vehicles only)");
            }
        }
    }

    private static void validateLicencePlate(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getLicencePlate())) {
            throw new IllegalArgumentException("Missing Mandatory Field: LicencePlate");
        }
    }

    private static void validateVehicleCategory(RepWebHookPayloadDto payload, boolean isUnidentifiedVehicle) {
        if (isEmpty(payload.getVehicleCategory())) {
            throw new IllegalArgumentException("Missing Mandatory Field: VehicleCategory");
        }

        String category = payload.getVehicleCategory();
        if (isUnidentifiedVehicle) {
            if (!"0".equals(category)) {
                throw new IllegalArgumentException("VehicleCategory must be '0' for unidentified vehicles");
            }
        } else {
            if (!category.startsWith("C") && !category.startsWith("H") && !category.startsWith("M")) {
                throw new IllegalArgumentException("VehicleCategory must start with 'C', 'H', or 'M' for normal vehicles");
            }
        }
    }

    // ==================== ANS Flag Validation ====================

    private static void validateANSFlag(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getAnsFlag())) {
            throw new IllegalArgumentException("Missing Mandatory Field: ANSFlag");
        }
        if (!"Y".equals(payload.getAnsFlag()) && !"N".equals(payload.getAnsFlag())) {
            throw new IllegalArgumentException("ANSFlag must be 'Y' or 'N'");
        }
    }

    private static void validateSubSystemLabel(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getSubSystemLabel())) {
            throw new IllegalArgumentException("Missing Mandatory Field: SubSystemLabel");
        }

        String subSystemLabel = payload.getSubSystemLabel();

        // Validate length (must be 8 characters)
        if (subSystemLabel.length() != 8) {
            throw new IllegalArgumentException("SubSystemLabel must be exactly 8 characters");
        }

        // Extract and validate first 3 digits
        String firstThreeChars = subSystemLabel.substring(0, 3);

        // Check if first 3 characters are numeric
        if (!firstThreeChars.matches("\\d{3}")) {
            throw new IllegalArgumentException("SubSystemLabel first 3 characters must be numeric");
        }

        // Parse and check if value is in valid set
        int firstThreeDigits = Integer.parseInt(firstThreeChars);
        if (!VALID_SUBSYSTEM_PREFIXES.contains(firstThreeDigits)) {
            throw new IllegalArgumentException("SubSystemLabel first 3 digits must be one of: 020, 021, 023");
        }
    }

    // ==================== Fine Amount Validation ====================

    private static void validateFineAmount(RepWebHookPayloadDto payload) {
        if (isEmpty(payload.getFineAmount())) {
            throw new IllegalArgumentException("Missing Mandatory Field: FineAmount");
        }

        try {
            BigDecimal fineAmount = new BigDecimal(payload.getFineAmount());
            if (fineAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("FineAmount must be greater than 0");
            }

            if (!DECIMAL_PATTERN_4_2.matcher(payload.getFineAmount()).matches()) {
                throw new IllegalArgumentException("FineAmount must be in format 9999.99 (max 4 integer digits, exactly 2 decimal digits)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("FineAmount must be a valid number");
        }
    }

    // ==================== DateTime Future Validations ====================

    private static void validateCreatedDateTime(RepWebHookPayloadDto payload) {
        if (payload.getCreatedDateTime() != null && payload.getCreatedDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("CreatedDateTime cannot be in the future");
        }
    }

    private static void validateParkingEntryDateTime(RepWebHookPayloadDto payload) {
        if (payload.getParkingEntryDateTime() != null && payload.getParkingEntryDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("ParkingEntryDateTime cannot be in the future");
        }
    }

    private static void validateParkingEndDateTime(RepWebHookPayloadDto payload) {
        if (payload.getParkingEndDateTime() != null && payload.getParkingEndDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("ParkingEndDateTime cannot be in the future");
        }
    }

    // ==================== Parking DateTime Relationship Validation ====================

    private static void validateParkingDateTimeRelationship(RepWebHookPayloadDto payload) {
        if (payload.getParkingStartDateTime() != null && payload.getParkingEndDateTime() != null) {
            if (payload.getParkingStartDateTime().isAfter(payload.getParkingEndDateTime()) ||
                payload.getParkingStartDateTime().isEqual(payload.getParkingEndDateTime())) {
                throw new IllegalArgumentException("ParkingStartDateTime must be before ParkingEndDateTime");
            }
        }
    }

    // ==================== Conditional Field Validation ====================

    private static void validateViolationCodeAndChargeAmount(RepWebHookPayloadDto payload) {
        boolean hasViolationCode = !isEmpty(payload.getViolationCode());
        boolean hasChargeAmount = !isEmpty(payload.getChargeAmount());

        if (hasViolationCode) {
            if (!hasChargeAmount) {
                throw new IllegalArgumentException("ChargeAmount must not be null when ViolationCode has value");
            }

            try {
                BigDecimal chargeAmount = new BigDecimal(payload.getChargeAmount());
                if (chargeAmount.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("ChargeAmount must not be 0.00 when ViolationCode has value");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ChargeAmount must be a valid number");
            }
        } else {
            if (hasChargeAmount) {
                try {
                    BigDecimal chargeAmount = new BigDecimal(payload.getChargeAmount());
                    if (chargeAmount.compareTo(BigDecimal.ZERO) != 0) {
                        throw new IllegalArgumentException("ChargeAmount must be empty or 0.00 when ViolationCode is empty");
                    }
                } catch (NumberFormatException e) {
                    // If ViolationCode is empty and ChargeAmount is not a valid number, it's acceptable
                }
            }
        }
    }

    // ==================== Length & Format Validations ====================

    private static void validateLengthAndFormat(RepWebHookPayloadDto payload) {
        // 17. TransactionID - Max 20 characters, alphanumeric only
        if (!isEmpty(payload.getTransactionId())) {
            if (payload.getTransactionId().length() > 20) {
                throw new IllegalArgumentException("TransactionID must not exceed 20 characters");
            }
            if (!ALPHANUMERIC_PATTERN.matcher(payload.getTransactionId()).matches()) {
                throw new IllegalArgumentException("TransactionID must be alphanumeric only");
            }
        }

        // 18. SubSystemLabel - Already validated in validateSubSystemLabel method

        // 19. EnforcementCarParkID - Max 20 characters, alphanumeric only
        if (!isEmpty(payload.getEnforcementCarParkId())) {
            if (payload.getEnforcementCarParkId().length() > 20) {
                throw new IllegalArgumentException("EnforcementCarParkID must not exceed 20 characters");
            }
            if (!ALPHANUMERIC_PATTERN.matcher(payload.getEnforcementCarParkId()).matches()) {
                throw new IllegalArgumentException("EnforcementCarParkID must be alphanumeric only");
            }
        }

        // 20. EnforcementCarParkName - Max 100 characters
        if (!isEmpty(payload.getEnforcementCarParkName())) {
            if (payload.getEnforcementCarParkName().length() > 100) {
                throw new IllegalArgumentException("EnforcementCarParkName must not exceed 100 characters");
            }
        }

        // 21. OperatorID - Max 10 characters, alphanumeric only
        if (!isEmpty(payload.getOperatorId())) {
            if (payload.getOperatorId().length() > 10) {
                throw new IllegalArgumentException("OperatorID must not exceed 10 characters");
            }
            if (!ALPHANUMERIC_PATTERN.matcher(payload.getOperatorId()).matches()) {
                throw new IllegalArgumentException("OperatorID must be alphanumeric only");
            }
        }

        // 22. ChargeAmount - Format: max 4 integer digits, 2 decimal digits (9999.99)
        if (!isEmpty(payload.getChargeAmount())) {
            if (!DECIMAL_PATTERN_4_2.matcher(payload.getChargeAmount()).matches()) {
                throw new IllegalArgumentException("ChargeAmount must be in format 9999.99 (max 4 integer digits, 2 decimal digits)");
            }
        }

        // 23. VehicleRegistration - Max 2 characters
        if (!isEmpty(payload.getVehicleRegistration())) {
            if (payload.getVehicleRegistration().length() > 2) {
                throw new IllegalArgumentException("VehicleRegistration must not exceed 2 characters");
            }
        }

        // 24. VehicleCategory - Max 30 characters
        if (!isEmpty(payload.getVehicleCategory())) {
            if (payload.getVehicleCategory().length() > 30) {
                throw new IllegalArgumentException("VehicleCategory must not exceed 30 characters");
            }
        }

        // 25. LicencePlate - Max 14 characters
        if (!isEmpty(payload.getLicencePlate())) {
            if (payload.getLicencePlate().length() > 14) {
                throw new IllegalArgumentException("LicencePlate must not exceed 14 characters");
            }
        }

        // 26. OffenceCode - Max 8 characters
        if (payload.getOffenceCode() != null) {
            String offenceCodeStr = String.valueOf(payload.getOffenceCode());
            if (offenceCodeStr.length() > 8) {
                throw new IllegalArgumentException("OffenceCode must not exceed 8 characters");
            }
        }

        // 27. NOPONumber - Max 10 characters
        if (!isEmpty(payload.getNopoNumber())) {
            if (payload.getNopoNumber().length() > 10) {
                throw new IllegalArgumentException("NOPONumber must not exceed 10 characters");
            }
        }

        // 28. ANSFlag - Max 1 character
        if (!isEmpty(payload.getAnsFlag())) {
            if (payload.getAnsFlag().length() > 1) {
                throw new IllegalArgumentException("ANSFlag must not exceed 1 character");
            }
        }

        // 29. OBULabel - Max 10 characters (optional)
        if (!isEmpty(payload.getObuLabel())) {
            if (payload.getObuLabel().length() > 10) {
                throw new IllegalArgumentException("OBULabel must not exceed 10 characters");
            }
        }

        // 30. OBULatitude - Format: max 1 integer digit, 6 decimal digits (9.999999)
        if (!isEmpty(payload.getObuLatitude())) {
            if (!DECIMAL_PATTERN_1_6.matcher(payload.getObuLatitude()).matches()) {
                throw new IllegalArgumentException("OBULatitude must be in format 9.999999 (max 1 integer digit, 6 decimal digits)");
            }
        }

        // 31. OBULongitude - Format: max 3 integer digits, 6 decimal digits (999.999999)
        if (!isEmpty(payload.getObuLongitude())) {
            if (!DECIMAL_PATTERN_3_6.matcher(payload.getObuLongitude()).matches()) {
                throw new IllegalArgumentException("OBULongitude must be in format 999.999999 (max 3 integer digits, 6 decimal digits)");
            }
        }

        // 32-36. DateTime fields - Max character validations
        // Note: These are handled by the deserializer and LocalDateTime type
        // The format validation is implicit in the deserialization process

        // 37. OperatorRemarks - Max 4000 characters (optional)
        // Note: Field not found in DTO, skipping

        // 38. CESRemarks - Max 1000 characters (optional)
        // Checking available remarks fields
        if (!isEmpty(payload.getRepRemarks())) {
            if (payload.getRepRemarks().length() > 1000) {
                throw new IllegalArgumentException("REPCCSRemarks must not exceed 1000 characters");
            }
        }

        if (!isEmpty(payload.getErp2ccsRemarks())) {
            if (payload.getErp2ccsRemarks().length() > 1000) {
                throw new IllegalArgumentException("ERP2CCSRemarks must not exceed 1000 characters");
            }
        }
    }

    // ==================== Offence Type Validation ====================

    private void validateOffenceType(RepWebHookPayloadDto payload) {
        Integer computerRuleCode = payload.getOffenceCode();
        if (computerRuleCode == null) {
            log.error("computerRuleCode is missing for transaction: {}", payload.getTransactionId());
            throw new IllegalArgumentException("Invalid Offence Rule Code");
        }

        // Extract vehicle category (first character only)
        String vehicleCategory = null;
        if (payload.getVehicleCategory() != null) {
            String[] parts = payload.getVehicleCategory().split("[–-]");
            vehicleCategory = parts[0].trim();
        }

        Optional<String> offenceTypeOptional = offenceRuleCodeRepository
                .findOffenceTypeByRuleCodeAndVehicleCategory(
                        computerRuleCode,
                        vehicleCategory,
                        LocalDateTime.now()
                );

        if (offenceTypeOptional.isEmpty()) {
            log.warn("No offence type found for ruleCode={}, vehicleCategory={} at {}",
                    computerRuleCode, vehicleCategory, LocalDateTime.now());
            throw new IllegalArgumentException("Invalid Offence Rule Code");
        }

        log.info("Offence type validated successfully: {} for ruleCode={}, vehicleCategory={}",
                offenceTypeOptional.get(), computerRuleCode, vehicleCategory);
    }

    // ==================== Helper Methods ====================

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}