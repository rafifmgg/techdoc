# Simulasi API Parking Fines

Dokumen ini berisi simulasi penggunaan API `eocmsvalidoffencenotice/v1/parkingFines` dengan berbagai skenario.

## Endpoint

```
GET eocmsvalidoffencenotice/v1/parkingFines
```

## Parameter

| Parameter | Tipe | Deskripsi |
|-----------|------|-----------|
| noticeNo | String | Nomor pemberitahuan pelanggaran |

## Skenario 1: Data dengan Status TS-PP (Tidak dapat dibayar)

### Request

```
GET eocmsvalidoffencenotice/v1/parkingFines?noticeNo=NTC202507051001
```

### Response

```json
{
  "notice_no": "NTC202507051001",
  "vehicle_no": "SGX1001Z",
  "notice_date_and_time": "2025-07-01T10:15:00",
  "amount_payable": 150.00,
  "pp_code": "PP01",
  "suspension_type": "TS",
  "crs_reason_of_suspension": "PP",
  "date_transaction": null,
  "user_message": "This notice has been suspended and cannot be processed for payment.",
  "app_code": "E3",
  "show": false,
  "notice_payment_flag": "UNPAYABLE"
}
```

Dalam skenario ini, denda parkir memiliki `suspension_type` = "TS" dan `crs_reason_of_suspension` = "PP", yang menunjukkan bahwa denda ini telah ditangguhkan dan tidak dapat diproses untuk pembayaran. Sistem mengembalikan pesan error yang sesuai dan flag pembayaran "UNPAYABLE".

## Skenario 2: Data dengan Transaksi Hari Ini

### Request

```
GET eocmsvalidoffencenotice/v1/parkingFines?noticeNo=NTC202507051002
```

### Response

```json
{
  "notice_no": "NTC202507051002",
  "vehicle_no": "SGX1002Z",
  "notice_date_and_time": "2025-07-04T14:35:00",
  "amount_payable": 120.00,
  "pp_code": "O123",
  "suspension_type": null,
  "crs_reason_of_suspension": null,
  "date_transaction": "2025-07-05T09:15:22",
  "user_message": "Transaction is still being processed. Please wait at least 5 minutes before trying again.",
  "app_code": "A1",
  "show": true,
  "notice_payment_flag": "PAYABLE"
}
```

Dalam skenario ini, denda parkir memiliki transaksi yang dilakukan hari ini (kurang dari 5 menit yang lalu). Sistem mengembalikan pesan yang menunjukkan bahwa transaksi masih dalam proses dan pengguna harus menunggu setidaknya 5 menit sebelum mencoba lagi.

### Request Alternatif (Transaksi > 5 menit)

```
GET eocmsvalidoffencenotice/v1/parkingFines?noticeNo=NTC202507051003
```

### Response

```json
{
  "notice_no": "NTC202507051003",
  "vehicle_no": "SGX1003Z",
  "notice_date_and_time": "2025-07-04T16:20:00",
  "amount_payable": 80.00,
  "pp_code": "O124",
  "suspension_type": null,
  "crs_reason_of_suspension": null,
  "date_transaction": "2025-07-05T08:30:15",
  "user_message": "Previous transaction has been completed. You may proceed with a new transaction.",
  "app_code": "A2",
  "show": true,
  "notice_payment_flag": "PAYABLE"
}
```

Dalam skenario alternatif ini, denda parkir memiliki transaksi yang dilakukan hari ini tetapi lebih dari 5 menit yang lalu. Sistem mengembalikan pesan yang menunjukkan bahwa transaksi sebelumnya telah selesai dan pengguna dapat melanjutkan dengan transaksi baru.

## Skenario 3: Data Tidak Kena TS-PP, Belum Bayar, dengan Offence Type E

### Request

```
GET eocmsvalidoffencenotice/v1/parkingFines?noticeNo=NTC202507051004
```

### Response

```json
{
  "notice_no": "NTC202507051004",
  "vehicle_no": "SGX1004Z",
  "notice_date_and_time": "2025-07-03T11:45:00",
  "amount_payable": 200.00,
  "pp_code": "E101",
  "offence_notice_type": "E",
  "suspension_type": null,
  "crs_reason_of_suspension": null,
  "date_transaction": null,
  "user_message": null,
  "app_code": null,
  "show": true,
  "notice_payment_flag": "PAYABLE"
}
```

Dalam skenario ini, denda parkir memiliki `offence_notice_type` = "E" (Environmental), tidak memiliki status TS-PP, dan belum ada transaksi pembayaran. Sistem mengembalikan data denda dengan flag pembayaran "PAYABLE", menunjukkan bahwa denda ini dapat dibayar.

## Catatan Penting

1. Nilai `notice_payment_flag` menentukan apakah denda dapat dibayar ("PAYABLE") atau tidak ("UNPAYABLE").
2. Jika `user_message` dan `app_code` tidak null, itu menunjukkan ada pesan informasi atau error yang perlu ditampilkan kepada pengguna.
3. Nilai `show` menentukan apakah data denda harus ditampilkan kepada pengguna atau tidak.
4. Transaksi dianggap "masih dalam proses" jika dilakukan kurang dari 5 menit yang lalu.
5. Transaksi dianggap "selesai" jika dilakukan lebih dari 5 menit yang lalu.
