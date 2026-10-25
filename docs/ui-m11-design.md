# Desain UI M11 — Teks & Tipografi

Spesifikasi desain untuk milestone **M11**. Basis visual tetap mockup Stage 2
yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.text`). Tidak ada aset,
font, atau ikon CapCut yang dipakai; font yang tersedia hanya font sistem
Android.

## 1. Lingkup M11

| Masuk M11 | Di luar M11 |
|---|---|
| Input teks multiline, font sistem, size, warna, align | Import font dari perangkat (ditunda; chip nonaktif + hint) |
| Bold/outline/shadow/neon/gradient, letter-spacing, opacity | Template teks multi-layer (butuh model + editor baru) |
| Animasi RISE/TYPEWRITER/POP/GLITCH di preview Compose | Burn-in teks ke video export (M13) |
| Tambah/edit/hapus clip teks, label teks di timeline | AI font (BUTUH BACKEND; model generatif tidak ada di perangkat) |
| Overlay teks di preview editor mengikuti playhead | Rich text per-range, curve text, handwrite (P1/P2) |

Satu track `TEXT` dipakai bersama. Dua teks yang bentrok waktunya tidak bisa
tampil bersamaan; clip baru digeser ke slot bebas berikutnya. Keterbatasan ini
dicatat di UI, bukan disembunyikan.

## 2. Anatomi layar

```
┌ TopBar: "Text" · subjudul nama project · kembali · [Selesai] ┐
│                                                              │
│  ┌ Preview card 176dp ─────────────────────────────────────┐ │
│  │ cover project + teks live (style draft + animasi loop)   │ │
│  │ label kiri-bawah: "JUDUL · 42 PT"                        │ │
│  └──────────────────────────────────────────────────────────┘ │
│  [TextField multiline: isi teks]                              │
│                                                              │
│  Font     [Display] [Serif] [Mono] [Hand] [Import]           │
│  Style    [Bold] [Outline] [Shadow] [Neon] [Gradient]        │
│  Warna    ● ● ● ● ● ●                                        │
│  Size     ─────────●────────  42                             │
│  Spacing  ─────●────────────  0                              │
│  Opacity  ───────────────●──  100%                           │
│  Animasi  [None] [Rise] [Typewriter] [Pop] [Glitch]          │
│                                                              │
│  [Tambah teks]  atau  [Terapkan] [Hapus teks]                │
└──────────────────────────────────────────────────────────────┘
```

### 2a. Komponen

| Komponen | Perilaku |
|---|---|
| Preview card | Cover project dari `mediaThumbnails`; teks digambar ulang tiap perubahan draft. Animasi diputar loop 1,6 s agar terlihat tanpa harus menekan play. |
| Input teks | Multiline, maksimal 1.000 karakter. Baris baru dipertahankan. |
| Chip Font | `Display` → SansSerif, `Serif` → Serif, `Mono` → Monospace, `Hand` → Cursive. `Import` nonaktif + hint "Import font dari perangkat menyusul". |
| Chip Style | Single-select: `Bold`, `Outline`, `Shadow`, `Neon`, `Gradient`. Tap chip aktif = kembali ke style dasar. `Neon` memakai warna fill sebagai stroke + shadow; `Gradient` memakai aksen lime→cyan. |
| Swatch warna | Putih, lime, coral, cyan, violet, hitam. Warna terpilih punya ring. |
| Slider Size | 12–96 pt (default 42). |
| Slider Spacing | 0–20 (letter spacing, dalam persen font size). |
| Slider Opacity | 0–100% (default 100%). |
| Chip Animation | `None`, `Rise`, `Typewriter`, `Pop`, `Glitch`. Pratinjau animasi di card. |
| Tombol utama | Belum ada clip: `Tambah teks` (membuat clip 3 detik di playhead). Mengedit: `Terapkan` lalu kembali. |
| Hapus teks | Hanya saat mengedit; menghapus clip teks dan kembali ke editor. |

## 3. State

| State | Tampilan |
|---|---|
| Loading | Progress bar di tengah, top bar tetap. |
| Project tidak ditemukan | Empty state "Draft tidak ditemukan" + tombol kembali. |
| Belum ada clip teks (`clipId` kosong/bukan teks) | Form kosong dengan teks awal "Teks baru", tombol `Tambah teks`. |
| Clip terpilih | Form terisi dari clip; tombol `Terapkan` + `Hapus teks`. |
| Clip terhapus saat layar terbuka | Kembali ke mode "belum ada clip" tanpa crash. |
| Teks kosong | Tombol simpan nonaktif; hint "Isi teks dulu". |
| Batas track | Hint "Teks lain sudah memakai waktu ini; clip baru digeser ke slot berikutnya." |

## 4. Draft vs commit

- Draft lokal di-seed sekali per `clip.id`; perubahan dari repository tidak
  menimpa draft yang sedang diketik.
- Slider dan chip commit saat aksi selesai (drag end / tap), bukan tiap frame.
- Input teks commit dengan debounce 300 ms; `Terapkan`/`Selesai` melakukan
  flush terakhir sebelum kembali.
- Semua tulis memakai `Mutex` read-modify-write yang sama dengan tool lain.

## 5. Preview yang jujur

- Overlay teks di editor adalah Compose, bukan Media3: Media3 1.9.0 hanya
  memetakan satu sequence visual + satu sequence audio.
- Animasi dihitung dari `playhead - clip.startMs`; scrub dan play sama-sama
  memperlihatkan animasi.
- Burn-in teks ke file MP4 baru dilakukan di M13. Sampai saat itu, UI
  menyebutkan "Teks tampil di preview; ikut ter-render saat export (M13)."
- Timeline hanya berisi teks tidak punya sequence Media3; tombol play tetap
  bisa scrub, tetapi tidak ada frame video yang diputar.

## 6. Aksesibilitas & konsistensi

- Target sentuh minimal 44dp; chip memakai `selected` visual (fill aksen).
- Semua slider punya label teks dan nilai numerik.
- Kontras teks putih di atas cover memakai scrim tipis bila perlu.
- Copy Indonesia, konsisten dengan mockup ACC dan layar tool M4–M10.
- Tidak memakai `material-icons-extended`; ikon dekoratif digambar lokal.
