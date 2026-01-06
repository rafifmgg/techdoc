# Langkah-Langkah Pembayaran di URA Payment Gateway

Berikut ini adalah alur umum proses pembayaran melalui URA Payment Gateway (Urban Redevelopment Authority, Singapore).

---

## 1. Navigasi ke Halaman Pembayaran
- Pengguna diarahkan ke URA Payment Gateway dari aplikasi eksternal (misalnya e-service URA atau portal perusahaan).
- Sistem akan menampilkan halaman pembayaran yang terkait dengan transaksi spesifik.

---

## 2. Tampilkan Informasi Transaksi
Informasi yang biasanya ditampilkan:
- Nomor referensi transaksi
- Deskripsi tagihan
- Jumlah tagihan
- Alamat email (jika tersedia)

---

## 3. Pilih Metode Pembayaran
Pengguna dapat memilih salah satu dari metode pembayaran berikut:
- eNETS (Internet Banking)
- Kartu Kredit/Debit (Visa/Mastercard)
- PayNow (Scan QR Code via Mobile Banking)
- GIRO (khusus institusi tertentu)

---

## 4. Lakukan Pembayaran
- Pengguna akan dialihkan ke halaman otorisasi bank jika memilih eNETS atau kartu kredit.
- Untuk PayNow, sistem akan menampilkan QR Code yang dapat dipindai dengan aplikasi mobile banking.

---

## 5. Konfirmasi Pembayaran
- Setelah proses pembayaran selesai, sistem akan:
    - Menampilkan status: `Success` atau `Failed`
    - Menyediakan nomor referensi pembayaran URA
    - Mengirimkan notifikasi email (jika email dicantumkan)

---

## 6. Kembali ke Aplikasi Asal
- Sistem akan mengarahkan pengguna kembali ke aplikasi awal (misal portal internal).
- Status pembayaran akan diterima oleh sistem, dan ditandai sebagai berhasil atau gagal.

---

## Catatan Tambahan
- URA Payment Gateway mengikuti standar keamanan pemerintah Singapura (HTTPS, enkripsi, 2FA bila perlu).
- Timeout dapat terjadi jika halaman dibiarkan terlalu lama.
- Nomor referensi URA sangat penting untuk keperluan audit dan rekonsiliasi internal.

---

Informasi ini merupakan rangkuman berdasarkan proses umum URA dan dapat disesuaikan dengan sistem internal yang mengintegrasikannya.
