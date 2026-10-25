# Desain UI M4–M10 — Tool editor per klip

Spesifikasi desain untuk batch **M4–M10**: trim, speed/curve, keyframe,
transisi, filter/adjust, efek, dan mask/chroma. Basis visual tetap mockup
Stage 2 yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.trim`,
`RENDER.speed`, `RENDER.keyframe`, `RENDER.transition`, `RENDER.filters`,
`RENDER.adjust`, `RENDER.effects`, `RENDER.mask`). Tidak ada aset, font, atau
ikon CapCut yang dipakai.

## 1. Lingkup batch

| Masuk batch | Di luar batch |
|---|---|
| Trim in/out dengan handle dan preview range | Scrub frame-accurate penuh (M13) |
| Speed konstan 0.1×–8×, reverse (tersimpan), curve editor | Pitch/reverse real-time di preview (disimpan, tidak dirender) |
| Keyframe posisi/scale/opacity/rotasi/volume + easing | Curve editor keyframe, copy/paste, tracking |
| Transisi batas klip (tipe + durasi, min 100 ms) | Preview transisi real-time, transisi AI (BUTUH BACKEND) |
| Filter preset matriks + intensity | LUT file eksternal, curves, color wheels |
| Adjust 8 slider + 8 band HSL | Vignette/HSL di preview (diterapkan saat export) |
| Efek lokal 9 item + intensity + stack | Effect pack format, AR/face penuh (BUTUH BACKEND) |
| Mask 7 bentuk + feather/invert/posisi + chroma key | Brush mask, chroma key di preview (diterapkan saat export) |

## 2. Pola bersama — `ToolCommon.kt`

Semua tool screen memakai satu skeleton `ClipToolScreen` agar konsisten dan
tidak menulis ulang scaffold:

```
┌ TopBar: judul tool · nama klip + durasi · tombol kembali ─┐
│                                                            │
│  ToolPreviewCard (thumbnail klip + badge state)            │
│  ToolSection "kontrol 1"  → ToolSlider / ToolChipRow /     │
│                              ToolValueRow / ToolHint       │
│  ToolSection "kontrol 2"                                   │
│  …                                                         │
└────────────────────────────────────────────────────────────┘
```

- `ToolPreviewCard` menampilkan thumbnail klip sekali per screen (dari
  `container.mediaThumbnails`), memakai `ColorFilter` matriks untuk filter/
  efek/adjust, `imageModifier` untuk blur efek, dan `overlay` untuk mask.
- `ToolSlider` menulis state lokal (`draft`) di `onValueChange` dan commit ke
  repository di `onValueChangeFinished` — satu tulis per gestur, bukan per
  frame.
- `ToolChip` = pilihan kategori/tipe; `ToolValueRow` = info; `ToolHint` =
  catatan kebijakan (termasuk penanda **BUTUH BACKEND**).
- Setiap screen punya `ClipToolViewModel` sendiri dengan `Mutex`
  read-modify-write; route `clipTool(route)` membawa `projectId` + `clipId`.

### 2a. Layar M4–M10

| Layar | Kontrol | Commit |
|---|---|---|
| Trim | RangeSlider start/end + label sumber | `setTrim` di drag-end |
| Speed | Slider kecepatan, toggle reverse, curve editor | `setSpeed`/`setCurve` di drag-end |
| Keyframe | Playhead + daftar properti + slider nilai + easing | `addKeyframe`/`setPropertyValue`/`setKeyframeEasing` |
| Transition | Chip sisi (Masuk/Keluar) + slider durasi + tile tipe | `setTransition` di aksi selesai |
| Filters | Slider intensity + grid preset 8 + hapus | `setFilter` di drag-end |
| Adjust | 8 slider + 8 band HSL + reset | `setColor` di drag-end |
| Effects | Slider intensity + kategori + grid 9 + stack | `setEffects` di drag-end |
| Mask | Chip 7 bentuk + feather/posisi/invert + chroma key | `setMask`/`setChroma` di drag-end |

## 3. State tool

| State | Tampilan |
|---|---|
| Klip tidak ditemukan (route basi) | `ClipToolScreen` menampilkan kosong + tombol kembali |
| Tanpa efek aktif | Hint "Pilih filter/efek dulu", grid tetap bisa dipilih |
| Efek/transisi aktif | Slider intensity/durasi aktif + tombol hapus |
| Klip < 100 ms + transisi | Kontrol transisi disembunyikan + hint minimum |
| BUTUH BACKEND | Badge amber di tile; tetap bisa dipilih dengan fallback lokal |
| Commit gagal (disk penuh) | State lokal dipertahankan; error tidak menutup layar |

## 4. Kebenaran data (aturan shared)

- **Trim** clamp ke tetangga dan sumber; snap hanya sisi yang digeser.
- **Split** men-seed keyframe kedua paruh dengan nilai sampel di titik potong.
- **Retime** men-scaling keyframe sebanding durasi baru dan ripple klip
  berikutnya.
- **Transisi** selalu di-repair di `recalculate`: clamp ke sisi terpendek,
  drop bila tetangga hilang atau < 100 ms.
- **Filter → adjust → efek** berurutan seperti rantai CSS `filter:`; efek
  bertumpuk sesuai urutan list.
- **Vignette/HSL band** disimpan untuk export, tidak mempengaruhi preview
  matriks (dicatat jujur di UI).

## 5. Preview yang jujur

Media3 1.9.0 memetakan satu sequence visual + satu sequence audio. Overlay,
teks, efek, transisi, mask, dan chroma **tidak** dirender di preview editor;
tiap tool screen menampilkan pratinjau Compose sendiri (matriks warna, blur,
scrim mask). Export (M13) menerapkan pipeline penuh. Reverse/pitch tidak
dirender. Semua keterbatasan ini ditandai di UI dan di `PROGRESS.md`, bukan
disembunyikan.

## 6. Aksesibilitas & konsistensi

- Kontrol minimal 44dp; label slider selalu ada; chip punya `selected` visual.
- Semua teks tool memakai copy Indonesia, konsisten dengan mockup ACC.
- Tidak ada ikon `material-icons-extended`; elemen dekoratif digambar dengan
  `Canvas`/vektor lokal (mis. diamond keyframe, garis mask putus-putus).
