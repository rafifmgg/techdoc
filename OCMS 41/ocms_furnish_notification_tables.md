# OCMS 41 - Furnished Notification Tables

## Overview

Dokumen ini menjelaskan struktur dan kegunaan dari dua tabel baru yang diusulkan untuk OCMS 41:
- `ocms_furnish_sms` - Menyimpan record SMS yang dikirim ke Hirer/Driver selama proses manual review
- `ocms_furnish_email` - Menyimpan record Email yang dikirim ke Hirer/Driver selama proses manual review

Kedua tabel ini digunakan untuk:
1. Melacak semua komunikasi (SMS/Email) yang dikirim ke submitter furnished application
2. Menyimpan status pengiriman (berhasil/gagal)
3. Menyimpan error message jika pengiriman gagal untuk keperluan troubleshooting
4. Audit trail untuk setiap notifikasi yang dikirim oleh OIC

---

## Table 1: ocms_furnish_sms

### Description
Tabel ini menyimpan record SMS yang dikirim kepada Hirer atau Driver selama proses manual review furnished submission di OCMS 41. Setiap record merepresentasikan satu SMS yang dikirim melalui Postman SMS service.

### Business Context
Ketika OIC melakukan review terhadap furnished submission dan perlu menghubungi submitter via SMS (misalnya untuk meminta dokumen tambahan atau menginformasikan status), sistem akan mencatat setiap SMS yang dikirim beserta statusnya.

### Fields

| Field Name | Data Type | Nullable | Description |
|------------|-----------|----------|-------------|
| `txn_no` | varchar(20) | No | **Primary Key**. Transaction number dari furnished submission record. Foreign key ke `ocms_furnish_application`. Mengidentifikasi furnished application mana yang terkait dengan SMS ini. |
| `notice_no` | varchar(10) | No | Nomor Notice yang terkait dengan furnished submission. Digunakan untuk referensi cepat ke Notice tanpa harus join ke tabel furnish_application. |
| `content` | varchar(4000) | No | Isi pesan SMS yang dikirim. Berisi teks lengkap yang diterima oleh penerima. Maksimal 4000 karakter untuk mengakomodasi SMS panjang atau multiple segments. |
| `date_sent` | datetime2(7) | No | Tanggal dan waktu ketika SMS dikirim ke Postman SMS service. Timestamp ini digunakan untuk audit dan tracking kapan komunikasi dilakukan. |
| `mobile_code` | varchar(4) | Yes | Kode negara dari nomor telepon penerima (contoh: +65 untuk Singapore). Nullable karena mungkin tidak selalu tersedia atau diperlukan. |
| `mobile_no` | varchar(12) | No | Nomor telepon penerima tanpa kode negara. Format: angka saja tanpa spasi atau karakter khusus. |
| `msg_error` | varchar(250) | Yes | Pesan error yang dikembalikan oleh Postman SMS service jika pengiriman gagal. Null jika pengiriman berhasil. Digunakan untuk troubleshooting dan support. |
| `status` | varchar(1) | No | Status pengiriman SMS. Values: **'S'** = Sent (berhasil dikirim), **'E'** = Error (gagal dikirim). Status ini menentukan apakah SMS berhasil terkirim ke gateway. |
| `cre_date` | datetime2(7) | No | Tanggal dan waktu ketika record ini dibuat di database. Biasanya sama dengan `date_sent` kecuali ada delay dalam penyimpanan. |
| `cre_user_id` | varchar(50) | No | User ID dari OIC yang mengirim SMS. Diambil dari session login OIC. Digunakan untuk audit trail siapa yang mengirim komunikasi. |
| `upd_date` | datetime2(7) | Yes | Tanggal dan waktu ketika record terakhir diupdate. Null pada saat record pertama kali dibuat. Diupdate jika ada retry atau koreksi status. |
| `upd_user_id` | varchar(50) | Yes | User ID dari user yang terakhir mengupdate record. Null pada saat record pertama kali dibuat. |

### Usage Flow
1. OIC membuka halaman "Compose Email/SMS" dari furnished submission detail
2. OIC memasukkan nomor telepon dan isi pesan SMS
3. OIC klik "Send SMS"
4. Backend mengirim SMS via Postman SMS service
5. Backend menyimpan record ke `ocms_furnish_sms` dengan status 'S' (success) atau 'E' (error)
6. Jika gagal, sistem akan retry hingga 3x sebelum menyimpan dengan status 'E'

---

## Table 2: ocms_furnish_email

### Description
Tabel ini menyimpan record Email yang dikirim kepada Hirer atau Driver selama proses manual review furnished submission di OCMS 41. Setiap record merepresentasikan satu email yang dikirim melalui URA SMTP server.

### Business Context
Ketika OIC melakukan review terhadap furnished submission dan perlu menghubungi submitter via Email (misalnya untuk meminta dokumen tambahan, menginformasikan status approval/rejection, atau follow-up lainnya), sistem akan mencatat setiap email yang dikirim beserta statusnya.

### Fields

| Field Name | Data Type | Nullable | Description |
|------------|-----------|----------|-------------|
| `txn_no` | varchar(20) | No | **Primary Key**. Transaction number dari furnished submission record. Foreign key ke `ocms_furnish_application`. Mengidentifikasi furnished application mana yang terkait dengan email ini. |
| `notice_no` | varchar(10) | No | Nomor Notice yang terkait dengan furnished submission. Digunakan untuk referensi cepat ke Notice tanpa harus join ke tabel furnish_application. |
| `furnish_email_subject` | varchar(255) | No | Subject line dari email yang dikirim. Berisi judul email yang ditampilkan kepada penerima. OIC dapat customize subject sesuai kebutuhan komunikasi. |
| `content` | varchar(4000) | No | Body content dari email yang dikirim. Berisi isi lengkap email dalam format plain text atau HTML. Maksimal 4000 karakter. |
| `date_sent` | datetime2(7) | No | Tanggal dan waktu ketika email dikirim ke SMTP server. Timestamp ini digunakan untuk audit dan tracking kapan komunikasi dilakukan. |
| `email_addr` | varchar(320) | No | Alamat email utama penerima (field "To"). Format standar email address. Maksimal 320 karakter sesuai RFC 5321. |
| `email_cc_addr` | varchar(320) | Yes | Alamat email CC (Carbon Copy). Nullable karena tidak selalu diperlukan. Dapat berisi satu atau multiple email addresses yang dipisahkan koma. |
| `msg_error` | varchar(250) | Yes | Pesan error yang dikembalikan oleh SMTP server jika pengiriman gagal. Null jika pengiriman berhasil. Digunakan untuk troubleshooting dan support. |
| `status` | varchar(1) | No | Status pengiriman email. Values: **'S'** = Sent (berhasil dikirim ke SMTP), **'E'** = Error (gagal dikirim). Status ini menentukan apakah email berhasil diterima oleh SMTP server. |
| `cre_date` | datetime2(7) | No | Tanggal dan waktu ketika record ini dibuat di database. Biasanya sama dengan `date_sent` kecuali ada delay dalam penyimpanan. |
| `cre_user_id` | varchar(50) | No | User ID dari OIC yang mengirim email. Diambil dari session login OIC. Digunakan untuk audit trail siapa yang mengirim komunikasi. |
| `upd_date` | datetime2(7) | Yes | Tanggal dan waktu ketika record terakhir diupdate. Null pada saat record pertama kali dibuat. Diupdate jika ada retry atau koreksi status. |
| `upd_user_id` | varchar(50) | Yes | User ID dari user yang terakhir mengupdate record. Null pada saat record pertama kali dibuat. |

### Usage Flow
1. OIC membuka halaman "Compose Email/SMS" dari furnished submission detail
2. OIC memasukkan email address, subject, dan isi email
3. OIC klik "Send Email"
4. Backend mengirim email via URA SMTP host
5. Backend menyimpan record ke `ocms_furnish_email` dengan status 'S' (success) atau 'E' (error)
6. Jika gagal, sistem akan retry hingga 3x sebelum menyimpan dengan status 'E'

---

## Relationship Diagram

```
┌─────────────────────────────┐
│  ocms_furnish_application   │
│  (existing table)           │
├─────────────────────────────┤
│  txn_no (PK)                │◄──────┐
│  notice_no                  │       │
│  status                     │       │
│  ...                        │       │
└─────────────────────────────┘       │
                                      │ FK
┌─────────────────────────────┐       │
│     ocms_furnish_sms        │       │
├─────────────────────────────┤       │
│  txn_no (PK, FK) ───────────┼───────┤
│  notice_no                  │       │
│  content                    │       │
│  date_sent                  │       │
│  mobile_code                │       │
│  mobile_no                  │       │
│  msg_error                  │       │
│  status                     │       │
│  cre_date                   │       │
│  cre_user_id                │       │
│  upd_date                   │       │
│  upd_user_id                │       │
└─────────────────────────────┘       │
                                      │
┌─────────────────────────────┐       │
│    ocms_furnish_email       │       │
├─────────────────────────────┤       │
│  txn_no (PK, FK) ───────────┼───────┘
│  notice_no                  │
│  furnish_email_subject      │
│  content                    │
│  date_sent                  │
│  email_addr                 │
│  email_cc_addr              │
│  msg_error                  │
│  cre_date                   │
│  cre_user_id                │
│  upd_date                   │
│  upd_user_id                │
└─────────────────────────────┘
```

---

## Status Values Reference

| Status Code | Description | Action |
|-------------|-------------|--------|
| S | Sent | SMS/Email berhasil dikirim ke gateway (Postman SMS / SMTP). Tidak ada action required. |
| E | Error | SMS/Email gagal dikirim setelah 3x retry. Review `msg_error` field untuk troubleshooting. |

---

## Notes

1. **Retry Mechanism**: Sistem akan melakukan retry hingga 3 kali jika pengiriman gagal sebelum menyimpan record dengan status 'E'.

2. **Audit Trail**: Semua field `cre_user_id` akan diisi dengan OIC ID yang login, memungkinkan tracking siapa yang mengirim komunikasi.

3. **Error Logging**: Jika pengiriman gagal, error message dari external service (Postman SMS / SMTP) akan disimpan di `msg_error` untuk keperluan troubleshooting.

4. **Data Retention**: Record di tabel ini harus dipertahankan sesuai dengan kebijakan data retention OCMS untuk keperluan audit.

---

## Epic Reference
- **Epic**: OCMS 41 - Furnished Hirer/Driver Manual Review
- **Section**: Section 3 - Manual Review Process
- **Functional Flow**: 3.4.4.5.3 - Backend Submission validation (Compose Email/SMS)
