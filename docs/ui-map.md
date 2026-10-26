# Peta UI — Mockup ke Android

Menjembatani mockup Stage 2 (42 layar) dengan implementasi Compose di
`app/src/main/java/com/zaaam/zcut/`. Setiap layar mockup punya satu rute di
`ui/navigation/ZcutDestinations.kt`, dan rute itu dipakai sebagai
`route` di `NavHost`.

Paritas dijaga oleh unit test `ZcutDestinationsTest`: kalau ada layar di
mockup tanpa rute Android (atau sebaliknya), CI gagal.

## Kebijakan placeholder

Rute yang milestone-nya belum selesai diarahkan ke `PlaceholderScreen`, yang
menampilkan judul, ID fitur, dan badge **BUTUH BACKEND** bila relevan. Tidak ada
layar yang disembunyikan atau dikunci di balik paywall — hanya belum dibangun.

## Layar

Legenda milestone: lihat tabel milestone di `PROGRESS.md`.

| Rute | Mockup | Milestone | Status | Composable |
|---|---|---|---|---|
| `splash` | splash | M0 | ✅ | `feature/splash/ZcutSplashScreen` |
| `onboarding` | onboarding | M0 | ✅ | `feature/onboarding/OnboardingScreen` |
| `home` | home | M0 | ✅ | `feature/home/HomeScreen` |
| `projects` | projects | M0 | ✅ | `feature/projects/ProjectsScreen` |
| `templates` | templates | M19 | ✅ | `feature/templates/TemplatesScreen` |
| `template-detail` | template-detail | M19 | ✅ | `feature/templates/TemplateDetailScreen` |
| `discover` | discover | M19 | ✅ | `feature/templates/DiscoverScreen` |
| `ai-studio` | ai-studio | M20 | ✅ | `feature/ai/AiStudioScreen` |
| `editor` | editor | M2–M3 | ✅ | `feature/editor/EditorScreen` |
| `media-picker` | media-picker | M2 | ✅ | `feature/media/MediaPickerScreen` |
| `trim` | trim | M4 | ⏳ | placeholder |
| `speed` | speed | M5 | ⏳ | placeholder |
| `keyframe` | keyframe | M6 | ⏳ | placeholder |
| `transition` | transition | M7 | ⏳ | placeholder |
| `filters` | filters | M8 | ⏳ | placeholder |
| `adjust` | adjust | M8 | ⏳ | placeholder |
| `effects` | effects | M9 | ⏳ | placeholder |
| `mask` | mask | M10 | ⏳ | placeholder |
| `matting` | matting | M14 | ✅ | `feature/tools/MattingScreen` |
| `beauty` | beauty | M15 | ✅ | `feature/tools/BeautyScreen` |
| `stickers` | stickers | M16 | ✅ | `feature/tools/StickerScreen` |
| `text` | text | M11 | ✅ | `feature/tools/TextScreen` |
| `captions` | captions | M17 | ✅ | `feature/tools/CaptionScreen` |
| `audio` | audio | M12 | ✅ | `feature/tools/AudioScreen` |
| `tts` | tts | M12 | ✅ | `feature/tools/TtsScreen` |
| `ai-music` | ai-music | M18 | ✅ | `feature/tools/AiMusicScreen` |
| `export` | export | M13 | ✅ | `feature/export/ExportScreen` |
| `export-done` | export-done | M13 | ✅ | `feature/export/ExportDoneScreen` |
| `drafts` | drafts | M27 | ⏳ | placeholder |
| `ai-clipper` | ai-clipper | M20 | ✅ | `feature/ai/AiClipperScreen` |
| `ai-agent` | ai-agent | M20 | ✅ | `feature/ai/AgentScreen` |
| `ai-generate` | ai-generate | M21 | ⏳ | placeholder |
| `digital-human` | digital-human | M22 | ⏳ | placeholder |
| `camera` | camera | M23 | ⏳ | placeholder |
| `photo-editor` | photo-editor | M23 | ⏳ | placeholder |
| `share` | share | M24 | ⏳ | placeholder |
| `profile` | profile | M1 | ✅ | `feature/profile/ProfileScreen` |
| `settings` | settings | M1 | ✅ | `feature/settings/SettingsScreen` |
| `split-screen` | split-screen | M25 | ⏳ | placeholder |
| `gameplay` | gameplay | M25 | ⏳ | placeholder |
| `live-photo` | live-photo | M25 | ⏳ | placeholder |
| `ad-maker` | ad-maker | M26 | ⏳ | placeholder |

## Navigasi

- `ZcutBottomNav` menampilkan lima tab: `home`, `templates`, `projects`,
  `ai-studio`, `profile`.
- Tab tanpa milestone selesai tetap bisa dibuka dan menampilkan placeholder.
- Layar non-tab menampilkan tombol kembali di `ZcutTopBar`.

## Komponen bersama

| File | Isi |
|---|---|
| `ui/theme/` | `Color.kt`, `Type.kt`, `Shape.kt`, `ZcutTokens.kt`, `Theme.kt` |
| `ui/components/ZcutScaffold.kt` | `ZcutScaffold`, `ZcutTopBar`, `ZcutBottomNav` |
| `ui/components/ZcutSurfaces.kt` | `ZcutCard`, `ZcutChip`, `BackendBadge`, `MetaRow`, `CoverPlaceholder` |
| `ui/components/ZcutLayout.kt` | `SectionHeader`, `EmptyState`, `HeroBanner` |
| `ui/components/ZcutControls.kt` | tombol primer/sekunder/ghost, `ZcutIconButton`, `ZcutSlider` |
| `ui/components/ProjectCard.kt` | `DraftCover`, `ProjectRailCard`, `ProjectRowCard` |
| `ui/components/ZcutRows.kt` | `ListRow`, `StatusBadge` |
| `ui/LocalAppContainer.kt` | `LocalAppContainer` + `zcutViewModelFactory` |

Spesifikasi desain M0 ada di [`ui-m0-design.md`](ui-m0-design.md).
Spesifikasi desain M2 ada di [`ui-m2-design.md`](ui-m2-design.md).
Spesifikasi desain M3 ada di [`ui-m3-design.md`](ui-m3-design.md).
Spesifikasi desain M11 ada di [`ui-m11-design.md`](ui-m11-design.md).

## Preview (M3)

Rute `editor` sekarang memutar timeline lewat Media3 `CompositionPlayer`.
Tidak ada rute baru: kontrol play/pause, mute, playhead, dan badge FPS berada
di panel preview yang sama.

| Bagian | Implementasi |
|---|---|
| Mapping timeline → composition | `core/render/PreviewPlan.kt`, `Media3CompositionBuilder.kt` |
| Player + ticker + FPS | `core/render/Media3PreviewEngine.kt` |
| Playhead tanpa recompose klip | `TimelineCanvas` membaca `StateFlow<Long>` di `Modifier.offset {}` |
| Range klip terpilih | `PreviewSeek.range` / `rangeAfterSeek` |
| Lifecycle | pause saat `ON_STOP`/dispose; rebuild ditunda saat editor tersembunyi |
| Sumber aman | hanya `filesDir` kanonik atau `content://` |

Batasan M3: satu sequence visual + satu sequence audio; overlay/teks/efek/
caption/sticker belum ter-composite; GIF tampil sebagai frame diam; global
speed menyusul di M5. Detail lengkap di [`ui-m3-design.md`](ui-m3-design.md).

## Preferensi (M1)

Semua preferensi disimpan di DataStore terpisah (`zcut_preferences`), bukan di
dokumen draft, supaya payload draft yang rusak tidak ikut menghapus setelan.

| Kunci | Tipe | Default | Dipakai oleh |
|---|---|---|---|
| `onboarding_seen` | Boolean | `false` | gerbang splash |
| `theme_mode` | `DARK` / `LIGHT` / `SYSTEM` | `DARK` | `MainActivity` → `ZcutTheme` |
| `language` | `ID` / `EN` | `ID` | disimpan; resource terjemahan menyusul di M28 |
| `export_resolution` | `Resolution` | `FHD_1080` | default ekspor (M13) |
| `export_frame_rate` | `24` / `30` / `60` | `30` | default ekspor (M13) |
| `export_audio_sample_rate` | `44_100` / `48_000` | `48_000` | default ekspor (M13) |

Nilai yang tidak dikenal/rusak jatuh ke default (`AppPreferences.fromStored`),
bukan memicu crash. Onboarding baru ditandai selesai setelah pengguna menekan
"Mulai"/"Lewati", jadi app yang dimatikan di tengah onboarding akan
menampilkannya lagi.

"Bersihkan" di Pengaturan hanya mengosongkan `cacheDir`; draft di `filesDir`
tidak disentuh (dijaga unit test `AppStorageTest`).
