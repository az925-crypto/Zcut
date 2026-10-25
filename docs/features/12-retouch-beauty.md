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

## Bukti terverifikasi (M15)

Koordinat artefak yang dipakai apa adanya (bukan tebakan):

- **Key + rentang slider** — `assets/local_beauty_all.json` →
  `beauty_all_info.category[].items[]`; tiap item membawa `key`,
  `relative_min_value`, `relative_max_value`, dan `_ui_value` (posisi slider
  0–100). Grup: `Smooth skin`, `Concealer`, `Shape`, `Makeup`.
  Yang dipakai M15: `auto_buffing` 0–0.8, `auto_balance_skin` 0–0.8,
  `thin_face` 0.5–0.8, `eye_size` 0.5–0.8, `nose_wing` 0.5–0.6,
  `type_lips` 0–1.
  Terbaca tapi belum dipakai di UI (tanpa slot di mockup ACC):
  `auto_dark_circle` 0–1, `auto_eye_bright` 0–0.6, `auto_oil_remove` 0–0.8,
  `face_little_head` 0–0.6, `type_makeup_all` 0–1, `type_blush` 0–1.
- **Objek parameter engine** — `libmiddle-bridge.so` → simbol SWIG
  `Java_..._BeautyAdjustParamModuleJNI_BeautyAdjustParam_1name_1get/1value_1get`:
  `BeautyAdjustParam` adalah pasangan nama/nilai generik, bukan struct bernama.
  `BeautyFacePreset` membawa `head_reshapes`, `make_ups`, `manual_deformation`,
  `auto_beautys`, `skin_color_info`; `BeautyBodyPreset` membawa `body_reshapes`.
- **Mesh reshape** — `assets/zip/FaceReshape_V2/distortion.json`: satu entri per
  landmark (`pos`, `offsetX`/`offsetY`, `radiusX`/`radiusY`, `scale`, `type`;
  dua jenis distorsi). `FaceReshape_V3.zip` adalah effect package (AmazingFeature).
- **Warp manual** — `assets/ManualReshape/event.lua` (slider
  `MANUAL_RESHAPE_INTENSITY_SLIM` / `_STRETCH`), `assets/ManualReshape.zip`.
- **Bukan bukti beauty**: `assets/beauty_kira_*.json` adalah animasi Lottie UI.

## Implementasi M15 (on-device)

Semua di perangkat; tidak ada unggahan dan tidak ada server CapCut.

| Lapisan | File |
|---|---|
| Model | `BeautyConfig` di `core/model/Clip.kt` (`beauty`, aditif, draft lama `null`) |
| Domain | `core/domain/BeautyEditor.kt` (tabel key/rentang, `toSdk`, clamp, label, ringkasan) |
| Deteksi | `core/render/MlKitFaceDetector.kt` + kontrak `FaceGeometry`/`FaceBox`/`FacePoint` |
| Piksel | `core/media/BeautyProcessor.kt` (warp 1 pass, smooth/whiten terlokalisasi, lip tint) |
| UI | `feature/tools/BeautyScreen.kt` (rute `beauty`, clip tool) |

Aturan konversi slider→engine yang dipakai (dua aturan dari bukti):

1. Item auto-beauty/makeup (rentang mulai 0): kirim posisi itu sendiri, dibatasi
   rentang asli. Bukti: konfigurasi asli menyimpan `_ui_value: 40` dengan
   `_sdk_value: 0.4`.
2. Item shape (rentang mulai di baseline netral > 0): interpolasi lintas rentang
   asli, sehingga 0 = netral dan 100 = maksimum asli.

## Catatan paritas

- **P0**: smooth, whiten, face slim, filter.
- **P1**: makeup, eye, body, manual reshape, sebelum/sesudah.
- **P2**: multi-face, hair, video beauty real-time, AI retouch.

## Unknown

- **Konversi slider→engine untuk item shape tidak terbaca penuh.** Preset asli
  menyimpan nilai shape yang cocok dengan `0.5 + ui/200` (mis. ui 29 → 0.65),
  bukan interpolasi `[relative_min, relative_max]` seperti aturan yang dipakai
  Zcut. Aturan asli tidak bisa direkonstruksi dari konfigurasi saja, jadi Zcut
  memakai aturan interpolasi rentang (0 = netral) dan itu ditulis di sini.
- **Pemetaan `type` di `distortion.json` → `WarpKind` bersifat *inferred***:
  bentuk dua jenis distorsi terbaca dari file, tetapi mana `type` yang berarti
  geser vs skala diperkirakan dari struktur berkas, tidak diamati di kode.
  Magnitudo warp milik Zcut, bukan angka asli.
- Jumlah landmark dan model face yang dipakai tidak terbaca (kemungkinan
  model internal `tt_fsnew_*` dari EffectSDK). ML Kit memberi 6 landmark
  (mata, hidung, sudut/bawah mulut), bukan 106 titik.
- Apakah beauty diproses di GPU EffectSDK atau CPU retouch SDK; keduanya ada.
