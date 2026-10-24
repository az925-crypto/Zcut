# Desain UI M2 — Editor Shell & Media Import

Spesifikasi desain untuk milestone **M2**: shell editor, kanvas timeline, dan
impor media. Basis visual tetap mockup Stage 2 yang di-ACC
(`mockup/assets/screens-core.js` → `RENDER.editor`, `RENDER['media-picker']`).
Tidak ada aset, font, atau ikon CapCut yang dipakai.

## 1. Lingkup M2

| Masuk M2 | Di luar M2 |
|---|---|
| Rute `editor/{projectId}` dan `media-picker?projectId=` | Pemutaran video (M3) |
| Preview frame statis + info project | Trim/split/move/ripple (M4) |
| Kanvas timeline multi-track + playhead + seek tap | Speed curve (M5), keyframe (M6) |
| Pilih klip (seleksi visual) | Panel tool yang sudah punya rute sendiri |
| Impor via Photo Picker & SAF, copy ke workspace privat | Proxy/transcode, waveform (M3+) |
| Metadata + thumbnail lokal | Hapus file yatim otomatis (M27) |

Editor M2 **tidak menulis** ke repository. Hanya layar media picker yang
menulis (menambah aset + klip). Ini mencegah dua penulis saling menimpa
dokumen draft.

## 2. Anatomi Editor

```
┌ top bar 54dp ─────────────────────────────────────────────┐
│ ←   Nama project            [Export]                       │
│     1080×1920 · 30 fps · 00:24                             │
├ preview ──────────────────────────────────────────────────┤
│  frame 9:16 (maks 42% tinggi layar), latar surface-2       │
│  thumbnail klip pertama · badge "9:16 · 24,0s"             │
│  tombol play (nonaktif) + label "Pemutar video di M3"      │
├ tool strip ────────────────────────────────────────────────┤
│  Edit · Audio · Teks · Sticker · Efek · Filter · Adjust · Mask │
├ timeline ──────────────────────────────────────────────────┤
│  ruler 0s … 24s                                            │
│  V1  ▓▓▓▓▓ ▓▓▓▓                                          │
│  OV                                                        │
│  T1                                                        │
│  CC                                                        │
│  A1  ▒▒▒▒▒▒▒▒                                             │
│  playhead │                                                │
├ footer ────────────────────────────────────────────────────┤
│  + Media · Trim · Speed · Volume                           │
└────────────────────────────────────────────────────────────┘
```

- Latar `background`; panel memakai `surface`/`surface2`; garis `outline`.
- Aksen per tipe track: video `ZcutCyan`, overlay `ZcutViolet`, teks
  `ZcutAmber`, caption `ZcutGreen`, audio `ZcutLime`.
- Klip terpilih: border 2dp `ZcutLime` + elevasi ringan; tidak ada animasi
  berlebih (seek-safe, tidak ada loop).
- Tool strip memakai **label teks** (bukan ikon): `material-icons-core` tidak
  menyediakan ikon scissor/music/filter/mask, dan menambah
  `material-icons-extended` hanya membengkakkan APK tanpa R8. Precedent sama
  dengan baris Pengaturan di M1.

### State editor

| State | Tampilan |
|---|---|
| Loading | progress bar tipis di bawah top bar |
| Project tidak ditemukan | `EmptyState` "Draft tidak ditemukan" + tombol kembali |
| Timeline kosong | frame preview kosong + CTA "Tambah media" |
| Ada klip | frame thumbnail + timeline terisi |
| Error baca | banner error, timeline tetap bisa dilihat bila ada data |

Playhead dan seleksi disimpan di ViewModel (bukan repository). Keduanya
di-clamp saat durasi project berubah (mis. setelah impor).

## 3. Matematika timeline

Semua fungsi murni di `feature/editor/EditorTimeline.kt` supaya bisa diuji JVM.

- Skala: `PX_PER_SECOND = 64dp`. Lebar konten =
  `max(lebar layar, durasiMs / 1000 * 64dp)`.
- Tick ruler: setiap 1 detik bila durasi ≤ 30 dtk, setiap 2 detik bila ≤ 90
  dtk, setiap 5 detik selebihnya. Label memakai `TimeFormat.clock`.
- Posisi klip: `x = startMs / 1000 * 64dp`, lebar =
  `durationMs / 1000 * 64dp` (minimum 8dp agar klip pendek tetap terlihat).
- Playhead: garis 2dp `ZcutLime` + handle segitiga di ruler.
- Tap pada ruler/lane → `seekTo(ms)`; nilai di-clamp ke `0..durationMs`.
- Satu `ScrollState` dipakai bersama oleh ruler dan semua lane; label track
  berada di kolom kiri yang tidak ikut scroll.

## 4. Media picker

Mockup menampilkan grid galeri perangkat. Zcut memilih **Photo Picker + SAF**
tanpa permission `READ_MEDIA_*`, sesuai spek F02 P0 dan prinsip privasi
(`DECISIONS.md` 2026-10-24). Konsekuensinya grid berisi **item yang dipilih**,
bukan seluruh galeri. Struktur visual mockup dipertahankan:

1. Segmented: **Foto · Video · Audio** (menentukan tipe yang bisa dipilih).
2. Grid 3 kolom item terpilih; badge nomor lime di pojok kanan atas; tap =
   hapus dari pilihan.
3. Tombol "Pilih dari galeri" (Photo Picker, maks 10 item) dan
   "Telusuri file" (SAF multi-dokumen, tanpa batas 10).
4. Hint: "Media disalin ke workspace privat aplikasi. Tidak ada upload."
5. CTA sticky: "Tambahkan N item ke timeline" (nonaktif saat N = 0).
6. Empty state punya aksi "Mulai dengan timeline kosong" agar tidak ada jalan
   buntu saat pengguna hanya ingin draft kosong.

### Alur impor

```
pilih URI → (picker) simpan daftar URI
CTA → MediaImporter.import(uri) per item
        copy ke filesDir/media/<uuid>.<ext>
        probe metadata (MediaMetadataRetriever / BitmapFactory)
        thumbnail 320px ke cacheDir/thumbs/<uuid>.jpg
     → MediaAppender.append(project, assets)
     → repository.upsert(project)
     → popBackStack (project lama) / navigate editor (project baru)
```

Kegagalan per item tidak membatalkan item lain; ringkasan "N berhasil, M
gagal" ditampilkan. Item yang gagal menghapus salinan parsialnya sendiri.
Salinan dari item yang sudah berhasil tetapi batch akhirnya tidak tersimpan
(mis. upsert gagal) belum disapu otomatis; penyapuan file yatim masuk M27.

Batas impor: 2 GB per file, dan sisa ruang ≥ ukuran file + 256 MB. MIME yang
diterima: `image/*`, `video/*`, `audio/*`.

## 5. Aksesibilitas

- Setiap tombol punya `contentDescription`/teks yang jelas.
- Target sentuh minimal 44dp.
- Kontras teks mengikuti skema M0 (lime hanya untuk aksen, teks gelap di atas
  lime).
- Timeline dapat di-scroll horizontal; tidak ada gestur yang membutuhkan dua
  jari.
- Status impor diumumkan lewat teks, bukan hanya warna.

## 6. Di luar lingkup (dijadwalkan)

- **M3:** pemutaran Media3, scrub playhead saat play, waveform audio.
- **M4:** trim/split/move/snap/ripple, undo/redo, tombol footer
  Split/Delete/Duplicate.
- **M13:** layar export nyata (tombol Export sekarang membuka placeholder
  M13).
- **M27:** pembersihan file media yatim dan trash draft.
