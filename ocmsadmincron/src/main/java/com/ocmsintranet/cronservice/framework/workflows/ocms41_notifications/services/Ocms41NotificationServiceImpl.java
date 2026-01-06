package com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.services;

import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.EmailNotificationResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PSCheckResult;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.dto.PendingApplicationDTO;
import com.ocmsintranet.cronservice.framework.workflows.ocms41_notifications.helpers.EmailTemplateHelper;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailRequest;
import com.ocmsintranet.cronservice.utilities.emailutility.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service implementation for OCMS 41 notification workflows
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ocms41NotificationServiceImpl implements Ocms41NotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final EmailTemplateHelper emailTemplateHelper;
    private final EmailService emailService;

    @Value("${ocms41.notification.officer.emails:}")
    private String officerEmailAddresses;

    @Value("${ocms41.notification.enabled:true}")
    private boolean notificationEnabled;

    @Override
    @Transactional(readOnly = true)
    public EmailNotificationResult sendDailyOfficerNotification() {
        try {
            log.info("Starting daily officer notification process");

            // Get pending applications
            List<PendingApplicationDTO> pendingApplications = getPendingApplications();

            if (pendingApplications.isEmpty()) {
                log.info("No pending applications found. No notification sent.");
                return EmailNotificationResult.builder()
                    .success(true)
                    .message("No pending applications found")
                    .totalPendingApplications(0)
                    .emailsSent(0)
                    .emailsFailed(0)
                    .build();
            }

            log.info("Found {} pending applications requiring officer approval", pendingApplications.size());

            // Generate email content
            String emailSubject = emailTemplateHelper.generateDailyOfficerNotificationSubject();
            String emailBody = emailTemplateHelper.generateDailyOfficerNotificationEmail(pendingApplications);

            // Send email notification
            if (notificationEnabled) {
                if (officerEmailAddresses == null || officerEmailAddresses.isBlank()) {
                    log.warn("No officer email addresses configured. Skipping email send.");
                    return EmailNotificationResult.builder()
                        .success(false)
                        .message("No officer email addresses configured")
                        .totalPendingApplications(pendingApplications.size())
                        .emailsSent(0)
                        .emailsFailed(1)
                        .build();
                }

                // Send email using EmailService
                log.info("Sending email notification to: {}", officerEmailAddresses);
                EmailRequest emailRequest = new EmailRequest();
                emailRequest.setTo(officerEmailAddresses);
                emailRequest.setSubject(emailSubject);
                emailRequest.setHtmlContent(emailBody);

                boolean sent = emailService.sendEmail(emailRequest);

                if (sent) {
                    log.info("Daily officer notification email sent successfully");
                    return EmailNotificationResult.builder()
                        .success(true)
                        .message("Email notification sent successfully")
                        .totalPendingApplications(pendingApplications.size())
                        .emailsSent(1)
                        .emailsFailed(0)
                        .build();
                } else {
                    log.error("Failed to send daily officer notification email");
                    return EmailNotificationResult.builder()
                        .success(false)
                        .message("Failed to send email notification")
                        .totalPendingApplications(pendingApplications.size())
                        .emailsSent(0)
                        .emailsFailed(1)
                        .build();
                }
            } else {
                log.warn("Email notification is disabled. Skipping email send.");
                return EmailNotificationResult.builder()
                    .success(true)
                    .message("Email notification is disabled")
                    .totalPendingApplications(pendingApplications.size())
                    .emailsSent(0)
                    .emailsFailed(0)
                    .build();
            }

        } catch (Exception e) {
            log.error("Error sending daily officer notification", e);
            return EmailNotificationResult.builder()
                .success(false)
                .message("Error: " + e.getMessage())
                .totalPendingApplications(0)
                .emailsSent(0)
                .emailsFailed(1)
                .build();
        }
    }

    @Override
    @Transactional
    public PSCheckResult checkPermanentSuspensions() {
        try {
            log.info("Starting PS check for pending applications");

            // Query to get pending applications with their notice details
            String query = """
                SELECT fa.txn_no, fa.notice_no, fa.vehicle_no, fa.status, fa.cre_date,
                       ond.perm_suspend_ind
                FROM ocmsizmgr.ocms_furnish_application fa
                INNER JOIN ocmsizmgr.ocms_offence_notice_detail ond ON fa.notice_no = ond.notice_no
                WHERE fa.status IN ('P', 'S')
                  AND ond.perm_suspend_ind = 'Y'
                """;

            List<String> txnNosToReject = jdbcTemplate.query(query, (rs, rowNum) -> rs.getString("txn_no"));

            if (txnNosToReject.isEmpty()) {
                log.info("No permanently suspended notices found with pending applications");
                return PSCheckResult.builder()
                    .success(true)
                    .message("No permanently suspended notices found")
                    .totalChecked(0)
                    .suspendedNotices(0)
                    .rejectedApplications(0)
                    .build();
            }

            log.info("Found {} applications with permanently suspended notices", txnNosToReject.size());

            // Auto-reject the applications
            String updateQuery = """
                UPDATE ocmsizmgr.ocms_furnish_application
                SET status = 'R',
                    remarks = CONCAT(COALESCE(remarks, ''), ' [Auto-rejected: Notice is permanently suspended]'),
                    upd_date = SYSTIMESTAMP,
                    upd_user_id = 'SYSTEM_PS_CHECK'
                WHERE txn_no = ?
                """;

            int rejectedCount = 0;
            for (String txnNo : txnNosToReject) {
                try {
                    int updated = jdbcTemplate.update(updateQuery, txnNo);
                    if (updated > 0) {
                        rejectedCount++;
                        log.info("Auto-rejected application txnNo: {} due to permanent suspension", txnNo);
                    }
                } catch (Exception e) {
                    log.error("Error rejecting application txnNo: {}", txnNo, e);
                }
            }

            return PSCheckResult.builder()
                .success(true)
                .message(String.format("PS check completed. Auto-rejected %d applications", rejectedCount))
                .totalChecked(txnNosToReject.size())
                .suspendedNotices(txnNosToReject.size())
                .rejectedApplications(rejectedCount)
                .build();

        } catch (Exception e) {
            log.error("Error during PS check", e);
            return PSCheckResult.builder()
                .success(false)
                .message("Error: " + e.getMessage())
                .totalChecked(0)
                .suspendedNotices(0)
                .rejectedApplications(0)
                .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingApplicationDTO> getPendingApplications() {
        String query = """
            SELECT fa.txn_no, fa.notice_no, fa.vehicle_no, fa.offence_date,
                   fa.last_processing_stage, fa.status, fa.cre_date,
                   fa.furnish_name, fa.furnish_id_no, fa.owner_name, fa.owner_id_no
            FROM ocmsizmgr.ocms_furnish_application fa
            WHERE fa.status IN ('P', 'S')
            ORDER BY fa.cre_date ASC
            """;

        return jdbcTemplate.query(query, new PendingApplicationRowMapper());
    }

    @Override
    public int calculateWorkingDays(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        int workingDays = 0;
        LocalDateTime current = startDate.toLocalDate().atStartOfDay();
        LocalDateTime end = endDate.toLocalDate().atStartOfDay();

        while (current.isBefore(end) || current.isEqual(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            // Count only weekdays (Monday to Friday)
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                // TODO: Add public holiday checking if needed
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * Row mapper for PendingApplicationDTO
     */
    private class PendingApplicationRowMapper implements RowMapper<PendingApplicationDTO> {
        @Override
        public PendingApplicationDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            LocalDateTime creDate = rs.getTimestamp("cre_date").toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            int workingDays = calculateWorkingDays(creDate, now);

            return PendingApplicationDTO.builder()
                .noticeNo(rs.getString("notice_no"))
                .vehicleNo(rs.getString("vehicle_no"))
                .offenceDate(rs.getTimestamp("offence_date").toLocalDateTime())
                .currentProcessingStage(rs.getString("last_processing_stage"))
                .status(rs.getString("status"))
                .submissionReceivedDate(creDate)
                .workingDaysPending(workingDays)
                .furnishName(rs.getString("furnish_name"))
                .furnishIdNo(rs.getString("furnish_id_no"))
                .ownerName(rs.getString("owner_name"))
                .ownerIdNo(rs.getString("owner_id_no"))
                .build();
        }
    }
}
