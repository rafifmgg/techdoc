package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.helpers;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PendingApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Helper class for generating email templates for OCMS 41 notifications
 * Based on email templates from OCMS 41 specifications
 */
@Slf4j
@Component
public class EmailTemplateHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /**
     * Generate daily officer notification email body
     * Template from OCMS 41 spec - Sheet 2: Email Alert for Manual Approval
     */
    public String generateDailyOfficerNotificationEmail(List<PendingApplicationDTO> pendingApplications) {
        StringBuilder emailBody = new StringBuilder();

        // Email header
        emailBody.append("The following notices from the Furnish Driver Details e-Service are pending approval.\n\n");

        // Table header
        emailBody.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>\n");
        emailBody.append("  <thead>\n");
        emailBody.append("    <tr style='background-color: #f2f2f2;'>\n");
        emailBody.append("      <th>S/No.</th>\n");
        emailBody.append("      <th>Notice No.</th>\n");
        emailBody.append("      <th>Vehicle No.</th>\n");
        emailBody.append("      <th>Offence Date & Time</th>\n");
        emailBody.append("      <th>Current Processing Stage</th>\n");
        emailBody.append("      <th>Status</th>\n");
        emailBody.append("      <th>Submission Received Date & Time</th>\n");
        emailBody.append("      <th>No. of Working Days Pending Approval</th>\n");
        emailBody.append("    </tr>\n");
        emailBody.append("  </thead>\n");
        emailBody.append("  <tbody>\n");

        // Table rows
        int rowNum = 1;
        for (PendingApplicationDTO app : pendingApplications) {
            emailBody.append("    <tr>\n");
            emailBody.append("      <td>").append(rowNum++).append("</td>\n");
            emailBody.append("      <td>").append(app.getNoticeNo()).append("</td>\n");
            emailBody.append("      <td>").append(app.getVehicleNo()).append("</td>\n");
            emailBody.append("      <td>").append(app.getOffenceDate().format(DATETIME_FORMATTER)).append("</td>\n");
            emailBody.append("      <td>").append(app.getCurrentProcessingStage()).append("</td>\n");
            emailBody.append("      <td>").append(getStatusDescription(app.getStatus())).append("</td>\n");
            emailBody.append("      <td>").append(app.getSubmissionReceivedDate().format(DATETIME_FORMATTER)).append("</td>\n");
            emailBody.append("      <td>").append(app.getWorkingDaysPending()).append("</td>\n");
            emailBody.append("    </tr>\n");
        }

        emailBody.append("  </tbody>\n");
        emailBody.append("</table>\n");

        return emailBody.toString();
    }

    /**
     * Generate email subject for daily officer notification
     */
    public String generateDailyOfficerNotificationSubject() {
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
        return String.format("Furnish Driver Details Pending Approval %s", currentDate);
    }

    /**
     * Generate email template for driver furnished notification
     * Template 1 from OCMS 41 spec - Sheet 4
     */
    public String generateDriverFurnishedEmail(String driverName, String noticeNo, String vehicleNo,
                                               LocalDateTime offenceDateTime, String placeOfOffence,
                                               String amount, String officerName) {
        String offenceDateTimeStr = offenceDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh.mm a"));
        String paymentDueDate = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);

        return String.format(
            "Website: https://go.gov.sg/ura-pon\n\n" +
            "%s\n\n" +
            "Dear %s,\n\n" +
            "Notice No.              : %s\n" +
            "Vehicle No.             : %s\n" +
            "Date & Time of Offence  : %s\n" +
            "Place of Offence        : %s\n" +
            "Amount payable          : %s by %s\n\n" +
            "The owner of %s has furnished you as the driver responsible for the above parking offence.\n\n" +
            "Please pay your parking fine immediately at https://go.gov.sg/ura-pf to avoid paying a court fine of $500 to $2,000 and/or serve a jail sentence of up to 3 months.\n\n" +
            "Yours faithfully,\n" +
            "%s\n" +
            "Car Parks Division\n" +
            "Urban Redevelopment Authority",
            LocalDateTime.now().format(DATE_FORMATTER),
            driverName,
            noticeNo,
            vehicleNo,
            offenceDateTimeStr,
            placeOfOffence,
            amount,
            paymentDueDate,
            vehicleNo,
            officerName
        );
    }

    /**
     * Generate SMS template for driver furnished notification
     * Template 2 from OCMS 41 spec - Sheet 4
     */
    public String generateDriverFurnishedSMS(String driverName, String vehicleNo, String noticeNo,
                                             LocalDateTime offenceDateTime, String compositionAmt) {
        String offenceDate = offenceDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String offenceTime = offenceDateTime.format(DateTimeFormatter.ofPattern("hh.mm a"));
        String paymentDueDate = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);

        return String.format(
            "Dear %s, the owner of %s has identified you as the driver responsible for the URA parking offence %s " +
            "committed on %s at %s. Please pay %s by %s at any AXS station or at the URA website (log in with Singpass). " +
            "Do not reply to this SMS",
            driverName,
            vehicleNo,
            noticeNo,
            offenceDate,
            offenceTime,
            compositionAmt,
            paymentDueDate
        );
    }

    /**
     * Generate email template for hirer furnished notification
     * Template 3 from OCMS 41 spec - Sheet 4
     */
    public String generateHirerFurnishedEmail(String hirerName, String noticeNo, String vehicleNo,
                                              LocalDateTime offenceDateTime, String placeOfOffence,
                                              String amount, String officerName) {
        String offenceDateTimeStr = offenceDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh.mm a"));
        String paymentDueDate = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);

        return String.format(
            "Website: https://go.gov.sg/ura-pon\n\n" +
            "%s\n\n" +
            "Dear %s,\n\n" +
            "Notice No.              : %s\n" +
            "Vehicle No.             : %s\n" +
            "Date & Time of Offence  : %s\n" +
            "Place of Offence        : %s\n" +
            "Amount payable          : %s by %s\n\n" +
            "The owner of %s has furnished you as the person in charge of the vehicle at the time of the offence.\n\n" +
            "Please pay your parking fine immediately at https://go.gov.sg/ura-pf to avoid paying a court fine of $500 to $2,000 and/or serve a jail sentence of up to 3 months.\n\n" +
            "If you were not the driver who had committed the offence, you may provide the details of the driver at https://go.gov.sg/ura-fdd by %s.\n\n" +
            "Yours faithfully,\n" +
            "%s\n" +
            "Car Parks Division\n" +
            "Urban Redevelopment Authority",
            LocalDateTime.now().format(DATE_FORMATTER),
            hirerName,
            noticeNo,
            vehicleNo,
            offenceDateTimeStr,
            placeOfOffence,
            amount,
            paymentDueDate,
            vehicleNo,
            paymentDueDate,
            officerName
        );
    }

    /**
     * Generate email template for rejected hirer (documents required)
     * Template 4 from OCMS 41 spec - Sheet 4
     */
    public String generateRejectedHirerEmail(String ownerName, String noticeNo, String vehicleNo,
                                             LocalDateTime offenceDateTime, String placeOfOffence,
                                             String amount, String officerName, String rejectionReason) {
        String offenceDateTimeStr = offenceDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh.mm a"));
        String resubmitDueDate = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);

        return String.format(
            "Website: https://go.gov.sg/ura-pon\n\n" +
            "%s\n\n" +
            "Dear %s,\n\n" +
            "Notice No.              : %s\n" +
            "Vehicle No.             : %s\n" +
            "Date & Time of Offence  : %s\n" +
            "Place of Offence        : %s\n" +
            "Amount payable          : %s by %s\n\n" +
            "We refer to your submission of hirer's particulars for the URA parking offence.\n\n" +
            "%s\n\n" +
            "Please submit the following document(s) via https://go.gov.sg/ura-fdd by %s:\n" +
            "- Vehicle rental agreement with supporting photo ID\n" +
            "- Vehicle log book with official company stamp and owner's signature\n" +
            "- Valid work permit/employment pass\n" +
            "- Statutory declaration (SD), i.e. 1 SD per parking offence\n\n" +
            "Alternatively, you can pay the parking fine at any AXS station or the URA website, https://go.gov.sg/ura-pf by %s. Otherwise, you will be liable for the parking offence.\n\n" +
            "Yours faithfully,\n" +
            "%s\n" +
            "Car Parks Division\n" +
            "Urban Redevelopment Authority",
            LocalDateTime.now().format(DATE_FORMATTER),
            ownerName,
            noticeNo,
            vehicleNo,
            offenceDateTimeStr,
            placeOfOffence,
            amount,
            resubmitDueDate,
            rejectionReason,
            resubmitDueDate,
            resubmitDueDate,
            officerName
        );
    }

    /**
     * Generate email template for general rejection
     * Template 7 from OCMS 41 spec - Sheet 4
     */
    public String generateGeneralRejectionEmail(String ownerName, String noticeNo, String vehicleNo,
                                                LocalDateTime offenceDateTime, String placeOfOffence,
                                                String amount, String officerName) {
        String offenceDateTimeStr = offenceDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh.mm a"));
        String paymentDueDate = LocalDateTime.now().plusDays(7).format(DATE_FORMATTER);

        return String.format(
            "Website: https://go.gov.sg/ura-pon\n\n" +
            "%s\n\n" +
            "Dear %s,\n\n" +
            "Notice No.              : %s\n" +
            "Vehicle No.             : %s\n" +
            "Date & Time of Offence  : %s\n" +
            "Place of Offence        : %s\n" +
            "Amount payable          : %s by %s\n\n" +
            "We are unable to accept the particulars. Please pay the parking fine at any AXS station or the URA website, " +
            "https://go.gov.sg/ura-pf, by %s. Otherwise, you will be liable for the parking offence.\n\n" +
            "Yours faithfully,\n" +
            "%s\n" +
            "Car Parks Division\n" +
            "Urban Redevelopment Authority",
            LocalDateTime.now().format(DATE_FORMATTER),
            ownerName,
            noticeNo,
            vehicleNo,
            offenceDateTimeStr,
            placeOfOffence,
            amount,
            paymentDueDate,
            paymentDueDate,
            officerName
        );
    }

    /**
     * Get status description for display
     */
    private String getStatusDescription(String status) {
        return switch (status) {
            case "P" -> "Pending";
            case "S" -> "Resubmission";
            case "A" -> "Approved";
            case "R" -> "Rejected";
            default -> status;
        };
    }
}
