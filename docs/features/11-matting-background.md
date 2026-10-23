# 11 — Matting, Background Removal & Replace

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Client: `MattingClient`, `AIMattingClient` (`AIMattingClientPubApi`).
- Module: `AddMatting`, `AddCustomMatting`, `AddMattingListener`,
  `PrepareMatting`, `CopyMattingDataOnMattingEdit`, `SetMattingCachePath`,
  `UpdateVideoMattingWithFlagPath`, `ExportCurrentMattingFrame`,
  `GetGreenScreenForegroundBoundingBoxPosition`.
- Asset: `assets/zip/MattingBlendV2.zip`, `mattingBlend.zip`,
  `matting_config.zip`, `Brush2D_Eraser.zip`.
- AI background: `MattingAiBackgroundGenerateInterceptor` (export intercept),
  `AIGC` background generation.
- `com.vega.edit.matting` (699 class), `com.xt.retouch` cutout/AI background.
- Smart crop: `assets/smart_crop/smart_cut.ofx.so-{32,64}`,
  `algorithmConfig.json`.

## Yang dilakukan

1. **Matting** — pisahkan foreground (orang/objek) dari background per frame.
2. **Background remove** — hasil matting dipakai untuk transparansi.
3. **Background replace** — isi background dengan warna, blur, gambar, atau
   generate AI.
4. **Cutout** — versi foto (retouch): hapus background, AI background,
   eraser brush.
5. **Smart crop / auto reframe** — deteksi subjek lalu crop mengikuti subjek.

## Alur matting

```
pilih segmen
  → PrepareMatting (cek model & cache)
  → AddMatting (track algoritma per frame)
  → progress callback
  → hasil: mask video + foreground bounds
  → apply: background replace / blur / transparan
```

`CopyMattingDataOnMattingEdit` penting: saat segmen di-trim/di-split, hasil
matting harus disalin/dipotong agar tidak menghitung ulang.

## Model data

```json
{
  "matting": {
    "flag": 1,
    "path": "/workspace/matting/seg-1",
    "mode": "auto",
    "background": {
      "type": "blur",        // color | image | blur | transparent | ai
      "color": "#000000",
      "image": "",
      "blur": 0.5,
      "prompt": ""
    }
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
interface MattingEngine {
    suspend fun prepare(segment: Segment): MattingSession
    fun release()
}

data class MattingSession(
    val maskDir: File,
    val frameCount: Int,
    val fps: Float,
)

class MattingController(
    private val engine: MattingEngine,
    private val cache: MattingCache,
) {
    suspend fun ensureMatting(segment: Segment): MattingSession {
        cache.get(segment.materialId, segment.sourceTimeRange)?.let { return it }
        val session = engine.prepare(segment)
        cache.put(segment.materialId, segment.sourceTimeRange, session)
        return session
    }

    fun onSegmentTrimmed(old: Segment, new: Segment) {
        cache.copyRange(old.materialId, old.sourceTimeRange,
            new.materialId, new.sourceTimeRange)
    }
}
```

Background blur sederhana:

```glsl
vec4 bg = blur(texture2D(uTex, vUv), radius);
vec4 fg = texture2D(uTex, vUv);
gl_FragColor = mix(bg, fg, matte);
```

## Catatan paritas

- **P0**: background blur/color + matte manual sederhana.
- **P1**: auto matting on-device (MediaPipe/Selfie Segmentation), background
  image, cutout foto, eraser.
- **P2**: AI background generation, per-frame tracking, hair detail, video
  matting temporal consistency.

## Unknown

- Model matting yang dipakai (nama model tidak muncul jelas; ada
  `matting_config.zip`).
- Apakah hasil disimpan per frame (PNG) atau video alpha; path API menunjukkan
  folder, bukan file.
- Resolusi pemrosesan matting tidak terbaca.
