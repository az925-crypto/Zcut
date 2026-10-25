# PROGRESS — Zcut 1:1 Parity

- **Repo:** https://github.com/az925-crypto/Zcut
- **Package:** `com.zaaam.zcut`
- **Target:** Android 8.0+ (minSdk 26), compile/target SDK 35
- **Prinsip:** semua fitur gratis, tanpa paywall/watermark, tanpa server CapCut.
- **Status sekarang:** Tahap 1 selesai, Tahap 2 mockup **di-ACC 2026-10-23**,
  Tahap 2b non-UI selesai, CI hijau, signed release `v0.1.0` sudah
  diterbitkan. **Tahap 3 (UI Android) berjalan: M0–M3 selesai, M4 berikutnya.**
- **Reviewer pre-commit:** security/bug/performance semuanya `BLOCKING: no`.
  Sisa temuan Medium/Low ada di bagian Backlog re-audit, bukan penghalang.

## Mockup Stage 2

- Lokasi: `mockup/index.html`
- Jalankan: `cd mockup && python3 -m http.server 8080`
- Preview: `http://127.0.0.1:8080/` · peta layar: `/?view=map`
- Cakupan: 42 layar untuk 31 area fitur, termasuk semua panel editor, AI,
  capture, export, share, dan settings.
- Status: **di-ACC 2026-10-23**. Lanjut ke Tahap 3 (UI Android).

## Status tahap

| Tahap | Status | Catatan |
|---|---|---|
| 1. DOC | ✅ Selesai | 31 spek fitur + index + arsitektur |
| 2. MOCKUP | ✅ Selesai, di-ACC 2026-10-23 | 42 layar · 31 area · HTML interaktif |
| 2b. Non-UI | ✅ Selesai | model, data layer, render contract, unit test, CI |
| 3. KODE UI | 🟡 Dikerjakan | ACC 2026-10-23 · fitur per fitur (M0–M3 selesai) |
| 4. BUILD/TEST | ✅ Selesai | CI hijau: test tiap push; signed release saat tag `v*` |
| 5. RELEASE | 🟡 Sebagian | `v0.1.0` (scaffold) sudah rilis; rilis fitur menyusul per tag |

## Tahap 3 — Milestone

Satu milestone = satu commit = satu run CI. Urutan P0 dulu, lalu P1/P2.

| # | Milestone | Fitur | Layar mockup | Status |
|---|---|---|---|---|
| M0 | Fondasi Compose, shell navigasi, daftar draft | F01/F20 (parsial) | splash, onboarding, home, projects | ✅ |
| M1 | Preferensi lokal, onboarding tersimpan, Profil & Pengaturan | F28/F29 | profile, settings | ✅ |
| M2 | Editor shell, kanvas timeline, media import | F01/F02 | editor, media-picker | ✅ |
| M3 | Preview player Media3 | F03 | (editor) | ✅ |
| M4 | Timeline editing: trim, split, move, snap, ripple | F04 | trim | [ ] |
| M5 | Speed, reverse, curve, motion | F05 | speed | [ ] |
| M6 | Keyframe | F06 | keyframe | [ ] |
| M7 | Transisi | F07 | transition | [ ] |
| M8 | Filter & color | F08 | filters, adjust | [ ] |
| M9 | Efek | F09 | effects | [ ] |
| M10 | Mask & chroma | F10 | mask | [ ] |
| M11 | Teks & tipografi | F14 | text | [ ] |
| M12 | Audio dasar + TTS sistem | F16/F17 | audio, tts | [ ] |
| M13 | Export & render (Media3 Transformer) | F19 | export, export-done | [ ] |
| M14 | Matting/cutout (ML Kit) | F11 | matting | [ ] |
| M15 | Retouch & beauty | F12 | beauty | [ ] |
| M16 | Sticker + Lottie | F13 | stickers | [ ] |
| M17 | Caption & speech lokal | F15 | captions | [ ] |
| M18 | AI music prosedural | F18 | ai-music | [ ] |
| M19 | Template & CutSame | F21 | templates, template-detail, discover | [ ] |
| M20 | AI clipper + agent lokal | F22/F23 | ai-clipper, ai-agent, ai-studio | [ ] |
| M21 | AI generation (BUTUH BACKEND) | F24 | ai-generate | [ ] |
| M22 | Digital human (BUTUH BACKEND) | F25 | digital-human | [ ] |
| M23 | Kamera + photo editor (CameraX) | F26 | camera, photo-editor | [ ] |
| M24 | Share & publish | F27 | share | [ ] |
| M25 | Split screen, gameplay, live photo | F30 | split-screen, gameplay, live-photo | [ ] |
| M26 | Ad maker | F31 | ad-maker | [ ] |
| M27 | Draft lanjutan: rename, trash, backup lokal | F20 | drafts | [ ] |
| M28 | Polish: aksesibilitas, lokalisasi, audit akhir | cross-cutting | — | [ ] |

Peta rute ↔ layar ↔ milestone ada di [`docs/ui-map.md`](docs/ui-map.md).

## Aturan implementasi

1. Semua fitur terbuka. Tidak ada billing, subscription, credit, reward ads,
   trial, quota, atau watermark.
2. Tidak ada network call ke `capcutapi.com`, CDN, atau layanan milik aplikasi
   asli.
3. Fitur yang butuh server/model ditandai **BUTUH BACKEND** dan tetap punya
   fallback lokal bila memungkinkan.
4. Tidak menyalin source, aset, font, template, effect, atau logo asli.
5. Build dan test berat hanya di GitHub Actions. Tidak ada Gradle lokal.
6. Signing release memakai GitHub Secrets; keystore/password/token tidak
   pernah masuk repo.
7. Setiap fitur selesai: update checklist ini + commit terpisah.
8. Jika CI gagal: baca log, perbaiki, push sampai hijau.

## Legenda

- `[x]` selesai
- `[~]` sedang dikerjakan
- `[ ]` belum
- `[!]` butuh backend / model eksternal
- `[-]` sengaja tidak dibuat (monetisasi/gating)

## Ringkasan cakupan

| Kategori | Jumlah area | Backend penuh | Catatan |
|---|---:|---|---|
| Core editor | 10 | 0 | draft, media, preview, edit, speed, keyframe, transition, color, effect, mask |
| Visual & audio | 7 | 2 | matting lokal, beauty lokal, sticker tracking, text, caption, audio, TTS |
| AI & template | 8 | 6 | AI music, clipper, agent, generation, digital human, template marketplace |
| Capture & distribution | 3 | 1 | camera, photo, share/publish |
| Platform & special | 3 | 1 | cloud, config, split/gameplay/live photo |
| Ad maker | 1 | 1 | script lokal, smart ad backend |
| Monetization | 1 | 0 | sengaja dihapus |

---

## F01 — Draft & Timeline Core

Spek: [`docs/features/01-draft-timeline.md`](docs/features/01-draft-timeline.md)

- [x] Spek perilaku, model data, algoritma
- [x] Model `Draft`, `Track`, `Segment`, `Material`, `TimeRange` (`Project`, `Track`, `Clip`, `MediaAsset`; rentang waktu = field start/duration/source pada `Clip`)
- [x] Serialization JSON + schema version + migration (field `assets` aditif, tanpa bump skema)
- [x] CRUD track/segment + target/source time range (`TimelineEditor`, `MediaAppender`)
- [ ] Transaction, undo, redo, snapshot
- [ ] Autosave atomic (temp + rename)
- [ ] Canvas/rasio/background
- [ ] Group/composition
- [ ] Unit test model + transaksi

## F02 — Media Import & Gallery

Spek: [`docs/features/02-media-import-gallery.md`](docs/features/02-media-import-gallery.md)

- [x] Spek
- [x] Android Photo Picker + multi-select (Photo Picker maks 10, SAF tanpa batas 10)
- [x] Copy ke workspace privat + metadata
- [ ] Probe durasi/resolusi/codec (durasi/resolusi/rotasi/audio sudah; codec belum)
- [ ] Thumbnail + waveform (thumbnail sudah; waveform di M3)
- [ ] Proxy/transcode untuk device lemah
- [ ] Dukungan HEIF/WebP/GIF (MIME diterima; decode bergantung platform)
- [x] Import dari folder lokal / SAF
- [!] Cloud import (BUTUH BACKEND; fallback SAF)
- [x] Unit test import + metadata (kebijakan MIME, penempatan klip, sanitasi aset; jalur Android diuji lewat kompilasi CI)

## F03 — Preview Player

Spek: [`docs/features/03-preview-player.md`](docs/features/03-preview-player.md)

- [x] Spek
- [x] Media3 CompositionPlayer + surface (`PlayerView`)
- [x] Composite timeline playback (satu sequence visual + satu sequence audio)
- [~] Frame-accurate seek (preview mengikuti keyframe Media3; frame-accurate di M13)
- [~] Preview range + speed (range klip terpilih sudah; global speed di M5, speed per klip sudah)
- [x] Playhead sync dengan timeline (30 fps, tanpa recompose klip)
- [x] Performance metrics (FPS; dropped-frame counter dihapus karena tidak ditampilkan)
- [~] Unit test seek/plan (`PreviewPlanTest`, `PreviewSeekTest`); instrumentation menyusul

## F04 — Timeline Editing

Spek: [`docs/features/04-timeline-editing.md`](docs/features/04-timeline-editing.md)

- [x] Spek
- [ ] Trim in/out
- [ ] Split
- [ ] Move + snap
- [ ] Ripple delete
- [ ] Collision resolve
- [ ] Linked audio/video
- [ ] Multi-select + duplicate
- [ ] Unit test operasi timeline

## F05 — Speed, Curve, Motion

Spek: [`docs/features/05-speed-curve-motion.md`](docs/features/05-speed-curve-motion.md)

- [x] Spek
- [ ] Speed konstan
- [ ] Reverse
- [ ] Speed curve
- [ ] Slow-motion interpolation
- [ ] Stabilisasi
- [ ] Deflicker
- [ ] Motion blur
- [ ] Noise reduction
- [ ] Unit test retime

## F06 — Keyframe

Spek: [`docs/features/06-keyframe.md`](docs/features/06-keyframe.md)

- [x] Spek
- [ ] Keyframe posisi/scale/opacity
- [ ] Rotasi + easing
- [ ] Curve editor
- [ ] Copy/paste keyframe
- [ ] Tracking keyframe
- [ ] Unit test interpolasi

## F07 — Transition

Spek: [`docs/features/07-transition.md`](docs/features/07-transition.md)

- [x] Spek
- [ ] Cut/dissolve/fade
- [ ] Library transisi lokal
- [ ] Durasi + curve
- [ ] Preview transisi
- [!] AI transition (BUTUH BACKEND; fallback transisi biasa)

## F08 — Filter & Color

Spek: [`docs/features/08-filter-color.md`](docs/features/08-filter-color.md)

- [x] Spek
- [ ] Filter/LUT + intensity
- [ ] Brightness/contrast/saturation
- [ ] HSL
- [ ] Curves
- [ ] Highlight/shadow/vignette
- [ ] Color wheels + color match
- [ ] Unit test color pipeline

## F09 — Video Effects

Spek: [`docs/features/09-video-effects.md`](docs/features/09-video-effects.md)

- [x] Spek
- [ ] Shader effect lokal
- [ ] Effect pack format sendiri
- [ ] Effect browser + preview
- [ ] AR/face/body effect
- [ ] Unit test shader compile

## F10 — Mask & Chroma

Spek: [`docs/features/10-mask-chroma.md`](docs/features/10-mask-chroma.md)

- [x] Spek
- [ ] Mask rectangle/linear/circle
- [ ] Feather + invert
- [ ] Brush mask
- [ ] Chroma key/green screen
- [ ] Unit test mask math

## F11 — Matting & Background

Spek: [`docs/features/11-matting-background.md`](docs/features/11-matting-background.md)

- [x] Spek
- [ ] Subject segmentation lokal (ML Kit)
- [ ] Background blur
- [ ] Background replace
- [ ] Smart crop/auto reframe
- [ ] AI background
- [ ] Unit test mask/composite

## F12 — Retouch & Beauty

Spek: [`docs/features/12-retouch-beauty.md`](docs/features/12-retouch-beauty.md)

- [x] Spek
- [ ] Skin smooth/whiten
- [ ] Face slim/eye/nose
- [ ] Makeup
- [ ] Body reshape
- [ ] Photo retouch
- [ ] Unit test parameter bounds

## F13 — Sticker & Tracking

Spek: [`docs/features/13-sticker-tracking.md`](docs/features/13-sticker-tracking.md)

- [x] Spek
- [ ] Image sticker + transform
- [ ] Lottie/GIF/video sticker
- [ ] Sticker animation
- [!] Object tracking lanjutan (BUTUH BACKEND; fallback manual keyframe)
- [ ] Unit test transform

## F14 — Text & Typography

Spek: [`docs/features/14-text-typography.md`](docs/features/14-text-typography.md)

- [x] Spek
- [ ] Text basic: font/color/stroke/shadow
- [ ] Style + animation
- [ ] Rich text + multiline
- [ ] Text template
- [ ] AI font
- [ ] Unit test layout text

## F15 — Captions & Transcript

Spek: [`docs/features/15-captions-transcript.md`](docs/features/15-captions-transcript.md)

- [x] Spek
- [ ] Import/edit SRT
- [ ] Manual caption editor
- [ ] SpeechRecognizer lokal
- [ ] Caption style + animation
- [!] Transkripsi akurat multi-bahasa (BUTUH BACKEND)
- [!] Translate/bilingual (BUTUH BACKEND; fallback ML Kit)
- [ ] Lyric + lyric effect
- [!] Overdub (BUTUH BACKEND)
- [ ] AI emoji/keyword (fallback rule-based)

## F16 — Audio Editing

Spek: [`docs/features/16-audio-editing.md`](docs/features/16-audio-editing.md)

- [x] Spek
- [ ] Add audio, volume, fade
- [ ] Mute + loudness
- [ ] Music library lokal
- [ ] Voiceover recorder
- [ ] Beat detection/sync
- [ ] Voice change
- [!] Vocal separation (BUTUH BACKEND)
- [ ] Denoise
- [!] Dubbing (BUTUH BACKEND)
- [ ] Unit test audio mix

## F17 — TTS & Voice Clone

Spek: [`docs/features/17-tts-voice-clone.md`](docs/features/17-tts-voice-clone.md)

- [x] Spek
- [ ] Android TextToSpeech engine
- [ ] Voice/language/speed/pitch picker
- [ ] Preview + insert ke timeline
- [ ] Cache hasil TTS
- [ ] SSML/streaming (fallback)
- [!] Voice clone (BUTUH BACKEND)
- [ ] Unit test cache key

## F18 — AI Music

Spek: [`docs/features/18-ai-music.md`](docs/features/18-ai-music.md)

- [x] Spek
- [ ] UI prompt/mood/duration
- [ ] Generator prosedural lokal (loop/nada)
- [ ] Insert ke timeline + beat metadata
- [!] Model musik generatif (BUTUH BACKEND)
- [ ] AI MV/lyric video (lokal, template)
- [ ] Unit test generator

## F19 — Export & Render

Spek: [`docs/features/19-export-render.md`](docs/features/19-export-render.md)

- [x] Spek
- [ ] Media3 Composition builder
- [ ] MP4 H.264/AAC
- [ ] Resolusi/fps/bitrate
- [ ] Progress + cancel
- [ ] Simpan ke MediaStore
- [ ] HEVC/audio-only
- [ ] Cover / smart cover
- [ ] Watermark **tidak ada** (by design)
- [ ] Alpha export
- [ ] HDR/10-bit
- [ ] Parallel/prerender/remux
- [ ] GIF
- [ ] Unit/instrumentation test export

## F20 — Draft, Cloud & Collaboration

Spek: [`docs/features/20-draft-cloud.md`](docs/features/20-draft-cloud.md)

- [x] Spek
- [ ] Draft list/rename/delete
- [ ] Duplicate/trash/restore
- [ ] Local backup zip + import
- [!] Cloud backup (BUTUH BACKEND; fallback folder lokal)
- [!] Shared draft (BUTUH BACKEND)
- [!] Review/komentar (BUTUH BACKEND)
- [!] Brand kit (BUTUH BACKEND; fallback lokal)
- [!] Enterprise/team (BUTUH BACKEND)

## F21 — Template, CutSame & Smart Vlog

Spek: [`docs/features/21-templates-cutsame.md`](docs/features/21-templates-cutsame.md)

- [x] Spek
- [ ] Template format sendiri
- [ ] Template browser + apply
- [ ] Slot mapping + auto-trim
- [ ] Split screen template
- [ ] Smart vlog
- [ ] Text start/marketing script
- [!] AI template (BUTUH BACKEND; fallback template biasa)
- [!] Marketplace/publish (BUTUH BACKEND)

## F22 — AI Clipper & AutoCut

Spek: [`docs/features/22-ai-clipper-autocut.md`](docs/features/22-ai-clipper-autocut.md)

- [x] Spek
- [ ] AutoCut silence/scene lokal
- [ ] Target duration + clip constraints
- [ ] Highlight scoring heuristic
- [ ] Arrange + caption + music
- [!] ASR/AED/highlight penuh (BUTUH BACKEND)
- [ ] AI Story/Vlog (lokal)
- [ ] Unit test selection

## F23 — AI Agent & AI Writer

Spek: [`docs/features/23-ai-lab-agent.md`](docs/features/23-ai-lab-agent.md)

- [x] Spek
- [ ] Chat UI + conversation state
- [ ] Tool registry + preview/undo
- [ ] Rule-based local assistant
- [ ] AI writer lokal (template teks)
- [!] LLM agent (BUTUH BACKEND)
- [ ] Unit test tool validation

## F24 — AI Image/Video Generation

Spek: [`docs/features/24-ai-generation.md`](docs/features/24-ai-generation.md)

- [x] Spek
- [ ] Generation UI (prompt/aspect/seed)
- [ ] History + import hasil
- [ ] Local placeholder preview
- [!] Text-to-image (BUTUH BACKEND)
- [!] Text-to-video/image-to-video (BUTUH BACKEND)
- [!] Inpainting/outpainting (BUTUH BACKEND)
- [!] Video extend (BUTUH BACKEND)
- [!] Face cover (BUTUH BACKEND)
- [!] Video translate (BUTUH BACKEND)

## F25 — Digital Human & Lip Sync

Spek: [`docs/features/25-digital-human.md`](docs/features/25-digital-human.md)

- [x] Spek
- [ ] Template browser + preview
- [ ] Script + TTS lokal
- [ ] Subtitle + background
- [!] Avatar render (BUTUH BACKEND)
- [!] Lip sync (BUTUH BACKEND)
- [!] Custom avatar/dressing (BUTUH BACKEND)

## F26 — Camera & Photo Editor

Spek: [`docs/features/26-camera-photo.md`](docs/features/26-camera-photo.md)

- [x] Spek
- [ ] CameraX photo/video
- [ ] Beauty/filter realtime
- [ ] Timer/speed record
- [ ] Photo crop/rotate/adjust/filter
- [ ] Photo text/sticker/brush
- [ ] Photo cutout/AI background
- [ ] Export JPG/PNG/WebP
- [ ] Unit/instrumentation test capture

## F27 — Share & Publish

Spek: [`docs/features/27-share-publish.md`](docs/features/27-share-publish.md)

- [x] Spek
- [ ] Android share sheet
- [ ] Save to gallery
- [ ] Caption/cover/hashtag
- [ ] TikTok/IG/YouTube direct share
- [!] Platform publish API (BUTUH BACKEND/SDK)
- [ ] Template publish (lokal)
- [!] AI post/publish marketplace (BUTUH BACKEND)

## F28 — Profile & Settings (Monetization Removed)

Spek: [`docs/features/28-account-subscription-ads.md`](docs/features/28-account-subscription-ads.md)

- [x] Spek
- [ ] Profil lokal (nama/avatar)
- [ ] Settings: tema, storage, bahasa, about
- [ ] Export/import pengaturan
- [x] **Tidak ada paywall / subscription / credit / ads / watermark**
- [-] Billing, IAP, reward ads, trial, SKU, license check
- [ ] Pastikan build tidak memuat SDK billing/ads

## F29 — Resource, Config & Analytics

Spek: [`docs/features/29-resource-config-analytics.md`](docs/features/29-resource-config-analytics.md)

- [x] Spek
- [ ] Local config + feature flags
- [ ] Bundled resource manifest
- [ ] Import effect/font/template lokal
- [ ] Local crash log + export
- [!] Remote catalog/telemetry (BUTUH BACKEND, off by default)
- [ ] Unit test manifest/checksum

## F30 — Split Screen, Gameplay & Live Photo

Spek: [`docs/features/30-split-gameplay-livephoto.md`](docs/features/30-split-gameplay-livephoto.md)

- [x] Spek
- [ ] Split screen 2–9 + center crop
- [ ] Layout border/background/music
- [ ] Gameplay overlay + facecam
- [ ] Live photo split
- [ ] Live photo combine/export
- [ ] Unit test layout

## F31 — Ad Maker & Script-to-Video

Spek: [`docs/features/31-ad-maker-business.md`](docs/features/31-ad-maker-business.md)

- [x] Spek
- [ ] Script → scene → media
- [ ] TTS voiceover + tone compare
- [ ] Caption otomatis
- [ ] Product card/CTA/logo/watermark
- [ ] Product album → video
- [ ] Template iklan lokal
- [!] Smart ad generatif (BUTUH BACKEND)

---

## Cross-cutting

- [x] Design system Zcut (warna, tipografi, komponen)
- [x] Navigation graph semua layar (42 rute; badan layar per milestone)
- [ ] Permission flow (media, camera, mic, notification)
- [ ] File provider + scoped storage
- [x] Error handling + empty/loading state (layar M0)
- [ ] Accessibility (content description, touch target) — audit penuh di M28
- [ ] Lokalisasi Indonesia + Inggris
- [x] Unit test core (model, timeline, export plan)
- [ ] Instrumentation test alur utama
- [x] CI build + test
- [x] Release signing via GitHub Secrets
- [x] GitHub Release APK
- [x] Tidak ada secret/keystore di repo

## Backend / model yang ditandai

| Fitur | Alasan | Fallback lokal |
|---|---|---|
| Voice clone | butuh training model suara | TTS sistem |
| AI music generatif | butuh model musik | generator prosedural |
| AI image/video | butuh model generatif | import hasil / placeholder |
| Inpainting/outpainting | butuh model | editor manual |
| Video extend/face cover/translate | butuh model | tool manual |
| Digital human/lip sync | butuh model + render | template statis |
| ASR akurat/overdub | butuh model | SpeechRecognizer/SRT |
| Cloud/collaboration/review | butuh server | backup zip lokal |
| Marketplace/template AI | butuh server | template bawaan |
| Publish platform | butuh SDK/API | share sheet |
| Smart ad | butuh model | template + TTS |

## Build & release

- [x] Workflow `.github/workflows/android.yml`
- [x] Unit test di Actions untuk setiap push/PR
- [x] `assembleRelease` di Actions saat tag `v*` (commit fitur tidak
      menghabiskan kuota build release)
- [x] Versi APK mengikuti nama tag (`ZCUT_VERSION_NAME` → `versionName`)
- [x] Signing dari secrets `ZCUT_KEYSTORE_BASE64`, `ZCUT_KEYSTORE_PASSWORD`,
      `ZCUT_KEY_ALIAS`, `ZCUT_KEY_PASSWORD`
- [x] Upload APK ke GitHub Releases
- [x] CI hijau
- [x] Link release terbaru: <https://github.com/az925-crypto/Zcut/releases/tag/v0.4.0>
      (riwayat: `v0.1.0` scaffold, `v0.2.0` M0, `v0.3.0` M1, `v0.4.0` M2)
- [x] Verifikasi signature: `apksigner verify --print-certs` →
      `CN=Zcut, OU=Mobile, O=Zcut, L=Local, ST=Local, C=ID`

Catatan release `v0.1.0`: ini APK scaffold non-UI, bukan hasil akhir. Rilis
berikutnya dibuat dengan push tag, mis. `git tag v0.2.0 && git push origin v0.2.0`.

## Commit log

| Tanggal | Commit | Isi |
|---|---|---|
| 2026-10-23 | `dadff9c` | Spesifikasi 31 fitur + mockup Stage 2 interaktif |
| 2026-10-23 | `c34923a` | Scaffold Android non-UI + workflow signed release |
| 2026-10-23 | `6102ad2` | CI pakai Android SDK bawaan runner |
| 2026-10-23 | `4cc000a` | Perbaiki fixture test quarantine; CI hijau |
| 2026-10-23 | `6a71910` | Dokumentasi status 2b, CI hijau, release `v0.1.0` |
| 2026-10-23 | `e50e137` | Re-audit fix: cegah draft hilang saat payload korup + hardening sanitize |
| 2026-10-23 | `a3ec4e6` | Bersihkan API mati + catatan backlog reviewer di PROGRESS |
| 2026-10-23 | `v0.1.0` | Signed release APK di GitHub Releases |
| 2026-10-24 | `900cbb4` | M1 preferensi lokal, onboarding tersimpan, Profil & Pengaturan; release `v0.3.0` |
| 2026-10-24 | `c129da5` | M2 editor shell, kanvas timeline, impor media; CI hijau |
| 2026-10-24 | `7c14b3b` | versionCode 4 + tag `v0.4.0`; signed release APK terbit |

## Backlog re-audit

Catatan non-blocking dari reviewer (semua reviewer: `BLOCKING: no`). Bukan
penghalang commit; jadi backlog untuk Tahap 3.

- **Speed curve belum jadi sumber kebenaran** (`TimelineEditor.splitClip`):
  offset sumber dihitung dari `speed` skalar, sementara `curve` hanya di-remap.
  Butuh keputusan semantik curve sebelum diimplementasikan.
- **Seam keyframe pakai nilai keyframe terakhir, bukan interpolasi di titik
  potong** — animasi yang melewati titik potong masih "dibekukan" di paruh kiri.
- **Erasure clip di decode tidak dikarantina** — clip invalid dibuang, bukan
  disimpan mentah; dibuat konsisten dengan karantina project saat sanitize
  sudah punya jalur karantina.
- **`schemaVersion` belum divalidasi/migrasi** (baru dibawa, belum ditolak).
- **Ordering draft** bisa salah saat `updatedAtMs` sama (tanpa tie-break).
- **Trim handle mentok** di batas sumber: edit ditolak, bukan di-clamp.
- **Pin actions ke commit SHA** dan gate job `release` di Environment
  ber-reviewer (hardening workflow).
- **`normalize`→`sanitize` menambah sort per track di jalur decode** — tidak
  signifikan, tapi bisa dilewati bila data sudah urut.
- **Duplikasi saturating-add** (`TimelineEditor.safeAdd` vs
  `ProjectFactory.advanceCursor`) — satukan saat math drag/trim UI ditulis.
- **`splitClip` sudah ~68 baris** — pisah seam-keyframe/trim/clamp ke helper
  saat salah satu concern berubah.
- **`Clip.isValid()` masih private di `TimelineEditor`** — angkat ke `Clip`
  saat layer lain (repo/UI) butuh invarian yang sama.
- **`quarantinedProjects` menyimpan dua bentuk** (dokumen mentah vs elemen
  project) tanpa penanda — beri diskriminator saat fitur recovery dibuat.

### Temuan reviewer M0 (2026-10-23)

Dibetulkan sebelum commit M0:

- **Build-breaker:** `zcutViewModelFactory` itu `@Composable` tapi dipanggil di
  dalam lambda `remember` → Compose compiler menolak. Sekarang dipanggil
  langsung.
- **Navigasi:** `popUpTo(findStartDestination())` menunjuk `splash` yang sudah
  di-pop → tab nav menumbuhkan back stack tanpa batas dan filter/pencarian reset
  tiap pindah tab. Sekarang anchor `Home`; lambda `navigateToTab` dibuat stabil
  supaya graph 42 node tidak dibangun ulang tiap navigasi.
- **Nama draft duplikat:** `nextDraftName()` menghitung dari daftar hasil filter.
  Sekarang membaca nama yang tersimpan lewat `repository.projects.first()`.
- **Hitungan di top bar ikut terfilter** → tambah `ProjectsUiState.totalCount`.
- **Hapus draft permanen tanpa konfirmasi** → `AlertDialog` konfirmasi.
- **Empty state salah saat error** → `isEmpty && error == null`.
- **`FileSizeFormat`** bisa mencetak `1000.0 KB` → unit dipilih setelah
  pembulatan; test batas ditambah.
- **Ikon splash tak terlihat** (glyph gelap di latar gelap) →
  `windowSplashScreenIconBackgroundColor` lime.
- **media3 (4 modul) belum dipakai** tapi ikut masuk APK → dihapus, ditambah
  kembali di M3.
- **GitHub Actions di-pin ke commit SHA** + gate "manifest rilis tidak boleh
  dapat permission `INTERNET`", sesuai keputusan di `DECISIONS.md`.

Backlog dari reviewer M0 (non-blocking, untuk M1+):

- **`CorruptionException` DataStore** disamarkan jadi "tidak ada draft" oleh
  `catch { IOException -> emptyPreferences() }`, dan `upsert`/`delete` bisa
  melempar keluar `viewModelScope`. Tambah `CorruptionHandler` eksplisit.
- **Tombol "Coba lagi"** saat error belum ada; `catch` mengakhiri flow sehingga
  tidak ada jalur pulih.
- **`@Preview`** untuk komponen/layar bersama belum ada (dokumen desain sudah
  disesuaikan).
- **Label waktu** (`nowMs`) tidak menua sendiri saat layar diam; butuh ticker.
- **`Icons.Filled.ArrowBack`** deprecated → pindah ke `AutoMirrored` saat
  menyentuh file itu lagi.
- **Scope state terlalu tinggi**: `pagerState.currentPage` di root
  `OnboardingScreen`; `menuForDraftId` dipakai di dalam item `LazyColumn`.
- **`DraftSummaries.from`** (sort + map) jalan di main thread; pindahkan ke
  `flowOn(Dispatchers.Default)` kalau daftar tumbuh besar.
- **Latency splash 700 ms** ditambahkan di atas splash sistem; pertimbangkan gate
  berbasis kesiapan data.
- **Path file di draft** (`FilterConfig.lutAssetPath`, `MediaAsset.thumbnailPath`)
  harus dikanonikalisasi + dipastikan di dalam app-private storage sebelum dibaca
  (mulai relevan di M3).
- **Hapus draft masih hard delete**; soft delete/trash + restore adalah P0 di
  `docs/features/20-draft-cloud.md` → dikerjakan di M27.

### Re-review M0 setelah CI hijau (2026-10-23)

CI M0 sempat merah dua kali. Penyebab dan perbaikannya:

1. **Kompilasi** (`39940710477`): `ProjectCard.kt` memakai `Color` tanpa impor;
   `ProjectsViewModel.kt` memanggil `repository.projects.first()` (butuh impor
   `first`, dan tetap salah karena `projects` adalah `Flow`). → impor ditambah,
   pembacaan nama pindah ke `repository.summaries()` yang sudah ada.
2. **Unit test** (`39941272667`): `DraftSummaries.nextName` mulai dari
   `taken.size + 1`, jadi draft yang di-rename/dihapus meninggalkan lubang nomor
   dan tes menangkapnya. → sekarang memindai dari 1 (nomor bebas pertama).

Verdict gate final: security **BLOCKING: no**, bug **BLOCKING: no**,
performance **BLOCKING: no**. Temuan non-blocking yang langsung dibereskan:

- **Dua tap cepat "Project baru"** bisa membuat dua draft bernama sama karena
  `summaries()` (baca) dan `upsert` (tulis) bukan satu transaksi. → guard
  `creating` di `ProjectsViewModel` (check-and-set di `Dispatchers.Main.immediate`).
- **Gate permission `INTERNET`** memakai `aapt2 ... | grep -q`: `grep` keluar
  lebih awal dan `pipefail` bisa membaca SIGPIPE sebagai "tidak ada match".
  → dump ke `$RUNNER_TEMP/permissions.txt` dulu, baru `grep` file itu.

Catatan performance (Low, sengaja tidak diubah): `initialValue` yang sempat
menampilkan progress bar 1–2 frame, `MetaRow` yang membangun list kecil per
recompose, dan `clip()` ganda di atas `Surface(shape=...)`.

### Temuan reviewer M1 (2026-10-24)

Gate M1: security **BLOCKING: no**, performance **BLOCKING: no**, bug
**BLOCKING: yes** → 3 Medium + 3 Low, semuanya diperbaiki dalam satu pass
sebelum commit `900cbb4`:

- **`ZcutRoutes.Settings` belum ada** padahal dipakai `ProfileScreen` dan
  `ZcutNavHost` (build gagal). → konstanta ditambahkan.
- **Back dari Settings mendarat di Home**: Profil memakai `navigateToTab`, yang
  `popUpTo(Home)`, sehingga entri Profil ikut ter-pop. → `openFromProfile`:
  route tab tetap lewat `navigateToTab`, non-tab di-push biasa.
- **Onboarding ditulis fire-and-forget tanpa `catch`** → exception tak
  tertangkap bisa crash, dan navigasi mendahului write. → `scope.launch {
  runCatching { … }; navigate(…) }` + `launchSingleTop`.
- **Setter `SettingsViewModel` bisa crash saat write gagal** → semua lewat
  `write { }` berbalut `runCatching`; `clearCache`/`refreshStorage` juga.
- **Traversal storage mengikuti symlink** (`deleteRecursively`/`walkTopDown`)
  → NIO `walkFileTree` tanpa FOLLOW_LINKS; symlink dihapus sebagai link, tidak
  diikuti. Dua unit test regresi ditambahkan.
- **Splash bisa crash/stuck saat flow preferensi error** → baca dibungkus
  `withTimeoutOrNull(3_000)` + `runCatching`, fallback `AppPreferences()`.
- **Isi `AlertDialog` bisa overflow** → `verticalScroll(rememberScrollState())`
  di kedua dialog.

Re-review commit `900cbb4`: **BLOCKING: no**. Backlog non-blocking baru:

- **Editor dibuka lewat `navigateToTab`** (`ProjectsScreen` → Editor) sehingga
  back dari Editor mendarat di Home, bukan Draft. Pre-existing; diperbaiki di
  M2 saat rute editor disentuh.
- **`runCatching` menelan `CancellationException`** di `onFinish`; dampak user
  belum terbukti (controller lama dibuang).
- **`refreshStorage` vs `clearCache` tidak diserialisasi**: hasil `usedBytes`
  yang lambat bisa menimpa angka pasca-clear sampai layar dibuka ulang.
- **`getOrDefault(0L)`** menampilkan "0 B" bila traversal melempar
  non-`IOException` (mis. `SecurityException`).

### Temuan reviewer M2 (2026-10-24)

Gate M2 **dijalankan manual**, bukan lewat subagent: tiga panggilan reviewer
otomatis gagal dengan `Free usage limit reached` dari harness. Ini bukan
"BLOCKING: no" — tidak ada verdict otomatis. Penggantinya: audit manual
security/bug/performance atas diff M2 + CI (`:app:testDebugUnitTest`) sebagai
verifikasi kompilasi dan tes. Exception ini dicatat di `DECISIONS.md`.

Temuan manual yang diperbaiki sebelum commit:

- **`ProjectCodec` memakai `MediaAsset` tanpa impor** (build gagal). → impor.
- **Provider melaporkan ukuran `-1`/`0`** untuk file valid → impor ditolak
  sebagai "terlalu besar". → ukuran `<= 0` dianggap tidak diketahui, batas
  ruang tetap dicek saat menyalin.
- **File 0 byte** bisa masuk sebagai aset "valid" → ditolak di importer.
- **Ruang penyimpanan bisa habis di tengah salinan** → cek sisa ruang berkala
  selama copy, file parsial dihapus saat gagal.
- **Metadata/thumbnail gagal membuat seluruh impor gagal** → sekarang
  non-fatal; aset tetap bisa dipakai dengan nilai fallback.
- **Cache thumbnail tidak membedakan ukuran** → kunci cache menyertakan
  `sizePx`; pratinjau besar memakai salinan privat (JPEG 320px terlalu lunak).
- **Label klip di atas thumbnail kontras rendah** → teks putih saat thumbnail
  ada, warna primer saat placeholder.
- **Ruler timeline membangun label tak terbatas** → batas 120 label.
- **Pratinjau editor bisa mendesak timeline habis di layar pendek** → tinggi
  pratinjau dibatasi 42% dan kolom bisa scroll.
- **Double-tap pada picker/impor** → guard `writeInFlight` sinkron.
- **Back dari Editor mendarat di Home** (backlog M1) → `openRoute`: tab lewat
  `navigateToTab`, non-tab di-push biasa; picker `launchSingleTop`.

Backlog M2 (non-blocking): file yatim bila upsert gagal / draft dihapus (M27),
waveform (M3), probe codec, decode HEIF/WebP/GIF bergantung platform.

### Temuan reviewer M3 (2026-10-25)

Gate otomatis dijalankan lagi (exception M2 tidak dipakai). Putaran pertama:

- security **BLOCKING: no** (4 Low: path draft dipercaya, fallback `file://`,
  root opsional fail-open, log URI).
- bug **BLOCKING: yes** (1 Critical: frame gambar tidak pernah dianggap
  render; 2 Medium: playback tidak pause saat editor ditinggalkan, state play
  salah; 4 Low).
- performance **BLOCKING: yes** (1 High: player/ticker hidup di back stack;
  2 Medium: seluruh editor recompose oleh `PreviewState`, ticker churn; 1 Low).

Semua Critical/High/Medium diperbaiki dalam satu pass:

- **Frame gambar**: `PreviewPlan.nextVisualItem` + cek `STATE_READY`/ticker
  menandai `hasFrame`; thumbnail tidak lagi menutupi gambar kedua.
- **Lifecycle**: `ON_STOP` + dispose pause; rebuild player ditunda saat editor
  tersembunyi dan di-flush saat `ON_START`.
- **Recomposition**: `PreviewState`/`Player` dikumpulkan di `PreviewPanel`,
  playhead dibaca di fase layout (`Modifier.offset {}`), FPS maksimal 1×/detik.
- **Range/seek**: `rangeAfterSeek` melepas range saat seek tepat ke ujung
  eksklusif; test regresi menangkap off-by-one ini.
- **Durasi Media3**: `durationUs = sourceStart + sourceDuration` untuk klip
  ter-clip; gap ekor per sequence (video dan audio) agar durasi composition =
  durasi timeline walau klip terakhir di-skip.
- **Sumber**: root `filesDir` wajib, path dikanonikalisasi (anti `..`/symlink),
  fallback hanya `content://`; log tidak lagi memuat URI/throwable.
- **FPS**: jendela sampel di-reset saat pause; `droppedFrames` dihapus.

Re-review: security **BLOCKING: no**, performance **BLOCKING: no**. Bug reviewer
sempat menemukan satu Critical lanjutan (gap ekor hanya ditambahkan ke sequence
visual sehingga audio-only/audio-panjang tetap lebih pendek); diperbaiki dengan
gap per sequence + test `trailingGapCoversTimelineTailPerSequence`. Verdict
akhir bug **BLOCKING: no**.

Backlog M3 (non-blocking):

- **`MediaThumbnails` belum memakai gate root yang sama** dengan preview
  (`thumbnailPath` di-join ke `cacheDir`, `localPath`/`uri` dibuka langsung).
  Dampaknya decode bitmap/null, bukan exfiltrasi; rapikan saat M27.
- **Test wiring composition** belum ada: `Media3CompositionBuilder.build`
  memanggil `Uri` Android sehingga butuh Robolectric; saat ini yang diuji hanya
  helper gap murni.
- **Durasi presentasi aktual bisa lebih pendek dari nominal** pada video VFR
  atau trim yang mengecilkan `sourceDurationMs` tanpa `durationMs`; belum
  reachable dari UI M3, verifikasi di M4/M13.
- **Preview error tidak punya tombol retry**; pemulihan saat ini lewat edit
  timeline atau keluar-masuk editor.
- **Rebuild player saat editor terlihat tetap di main thread**; pindahkan ke
  dispatcher player bila jeda terasa di perangkat rendah.
- **Timeline belum divirtualisasi**; draft 100+ klip mengukur semua klip dalam
  satu frame. Optimasi saat draft panjang jadi umum.

## Ringkasan akhir

Diisi setelah semua fitur selesai dan release sukses.
