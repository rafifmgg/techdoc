package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating amount payable based on processing stage changes
 * Based on OCMS CPS Spec Section 2.5.1.3
 *
 * Calculation Matrix:
 * | Previous Stage              | New Stage         | Formula                        |
 * |-----------------------------|-------------------|--------------------------------|
 * | ROV/ENA/RD1/RD2             | RR3               | composition + adminFee         |
 * | DN1/DN2                     | DR3               | composition + adminFee         |
 * | ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3 | CFC/CPC      | composition + surcharge        |
 * | CFC/CPC                     | RR3/DR3           | composition + adminFee         |
 * | CFC/CPC                     | ROV/RD1/RD2/DN1/DN2 | composition                  |
 * | RR3                         | ROV/RD1/RD2       | composition                    |
 * | DR3                         | DN1/DN2           | composition                    |
 * | ROV/ENA/RD1                 | RD1/RD2           | composition                    |
 * | DN1                         | DN2               | composition                    |
 * | RD1/RD2/RR3                 | DN1/DN2           | composition                    |
 * | DN1/DN2/DR3                 | ROV/RD1/RD2       | composition                    |
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmountPayableCalculationService {

    private final ParameterService parameterService;

    // Parameter IDs for retrieving admin fee and surcharge
    private static final String PARAM_ADMIN_FEE = "ADMIN_FEE";
    private static final String PARAM_SURCHARGE = "SURCHARGE";
    private static final String PARAM_CODE = "AMOUNT";

    // Processing stage groups
    private static final List<String> OWNER_EARLY_STAGES = Arrays.asList("ROV", "ENA", "RD1", "RD2");
    private static final List<String> DRIVER_EARLY_STAGES = Arrays.asList("DN1", "DN2");
    private static final List<String> OWNER_FINAL_STAGES = Arrays.asList("RR3");
    private static final List<String> DRIVER_FINAL_STAGES = Arrays.asList("DR3");
    private static final List<String> COURT_STAGES = Arrays.asList("CFC", "CPC");
    private static final List<String> ALL_REMINDER_STAGES = Arrays.asList("ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3");

    /**
     * Calculate amount payable for stage change
     * Based on OCMS CPS Spec Section 2.5.1.3
     *
     * @param prevStage Previous processing stage
     * @param newStage New processing stage
     * @param compositionAmount Base composition amount from VON table
     * @return BigDecimal calculated amount payable (2 decimal places)
     */
    public BigDecimal calculateAmountPayable(
            String prevStage,
            String newStage,
            BigDecimal compositionAmount) {

        log.debug("Calculating amount payable: prevStage={}, newStage={}, compositionAmount={}",
                prevStage, newStage, compositionAmount);

        // Validate inputs
        if (prevStage == null || prevStage.trim().isEmpty()) {
            log.warn("Previous stage is null or empty, using composition amount only");
            return formatAmount(compositionAmount);
        }

        if (newStage == null || newStage.trim().isEmpty()) {
            log.warn("New stage is null or empty, using composition amount only");
            return formatAmount(compositionAmount);
        }

        if (compositionAmount == null) {
            log.warn("Composition amount is null, defaulting to 0.00");
            compositionAmount = BigDecimal.ZERO;
        }

        // Normalize stage names
        String prev = prevStage.trim().toUpperCase();
        String next = newStage.trim().toUpperCase();

        // Apply calculation rules based on matrix
        BigDecimal result = applyCalculationRules(prev, next, compositionAmount);

        log.debug("Calculated amount payable: {}", result);
        return result;
    }

    /**
     * Apply calculation rules based on the matrix from Section 2.5.1.3
     */
    private BigDecimal applyCalculationRules(String prevStage, String newStage, BigDecimal compositionAmount) {

        // Rule 1: ROV/ENA/RD1/RD2 → RR3 = composition + adminFee
        if (OWNER_EARLY_STAGES.contains(prevStage) && "RR3".equals(newStage)) {
            return addAdminFee(compositionAmount);
        }

        // Rule 2: DN1/DN2 → DR3 = composition + adminFee
        if (DRIVER_EARLY_STAGES.contains(prevStage) && "DR3".equals(newStage)) {
            return addAdminFee(compositionAmount);
        }

        // Rule 3: ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3 → CFC/CPC = composition + surcharge
        if (ALL_REMINDER_STAGES.contains(prevStage) && COURT_STAGES.contains(newStage)) {
            return addSurcharge(compositionAmount);
        }

        // Rule 4: CFC/CPC → RR3/DR3 = composition + adminFee
        if (COURT_STAGES.contains(prevStage) &&
            (OWNER_FINAL_STAGES.contains(newStage) || DRIVER_FINAL_STAGES.contains(newStage))) {
            return addAdminFee(compositionAmount);
        }

        // Rule 5: CFC/CPC → ROV/RD1/RD2/DN1/DN2 = composition
        if (COURT_STAGES.contains(prevStage) &&
            (Arrays.asList("ROV", "RD1", "RD2", "DN1", "DN2").contains(newStage))) {
            return formatAmount(compositionAmount);
        }

        // Rule 6: RR3 → ROV/RD1/RD2 = composition
        if ("RR3".equals(prevStage) && Arrays.asList("ROV", "RD1", "RD2").contains(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Rule 7: DR3 → DN1/DN2 = composition
        if ("DR3".equals(prevStage) && DRIVER_EARLY_STAGES.contains(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Rule 8: ROV/ENA/RD1 → RD1/RD2 = composition
        if (Arrays.asList("ROV", "ENA", "RD1").contains(prevStage) &&
            Arrays.asList("RD1", "RD2").contains(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Rule 9: DN1 → DN2 = composition
        if ("DN1".equals(prevStage) && "DN2".equals(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Rule 10: RD1/RD2/RR3 → DN1/DN2 = composition
        if (Arrays.asList("RD1", "RD2", "RR3").contains(prevStage) &&
            DRIVER_EARLY_STAGES.contains(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Rule 11: DN1/DN2/DR3 → ROV/RD1/RD2 = composition
        if (Arrays.asList("DN1", "DN2", "DR3").contains(prevStage) &&
            Arrays.asList("ROV", "RD1", "RD2").contains(newStage)) {
            return formatAmount(compositionAmount);
        }

        // Default: If no rule matches, return composition amount
        log.warn("No specific calculation rule matched for prevStage={} to newStage={}, using composition amount",
                prevStage, newStage);
        return formatAmount(compositionAmount);
    }

    /**
     * Add admin fee to composition amount
     * Formula: compositionAmount + adminFee
     */
    private BigDecimal addAdminFee(BigDecimal compositionAmount) {
        BigDecimal adminFee = getAdminFee();
        if (adminFee == null) {
            log.error("Admin fee not found in parameters, using composition amount only");
            return formatAmount(compositionAmount);
        }
        BigDecimal result = compositionAmount.add(adminFee);
        log.debug("Added admin fee: {} + {} = {}", compositionAmount, adminFee, result);
        return formatAmount(result);
    }

    /**
     * Add surcharge to composition amount
     * Formula: compositionAmount + surcharge
     */
    private BigDecimal addSurcharge(BigDecimal compositionAmount) {
        BigDecimal surcharge = getSurcharge();
        if (surcharge == null) {
            log.error("Surcharge not found in parameters, using composition amount only");
            return formatAmount(compositionAmount);
        }
        BigDecimal result = compositionAmount.add(surcharge);
        log.debug("Added surcharge: {} + {} = {}", compositionAmount, surcharge, result);
        return formatAmount(result);
    }

    /**
     * Retrieve admin fee from ocms_parameter table
     * Parameter: parameter_id='ADMIN_FEE', code='AMOUNT'
     */
    private BigDecimal getAdminFee() {
        return getParameterAmount(PARAM_ADMIN_FEE, PARAM_CODE);
    }

    /**
     * Retrieve surcharge from ocms_parameter table
     * Parameter: parameter_id='SURCHARGE', code='AMOUNT'
     */
    private BigDecimal getSurcharge() {
        return getParameterAmount(PARAM_SURCHARGE, PARAM_CODE);
    }

    /**
     * Generic method to retrieve parameter value as BigDecimal
     */
    private BigDecimal getParameterAmount(String parameterId, String code) {
        try {
            ParameterId id = new ParameterId(parameterId, code);
            Optional<Parameter> paramOpt = parameterService.getById(id);

            if (paramOpt.isPresent()) {
                String value = paramOpt.get().getValue();
                if (value != null && !value.trim().isEmpty()) {
                    BigDecimal amount = new BigDecimal(value);
                    log.debug("Retrieved parameter {}:{} = {}", parameterId, code, amount);
                    return amount;
                }
            }

            log.warn("Parameter not found or empty: parameterId={}, code={}", parameterId, code);
            return null;

        } catch (NumberFormatException e) {
            log.error("Invalid number format in parameter {}:{}", parameterId, code, e);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving parameter {}:{}", parameterId, code, e);
            return null;
        }
    }

    /**
     * Format amount to 2 decimal places
     */
    private BigDecimal formatAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Check if stage change requires amount payable update
     * For Toppan cron job: manual changes should NOT update amount_payable
     *
     * @param source Source of change (OCMS, PLUS, SYSTEM, etc.)
     * @return true if amount payable should be updated, false otherwise
     */
    public boolean shouldUpdateAmountPayable(String source) {
        // Manual changes (OCMS, PLUS) already updated amount_payable during submission
        // Only automatic (SYSTEM) changes should update during Toppan cron
        boolean shouldUpdate = ChangeOfProcessingService.SOURCE_SYSTEM.equals(source);
        log.debug("shouldUpdateAmountPayable: source={}, result={}", source, shouldUpdate);
        return shouldUpdate;
    }
}
