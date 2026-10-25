# Desain UI M3 — Preview Player

Spesifikasi desain untuk milestone **M3**: pemutaran pratinjau timeline dengan
Media3. Basis visual tetap mockup Stage 2 yang di-ACC
(`mockup/assets/screens-core.js` → `RENDER.editor`). Tidak ada aset, font,
atau ikon CapCut yang dipakai.

## 1. Lingkup M3

| Masuk M3 | Di luar M3 |
|---|---|
| Pemutaran video/foto/audio dari timeline | Overlay, teks, caption, sticker, efek, filter (milestone masing-masing) |
| Play/pause, mute, playhead sinkron | Mixing beberapa track audio (M12) |
| Preview range klip terpilih | Global playback speed (M5); speed per klip sudah jalan |
| FPS terukur saat memutar | Dropped-frame counter (dihapus, tidak ditampilkan) |
| Fallback thumbnail sampai frame pertama | Scrub frame-accurate saat drag (M4/M13) |
| Pause saat editor ditinggalkan | Export/render (M13) |

M3 memutar **satu sequence visual + satu sequence audio**. `CompositionPlayer`
Media3 1.9.0 memang dibatasi dua sequence pada pemetaan ini; layer lain tetap
tampil di timeline dan baru ikut ter-composite di milestone-nya.

## 2. Anatomi preview

```
┌ frame 9:16 (maks 42% tinggi body) ────────────────────────┐
│                                                            │
│              [ surface Media3 / thumbnail ]                │
│                                                            │
│                        ( ▶ / ❚❚ )  52dp                   │
│                                                            │
│  9:16 · 00:24                         ( 🔊 )              │
│  00:03 · 30 fps                                            │
└────────────────────────────────────────────────────────────┘
```

- Frame memakai rasio project (`AspectRatioFrameLayout.RESIZE_MODE_FIT`),
  sudut 14dp, latar `surfaceVariant`.
- `PlayerView` `useController=false`; kontrol dibuat di Compose agar konsisten
  dengan tema dan bisa diakses.
- Scrim gradien hitam 64dp di bawah supaya timestamp tetap terbaca di frame
  terang.
- Badge rasio + durasi di kanan atas; badge error di kiri atas bila pratinjau
  gagal.
- Tombol play 52dp di tengah; ikon pause kustom (`ZcutIcons.Pause`) supaya
  tidak menambah `material-icons-extended`.
- Tombol mute 36dp di kanan bawah, hanya muncul bila ada audio.
- FPS hanya tampil saat memutar dan sudah terukur (bukan angka awal yang
  menyesatkan).

## 3. State preview

| State | Tampilan |
|---|---|
| Belum ada media | CTA "Tambah media" |
| Ada media, player belum siap | Thumbnail cover (bukan layar hitam) |
| Frame pertama sudah render | Surface player, overlay kontrol |
| Audio-only | Panel "Pratinjau audio" + tombol putar |
| Gagal menyiapkan player | Badge "Pratinjau tidak tersedia"; timeline tetap bisa diedit |
| Timeline habis | Playhead di ujung; tekan play mengulang dari awal |

`hasFrame` dipisahkan dari `isReady`: surface Media3 hitam sampai frame
pertama, jadi thumbnail tetap menutupinya. Untuk foto, `ImageRenderer` tidak
mengirim callback `VideoFrameMetadataListener`; `hasFrame` di-set saat player
`STATE_READY` dan item visual di playhead adalah gambar.

## 4. Perilaku pemutaran

- **Tanpa seleksi**: play dari posisi playhead sampai akhir timeline.
- **Dengan klip terpilih**: play hanya rentang `clip.startMs..clip.endMs`.
  Playhead yang berada di luar rentang melompat ke awal rentang.
- **Seek manual** ke luar rentang membatalkan range; tanpa itu ticker akan
  menarik playhead kembali ke ujung range.
- **Menyentuh ujung** (timeline atau range): player pause di ujung, ticker
  berhenti. Play berikutnya mengulang dari awal rentang/timeline.
- **Mute** hanya mengubah volume player; state track tidak ditulis ke draft.
- **Pindah layar/background**: `ON_STOP` dan dispose memanggil pause. Player
  tetap hidup di back stack sampai ViewModel di-`onCleared`, tetapi tidak
  memutar audio di layar lain.
- **Perubahan draft saat editor tersembunyi** (mis. impor dari picker): rebuild
  player ditunda sampai `ON_START`, supaya pipeline GL tidak dibangun untuk
  layar yang tidak terlihat.

## 5. Arsitektur

```
Project ──► PreviewPlanBuilder (murni) ──► PreviewPlan
                                              │
                                              ▼
                                  Media3CompositionBuilder
                                              │
                                              ▼
                                  CompositionPlayer (1 video + 1 audio)
                                              │
              ┌───────────────┬───────────────┼────────────────┐
              ▼               ▼               ▼                ▼
        positionMs       state (play,     playerFlow       frame metadata
        (30 Hz)          frame, FPS)     (PlayerView)     (FPS, first frame)
              │               │
              ▼               ▼
     TimelineCanvas     PreviewPanel
     (layout offset)    (composition)
```

- `PreviewPlan` memetakan klip ke item dengan gap, rentang sumber, speed, dan
  durasi presentasi. Semua keputusan yang bisa salah diuji di JVM.
- `Media3CompositionBuilder` hanya menerjemahkan plan ke `Composition`:
  `ClippingConfiguration` untuk trim, `setImageDurationMs` untuk gambar,
  `setDurationUs` pre-clipping, `SpeedProvider` konstan per klip, dan gap
  (termasuk gap ekor) agar durasi composition = durasi timeline.
- `Media3PreviewEngine` memiliki `CompositionPlayer`, ticker playhead, FPS,
  mute, range, dan error state. Semua operasi player di main thread.
- `EditorViewModel` menyiapkan ulang player hanya saat timeline/aset berubah,
  bukan saat rename atau save lain.
- Playhead adalah `StateFlow<Long>` terpisah. `TimelineCanvas` membacanya di
  dalam `Modifier.offset { }` (fase layout), jadi 30 fps hanya meng-invalidate
  penempatan playhead, bukan seluruh klip. Hanya `PreviewClock` yang membaca
  posisi saat composition.

## 6. Batasan Media3 1.9.0

- `CompositionPlayer` masih `@ExperimentalApi`; `Composition` dan
  `EditedMediaItem` `@UnstableApi`. Semua pemakaian diberi `@OptIn`.
- Versi 1.10+ membutuhkan `compileSdk 36`; proyek memakai 35, jadi dipilih
  1.9.0 (lihat `DECISIONS.md`).
- Satu video sequence + satu audio sequence. Overlay/teks/efek belum
  ter-composite.
- Semua item wajib punya `durationUs`. Untuk klip ter-clip, nilainya
  `sourceStart + sourceDuration` karena `CompositionPlayer` mengurangkan
  clipping start.
- Gambar memakai durasi `durationMs * speed`; durasi sumber yang tersimpan
  diabaikan agar edit speed berikutnya tetap benar.
- GIF/Live Photo dipratinjau sebagai satu frame diam.
- Seek mengikuti keyframe Media3. Frame-accurate scrub/export menyusul di M4
  dan M13.
- `CompositionPlayer` tidak mengiklankan `COMMAND_SET_SPEED_AND_PITCH`; global
  playback speed kembali di M5. Speed per klip tetap bekerja lewat
  `SpeedProvider`.
- Sumber hanya boleh `filesDir` (kanonik, anti `..`/symlink) atau
  `content://` dari picker. `file://`, network, dan path di luar workspace
  ditolak.

## 7. Aksesibilitas & konsistensi

- Semua tombol punya `contentDescription` Indonesia ("Putar", "Jeda",
  "Bisukan suara", "Nyalakan suara").
- Target sentuh play 52dp, mute 36dp (di atas minimum 24dp Material).
- FPS memakai `labelSmall` dan hanya informatif; tidak menggeser layout
  karena ruang kanan sudah dialokasikan.
- Warna kontrol selalu putih di atas scrim hitam, tidak bergantung tema.
- Tidak ada animasi loop; semua state seek-safe.

## 8. Verifikasi

- Unit test murni: `PreviewPlanTest` (gap, overlap, aset hilang, mute, gambar,
  speed, allowlist sumber, gap ekor) dan `PreviewSeekTest` (clamp, range,
  play-from, reached-end, range-after-seek).
- Kompilasi dan test dijalankan di GitHub Actions (`:app:testDebugUnitTest`),
  bukan lokal.
- Uji perangkat manual: video+audio, foto-only, audio-only, klip terpilih,
  seek ke ujung, background/pindah layar, dan draft dengan media hilang.
