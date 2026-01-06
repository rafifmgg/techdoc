# SingPass/CorpPass Authentication Integration

## Deskripsi

Modul ini menyediakan integrasi dengan SingPass dan CorpPass untuk autentikasi pengguna dalam aplikasi OCMSE. SingPass digunakan untuk autentikasi warga negara, sedangkan CorpPass digunakan untuk autentikasi entitas bisnis.

## Alur Proses Autentikasi

### 1. Inisiasi Autentikasi
- Pengguna memulai proses login dari frontend
- Frontend memanggil endpoint `/spcpDS/spcp/v1/createAppTxnId` untuk mendapatkan ID transaksi
- Backend meneruskan permintaan ke endpoint SIT: `https://singpasscorppass.azurewebsites.net/spcpDS/spcp/createAppTxnId`
- Backend menerima application transaction ID (AppTxnId) dari layanan SIT dan mengembalikannya ke frontend
- Frontend mengarahkan pengguna ke portal SingPass/CorpPass dengan AppTxnId

### 2. Autentikasi dengan SingPass/CorpPass
- Pengguna melakukan login di portal SingPass/CorpPass
- Setelah autentikasi berhasil, SingPass/CorpPass mengarahkan pengguna kembali ke aplikasi dengan authTxnId
- Frontend menerima redirect dan memanggil endpoint `/spcpDS/spcp/v1/getAuthResponse` dengan appId dan authTxnId
- Backend meneruskan permintaan ke endpoint SIT: `https://singpasscorppass.azurewebsites.net/spcpDS/spcp/getAuthResponse`
- Backend menerima respons autentikasi dari layanan SIT dan mengembalikannya ke frontend

### 3. Pengambilan Data MyInfo (Opsional)
- Setelah autentikasi berhasil, frontend dapat memanggil endpoint `/spcpDS/myinfo/getMyInfoData` dengan appId, nric, dan txnNo
- Backend meneruskan permintaan ke endpoint SIT: `https://singpasscorppass.azurewebsites.net/spcpDS/myinfo/getMyInfoData`
- Backend menerima data MyInfo dari layanan SIT dan mengembalikannya ke frontend
- Frontend menampilkan data MyInfo kepada pengguna

## Komponen Utama

### 1. Model/DTO

```java
// Request untuk membuat application transaction ID
public class AuthAppTxnIdRequest {
    private String sessionId;
    private String appId;
}

// Response yang berisi application transaction ID
public class AuthAppTxnIdResponse {
    private String responseCode;
    private String responseMsg;
    private String appTxnId;
}

// Request untuk autentikasi SingPass/CorpPass
public class SpcpAuthRequest {
    private String appId;
    private String authTxnId;
}

// Response dari autentikasi SingPass/CorpPass
public class SpcpAuthResponse {
    private String responseCode;
    private String responseMsg;
    private String nric;
    private String entityId;
    private String entityStatus;
    private String entityType;
    private String userName;
}

// Request untuk mendapatkan data MyInfo
public class MyInfoRequest {
    private String appId;
    private String nric;
    private String txnNo;
}

// Response yang berisi data MyInfo
public class MyInfoResponse {
    private MobileNo mobileNo;
    private RegisteredAddress registeredAddress;
    private List<Vehicle> vehicles;
    private String responseCode;
    private String responseMsg;
    private String name;
    private String email;
    
    // Inner class untuk nomor telepon
    public static class MobileNo {
        private String prefix;
        private String countryCode;
        private String number;
    }
    
    // Inner class untuk alamat terdaftar
    public static class RegisteredAddress {
        private String block;
        private String building;
        private String floor;
        private String unitNo;
        private String street;
        private String postalCode;
        private String country;
        private String countryCode;
    }
    
    // Inner class untuk informasi kendaraan
    public static class Vehicle {
        private String vehicleNumber;
        private String chassisNumber;
        private String statusCode;
        private String status;
        // ... dan properti lainnya
    }
}
```

### 2. Service

```java
@Service
public class SpcpAuthService {
    @Value("${spcp.service.url:https://singpasscorppass.azurewebsites.net}")
    private String spcpBaseUrl;
    
    private final RestTemplate restTemplate;
    
    // Membuat application transaction ID dengan memanggil layanan SIT
    public AuthAppTxnIdResponse createAppTxnId(AuthAppTxnIdRequest request) {
        // Memanggil endpoint SIT: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/createAppTxnId
        // dengan payload JSON: {"sessionId": "...", "appId": "..."}
    }
    
    // Mendapatkan respons autentikasi dari layanan SIT
    public SpcpAuthResponse getAuthResponse(SpcpAuthRequest request) {
        // Memanggil endpoint SIT: https://singpasscorppass.azurewebsites.net/spcpDS/spcp/getAuthResponse
        // dengan payload JSON: {"appId": "...", "authTxnId": "..."}
    }
    
    // Mendapatkan data MyInfo dari layanan SIT
    public MyInfoResponse getMyInfoData(MyInfoRequest request) {
        // Memanggil endpoint SIT: https://singpasscorppass.azurewebsites.net/spcpDS/myinfo/getMyInfoData
        // dengan payload JSON: {"appId": "...", "nric": "...", "txnNo": "..."}
    }
}
```

### 3. Controller

```java
@RestController
public class SpcpAuthController {
    // Endpoint untuk membuat application transaction ID
    @PostMapping("/spcpDS/spcp/v1/createAppTxnId")
    public ResponseEntity<AuthAppTxnIdResponse> createAppTxnId(@RequestBody AuthAppTxnIdRequest request) {
        // Meneruskan permintaan ke layanan SIT dan mengembalikan respons
    }
    
    // Endpoint untuk mendapatkan respons autentikasi
    @PostMapping("/spcpDS/spcp/v1/getAuthResponse")
    public ResponseEntity<SpcpAuthResponse> getAuthResponse(@RequestBody SpcpAuthRequest request) {
        // Meneruskan permintaan ke layanan SIT dan mengembalikan respons
    }
    
    // Endpoint untuk mendapatkan data MyInfo
    @PostMapping("/spcpDS/myinfo/getMyInfoData")
    public ResponseEntity<MyInfoResponse> getMyInfoData(@RequestBody MyInfoRequest request) {
        // Meneruskan permintaan ke layanan SIT dan mengembalikan respons
    }
}
```

### 4. Utility

```java
// Utility untuk cache aplikasi
public class AppCacheUtil {
    // Konstanta untuk header APIM
    public static final String APIM_HEADER = "Ocp-Apim-Subscription-Key";
    
    // Konstanta untuk key APIM
    public static final String APIM_SPMS_KEY = "spms-spcpds-apim-subscription";
    
    // Konstanta untuk parameter appId
    public static final String SPMS_GCC_APPID_KEY = "appId";
    
    // Konstanta untuk parameter sessionId
    public static final String SPMS_GCC_SESSIONID_KEY = "sessionId";
    
    // Konstanta untuk parameter authTxnId
    public static final String SPMS_GCC_AUTH_TXN_ID_KEY = "authTxnId";
    
    // Menyimpan data dalam cache
    public static void put(String key, String value) {
        // Implementasi penyimpanan cache
    }
    
    // Mengambil data dari cache
    public static String getValue(String key) {
        // Implementasi pengambilan cache
    }
}
```

## Konfigurasi

Konfigurasi SingPass/CorpPass disimpan dalam file properties sesuai dengan lingkungan (environment) yang digunakan:

```properties
# URL layanan SPCP SIT
spcp.service.url=https://singpasscorppass.azurewebsites.net

# API key untuk layanan SPCP
spcp.api.key=${SPCP_API_KEY:default-api-key}
```

## Endpoint API

### 1. Membuat Application Transaction ID

**Request:**
```
POST /spcpDS/spcp/v1/createAppTxnId
Content-Type: application/json

{
  "sessionId": "6604c9a9-6f0f-432c-8000-000000000000",
  "appId": "SP833057844600"
}
```

**Response:**
```
{
  "responseCode": "SP200",
  "responseMsg": "Success",
  "appTxnId": "SP110125678452"
}
```

### 2. Mendapatkan Respons Autentikasi

**Request:**
```
POST /spcpDS/spcp/v1/getAuthResponse
Content-Type: application/json

{
  "appId": "SP833057844600",
  "authTxnId": "SP110125678452"
}
```

**Response:**
```
{
  "responseCode": "SP200",
  "responseMsg": "Success",
  "nric": "S1234567A",
  "entityId": null,
  "entityStatus": null,
  "entityType": null,
  "userName": "John Doe"
}
```

### 3. Mendapatkan Data MyInfo

**Request:**
```
POST /spcpDS/myinfo/getMyInfoData
Content-Type: application/json

{
  "appId": "SP833057844600",
  "nric": "S8243452F",
  "txnNo": "SP608065503247"
}
```

**Response:**
```
{
  "mobileNo": {
    "prefix": "+",
    "countryCode": "65",
    "number": "84517544"
  },
  "registeredAddress": {
    "block": "153",
    "building": "",
    "floor": "9",
    "unitNo": "167",
    "street": "",
    "postalCode": "570153",
    "country": "",
    "countryCode": "SG"
  },
  "vehicles": [
    {
      "vehicleNumber": "FBQ30A",
      "chassisNumber": "WIN1499842M496820",
      "statusCode": "1",
      "status": "REGISTERED",
      "type": "Motorcycle",
      "make": "ROVER",
      "model": "VEZEL 1.5X CVT"
      // ... properti lainnya
    }
  ],
  "responseCode": "MI200",
  "responseMsg": "Success",
  "name": "Lee Shu Kwan Elicia",
  "email": "mgguratestmail@gmail.com"
}
```

## Catatan Penting

1. Pastikan konfigurasi URL dan API key sudah benar di file properties
2. Pastikan APIM subscription key (`Ocp-Apim-Subscription-Key`) tersedia di cache dengan key `spms-spcpds-apim-subscription`
3. Validasi semua input dari pengguna untuk mencegah serangan keamanan
4. Gunakan HTTPS untuk semua komunikasi dengan layanan SingPass/CorpPass
5. Pastikan header Content-Type diatur ke `application/json` untuk semua permintaan
6. Pastikan error handling yang tepat untuk menangani kegagalan koneksi atau respons error dari layanan SIT

## Pengujian

1. Uji endpoint `/spcpDS/spcp/v1/createAppTxnId` dengan cURL:
   ```
   curl -X POST http://localhost:8080/spcpDS/spcp/v1/createAppTxnId \
     -H "Content-Type: application/json" \
     -d '{"sessionId":"6604c9a9-6f0f-432c-8000-000000000000","appId":"SP833057844600"}'
   ```

2. Uji endpoint `/spcpDS/spcp/v1/getAuthResponse` dengan cURL:
   ```
   curl -X POST http://localhost:8080/spcpDS/spcp/v1/getAuthResponse \
     -H "Content-Type: application/json" \
     -d '{"appId":"SP833057844600","authTxnId":"SP110125678452"}'
   ```

3. Uji endpoint `/spcpDS/myinfo/getMyInfoData` dengan cURL:
   ```
   curl -X POST http://localhost:8080/spcpDS/myinfo/getMyInfoData \
     -H "Content-Type: application/json" \
     -d '{"appId":"SP833057844600","nric":"S8243452F","txnNo":"SP608065503247"}'
   ```

4. Verifikasi bahwa respons dari ketiga endpoint sesuai dengan format yang diharapkan
5. Uji skenario error handling (APIM key tidak valid, timeout, parameter tidak lengkap, dll)
6. Pastikan log yang memadai untuk debugging dan pemantauan
