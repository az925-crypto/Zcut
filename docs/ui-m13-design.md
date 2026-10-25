# Desain UI M13 — Export & render (Media3 Transformer)

Spesifikasi desain untuk milestone **M13**. Basis visual tetap mockup Stage 2
yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.export` dan
`RENDER['export-done']`). Tidak ada aset, ikon, atau komponen CapCut yang
dipakai.

Catatan kejujuran: subagent `design` belum bisa dipanggil (model unavailable),
sama seperti M11/M12. Dokumen ini ditulis manual dari mockup ACC; kegagalan
dicatat di `DECISIONS.md`.

## 1. Lingkup M13

| Masuk M13 | Di luar M13 |
|---|---|
| Render lokal MP4 H.264/AAC lewat Media3 `Transformer` | HEVC, WebM, GIF, PNG sequence |
| Resolusi mengikuti `ExportPreset` (`Presentation.createForShortSide`) | Bitrate/fps eksplisit (encoder perangkat yang menentukan) |
| Progress + estimasi sisa (polling `Transformer.getProgress`) | Render paralel / prerender cache / remux |
| Batal saat render | Pause/resume render |
| Simpan ke galeri (`MediaStore`, API 29+) | Alpha export, HDR/10-bit |
| Bagi lewat share sheet sistem (`FileProvider`) | Unggah ke server/marketplace (F27, BUTUH BACKEND) |
| Smart cover (satu frame → JPEG) | Burn-in teks/efek/sticker/mask di video final |
| Layar hasil `export-done` dengan status jujur | Audio-only export (M4A/MP3) |

Klip yang tidak bisa diputar juga tidak bisa diekspor: `PreviewPlanBuilder`
menjadi satu-satunya gerbang sumber, jadi UI bisa menyebut media mana yang
hilang alih-alih menghasilkan video setengah jadi.

## 2. Anatomi layar

### 2a. `export` (F19)

```
┌ TopBar: "Export" · sub "Render lokal · tanpa watermark" · [GRATIS]      ┐
│                                                                         │
│  [cover 96×72]  Nama draft                                              │
│                 1080p · 30 fps · 00:24                                  │
│                                                                         │
│  Setelan render                                                         │
│    Resolusi      1080p                                                  │
│    Frame rate    30 fps                                                 │
│    Format        MP4 · H.264                                            │
│    Audio         AAC                                                    │
│    Bitrate       Otomatis                                               │
│    (hint) Encoder perangkat menentukan frame rate/bitrate akhir.        │
│                                                                         │
│  Smart cover — Simpan satu frame sebagai cover ekspor   (switch)        │
│  Watermark — Tidak pernah ditambahkan                    [OFF]          │
│                                                                         │
│  Merender                          (muncul hanya saat render)           │
│    ▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░                                        │
│    Merender video...                                          42%       │
│                                                                         │
│  [Batalkan]  /  [Export video]                                          │
│  (hint) Render berjalan di perangkat dengan Media3. Estimasi 36.6 MB    │
│         · 00:24. Tidak ada unggahan.                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

Perilaku:

- Tombol "Export video" nonaktif hanya kalau timeline kosong atau render sedang
  jalan.
- Saat render, tombol utama berganti "Batalkan" (memanggil
  `VideoRenderEngine.cancel()`).
- Pesan gagal tampil di kartu pesan dengan tombol "Tutup"; tidak ada state
  diam-diam.
- Selesai → pindah ke `export-done` menggantikan form export, sehingga tombol
  "kembali" dari layar hasil mengarah ke editor, bukan ke form yang sudah
  selesai.

### 2b. `export-done` (F19)

```
┌ (tanpa top bar)                                                          ┐
│                          ( ✓ )  64dp                                    │
│                    RENDER SELESAI                                       │
│                    Video kamu siap.                                     │
│                    Nama draft · 1080p · 00:24                           │
│                                                                         │
│  ┌ KARTU ─────────────────────────────────────────────┐                 │
│  │ Nama-1712345678.mp4                                │                 │
│  │ 36.6 MB · tersimpan di ruang kerja app             │                 │
│  │ Cover: Nama-1712345678-cover.jpg   (opsional)      │                 │
│  └────────────────────────────────────────────────────┘                 │
│                                                                         │
│  [Bagikan]                              [Simpan]                        │
│  [Kembali ke home]                                                      │
│                                                                         │
│  Watermark .................................... [TIDAK ADA]             │
│  Paywall ...................................... [TIDAK ADA]             │
│  Unggahan ..................................... [TIDAK ADA]             │
│  (hint) Video ditulis ke penyimpanan perangkat. Zcut tidak mengunggah.  │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Bagikan** membuka share sheet sistem (`ACTION_SEND` + `FileProvider`,
  authority `<applicationId>.fileprovider`, hanya folder `filesDir/exports`).
- **Simpan** menyalin ke galeri `Movies/Zcut` lewat `MediaStore` (tanpa izin di
  Android 10+). Di Android 8–9 file masuk ke folder Movies milik app, dan pesan
  status menyebut lokasinya apa adanya.
- Nama file selalu dibawa antar layar sebagai **nama**, bukan path; file selalu
  dibaca dari folder export app.

## 3. Kontrak visual

- Komponen memakai design system Zcut: `ZcutScaffold`, `ZcutTopBar`,
  `StatusBadge`, `ZcutCard`, `ZcutPrimaryButton`, `ZcutSecondaryButton`,
  `ListRow`, dan `ToolSection`/`ToolHint` dari panel tool.
- Warna/aksen tidak menambah palet baru (lime = aksi utama, badge netral untuk
  status "TIDAK ADA").
- Touch target tombol ≥ 44dp mengikuti komponen yang sudah ada.

## 4. Yang sengaja tidak dipalsukan

- **Tidak ada server**: tidak ada endpoint, tidak ada akun, tidak ada unggahan.
- **Tidak ada watermark** dan tidak ada paywall; panel di layar hasil hanya
  menyatakan fakta itu.
- **Smart cover** benar-benar menulis satu JPEG; kalau ekstraksi frame gagal,
  namanya tidak dilaporkan di layar hasil.
- **Resolusi** benar-benar diterapkan; bitrate/fps/sample rate akhir diakui
  sebagai keputusan encoder perangkat lewat hint di layar export.
