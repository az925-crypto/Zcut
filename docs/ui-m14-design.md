# Desain UI M14 — Matting & cutout (ML Kit)

Spesifikasi desain untuk milestone **M14**. Basis visual tetap mockup Stage 2
yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.matting`). Tidak ada
aset, ikon, atau model CapCut yang dipakai.

Catatan kejujuran: subagent `design` belum bisa dipanggil (model unavailable),
sama seperti M11–M13. Dokumen ini ditulis manual dari mockup ACC; kegagalan
dicatat di `DECISIONS.md`.

## 1. Lingkup M14

| Masuk M14 | Di luar M14 |
|---|---|
| Segmentasi subjek on-device (ML Kit selfie, model dibundel) | Matte per frame seluruh video saat export |
| Cutout satu frame: subjek dipertahankan, latar dibuang | Hair detail / temporal consistency |
| Latar transparan (checkerboard), warna preset, blur, shadow | Generate latar AI (BUTUH BACKEND, prompt disimpan) |
| Edge refine (ambang confidence) | Brush eraser manual per frame |
| Label kepercayaan subjek yang jujur | Smart crop / auto reframe (F30) |

Keputusan penting: kalau model tidak bisa dimuat atau frame terlalu kecil,
layar **mengatakannya**, bukan menggambar tebakan. Label tidak pernah muncul
tanpa ada mask yang benar-benar dihitung.

## 2. Anatomi layar

```
┌ TopBar: "Cutout" · sub "Nama klip · 00:04" · [Selesai]                   ┐
│  ┌ pratinjau 196dp ─────────────────────────────────────────────────┐   │
│  │  checkerboard (transparansi terlihat apa adanya)                 │   │
│  │  frame hasil composite                                           │   │
│  │  [SUBYEK · 92%]  (chip kiri-bawah)                                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  Subjek                                                                 │
│    (hint) Segmentasi jalan di perangkat; tidak ada gambar dikirim.      │
│    Aktifkan cutout                                        (switch)      │
│    Edge refine  ▓▓▓▓▓▓░░░░░░  50%                                       │
│    Shadow       ▓▓░░░░░░░░░░  20%                                       │
│                                                                         │
│  Background                                                             │
│    [Remove BG]  [Blur BG]                                               │
│    [Studio] [Forest] [Sunset] [AI]                                      │
│    ● Latar diburamkan 50%                                               │
│    (hint) Latar AI BUTUH BACKEND …                                      │
│    (hint) Cutout pratinjau memakai satu frame klip. Matte per frame saat │
│           export belum ada (batas render M13).                          │
└─────────────────────────────────────────────────────────────────────────┘
```

Perilaku:

- Slider (`Edge refine`, `Shadow`, `Blur`) memakai draft lokal: angka bergerak
  saat drag, draft ditulis sekali saat jari dilepas. Composite mahal (loop per
  piksel) hanya jalan sekali per commit.
- `Remove BG` = latar transparan; `Blur BG` = latar frame yang diburamkan;
  chip warna = latar warna solid dari preset mockup (Studio/Forest/Sunset).
- `AI` menyimpan prompt di draft dan menandai **BUTUH BACKEND**; tidak ada
  gambar latar yang dipalsukan. Latar transparan memakai frame sebagai latar
  sementara supaya pratinjau tidak kosong — state itu tidak diklaim sebagai AI.
- Label chip: `SUBYEK · nn%` (rata-rata confidence piksel foreground),
  `TIDAK ADA SUBYEK`, `MENSEGMENTASI...`, `SEGMENTASI TIDAK TERSEDIA`,
  `CUTOUT GAGAL DIBUAT`, atau `CUTOUT MATI`.

## 3. Kontrak teknis yang terlihat di UI

- `SubjectSegmenter.segment(Bitmap): SubjectMask?` — kontrak lama berbasis
  path (`inputUri`/`outputMaskPath`) diganti berbasis bitmap supaya tidak ada
  tulis-baca PNG mask yang tidak perlu; mask tetap di memori.
- `MlKitSubjectSegmenter` memakai `SelfieSegmenterOptions.SINGLE_IMAGE_MODE`
  (tanpa `enableRawSizeMask`), satu client per proses, dan membaca buffer
  `FloatArray` native-order.
- `MattingComposer` menyusun hasil: stencil alpha dari matte → `DST_IN` pada
  frame → gambar latar (warna / blur turun-naik skala / frame) → shadow.
- `MattingEditor` (domain murni) memegang clamp config, remap confidence→alpha,
  hitung confidence subjek, dan label; semuanya diuji di JVM.

## 4. Yang sengaja tidak dipalsukan

- Tidak ada server, akun, atau unggahan; model dibundel di APK.
- Tidak ada persentase yang dicetak kalau model tidak menemukan subjek.
- Latar AI tetap ditandai butuh backend, dan pratinjaunya memakai frame asli
  (bukan gambar karangan) supaya tidak menyamar sebagai hasil generate.
