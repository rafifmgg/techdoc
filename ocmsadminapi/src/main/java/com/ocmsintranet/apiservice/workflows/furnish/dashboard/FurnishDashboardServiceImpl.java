package com.ocmsintranet.apiservice.workflows.furnish.dashboard;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplication;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplication.OcmsFurnishApplicationRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDoc;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsFurnishApplicationDoc.OcmsFurnishApplicationDocRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListRequest;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of officer dashboard service.
 * Based on OCMS 41 User Stories 41.9-41.14.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishDashboardServiceImpl implements FurnishDashboardService {

    private final OcmsFurnishApplicationRepository furnishApplicationRepository;
    private final OcmsFurnishApplicationDocRepository furnishApplicationDocRepository;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;

    @Override
    public FurnishApplicationListResponse listFurnishApplications(FurnishApplicationListRequest request) {
        log.debug("Listing furnish applications with filters: {}", request);

        // Get all applications based on status filter
        List<OcmsFurnishApplication> allApplications;
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            allApplications = furnishApplicationRepository.findByStatusIn(request.getStatuses());
        } else {
            allApplications = furnishApplicationRepository.findAll();
        }

        // Apply additional filters
        List<OcmsFurnishApplication> filteredApplications = allApplications.stream()
                .filter(app -> matchesFilters(app, request))
                .collect(Collectors.toList());

        // Sort results
        sortApplications(filteredApplications, request);

        // Apply pagination
        int page = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 50;
        long totalRecords = filteredApplications.size();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredApplications.size());

        List<OcmsFurnishApplication> paginatedApplications = filteredApplications.subList(fromIndex, toIndex);

        // Convert to summary DTOs
        List<FurnishApplicationListResponse.FurnishApplicationSummary> summaries = paginatedApplications.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        log.info("Found {} furnish applications (page {}/{})", totalRecords, page + 1, totalPages);

        return FurnishApplicationListResponse.builder()
                .applications(summaries)
                .totalRecords(totalRecords)
                .currentPage(page)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public FurnishApplicationDetailResponse getApplicationDetail(String txnNo) {
        log.debug("Getting furnish application detail for txnNo: {}", txnNo);

        Optional<OcmsFurnishApplication> applicationOpt = furnishApplicationRepository.findById(txnNo);
        if (applicationOpt.isEmpty()) {
            throw new IllegalArgumentException("Furnish application not found: " + txnNo);
        }

        OcmsFurnishApplication application = applicationOpt.get();

        // Get notice details for current processing stage
        Optional<OcmsValidOffenceNotice> noticeOpt = validOffenceNoticeRepository.findById(application.getNoticeNo());
        String currentProcessingStage = noticeOpt.map(OcmsValidOffenceNotice::getCurrentProcessingStage).orElse("N/A");

        // Calculate working days pending
        Integer workingDaysPending = calculateWorkingDaysPending(application.getCreDate());

        // Get document attachments
        List<OcmsFurnishApplicationDoc> docs = furnishApplicationDocRepository.findByTxnNo(txnNo);
        List<FurnishApplicationDetailResponse.DocumentInfo> documentInfos = docs.stream()
                .map(doc -> FurnishApplicationDetailResponse.DocumentInfo.builder()
                        .txnNo(doc.getTxnNo())
                        .attachmentId(doc.getAttachmentId())
                        .docName(doc.getDocName())
                        .build())
                .collect(Collectors.toList());

        log.info("Retrieved detail for furnish application: {}, status: {}", txnNo, application.getStatus());

        return FurnishApplicationDetailResponse.fromEntity(
                application,
                currentProcessingStage,
                workingDaysPending,
                documentInfos
        );
    }

    /**
     * Check if application matches search filters
     */
    private boolean matchesFilters(OcmsFurnishApplication app, FurnishApplicationListRequest request) {
        if (request.getNoticeNo() != null && !app.getNoticeNo().contains(request.getNoticeNo())) {
            return false;
        }
        if (request.getVehicleNo() != null && !app.getVehicleNo().equalsIgnoreCase(request.getVehicleNo())) {
            return false;
        }
        if (request.getFurnishIdNo() != null && !app.getFurnishIdNo().contains(request.getFurnishIdNo())) {
            return false;
        }
        if (request.getSubmissionDateFrom() != null && app.getCreDate().isBefore(request.getSubmissionDateFrom())) {
            return false;
        }
        if (request.getSubmissionDateTo() != null && app.getCreDate().isAfter(request.getSubmissionDateTo())) {
            return false;
        }
        return true;
    }

    /**
     * Sort applications based on request parameters
     */
    private void sortApplications(List<OcmsFurnishApplication> applications, FurnishApplicationListRequest request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "submissionDate";
        boolean ascending = "ASC".equalsIgnoreCase(request.getSortDirection());

        applications.sort((a1, a2) -> {
            int comparison = switch (sortBy) {
                case "noticeNo" -> a1.getNoticeNo().compareTo(a2.getNoticeNo());
                case "vehicleNo" -> a1.getVehicleNo().compareTo(a2.getVehicleNo());
                case "status" -> a1.getStatus().compareTo(a2.getStatus());
                default -> a1.getCreDate().compareTo(a2.getCreDate()); // submissionDate
            };
            return ascending ? comparison : -comparison;
        });
    }

    /**
     * Convert entity to summary DTO
     */
    private FurnishApplicationListResponse.FurnishApplicationSummary toSummary(OcmsFurnishApplication app) {
        // Get notice details
        Optional<OcmsValidOffenceNotice> noticeOpt = validOffenceNoticeRepository.findById(app.getNoticeNo());
        String currentProcessingStage = noticeOpt.map(OcmsValidOffenceNotice::getCurrentProcessingStage).orElse("N/A");

        // Calculate working days pending
        Integer workingDaysPending = calculateWorkingDaysPending(app.getCreDate());

        return FurnishApplicationListResponse.FurnishApplicationSummary.builder()
                .txnNo(app.getTxnNo())
                .noticeNo(app.getNoticeNo())
                .vehicleNo(app.getVehicleNo())
                .offenceDate(app.getOffenceDate())
                .ppCode(app.getPpCode())
                .ppName(app.getPpName())
                .currentProcessingStage(currentProcessingStage)
                .status(app.getStatus())
                .submissionDate(app.getCreDate())
                .workingDaysPending(workingDaysPending)
                .furnishName(app.getFurnishName())
                .furnishIdNo(app.getFurnishIdNo())
                .ownerDriverIndicator(app.getOwnerDriverIndicator())
                .build();
    }

    /**
     * Calculate number of working days between submission date and now.
     * Excludes weekends (Saturday, Sunday).
     * Note: Does not account for public holidays - would need external holiday calendar service.
     */
    private Integer calculateWorkingDaysPending(LocalDateTime submissionDate) {
        if (submissionDate == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long totalDays = ChronoUnit.DAYS.between(submissionDate.toLocalDate(), now.toLocalDate());

        int workingDays = 0;
        LocalDateTime current = submissionDate;

        for (int i = 0; i < totalDays; i++) {
            current = current.plusDays(1);
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }

        return workingDays;
    }
}
