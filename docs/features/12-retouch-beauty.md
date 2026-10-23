# 12 — Retouch, Beauty & Body

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.xt.retouch` (21.351 class), `com.vega.retouch`,
  `com.xt.edit` (4.650 class).
- Native: `libretouch_sdk.so` (2 MB), `libfastcv`, `libbytenn`.
- Struktur: `com.xt.retouch.sdk.render` (`RenderRequest`, `RenderResult`,
  `IRenderEnv`), `sdk.edit` (draft/template/session), `business/*`.
- Module: `BeautyAdjustParam`, `BodyReshapeInfo`, `HeadReshapeInfo`,
  `EnableMultFacesEffect`, `GetManualBeautyAlgorithmWorkspace`,
  `CopyMattingDataOnMattingEdit`.
- Asset: `assets/zip/FaceReshape_V2.zip`, `FaceReshape_V3.zip`,
  `ManualReshape.zip`, `XT_Removal_Pen.zip`, `beauty_kira_*.json`.

## Cakupan

| Grup | Contoh |
|---|---|
| Face | skin smooth, whiten, acne, blemish, eye enlarge, eye brighten, face slim, jaw, chin, nose |
| Makeup | lipstick, blush, eyeliner, eyeshadow, eyebrow, lashes |
| Body | slim, long legs, waist, hip, shoulder |
| Hair | hair color, hair volume |
| Filter | retouch filter, tone |
| Removal | object eraser, blemish pen |

## Pipeline

```
foto/video frame
  → face detect + landmark (106 titik)
  → skin mask / hair mask / body mask
  → warp (reshape) → blend
  → color (smooth/whiten/makeup)
  → render
```

`FaceReshape_V3` adalah effect package (AmazingFeature) untuk warp wajah;
`ManualReshape` untuk warp manual dengan brush.

## Model parameter

```json
{
  "beauty": {
    "face": {"smooth": 0.5, "whiten": 0.3, "eye_enlarge": 0.2,
             "face_slim": 0.4, "jaw": 0.1, "nose": 0.0},
    "makeup": {"lipstick": {"id": "c1", "intensity": 0.6},
               "blush": {"id": "b2", "intensity": 0.4}},
    "body": {"slim": 0.2, "long_legs": 0.3},
    "filter": {"id": "f1", "intensity": 0.5}
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class BeautyParams(
    val smooth: Float = 0f,
    val whiten: Float = 0f,
    val eyeEnlarge: Float = 0f,
    val faceSlim: Float = 0f,
    val lipstick: Makeup? = null,
    val bodySlim: Float = 0f,
)

fun applyBeauty(src: Bitmap, landmarks: FaceLandmarks, p: BeautyParams): Bitmap {
    var out = src
    if (p.smooth > 0f) out = bilateralFilter(out, radius = 3, sigma = p.smooth * 20)
    if (p.whiten > 0f) out = screenBlend(out, p.whiten * 0.2f)
    if (p.faceSlim > 0f) out = localWarp(out, landmarks.jaw, -p.faceSlim * 0.1f)
    if (p.eyeEnlarge > 0f) out = localWarp(out, landmarks.eyes, p.eyeEnlarge * 0.1f)
    return out
}
```

Untuk video, parameter harus stabil antar frame; gunakan smoothing temporal
pada landmark (One Euro Filter) agar tidak bergetar.

## Catatan paritas

- **P0**: smooth, whiten, face slim, filter.
- **P1**: makeup, eye, body, manual reshape, sebelum/sesudah.
- **P2**: multi-face, hair, video beauty real-time, AI retouch.

## Unknown

- Jumlah landmark dan model face yang dipakai tidak terbaca (kemungkinan
  model internal `tt_fsnew_*` dari EffectSDK).
- Apakah beauty diproses di GPU EffectSDK atau CPU retouch SDK; keduanya ada.
