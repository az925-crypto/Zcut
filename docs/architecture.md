# Arsitektur CapCut 19.8.0

## 1. Identitas & packaging

| Field | Nilai |
|---|---|
| Package | `com.lemon.lvoverseas` |
| Label | CapCut |
| versionName / Code | 19.8.0 / 19801600 |
| minSdk / targetSdk / compileSdk | 23 / 36 / 36 |
| Launcher | `com.vega.main.MainActivity` |
| Split | `base.apk`, `split_config.arm64_v8a.apk`, `split_config.xhdpi.apk`, `split_config.in.apk`, `split_config.id.apk` |
| Dex | 35 (`classes.dex` … `classes34.dex`) |
| Native | 162 `.so` arm64-v8a |
| Signing/obfuscation | R8 + name obfuscation, string encryption di beberapa modul, Play Integrity/TicketGuard, `libmetasec_ov`, `libpglarmor` |

Build metadata yang terlihat: VESDK `21.2.0.0_alpha_20260330144010`, middleware
Retouch `82db5bf…`, branch internal `feature/cc-agent-edit`. Ini menandakan
19.8.0 adalah build menjelang rilis dengan modul AI agent aktif.

## 2. Peta layer

```
┌──────────────────────────────────────────────────────────────────┐
│ UI / Feature layer                                               │
│  com.vega.main (home) · com.vega.edit (editor) · com.vega.image   │
│  com.vega.cutsameedit (template) · com.vega.ailabagent (agent)   │
│  Compose + ViewBinding + Lynx (growth/paywall) + OTIS inflater    │
├──────────────────────────────────────────────────────────────────┤
│ Application services                                             │
│  com.vega.draft · com.vega.export · com.vega.audio · com.vega.pay │
│  com.vega.cloud · com.vega.share · com.vega.publish · com.vega.ad │
├──────────────────────────────────────────────────────────────────┤
│ Editing domain API                                               │
│  com.vega.middlebridge.client.*  (95 client, 1.390 nama operasi/tipe) │
│  com.vega.middlebridge.swig.*    (SWIG binding ke C++ Lyra)      │
│  com.vega.ve.api.*               (VESDK helper/player/export)     │
├──────────────────────────────────────────────────────────────────┤
│ Native engines                                                    │
│  libmiddle-bridge.so  → Lyra NLE (draft, track, segment, render)  │
│  libcccreator.so      → VESDK 21.2 + EffectSDK (BEF) + codec      │
│  libclipflow.so       → DAG workflow engine (AI pipeline)         │
│  libretouch_sdk.so    → XT Retouch (beauty/makeup/reshape)        │
│  libmiddleware.so     → media/algorithm middleware                │
│  libttffmpeg / libttdemux / libttmplayer / libyuv / codec         │
└──────────────────────────────────────────────────────────────────┘
```

### Kenapa ada dua “editor”

- `com.vega.ve.api` = API lama/umum ke VESDK (player, compile, metadata).
- `com.vega.middlebridge` = **Lyra**, NLE baru berbasis draft JSON. Hampir semua
  operasi timeline (add/remove/update segment, keyframe, mask, template,
  export) lewat sini. 95 `*Client` di `com.vega.middlebridge.client` adalah
  facade per domain.
- `com.vega.clipflow` + `libclipflow.so` = engine DAG untuk pipeline AI
  (ASR → highlight → arrange → apply draft). 428 node class terdaftar.

## 3. Modul aplikasi (dari package & manifest)

| Domain | Package utama | Bukti |
|---|---|---|
| Home/discovery | `com.vega.main.home`, `com.vega.feedx` | template feed, banner, tool page |
| Editor | `com.vega.edit.editpage`, `com.vega.edit.base` | 2.325 class editpage |
| Timeline/draft | `com.vega.draft`, `com.vega.middlebridge` | 14.167 class SWIG |
| Preview | `com.vega.ve.api`, `PlayerClient` | 80+ callback player |
| Export | `com.vega.export`, `ExportClient` | `ExportConfig` 90+ field |
| Template/CutSame | `com.vega.cutsameedit`, `com.vega.libcutsame`, `com.vega.edward` | 5.930 + 4.392 + 1.370 class |
| Text/subtitle | `com.vega.libsticker`, `com.vega.transcriptedit` | caption, lyric, AI emoji |
| Audio | `com.vega.audio` | tone/TTS 2.135, AI music 1.077, library 726 |
| Beauty | `com.xt.retouch`, `com.vega.retouch` | 21.351 class retouch |
| Photo editor | `com.vega.image`, `com.xt.retouch` | 5.988 class image |
| AI agent | `com.vega.ailabagent` | chat, ability, autocut |
| AI generation | `com.vega.edit.aigenerator`, `materialgeneration`, `inpainting`, `outpainting` | 1.667 + 481 + 411 + 357 |
| Digital human | `com.vega.edit.digitalhuman`, `com.vega.audio.lipsync` | 1.114 + 366 |
| Cloud | `com.vega.cloud` | autobackup, group, review, brand, sharedraft |
| Monetization | `com.lemon.clipmonetize` | 2.816 class commerce page |
| Share | `com.vega.share` | TikTok, IG, YouTube, LinkedIn, Lemon8 |
| Resource | `com.vega.effectplatform`, `com.vega.packedresource` | Artist, Gecko, Vimo |
| Telemetry | `com.bytedance.applog`, `apm6`, Slardar | log, trace, memory |

## 4. Alur dokumen/draft

```
Media picker
   ↓ URI + metadata
Import/transcode (proxy bila perlu)
   ↓
Draft (JSON, schema version 142.0.0)
   ├── canvas_config  : width/height/ratio/background
   ├── config         : video_mute, maintrack_adsorb, export_range, zoom
   ├── tracks[]       : type, attribute, flag, segments[]
   ├── materials{}    : videos[], audios[], texts[], stickers[], ...
   ├── keyframes{}    : per material/segment
   └── segments[]     : type, target/source timerange, clip, speed, volume
   ↓
Lyra NLE (libmiddle-bridge)  ←→  VESDK preview / EffectSDK render
   ↓
ExportConfig → compile → mux (TTFFmpeg) → MP4/GIF/audio
```

Skema mini-draft yang ditemukan di `classes2.dex` memakai header:

```json
{"header":{"version":"1","type":"minidraft","modify_timestamp":123456},
 "draft":{"id":"…","new_version":"…142.0.0","name":"…","duration":1000,
 "fps":30,"canvas_config":{…},"tracks":[{"id":"","attribute":0,
 "segments":[{"id":""}]}],"config":{…}},"segments":[…]}
```

Tipe segment yang terdaftar: `segment_video`, `segment_audio`, `segment_text`,
`segment_sticker`, `segment_image_sticker`, `segment_text_template`,
`segment_video_effect`, `segment_composition`, `segment_tail_leader`,
`segment_handwrite`, `segment_filter`, `segment_shape`,
`segment_picture_adjust`, `segment_adcube`.

Detail: `features/01-draft-timeline.md`.

## 5. Engine media

| Kebutuhan | Engine | Bukti |
|---|---|---|
| Decode/encode | VESDK (`libcccreator`), TTFFmpeg, ByteVC1/VC2 | `VESDK-TEAvcEncoder`, `libByteVC1_dec`, `libbyteVC2dec` |
| Preview | TTMPlayer + VESDK player | `libttmplayer`, `PlayerClient` callbacks |
| Effect/AR | EffectSDK (BEF) | `com.bef.effectsdk.*`, `bef_effect_get_sdk_version` |
| Color/effect package | AmazingFeature (Lua + xshader + LUT) | `assets/zip/*.zip` |
| Beauty | XT Retouch | `libretouch_sdk`, `com.xt.retouch` |
| Audio DSP | native audio effect | `libaudioeffect`, `libfdk-aac`, `libopus` |
| Speech | speech SDK + Whisper | `libspeechsdk`, `libwhisper` |
| Smart crop | OFX plugin | `assets/smart_crop/smart_cut.ofx.so-*` |
| AI inference on-device | ByteNN + FastCV | `libbytenn`, `libfastcv` |
| NLE | Lyra | `libmiddle-bridge.so` (49.020 JNI symbol) |
| Workflow AI | ClipFlow | `libclipflow.so` |

## 6. Data & storage

- **Draft**: file JSON di workspace aplikasi; operasi atomic lewat
  `DraftBeginTransaction` / `DraftCommit` / `DraftAmend`.
- **Room**: `DraftDatabase` (versi 2, `Migration1To2`), entity `StableEntity`,
  `MediaDataTransEntity`; dipakai untuk metadata stabil & transcode.
- **KV**: `com.vega.kv` + Keva native (`libkeva`).
- **Native DB**: WCDB (`libwcdb`) untuk pesan/agent.
- **Cloud**: auto-backup, shared draft, review, brand kit, enterprise/team.

## 7. Networking

Domain yang muncul di dex:

- `feed-api.capcutapi.com`, `editor-api.capcutapi.com`
- `aivideo-normal.capcutapi.com`
- `mon-v2-boot.capcutapi.com`, `mon.byteoversea.com`
- `sf16-sg.tiktokcdn.com`, `lf16-beecdn.ibytedtos.com`
- `www.capcut.com`, `www.capcut.net`

Stack: Cronet (`libsscronet`), TTNet, OkHttp, Protobuf/Gson, upload SDK
(`com.ss.bduploader`), CDN download, Gecko resource update.

## 8. AI architecture

Dua lapis:

1. **ClipFlow DAG** — pipeline deterministik untuk AI clipper, template,
   ASR, matting, dubbing, AI music, dsb. Node contoh: `ASRNode`,
   `VideoContentUnderstandNode`, `AIRecognitionHighlightNode`,
   `AITemplateRemoteTaskParamNode`, `AiAudioBuildRequestNode`.
2. **AI Lab Agent** — `com.vega.ailabagent.unifyagent`: chat, tool calling,
   conversation state, preview, history, suggestion. Dipakai untuk
   “AI editor” yang mengubah draft lewat tool.

Model pihak ketiga yang terlihat: ElevenLabs (TTS), Microsoft Azure TTS,
Qwen TTS, model internal ByteDance (V3 streaming), Whisper (on-device ASR).

## 9. UI

- Compose dipakai masif (`androidx.compose`, `com.vega.ui.*`, resource
  `composeResources/...`).
- Layar lama masih View/XML. Ada **OTIS** (`com.bytedance.otis`) yang
  meng-generate layout inflater untuk mempercepat startup.
- Lynx dipakai untuk halaman growth/paywall (`assets/offline/image_lynx_*`).
- Arsitektur: Koin DI (`KoinEditModule`, `KoinVEModule`, `KoinShareModule`,
  `KoinPayOverseaModule`), ViewModel + StateFlow/Flow, Jedi untuk sebagian
  modul lama.

## 10. Monetisasi & growth

- `com.lemon.clipmonetize.biz.commerce_page.lv_oversea` (2.816 class):
  paywall Pro/Standard, weekly/monthly/annual, free trial, credits.
- Google Play Billing + **GlobalPay** (ByteDance) + PayPal/Paylution.
- Ads: AppLovin MAX, Pangle/TTAd, MBridge, Vungle, Facebook Audience.
- Credit system: `CreditPageInfoData`, `ai_model_product_id`, reward ads, AI generation quota.
- Push: ByteDance push + FCM; AppsFlyer untuk attribution.

## 11. Rekomendasi membangun “versi sendiri”

Urutan termurah → paling mahal:

1. **Pakai engine yang ada** (Media3/ExoPlayer + FFmpeg + GPU shader). Jangan
   bikin NLE dari nol.
2. **Model data dulu**: draft JSON + operasi timeline. Ini 40% kompleksitas.
3. **Preview**: satu player + render graph. Baru setelah stabil tambah effect.
4. **Export**: mulai dari FFmpeg command line, lalu hardware encoder.
5. **AI**: integrasi API (ASR/TTS/LLM) sebelum mencoba on-device model.
6. **Template**: mulai dari format sendiri; interoperabilitas template CapCut
   tidak realistis tanpa kontrak internal.
7. **Monetisasi** paling akhir; jangan ikut kompleksitas paywall CapCut.

Detail prioritas ada di `parity-checklist.md`.

## 12. Risiko & batasan

- **Legal**: jangan salin asset/effect/font/template. Effect package memakai
  format AmazingFeature + Lua; gunakan engine sendiri.
- **Skala**: 275k class dan 162 library menunjukkan tim besar; parity 100%
  bukan target realistis untuk solo dev.
- **Server-side**: AI, template marketplace, music library, dan cloud tidak
  bisa direplikasi dari APK saja.
- **Obfuscation/anti-tamper**: beberapa string dienkripsi; analisis ini tidak
  membongkar proteksi dan tidak menyediakan cara melewatinya.

---

# Bagian II — Arsitektur Implementasi Zcut

Dokumen di atas adalah peta aplikasi asli. Bagian ini adalah keputusan
implementasi untuk aplikasi baru `com.zaaam.zcut`.

## Stack

| Layer | Pilihan | Alasan |
|---|---|---|
| UI | Jetpack Compose + Material 3 | cepat, modern, mudah dites |
| Navigasi | Navigation Compose | satu graph untuk semua layar |
| State | ViewModel + StateFlow | lifecycle-aware, testable |
| DI | manual `AppContainer` | tanpa framework berat, build cepat |
| Data | Room + DataStore + kotlinx.serialization | draft, project, preferensi |
| Preview | Media3 ExoPlayer | stabil, hardware decode |
| Render | Media3 Transformer + MediaCodec/OpenGL | export lokal tanpa server |
| Efek | `media3-effect` + shader sendiri | tidak memakai EffectSDK asli |
| Foto | CameraX + Bitmap/Canvas/GPU | camera dan photo editor |
| Matting | ML Kit Subject Segmentation | on-device, tanpa server |
| TTS | Android `TextToSpeech` | lokal, tanpa API key |
| Background job | WorkManager | export/import tahan proses mati |
| Analitik | log lokal (opsional) | tanpa telemetry pihak ketiga |

## Struktur paket

```
com.zaaam.zcut
├── ZcutApp.kt
├── MainActivity.kt
├── core/
│   ├── model/          Draft, Track, Segment, Material, Keyframe
│   ├── data/           Room, DataStore, repository
│   ├── media/          probe, import, thumbnail, waveform
│   ├── render/         composition builder, exporter, effects
│   └── util/           time, ids, files
├── feature/
│   ├── home/           projects, templates, discover
│   ├── editor/         preview, timeline, inspector
│   ├── tools/          semua panel fitur
│   ├── capture/        camera
│   ├── photo/          photo editor
│   ├── ai/             AI studio, agent, clipper
│   ├── export/         export sheet, result
│   └── settings/       profil, storage, about
└── ui/
    ├── theme/          warna, tipografi, komponen
    └── components/     timeline, scrubber, sheet, chip
```

## Model draft

Draft disimpan sebagai JSON di workspace privat aplikasi dan diindeks di Room.
Operasi edit memakai transaksi + snapshot untuk undo/redo. Setiap perubahan
menulis file sementara lalu rename atomik.

```json
{
  "schema": 1,
  "id": "uuid",
  "name": "Project",
  "canvas": {"width": 1080, "height": 1920, "fps": 30},
  "durationUs": 0,
  "tracks": [],
  "materials": {},
  "createdAt": 0,
  "updatedAt": 0
}
```

## Render

1. Bangun `Composition` Media3 dari track/segment.
2. Terapkan `ScaleAndRotateTransformation`, `Crop`, `OverlayEffect`,
   `RgbAdjustment`, `HslAdjustment`, LUT shader.
3. Render audio mix dengan volume/fade; TTS/voiceover sebagai track audio.
4. Export H.264/AAC ke cache, lalu publish ke MediaStore.
5. Progress dilaporkan per tahap; cancel menghentikan transformer.

Fitur codec lanjutan (alpha, HDR/10-bit, parallel export, remux) menjadi
pekerjaan lanjutan dan ditandai di `PROGRESS.md`.

## Fallback lokal

| Kebutuhan | Fallback Zcut |
|---|---|
| TTS | Android TextToSpeech, pilih voice sistem |
| Voice clone | UI tersedia, proses ditandai `BUTUH BACKEND` |
| AI music | generator nada/loop prosedural sederhana |
| AI image/video | UI + import hasil; generasi ditandai `BUTUH BACKEND` |
| ASR | `SpeechRecognizer` + edit manual/SRT |
| Terjemahan | ML Kit Translation on-device (opsional) |
| Matting | ML Kit Subject Segmentation |
| Smart crop | saliency heuristic + face detection |
| Cloud | folder lokal + export/import zip |
| Template marketplace | template bawaan + impor template sendiri |
| Digital human | preview template; render ditandai `BUTUH BACKEND` |

## Prinsip

- Tidak ada network call ke domain CapCut.
- Tidak ada billing, ads, credit, atau watermark.
- Tidak ada penyalinan aset; semua ikon/font/effect dibuat sendiri.
- Fitur yang belum bisa lokal tetap terlihat di UI dengan status jelas, bukan
  disembunyikan di balik paywall.
