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
