package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ChangeOfProcessingRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ChangeOfProcessingResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.EligibilityOutcome;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.SearchChangeProcessingStageRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.SearchChangeProcessingStageResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ValidateChangeProcessingStageRequest;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ValidateChangeProcessingStageResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.Parameter;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.parameter.ParameterService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap.StageMapRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Change Processing Stage operations
 * Based on OCMS CPS Spec §4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeOfProcessingService {

    private final OcmsValidOffenceNoticeService vonService;
    private final OcmsOffenceNoticeOwnerDriverService onodService;
    private final OcmsChangeOfProcessingRepository cpsRepository;
    private final EligibilityService eligibilityService;
    private final StageMapRepository stageMapRepository;
    private final ParameterService parameterService;
    private final ReportGenerationService reportGenerationService;
    private final AmountPayableCalculationService amountPayableCalculationService;
    private final ChangeProcessingStageNotificationService notificationService;

    // Source constants for tracking origin of stage changes
    public static final String SOURCE_OCMS = "OCMS";
    public static final String SOURCE_PLUS = "PLUS";
    public static final String SOURCE_AVSS = "AVSS";
    public static final String SOURCE_SYSTEM = "SYSTEM";

    /**
     * Process batch change processing stage request
     * Based on OCMS CPS Spec §2, §4
     *
     * @param request Batch request
     * @param userId User ID performing the operation
     * @return Batch response with per-notice results
     */
    public ChangeOfProcessingResponse processBatch(ChangeOfProcessingRequest request, String userId) {
        log.info("Processing CPS batch request with {} items", request.getItems().size());

        String requestId = UUID.randomUUID().toString();
        List<ChangeOfProcessingResponse.NoticeResult> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;
        int warnings = 0;
        boolean hasExistingRecordWarning = false;

        // Process each notice
        for (ChangeOfProcessingRequest.ChangeOfProcessingItem item : request.getItems()) {
            try {
                ChangeOfProcessingResponse.NoticeResult result = processNotice(
                    item, userId, requestId
                );
                results.add(result);

                if ("UPDATED".equals(result.getOutcome())) {
                    succeeded++;
                } else if ("WARNING".equals(result.getOutcome())) {
                    warnings++;
                    hasExistingRecordWarning = true;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("Error processing notice {}: {}", item.getNoticeNo(), e.getMessage(), e);
                results.add(ChangeOfProcessingResponse.NoticeResult.builder()
                    .noticeNo(item.getNoticeNo())
                    .outcome("FAILED")
                    .code("OCMS.CPS.UNEXPECTED")
                    .message("Unexpected error: " + e.getMessage())
                    .build());
                failed++;
            }
        }

        // Determine overall status
        String status;
        if (failed == 0 && warnings == 0) {
            status = "SUCCESS";
        } else if (succeeded == 0 && warnings > 0) {
            status = "WARNING";
        } else if (succeeded == 0) {
            status = "FAILED";
        } else {
            status = "PARTIAL";
        }

        // Generate report for successful changes (§7)
        String reportUrl = null;
        if (succeeded > 0) {
            try {
                // Get all successful change records from today
                LocalDate today = LocalDate.now();
                List<OcmsChangeOfProcessing> successfulRecords = cpsRepository
                        .findByDateOfChange(today);

                if (!successfulRecords.isEmpty()) {
                    reportUrl = reportGenerationService.generateChangeStageReport(
                            successfulRecords, userId);
                    log.info("Report generated successfully: {}", reportUrl);
                }
            } catch (Exception e) {
                log.error("Failed to generate report, but batch processing succeeded", e);
                // Don't fail the entire operation if report generation fails
            }
        }

        // Send error notification email if there were failures (§9)
        if (failed > 0 || warnings > 0) {
            try {
                log.info("Sending error notification email for {} failed/warning notices", failed + warnings);
                LocalDate today = LocalDate.now();

                // Send email asynchronously (don't wait for completion)
                notificationService.sendErrorNotificationAsync(
                    today,
                    userId,
                    request.getItems().size(),
                    failed + warnings,
                    results
                ).whenComplete((sent, throwable) -> {
                    if (throwable != null) {
                        log.error("Error notification email failed: {}", throwable.getMessage());
                    } else if (sent) {
                        log.info("Error notification email sent successfully");
                    } else {
                        log.warn("Error notification email not sent (check EmailService logs)");
                    }
                });

            } catch (Exception e) {
                log.error("Failed to send error notification email: {}", e.getMessage(), e);
                // Don't fail the batch operation if email fails
            }
        }

        // Build response
        return ChangeOfProcessingResponse.builder()
            .status(status)
            .summary(ChangeOfProcessingResponse.BatchSummary.builder()
                .requested(request.getItems().size())
                .succeeded(succeeded)
                .failed(failed + warnings)  // Warnings count as failures in summary
                .build())
            .results(results)
            .reportUrl(reportUrl)
            .existingRecordWarning(hasExistingRecordWarning)  // Set warning flag per spec §2.5.1 Step 4
            .build();
    }

    /**
     * Validate notices for change processing stage eligibility
     * Based on OCMS CPS Spec §2.4.2
     *
     * This method validates notices WITHOUT making any database changes.
     * It checks eligibility rules and returns segregated lists of changeable
     * vs non-changeable notices.
     *
     * @param request Validation request
     * @return Validation response with changeable/non-changeable lists
     */
    public ValidateChangeProcessingStageResponse validateNotices(
            ValidateChangeProcessingStageRequest request) {

        log.info("Validating {} notices for stage change to {}",
                request.getNotices().size(), request.getNewProcessingStage());

        List<ValidateChangeProcessingStageResponse.ChangeableNotice> changeableNotices = new ArrayList<>();
        List<ValidateChangeProcessingStageResponse.NonChangeableNotice> nonChangeableNotices = new ArrayList<>();

        String newStage = request.getNewProcessingStage();

        // Validate each notice
        for (ValidateChangeProcessingStageRequest.NoticeToValidate notice : request.getNotices()) {
            String noticeNo = notice.getNoticeNo();

            try {
                // Check eligibility using existing service
                EligibilityOutcome eligibility = eligibilityService.checkEligibility(noticeNo, newStage);

                if (eligibility.isChangeable()) {
                    // Notice is eligible
                    // Get additional details from VON for response
                    Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
                    String currentStage = vonOpt.isPresent() ? vonOpt.get().getLastProcessingStage() : notice.getCurrentStage();

                    // Determine offender type if not provided
                    String offenderType = notice.getOffenderType();
                    if (offenderType == null && eligibility.getRole() != null) {
                        offenderType = eligibility.getRole();
                    }

                    changeableNotices.add(ValidateChangeProcessingStageResponse.ChangeableNotice.builder()
                        .noticeNo(noticeNo)
                        .currentStage(currentStage)
                        .offenderType(offenderType)
                        .entityType(notice.getEntityType())
                        .message("Eligible for stage change to " + newStage)
                        .build());

                    log.debug("Notice {} is CHANGEABLE to {}", noticeNo, newStage);

                } else {
                    // Notice is NOT eligible
                    // Get current stage if available
                    String currentStage = null;
                    Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
                    if (vonOpt.isPresent()) {
                        currentStage = vonOpt.get().getLastProcessingStage();
                    }

                    nonChangeableNotices.add(ValidateChangeProcessingStageResponse.NonChangeableNotice.builder()
                        .noticeNo(noticeNo)
                        .currentStage(currentStage)
                        .code(eligibility.getCode())
                        .message(eligibility.getMessage())
                        .build());

                    log.debug("Notice {} is NON-CHANGEABLE: {} - {}", noticeNo, eligibility.getCode(), eligibility.getMessage());
                }

            } catch (Exception e) {
                // Handle unexpected errors
                log.error("Error validating notice {}: {}", noticeNo, e.getMessage(), e);
                nonChangeableNotices.add(ValidateChangeProcessingStageResponse.NonChangeableNotice.builder()
                    .noticeNo(noticeNo)
                    .code("OCMS.CPS.VALIDATION_ERROR")
                    .message("Validation error: " + e.getMessage())
                    .build());
            }
        }

        // Build summary
        ValidateChangeProcessingStageResponse.ValidationSummary summary =
            ValidateChangeProcessingStageResponse.ValidationSummary.builder()
                .total(request.getNotices().size())
                .changeable(changeableNotices.size())
                .nonChangeable(nonChangeableNotices.size())
                .build();

        log.info("Validation completed: total={}, changeable={}, nonChangeable={}",
                summary.getTotal(), summary.getChangeable(), summary.getNonChangeable());

        return ValidateChangeProcessingStageResponse.builder()
            .changeableNotices(changeableNotices)
            .nonChangeableNotices(nonChangeableNotices)
            .summary(summary)
            .build();
    }

    /**
     * Search notices for change processing stage
     * Based on OCMS CPS Spec §2.3.1
     *
     * Search Flow:
     * 1. If searching by ID Number: Query ONOD first, then VON
     * 2. If searching by other criteria: Query VON first, then ONOD
     * 3. Check court stage and PS status using EligibilityService
     * 4. Segregate into eligible vs ineligible lists
     *
     * @param request Search request with criteria
     * @return Search response with segregated lists
     */
    public SearchChangeProcessingStageResponse searchNotices(SearchChangeProcessingStageRequest request) {
        log.info("Searching notices with criteria: noticeNo={}, idNo={}, vehicleNo={}, stage={}, date={}",
                request.getNoticeNo(), request.getIdNo(), request.getVehicleNo(),
                request.getLastProcessingStage(), request.getDateOfCurrentProcessingStage());

        // Validate at least one criterion is provided
        if (!request.hasValidCriteria()) {
            log.warn("No search criteria provided");
            return SearchChangeProcessingStageResponse.builder()
                .eligibleNotices(new ArrayList<>())
                .ineligibleNotices(new ArrayList<>())
                .summary(SearchChangeProcessingStageResponse.SearchSummary.builder()
                    .total(0)
                    .eligible(0)
                    .ineligible(0)
                    .build())
                .build();
        }

        List<OcmsValidOffenceNotice> vonList;

        // Determine search path based on criteria (Spec §2.3.1 Step 4)
        if (request.isSearchingByIdNo()) {
            // Path 1: Search by ID Number (ONOD → VON)
            log.debug("Search path: ID Number → ONOD → VON");
            vonList = searchByIdNumber(request.getIdNo());
        } else {
            // Path 2: Search by other criteria (VON → ONOD)
            log.debug("Search path: VON criteria");
            vonList = searchByVonCriteria(request);
        }

        log.info("Found {} VON records matching search criteria", vonList.size());

        // Build segregated response lists
        List<SearchChangeProcessingStageResponse.EligibleNotice> eligibleNotices = new ArrayList<>();
        List<SearchChangeProcessingStageResponse.IneligibleNotice> ineligibleNotices = new ArrayList<>();

        // Process each VON record
        for (OcmsValidOffenceNotice von : vonList) {
            String noticeNo = von.getNoticeNo();

            try {
                // Get ONOD record for offender details
                List<OcmsOffenceNoticeOwnerDriver> onodList = onodService.findByNoticeNo(noticeNo);
                OcmsOffenceNoticeOwnerDriver onod = onodList.isEmpty() ? null : onodList.get(0);

                // Check court stage (Spec §2.3.1 Step 6-8)
                boolean isCourtStage = eligibilityService.isCourtStage(von.getLastProcessingStage());

                // Check PS status (Spec §2.3.1 Step 6-8)
                boolean isPsActive = "PS".equals(von.getSuspensionType());

                if (isCourtStage || isPsActive) {
                    // Ineligible notice
                    String reasonCode;
                    String reasonMessage;

                    if (isPsActive) {
                        reasonCode = "OCMS.CPS.SEARCH.PS_ACTIVE";
                        reasonMessage = "Permanent Suspension is active";
                    } else {
                        reasonCode = "OCMS.CPS.SEARCH.COURT_STAGE";
                        reasonMessage = "Notice is in court stage";
                    }

                    ineligibleNotices.add(SearchChangeProcessingStageResponse.IneligibleNotice.builder()
                        .noticeNo(noticeNo)
                        .offenceType(von.getOffenceType())
                        .offenceDateTime(von.getNoticeDateAndTime())
                        .offenderName(onod != null ? onod.getName() : null)
                        .offenderId(onod != null ? onod.getIdNo() : null)
                        .vehicleNo(von.getVehicleNo())
                        .currentProcessingStage(von.getLastProcessingStage())
                        .currentProcessingStageDate(von.getLastProcessingDate())
                        .reasonCode(reasonCode)
                        .reasonMessage(reasonMessage)
                        .build());

                    log.debug("Notice {} is INELIGIBLE: {}", noticeNo, reasonMessage);

                } else {
                    // Eligible notice
                    eligibleNotices.add(SearchChangeProcessingStageResponse.EligibleNotice.builder()
                        .noticeNo(noticeNo)
                        .offenceType(von.getOffenceType())
                        .offenceDateTime(von.getNoticeDateAndTime())
                        .offenderName(onod != null ? onod.getName() : null)
                        .offenderId(onod != null ? onod.getIdNo() : null)
                        .vehicleNo(von.getVehicleNo())
                        .currentProcessingStage(von.getLastProcessingStage())
                        .currentProcessingStageDate(von.getLastProcessingDate())
                        .suspensionType(von.getSuspensionType())
                        .suspensionStatus(von.getSuspensionStatus())
                        .ownerDriverIndicator(onod != null ? onod.getId().getOwnerDriverIndicator() : null)
                        .entityType(onod != null ? onod.getEntityType() : null)
                        .build());

                    log.debug("Notice {} is ELIGIBLE", noticeNo);
                }

            } catch (Exception e) {
                log.error("Error processing notice {} during search: {}", noticeNo, e.getMessage(), e);
                // Add to ineligible with error
                ineligibleNotices.add(SearchChangeProcessingStageResponse.IneligibleNotice.builder()
                    .noticeNo(noticeNo)
                    .offenceType(von.getOffenceType())
                    .offenceDateTime(von.getNoticeDateAndTime())
                    .vehicleNo(von.getVehicleNo())
                    .currentProcessingStage(von.getLastProcessingStage())
                    .currentProcessingStageDate(von.getLastProcessingDate())
                    .reasonCode("OCMS.CPS.SEARCH.ERROR")
                    .reasonMessage("Search error: " + e.getMessage())
                    .build());
            }
        }

        // Build summary
        SearchChangeProcessingStageResponse.SearchSummary summary =
            SearchChangeProcessingStageResponse.SearchSummary.builder()
                .total(vonList.size())
                .eligible(eligibleNotices.size())
                .ineligible(ineligibleNotices.size())
                .build();

        log.info("Search completed: total={}, eligible={}, ineligible={}",
                summary.getTotal(), summary.getEligible(), summary.getIneligible());

        return SearchChangeProcessingStageResponse.builder()
            .eligibleNotices(eligibleNotices)
            .ineligibleNotices(ineligibleNotices)
            .summary(summary)
            .build();
    }

    /**
     * Search by ID Number (ONOD → VON path)
     * Spec §2.3.1 Step 4: Query ONOD first, then get VON records
     *
     * @param idNo ID number to search
     * @return List of VON records
     */
    private List<OcmsValidOffenceNotice> searchByIdNumber(String idNo) {
        log.debug("Searching ONOD by ID number: {}", idNo);

        // Step 1: Query ONOD table
        List<OcmsOffenceNoticeOwnerDriver> onodList = onodService.findByIdNo(idNo);
        log.debug("Found {} ONOD records for ID number: {}", onodList.size(), idNo);

        // Step 2: Extract notice numbers
        List<String> noticeNumbers = new ArrayList<>();
        for (OcmsOffenceNoticeOwnerDriver onod : onodList) {
            noticeNumbers.add(onod.getId().getNoticeNo());
        }

        // Step 3: Query VON for these notice numbers
        List<OcmsValidOffenceNotice> vonList = new ArrayList<>();
        for (String noticeNo : noticeNumbers) {
            Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
            if (vonOpt.isPresent()) {
                vonList.add(vonOpt.get());
            }
        }

        log.debug("Retrieved {} VON records from {} notice numbers", vonList.size(), noticeNumbers.size());
        return vonList;
    }

    /**
     * Search by VON criteria (direct VON query path)
     * Spec §2.3.1 Step 4: Query VON directly
     *
     * @param request Search request
     * @return List of VON records
     */
    private List<OcmsValidOffenceNotice> searchByVonCriteria(SearchChangeProcessingStageRequest request) {
        List<OcmsValidOffenceNotice> vonList = new ArrayList<>();

        // Search by notice number (highest priority)
        if (request.getNoticeNo() != null && !request.getNoticeNo().trim().isEmpty()) {
            log.debug("Searching by notice number: {}", request.getNoticeNo());
            Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(request.getNoticeNo());
            if (vonOpt.isPresent()) {
                vonList.add(vonOpt.get());
            }
            return vonList;
        }

        // Search by vehicle number
        if (request.getVehicleNo() != null && !request.getVehicleNo().trim().isEmpty()) {
            log.debug("Searching by vehicle number: {}", request.getVehicleNo());
            vonList.addAll(vonService.findByVehicleNo(request.getVehicleNo()));
        }

        // Search by stage and date
        if (request.getLastProcessingStage() != null && !request.getLastProcessingStage().trim().isEmpty()) {
            if (request.getDateOfCurrentProcessingStage() != null) {
                log.debug("Searching by stage and date: {} on {}",
                        request.getLastProcessingStage(), request.getDateOfCurrentProcessingStage());
                vonList.addAll(vonService.findByLastProcessingStageAndDate(
                        request.getLastProcessingStage(),
                        request.getDateOfCurrentProcessingStage()));
            } else {
                log.debug("Searching by stage: {}", request.getLastProcessingStage());
                vonList.addAll(vonService.findByLastProcessingStage(request.getLastProcessingStage()));
            }
        }

        // Remove duplicates (if any)
        List<OcmsValidOffenceNotice> uniqueVonList = new ArrayList<>();
        java.util.Set<String> seenNotices = new java.util.HashSet<>();
        for (OcmsValidOffenceNotice von : vonList) {
            if (!seenNotices.contains(von.getNoticeNo())) {
                uniqueVonList.add(von);
                seenNotices.add(von.getNoticeNo());
            }
        }

        log.debug("Found {} unique VON records (from {} total)", uniqueVonList.size(), vonList.size());
        return uniqueVonList;
    }

    /**
     * Process a single notice change
     * Based on OCMS CPS Spec §4.1
     *
     * @param item Request item
     * @param userId User ID
     * @param requestId Batch request ID
     * @return Notice result
     */
    @Transactional
    protected ChangeOfProcessingResponse.NoticeResult processNotice(
            ChangeOfProcessingRequest.ChangeOfProcessingItem item,
            String userId,
            String requestId) {

        String noticeNo = item.getNoticeNo();
        log.debug("Processing notice: {}", noticeNo);

        // Step 1: Check idempotency (§6) and confirmation flow (§2.5.1 Step 4)
        LocalDate today = LocalDate.now();
        boolean duplicateExists = item.getNewStage() != null &&
            cpsRepository.existsByNoticeNoAndNewStageAndDate(noticeNo, item.getNewStage(), today);

        if (duplicateExists) {
            // Check if user has confirmed
            if (Boolean.TRUE.equals(item.getIsConfirmation())) {
                // User confirmed - proceed with update
                log.info("Duplicate record confirmed by user for notice {}, proceeding with update", noticeNo);
            } else {
                // No confirmation - return warning
                log.warn("Duplicate change request for notice {} on same day, requires confirmation", noticeNo);
                return ChangeOfProcessingResponse.NoticeResult.builder()
                    .noticeNo(noticeNo)
                    .outcome("WARNING")
                    .code(EligibilityService.EXISTING_CHANGE_TODAY)
                    .message("Notice No. " + noticeNo + " has existing stage change update. Please confirm to proceed.")
                    .build();
            }
        }

        // Step 2: Check eligibility (§3)
        EligibilityOutcome eligibility = eligibilityService.checkEligibility(noticeNo, item.getNewStage());
        if (!eligibility.isChangeable()) {
            log.warn("Notice {} not eligible: {}", noticeNo, eligibility.getMessage());
            return ChangeOfProcessingResponse.NoticeResult.builder()
                .noticeNo(noticeNo)
                .outcome("FAILED")
                .code(eligibility.getCode())
                .message(eligibility.getMessage())
                .build();
        }

        // Step 3: Get VON for update
        Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
        if (!vonOpt.isPresent()) {
            return ChangeOfProcessingResponse.NoticeResult.builder()
                .noticeNo(noticeNo)
                .outcome("FAILED")
                .code(EligibilityService.NOT_FOUND)
                .message("VON not found")
                .build();
        }

        OcmsValidOffenceNotice von = vonOpt.get();
        String previousStage = von.getLastProcessingStage();
        String newStage = eligibility.getChosenStage();

        // Step 4: Calculate and update amount payable (§2.5.1.3)
        java.math.BigDecimal compositionAmount = von.getAmountPayable();
        java.math.BigDecimal newAmountPayable = amountPayableCalculationService.calculateAmountPayable(
                previousStage, newStage, compositionAmount);
        von.setAmountPayable(newAmountPayable);
        von.setPaymentAcceptanceAllowed("Y"); // Set acceptance flag

        log.debug("Updated amount payable for notice {}: {} -> {}", noticeNo, compositionAmount, newAmountPayable);

        // Step 5: Update VON stages (§4.3)
        updateVonStage(von, newStage);
        vonService.save(von);

        // Step 6: Update ONOD DH/MHA check flag if provided (§4.4)
        if (item.getDhMhaCheck() != null) {
            updateOnodDhMhaCheck(noticeNo, item.getDhMhaCheck());
        }

        // Step 7: Insert CPS audit record (§4.5)
        OcmsChangeOfProcessing cpsRecord = OcmsChangeOfProcessing.builder()
            .noticeNo(noticeNo)
            .dateOfChange(today)
            .lastProcessingStage(previousStage)
            .newProcessingStage(newStage)
            .reasonOfChange(item.getReason())
            .authorisedOfficer(userId)
            .source(SOURCE_OCMS)
            .remarks(item.getRemark())
            .creDate(LocalDateTime.now())
            .creUserId(userId)
            .build();

        cpsRepository.save(cpsRecord);

        log.info("Successfully updated notice {} from {} to {}", noticeNo, previousStage, newStage);

        return ChangeOfProcessingResponse.NoticeResult.builder()
            .noticeNo(noticeNo)
            .outcome("UPDATED")
            .previousStage(previousStage)
            .newStage(newStage)
            .message("Stage changed successfully")
            .build();
    }

    /**
     * Update VON processing stages
     * Based on OCMS CPS Spec §2.1 Lines 469-479 (Universal pattern for all sources)
     *
     * Pattern (applies to OCMS, PLUS, and Toppan):
     * - prev_processing_stage = old value last_processing_stage
     * - prev_processing_date = old value last_processing_date
     * - last_processing_stage = old value next_processing_stage
     * - last_processing_date = current date
     * - next_processing_stage = computed from NEXT_STAGE_* parameter
     * - next_processing_date = current date + STAGEDAYS parameter
     *
     * @param von VON entity to update
     * @param newStage The new processing stage being applied
     */
    private void updateVonStage(OcmsValidOffenceNotice von, String newStage) {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Save OLD values before overwriting (spec §2.1 lines 469-475)
        String oldLastStage = von.getLastProcessingStage();
        LocalDateTime oldLastDate = von.getLastProcessingDate();
        String oldNextStage = von.getNextProcessingStage();

        log.debug("VON stage update - OLD values: prev={}, last={}, next={}",
                von.getPrevProcessingStage(), oldLastStage, oldNextStage);

        // Step 2: Set prev = old last (spec §2.1 line 469)
        von.setPrevProcessingStage(oldLastStage);
        von.setPrevProcessingDate(oldLastDate);

        // Step 3: Set last = old next (spec §2.1 line 471)
        // For manual change, old_next might be null/empty, so use newStage as fallback
        String newLastStage = (oldNextStage != null && !oldNextStage.isEmpty()) ? oldNextStage : newStage;
        von.setLastProcessingStage(newLastStage);
        von.setLastProcessingDate(now);

        // Step 4: Compute and set next stage from parameters (spec §2.1 line 473)
        // Query NEXT_STAGE_{newStage} parameter to get the next stage
        String parameterId = "NEXT_STAGE_" + newStage;
        String computedNextStage = getParameterValue(parameterId, "NEXT_STAGE");

        // If parameter not found, use newStage as fallback (same stage)
        String newNextStage = (computedNextStage != null && !computedNextStage.isEmpty())
                ? computedNextStage
                : newStage;
        von.setNextProcessingStage(newNextStage);

        // Step 5: Compute next_processing_date = now + stage_days (spec §2.1 line 479)
        String stageDaysStr = getParameterValue("STAGEDAYS", newStage);
        int stageDays = 14; // Default to 14 days if parameter not found
        if (stageDaysStr != null && !stageDaysStr.trim().isEmpty()) {
            try {
                stageDays = Integer.parseInt(stageDaysStr);
                if (stageDays < 0) {
                    log.warn("Invalid STAGEDAYS value {} for stage {}, using default 14", stageDaysStr, newStage);
                    stageDays = 14;
                }
            } catch (NumberFormatException e) {
                log.warn("Cannot parse STAGEDAYS value '{}' for stage {}, using default 14", stageDaysStr, newStage);
            }
        } else {
            log.warn("STAGEDAYS parameter not found for stage {}, using default 14 days", newStage);
        }

        LocalDateTime newNextDate = now.plusDays(stageDays);
        von.setNextProcessingDate(newNextDate);

        log.debug("VON stage update - NEW values: prev={}, last={}, next={}, next_date={}",
                von.getPrevProcessingStage(), von.getLastProcessingStage(),
                newNextStage, newNextDate);
    }

    /**
     * Update ONOD dh_mha_check_allow field
     * Based on OCMS CPS Spec §4.4
     *
     * When dhMhaCheck is TRUE in request, set dh_mha_check_allow = 'Y' (allow check)
     * When dhMhaCheck is FALSE in request, set dh_mha_check_allow = 'N' (skip check)
     *
     * @param noticeNo Notice number
     * @param dhMhaCheck Boolean flag from request (true = allow check, false = skip check)
     */
    private void updateOnodDhMhaCheck(String noticeNo, Boolean dhMhaCheck) {
        try {
            List<OcmsOffenceNoticeOwnerDriver> onods = onodService.findByNoticeNo(noticeNo);
            if (onods != null && !onods.isEmpty()) {
                String checkValue = Boolean.TRUE.equals(dhMhaCheck) ? "Y" : "N";

                for (OcmsOffenceNoticeOwnerDriver onod : onods) {
                    onod.setDhMhaCheckAllow(checkValue);
                    onodService.save(onod);
                }

                log.debug("Updated dh_mha_check_allow to '{}' for notice {}", checkValue, noticeNo);
            } else {
                log.warn("No ONOD records found for notice {}, skipping DH/MHA check update", noticeNo);
            }
        } catch (Exception e) {
            log.error("Failed to update ONOD dh_mha_check_allow for notice {}: {}", noticeNo, e.getMessage(), e);
            // Non-critical - don't throw, just log
        }
    }

    /**
     * Process PLUS change stage request (batch processing) - New Flow
     * Based on diagram: OCMS15-Plus Apply Change Stage - New Flow
     * All notices in the request will have the same stage change applied
     * Throws exception if any notice fails to process
     *
     * @param request PLUS request
     * @throws PlusChangeStageException if validation or processing fails
     */
    public void processPlusChangeStage(
            com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.PlusChangeStageRequest request)
            throws PlusChangeStageException {

        log.info("Processing PLUS change stage request with {} notices", request.getNoticeNo().size());

        // Get common values from request
        String nextStageName = request.getNextStageName();
        String lastStageName = request.getLastStageName();
        String offenceType = request.getOffenceType();
        String source = request.getSource();

        // STEP 1: Validate source = PLUS (005) AND nextStageName = CFC (NOT ALLOWED)
        log.debug("STEP 1: Validating PLUS source restriction - source={}, nextStageName={}", source, nextStageName);
        if (SystemConstant.Subsystem.PLUS_CODE.equals(source) && "CFC".equals(nextStageName)) {
            String errorMsg = String.format("FAILED: Stage change to CFC is not allowed from PLUS source (source=%s). This operation is restricted.",
                SystemConstant.Subsystem.PLUS_CODE);
            log.error(errorMsg);
            throw new PlusChangeStageException("OCMS-4004", errorMsg);
        }
        log.debug("STEP 1 PASSED: PLUS source validation passed");

        // STEP 2: Validate offenceType = U AND nextStageName IN (DN1, DN2, DR3, CPC)
        log.debug("STEP 2: Checking skip condition - offenceType={}, nextStageName={}", offenceType, nextStageName);
        if ("U".equals(offenceType) &&
            java.util.Arrays.asList("DN1", "DN2", "DR3", "CPC").contains(nextStageName)) {
            log.info("STEP 2 SKIPPED: Processing skipped for offenceType=U AND nextStageName IN (DN1, DN2, DR3, CPC). Request completed successfully without changes.");
            return; // Skip processing, respond success immediately
        }
        log.debug("STEP 2 PASSED: Condition not met, continue processing");

        // STEP 3: Query stagemap validation
        log.debug("STEP 3: Validating stage transition: {} -> {}", lastStageName, nextStageName);
        long stageMapCount = stageMapRepository.countByLastStageAndNextStageLike(lastStageName, nextStageName);
        if (stageMapCount == 0) {
            String errorMsg = String.format("STEP 3 FAILED: Stage transition not allowed: %s -> %s. No matching record found in ocms_stage_map table.",
                lastStageName, nextStageName);
            log.error(errorMsg);
            throw new PlusChangeStageException("OCMS-4000", errorMsg);
        }
        log.debug("STEP 3 SUCCESS: Found {} matching stage transition(s)", stageMapCount);

        // STEP 4-5: Parameter validation (NEXT_STAGE and STAGEDAYS)
        // NOTE: These parameters will be queried by updateVonStage() method
        // But we validate they exist here to fail fast and provide clear error messages
        log.debug("STEP 4: Validating NEXT_STAGE_{} parameter exists", nextStageName);
        String parameterId = "NEXT_STAGE_" + nextStageName;
        String newNextStageName = getParameterValue(parameterId, "NEXT_STAGE");
        if (newNextStageName == null || newNextStageName.trim().isEmpty()) {
            log.warn("NEXT_STAGE_{} parameter not found, will use '{}' as fallback in updateVonStage()",
                    nextStageName, nextStageName);
        } else {
            log.debug("STEP 4 SUCCESS: New next stage = {}", newNextStageName);
        }

        log.debug("STEP 5: Validating STAGEDAYS parameter for stage: {}", nextStageName);
        String stageDaysStr = getParameterValue("STAGEDAYS", nextStageName);
        if (stageDaysStr == null || stageDaysStr.trim().isEmpty()) {
            log.warn("STAGEDAYS parameter not found for {}, will use default 14 days in updateVonStage()", nextStageName);
        } else {
            try {
                int stageDays = Integer.parseInt(stageDaysStr);
                if (stageDays < 0) {
                    log.warn("Invalid STAGEDAYS value: {}, will use default 14 days", stageDaysStr);
                } else {
                    log.debug("STEP 5 SUCCESS: Stage days = {}", stageDays);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid STAGEDAYS format: {}, will use default 14 days", stageDaysStr);
            }
        }

        // Validate for duplicate notice numbers in request
        java.util.Set<String> seenNoticeNos = new java.util.HashSet<>();
        List<String> errors = new ArrayList<>();

        // Process each notice
        for (String noticeNo : request.getNoticeNo()) {
            // Check for duplicate in request
            if (seenNoticeNos.contains(noticeNo)) {
                String error = String.format("Duplicate noticeNo in request: %s", noticeNo);
                log.warn(error);
                errors.add(error);
                continue;
            }
            seenNoticeNos.add(noticeNo);

            try {
                // Process single notice
                // updateVonStage() will compute next_stage and next_date automatically
                processPlusNoticeWithStageNewFlow(
                    noticeNo, lastStageName, nextStageName,
                    offenceType, source
                );

            } catch (PlusChangeStageException e) {
                // Expected validation/business errors
                String error = String.format("Notice %s failed: %s - %s", noticeNo, e.getErrorCode(), e.getMessage());
                log.warn(error);
                errors.add(error);
            } catch (Exception e) {
                // Unexpected errors
                String error = String.format("Notice %s failed with unexpected error: %s", noticeNo, e.getMessage());
                log.error(error, e);
                errors.add(error);
            }
        }

        // If there are any errors, throw exception with all error messages
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            throw new PlusChangeStageException("PROCESSING_FAILED", errorMessage);
        }

        log.info("Successfully processed all {} notices", request.getNoticeNo().size());

        // STEP 8: Generate Excel report for successful PLUS changes (spec §3.2 Step 5.3c)
        // Similar to OCMS flow, generate report after all notices processed successfully
        int successCount = seenNoticeNos.size() - errors.size();
        if (successCount > 0) {
            try {
                log.debug("STEP 8: Generating Excel report for {} successful PLUS changes", successCount);

                // Get all successful PLUS change records from today
                LocalDate today = LocalDate.now();
                List<OcmsChangeOfProcessing> successfulRecords = cpsRepository
                        .findByDateOfChange(today);

                // Filter to only PLUS source records (optional - report can include all today's changes)
                List<OcmsChangeOfProcessing> plusRecords = new ArrayList<>();
                for (OcmsChangeOfProcessing record : successfulRecords) {
                    if (SOURCE_PLUS.equals(record.getSource())) {
                        plusRecords.add(record);
                    }
                }

                if (!plusRecords.isEmpty()) {
                    String reportUrl = reportGenerationService.generateChangeStageReport(
                            plusRecords, "PLUS_SYSTEM");
                    log.info("STEP 8 SUCCESS: PLUS report generated successfully: {}", reportUrl);
                } else {
                    log.warn("STEP 8: No PLUS records found to generate report");
                }
            } catch (Exception e) {
                // Don't fail the entire operation if report generation fails
                // Report generation is non-critical - PLUS can still function without it
                log.error("STEP 8 FAILED: Failed to generate PLUS report, but processing succeeded: {}", e.getMessage());
            }
        }
    }

    /**
     * Get parameter value from ocms_parameter table
     * @param parameterId parameter_id column
     * @param code code column
     * @return value or null if not found
     */
    private String getParameterValue(String parameterId, String code) {
        try {
            ParameterId id = new ParameterId(parameterId, code);
            Optional<Parameter> paramOpt = parameterService.getById(id);
            if (paramOpt.isPresent()) {
                return paramOpt.get().getValue();
            }
        } catch (Exception e) {
            log.warn("Error querying parameter {}:{} - {}", parameterId, code, e.getMessage());
        }
        return null;
    }

    /**
     * Validate stage and offence type compatibility
     *
     * @param nextStageName New stage to apply
     * @param offenceType Offence type (O=Owner, D=Driver, H=Hirer, DIR=Director)
     * @return Error message if validation fails, null if valid
     */
    private String validateStageAndOffenceType(String nextStageName, String offenceType) {
        // Normalize offence type
        String normalizedType = normalizeOffenceType(offenceType);

        if ("DRIVER".equals(normalizedType)) {
            // Driver can only have: DN1, DN2, DR3
            if (!java.util.Arrays.asList("DN1", "DN2", "DR3").contains(nextStageName)) {
                return String.format("Stage %s is not eligible for DRIVER. Allowed stages: DN1, DN2, DR3", nextStageName);
            }
        } else {
            // Owner, Hirer, Director can only have: ROV, RD1, RD2, RR3
            if (!java.util.Arrays.asList("ROV", "RD1", "RD2", "RR3").contains(nextStageName)) {
                return String.format("Stage %s is not eligible for %s. Allowed stages: ROV, RD1, RD2, RR3", nextStageName, normalizedType);
            }
        }

        return null; // Valid
    }

    /**
     * Normalize offence type code
     */
    private String normalizeOffenceType(String offenceType) {
        if (offenceType == null) return "OWNER";

        switch (offenceType.toUpperCase()) {
            case "D":
            case "DRIVER":
                return "DRIVER";
            case "O":
            case "OWNER":
                return "OWNER";
            case "H":
            case "HIRER":
                return "HIRER";
            case "DIR":
            case "DIRECTOR":
                return "DIRECTOR";
            default:
                return "OWNER"; // Default to OWNER
        }
    }

    /**
     * Process a single PLUS notice change - New Flow
     * Based on diagram: OCMS15-Plus Apply Change Stage - New Flow (Step 6-7)
     *
     * Uses universal updateVonStage() method to ensure consistent stage progression
     * across all sources (OCMS, PLUS, Toppan)
     *
     * @param noticeNo Notice number
     * @param lastStageName Previous stage name (not used, kept for audit trail)
     * @param nextStageName New current stage to apply
     * @param offenceType Offence type
     * @param source Source of change
     * @throws PlusChangeStageException if validation or business rules fail
     */
    @Transactional
    protected void processPlusNoticeWithStageNewFlow(
            String noticeNo,
            String lastStageName,
            String nextStageName,
            String offenceType,
            String source) throws PlusChangeStageException {

        log.debug("Processing PLUS notice: {}, lastStage: {}, nextStage: {}, offenceType: {}",
                noticeNo, lastStageName, nextStageName, offenceType);

        // STEP 6: Patch VON (ocms_valid_offence_notice)
        log.debug("STEP 6: Updating VON for notice: {}", noticeNo);
        Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
        if (!vonOpt.isPresent()) {
            String errorMsg = String.format("STEP 6 FAILED: Notice number '%s' not found in ocms_valid_offence_notice table.", noticeNo);
            log.error(errorMsg);
            throw new PlusChangeStageException("NOTICE_NOT_FOUND", errorMsg);
        }

        OcmsValidOffenceNotice von = vonOpt.get();
        String currentLastStage = von.getLastProcessingStage();

        log.debug("STEP 6: Updating VON fields using universal updateVonStage() method");

        // Use the universal stage update method (same logic for OCMS, PLUS, and Toppan)
        // This ensures consistent stage progression across all sources
        updateVonStage(von, nextStageName);

        try {
            vonService.save(von);
            log.debug("STEP 6 SUCCESS: VON updated successfully for notice: {}", noticeNo);
        } catch (Exception e) {
            String errorMsg = String.format("STEP 6 FAILED: Error updating VON for notice '%s': %s", noticeNo, e.getMessage());
            log.error(errorMsg, e);
            throw new PlusChangeStageException("VON_UPDATE_FAILED", errorMsg);
        }

        // STEP 7: Insert audit trail to ocms_change_of_processing
        log.debug("STEP 7: Inserting audit trail for notice: {}", noticeNo);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        OcmsChangeOfProcessing cpsRecord = OcmsChangeOfProcessing.builder()
            .noticeNo(noticeNo)
            .dateOfChange(today)
            .lastProcessingStage(currentLastStage)
            .newProcessingStage(nextStageName)
            .reasonOfChange("PLS") // PLUS reason code
            .authorisedOfficer(source) // Use source system as officer
            .source(SOURCE_PLUS) // Track that this came from PLUS
            .remarks(String.format("Type: %s, Source: %s", offenceType, source))
            .creDate(now)
            .creUserId("SYSTEM") // System user for PLUS integration
            .build();

        try {
            cpsRepository.save(cpsRecord);
            log.debug("STEP 7 SUCCESS: Audit trail inserted successfully for notice: {}", noticeNo);
        } catch (Exception e) {
            String errorMsg = String.format("STEP 7 FAILED: Error inserting audit trail for notice '%s': %s", noticeNo, e.getMessage());
            log.error(errorMsg, e);
            throw new PlusChangeStageException("AUDIT_INSERT_FAILED", errorMsg);
        }

        log.info("Successfully updated PLUS notice {} from {} to {}", noticeNo, currentLastStage, nextStageName);
    }

    /**
     * Map internal eligibility error codes to PLUS error codes
     */
    private String mapEligibilityCodeToPlusCode(String eligibilityCode) {
        if (eligibilityCode == null) {
            return "INTERNAL_ERROR";
        }

        switch (eligibilityCode) {
            case EligibilityService.NOT_FOUND:
                return "NOTICE_NOT_FOUND";
            case EligibilityService.INELIGIBLE_STAGE:
            case EligibilityService.NO_STAGE_RULE:
            case EligibilityService.ROLE_CONFLICT:
            case EligibilityService.COURT_STAGE:
            case EligibilityService.PS_BLOCKED:
            case EligibilityService.TS_BLOCKED:
                return "STAGE_NOT_ELIGIBLE";
            case EligibilityService.EXISTING_CHANGE_TODAY:
                return "EXISTING_STAGE_CHANGE";
            default:
                return "INTERNAL_ERROR";
        }
    }

    /**
     * Get change records by date range
     * Used by BE-007 Retrieve Reports API
     *
     * @param startDate Start date time (inclusive)
     * @param endDate End date time (exclusive)
     * @return List of change records
     */
    public List<OcmsChangeOfProcessing> getChangeRecordsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate) {
        return cpsRepository.findByCreDateBetween(startDate, endDate);
    }

    /**
     * Process Toppan stage updates for notices processed by Toppan cron
     * Based on OCMS 15 Spec §2.5.2 - Handling for Manual Stage Change Notice in Toppan Cron
     *
     * This method:
     * 1. Checks if each notice has a manual change record (from OCMS or PLUS)
     * 2. For MANUAL changes: Updates VON stage only (amount_payable already updated)
     * 3. For AUTOMATIC changes: Updates VON stage AND calculates amount_payable
     *
     * @param request Toppan stage update request
     * @return Update statistics (automatic count, manual count, skipped, errors)
     */
    @Transactional
    public com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse
            processToppanStageUpdates(
                com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateRequest request) {

        log.info("Processing Toppan stage updates for {} notices, stage={}",
                request.getNoticeNumbers().size(), request.getCurrentStage());

        LocalDate today = request.getProcessingDate().toLocalDate();
        String currentStage = request.getCurrentStage();

        List<String> errors = new ArrayList<>();
        int automaticCount = 0;
        int manualCount = 0;
        int skippedCount = 0;

        // Process each notice
        for (String noticeNo : request.getNoticeNumbers()) {
            try {
                log.debug("Processing notice: {}", noticeNo);

                // STEP 1: Check if this is a manual change (Spec §2.5.2 Step 6)
                boolean isManual = isManualChange(noticeNo, today, currentStage);

                // STEP 2: Get VON record
                Optional<OcmsValidOffenceNotice> vonOpt = vonService.getById(noticeNo);
                if (!vonOpt.isPresent()) {
                    log.warn("VON not found for notice {}", noticeNo);
                    errors.add(noticeNo + ": VON not found");
                    skippedCount++;
                    continue;
                }

                OcmsValidOffenceNotice von = vonOpt.get();

                // STEP 3: Verify current stage matches (sanity check)
                // Toppan should only process notices at the expected stage
                if (!currentStage.equals(von.getNextProcessingStage())) {
                    log.warn("Stage mismatch for notice {}: expected {}, actual {}",
                            noticeNo, currentStage, von.getNextProcessingStage());
                    errors.add(noticeNo + ": Stage mismatch (expected " + currentStage +
                              ", actual " + von.getNextProcessingStage() + ")");
                    skippedCount++;
                    continue;
                }

                if (isManual) {
                    // STEP 4a: Manual change handling (Spec §2.5.2 Step 7)
                    // Update VON stage ONLY, DON'T touch amount_payable
                    log.debug("Processing MANUAL change for notice {}", noticeNo);

                    updateVonStage(von, currentStage);
                    vonService.save(von);

                    manualCount++;
                    log.info("Manual stage update completed for notice {}: stage={}, amount_payable NOT updated",
                            noticeNo, currentStage);

                } else {
                    // STEP 4b: Automatic change handling (Spec §2.5.2 Step 8)
                    // Update VON stage AND calculate/update amount_payable
                    log.debug("Processing AUTOMATIC change for notice {}", noticeNo);

                    String previousStage = von.getLastProcessingStage();
                    java.math.BigDecimal oldAmount = von.getAmountPayable();

                    // Update VON stage
                    updateVonStage(von, currentStage);

                    // Calculate new amount_payable
                    java.math.BigDecimal newAmount = amountPayableCalculationService.calculateAmountPayable(
                            previousStage, currentStage, oldAmount);

                    von.setAmountPayable(newAmount);
                    vonService.save(von);

                    automaticCount++;
                    log.info("Automatic stage update completed for notice {}: stage={}, amount_payable {} -> {}",
                            noticeNo, currentStage, oldAmount, newAmount);
                }

            } catch (Exception e) {
                log.error("Error processing notice {}: {}", noticeNo, e.getMessage(), e);
                errors.add(noticeNo + ": " + e.getMessage());
            }
        }

        log.info("Toppan stage update completed: total={}, automatic={}, manual={}, skipped={}, errors={}",
                request.getNoticeNumbers().size(), automaticCount, manualCount, skippedCount, errors.size());

        return com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsChangeOfProcessing.dto.ToppanStageUpdateResponse.builder()
                .totalNotices(request.getNoticeNumbers().size())
                .automaticUpdates(automaticCount)
                .manualUpdates(manualCount)
                .skipped(skippedCount)
                .errors(errors.isEmpty() ? null : errors)
                .success(errors.isEmpty())
                .build();
    }

    /**
     * Check if a notice has a manual change record in ocms_change_of_processing_stage table
     * Based on OCMS 15 Spec §2.5.2 Step 2-3 (Compare and identify manual vs automatic)
     *
     * A change is considered MANUAL if:
     * - Record exists in ocms_change_of_processing_stage for the notice on the given date
     * - AND source = 'OCMS' (from OCMS Staff Portal) OR source = 'PLUS' (from PLUS integration)
     *
     * A change is considered AUTOMATIC if:
     * - No record exists (natural Toppan progression)
     * - OR record exists but source = 'SYSTEM' or other non-manual source
     *
     * @param noticeNo Notice number
     * @param date Date of change
     * @param newStage New processing stage
     * @return true if manual change, false if automatic
     */
    private boolean isManualChange(String noticeNo, LocalDate date, String newStage) {
        try {
            // Query ocms_change_of_processing_stage table with composite key
            // Composite key: (notice_no, date_of_change, new_processing_stage)
            OcmsChangeOfProcessing.CompositeKey key =
                    new OcmsChangeOfProcessing.CompositeKey(noticeNo, date, newStage);

            Optional<OcmsChangeOfProcessing> changeRecordOpt = cpsRepository.findById(key);

            if (changeRecordOpt.isPresent()) {
                OcmsChangeOfProcessing record = changeRecordOpt.get();
                String source = record.getSource();

                // Check if source is OCMS or PLUS (manual sources)
                boolean isManual = SOURCE_OCMS.equals(source) || SOURCE_PLUS.equals(source);

                log.debug("Found change record for notice {}: source={}, isManual={}",
                        noticeNo, source, isManual);

                return isManual;
            }

            log.debug("No change record found for notice {} on {}, treating as automatic",
                    noticeNo, date);
            return false; // No record = automatic change

        } catch (Exception e) {
            log.error("Error checking manual change for notice {}: {}", noticeNo, e.getMessage(), e);
            // Default to automatic if check fails (safer - will calculate amount)
            return false;
        }
    }

    /**
     * Custom exception for PLUS change stage errors
     */
    public static class PlusChangeStageException extends Exception {
        private final String errorCode;

        public PlusChangeStageException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
