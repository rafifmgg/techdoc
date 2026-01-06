package com.ocmseservice.apiservice.workflows.urapg.service;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice.EocmsValidOffenceNotice;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice.EocmsValidOffenceNoticeRepository;
import com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver.EocmsOffenceNoticeOwnerDriver;
import com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver.EocmsOffenceNoticeOwnerDriverRepository;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetailRepository;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAudit;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAuditRepository;
import com.ocmseservice.apiservice.workflows.urapg.model.GenerateTransactionRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.TransactionDTO;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentResponse;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for URA Payment Gateway integration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UraPaymentService {

    private final EocmsWebTxnDetailRepository webTxnDetailRepository;
    private final EocmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final EocmsOffenceNoticeOwnerDriverRepository ownerDriverRepository;
    //private final EocmsWebTxnAuditRepository webTxnAuditRepository;
    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${ura.payment.gateway.url}")
    private String paymentGatewayUrl;

    @Value("${ura.payment.gateway.store.url}")
    private String storeApiUrl;

    @Value("${ura.payment.gateway.apim.header:Ocp-Apim-Subscription-Key}")
    private String apimHeader;

    @Value("${ura.payment.gateway.apim.key:test-key}")
    private String apimKey;

    @Value("${ura.payment.success.url}")
    private String successUrl;

    @Value("${ura.payment.failure.url}")
    private String failureUrl;

    @Value("${ura.payment.callback.url}")
    private String callbackUrl;

    public class TransactionIdGenerator {
        private static final String PREFIX = "TXN";
        private static final int SEQUENCE_LENGTH = 9;

        /**
         * Generate a transaction ID using database sequence and checksum
         *
         * @return Transaction ID in format: PREFIX + 9-digit sequence + checksum digit
         */
        public String generateTransactionId() {
            try {
                // Get next value from sequence
                Long sequenceValue = jdbcTemplate.queryForObject(
                        "SELECT NEXTVAL('seq_ocms_txn_ref_number')", Long.class);

                // Format sequence to 9 digits with leading zeros if needed
                String formattedSequence = String.format("%0" + SEQUENCE_LENGTH + "d", sequenceValue);

                // Calculate checksum
                int checksum = calculateChecksum(formattedSequence);

                // Combine prefix + sequence + checksum
                return PREFIX + formattedSequence + checksum;
            } catch (Exception e) {
                log.error("Error generating transaction ID from sequence: {}", e.getMessage(), e);
                // Fallback to UUID-based ID if sequence fails
                return PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
        }

        /**
         * Calculate checksum using the specified formula:
         * Checksum = [(1×d1) + (2×d2) + (3×d3) + (4×d4) + (10×d5) + (9×d6) + (8×d7) + (7×d8) + (6×d9)] % 10
         *
         * @param sequence 9-digit sequence number
         * @return Checksum digit (0-9)
         */
        private int calculateChecksum(String sequence) {
            if (sequence.length() != SEQUENCE_LENGTH) {
                throw new IllegalArgumentException("Sequence must be exactly " + SEQUENCE_LENGTH + " digits");
            }

            int[] weights = {1, 2, 3, 4, 10, 9, 8, 7, 6};
            int sum = 0;

            for (int i = 0; i < SEQUENCE_LENGTH; i++) {
                int digit = Character.getNumericValue(sequence.charAt(i));
                sum += weights[i] * digit;
            }

            // Get last digit of the sum modulo 10
            return sum % 10;
        }
    }

    /**
     * Generate transaction for payment with URA Payment Gateway and get payment URL
     *
     * @param request Request containing notices array and payment method
     * @return UraPaymentUrlResponse with response code and payment URL
     */
    public UraPaymentUrlResponse generateTransaction(GenerateTransactionRequest request) {
        log.info("Generating transaction for notices: {}", request.getNotices());

        try {
            // Generate transaction ID using the TransactionIdGenerator
            TransactionIdGenerator generator = new TransactionIdGenerator();
            String txnId = generator.generateTransactionId();
            String txnDate = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Date());

            // Process each notice
            List<TransactionDTO.TxnDetail> txnDetails = new ArrayList<>();
            double totalAmount = 0.0;
            String email = null;

            for (String noticeNo : request.getNotices()) {
                // Query offence notice data
                EocmsValidOffenceNotice offenceNotice = validOffenceNoticeRepository.findByNoticeNo(noticeNo);

                if (offenceNotice != null) {
                    // Create transaction detail
                    TransactionDTO.TxnDetail txnDetail = new TransactionDTO.TxnDetail();
                    txnDetail.setTxnId(txnId);
                    txnDetail.setProductId(offenceNotice.getNoticeNo());

                    // Set pricing details
                    double unitPrice = 0.0;
                    double unitGst = 0.0;

                    if (offenceNotice.getAmountPayable() != null) {
                        unitPrice = offenceNotice.getAmountPayable().doubleValue();
                        // Assume GST is included in amount payable
                        unitGst = 0.0;
                    }

                    txnDetail.setUnitPrice(unitPrice);
                    txnDetail.setUnitGst(unitGst);
                    txnDetail.setQuantity(1);
                    txnDetail.setTotalProductPrice(unitPrice + unitGst);

                    // Add to total amount
                    totalAmount += (unitPrice + unitGst);

                    // Add to transaction details
                    txnDetails.add(txnDetail);

                    // Try to get email if not found yet
                    if (email == null) {
                        try {
                            // Query owner driver data to get email
                            List<EocmsOffenceNoticeOwnerDriver> ownerDrivers = ownerDriverRepository.findByNoticeNo(noticeNo);

                            if (!ownerDrivers.isEmpty() && ownerDrivers.get(0).getEmailAddr() != null) {
                                email = ownerDrivers.get(0).getEmailAddr();
                            }
                        } catch (Exception e) {
                            log.error("Error getting email for notice {}: {}", noticeNo, e.getMessage());
                        }
                    }
                } else {
                    log.warn("Notice not found: {}", noticeNo);
                }
            }

            // Create transaction DTO as payload for URA API
            TransactionDTO transaction = new TransactionDTO();
            transaction.setTxnId(txnId);
            transaction.setTxnDate(txnDate);
            transaction.setPaymentMethod(request.getPaymentMethod());
            transaction.setTxnDetails(txnDetails);
            transaction.setAmountPayable(totalAmount);
            transaction.setEmail(email);

            // Send transaction data to URA API

            // Create headers with APIM subscription key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Add APIM subscription key for DEV environment
            headers.set(apimHeader, apimKey);

            // Create HTTP entity with transaction payload
            HttpEntity<TransactionDTO> entity = new HttpEntity<>(transaction, headers);

            // Call URA API to get payment URL
            log.info("Sending transaction data to URA API: {}", transaction);
            ResponseEntity<UraPaymentUrlResponse> response = restTemplate.exchange(
                    storeApiUrl,
                    HttpMethod.POST,
                    entity,
                    UraPaymentUrlResponse.class
            );

            UraPaymentUrlResponse paymentUrlResponse = response.getBody();
            log.info("Received payment URL response: {}", paymentUrlResponse);

            // Record transaction details in audit table //pindahkan ke txndetail
            try {
                EocmsWebTxnDetail txnSave = new EocmsWebTxnDetail();
                txnSave.setReceiptNo(txnId);
                //txnSave.set(request.getNotices().size());

                // Set current date and time
                LocalDateTime now = LocalDateTime.now();
                txnSave.setCreDate(now);
                txnSave.setCreUserId("System");

                // Set sender and receiver
                txnSave.setSender("eocms");
                txnSave.setStatus("I"); // Initial sending status
               // txnSave.set("urapg");

                // Store notice numbers as transaction detail
               // txnSave.set(String.join(",", request.getNotices()));

                webTxnDetailRepository.save(txnSave);
                log.info("Saved transaction audit record for txnId: {}", txnId);
            } catch (Exception e) {
                log.error("Error saving transaction audit record: {}", e.getMessage(), e);
                // Continue with the process even if audit record fails
            }

            return paymentUrlResponse;

        } catch (Exception e) {
            log.error("Error generating transaction and getting payment URL: {}", e.getMessage(), e);

            // Return error response
            UraPaymentUrlResponse errorResponse = new UraPaymentUrlResponse();
            errorResponse.setResponseCode("ERROR");
            errorResponse.setPaymentUrl(null);
            return errorResponse;
        }
    }

    /**
     * Generate receipt number for transaction
     */
    private String generateReceiptNo() {
        return "RCT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Initiate payment with URA Payment Gateway
     */
    public UraPaymentResponse initiatePayment(UraPaymentRequest request) {
        log.info("Initiating payment for notice: {}", request.getNoticeNo());

        try {
            // Generate receipt number if not provided
            String receiptNo = request.getTransactionRef();
            if (receiptNo == null || receiptNo.isEmpty()) {
                receiptNo = generateReceiptNo();
                request.setTransactionRef(receiptNo);
            }

            // Set callback URLs if not provided
            if (request.getSuccessUrl() == null || request.getSuccessUrl().isEmpty()) {
                request.setSuccessUrl(successUrl);
            }
            if (request.getFailureUrl() == null || request.getFailureUrl().isEmpty()) {
                request.setFailureUrl(failureUrl);
            }
            if (request.getCallbackUrl() == null || request.getCallbackUrl().isEmpty()) {
                request.setCallbackUrl(callbackUrl);
            }

            // Save transaction to database
            EocmsWebTxnDetail txnDetail = new EocmsWebTxnDetail();
            txnDetail.setReceiptNo(receiptNo);
            txnDetail.setOffenceNoticeNo(request.getNoticeNo());
            txnDetail.setVehicleNo(request.getVehicleNo());
            txnDetail.setTotalAmount(request.getAmountPayable().toString());
            txnDetail.setPaymentAmount(request.getAmountPayable().toString());
            txnDetail.setTransactionDateAndTime(LocalDateTime.now());
            txnDetail.setStatus("I"); // I = Initiated
            txnDetail.setSender(request.getUserId());
            txnDetail.setRemarks("URA Payment Gateway transaction initiated");
            txnDetail.setTypeOfReceipt("PF"); // Parking Fine

            webTxnDetailRepository.save(txnDetail);

            // Prepare HTTP headers with APIM
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(apimHeader, apimKey);

            // Call URA Payment Gateway API
            HttpEntity<UraPaymentRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<UraPaymentResponse> response = restTemplate.exchange(
                    paymentGatewayUrl + "/initiate",
                    HttpMethod.POST,
                    entity,
                    UraPaymentResponse.class
            );

            UraPaymentResponse paymentResponse = response.getBody();

            // Update transaction with URA transaction ID
            if (paymentResponse != null && paymentResponse.getTransactionId() != null) {
                txnDetail.setRemarks("URA Transaction ID: " + paymentResponse.getTransactionId());
                webTxnDetailRepository.save(txnDetail);

                // Set receipt number in response
                paymentResponse.setReceiptNo(receiptNo);
            }

            return paymentResponse;

        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);

            // Create error response
            UraPaymentResponse errorResponse = new UraPaymentResponse();
            errorResponse.setStatusCode("ERROR");
            errorResponse.setStatusMessage("Failed to initiate payment");
            errorResponse.setReceiptNo(request.getTransactionRef());
            errorResponse.setErrorCode("PAYMENT_INIT_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Check payment status
     */
    public UraPaymentResponse checkPaymentStatus(String receiptNo) {
        log.info("Checking payment status for receipt: {}", receiptNo);

        try {
            // Check status in local database
            webTxnDetailRepository.findById(receiptNo)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + receiptNo));

            // Prepare HTTP headers with APIM
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(apimHeader, apimKey);

            // Call URA Payment Gateway API
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<UraPaymentResponse> response = restTemplate.exchange(
                    paymentGatewayUrl + "/status/" + receiptNo,
                    HttpMethod.GET,
                    entity,
                    UraPaymentResponse.class
            );

            UraPaymentResponse paymentResponse = response.getBody();

            // Set receipt number in response
            if (paymentResponse != null) {
                paymentResponse.setReceiptNo(receiptNo);
            }

            return paymentResponse;

        } catch (Exception e) {
            log.error("Error checking payment status: {}", e.getMessage(), e);

            // Create error response
            UraPaymentResponse errorResponse = new UraPaymentResponse();
            errorResponse.setStatusCode("ERROR");
            errorResponse.setStatusMessage("Failed to check payment status");
            errorResponse.setReceiptNo(receiptNo);
            errorResponse.setErrorCode("STATUS_CHECK_ERROR");
            errorResponse.setErrorMessage(e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Handle callback from URA Payment Gateway
     */
    public void handleCallback(String receiptNo, String status, String transactionId) {
        log.info("Handling payment callback for receipt: {}, status: {}, transactionId: {}", receiptNo, status, transactionId);

        // Update EocmsWebTxnDetail
        //webTxnDetailRepository.findById(receiptNo)
         //       .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + receiptNo));
        EocmsWebTxnDetail txnDetail = (EocmsWebTxnDetail) webTxnDetailRepository.findById(receiptNo)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + receiptNo));

        // Update transaction status
        if ("SUCCESS".equals(status)) {
            txnDetail.setStatus("S"); // S = Success
        } else if ("FAILED".equals(status)) {
            txnDetail.setStatus("F"); // F = Failed
        } else {
            txnDetail.setStatus("P"); // P = Pending
        }

        txnDetail.setRemarks("URA Transaction ID: " + transactionId + ", Status: " + status);
        webTxnDetailRepository.save(txnDetail);

        // Update EocmsWebTxnAudit if transactionId exists
        if (transactionId != null && !transactionId.isEmpty()) {
            try {
                webTxnDetailRepository.findById(transactionId).ifPresent(txnDetail2 -> {
                    // Set current date and time
                    LocalDateTime now = LocalDateTime.now();
                    txnDetail2.setUpdDate(now);
                    txnDetail2.setUpdUserId("SYSTEM");

                    // Set sender and receiver (reversed from initial transaction)
                    //txnAudit.setSender("urapg");
                    //txnAudit.setTargetReceiver("eocms");

                    // Set status based on payment result
                    if ("SUCCESS".equals(status)) {
                        txnDetail2.setStatus("S"); // 2 = callback success
                        txnDetail2.setErrorRemarks("SUCCESS");
                    } else {
                        txnDetail2.setStatus("F"); // 1 = callback failed
                        txnDetail2.setErrorRemarks("FAILED: " + status);
                    }

                    webTxnDetailRepository.save(txnDetail2);
                    log.info("Updated transaction audit record for txnId: {}", transactionId);
                });
            } catch (Exception e) {
                log.error("Error updating transaction audit record: {}", e.getMessage(), e);
                // Continue with the process even if audit record update fails
            }
        }
    }
}
