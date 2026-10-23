# Zcut — Spesifikasi Rekonstruksi

Zcut adalah aplikasi editor video Android yang dibangun ulang dari hasil
reverse engineering **CapCut 19.8.0**. Semua kode, aset, dan branding ditulis
ulang; tidak ada source code, aset, atau logo milik aplikasi asli yang disalin.

- **Repo:** `https://github.com/az925-crypto/Zcut`
- **Package:** `com.zaaam.zcut`
- **Bahasa:** Kotlin + Jetpack Compose
- **Target:** Android 8.0+ (minSdk 26), compile/target SDK 35
- **Status:** Tahap 1 (spek) selesai; Tahap 2 (mockup) menunggu ACC.

## Kebijakan produk

1. **Semua fitur gratis.** Tidak ada paywall, langganan, trial, kredit,
   iklan berhadiah, atau watermark.
2. **Tidak memanggil server CapCut.** Endpoint, API key, dan akun aplikasi
   asli tidak dipakai.
3. **Fitur yang butuh backend** diberi tanda `BUTUH BACKEND` di
   `PROGRESS.md`. Padanan lokal disediakan bila memungkinkan.
4. **Tidak menyalin aset.** Font, template, effect, musik, dan ikon dibuat
   sendiri atau memakai aset berlisensi yang diizinkan.
5. **Tidak melewati proteksi.** Tidak ada patch lisensi, signature check,
   atau proteksi aplikasi asli yang disentuh.

## Struktur dokumen

```
docs/
├── README.md                  ← file ini
├── architecture.md            ← arsitektur & stack
├── evidence.md                ← indeks bukti reverse engineering
├── feature-index.md           ← matriks 31 area fitur + status backend
└── features/
    ├── 01-draft-timeline.md
    ├── ...
    └── 31-ad-maker-business.md
```

## Cara membaca

1. `architecture.md` — gambaran layer, engine, data, dan keputusan teknis Zcut.
2. `feature-index.md` — daftar semua area fitur, prioritas, dan ketergantungan
   backend.
3. `features/NN-*.md` — spek perilaku, model data, algoritma, dan referensi
   implementasi per fitur.
4. `PROGRESS.md` — checklist paritas 1:1, status implementasi, dan commit.

## Ringkasan hasil reverse

| Item | Nilai |
|---|---|
| Package asli | `com.lemon.lvoverseas` |
| Versi asli | 19.8.0 (19801600) |
| Dex | 35 file, ~298 MB |
| Class descriptor | 275.246 |
| Native library | 162 `.so` arm64-v8a |
| Asset | 1.075 file |
| SWIG class | 14.167 |
| Client facade | 95 |
| Nama operasi/tipe native | 1.390 |
| Skema draft | `142.0.0` |

Engine asli yang teridentifikasi: Lyra NLE (`libmiddle-bridge`), VESDK 21.2 +
EffectSDK (`libcccreator`), ClipFlow DAG (`libclipflow`), XT Retouch
(`libretouch_sdk`), TTFFmpeg/TTMPlayer.

## Batasan

- Analisis bersifat statis; perilaku runtime bisa berbeda.
- Nama class asli banyak yang diobfuscate.
- Fitur AI generatif, marketplace, cloud collaboration, dan digital human
  membutuhkan backend/model yang tidak ada di APK. Zcut menyediakan fallback
  lokal atau menandainya `BUTUH BACKEND`.
- Dokumen ini bukan salinan dokumentasi resmi CapCut.

## Legal

CapCut, VESDK, EffectSDK, dan asetnya milik ByteDance. Dokumen ini untuk riset
interoperabilitas dan pembangunan produk orisinal. Jangan menyalin aset, font,
effect package, template, atau kode proprietary tanpa lisensi.
