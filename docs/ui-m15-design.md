# Desain UI M15 — Retouch & beauty (on-device)

Spesifikasi desain untuk milestone **M15**. Basis visual tetap mockup Stage 2
yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.beauty`, layar
`beauty` #19). Tidak ada aset, ikon, model, atau branding CapCut yang dipakai.

Catatan kejujuran: subagent `design` masih belum bisa dipanggil (model
unavailable), sama seperti M11–M14. Dokumen ini ditulis manual dari mockup ACC
dan dari bukti artefak yang sudah dibaca; kegagalan dicatat di `DECISIONS.md`.

## 1. Lingkup M15

| Masuk M15 | Di luar M15 |
|---|---|
| Deteksi wajah + landmark on-device (ML Kit, model dibundel) | Beauty per frame seluruh video saat export |
| 6 slider mockup: Skin smooth, Whiten, Face slim, Eyes, Nose, Lips | Tab Body & Makeup (key sudah terbaca, slider belum dirancang mockup) |
| Warp reshape wajah satu pass (slim/eyes/nose) | Makeup penuh (blush, eyeliner, eyeshadow, eyebrow, lashes) |
| Smooth & whiten terlokalisasi ke area wajah | Hair color / hair volume |
| Lip tint lokal dari landmark mulut | Manual reshape brush, penghapus objek |
| Switch "Preserve texture" | Body reshape (slim/long legs/waist/hip/shoulder) |

Keputusan penting: kalau model tidak bisa dimuat atau tidak menemukan wajah,
layar **mengatakannya** dan slider tidak dijalankan. Tidak ada wajah karangan
dan tidak ada persentase palsu.

## 2. Anatomi layar

```
┌ TopBar: "Retouch" · sub "Nama klip · 00:04" · [Selesai]                 ┐
│  ┌ pratinjau 180dp ─────────────────────────────────────────────────┐   │
│  │  frame (ContentScale.Fit, letterbox hitam)                        │   │
│  │  kotak wajah terdeteksi (digambar di ruang skala yang sama)       │   │
│  │  [FACE DETECTED · 1]  (chip kiri-bawah)                           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  [ Face | Body | Makeup ]   (segmented control mockup)                  │
│                                                                         │
│  Wajah                                                                  │
│    (hint) deteksi + pemrosesan di perangkat, tidak ada unggahan         │
│    Skin smooth ▓▓▓▓░░░░░░  40                                          │
│    Whiten      ▓▓░░░░░░░░  20                                          │
│    Face slim   ▓▓▓░░░░░░░  30                                          │
│    Eyes        ▓░░░░░░░░░  10                                          │
│    Nose        ░░░░░░░░░░   0                                          │
│    Lips        ▓▓░░░░░░░░  25                                          │
│    (hint) key asli tiap slider (assets/local_beauty_all.json)           │
│    (hint) N slider aktif · tekstur dijaga                               │
│                                                                         │
│  Preserve texture                                                       │
│    Jaga detail pori                                    (switch)         │
│    (hint) Smoothing dilembutkan supaya pori tetap terlihat / penuh      │
│    (hint) Retouch memakai satu frame klip; export belum membakarnya     │
└─────────────────────────────────────────────────────────────────────────┘
```

Perilaku:

- Slider memakai draft lokal: angka bergerak saat drag, draft ditulis sekali
  saat jari dilepas. Composite piksel hanya jalan sekali per commit
  (`BeautyProcessor.apply`).
- Slider nilai 0–100 (sama seperti `_ui_value` di konfigurasi asli).
- Kotak wajah digambar dengan menghitung skala `ContentScale.Fit` lebih dulu,
  supaya koordinat normal (0..1) jatuh tepat di atas wajah, bukan di panel.
- Label chip: `FACE DETECTED · n`, `MENDETEKSI WAJAH...`, `TANPA MEDIA`.
- Tab Body & Makeup menampilkan catatan jujur yang menyebut bukti aslinya
  (`BeautyBodyPreset.body_reshapes`, `assets/ManualReshape/event.lua`,
  `type_makeup_all`/`type_blush`), bukan slider karangan. Slider Face berlaku
  untuk seluruh wajah.

## 3. Kontrak teknis yang terlihat di UI

- `FaceDetector.detect(Bitmap): List<FaceGeometry>` (`MlKitFaceDetector`,
  `PERFORMANCE_MODE_ACCURATE` + `LANDMARK_MODE_ALL`, satu client per proses).
  Geometri dinormalisasi (0..1) supaya satu deteksi dipakai untuk pratinjau dan
  frame resolusi penuh; landmark yang tidak dikembalikan model tetap `null`.
- `BeautyProcessor.apply(frame, face, config): Bitmap?` — `null` berarti
  "tidak ada yang ditampilkan" (tidak ada wajah / tidak ada slider digeser).
  Satu pass warp (attractor `OFFSET`/`EXPAND`, meniru bentuk
  `assets/zip/FaceReshape_V2/distortion.json`), lalu smooth & whiten
  terlokalisasi (mask elips lembut + `DST_IN`), lalu lip tint dari landmark mulut.
- `BeautyEditor` (domain murni) memegang tabel key/rentang asli, konversi
  slider→nilai engine, clamp config tak terpercaya, label, dan ringkasan;
  semuanya diuji di JVM.
- `BeautyConfig` aditif di `Clip` (`beauty`), draft lama decode `null` tanpa
  naik `schemaVersion`.

## 4. Yang sengaja tidak dipalsukan

- Tidak ada server, akun, atau unggahan; model dibundel di APK.
- Konversi slider→engine untuk grup **shape** tidak bisa dibuktikan penuh dari
  konfigurasi (preset asli memakai `0.5 + ui/200`, bukan interpolasi rentang);
  ditulis sebagai **Unknown** di `docs/features/12-retouch-beauty.md`, bukan
  disembunyikan.
- Pemetaan `type` pada `distortion.json` → `WarpKind` ditandai **inferred**;
  magnitudo warp milik kita sendiri.
- Export belum membakar retouch (batas render M13) dan itu ditulis di layar.
