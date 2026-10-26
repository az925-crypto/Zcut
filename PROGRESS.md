# PROGRESS — Zcut 1:1 Parity

- **Repo:** https://github.com/az925-crypto/Zcut
- **Package:** `com.zaaam.zcut`
- **Target:** Android 8.0+ (minSdk 26), compile/target SDK 35
- **Prinsip:** semua fitur gratis, tanpa paywall/watermark, tanpa server CapCut.
- **Status sekarang:** Tahap 1 selesai, Tahap 2 mockup **di-ACC 2026-10-23**,
  Tahap 2b non-UI selesai, CI hijau, signed release `v0.7.0` sudah
  diterbitkan. **Tahap 3 (UI Android) berjalan: M0–M15 selesai; batch M4–M10
  dan batch M11–M15 sudah melewati review gate (setelah perbaikan, ketiga
  reviewer `BLOCKING: no`).**
  Batch M16–M20 menyusul; rilisnya di tag `v0.8.0`.
- **Reviewer pre-commit:** security/bug/performance semuanya `BLOCKING: no`
  untuk batch M4–M10. Temuan Medium/Low yang relevan sudah diperbaiki di commit
  `2af6eb8`; sisanya di Backlog re-audit, bukan penghalang.

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
| 3. KODE UI | 🟡 Dikerjakan | ACC 2026-10-23 · M0–M15 selesai · batch M11–M15 lulus gate, rilis `v0.7.0` |
| 4. BUILD/TEST | ✅ Selesai | CI hijau: test tiap push; signed release saat tag `v*` |
| 5. RELEASE | 🟡 Sebagian | `v0.1.0`–`v0.7.0` sudah rilis; rilis fitur menyusul per tag |

## Tahap 3 — Milestone

Satu milestone = satu commit = satu run CI. Urutan P0 dulu, lalu P1/P2.

| # | Milestone | Fitur | Layar mockup | Status |
|---|---|---|---|---|
| M0 | Fondasi Compose, shell navigasi, daftar draft | F01/F20 (parsial) | splash, onboarding, home, projects | ✅ |
| M1 | Preferensi lokal, onboarding tersimpan, Profil & Pengaturan | F28/F29 | profile, settings | ✅ |
| M2 | Editor shell, kanvas timeline, media import | F01/F02 | editor, media-picker | ✅ |
| M3 | Preview player Media3 | F03 | (editor) | ✅ |
| M4 | Timeline editing: trim, split, move, snap, ripple | F04 | trim | ✅ `a2c6abe` `e60804a` |
| M5 | Speed, reverse, curve, motion | F05 | speed | ✅ `771aa82` |
| M6 | Keyframe | F06 | keyframe | ✅ `646ab88` |
| M7 | Transisi | F07 | transition | ✅ `a59cf91` |
| M8 | Filter & color | F08 | filters, adjust | ✅ `b732ba1` |
| M9 | Efek | F09 | effects | ✅ `f36af6f` |
| M10 | Mask & chroma | F10 | mask | ✅ `5d735a9` `b7192a6` |
| M11 | Teks & tipografi | F14 | text | ✅ |
| M12 | Audio dasar + TTS sistem | F16/F17 | audio, tts | ✅ |
| M13 | Export & render (Media3 Transformer) | F19 | export, export-done | ✅ |
| M14 | Matting/cutout (ML Kit) | F11 | matting | ✅ |
| M15 | Retouch & beauty | F12 | beauty | ✅ |
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
- [x] Trim in/out (snap hanya sisi yang digeser, clamp ke tetangga/sumber)
- [x] Split (seam keyframe diinterpolasi di titik potong)
- [x] Move + snap (push-kanan saat tabrakan)
- [x] Ripple delete (tutup gap)
- [x] Collision resolve (`firstFreeStart`, sanitize shift overlap)
- [ ] Linked audio/video
- [x] Duplicate (transisi dangling dibersihkan) / multi-select belum
- [x] Unit test operasi timeline

## F05 — Speed, Curve, Motion

Spek: [`docs/features/05-speed-curve-motion.md`](docs/features/05-speed-curve-motion.md)

- [x] Spek
- [x] Speed konstan (0.1×–8×, ripple ke klip berikutnya)
- [~] Reverse (tersimpan + UI; preview/pitch tidak dirender Media3)
- [x] Speed curve (integral tertutup `S/(b−a)·ln(b/a)`, curve editor)
- [~] Slow-motion interpolation (keyframe retime; frame interpolasi di export)
- [ ] Stabilisasi
- [ ] Deflicker
- [ ] Motion blur
- [ ] Noise reduction
- [x] Unit test retime (kecepatan, curve, rippling, keyframe rescale)

## F06 — Keyframe

Spek: [`docs/features/06-keyframe.md`](docs/features/06-keyframe.md)

- [x] Spek
- [x] Keyframe posisi/scale/opacity (POSITION_X/Y, SCALE, OPACITY, ROTATION, VOLUME)
- [x] Rotasi + easing (LINEAR/EASE_IN/EASE_OUT/EASE_IN_OUT/BEZIER smoothstep)
- [ ] Curve editor
- [ ] Copy/paste keyframe
- [ ] Tracking keyframe
- [x] Unit test interpolasi

## F07 — Transition

Spek: [`docs/features/07-transition.md`](docs/features/07-transition.md)

- [x] Spek
- [x] Cut/dissolve/fade (jenis disimpan per batas; clamp ke sisi terpendek, min 100 ms)
- [x] Library transisi lokal (tipe dipilih dari katalog)
- [~] Durasi + curve (durasi ✅; kurva easing belum)
- [ ] Preview transisi (BUTUH BACKEND — ditandai di UI)
- [!] AI transition (BUTUH BACKEND; fallback transisi biasa)

## F08 — Filter & Color

Spek: [`docs/features/08-filter-color.md`](docs/features/08-filter-color.md)

- [x] Spek
- [x] Filter/LUT + intensity (8 preset matriks warna lokal, blend intensity)
- [x] Brightness/contrast/saturation (+ temperature, tint, highlight/shadow)
- [~] HSL (8 band disimpan untuk export; preview matriks tidak mempengaruhi)
- [ ] Curves (menyusul — ditandai di UI)
- [x] Highlight/shadow/vignette (vignette disimpan untuk export)
- [ ] Color wheels + color match (menyusul — ditandai di UI)
- [x] Unit test color pipeline (matrix, preset, order, band upsert)

## F09 — Video Effects

Spek: [`docs/features/09-video-effects.md`](docs/features/09-video-effects.md)

- [x] Spek
- [x] Shader effect lokal (9 efek, matrix warna + blur, stack berurutan)
- [ ] Effect pack format sendiri (katalog dikode keras; format pack di M29)
- [x] Effect browser + preview (kategori + intensity + preview card)
- [~] AR/face/body effect (BUTUH BACKEND; fallback warna lokal, tetap bisa dipilih)
- [x] Unit test katalog/matrix/stack

## F10 — Mask & Chroma

Spek: [`docs/features/10-mask-chroma.md`](docs/features/10-mask-chroma.md)

- [x] Spek
- [x] Mask rectangle/linear/circle (+ mirror, heart, star; scrim + outline putus-putus)
- [x] Feather + invert + posisi
- [ ] Brush mask
- [~] Chroma key/green screen (disimpan untuk export; preview area mask saja)
- [x] Unit test mask math

## F11 — Matting & Background

Spek: [`docs/features/11-matting-background.md`](docs/features/11-matting-background.md)

- [x] Spek
- [x] Subject segmentation lokal (ML Kit selfie, model dibundel; satu frame)
- [x] Background blur (turun-naik skala; latar transparan/warna juga ada)
- [~] Background replace gambar pilihan user (config siap, pemilih aset belum)
- [ ] Smart crop/auto reframe (F30)
- [!] AI background (BUTUH BACKEND; prompt disimpan di draft)
- [x] Unit test mask/composite (`MattingEditorTest`)
- [ ] Matte per frame saat export (batas render M13)

## F12 — Retouch & Beauty

Spek: [`docs/features/12-retouch-beauty.md`](docs/features/12-retouch-beauty.md)

- [x] Spek
- [x] Skin smooth/whiten (terlokalisasi ke area wajah; key `auto_buffing` / `auto_balance_skin`)
- [x] Face slim/eye/nose (warp satu pass; key `thin_face` / `eye_size` / `nose_wing`)
- [x] Makeup (lip tint lokal dari landmark mulut; key `type_lips`)
- [~] Body reshape (key `body_reshapes` & `event.lua` sudah dibaca; slider belum dirancang mockup)
- [~] Photo retouch (retouch jalan pada frame klip; editor foto F26 belum)
- [x] Unit test parameter bounds (`BeautyEditorTest`)

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
- [x] Text basic: font/color/stroke/shadow (M11)
- [x] Style + animation preview (M11; burn-in export di M13)
- [x] Multiline + alignment + spacing + line height (M11)
- [ ] Rich text per-range (P1)
- [ ] Text template multi-layer (deferred)
- [!] AI font (BUTUH BACKEND)
- [x] Unit test layout text (M11: `TextEditor`, `TextAnimator`, `TextStyle`)

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
- [x] Add audio, volume, fade (M12; mix diterapkan saat export M13)
- [x] Mute + loudness (M12: `Loudness` RMS → target ≈ -16 dBFS)
- [ ] Music library lokal (impor perangkat saja di M12)
- [x] Voiceover recorder (M12: `VoiceoverRecorder` + izin `RECORD_AUDIO`)
- [x] Beat detection/sync (M12: `BeatDetector` + `beatGridMs` + snap)
- [ ] Voice change (M12 hanya pitch; formant/robot menyusul)
- [!] Vocal separation (BUTUH BACKEND)
- [ ] Denoise (belum ada)
- [!] Dubbing (BUTUH BACKEND)
- [x] Unit test audio mix (M12: `AudioEditorTest`, `BeatDetectorTest`, `LoudnessTest`)

## F17 — TTS & Voice Clone

Spek: [`docs/features/17-tts-voice-clone.md`](docs/features/17-tts-voice-clone.md)

- [x] Spek
- [x] Android TextToSpeech engine (M12: `AndroidSpeechEngine`)
- [x] Voice/language/speed/pitch picker (M12: `SpeechEngine.voices()` + rate/pitch)
- [x] Preview + insert ke timeline (M12: `speak` + `insertAudio` di playhead)
- [x] Cache hasil TTS (M12: cache key SHA-256 di `filesDir/tts`)
- [ ] SSML/streaming (fallback)
- [!] Voice clone (BUTUH BACKEND)
- [x] Unit test cache key (M12: `TtsScriptTest`)

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
- [x] Media3 Composition builder
- [x] MP4 H.264/AAC
- [x] Resolusi (preset) — fps/bitrate mengikuti encoder perangkat, disebut di UI
- [x] Progress + cancel
- [x] Simpan ke MediaStore (`Movies/Zcut`; API 26–28 → folder Movies app)
- [ ] HEVC/audio-only
- [x] Cover / smart cover (JPEG frame 1/3 durasi)
- [x] Watermark **tidak ada** (by design)
- [ ] Alpha export
- [ ] HDR/10-bit
- [ ] Parallel/prerender/remux
- [ ] GIF
- [~] Unit test export (ExportPlan + paritas rute; engine butuh perangkat)
- [ ] Burn-in teks/efek/mask/sticker ke video final

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

### Rencana implementasi fitur BUTUH BACKEND — dummy jujur

Semua fitur di tabel di atas tetap **dibangun di UI** pada milestone-nya, tapi
tanpa backend: yang tersedia hanya **dummy/placeholder yang jujur** — kontrol
tampil dan bisa ditekan, lalu menampilkan status "butuh backend" beserta
fallback lokalnya (bila ada). Tidak ada server tiruan, tidak ada hasil palsu,
tidak ada paywall, dan tidak ada yang disembunyikan.

Aturan dummy:

- Kontrol tetap terlihat dan serumpun dengan fitur lain (tidak dikunci, tidak
  di-disable tanpa penjelasan).
- Menekan tombol dummy **tidak** menghasilkan artefak palsu; hasil = pesan
  status + arahan fallback lokal, atau menerapkan fallback lokal yang nyata.
- Badge **BUTUH BACKEND** dipakai konsisten lewat `BackendBadge`/`FeatureRegistry`.
- Bila kelak ada backend/model, dummy diganti implementasi nyata tanpa mengubah
  rute/navigasi (kontrak layar sudah ada duluan).

Prioritas dummy: F24 (AI image/video), F25 (digital human), F23 (LLM agent),
F18 (AI music generatif), F21 (template AI), F22 (ASR penuh), F26 (face cover),
F27 (publish API), F20/F28 (cloud/kolaborasi), F31 (smart ad). Untuk tiap item,
fallback lokal di kolom ketiga tetap dikerjakan lebih dulu; dummy menutup sisanya.

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
- [x] Link release terbaru: <https://github.com/az925-crypto/Zcut/releases/tag/v0.5.0>
      (riwayat: `v0.1.0` scaffold, `v0.2.0` M0, `v0.3.0` M1, `v0.4.0` M2,
      `v0.5.0` M3)
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
| 2026-10-25 | `4bf0df2` | M3 preview player Media3: plan, engine, timeline playhead, lifecycle |
| 2026-10-25 | `cc295cd` | Perbaikan kompilasi ikon vektor M3; CI hijau |
| 2026-10-25 | `1de612b` | versionCode 5 + tag `v0.5.0`; signed release APK terbit |
| 2026-10-25 | `a2c6abe` `e60804a` | M4 timeline editing: trim/split/move/snap/ripple + TrimScreen + tool layer |
| 2026-10-25 | `771aa82` | M5 speed: kurva closed-form, reverse, SpeedScreen |
| 2026-10-25 | `646ab88` | M6 keyframe: KeyframeEditor + KeyframeScreen |
| 2026-10-25 | `a59cf91` | M7 transisi batas klip + penanda timeline |
| 2026-10-25 | `b732ba1` | M8 filter LUT lokal + adjust warna/HSL |
| 2026-10-25 | `f36af6f` | M9 efek lokal + katalog + intensity |
| 2026-10-25 | `5d735a9` `b7192a6` | M10 mask bentuk + chroma key lokal |
| 2026-10-25 | `2af6eb8` | Review gate batch M4–M10: fix trim-snap, split/retime keyframe, repair transisi, sanitize overflow, mask draft, thumbnail gate/dedup, hot-path speed, ruler cap |
| 2026-10-25 | `a63e970` `212f7cc` | M11 teks & tipografi: TextStyle/TextEditor/TextAnimator + TextScreen |
| 2026-10-25 | `0e5d5e0` `3f3d199` `e675f79` | M12 audio dasar & TTS sistem: envelope gain, beat/loudness lokal, voiceover, TtsScreen |
| 2026-10-25 | `90bd3b5` | M13 export & render: Media3ExportEngine, ExportPlan, layar export/export-done, FileProvider + MediaStore |
| 2026-10-25 | `b4ab340` | M14 matting & cutout: MlKitSubjectSegmenter, MattingComposer, MattingScreen, MattingConfig |
| 2026-10-25 | `7e673f2` `36c7980` | M15 retouch & beauty: MlKitFaceDetector, BeautyProcessor, BeautyEditor, BeautyScreen, rail Cutout/Retouch |
| 2026-10-26 | `87cbffc` | Review gate batch M11–M15: fix Critical tombol TTS mati, MediaRecorder bocor, clamp beauty/matting, guard recycle, status jujur model wajah, DSP ke Default |
| 2026-10-26 | `dcf1422` `91955f8` | Tutup sisa Low gate: recycle `smooth`, inferensi gagal = model tak tersedia, hapus dead `unavailableReason`, buang gate `MIN_SIDE` FaceDetector |
| 2026-10-26 | `v0.7.0` | versionCode 7 + tag `v0.7.0`; signed release APK terbit |

## Backlog re-audit

Catatan non-blocking dari reviewer (semua reviewer: `BLOCKING: no`). Bukan
penghalang commit; jadi backlog untuk Tahap 3.

- **Speed curve belum jadi sumber kebenaran** (`TimelineEditor.splitClip`):
  offset sumber dihitung dari `speed` skalar, sementara `curve` hanya di-remap.
  Butuh keputusan semantik curve sebelum diimplementasikan.
- **Erasure clip di decode tidak dikarantina** — clip invalid dibuang, bukan
  disimpan mentah; dibuat konsisten dengan karantina project saat sanitize
  sudah punya jalur karantina.
- **`schemaVersion` belum divalidasi/migrasi** (baru dibawa, belum ditolak).
- **Ordering draft** bisa salah saat `updatedAtMs` sama (tanpa tie-break).
- **Trim handle mentok** di batas sumber: edit ditolak, bukan di-clamp.
- **Pin actions ke commit SHA** dan gate job `release` di Environment
  ber-reviewer (hardening workflow).
- **Duplikasi saturating-add** (`TimelineEditor.safeAdd` vs
  `ProjectFactory.advanceCursor`) — satukan saat math drag/trim UI ditulis.
- **`Clip.isValid()` masih private di `TimelineEditor`** — angkat ke `Clip`
  saat layer lain (repo/UI) butuh invarian yang sama.
- **`quarantinedProjects` menyimpan dua bentuk** (dokumen mentah vs elemen
  project) tanpa penanda — beri diskriminator saat fitur recovery dibuat.
- **Rebuild player Media3 sinkron di main thread** tiap commit edit
  (`EditorViewModel`/`Media3PreviewEngine`) — swap-ke-player-baru ditunda;
  terasa sebagai jeda 0,2–0,5 s saat lepas trim handle di device rendah.
- **Mutex terpisah per ViewModel** (`ClipToolViewModel` vs `EditorViewModel`)
  — edit bersamaan dari kedua layar bisa saling menimpa; belum reachable
  (editor tertutup saat tool screen terbuka), didokumentasikan.
- **`KeyframeEditor.sample` menyortir ulang daftar keyframe tiap panggil** —
  list kecil, dioptimasi kalau keyframe banyak.
- **Ruler timeline minor-tick di-cap 600 detik** (bukan dihapus) — label tetap
  penuh, tick halus berhenti setelah 10 menit (`ponytail:` di kode).
- **Virtualisasi timeline** belum ada — dedup in-flight + semaphore sudah
  meredakan; draft 100+ klip tetap mengukur semua klip per frame.
- **`MediaRecorder.prepare()/start()`/`stop()` + `AudioDuration.of` sinkron di
  main thread** saat rekam/stop voiceover (`AudioScreen` lambda tombol,
  `DisposableEffect onDispose`) — hitch ratusan ms di device rendah; pindahkan
  ke dispatcher IO + state loading saat itu dirapikan.
- **`AudioDuration.of` di `TtsViewModel.generate` dan builder export
  (`PreviewPlanBuilder.build`, `Media3CompositionBuilder.build`,
  `output.parentFile?.mkdirs()`)** jalan di dispatcher Main sebelum export
  masuk `Main.immediate` — hitch kecil; pindah ke IO/Default.
- **Bitmap hasil composite slider** (`BeautyScreen`/`MattingScreen`) tidak
  di-`recycle()`; churn heap ~2 MB per commit slider yang ditangani GC — pola
  sama dengan M14, dibersihkan lewat `DisposableEffect` kalau churn terasa.

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

### Temuan reviewer batch M4–M10 (2026-10-25)

Gate otomatis dijalankan pada batch (M4–M10) di commit `b7192a6`. Verdict
ketiga reviewer: security **BLOCKING: no**, bug **BLOCKING: no**, performance
**BLOCKING: no**. Tidak ada temuan Critical/High; Medium/Low yang relevan
diperbaiki dalam satu pass di commit `2af6eb8`:

- **Trim snap dua sisi** (bug Medium): drag satu handle ikut men-snap sisi
  satunya → klip bisa membesar diam-diam atau gestur tertelan. → snap hanya
  sisi yang berubah (`EditorViewModel.trimClip`).
- **Split seam keyframe** (bug Medium): paruh kiri tidak dapat keyframe di
  titik potong dan paruh kanan di-seed nilai keyframe terakhir → bentuk animasi
  berubah di seam. → kedua paruh di-seed nilai sampel di titik potong.
- **Retime tidak men-scaling keyframe** (bug Medium): keyframe melewati durasi
  baru → animasi drift tak terlihat. → keyframe di-scale ke durasi baru.
- **Transisi menggantung** (bug Medium): delete/duplicate/move/split tidak
  memperbaiki transisi tetangga. → repair transisi di `recalculate`
  (choke point semua mutasi), clamp ke sisi terpendek, drop bila < 100 ms.
- **Sanitize overflow** (bug Low): klip bisa "stuck" di `Long.MAX_VALUE`. →
  klip yang overflow di-drop, bukan dibiarkan tak bisa diedit.
- **MaskScreen slider** (bug Medium): slider menampilkan nilai committed (basi)
  saat menulis draft → thumb tidak mengikuti jari; chip shape juga bisa
  ditimpa draft lama. → slider/invert/chip memakai draft, commit di ujung.
- **Transisi klip pendek** (bug Low): kedua klip < 100 ms → tile transisi
  no-op tanpa umpan balik. → kontrol disembunyikan + hint.
- **Path thumbnail** (security Low): `thumbnailPath`/`localPath`/`uri` dibaca
  tanpa gate containment (gap satu-satunya vs `PreviewPlan`). → contain di
  `cacheDir`/`filesDir` + hanya `content://`.
- **Decode-time NaN** (security Low): `ColorAdjustments.brightness` tanpa
  `coerceIn`. → di-clamp seperti saudaranya.
- **speedAt re-normalisasi** (perf Medium): 73×/frame di curve editor dan
  tiap frame playback. → `speedAtNormalized` (binary search) di hot path.
- **Thumbnail fan-out** (perf Medium): N klip → N retriever paralel, split
  mendecode file sama 2×. → dedup in-flight + `Semaphore(3)`.
- **Ruler tick tak terbatas** (perf Low) → minor tick di-cap; **drawDiamond
  alokasi Path** (perf Low) → 8 `drawLine`; **waktu keyframe > durasi klip**
  (bug Low) → `remember` ikut `durationMs`; komentar `valueOf`/`sample`
  diperbaiki (perf/Low).

Yang sengaja **ditunda** ke backlog (bukan penghalang): rebuild player sinkron
di main thread (refactor besar, pre-existing M3), mutex lintas-ViewModel
(latent), virtualisasi timeline penuh.

Test baru/update di `TimelineEditorTest`: split seam sampel, retime rescale
keyframe, ripple-delete/duplicate repair transisi, sanitize overflow drop.

### Temuan reviewer batch M11–M15 (2026-10-26)

Gate otomatis dijalankan pada batch (M11–M15) di commit `cd77987`. Verdict:
security **BLOCKING: no**, performance **BLOCKING: no**, bug **BLOCKING: yes**
(1 Critical + 1 Medium). Semua temuan Critical/Medium dan Low murah diperbaiki
dalam satu pass:

- **Critical — tombol TTS mati permanen** (`TtsViewModel.generate`): jalur
  `!ready` (mesin TTS perangkat tidak ada / init > 8 s) hanya menulis notice lalu
  `return@launch` tanpa me-reset `isBusy`, sehingga `canGenerate` tetap false dan
  label tombol tetap "Membuat suara..." sampai layar dibuka ulang. → jalur itu
  kini me-reset `isBusy = false` bersama notice-nya.
- **Medium — `MediaRecorder` bocor** (`VoiceoverRecorder.start`): saat
  `prepare()`/`start()` gagal, recorder yang baru dibuat tidak di-release
  (`runCatching { recorder?.release() }` no-op karena `recorder` baru di-assign
  di jalur sukses) → mic tertahan dan tiap retry bocor lagi. → recorder
  dikonstruksi terpisah dan di-release di jalur gagal.
- **Low — `beauty`/`matting` tidak disanitasi saat decode** (`ProjectCodec` →
  `TimelineEditor.sanitize`): draft yang disunting tangan dengan `smooth: 1e30`
  atau `blur: 1e30`/NaN lolos ke model dan label slider mencetak angka sampah.
  → `sanitize` kini meng-clamp `matting`/`beauty` tiap klip (satu gerbang untuk
  semua jalur decode/impor), + tes regresi di `TimelineEditorTest`.
- **Low — recycle bitmap identitas** (`BeautyProcessor.boxBlur` /
  `MattingComposer` blur): frame sangat kecil membuat `createScaledBitmap`
  mengembalikan instance yang sama sehingga recycle ganda menabrak bitmap yang
  masih dipakai; `coerceIn` rentang kosong juga bisa melempar. → guard
  identitas + batas atas `coerceAtLeast`.
- **Low — `CancellationException` tertelan** (`MlKitFaceDetector`,
  `MlKitSubjectSegmenter`): coroutine yang dibatalkan (pindah layar) diubah jadi
  "tidak ada wajah/subjek". → rethrow CancellationException.
- **Low — status jujur model wajah** (`BeautyScreen`): `detect()` dulu
  mengembalikan list kosong baik saat tidak ada wajah maupun saat model gagal
  dimuat, jadi layar menuduh frame. → kontrak `FaceDetector.detect` kini
  `List<FaceGeometry>?` (`null` = model tidak tersedia) dan layar menampilkan
  "MODEL WAJAH TIDAK TERSEDIA" + hint jujur.
- **Low — dokumen vs kode** (`AudioEditor.fadeGain`): KDoc menyebut "kedua ramp
  dikalikan" padahal implementasinya `minOf`. → komentar diselaraskan.
- **Perf Medium (sebagian)** — pass DSP `Loudness.normalizeGain` dan
  `BeatDetector.detect` dipindah ke `Dispatchers.Default` (sebelumnya ikut main
  thread setelah decode). Sisanya masuk backlog.

Test baru: `sanitizeClampsBeautyAndMattingFromAnUntrustedDraft`,
`sanitizeLeavesAnAbsentBeautyAndMattingAbsent`.

Yang sengaja **ditunda** ke backlog (non-blocking):
`MediaRecorder.prepare()/stop()` + `AudioDuration.of` di main thread saat
rekam voiceover (hitch ~ratusan ms), `AudioDuration.of` di `TtsViewModel`, dan
builder export (`PreviewPlanBuilder.build`/`Media3CompositionBuilder.build`) di
dispatcher Main sebelum pindah ke `Main.immediate`. Composite bitmap hasil
slider tidak di-`recycle()` (dibersihkan GC; pola sama dengan M14) juga dicatat
sebagai churn, bukan leak.

### Catatan M11 — teks & tipografi (2026-10-25)

Lingkup yang selesai:

- `ClipType.TEXT` pada track `TEXT` tunggal; tambah/edit/hapus lewat
  `TextEditor` (push-right ke slot bebas bila benturan).
- `TextConfig` diperluas aditif: `stylePreset`, `gradientColorArgb`,
  `letterSpacingEm`, `lineHeight` (draft lama decode dengan default).
- Editor overlay Compose mengikuti playhead + keyframe opacity/posisi/skala/
  rotasi; timeline memakai baris pertama teks sebagai label.
- Animasi `RISE`/`TYPEWRITER`/`POP`/`GLITCH` dihitung fungsi murni
  `TextAnimator`, dipakai preview editor dan preview tool.
- Kontrak ukuran `TextLayout.REFERENCE_FRAME_HEIGHT_DP = 260f`; M13 wajib
  memakai konstanta yang sama saat burn-in.
- `PreviewPlan.sameMediaAs` + guard di `Media3PreviewEngine.prepare` mencegah
  rebuild player Media3 saat teks/gaya berubah.

Batasan yang diakui di UI dan docs:

- Burn-in teks ke MP4 baru M13; preview Media3 tidak menggambar teks.
- Import font dari perangkat (SAF) ditunda; hanya font sistem.
- Template multi-layer dan AI font ditandai deferred / **BUTUH BACKEND**.
- Satu track `TEXT`; teks yang bentrok waktu digeser, tidak ditumpuk.
- Rich text per-range, curve text, dan handwrite tetap P1/P2.

`docs/ui-m11-design.md` ditulis manual karena subagent `design` gagal
(model unavailable); kegagalan dicatat di `DECISIONS.md`.

### Catatan M12 — audio dasar & TTS sistem (2026-10-25)

Lingkup yang selesai:

- `ClipType.AUDIO` di track `AUDIO` tunggal; klip masuk lewat
  `AudioEditor.insertAudio` (impor lagu, rekaman voiceover, hasil TTS), semuanya
  memakai satu jalur push-right ke slot bebas.
- Envelope gain per klip (`volume`, `fadeInMs`, `fadeOutMs`, `pitch`, `muted`,
  `looping`) dengan clamp di `AudioEditor.normalize`; `ProjectCodec.normalize`
  men-sanitasi draft impor.
- Analisis lokal: `BeatDetector` (energy onset, BPM median), `Loudness`
  (RMS → target ≈ -16 dBFS), `PcmDecoder` (MediaExtractor + MediaCodec → mono
  float, batas 60 detik).
- `RECORD_AUDIO` + `VoiceoverRecorder` (AAC ke workspace privat) dan
  `AudioDuration` untuk panjang berkas hasil.
- TTS sistem: `SpeechEngine` diperluas aditif (`SpeechVoice`, `voices()`,
  `speak()`, `stop()`), `AndroidSpeechEngine` untuk `android.speech.tts`;
  `TtsScript` menangani sanitize, estimasi, dan cache key SHA-256.
- Rute `audio?projectId=&clipId=&atMs=` dan `tts?projectId=&atMs=`, keduanya
  `implemented = true`; `EditorScreen.openTool` mengarahkan tool `Audio` ke rute
  ber-argumen.
- Snap ke beat: `EditorViewModel.snapCandidates` menambah `beatGridMs` hanya
  saat `snapToBeat` menyala; `beatGridMs` ditambahkan aditif tanpa bump
  `schemaVersion`.

Batasan yang diakui di UI dan docs:

- Volume/fade/mute/pitch disimpan di draft, baru diterapkan oleh mixer export
  (M13). Preview Media3 memutar audio sumber apa adanya.
- Pustaka musik/SFX bawaan belum ada; impor dari perangkat saja.
- Denoise, voice change berbasis formant, dan auto-cut ke beat menyusul;
  vocal separation & dubbing **BUTUH BACKEND**; voice clone **BUTUH BACKEND**.
- `PcmDecoder` dibatasi 60 detik pertama klip.

`docs/ui-m12-design.md` ditulis manual karena subagent `design` gagal
(model unavailable); kegagalan dicatat di `DECISIONS.md`.

### Catatan M13 — export & render Media3 (2026-10-25)

Lingkup yang selesai:

- `Media3ExportEngine : VideoRenderEngine` (dibuat per layar lewat
  `AppContainer.newRenderEngine()`, bukan singleton) memakai
  `PreviewPlanBuilder` + `Media3CompositionBuilder.build(plan, shortEdge)` —
  gerbang sumber sama dengan preview, jadi klip yang tidak bisa diputar tidak
  bisa diekspor.
- MP4 H.264/AAC (`setVideoMimeType`/`setAudioMimeType`); resolusi dari
  `ExportPreset` lewat `Presentation.createForShortSide` di level komposisi.
- Progress polling `Transformer.getProgress` tiap 400 ms → `Preparing`,
  `Rendering(percent)`, `Finalizing`, `Completed`, `Failed`; `cancel()` dipanggil
  saat tombol Batal dan saat flow berhenti (layar ditutup di tengah render).
- Hasil ke `filesDir/exports`; nama file dari `ExportPlan` (sanitasi nama draft,
  timestamp mencegah tabrakan). `export-done` membawa **nama file**, bukan path.
- `ExportDelivery`: bagikan lewat `FileProvider` (+ `res/xml/file_paths.xml`,
  hanya `exports/`) dan simpan ke galeri `Movies/Zcut` via `MediaStore` (API 29+
  tanpa izin; API 26–28 ke folder Movies app dengan pesan lokasi apa adanya).
- Smart cover menulis satu JPEG (`CoverFrame`, frame 1/3 durasi).
- Rute `export?projectId=` dan `export-done?projectId=&file=&durationMs=&sizeBytes=&cover=`
  keduanya `implemented = true`; tombol Export di editor kini membawa `projectId`.

Batasan yang diakui di UI dan docs:

- Bitrate/frame rate/sample rate akhir ditentukan encoder perangkat
  (`Bitrate: Otomatis` di panel); hanya resolusi yang benar-benar dipaksa.
- Burn-in teks/efek/mask/sticker belum ada (batas preview M3 yang sama),
  HEVC/audio-only/GIF/alpha/HDR/parallel-remux belum ada.
- Default export di Pengaturan belum mengalir ke draft; layar export memakai
  `ProjectSettings.exportPreset` milik draft (backlog).

`docs/ui-m13-design.md` ditulis manual (subagent `design` masih unavailable).

### Catatan M14 — matting & cutout (2026-10-25)

Lingkup yang selesai:

- `MattingConfig` aditif di `Clip` (`enabled`, `background`, `colorArgb`,
  `blur`, `edgeRefine`, `shadow`, `imageAssetId`, `aiPrompt`) +
  `MattingBackground` (TRANSPARENT/COLOR/BLUR/IMAGE/AI). Draft lama decode
  dengan `null`, tanpa naik `schemaVersion`.
- `SubjectSegmenter.segment(Bitmap): SubjectMask?` — kontrak lama berbasis
  path dihapus (tidak ada tulis-baca PNG mask); `SubjectMask` hanya data.
- `MlKitSubjectSegmenter` memakai ML Kit selfie segmentation
  (`com.google.mlkit:segmentation-selfie:16.0.0-beta6`, model dibundel ±4,5 MB),
  satu client per proses, `SINGLE_IMAGE_MODE`; null = perangkat tidak bisa
  segmentasi (dan UI mengatakannya, bukan menggambar tebakan).
- `MattingEditor` (domain murni): clamp config dari draft tak terpercaya,
  remap confidence→alpha lewat ambang edge refine, confidence subjek, label.
  Dipanggil dari `TimelineEditor.setMatting`.
- `MattingComposer`: stencil alpha → `DST_IN` pada frame → latar (transparan,
  warna, blur turun-naik skala, atau frame untuk IMAGE/AI) → shadow.
- `MattingScreen` (rute `matting`, clip tool) mengikuti mockup: checkerboard +
  frame composite, chip label kepercayaan subjek yang jujur, Remove BG / Blur
  BG, chip warna Studio/Forest/Sunset, chip AI (BUTUH BACKEND), slider Edge
  refine/Shadow/Blur dengan commit saat jari dilepas.
- Uji: `MattingEditorTest` (clamp, prompt, remap alpha, confidence subjek,
  label, needs-backend) + `ZcutDestinationsTest` (rute `matting`).
- `docs/ui-map.md` menandai baris `matting` ✅.

Batasan yang diakui di UI dan docs:

- Matte per frame seluruh video saat export belum ada (batas render M13);
  pratinjau memakai satu frame klip dan hal itu ditulis di layar.
- Latar gambar pilihan user belum dirender (config ada, pemilih aset belum);
  latar AI tetap BUTUH BACKEND dan memakai frame asli sebagai latar sementara.
- Hair detail, temporal consistency, dan brush eraser belum ada.

`docs/ui-m14-design.md` ditulis manual (subagent `design` masih unavailable).

### Catatan M15 — retouch & beauty (2026-10-25)

Lingkup yang selesai:

- `BeautyConfig` aditif di `Clip` (`smooth`, `whiten`, `faceSlim`, `eyes`,
  `nose`, `lips`, `preserveTexture`); draft lama decode `null` tanpa naik
  `schemaVersion`. Nilai adalah posisi slider 0–100, sama seperti `_ui_value` di
  konfigurasi asli.
- `BeautyEditor` (domain murni, diuji JVM) memegang tabel key/rentang asli dari
  `assets/local_beauty_all.json` (`auto_buffing` 0–0.8, `auto_balance_skin`
  0–0.8, `thin_face` 0.5–0.8, `eye_size` 0.5–0.8, `nose_wing` 0.5–0.6,
  `type_lips` 0–1), konversi slider→engine (dua aturan dari bukti), clamp config
  tak terpercaya, jumlah slider aktif, label chip, ringkasan, dan catatan tab.
  Key asli yang belum punya slot mockup tetap disimpan di `SHIPPED_KEYS`.
- `FaceDetector.detect(Bitmap): List<FaceGeometry>` + `MlKitFaceDetector`
  (`com.google.mlkit:face-detection:16.1.7`, model dibundel, satu client per
  proses, landmark mata/hidung/mulut, geometri normal 0..1). Hasil kosong =
  tidak ada wajah, bukan tebakan.
- `BeautyProcessor.apply`: satu pass warp (`OFFSET` untuk slim, `EXPAND` untuk
  mata/nose, meniru bentuk `FaceReshape_V2/distortion.json`), lalu smooth &
  whiten terlokalisasi (mask elips lembut + `DST_IN`), lalu lip tint dari
  landmark mulut. `null` = tidak ada wajah / tidak ada slider digeser.
- `BeautyScreen` (rute `beauty`, clip tool) mengikuti mockup: pratinjau +
  kotak wajah (digambar di ruang skala `ContentScale.Fit`), segmented
  Face/Body/Makeup, 6 slider dengan key asli ditampilkan, switch
  "Preserve texture", chip jumlah wajah.
- `ToolSegmented` ditambahkan ke `ToolCommon` (segmented control mockup).
- Semua review gate M15 (`security/bug/performance`) dijalankan di akhir batch
  M11–M15 sebelum rilis batch.

Jalur masuknya: rail tool editor kini punya chip **Cutout** dan **Retouch**.
Alasannya: `MattingScreen` (M14) sudah terdaftar sebagai rute tapi tidak punya
jalur masuk sama sekali — mockup menggambar tombol Cutout di panel Mask, dan
Retouch tidak punya jalur masuk di mockup. Rail adalah satu-satunya pintu masuk
clip tool di aplikasi (pola yang sama seperti Keyframe/Transisi), jadi kedua
chip itu ditambahkan supaya panel tidak "ada tapi tidak bisa dibuka". Dicatat di
`DECISIONS.md`.

Batasan yang diakui di UI dan docs:

- Retouch memakai satu frame klip; matte/composite per frame saat export belum
  ada (batas render M13), jadi video final belum ikut di-retouch. Ditulis di
  layar.
- Tab Body & Makeup belum punya slider (mockup ACC tidak merancangnya); layar
  menyebut bukti aslinya (`BeautyBodyPreset.body_reshapes`, `event.lua`
  `MANUAL_RESHAPE_INTENSITY_SLIM`/`_STRETCH`, `type_makeup_all`/`type_blush`)
  alih-alih mengarang kontrol.
- Konversi slider→engine untuk item shape tidak terbaca penuh (preset asli
  memakai `0.5 + ui/200`); statusnya **Unknown** di spek fitur.
- Pemetaan `type` `distortion.json` → `WarpKind` **inferred**; magnitudo warp
  milik Zcut. Hair, makeup penuh, manual reshape brush, dan temporal
  consistency belum ada.

`docs/ui-m15-design.md` ditulis manual (subagent `design` masih unavailable).

## Ringkasan akhir

Diisi setelah semua fitur selesai dan release sukses.
