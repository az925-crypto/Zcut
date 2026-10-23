# Feature Index — 31 Area Fitur

Semua fitur di bawah ini **gratis dan tanpa watermark**. Kolom `Backend`
menjelaskan ketergantungan yang tidak bisa dipenuhi murni on-device.

Legenda:

- `LOKAL` — dapat dijalankan on-device.
- `LOKAL*` — on-device dengan model/dependency lokal (ML Kit, TTS sistem, DSP).
- `BUTUH BACKEND` — kualitas/paritas penuh membutuhkan server/model sendiri.
- `TIDAK ADA` — sengaja tidak direkonstruksi (monetisasi/gating).

| ID | Area | Spek | Pri | Implementasi Zcut | Backend |
|---|---|---|---|---|---|
| 01 | Draft & timeline core | `features/01-draft-timeline.md` | P0 | JSON draft, track, segment, undo | LOKAL |
| 02 | Media import & gallery | `features/02-media-import-gallery.md` | P0 | Photo Picker, copy, probe, thumbnail | LOKAL |
| 03 | Preview player | `features/03-preview-player.md` | P0 | Media3 ExoPlayer + custom surface | LOKAL |
| 04 | Timeline editing | `features/04-timeline-editing.md` | P0 | trim, split, move, snap, ripple | LOKAL |
| 05 | Speed, curve, motion | `features/05-speed-curve-motion.md` | P0/P1 | retime, reverse, curve, stabilizer | LOKAL* |
| 06 | Keyframe | `features/06-keyframe.md` | P0 | posisi, scale, rotasi, opacity, easing | LOKAL |
| 07 | Transition | `features/07-transition.md` | P0/P1 | cut, dissolve, fade, library lokal | LOKAL |
| 08 | Filter & color | `features/08-filter-color.md` | P0/P1 | LUT, adjust, HSL, curves, wheels | LOKAL |
| 09 | Video effects | `features/09-video-effects.md` | P1 | shader/effect pack sendiri | LOKAL |
| 10 | Mask & chroma | `features/10-mask-chroma.md` | P0/P1 | shape mask, brush, chroma key | LOKAL |
| 11 | Matting & background | `features/11-matting-background.md` | P1 | ML Kit subject segmentation, smart crop | LOKAL* |
| 12 | Retouch & beauty | `features/12-retouch-beauty.md` | P1 | GPU beauty, makeup, reshape | LOKAL |
| 13 | Sticker & tracking | `features/13-sticker-tracking.md` | P1/P2 | sticker layer; tracking lanjutan | LOKAL* / BUTUH BACKEND |
| 14 | Text & typography | `features/14-text-typography.md` | P0/P1 | font, style, animasi, text template | LOKAL |
| 15 | Captions & transcript | `features/15-captions-transcript.md` | P1/P2 | manual/SRT, SpeechRecognizer, translate | LOKAL* / BUTUH BACKEND |
| 16 | Audio editing | `features/16-audio-editing.md` | P0/P1 | volume, fade, beat, DSP, dubbing | LOKAL* / BUTUH BACKEND |
| 17 | TTS & voice clone | `features/17-tts-voice-clone.md` | P1/P2 | Android TTS; clone | LOKAL* / BUTUH BACKEND |
| 18 | AI music | `features/18-ai-music.md` | P2 | generator prosedural lokal; model musik | LOKAL* / BUTUH BACKEND |
| 19 | Export & render | `features/19-export-render.md` | P0/P1 | Media3 Transformer + MediaCodec | LOKAL* |
| 20 | Draft, cloud, collaboration | `features/20-draft-cloud.md` | P0/P2 | draft lokal; backup/kolaborasi | LOKAL / BUTUH BACKEND |
| 21 | Template, CutSame, smart vlog | `features/21-templates-cutsame.md` | P1/P2 | template lokal; marketplace/AI | LOKAL / BUTUH BACKEND |
| 22 | AI clipper & autocut | `features/22-ai-clipper-autocut.md` | P2 | heuristic lokal; ASR/highlight penuh | LOKAL* / BUTUH BACKEND |
| 23 | AI agent & AI writer | `features/23-ai-lab-agent.md` | P2 | rule-based assistant; LLM | LOKAL* / BUTUH BACKEND |
| 24 | AI image/video generation | `features/24-ai-generation.md` | P2 | import/placeholder; generasi | BUTUH BACKEND |
| 25 | Digital human & lip sync | `features/25-digital-human.md` | P2 | template statis; render avatar | BUTUH BACKEND |
| 26 | Camera & photo editor | `features/26-camera-photo.md` | P1 | CameraX, GPU photo editor | LOKAL |
| 27 | Share & publish | `features/27-share-publish.md` | P1/P2 | Android share sheet; publish platform | LOKAL / BUTUH BACKEND |
| 28 | Account, premium, ads | `features/28-account-subscription-ads.md` | — | profil lokal; **tanpa paywall/ads** | TIDAK ADA |
| 29 | Resource, config, analytics | `features/29-resource-config-analytics.md` | P1 | config lokal; katalog/telemetry | LOKAL / BUTUH BACKEND |
| 30 | Split screen, gameplay, live photo | `features/30-split-gameplay-livephoto.md` | P1/P2 | layout, overlay, motion photo | LOKAL |
| 31 | Ad maker & script-to-video | `features/31-ad-maker-business.md` | P2 | template + TTS lokal; smart ad | LOKAL* / BUTUH BACKEND |

## Catatan premium

Fitur yang di aplikasi asli berbayar—AI generation, TTS premium, voice clone,
AI music, digital human, template marketplace, cloud collaboration, dan
sejenisnya—di Zcut **tidak memiliki gate**. Jika implementasi lokal belum
mampu mencapai kualitas penuh, fitur tetap tersedia dengan fallback lokal dan
ditandai `BUTUH BACKEND` untuk peningkatan kualitas, bukan untuk membuka akses.
