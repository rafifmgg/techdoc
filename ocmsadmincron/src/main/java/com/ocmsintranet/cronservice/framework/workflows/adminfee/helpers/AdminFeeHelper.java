package com.ocmsintranet.cronservice.framework.workflows.adminfee.helpers;

import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.ParameterId;
import com.ocmsintranet.cronservice.crud.ocmsizdb.parameter.ParameterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class for Admin Fee processing workflow.
 *
 * This helper implements the OCMS 14 requirement to add administration fees to foreign vehicle notices
 * that remain unpaid for a pre-defined period (FOD parameter) after the offence date.
 *
 * Process Flow:
 * 1. Query Parameter table to get FOD (days) and AFO (admin fee amount) values
 * 2. Query unpaid foreign vehicle notices (PS-FOR) where offence_date + FOD days <= today
 * 3. Batch update notices with:
 *    - administration_fee = AFO amount
 *    - amount_payable = composition_amount + administration_fee
 * 4. Return updated notices for vHub notification
 *
 * Reference: OCMS 14 Functional Document v1.7, Section 3.7 - Foreign Vehicle Processing
 */
@Slf4j
@Component
public class AdminFeeHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ParameterRepository parameterRepository;
    private final OcmsValidOffenceNoticeRepository noticeRepository;

    public AdminFeeHelper(
            JdbcTemplate jdbcTemplate,
            ParameterRepository parameterRepository,
            OcmsValidOffenceNoticeRepository noticeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.parameterRepository = parameterRepository;
        this.noticeRepository = noticeRepository;
    }

    /**
     * Retrieves the FOD parameter value (number of days after offence date to apply admin fee).
     *
     * @return Number of days from offence date, or null if parameter not found
     */
    public Integer getFodDays() {
        try {
            Optional<Parameter> fodParam = parameterRepository.findById(
                    new ParameterId("FOD", "001"));

            if (fodParam.isPresent()) {
                String value = fodParam.get().getValue();
                int days = Integer.parseInt(value);
                log.info("FOD parameter value: {} days", days);
                return days;
            } else {
                log.error("FOD parameter not found in database");
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving FOD parameter: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves the AFO parameter value (admin fee amount to be added).
     *
     * @return Admin fee amount, or null if parameter not found
     */
    public BigDecimal getAfoAmount() {
        try {
            Optional<Parameter> afoParam = parameterRepository.findById(
                    new ParameterId("AFO", "001"));

            if (afoParam.isPresent()) {
                String value = afoParam.get().getValue();
                BigDecimal amount = new BigDecimal(value);
                log.info("AFO parameter value: ${}", amount);
                return amount;
            } else {
                log.error("AFO parameter not found in database");
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving AFO parameter: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Queries unpaid foreign vehicle notices that are eligible for admin fee application.
     *
     * Criteria:
     * - vehicle_registration_type = 'F' (Foreign)
     * - suspension_type = 'PS' (Permanent Suspension)
     * - epr_reason_of_suspension = 'FOR' (Foreign)
     * - amount_paid = 0 or NULL (unpaid)
     * - administration_fee = 0 or NULL (not yet applied)
     * - offence_date + FOD days <= today
     *
     * @param fodDays Number of days after offence date
     * @return List of notice records eligible for admin fee
     */
    public List<Map<String, Object>> queryEligibleNotices(Integer fodDays) {
        if (fodDays == null) {
            log.error("FOD days parameter is null");
            return new ArrayList<>();
        }

        try {
            // Calculate cutoff date: today - FOD days
            LocalDate cutoffDate = LocalDate.now().minusDays(fodDays);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String cutoffDateStr = cutoffDate.format(formatter);

            log.info("Querying notices with offence date on or before: {}", cutoffDateStr);

            String sql =
                "SELECT " +
                "von.notice_no AS noticeNo, " +
                "von.vehicle_no AS vehicleNo, " +
                "von.notice_date_and_time AS noticeDateAndTime, " +
                "von.composition_amount AS compositionAmount, " +
                "von.amount_payable AS amountPayable, " +
                "von.amount_paid AS amountPaid, " +
                "von.administration_fee AS administrationFee, " +
                "von.vehicle_registration_type AS vehicleRegistrationType, " +
                "von.suspension_type AS suspensionType, " +
                "von.epr_reason_of_suspension AS eprReasonOfSuspension " +
                "FROM ocmsizmgr.ocms_valid_offence_notice von " +
                "WHERE von.vehicle_registration_type = 'F' " +
                "AND von.suspension_type = 'PS' " +
                "AND von.epr_reason_of_suspension = 'FOR' " +
                "AND (von.amount_paid = 0 OR von.amount_paid IS NULL) " +
                "AND (von.administration_fee = 0 OR von.administration_fee IS NULL) " +
                "AND CAST(von.notice_date_and_time AS date) <= '" + cutoffDateStr + "' " +
                "ORDER BY von.notice_no";

            // Execute query using JdbcTemplate
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            log.info("Found {} notices eligible for admin fee application", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error querying eligible notices: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Batch updates notices with administration fee and recalculates amount_payable.
     *
     * Updates:
     * - administration_fee = AFO amount
     * - amount_payable = composition_amount + administration_fee
     *
     * Uses batch processing for efficiency when dealing with multiple notices.
     *
     * @param notices List of notices to update
     * @param afoAmount Admin fee amount to apply
     * @return Number of notices successfully updated
     */
    @Transactional
    public int batchUpdateAdminFee(List<Map<String, Object>> notices, BigDecimal afoAmount) {
        if (notices == null || notices.isEmpty()) {
            log.info("No notices to update");
            return 0;
        }

        if (afoAmount == null) {
            log.error("AFO amount is null");
            return 0;
        }

        int updateCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            log.info("Starting batch update of {} notices with admin fee ${}", notices.size(), afoAmount);

            for (Map<String, Object> notice : notices) {
                try {
                    String noticeNo = (String) notice.get("noticeNo");

                    // Retrieve the entity
                    Optional<OcmsValidOffenceNotice> optNotice = noticeRepository.findById(noticeNo);

                    if (!optNotice.isPresent()) {
                        String errorMsg = "Notice not found: " + noticeNo;
                        log.warn(errorMsg);
                        errors.add(errorMsg);
                        continue;
                    }

                    OcmsValidOffenceNotice entity = optNotice.get();

                    // Get composition amount
                    BigDecimal compositionAmount = entity.getCompositionAmount();
                    if (compositionAmount == null) {
                        String errorMsg = "Composition amount is null for notice: " + noticeNo;
                        log.warn(errorMsg);
                        errors.add(errorMsg);
                        continue;
                    }

                    // Set administration fee
                    entity.setAdministrationFee(afoAmount);

                    // Recalculate amount payable = composition amount + admin fee
                    BigDecimal newAmountPayable = compositionAmount.add(afoAmount);
                    entity.setAmountPayable(newAmountPayable);

                    // Save the entity
                    noticeRepository.save(entity);

                    log.info("Updated notice {}: admin_fee=${}, new amount_payable=${}",
                            noticeNo, afoAmount, newAmountPayable);

                    updateCount++;

                } catch (Exception e) {
                    String errorMsg = "Error updating notice " + notice.get("noticeNo") + ": " + e.getMessage();
                    log.error(errorMsg, e);
                    errors.add(errorMsg);
                }
            }

            log.info("Batch update completed: {} of {} notices updated successfully",
                    updateCount, notices.size());

            if (!errors.isEmpty()) {
                log.warn("Encountered {} errors during batch update", errors.size());
            }

            return updateCount;

        } catch (Exception e) {
            log.error("Error in batch update process: {}", e.getMessage(), e);
            return updateCount;
        }
    }

    /**
     * Prepares notice data for vHub notification.
     * Converts the updated notices into the format required by vHub API.
     *
     * @param notices List of updated notices
     * @return List of notices formatted for vHub
     */
    public List<Map<String, Object>> prepareVhubNotification(List<Map<String, Object>> notices) {
        if (notices == null || notices.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> vhubNotices = new ArrayList<>();

        try {
            for (Map<String, Object> notice : notices) {
                String noticeNo = (String) notice.get("noticeNo");

                // Retrieve fresh data after update
                Optional<OcmsValidOffenceNotice> optNotice = noticeRepository.findById(noticeNo);

                if (optNotice.isPresent()) {
                    OcmsValidOffenceNotice entity = optNotice.get();

                    Map<String, Object> vhubNotice = new HashMap<>();
                    vhubNotice.put("noticeNo", entity.getNoticeNo());
                    vhubNotice.put("vehicleNo", entity.getVehicleNo());
                    vhubNotice.put("compositionAmount", entity.getCompositionAmount());
                    vhubNotice.put("administrationFee", entity.getAdministrationFee());
                    vhubNotice.put("amountPayable", entity.getAmountPayable());

                    vhubNotices.add(vhubNotice);
                }
            }

            log.info("Prepared {} notices for vHub notification", vhubNotices.size());

        } catch (Exception e) {
            log.error("Error preparing vHub notification: {}", e.getMessage(), e);
        }

        return vhubNotices;
    }

    /**
     * Sends error notification email if any errors occur during admin fee processing.
     *
     * @param errors List of error messages
     */
    public void sendErrorNotification(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        try {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Admin Fee Processing Errors:\n\n");

            for (int i = 0; i < errors.size(); i++) {
                errorMsg.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }

            log.error("Admin Fee Processing Errors:\n{}", errorMsg.toString());

            // TODO: Integrate with email service to send error notification
            // For now, just log the errors

        } catch (Exception e) {
            log.error("Error sending error notification: {}", e.getMessage(), e);
        }
    }
}
