package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Helper class for Advisory Notice (AN) Qualification logic
 * Phase 2 - OCMS 10 Implementation
 */
@Component
@Slf4j
public class AdvisoryNoticeHelper {

    @Autowired
    private OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    /**
     * Result class for AN qualification check
     */
    public static class AdvisoryNoticeResult {
        private boolean qualified;
        private String reasonNotQualified;
        private Map<String, Object> details;

        public AdvisoryNoticeResult(boolean qualified, String reasonNotQualified) {
            this.qualified = qualified;
            this.reasonNotQualified = reasonNotQualified;
            this.details = new HashMap<>();
        }

        public boolean isQualified() {
            return qualified;
        }

        public String getReasonNotQualified() {
            return reasonNotQualified;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void addDetail(String key, Object value) {
            this.details.put(key, value);
        }
    }

    /**
     * Check if a notice qualifies for Advisory Notice
     *
     * Qualification Criteria:
     * 1. Offense Type = 'O' (Offender notices only)
     * 2. Vehicle Type in [S, D, V, I] (Local vehicles only)
     * 3. Same-day limit: Maximum 1 AN per vehicle per day
     * 4. Exemption rules:
     *    - Offense code 20412 with composition amount = $80
     *    - Offense code 11210 with vehicle category LB or HL
     * 5. Past offense check: Must have qualifying offense in past 24 months
     *
     * @param data Map containing the DTO and other processing data
     * @return AdvisoryNoticeResult with qualification status and details
     */
    public AdvisoryNoticeResult checkQualification(Map<String, Object> data) {
        log.info("Step 7: Checking OCMS AN Qualification (NEW logic)");

        try {
            OffenceNoticeDto dto = (OffenceNoticeDto) data.get("dto");
            if (dto == null) {
                log.error("No DTO found for AN qualification check");
                return new AdvisoryNoticeResult(false, "Missing DTO");
            }

            String vehicleNo = dto.getVehicleNo();
            String vehicleRegType = dto.getVehicleRegistrationType();
            String offenseType = (String) data.get("offenseType");
            Integer computerRuleCode = dto.getComputerRuleCode();
            LocalDateTime noticeDateAndTime = dto.getNoticeDateAndTime();

            // Gating Condition 1: Offense Type must be 'O'
            if (!"O".equals(offenseType)) {
                log.info("AN not applicable - offense type is {} (must be O)", offenseType);
                return new AdvisoryNoticeResult(false, "Offense type must be 'O'");
            }

            // Gating Condition 2: Vehicle Type must be S, D, V, or I
            if (vehicleRegType == null || !vehicleRegType.matches("[SDVI]")) {
                log.info("AN not applicable - vehicle type is {} (must be S/D/V/I)", vehicleRegType);
                return new AdvisoryNoticeResult(false, "Vehicle type must be S/D/V/I");
            }

            log.info("Gating conditions passed - offense type: {}, vehicle type: {}", offenseType, vehicleRegType);

            // Check 1: Same-day limit (1 AN per vehicle per day)
            if (!checkSameDayLimit(vehicleNo, noticeDateAndTime)) {
                log.info("AN not qualified - same-day limit exceeded (already has AN today)");
                return new AdvisoryNoticeResult(false, "Same-day limit exceeded");
            }

            // Check 2: Exemption rules
            if (isExemptFromAN(computerRuleCode, dto)) {
                log.info("AN not qualified - offense is exempt from AN (rule: {})", computerRuleCode);
                return new AdvisoryNoticeResult(false, "Offense exempt from AN");
            }

            // Check 3: Past offense check (24-month window)
            if (!hasPastQualifyingOffense(vehicleNo, noticeDateAndTime)) {
                log.info("AN not qualified - no qualifying offense in past 24 months");
                return new AdvisoryNoticeResult(false, "No qualifying offense in past 24 months");
            }

            // All checks passed - qualify for AN
            log.info("Notice qualifies for Advisory Notice");
            AdvisoryNoticeResult result = new AdvisoryNoticeResult(true, null);
            result.addDetail("vehicleNo", vehicleNo);
            result.addDetail("offenseType", offenseType);
            result.addDetail("vehicleType", vehicleRegType);
            result.addDetail("ruleCode", computerRuleCode);
            return result;

        } catch (Exception e) {
            log.error("Error checking AN qualification: {}", e.getMessage(), e);
            return new AdvisoryNoticeResult(false, "Error checking qualification: " + e.getMessage());
        }
    }

    /**
     * Check same-day limit: Maximum 1 AN per vehicle per day
     * @param vehicleNo Vehicle number
     * @param noticeDateAndTime Notice date and time
     * @return true if within limit (no AN today), false if limit exceeded
     */
    private boolean checkSameDayLimit(String vehicleNo, LocalDateTime noticeDateAndTime) {
        log.info("Checking same-day limit for vehicle: {}", vehicleNo);

        if (vehicleNo == null || noticeDateAndTime == null) {
            log.warn("Vehicle number or notice date is null");
            return false;
        }

        try {
            // Get start and end of the day
            LocalDate noticeDate = noticeDateAndTime.toLocalDate();
            LocalDateTime dayStart = noticeDate.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);

            // Query for existing AN notices for this vehicle on the same day
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("vehicleNo", new String[]{vehicleNo});
            queryParams.put("anFlag", new String[]{"Y"});

            FindAllResponse<OcmsValidOffenceNotice> response = ocmsValidOffenceNoticeService.getAll(queryParams);

            if (response != null && response.getData() != null) {
                for (OcmsValidOffenceNotice notice : response.getData()) {
                    LocalDateTime existingNoticeDate = notice.getNoticeDateAndTime();
                    if (existingNoticeDate != null &&
                        !existingNoticeDate.isBefore(dayStart) &&
                        !existingNoticeDate.isAfter(dayEnd)) {
                        log.info("Found existing AN on same day: {}", notice.getNoticeNo());
                        return false; // Limit exceeded
                    }
                }
            }

            log.info("No existing AN found on same day - limit check passed");
            return true;

        } catch (Exception e) {
            log.error("Error checking same-day limit: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if offense is exempt from AN based on exemption rules
     * Rule 1: Offense code 20412 with composition amount = $80
     * Rule 2: Offense code 11210 with vehicle category LB or HL
     *
     * @param computerRuleCode Computer rule code
     * @param dto OffenceNoticeDto
     * @return true if exempt, false if not exempt
     */
    private boolean isExemptFromAN(Integer computerRuleCode, OffenceNoticeDto dto) {
        log.info("Checking exemption rules for rule code: {}", computerRuleCode);

        if (computerRuleCode == null) {
            return false;
        }

        // Rule 1: Offense code 20412 with composition amount = $80
        if (computerRuleCode == 20412) {
            BigDecimal compositionAmount = dto.getCompositionAmount();
            if (compositionAmount != null && compositionAmount.compareTo(new BigDecimal("80")) == 0) {
                log.info("Exempt - Rule code 20412 with composition amount $80");
                return true;
            }
        }

        // Rule 2: Offense code 11210 with vehicle category LB or HL
        if (computerRuleCode == 11210) {
            String vehicleCategory = dto.getVehicleCategory();
            if ("LB".equals(vehicleCategory) || "HL".equals(vehicleCategory)) {
                log.info("Exempt - Rule code 11210 with vehicle category {}", vehicleCategory);
                return true;
            }
        }

        log.info("Not exempt from AN");
        return false;
    }

    /**
     * Check if vehicle has a qualifying offense in the past 24 months
     * @param vehicleNo Vehicle number
     * @param currentNoticeDate Current notice date and time
     * @return true if has qualifying past offense, false otherwise
     */
    private boolean hasPastQualifyingOffense(String vehicleNo, LocalDateTime currentNoticeDate) {
        log.info("Checking for past qualifying offenses in 24-month window for vehicle: {}", vehicleNo);

        if (vehicleNo == null || currentNoticeDate == null) {
            log.warn("Vehicle number or current notice date is null");
            return false;
        }

        try {
            // Calculate 24 months ago from current notice date
            LocalDateTime twentyFourMonthsAgo = currentNoticeDate.minusMonths(24);

            // Query for past notices for this vehicle
            Map<String, String[]> queryParams = new HashMap<>();
            queryParams.put("vehicleNo", new String[]{vehicleNo});

            FindAllResponse<OcmsValidOffenceNotice> response = ocmsValidOffenceNoticeService.getAll(queryParams);

            if (response != null && response.getData() != null) {
                for (OcmsValidOffenceNotice notice : response.getData()) {
                    LocalDateTime noticeDate = notice.getNoticeDateAndTime();

                    // Check if notice is within 24-month window
                    if (noticeDate != null &&
                        !noticeDate.isBefore(twentyFourMonthsAgo) &&
                        noticeDate.isBefore(currentNoticeDate)) {

                        // Check if this past notice is a qualifying offense
                        // (offenseType='O' and not suspended for non-qualifying reasons)
                        // For Phase 2, we'll keep it simple: any past offense qualifies
                        log.info("Found qualifying past offense: {} on {}", notice.getNoticeNo(), noticeDate);
                        return true;
                    }
                }
            }

            log.info("No qualifying past offense found in 24-month window");
            return false;

        } catch (Exception e) {
            log.error("Error checking past offenses: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update notice with AN flags after qualification check
     * Sets an_flag = 'Y' and payment_acceptance_allowed = 'N'
     *
     * @param noticeNumber Notice number to update
     */
    public void updateNoticeWithAnFlags(String noticeNumber) {
        log.info("Updating notice {} with AN flags", noticeNumber);

        try {
            Optional<OcmsValidOffenceNotice> optionalNotice = ocmsValidOffenceNoticeService.getById(noticeNumber);

            if (optionalNotice.isPresent()) {
                OcmsValidOffenceNotice notice = optionalNotice.get();

                // Set AN flags
                notice.setAnFlag("Y");
                notice.setPaymentAcceptanceAllowed(SystemConstant.PaymentAcceptance.NOT_ALLOWED);

                // Save updated notice
                ocmsValidOffenceNoticeService.save(notice);

                log.info("Successfully updated notice {} with an_flag='Y' and payment_acceptance_allowed='N'", noticeNumber);
            } else {
                log.error("Notice {} not found for AN flag update", noticeNumber);
            }

        } catch (Exception e) {
            log.error("Error updating notice {} with AN flags: {}", noticeNumber, e.getMessage(), e);
        }
    }
}
