# 26 — Camera/Recorder & Photo Editor

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti kamera

- Package: `com.vega.recorder` (2.205 class), `recorder/effect` (348),
  `com.vega.edit.videocapture` (177), `com.vega.recorder.view.prompt`
  (`RecorderPromptEditActivity`).
- Permission: `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION` (opsional),
  `READ_MEDIA_*`.
- Native: EffectSDK (BEF) untuk preview ber-filter, `libttffmpeg`,
  `libaudioeffect`.
- Activity recorder terlihat di manifest: `LvRecordActivity`,
  `MainCameraSelectActivity`, `RecorderPromptEditActivity`,
  `RecordSamePreviewActivity`.

## Bukti photo editor

- `com.vega.image` (5.988): `edit/clipflow2/chat2edit` (399),
  `edit/component/{edit,biz,ui}`, `edit/panel/{edit,element,aichat}`,
  `edit/template/*`, `edit/clipflow2/{smartcover,template}`.
- `com.xt.retouch` (21.351) dan `com.xt.edit` (4.650) — engine foto.
- Client: `ImageEditorCommonClient`, `GetImageEditorCommonInfo`,
  `InitImageEditor`, `ApplyImageEditorInspireLayerTemplate`,
  `HasUsedAnySpecialEffectForNewImageEditor`.
- Activity: `ImageDraftViewerActivity`, `ImageEditor*`.
- Brand “Hypic” muncul di string/domain (`hypic.com`, `hypic.onelink.me`) —
  CapCut memakai teknologi editor foto Hypic.

## Fitur kamera

| Fitur | Detail |
|---|---|
| Record video | resolusi/fps, front/back, flash, grid |
| Photo | capture, timer, ratio |
| Beauty | smooth/whiten/face (real-time preview) |
| Filter/effect | EffectSDK preview |
| Speed | slow/fast record |
| Countdown/timer | 3s/10s |
| Hands-free | gesture/stop |
| Template record | RecordSame / template |
| Voiceover | rekam saat capture |

Pipeline kamera:

```
Camera2/CameraX → SurfaceTexture → EffectSDK (beauty/filter)
  → encoder (MediaCodec/VESDK) → file
  → (opsional) langsung tambah ke draft
```

## Fitur photo editor

- Crop/rotate/flip/straighten, aspect ratio.
- Adjust (brightness/contrast/saturation/HSL/curves).
- Filter/LUT.
- Beauty & makeup (wajah).
- Cutout/background remove + AI background.
- Text, sticker, shape, brush, mosaic.
- Blur/pen tool, object eraser.
- AI: enhance, expand (outpaint), replace, generate.
- Template foto, collage.
- Export JPG/PNG/WebP.

## Implementasi referensi kamera (clean-room)

```kotlin
class CameraRecorder(
    private val context: Context,
    private val effects: EffectPipeline,
    private val encoder: VideoEncoder,
) {
    fun start(config: RecordConfig, previewSurface: Surface) {
        val session = CameraSession(context, config)
        session.onFrame = { tex, pts ->
            val out = effects.process(tex, pts)   // beauty + filter
            encoder.encode(out, pts)
        }
        session.start(previewSurface)
    }
}
```

## Implementasi referensi photo editor (clean-room)

```kotlin
data class PhotoEditState(
    val source: Bitmap,
    val crop: RectF = RectF(0f, 0f, 1f, 1f),
    val rotation: Float = 0f,
    val adjust: AdjustParams = AdjustParams(),
    val filter: FilterParams? = null,
    val beauty: BeautyParams = BeautyParams(),
    val layers: List<Layer> = emptyList(),
)

fun render(state: PhotoEditState): Bitmap {
    var bmp = cropBitmap(state.source, state.crop)
    bmp = rotate(bmp, state.rotation)
    bmp = applyAdjust(bmp, state.adjust)
    state.filter?.let { bmp = applyLut(bmp, it) }
    bmp = applyBeauty(bmp, state.beauty)
    bmp = compositeLayers(bmp, state.layers)
    return bmp
}
```

Edit non-destruktif: simpan `PhotoEditState`, bukan bitmap hasil, agar bisa
di-undo dan diekspor ulang.

## Catatan paritas

- **P0**: record video, photo, beauty dasar, crop/adjust/filter foto.
- **P1**: template record, speed, timer, cutout, text/sticker, export.
- **P2**: AI photo (enhance/expand/replace), real-time AR, collage,
  multi-layer.

## Unknown

- Camera API persis (Camera2 vs CameraX) tidak terbaca dari class saja;
  keduanya lazim.
- Resolusi/fps maksimum per device tidak terbaca.
- Seberapa banyak editor foto yang dibagi dengan Hypic vs CapCut.
