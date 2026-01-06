package com.ocmsintranet.apiservice.workflows.furnish.helpers;

import com.ocmsintranet.apiservice.workflows.furnish.domain.EmailTemplateType;
import com.ocmsintranet.apiservice.workflows.furnish.dashboard.dto.FurnishApplicationDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating email and SMS content from templates.
 * Based on OCMS 41 email/SMS templates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FurnishTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy hh.mm a");

    /**
     * Generate email subject for approval
     */
    public String generateApprovalEmailSubject(String noticeNo) {
        return "URA Parking Offence Notice " + noticeNo;
    }

    /**
     * Generate email body for owner approval confirmation
     */
    public String generateOwnerApprovalConfirmationEmail(FurnishApplicationDetailResponse detail) {
        return String.format("""
                Website: https://go.gov.sg/ura-pon

                %s

                Dear %s,

                Notice No.              : %s
                Vehicle No.             : %s
                Date & Time of Offence  : %s
                Place of Offence        : %s

                Your furnish application has been approved. The furnished person (%s) will be notified separately.

                Thank you.

                Yours faithfully,
                Car Parks Division
                Urban Redevelopment Authority
                """,
                LocalDateTime.now().format(DATE_FORMATTER),
                detail.getOwnerName(),
                detail.getNoticeNo(),
                detail.getVehicleNo(),
                detail.getOffenceDate().format(DATETIME_FORMATTER),
                detail.getPpName(),
                detail.getFurnishName()
        );
    }

    /**
     * Generate email body for driver furnished (Template 1)
     */
    public String generateDriverFurnishedEmail(FurnishApplicationDetailResponse detail, String officerName) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format("""
                Website: https://go.gov.sg/ura-pon

                %s

                Dear %s,

                Notice No.              : %s
                Vehicle No.             : %s
                Date & Time of Offence  : %s
                Place of Offence        : %s
                Amount payable          : $__ by %s

                The owner of %s has furnished you as the driver responsible for the above parking offence.

                Please pay your parking fine immediately at https://go.gov.sg/ura-pf to avoid paying a court fine of $500 to $2,000 and/or serve a jail sentence of up to 3 months.

                Yours faithfully,
                %s
                Car Parks Division
                Urban Redevelopment Authority
                """,
                LocalDateTime.now().format(DATE_FORMATTER),
                detail.getFurnishName(),
                detail.getNoticeNo(),
                detail.getVehicleNo(),
                detail.getOffenceDate().format(DATETIME_FORMATTER),
                detail.getPpName(),
                dueDate.format(DATE_FORMATTER),
                detail.getVehicleNo(),
                officerName
        );
    }

    /**
     * Generate SMS body for driver furnished (Template 2)
     */
    public String generateDriverFurnishedSms(FurnishApplicationDetailResponse detail) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format(
                "Dear %s, the owner of %s has identified you as the driver responsible for the URA parking offence %s committed on %s at %s. Please pay $__ by %s at any AXS station or at the URA website (log in with Singpass). Do not reply to this SMS",
                detail.getFurnishName(),
                detail.getVehicleNo(),
                detail.getNoticeNo(),
                detail.getOffenceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                detail.getOffenceDate().format(DateTimeFormatter.ofPattern("hh.mm a")),
                dueDate.format(DATE_FORMATTER)
        );
    }

    /**
     * Generate email body for hirer furnished (Template 3)
     */
    public String generateHirerFurnishedEmail(FurnishApplicationDetailResponse detail, String officerName) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format("""
                Website: https://go.gov.sg/ura-pon

                %s

                Dear %s,

                Notice No.              : %s
                Vehicle No.             : %s
                Date & Time of Offence  : %s
                Place of Offence        : %s
                Amount payable          : $__ by %s

                The owner of %s has furnished you as the person in charge of the vehicle at the time of the offence.

                Please pay your parking fine immediately at https://go.gov.sg/ura-pf to avoid paying a court fine of $500 to $2,000 and/or serve a jail sentence of up to 3 months.

                If you were not the driver who had committed the offence, you may provide the details of the driver at https://go.gov.sg/ura-fdd by %s.

                Yours faithfully,
                %s
                Car Parks Division
                Urban Redevelopment Authority
                """,
                LocalDateTime.now().format(DATE_FORMATTER),
                detail.getFurnishName(),
                detail.getNoticeNo(),
                detail.getVehicleNo(),
                detail.getOffenceDate().format(DATETIME_FORMATTER),
                detail.getPpName(),
                dueDate.format(DATE_FORMATTER),
                detail.getVehicleNo(),
                dueDate.format(DATE_FORMATTER),
                officerName
        );
    }

    /**
     * Generate email subject for rejection
     */
    public String generateRejectionEmailSubject(String noticeNo) {
        return "URA Parking Offence Notice " + noticeNo + " - Additional Information Required";
    }

    /**
     * Generate email body for rejected submission (Template 4: Documents Required)
     */
    public String generateRejectionDocsRequiredEmail(FurnishApplicationDetailResponse detail, String officerName) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format("""
                Website: https://go.gov.sg/ura-pon

                %s

                Dear %s,

                Notice No.              : %s
                Vehicle No.             : %s
                Date & Time of Offence  : %s
                Place of Offence        : %s
                Amount payable          : $__ by %s

                We refer to your submission of hirer's particulars for the URA parking offence.

                Please submit the following document(s) via https://go.gov.sg/ura-fdd by %s:
                - Vehicle rental agreement with supporting photo ID
                - Vehicle log book with official company stamp and owner's signature
                - Valid work permit/employment pass
                - Statutory declaration (SD), i.e. 1 SD per parking offence

                Alternatively, you can pay the parking fine at any AXS station or the URA website, https://go.gov.sg/ura-pf by %s. Otherwise, you will be liable for the parking offence.

                Yours faithfully,
                %s
                Car Parks Division
                Urban Redevelopment Authority
                """,
                LocalDateTime.now().format(DATE_FORMATTER),
                detail.getOwnerName(),
                detail.getNoticeNo(),
                detail.getVehicleNo(),
                detail.getOffenceDate().format(DATETIME_FORMATTER),
                detail.getPpName(),
                dueDate.format(DATE_FORMATTER),
                dueDate.format(DATE_FORMATTER),
                dueDate.format(DATE_FORMATTER),
                officerName
        );
    }

    /**
     * Generate email body for rejected submission (Template 5: Multiple Hirers)
     */
    public String generateRejectionMultipleHirersEmail(FurnishApplicationDetailResponse detail) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format("""
                We are unable to accept the particulars as there is an existing hirer identified for the parking offence.

                Please submit the following document(s) via https://go.gov.sg/ura-fdd by %s:
                - Vehicle rental agreement with supporting photo ID
                - Vehicle log book with official company stamp and owner's signature
                - Work permit/employment pass
                - Statutory declaration (SD)
                """,
                dueDate.format(DATE_FORMATTER)
        );
    }

    /**
     * Generate email body for rejected submission (Template 6: Rental Discrepancy)
     */
    public String generateRejectionRentalDiscrepancyEmail() {
        return """
                We are unable to accept the particulars due to the following:
                - Offence date is not within the rental period
                - Rental agreement shows a different vehicle / hirer
                """;
    }

    /**
     * Generate email body for rejected submission (Template 7: General)
     */
    public String generateRejectionGeneralEmail() {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        return String.format(
                "We are unable to accept the particulars. Please pay the parking fine at any AXS station or the URA website, https://go.gov.sg/ura-pf, by %s. Otherwise, you will be liable for the parking offence.",
                dueDate.format(DATE_FORMATTER)
        );
    }

    /**
     * Generate email content based on template type
     */
    public String generateEmailContent(
            EmailTemplateType templateType,
            FurnishApplicationDetailResponse detail,
            String officerName) {

        return switch (templateType) {
            case DRIVER_FURNISHED -> generateDriverFurnishedEmail(detail, officerName);
            case HIRER_FURNISHED -> generateHirerFurnishedEmail(detail, officerName);
            case REJECTED_DOCS_REQUIRED -> generateRejectionDocsRequiredEmail(detail, officerName);
            case REJECTED_MULTIPLE_HIRERS -> generateRejectionMultipleHirersEmail(detail);
            case REJECTED_RENTAL_DISCREPANCY -> generateRejectionRentalDiscrepancyEmail();
            case REJECTED_GENERAL -> generateRejectionGeneralEmail();
        };
    }
}
