# DECISIONS — Zcut

Satu baris per keputusan: keputusan + alasan. Ditulis otomatis oleh agent ketika
ada keputusan yang tidak boleh diambil sendiri atau ketika ada konflik aturan.

| Tanggal | Keputusan | Alasan |
|---|---|---|
| 2026-10-23 | Penyimpanan pakai JSON/DataStore, bukan Room | Cukup untuk puluhan draft; menghindari KSP + audit migrasi blocking di tiap perubahan skema. Reversibel. |
| 2026-10-23 | Dependency berat boleh (ML Kit, CameraX, Lottie), tanpa batas ukuran APK | Semua fitur diperlakukan setara; paritas fitur lebih penting daripada ukuran unduhan. |
| 2026-10-23 | Speed curve: offset sumber dihitung dari integral kurva kecepatan | Kurva jadi sumber kebenaran tunggal, bukan sekadar remap di atas `speed` skalar. |
| 2026-10-23 | Seam keyframe: interpolasi di titik potong | Animasi lintas titik potong tidak lagi "dibekukan" di paruh kiri. |
| 2026-10-23 | GitHub Actions di-pin ke commit SHA; gerbang Environment reviewer ditunda | Mencegah tag upstream digeser/di-takeover; gerbang Environment belum dibutuhkan sekarang. |
| 2026-10-23 | Release hanya dibuat saat push tag `v*`, bukan tiap push ke `main` | Commit fitur cukup menjalankan unit test (cepat); release lama tidak tertimpa. `versionName` mengikuti nama tag. |
| 2026-10-23 | Release di-gate di tag, tapi `versionCode` dinaikkan manual tiap rilis | Diminta aturan proyek; `versionName` dari tag tidak cukup untuk Play/upgrade. |
| 2026-10-23 | Konflik merge AGENTS.md: URL repo memakai versi baru (`Editors.git`) walau remote aktual `Zcut.git` | Aturan merge: versi baru menang; remote aktual dicatat di AGENTS.md agar tidak menyesatkan. |
| 2026-10-23 | Konflik merge AGENTS.md: telegram_notify diganti "kalau tool tersedia, kalau tidak minta ACC di chat" | Tool `telegram_notify` tidak ada di harness ini; versi baru menang supaya ACC tidak macet. |
| 2026-10-23 | Konflik merge AGENTS.md: alur per milestone (tanpa explore/planner wajib) jadi alur operasional harian, tapi gate review BLOCKING tetap berlaku | Gate mutu tidak boleh dilemahkan oleh instruksi repo; explore/planner jadi advisory. |
| 2026-10-23 | `migration-guard` diperlakukan tidak berlaku selama proyek tidak memakai Room | Tanpa skema Room, tidak ada migration path yang bisa rusak. |
| 2026-10-23 | Subagent `design` sedang tidak tersedia (model down) → spesifikasi desain ditulis langsung sebagai dokumen di `docs/` | Menjaga prinsip "desain dulu sebelum build" tanpa memblokir progres. |
| 2026-10-23 | Dependency media3 ditunda ke M3 (dihapus dari `build.gradle.kts` di M0) | Belum ada satu pun pemakaian; `isMinifyEnabled=false` bikin module yang tidak dipakai ikut masuk APK. |
| 2026-10-23 | Layar dengan milestone belum selesai diarahkan ke `PlaceholderScreen`, bukan disembunyikan | Semua fitur bebas; "belum dibangun" bukan "dikunci". |
| 2026-10-24 | Preferensi disimpan di DataStore terpisah (`zcut_preferences`), bukan di dokumen draft | Payload draft yang rusak tidak boleh ikut menghapus tema/bahasa/default ekspor; dua store juga membuat audit migrasi draft tidak menyentuh setelan. |
| 2026-10-24 | Pilihan bahasa disimpan di M1, resource terjemahan menyusul di M28 | Menghindari dua kali sentuh seluruh layar; M28 memang milestone lokalisasi. Sampai saat itu copy tetap Indonesia. |
| 2026-10-24 | Kegagalan tulis preferensi ditelan (UI tetap menampilkan nilai tersimpan), error ke pengguna masuk backlog | Layar pengaturan tidak boleh crash karena disk penuh; menampilkan nilai yang benar-benar persisten lebih jujur daripada state optimistis. |
| 2026-10-24 | Splash menunggu preferensi maksimal 3 detik lalu jatuh ke default | Lebih baik pengguna lama melihat onboarding sekali (fail-open) daripada splash menggantung selamanya saat DataStore error/lambat. |
| 2026-10-24 | M2 menyimpan material di `Project.assets` sebagai field aditif, tanpa naik `schemaVersion` | Dokumen M0/M1 decode dengan daftar kosong; tidak ada migrasi yang perlu ditulis atau diaudit. |
| 2026-10-24 | Editor M2 read-only; hanya media picker yang menulis draft | Menghindari dua penulis yang saling menimpa dokumen; editor mengamati repository sehingga impor langsung terlihat. |
| 2026-10-24 | Media picker memakai Photo Picker + SAF, bukan grid galeri penuh | Nol izin `READ_MEDIA_*`, sesuai F02 dan prinsip privasi; grid menampilkan item terpilih, struktur mockup dipertahankan. Lihat `docs/ui-m2-design.md`. |
| 2026-10-24 | Tool strip editor memakai label teks, bukan ikon | `material-icons-core` tidak punya scissor/music/filter/mask; `material-icons-extended` membengkakkan APK tanpa R8. |
| 2026-10-24 | Thumbnail disimpan relatif di `cacheDir/thumbs` dan dibuat ulang dari salinan privat bila hilang | "Bersihkan cache" boleh menghapus thumbnail tanpa merusak draft; file asli tetap di `filesDir/media`. |
| 2026-10-24 | Batas impor: 2 GB per file, sisa ruang ≥ ukuran + 256 MB, maksimal 20 item per batch | Mencegah provider yang salah lapor ukuran memenuhi volume; batas item menjaga batch tetap responsif. |
| 2026-10-24 | Foto tanpa durasi memakai 3 detik; video/audio tanpa metadata memakai 1 detik | Draft tidak boleh berisi klip nol detik yang tak terlihat/tak bisa dipilih. |
| 2026-10-24 | Gate review M2 dijalankan manual + CI, bukan subagent | Subagent security/bug/performance gagal karena batas pemakaian harness. Audit manual menutup temuan yang sama dan dicatat di `PROGRESS.md`; CI tetap jadi verifikasi kompilasi/tes. Begitu subagent tersedia lagi, milestone berikutnya kembali ke gate otomatis. |
| 2026-10-25 | Media3 dipin ke 1.9.0, bukan 1.10+/1.11 | 1.10+ menuntut `compileSdk 36`; proyek memakai compileSdk 35, AGP 8.7.3, Gradle 8.9. 1.9.0 sudah punya `CompositionPlayer` dan `SpeedProvider`. |
| 2026-10-25 | Preview M3 memakai `CompositionPlayer` dengan satu sequence visual + satu sequence audio | Batas pemetaan Media3 1.9.0; overlay/teks/efek/caption/sticker dan track audio tambahan menyusul di milestone masing-masing. |
| 2026-10-25 | `EditedMediaItem.durationUs` = `sourceStart + sourceDuration` untuk klip ter-clip | `CompositionPlayer` mengurangkan clipping start; memakai durasi klip saja membuat presentasi terpotong. |
| 2026-10-25 | Gambar memakai durasi `durationMs * speed`, bukan `sourceDurationMs` tersimpan | Gambar tidak punya durasi sumber intrinsik; speed harus mengubah panjang presentasi, bukan panjang sumber. |
| 2026-10-25 | Play dengan klip terpilih memutar `startMs..endMs`; seek manual ke luar rentang membatalkan range | Meniru preview range CapCut dan mencegah ticker menarik playhead kembali ke ujung range. |
| 2026-10-25 | Playhead diekspos sebagai `StateFlow<Long>` dan dibaca di fase layout | 30 fps hanya meng-invalidate penempatan playhead; klip dan ruler tidak ikut recompose. |
| 2026-10-25 | Player diekspos sebagai `StateFlow<Player?>` dan dikumpulkan di `PreviewPanel` | Perubahan play/FPS tidak boleh merecompose timeline; `PlayerView` tetap mendapat player terbaru. |
| 2026-10-25 | Editor pause saat `ON_STOP`/dispose; rebuild player ditunda saat tersembunyi | ViewModel hidup di back stack; tanpa ini audio/decoder/GL tetap jalan di layar lain dan impor membangun player untuk layar tak terlihat. |
| 2026-10-25 | Global playback speed ditunda ke M5 | `CompositionPlayer` 1.9.0 tidak mengiklankan `COMMAND_SET_SPEED_AND_PITCH`; speed per klip tetap jalan lewat `SpeedProvider`. |
| 2026-10-25 | Sumber preview hanya `filesDir` kanonik atau `content://` | Mencegah draft rusak membaca file internal lain, symlink/`..`, `file://`, atau URL network; `build()` mewajibkan root non-blank. |
| 2026-10-25 | Gap ekor dihitung per sequence dan ditambahkan ke video serta audio | Durasi composition = max durasi sequence; tanpa padding, klip terakhir yang di-skip membuat seek ke ekor timeline mental mundur. |
| 2026-10-25 | Metrik performa hanya FPS; `droppedFrames` dihapus | Tidak pernah ditampilkan dan menambah update state; FPS cukup untuk indikator pratinjau. |
| 2026-10-25 | GIF/Live Photo dipratinjau sebagai satu frame diam di M3 | `ImageRenderer` Media3 menampilkan frame pertama container; animasi GIF butuh pipeline sendiri di milestone sticker/media lanjutan. |
| 2026-10-25 | Review gate dijalankan per batch (M4–M10, lalu M11–M15, lalu per 5 milestone), bukan per milestone | Permintaan langsung pengguna 2026-10-25: reviewer lambat; commit/CI tetap per milestone, gate security/bug/performance tetap BLOCKING di akhir tiap batch sebelum rilis batch. |
| 2026-10-25 | Tool editor memakai route ber-argumen `projectId` + `clipId`, bukan state global | Layar tool harus bisa dibuka dari editor mana pun dan tetap benar setelah process death; argumen navigasi eksplisit lebih aman daripada singleton "draft aktif". |
| 2026-10-25 | Perubahan slider/gesture disimpan saat aksi selesai (drag end / slider release), bukan tiap frame | Menghindari ratusan tulis DataStore dan rebuild preview per detik; UI memakai state lokal selama gesture. |
| 2026-10-25 | Temuan Medium/Low batch M4–M10 diperbaiki sekaligus walau semua reviewer `BLOCKING: no` | Ini batas rilis batch; bug yang menyentuh state timeline bersama (trim, keyframe, transisi) paling murah dibetulkan sebelum release daripada jadi utang yang ketauan telat. |
| 2026-10-25 | Trim snap hanya diterapkan ke sisi yang benar-benar digeser | Snap dua sisi membuat drag satu handle bisa menarik sisi satunya atau membatalkan gestur (klip membesar diam-diam). |
| 2026-10-25 | SplitClip men-seed kedua paruh dengan nilai sampel di titik potong, termasuk paruh kiri dapat keyframe di seam | Paruh kiri yang tanpa keyframe di seam memotong ramp jadi "hold"; sampel di titik potong menjaga bentuk animasi (keputusan seam keyframe 2026-10-23 diterapkan penuh). |
| 2026-10-25 | RetimeClip men-scaling waktu keyframe sebanding durasi baru | Keyframe berpagar presentasi; tanpa scaling, keyframe melewati durasi baru dan animasi drift tak terlihat. |
| 2026-10-25 | Repair transisi dipusatkan di `recalculate` (choke point semua mutasi) | Delete/duplicate/move/split meninggalkan transisi menggantung di batas yang sudah hilang; satu titik repair lebih kecil dan konsisten daripada per-op. |
| 2026-10-25 | `sanitize` meng-drop klip yang overflow ujung timeline, bukan "stuck" di `Long.MAX_VALUE` | Klip di `MAX` tidak pernah lolos `isValid` di jalur edit berikutnya → permanen tak bisa diedit; drop lebih jujur. |
| 2026-10-25 | MaskScreen memakai draft lokal untuk semua kontrol (slider/chip/invert), commit di aksi selesai | Nilai committed yang dipakai sebagai `value` slider membuat thumb tidak mengikuti jari; chip yang tidak sinkron draft bisa ditimpa commit slider berikutnya. |
| 2026-10-25 | Transisi disembunyikan + hint saat kedua klip < 100 ms | `setTransition` menolak di bawah minimum; tile yang no-op tanpa umpan balik = dead control. |
| 2026-10-25 | Thumbnail wajib resolusi di bawah `cacheDir`/`filesDir` kanonik; hanya `content://` diterima | Draft adalah input tak tepercaya; ini menutup satu-satunya gap path gate vs `PreviewPlan` (thumbnail/localPath/uri dibaca tanpa contain). |
| 2026-10-25 | `speedAtNormalized` + `presentationDurationMsNormalized` untuk hot path (tanpa re-normalisasi) | Curve editor memanggil `speedAt` 73×/frame dan player tiap frame; sort+dedupe+closure ulang per panggilan adalah churn CPU/GC yang nyata. |
| 2026-10-25 | Thumbnail: dedup in-flight + `Semaphore(3)`, bukan tanpa batas | N klip → N retriever native paralel; split mendecode file sama dua kali. Satu decode per key, maksimal 3 decoder bersamaan. |
| 2026-10-25 | Rebuild player sinkron di main thread, mutex lintas-ViewModel, dan virtualisasi timeline ditunda ke backlog | Ketiganya refactor besar di atas arsitektur M3/M4 yang berfungsi; catatan `ponytail:` dan backlog menandai ceiling + jalur upgrade. |
| 2026-10-25 | Teks M11 = `ClipType.TEXT` di track `TEXT` yang sudah ada, bukan model overlay baru | Model `TextConfig` sudah ada sejak Stage 2b; memakai ulang track membuat teks ikut sanitize/repair timeline yang sama dan tidak menambah konsep baru. |
| 2026-10-25 | Satu track `TEXT`; teks baru yang bentrok waktu digeser ke slot bebas berikutnya | Track teks bertumpuk butuh z-order + pemilihan layer yang belum ada di M11; push-right adalah perilaku "fix clash" yang sudah dipakai timeline. |
| 2026-10-25 | Field M11 (`stylePreset`, `gradientColorArgb`, `letterSpacingEm`, `lineHeight`) ditambahkan aditif tanpa naik `schemaVersion` | Semua punya default; dokumen M0–M10 decode tanpa migrasi. Sama dengan pola `Project.assets`. |
| 2026-10-25 | Preview teks memakai Compose, bukan Media3 | Media3 1.9.0 hanya memetakan satu sequence visual + satu audio; teks harus composited di atas frame. Burn-in ke MP4 ditunda ke M13. |
| 2026-10-25 | `TextLayout.REFERENCE_FRAME_HEIGHT_DP = 260f` jadi kontrak ukuran preview↔export | Tanpa konstanta bersama, teks akan berubah ukuran antara preview dan hasil export; M13 wajib memakai nilai yang sama. |
| 2026-10-25 | `fontSizeSp` tetap nama lama walau diskalakan terhadap frame referensi | Menghindari migrasi nama field; dokumentasi dan `TextLayout` menjelaskan arti sebenarnya. |
| 2026-10-25 | Import font SAF ditunda; M11 hanya font sistem (Display/Serif/Mono/Hand) | SAF + persistable permission + fallback font menambah permukaan izin/IO yang tidak diperlukan untuk paritas dasar; chip Import tampil nonaktif + hint jujur. |
| 2026-10-25 | Template teks multi-layer dan AI font keluar dari M11; AI font ditandai BUTUH BACKEND | Keduanya butuh model data/editor baru atau model generatif on-device yang tidak ada; menandainya jujur lebih baik daripada kontrol palsu. |
| 2026-10-25 | Autosave teks memakai debounce 400 ms + guard "tidak berubah = tidak menulis" | Mencegah tulis DataStore tiap ketikan dan mencegah upsert no-op saat layar baru dibuka. Tombol commit tetap memaksa simpan lalu kembali. |
| 2026-10-25 | `PreviewPlan.sameMediaAs` + early-return di `Media3PreviewEngine.prepare` | Edit teks/gaya tidak mengubah sequence media; tanpa guard, tiap ketikan membongkar GL pipeline dan preview berkedip hitam. |
| 2026-10-25 | Subagent `design` kembali gagal (model unavailable) untuk M11; `docs/ui-m11-design.md` ditulis manual | Gate desain tetap dijalankan sebagai dokumen; kegagalan tool dicatat agar tidak terlihat seperti desain otomatis. |
