# SingPass/CorpPass Authentication Flow

Implementasi alur autentikasi menggunakan SingPass/CorpPass untuk OCMSE Service API.

## Alur Autentikasi

### 1. Inisiasi Autentikasi
- User memulai proses login
- Sistem membuat application transaction ID (AppTxnId)

### 2. Autentikasi dengan SingPass/CorpPass
- User diarahkan ke portal SingPass/CorpPass
- Setelah autentikasi berhasil, sistem menerima respons dari SPCP

### 3. Pembuatan Token
- Sistem memvalidasi respons SPCP
- Jika valid, sistem membuat token JWT untuk autentikasi
- Token disimpan dalam sesi login pengguna

## Struktur Kode

### Model/DTO
- `AuthAppTxnIdRequest`: Request untuk membuat application transaction ID
- `AuthAppTxnIdResponse`: Response yang berisi application transaction ID
- `SpcpAuthRequest`: Request untuk autentikasi SingPass/CorpPass
- `SpcpAuthResponse`: Response dari autentikasi SingPass/CorpPass
- `LoginInfo`: Informasi login termasuk token JWT

### Entity
- `LoginSession`: Entity untuk menyimpan informasi sesi login

### Repository
- `LoginSessionRepository`: Repository untuk entity LoginSession

### Service
- `SpcpAuthService`: Service untuk autentikasi SingPass/CorpPass

### Utility
- `JwtTokenUtil`: Utility untuk operasi token JWT

### Controller
- `SpcpAuthController`: Controller untuk endpoint autentikasi

## Endpoint API

### 1. Inisiasi Autentikasi
```
POST /api/v1/auth/initiate
```

### 2. Penyelesaian Autentikasi
```
POST /api/v1/auth/complete/{authType}
```
Dimana `authType` adalah "SP" untuk SingPass atau "CP" untuk CorpPass.

## Konfigurasi

Konfigurasi yang diperlukan dalam `application.properties` atau `application.yml`:

```properties
# SPCP Configuration
spcp.service.url=http://localhost:8080
spcp.api.key=your-api-key

# JWT Configuration
jwt.secret.key=your-secret-key
jwt.token.expiration=3600
jwt.refresh.token.expiration=86400
```
