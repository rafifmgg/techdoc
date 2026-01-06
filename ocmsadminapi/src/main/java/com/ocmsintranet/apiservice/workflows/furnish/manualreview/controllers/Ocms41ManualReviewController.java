package com.ocmsintranet.apiservice.workflows.furnish.manualreview.controllers;

import com.ocmsintranet.apiservice.workflows.furnish.manualreview.services.Ocms41ManualReviewService;
import com.ocmsintranet.apiservice.workflows.furnish.manualreview.services.Ocms41ManualReviewService.ApprovalResult;
import com.ocmsintranet.apiservice.workflows.furnish.manualreview.services.Ocms41ManualReviewService.FurnishabilityResult;
import com.ocmsintranet.apiservice.workflows.furnish.manualreview.services.Ocms41ManualReviewService.NotificationResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OCMS 41: Manual Review Controller
 *
 * REST APIs for OIC manual review and approval/rejection of furnished submissions.
 *
 * Endpoints:
 * - POST /furnishability-check: Check if notice is still furnishable
 * - POST /approve-hirer: Approve Hirer submission
 * - POST /approve-driver: Approve Driver submission
 * - POST /reject: Reject submission
 * - POST /send-email: Send email notification
 * - POST /send-sms: Send SMS notification
 *
 * Based on Functional Spec v1.1 Section 3.4.4.5.3
 */
@RestController
@RequestMapping("/${api.version}/ocms41/manual-review")
@Slf4j
@RequiredArgsConstructor
public class Ocms41ManualReviewController {

    private final Ocms41ManualReviewService manualReviewService;

    /**
     * API 13: Furnishability Check
     *
     * Checks whether a notice is still furnishable before approval.
     *
     * Request Body:
     * {
     *   "noticeNo": "500500303J"
     * }
     *
     * Response:
     * {
     *   "furnishable": true,
     *   "reason": null
     * }
     *
     * @param request Notice number to check
     * @return FurnishabilityResult
     */
    @PostMapping("/furnishability-check")
    public ResponseEntity<Map<String, Object>> checkFurnishability(@RequestBody FurnishabilityCheckRequest request) {
        log.info("REST request to check furnishability for notice={}", request.getNoticeNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
                response.put("furnishable", false);
                response.put("reason", "Notice number is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Check furnishability
            FurnishabilityResult result = manualReviewService.checkFurnishability(request.getNoticeNo());

            response.put("furnishable", result.isFurnishable());
            response.put("reason", result.getReason());

            return result.isFurnishable()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response); // 422 Unprocessable Entity

        } catch (Exception e) {
            log.error("Error checking furnishability for notice={}: {}", request.getNoticeNo(), e.getMessage(), e);
            response.put("furnishable", false);
            response.put("reason", "Error checking furnishability: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 14: Approve Hirer
     *
     * Approves a furnished submission for a Hirer.
     *
     * Request Body:
     * {
     *   "txnNo": "TXN001",
     *   "reviewedBy": "OIC_USER123"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Success"
     * }
     *
     * @param request Approval request
     * @return ApprovalResult
     */
    @PostMapping("/approve-hirer")
    public ResponseEntity<Map<String, Object>> approveHirer(@RequestBody ApprovalRequest request) {
        log.info("REST request to approve Hirer for txn_no={}, reviewed_by={}", request.getTxnNo(), request.getReviewedBy());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getTxnNo() == null || request.getTxnNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Transaction number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getReviewedBy() == null || request.getReviewedBy().isBlank()) {
                response.put("success", false);
                response.put("message", "Reviewed by user ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Approve Hirer
            ApprovalResult result = manualReviewService.approveHirer(request.getTxnNo(), request.getReviewedBy());

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("Error approving Hirer for txn_no={}: {}", request.getTxnNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error approving Hirer: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 15: Approve Driver
     *
     * Approves a furnished submission for a Driver.
     *
     * Request Body:
     * {
     *   "txnNo": "TXN002",
     *   "reviewedBy": "OIC_USER123"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Success"
     * }
     *
     * @param request Approval request
     * @return ApprovalResult
     */
    @PostMapping("/approve-driver")
    public ResponseEntity<Map<String, Object>> approveDriver(@RequestBody ApprovalRequest request) {
        log.info("REST request to approve Driver for txn_no={}, reviewed_by={}", request.getTxnNo(), request.getReviewedBy());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getTxnNo() == null || request.getTxnNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Transaction number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getReviewedBy() == null || request.getReviewedBy().isBlank()) {
                response.put("success", false);
                response.put("message", "Reviewed by user ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Approve Driver
            ApprovalResult result = manualReviewService.approveDriver(request.getTxnNo(), request.getReviewedBy());

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("Error approving Driver for txn_no={}: {}", request.getTxnNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error approving Driver: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 18: Reject Submission
     *
     * Rejects a furnished submission.
     *
     * Request Body:
     * {
     *   "txnNo": "TXN003",
     *   "reviewedBy": "OIC_USER123",
     *   "rejectionReason": "Invalid documents provided"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Success"
     * }
     *
     * @param request Rejection request
     * @return ApprovalResult
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, Object>> rejectSubmission(@RequestBody RejectionRequest request) {
        log.info("REST request to reject submission for txn_no={}, reviewed_by={}, reason={}",
                request.getTxnNo(), request.getReviewedBy(), request.getRejectionReason());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getTxnNo() == null || request.getTxnNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Transaction number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getReviewedBy() == null || request.getReviewedBy().isBlank()) {
                response.put("success", false);
                response.put("message", "Reviewed by user ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                response.put("success", false);
                response.put("message", "Rejection reason is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Reject submission
            ApprovalResult result = manualReviewService.rejectSubmission(
                    request.getTxnNo(),
                    request.getReviewedBy(),
                    request.getRejectionReason()
            );

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(422).body(response);

        } catch (Exception e) {
            log.error("Error rejecting submission for txn_no={}: {}", request.getTxnNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error rejecting submission: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 16: Send Email
     *
     * Sends email notification.
     *
     * Request Body:
     * {
     *   "txnNo": "TXN001",
     *   "noticeNo": "500500303J",
     *   "recipient": "test@example.com",
     *   "subject": "Your furnish submission has been approved",
     *   "htmlBody": "<html>...</html>"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Email sent successfully"
     * }
     *
     * @param request Email request
     * @return NotificationResult
     */
    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody EmailRequest request) {
        log.info("REST request to send email for txn_no={}, notice={}, recipient={}",
                request.getTxnNo(), request.getNoticeNo(), request.getRecipient());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getTxnNo() == null || request.getTxnNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Transaction number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Notice number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getRecipient() == null || request.getRecipient().isBlank()) {
                response.put("success", false);
                response.put("message", "Recipient email is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSubject() == null || request.getSubject().isBlank()) {
                response.put("success", false);
                response.put("message", "Email subject is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getHtmlBody() == null || request.getHtmlBody().isBlank()) {
                response.put("success", false);
                response.put("message", "Email body is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Send email
            NotificationResult result = manualReviewService.sendEmail(
                    request.getTxnNo(),
                    request.getNoticeNo(),
                    request.getRecipient(),
                    request.getSubject(),
                    request.getHtmlBody()
            );

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            log.error("Error sending email for txn_no={}: {}", request.getTxnNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error sending email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API 17: Send SMS
     *
     * Sends SMS notification.
     *
     * Request Body:
     * {
     *   "txnNo": "TXN001",
     *   "noticeNo": "500500303J",
     *   "mobileCode": "65",
     *   "mobileNo": "91234567",
     *   "message": "Your furnish submission has been approved"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "SMS sent successfully"
     * }
     *
     * @param request SMS request
     * @return NotificationResult
     */
    @PostMapping("/send-sms")
    public ResponseEntity<Map<String, Object>> sendSms(@RequestBody SmsRequest request) {
        log.info("REST request to send SMS for txn_no={}, notice={}, mobile=+{}{}",
                request.getTxnNo(), request.getNoticeNo(), request.getMobileCode(), request.getMobileNo());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getTxnNo() == null || request.getTxnNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Transaction number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getNoticeNo() == null || request.getNoticeNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Notice number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMobileNo() == null || request.getMobileNo().isBlank()) {
                response.put("success", false);
                response.put("message", "Mobile number is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMessage() == null || request.getMessage().isBlank()) {
                response.put("success", false);
                response.put("message", "SMS message is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Send SMS
            NotificationResult result = manualReviewService.sendSms(
                    request.getTxnNo(),
                    request.getNoticeNo(),
                    request.getMobileCode(),
                    request.getMobileNo(),
                    request.getMessage()
            );

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return result.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            log.error("Error sending SMS for txn_no={}: {}", request.getTxnNo(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error sending SMS: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== REQUEST DTOs ====================

    @Data
    public static class FurnishabilityCheckRequest {
        private String noticeNo;
    }

    @Data
    public static class ApprovalRequest {
        private String txnNo;
        private String reviewedBy;
    }

    @Data
    public static class RejectionRequest {
        private String txnNo;
        private String reviewedBy;
        private String rejectionReason;
    }

    @Data
    public static class EmailRequest {
        private String txnNo;
        private String noticeNo;
        private String recipient;
        private String subject;
        private String htmlBody;
    }

    @Data
    public static class SmsRequest {
        private String txnNo;
        private String noticeNo;
        private String mobileCode;
        private String mobileNo;
        private String message;
    }
}
