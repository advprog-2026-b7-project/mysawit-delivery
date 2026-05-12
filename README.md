# MySawit Delivery Service

Service ini jalan di port `8080` dan menggunakan database PostgreSQL terpisah khusus delivery.

## Menjalankan Dengan Infra Repo

1. Jalankan dulu infra repo (yang menyalakan PostgreSQL via Docker).
2. Pastikan database plantation tersedia, default nama DB: `mysawit_delivery`.
3. Jalankan service ini.

## Konfigurasi Database

Service ini sudah membaca konfigurasi datasource dari environment variable, dengan fallback default untuk local:

- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5435/mysawit_delivery`)
- `SPRING_DATASOURCE_USERNAME` (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (default: `postgres`)

Jika infra repo expose PostgreSQL di host dan port berbeda, cukup ubah env var tersebut.

## Menjalankan Service

### Local (tanpa Docker)

```bash
./gradlew bootRun
```

### Via Docker Compose

```bash
docker compose up --build
```

## Catatan Skema Delivery

Entity pada service ini sudah disejajarkan dengan skema tim untuk kebutuhan operasional pengiriman. Terdapat dua tabel utama:

1. Entity `Shipment` (Tabel `shipments`)
   Menyimpan data utama pengiriman hasil panen.
- `id` (UUID)
- `plantation_id` (UUID, wajib diisi)
- `mandor_id` (UUID, wajib diisi)
- `driver_id` (UUID, bisa kosong saa pertama kali dibuat) 
- `total_weight_kg` (Decimal/Numeric, Wajib diisi)
- `status` (Enum, Wajib diisi, Default: `MEMUAT`)
- `rejected_reason` (Text)
- `created_at` (Timestamp, Otomatis terisi & tidak bisa diubah)
- `updated_at` (Timestamp, Otomatis terupdate)

2. Entity `ShipmentItems` (Tabel `shipment_items`)
   Menyimpan detail relasi antara pengiriman dengan data panen yang diangkut.
- `id` (UUID, Primary Key)
- `shipment_id` (UUID, Wajib diisi)
- `harvest_id` (UUID, Wajib diisi)

Fitur update shipments juga sudah menerapkan rule bahwa `code` tidak boleh diubah.

<img width="2339" height="1954" alt="Screenshot 2026-05-12 202101" src="https://github.com/user-attachments/assets/d1d14a2d-0a11-4b6b-aa2b-166e49d1add4" />
<img width="2488" height="6562" alt="mysawit-delivery-codediagram2" src="https://github.com/user-attachments/assets/61af84e6-f640-4b0e-a234-67e77a16e761" />
