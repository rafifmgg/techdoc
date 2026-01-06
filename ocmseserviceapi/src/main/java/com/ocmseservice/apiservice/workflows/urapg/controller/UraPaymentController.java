package com.ocmseservice.apiservice.workflows.urapg.controller;

import com.ocmseservice.apiservice.workflows.urapg.model.GenerateTransactionRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentResponse;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentUrlResponse;
import com.ocmseservice.apiservice.workflows.urapg.service.UraPaymentService;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAudit;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for URA Payment Gateway integration
 */
@RestController
@RequestMapping("/v1/")
@Slf4j
@RequiredArgsConstructor
public class UraPaymentController {

    private final UraPaymentService paymentService;
    private final EocmsWebTxnAuditRepository webTxnAuditRepository;

    /**
     * Generate transaction for payment with URA Payment Gateway
     */
    @PostMapping("/egeneratetransaction")
    public ResponseEntity<UraPaymentUrlResponse> generateTransaction(@RequestBody GenerateTransactionRequest request) {
        log.info("Menerima request generate transaction untuk notices: {}", request.getNotices());
        UraPaymentUrlResponse response = paymentService.generateTransaction(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Redirect ke URA Payment Gateway
     */
    @GetMapping("/redirect/{receiptNo}")
    public void redirectToPaymentGateway(@PathVariable String receiptNo, HttpServletResponse response) throws IOException {
        log.info("Redirect ke payment gateway untuk receipt: {}", receiptNo);
        UraPaymentResponse paymentResponse = paymentService.checkPaymentStatus(receiptNo);

        if (paymentResponse != null && paymentResponse.getRedirectUrl() != null) {
            response.sendRedirect(paymentResponse.getRedirectUrl());
        } else {
            // Redirect ke halaman error jika URL redirect tidak tersedia
            response.sendRedirect("/payment/error?ref=" + receiptNo);
        }
    }

    /**
     * Handle callback dari URA Payment Gateway
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody Map<String, String> payload) {
        log.info("Menerima callback dari payment gateway: {}", payload);

        String receiptNo = payload.get("receiptNo");
        String status = payload.get("status");
        String transactionId = payload.get("transactionId");

        if (receiptNo == null || status == null) {
            return new ResponseEntity<>("Parameter yang diperlukan tidak ada", HttpStatus.BAD_REQUEST);
        }

        try {
            paymentService.handleCallback(receiptNo, status, transactionId);
            log.info("Callback berhasil diproses untuk receipt: {}", receiptNo);
            return new ResponseEntity<>("Callback berhasil diproses", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error memproses callback: {}", e.getMessage(), e);
            return new ResponseEntity<>("Error memproses callback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Cek status pembayaran berdasarkan receipt number
     */
    @GetMapping("/status/{receiptNo}")
    public ResponseEntity<UraPaymentResponse> checkPaymentStatus(@PathVariable String receiptNo) {
        log.info("Mengecek status pembayaran untuk receipt: {}", receiptNo);
        UraPaymentResponse response = paymentService.checkPaymentStatus(receiptNo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Cek status pembayaran berdasarkan transaction ID
     * Endpoint ini digunakan untuk mengecek status pembayaran setelah redirect dari payment gateway
     */
    @GetMapping("/payment/status")
    public ResponseEntity<?> checkPaymentStatusByTxnId(@RequestParam("txnId") String txnId) {
        log.info("Mengecek status pembayaran untuk transaction ID: {}", txnId);

        // Cari data transaksi berdasarkan transaction ID
        EocmsWebTxnAudit txnAudit = webTxnAuditRepository.findById(txnId).orElse(null);

        if (txnAudit == null) {
            return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Transaction ID tidak ditemukan"
            ), HttpStatus.NOT_FOUND);
        }

        // Ambil notice numbers dari txn_detail
        String[] noticeNumbers = txnAudit.getTxnDetail().split(",");

        // Buat response
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", txnId);
        response.put("noticeNumbers", Arrays.asList(noticeNumbers));
        response.put("timestamp", new Date());

        // Cek status pembayaran di URA Payment Gateway
        try {
            // Gunakan receipt number pertama untuk cek status
            // Ini hanya contoh, Anda mungkin perlu menyesuaikan logika ini
            UraPaymentResponse paymentResponse = paymentService.checkPaymentStatus(txnId);
            response.put("status", paymentResponse.getStatusCode());
            response.put("statusMessage", paymentResponse.getStatusMessage());
        } catch (Exception e) {
            log.error("Error checking payment status: {}", e.getMessage(), e);
            response.put("status", "UNKNOWN");
            response.put("statusMessage", "Tidak dapat mengecek status pembayaran");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
