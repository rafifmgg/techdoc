# URA Payment Gateway Integration

## Alur Proses Pembayaran

Integrasi dengan URA Payment Gateway mengikuti alur berikut:

1. **Pemilihan Metode Pembayaran**
   - User memilih metode pembayaran (eNETS atau PayNow)

2. **Alur eNETS**
   - Menampilkan halaman pembayaran eNETS
   - User memilih eNETS credit
   - User memverifikasi jumlah pembayaran
   - User memasukkan detail kartu kredit
   - User mereview pembayaran
   - User mengirimkan pembayaran

3. **Alur PayNow**
   - Menampilkan halaman konfirmasi
   - User memverifikasi ID transaksi dan jumlah
   - User memindai QR code PayNow
   - User mengkonfirmasi pembayaran di aplikasi mobile

4. **Callback dan Penyelesaian**
   - URA Payment Gateway mengirimkan callback ke URL backend OCMS
   - Sistem memperbarui status transaksi di database

## Implementasi

### 1. Model Request

```java
package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UraPaymentRequest {
   private String noticeNo;
   private String vehicleNo;
   private BigDecimal amountPayable;
   private String ppCode;
   private String userId;
   private String receiptNo;
   private String successUrl;
   private String failureUrl;
   private String callbackUrl;
}
```

### 2. Model Response

```java
package com.ocmseservice.apiservice.workflows.urapg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UraPaymentResponse {
   private String statusCode;
   private String statusMessage;
   private String transactionId;
   private String receiptNo;
   private String redirectUrl;
   private String errorCode;
   private String errorMessage;
}
```

### 3. Service untuk Integrasi

```java
package com.ocmseservice.apiservice.workflows.urapg.service;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetailRepository;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UraPaymentService {

   private final EocmsWebTxnDetailRepository webTxnDetailRepository;
   private final RestTemplate restTemplate;

   @Value("${ura.payment.gateway.url}")
   private String paymentGatewayUrl;

   @Value("${ura.payment.gateway.apim.header}")
   private String apimHeader;

   @Value("${ura.payment.gateway.apim.key}")
   private String apimKey;

   @Value("${ura.payment.success.url}")
   private String successUrl;

   @Value("${ura.payment.failure.url}")
   private String failureUrl;

   @Value("${ura.payment.callback.url}")
   private String callbackUrl;

   /**
    * Inisiasi pembayaran dengan URA Payment Gateway
    */
   public UraPaymentResponse initiatePayment(UraPaymentRequest request) {
      log.info("Inisiasi pembayaran untuk notice: {}", request.getNoticeNo());

      try {
         // Generate receipt number jika belum ada
         if (request.getReceiptNo() == null || request.getReceiptNo().isEmpty()) {
            request.setReceiptNo(generateReceiptNo());
         }

         // Set URL callback jika belum ada
         if (request.getSuccessUrl() == null || request.getSuccessUrl().isEmpty()) {
            request.setSuccessUrl(successUrl);
         }
         if (request.getFailureUrl() == null || request.getFailureUrl().isEmpty()) {
            request.setFailureUrl(failureUrl);
         }
         if (request.getCallbackUrl() == null || request.getCallbackUrl().isEmpty()) {
            request.setCallbackUrl(callbackUrl);
         }

         // Simpan transaksi ke database
         EocmsWebTxnDetail txnDetail = new EocmsWebTxnDetail();
         txnDetail.setReceiptNo(request.getReceiptNo());
         txnDetail.setOffenceNoticeNo(request.getNoticeNo());
         txnDetail.setVehicleNo(request.getVehicleNo());
         txnDetail.setTotalAmount(request.getAmountPayable().toString());
         txnDetail.setPaymentAmount(request.getAmountPayable().toString());
         txnDetail.setTransactionDateAndTime(LocalDateTime.now());
         txnDetail.setStatus("I"); // I = Initiated
         txnDetail.setSender(request.getUserId());

         webTxnDetailRepository.save(txnDetail);

         // Siapkan HTTP headers dengan APIM
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         headers.set(apimHeader, apimKey);

         // Panggil API URA Payment Gateway
         HttpEntity<UraPaymentRequest> entity = new HttpEntity<>(request, headers);
         ResponseEntity<UraPaymentResponse> response = restTemplate.exchange(
                 paymentGatewayUrl + "/initiate",
                 HttpMethod.POST,
                 entity,
                 UraPaymentResponse.class
         );

         UraPaymentResponse paymentResponse = response.getBody();

         // Update transaksi dengan ID transaksi URA
         if (paymentResponse != null && paymentResponse.getTransactionId() != null) {
            txnDetail.setRemarks("URA Transaction ID: " + paymentResponse.getTransactionId());
            webTxnDetailRepository.save(txnDetail);
         }

         return paymentResponse;

      } catch (Exception e) {
         log.error("Error inisiasi pembayaran: {}", e.getMessage(), e);

         // Buat response error
         return UraPaymentResponse.builder()
                 .statusCode("ERROR")
                 .statusMessage("Gagal inisiasi pembayaran")
                 .receiptNo(request.getReceiptNo())
                 .errorCode("PAYMENT_INIT_ERROR")
                 .errorMessage(e.getMessage())
                 .build();
      }
   }

   /**
    * Tangani callback dari URA Payment Gateway
    */
   public void handleCallback(String receiptNo, String status, String transactionId) {
      log.info("Menangani callback pembayaran untuk receipt: {}, status: {}", receiptNo, status);

      EocmsWebTxnDetail txnDetail = webTxnDetailRepository.findById(receiptNo)
              .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan: " + receiptNo));

      // Update status transaksi
      if ("SUCCESS".equals(status)) {
         txnDetail.setStatus("S"); // S = Success
      } else if ("FAILED".equals(status)) {
         txnDetail.setStatus("F"); // F = Failed
      } else {
         txnDetail.setStatus("P"); // P = Pending
      }

      txnDetail.setRemarks("URA Transaction ID: " + transactionId + ", Status: " + status);
      webTxnDetailRepository.save(txnDetail);
   }

   /**
    * Cek status pembayaran
    */
   public UraPaymentResponse checkPaymentStatus(String receiptNo) {
      log.info("Mengecek status pembayaran untuk receipt: {}", receiptNo);

      try {
         // Siapkan HTTP headers dengan APIM
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         headers.set(apimHeader, apimKey);

         // Panggil API URA Payment Gateway
         HttpEntity<?> entity = new HttpEntity<>(headers);
         ResponseEntity<UraPaymentResponse> response = restTemplate.exchange(
                 paymentGatewayUrl + "/status/" + receiptNo,
                 HttpMethod.GET,
                 entity,
                 UraPaymentResponse.class
         );

         return response.getBody();

      } catch (Exception e) {
         log.error("Error mengecek status pembayaran: {}", e.getMessage(), e);

         // Buat response error
         return UraPaymentResponse.builder()
                 .statusCode("ERROR")
                 .statusMessage("Gagal mengecek status pembayaran")
                 .receiptNo(receiptNo)
                 .errorCode("PAYMENT_STATUS_ERROR")
                 .errorMessage(e.getMessage())
                 .build();
      }
   }

   /**
    * Generate nomor receipt unik
    */
   private String generateReceiptNo() {
      return "URA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
   }
}
```

### 4. Controller untuk API

```java
package com.ocmseservice.apiservice.workflows.urapg.controller;

import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentRequest;
import com.ocmseservice.apiservice.workflows.urapg.model.UraPaymentResponse;
import com.ocmseservice.apiservice.workflows.urapg.service.UraPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment/ura")
@Slf4j
@RequiredArgsConstructor
public class UraPaymentController {

   private final UraPaymentService paymentService;

   /**
    * Inisiasi pembayaran dengan URA Payment Gateway
    */
   @PostMapping("/initiate")
   public ResponseEntity<UraPaymentResponse> initiatePayment(@RequestBody UraPaymentRequest request) {
      log.info("Menerima request inisiasi pembayaran untuk notice: {}", request.getNoticeNo());
      UraPaymentResponse response = paymentService.initiatePayment(request);
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
    * Cek status pembayaran
    */
   @GetMapping("/status/{receiptNo}")
   public ResponseEntity<UraPaymentResponse> checkPaymentStatus(@PathVariable String receiptNo) {
      log.info("Mengecek status pembayaran untuk receipt: {}", receiptNo);
      UraPaymentResponse response = paymentService.checkPaymentStatus(receiptNo);
      return new ResponseEntity<>(response, HttpStatus.OK);
   }
}
```

### 5. Konfigurasi di application.properties

```properties
# URA Payment Gateway Configuration
ura.payment.gateway.url=https://api.ura.gov.sg/payment/v1
ura.payment.success.url=https://ocmseservice.com/payment/success
ura.payment.failure.url=https://ocmseservice.com/payment/failure
ura.payment.callback.url=https://ocmseservice.com/api/v1/payment/ura/callback

# APIM Configuration
ura.payment.gateway.apim.header=Ocp-Apim-Subscription-Key
ura.payment.gateway.apim.key=${URA_PAYMENT_APIM_KEY:your-apim-key-here}
```

## Perbedaan dengan Implementasi Sebelumnya

1. **Penyimpanan Data**
   - Menggunakan `EocmsWebTxnDetail` yang sudah ada untuk menyimpan transaksi
   - Tidak perlu membuat tabel baru (`UraPaymentTransaction`)

2. **Konfigurasi**
   - Menghilangkan `UraPaymentGatewayConfig` yang tidak diperlukan
   - Menggunakan `RestTemplate` yang sudah ada

3. **Alur Proses**
   - Mengikuti diagram alur yang diberikan
   - Mendukung dua metode pembayaran: eNETS dan PayNow

## Petunjuk Implementasi

1. Buat model request dan response
2. Buat service untuk integrasi dengan URA Payment Gateway
3. Buat controller untuk API
4. Tambahkan konfigurasi di application.properties
5. Pastikan `EocmsWebTxnDetail` sudah ada dan siap digunakan

## Konfigurasi

Konfigurasi URA Payment Gateway disimpan dalam file properties sesuai dengan lingkungan (environment) yang digunakan:

### application-local.properties (Pengembangan Lokal)

```properties
# URA Payment Gateway Configuration
# API URLs
ura.payment.gateway.url=http://localhost:9000/ura/payment/v1
ura.payment.success.url=http://localhost:8081/payment/success
ura.payment.failure.url=http://localhost:8081/payment/failure
ura.payment.callback.url=http://localhost:8085/ocms/api/v1/payment/ura/callback

# APIM Configuration
ura.payment.gateway.apim.header=Ocp-Apim-Subscription-Key
ura.payment.gateway.apim.key=local-development-key

# Transaction Configuration
ura.payment.transaction.timeout=300
```

### application-sit.properties (SIT Environment)

```properties
# URA Payment Gateway Configuration
# API URLs
ura.payment.gateway.url=https://api-sit.ura.gov.sg/payment/v1
ura.payment.success.url=https://ocmseservice-sit.com/payment/success
ura.payment.failure.url=https://ocmseservice-sit.com/payment/failure
ura.payment.callback.url=https://ocmseservice-sit.com/api/v1/payment/ura/callback

# APIM Configuration
ura.payment.gateway.apim.header=Ocp-Apim-Subscription-Key
ura.payment.gateway.apim.key=${URA_PAYMENT_APIM_KEY:your-sit-apim-key-here}

# Transaction Configuration
ura.payment.transaction.timeout=300
```

## RestTemplate Configuration

RestTemplate untuk API calls dikonfigurasi dalam kelas `UraPaymentGatewayConfig`:

```java
package com.ocmseservice.apiservice.workflows.urapg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UraPaymentGatewayConfig {

   @Bean
   public RestTemplate uraPaymentRestTemplate() {
      RestTemplate restTemplate = new RestTemplate();
      return restTemplate;
   }
}
```

## Catatan Penting

- Pastikan konfigurasi URL dan APIM key sudah benar di file properties
- Pastikan URL callback dapat diakses dari internet
- Pastikan status transaksi diperbarui dengan benar setelah menerima callback
- Transaksi pembayaran disimpan dalam entitas `EocmsWebTxnDetail` yang sudah ada
- Nomor receipt (receiptNo) digunakan sebagai ID transaksi untuk tracking
