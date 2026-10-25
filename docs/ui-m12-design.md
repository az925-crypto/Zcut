# Desain UI M12 — Audio dasar & TTS sistem

Spesifikasi desain untuk milestone **M12**. Basis visual tetap mockup Stage 2
yang di-ACC (`mockup/assets/screens-tools.js` → `RENDER.audio` dan
`RENDER.tts`). Tidak ada aset, ikon, atau library audio CapCut yang dipakai.

Catatan kejujuran: subagent `design` gagal dipanggil (model unavailable), sama
seperti M11. Dokumen ini ditulis manual berdasarkan mockup ACC; kegagalan
dicatat di `DECISIONS.md`.

## 1. Lingkup M12

| Masuk M12 | Di luar M12 |
|---|---|
| Volume, fade in/out, mute, loop, pitch per clip audio | Mixer/normalisasi diterapkan saat export (M13) |
| Normalisasi loudness lokal (target ≈ -16 dBFS) | Denoise realtime (butuh DSP native) |
| Deteksi beat lokal dari waveform + snap ke beat | Auto-cut ke beat (M13) |
| Rekam voiceover dari mikrofon perangkat | Monitor rekaman (headphone) |
| TTS sistem: skrip, daftar suara, rate, pitch, pratinjau, generate + sisip | Voice clone (BUTUH BACKEND) |
| Tambah audio lewat Media Picker (impor lagu) | Pustaka musik & SFX bawaan |
| Pemilihan klip audio + hapus klip | Ekstraksi audio dari video |

Satu track `AUDIO` dipakai untuk semua klip audio (impor, voiceover, TTS). Klip
baru yang bentrok waktu digeser ke slot bebas berikutnya, mengikuti perilaku
"fix clash" timeline yang sudah ada.

## 2. Anatomi layar

### 2a. `audio` (F16)

```
┌ TopBar: "Audio" · subjudul "Music + voiceover · mix lokal" · [Selesai] ┐
│                                                                        │
│  [notice / hint bila ada]                                              │
│  Klip audio   [lagu] [Voiceover] [TTS · teks…]                         │
│  [Tambah audio]            [Text to speech]                            │
│                                                                        │
│  Klip terpilih                                                         │
│    Nama  midnight_drive                                                │
│    Mulai 00:00 · Durasi 00:24                                          │
│    Volume   ─────────●────  72%                                        │
│    Fade in  ─────●────────  800 ms                                     │
│    Fade out ────────●─────  1200 ms                                    │
│    Pitch    ────────●─────  1.00×                                      │
│    [Mute] [Loop]                                                       │
│    [Normalisasi volume]                                                │
│    [Hapus klip]                                                        │
│                                                                        │
│  Beat sync                                                             │
│    Snap ke beat                                  (switch)              │
│    Marker 24 beat                                                      │
│    [Deteksi beat dari klip]                                            │
│                                                                        │
│  Voiceover                                                             │
│    Rekam narasi dari mikrofon perangkat.                               │
│    [Rekam voiceover] / [Stop rekaman · 00:03]                          │
│                                                                        │
│  Catatan jujur  (3 hint)                                               │
└────────────────────────────────────────────────────────────────────────┘
```

### 2b. `tts` (F17)

```
┌ TopBar: "Text to speech" · "Android TTS · suara lokal" · [Selesai] ┐
│                                                                     │
│  Skrip                                                              │
│    [textarea multiline]                                             │
│    Perkiraan  62 / 1000 · 00:04                                     │
│                                                                     │
│  Suara                                                              │
│    [Aria · en-US] [Kai · en-US] [Nova · id-ID] [Sol · en-GB] …       │
│    Terpilih  Nova · id-ID                                           │
│    [Pratinjau suara]                                                │
│                                                                     │
│  Nada bicara                                                        │
│    Rate  ────────●────────  1.00×                                    │
│    Pitch ────────●────────  1.00×                                    │
│                                                                     │
│  [Buat & sisipkan ke timeline]                                      │
│  Hasil  → status + hint posisi playhead (muncul setelah berhasil)    │
│  Catatan jujur  (2 hint)                                            │
└─────────────────────────────────────────────────────────────────────┘
```

### 2c. Komponen

| Komponen | Perilaku |
|---|---|
| Chip klip audio | Satu chip per klip `AUDIO`, label = nama file tanpa ekstensi (fallback `Audio N`). Tap = pilih klip. |
| Slider Volume | 0–200% dari `AudioEditor.MAX_VOLUME`. Commit saat drag selesai. |
| Slider Fade in/out | 0–3000 ms, juga dibatasi durasi klip. Nilai di-clamp ulang oleh domain saat disimpan. |
| Slider Pitch | 0.5×–2.0×. Label "Pitch (voice change)" agar jelas ini bukan voice clone. |
| Chip Mute/Loop | Toggle pada `AudioConfig`; mute menang atas volume saat mix (M13). |
| Tombol Normalisasi | Dekode 60 s pertama klip, ukur RMS, tulis gain hasil ke `volume`. |
| Switch Snap ke beat | Nonaktif bila grid kosong; menyalakan tanpa grid memberi hint, bukan state palsu. |
| Tombol Deteksi beat | Dekode klip terpilih, simpan grid ke `ProjectSettings.beatGridMs`, nyalakan snap, tampilkan jumlah beat + BPM. |
| Tombol Rekam voiceover | Minta izin `RECORD_AUDIO` saat perlu; timer berjalan selama rekaman; stop menyimpan file ke workspace privat di posisi playhead. |
| Textarea skrip | Multiline, maksimal 1.000 karakter, dengan penghitung + estimasi durasi. |
| Chip suara | Daftar dari `SpeechEngine.voices()`. Bila engine tidak memberi daftar, tampil satu entri "Bawaan perangkat" — bukan daftar kosong. |
| Tombol Pratinjau suara | `SpeechEngine.speak()` (tanpa file); dihentikan saat layar ditutup. |
| Tombol Buat | Sintesis ke cache lokal (kunci SHA-256 atas teks+suara+rate+pitch) lalu sisipkan sebagai klip audio di playhead. |

## 3. State

| State | Tampilan |
|---|---|
| Loading draft | Progress bar di tengah, top bar tetap. |
| Draft tidak ditemukan | Empty state + tombol kembali. |
| Belum ada klip audio | Hint "Belum ada audio di timeline" + arahan impor/rekam. |
| Sedang menganalisis | Tombol analisis nonaktif (`busy`), slider tetap bisa dilihat. |
| Deteksi beat kosong | Pesan "Tidak ada beat yang terdeteksi pada klip ini." |
| Dekode gagal | Pesan "Audio tidak bisa didekode di perangkat ini." |
| Izin mikrofon ditolak | Pesan inline "Izin mikrofon ditolak." |
| Rekaman terlalu pendek | Pesan "Rekaman terlalu pendek untuk disimpan." |
| TTS engine tidak ada | Pesan "Mesin TTS perangkat tidak tersedia." (tidak crash) |
| Generate berhasil | Kartu "Hasil" + status sudah masuk timeline. |

## 4. Draft vs commit

- Mutasi audio memakai `Mutex` read-modify-write yang sama dengan tool lain;
  satu tulis per aksi selesai, bukan per frame.
- Slider memakai state lokal (`mutableFloatStateOf`) dan commit lewat
  `onValueChangeFinished`.
- Penulisan yang tidak mengubah draft tidak menulis DataStore (guard
  `updated == current`).

## 5. Preview yang jujur

- Preview Media3 memutar audio sumber apa adanya: volume/fade/mute/pitch belum
  terdengar di preview, baru diterapkan oleh mixer export (M13). Ini disebutkan
  di "Catatan jujur" pada layar Audio.
- Analisis beat/loudness memakai dekode lokal (`PcmDecoder`, maksimum 60 detik)
  di `Dispatchers.IO`; tidak ada audio yang diunggah.
- Pustaka musik dan SFX bawaan belum ada; pengguna mengimpor berkas sendiri.
- Voice clone suara tertentu ditandai **BUTUH BACKEND**; TTS sistem tetap gratis.

## 6. Aksesibilitas & konsistensi

- Target sentuh minimal 44dp; chip menyatakan `selected` lewat warna, switch
  memakai kontrol Material standar.
- Semua slider punya label teks dan nilai numerik.
- Tombol rekam menyatakan state (label berubah + timer) agar tidak ambigu.
- Copy Indonesia, konsisten dengan mockup ACC dan layar tool M11.
- Tidak memakai `material-icons-extended`; ikon dekoratif dari set core.
