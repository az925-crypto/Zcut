# 19 — Export & Render

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Client: `ExportClient`, callback `IExportCancelCallback`,
  `IExportStartCallback`, `IExportStatusCallback`,
  `IExportPauseOrResumeCallback`, `ExportCompletionCallbackWrapper`,
  `ExportProgressCallbackWrapper`, `IEnableCompileNotSendEofCallback`,
  `IExportCompositionToFileSyncCallback`.
- `ExportStartReqStruct` memuat: `config`/`configs`, `output`/`outputs`,
  `progress_callback_flag`, `completion_callback_flag`,
  `information_callback_flag`, `usage_type`, `offset`, `size`.
- `ExportConfig` (via `AdapterParamModuleJNI`) memiliki ±90 field; yang terbaca:
  `codec, bitrate, bitrate_level, fps, size, profile, preset, crf, maxrate,
  qpoffset, gop_size, channels, audio_bitrate, audio_sampling, audio_quality,
  color_space, enable_hdr, enable_hdr_vivid_metadata, enable_10bit_export,
  enable_parallel_export, max_number_of_parallel, enable_vbr, fast_start,
  hardEncode, hardDecode, is_video_only, is_audio_only, needWaterMark,
  compile_with_alpha, gif_with_alpha, is_support_remux, is_separate_synthesis,
  is_fast_trim, enable_smart_complement_frame, enable_ffmpeg_writer,
  exportAspectRatio, prerender_cache_path, compile_json, encode_option_json`.
- Native: VESDK (`VESDK-TEAvcEncoder`), `libttffmpeg`, `libttdemux`,
  hardware encoder.
- UI: `com.vega.export.edit.resultpage.*` (quick action, share, cover,
  AI suggest, text), `imageeditor`, `template/resultpage`.
- `assets/simkit_rate_settings/other.json` — pengaturan bitrate/gear.

## Format output

| Output | Dukungan |
|---|---|
| MP4 H.264/HEVC/ByteVC1 | utama |
| MP4 alpha (`compile_with_alpha`) | overlay/transparan |
| GIF (alpha opsional) | `gif_with_alpha` |
| Audio-only (M4A/MP3) | `is_audio_only`, `audio_path` |
| Frame/cover | `has_cover_opt`, cover result page |
| HDR / 10-bit | `enable_hdr`, `enable_10bit_export` |

## Alur export

```
result page
  → pilih resolusi/fps/codec/bitrate
  → validasi (durasi, ukuran, storage, codec)
  → ExportStart (config + output path)
  → progress callback (per frame / per stage)
  → compile video + audio
  → mux (remux jika memungkinkan)
  → tulis ke galeri / workspace
  → cover + share sheet
```

**Optimasi yang terlihat dari config:**

- `is_support_remux` — jika hanya trim/container berubah, hindari re-encode.
- `is_fast_trim` — potong keyframe terdekat.
- `enable_parallel_export` + `max_number_of_parallel` — render beberapa segmen
  paralel lalu concat.
- `prerender_cache_path` — segmen berat sudah dirender sebelumnya.
- `enable_smart_complement_frame` — interpolasi frame saat fps naik.

## Model request

```json
{
  "output": "/workspace/export/out.mp4",
  "config": {
    "codec": "h264",
    "width": 1080, "height": 1920,
    "fps": 30, "bitrate": 12000000, "bitrate_level": "high",
    "audio_bitrate": 128000, "audio_sampling": 48000, "channels": 2,
    "hardEncode": true, "hardDecode": true,
    "enable_hdr": false, "enable_parallel_export": true,
    "max_number_of_parallel": 4,
    "is_support_remux": true,
    "needWaterMark": false,
    "start_time": 0, "end_time": 30000000
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class ExportRequest(
    val output: File,
    val width: Int,
    val height: Int,
    val fps: Int,
    val codec: Codec = Codec.H264,
    val bitrate: Int,
    val audioBitrate: Int = 128_000,
    val range: TimeRange? = null,
    val watermark: File? = null,
    val hdr: Boolean = false,
    val alpha: Boolean = false,
)

interface ExportEngine {
    fun export(request: ExportRequest, draft: Draft): Flow<ExportProgress>
    fun cancel()
}

sealed interface ExportProgress {
    data class Stage(val name: String) : ExportProgress
    data class Percent(val value: Int, val ptsUs: Long) : ExportProgress
    data class Done(val file: File, val cover: File?) : ExportProgress
    data class Failed(val code: Int, val message: String) : ExportProgress
}

class ExportController(
    private val engine: ExportEngine,
    private val mediaStore: MediaStoreWriter,
    private val scope: CoroutineScope,
) {
    fun start(draft: Draft, req: ExportRequest) = scope.launch {
        engine.export(req, draft)
            .catch { emit(ExportProgress.Failed(-1, it.message ?: "export")) }
            .collect { p ->
                when (p) {
                    is ExportProgress.Done -> mediaStore.publish(p.file)
                    else -> _state.value = p
                }
            }
    }
}
```

Validasi sebelum export:

```kotlin
fun validate(req: ExportRequest, draft: Draft): List<String> = buildList {
    if (req.width % 2 != 0 || req.height % 2 != 0) add("dimensi harus genap")
    if (req.fps !in 1..120) add("fps tidak valid")
    if (draft.durationUs <= 0) add("timeline kosong")
    val free = req.output.parentFile!!.usableSpace
    val estimate = req.bitrate.toLong() * draft.durationUs / 8_000_000
    if (free < estimate * 2) add("storage kurang")
}
```

## Catatan paritas

- **P0**: MP4 H.264, resolusi/fps/bitrate, progress, cancel, simpan galeri.
- **P1**: HEVC, audio-only, cover, hardware encode, remux/fast trim.
- **P2**: alpha, HDR/10-bit, parallel export, prerender, smart frame, codec
  ByteVC1/VC2.

## Unknown

- Nilai enum codec/quality/bitrate level tidak terbaca; hanya nama field.
- Apakah `enable_parallel_export` aktif default di device tertentu.
- Watermark AIGC (`exportAIGCWaterMarkConfig`) punya aturan tersendiri yang
  tidak sepenuhnya terbaca.

## Status implementasi Zcut (M13)

Engine: `core/render/Media3ExportEngine` (`VideoRenderEngine`), dibangun per
layar export seperti preview player — satu draft, satu `Transformer`.

- **Komposisi**: `PreviewPlanBuilder` → `Media3CompositionBuilder.build(plan,
  presentationShortSide = preset.resolution.shortEdge)`. Gerbang sumber sama
  dengan preview, jadi klip yang tidak bisa diputar tidak bisa diekspor.
- **Kontainer**: MP4, video H.264, audio AAC
  (`Transformer.Builder.setVideoMimeType/setAudioMimeType`). Format lain di
  `ExportPreset` (WebM/GIF/PNG) belum didukung dan disebut apa adanya di panel
  setelan lewat labelnya.
- **Resolusi**: `Presentation.createForShortSide(shortEdge)` sebagai effect
  level-komposisi (bukan per item, supaya klip beda aspek tidak menghasilkan
  ukuran frame berbeda).
- **Progress**: polling `Transformer.getProgress(ProgressHolder)` tiap 400 ms →
  `Preparing`/`Rendering(percent)`/`Finalizing`/`Completed`/`Failed`.
- **Cancel**: `Transformer.cancel()` dipanggil saat pengguna menekan Batal dan
  saat flow berhenti (layar ditutup di tengah render).
- **Kelola file**: hasil ditulis ke `filesDir/exports/<nama>-<timestamp>.mp4`.
  Nama file dibuat `ExportPlan.fileName` (nama draft disanitasi agar tidak bisa
  keluar folder). "Simpan" menyalin ke galeri `Movies/Zcut` via `MediaStore`
  (Android 10+ tanpa izin; Android 8–9 ke folder Movies milik app). "Bagikan"
  memakai share sheet sistem lewat `FileProvider` yang hanya mengekspos
  `exports/`.
- **Smart cover**: `core/media/CoverFrame` menulis satu JPEG (frame 1/3 durasi)
  bila toggle dinyalakan.

Catatan jujur / belum ada:

- Bitrate, frame rate video, dan sample rate audio akhir ditentukan encoder
  perangkat (`Bitrate: Otomatis`), bukan angka di `ExportPreset`; hal ini
  dinyatakan di hint layar export.
- Teks, efek, mask/chroma, sticker, dan filter belum di-burn ke video final —
  sama seperti batas preview M3; layer itu ditambahkan milestone-nya sendiri.
- HEVC, audio-only, GIF, alpha, HDR/10-bit, parallel/prerender/remux belum ada.
- Default export di Pengaturan (`export_resolution`, `export_frame_rate`,
  `export_audio_sample_rate`) belum mengalir ke draft; layar export membaca
  `ProjectSettings.exportPreset` milik draft itu sendiri (masuk backlog).

Uji: `ExportPlanTest` (nama file aman, estimasi ukuran, label durasi),
`ZcutDestinationsTest` (rute `export`/`export-done` + argumen nama file).
