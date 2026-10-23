# PROGRESS — Zcut 1:1 Parity

- **Repo:** https://github.com/az925-crypto/Zcut
- **Package:** `com.zaaam.zcut`
- **Target:** Android 8.0+ (minSdk 26), compile/target SDK 35
- **Prinsip:** semua fitur gratis, tanpa paywall/watermark, tanpa server CapCut.
- **Status sekarang:** Tahap 1 selesai, Tahap 2 mockup selesai menunggu
  ACC, Tahap 2b non-UI selesai, CI hijau, signed release `v0.1.0` sudah
  diterbitkan. UI Android belum dimulai sampai ACC.

## Mockup Stage 2

- Lokasi: `mockup/index.html`
- Jalankan: `cd mockup && python3 -m http.server 8080`
- Preview: `http://127.0.0.1:8080/` · peta layar: `/?view=map`
- Cakupan: 42 layar untuk 31 area fitur, termasuk semua panel editor, AI,
  capture, export, share, dan settings.
- Status: **menunggu ACC via Telegram**. UI Android belum dimulai.

## Status tahap

| Tahap | Status | Catatan |
|---|---|---|
| 1. DOC | ✅ Selesai | 31 spek fitur + index + arsitektur |
| 2. MOCKUP | ✅ Selesai, menunggu ACC | 42 layar · 31 area · HTML interaktif |
| 2b. Non-UI | ✅ Selesai | model, data layer, render contract, unit test, CI |
| 3. KODE UI | ⛔ Diblokir | mulai setelah ACC Telegram |
| 4. BUILD/TEST | ✅ Selesai | CI hijau: test + signed release di GitHub Actions |
| 5. RELEASE | 🟡 Sebagian | `v0.1.0` signed APK sudah di GitHub Releases; fitur belum lengkap |

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
- [ ] Model `Draft`, `Track`, `Segment`, `Material`, `TimeRange`
- [ ] Serialization JSON + schema version + migration
- [ ] CRUD track/segment + target/source time range
- [ ] Transaction, undo, redo, snapshot
- [ ] Autosave atomic (temp + rename)
- [ ] Canvas/rasio/background
- [ ] Group/composition
- [ ] Unit test model + transaksi

## F02 — Media Import & Gallery

Spek: [`docs/features/02-media-import-gallery.md`](docs/features/02-media-import-gallery.md)

- [x] Spek
- [ ] Android Photo Picker + multi-select
- [ ] Copy ke workspace privat + metadata
- [ ] Probe durasi/resolusi/codec
- [ ] Thumbnail + waveform
- [ ] Proxy/transcode untuk device lemah
- [ ] Dukungan HEIF/WebP/GIF
- [ ] Import dari folder lokal / SAF
- [!] Cloud import (BUTUH BACKEND; fallback SAF)
- [ ] Unit test import + metadata

## F03 — Preview Player

Spek: [`docs/features/03-preview-player.md`](docs/features/03-preview-player.md)

- [x] Spek
- [ ] Media3 ExoPlayer + surface
- [ ] Composite timeline playback
- [ ] Frame-accurate seek
- [ ] Preview range + speed
- [ ] Playhead sync dengan timeline
- [ ] Performance metrics (FPS/dropped frame)
- [ ] Unit/instrumentation test seek

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

- [ ] Design system Zcut (warna, tipografi, komponen)
- [ ] Navigation graph semua layar
- [ ] Permission flow (media, camera, mic, notification)
- [ ] File provider + scoped storage
- [ ] Error handling + empty/loading state
- [ ] Accessibility (content description, touch target)
- [ ] Lokalisasi Indonesia + Inggris
- [ ] Unit test core (model, timeline, export plan)
- [ ] Instrumentation test alur utama
- [ ] CI build + test
- [ ] Release signing via GitHub Secrets
- [ ] GitHub Release APK
- [ ] Tidak ada secret/keystore di repo

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
- [x] `assembleRelease` + unit test di Actions
- [x] Signing dari secrets `ZCUT_KEYSTORE_BASE64`, `ZCUT_KEYSTORE_PASSWORD`,
      `ZCUT_KEY_ALIAS`, `ZCUT_KEY_PASSWORD`
- [x] Upload APK ke GitHub Releases
- [x] CI hijau
- [x] Link release: <https://github.com/az925-crypto/Zcut/releases/tag/v0.1.0>
- [x] Verifikasi signature: `apksigner verify --print-certs` →
      `CN=Zcut, OU=Mobile, O=Zcut, L=Local, ST=Local, C=ID`

Catatan release `v0.1.0`: ini APK scaffold non-UI, bukan hasil akhir. Fitur
editor dan UI menyusul setelah ACC.

## Commit log

| Tanggal | Commit | Isi |
|---|---|---|
| 2026-10-23 | `dadff9c` | Spesifikasi 31 fitur + mockup Stage 2 interaktif |
| 2026-10-23 | `c34923a` | Scaffold Android non-UI + workflow signed release |
| 2026-10-23 | `6102ad2` | CI pakai Android SDK bawaan runner |
| 2026-10-23 | `4cc000a` | Perbaiki fixture test quarantine; CI hijau |
| 2026-10-23 | `v0.1.0` | Signed release APK di GitHub Releases |

## Ringkasan akhir

Diisi setelah semua fitur selesai dan release sukses.
