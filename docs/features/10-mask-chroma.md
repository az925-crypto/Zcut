# 10 — Mask, Chroma Key & Green Screen

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Client: `MaskClient`, `GreenScreenClient`, `ShapeClient`, `HandwriteClient`.
- Module: `MaskParam`, `MaskParentInfo`, `MaskTypeDefine`, `SetMaskMaterialPath`,
  `RemoveVideoMask`, `MaskVideo`, `GenerateBrushMaskFromVe`,
  `GetGreenScreenForegroundBoundingBoxPosition`, `GetPureGreenScreenDraft`.
- Asset: `assets/zip/Chroma_V320.zip`, `Chroma_V1370.zip`,
  `Chroma_V1370.zip`, `Brush2D_Eraser.zip`, `Brush2D_Mosiac_Grid.zip`,
  `Brush2D_StickerEraser.zip`, `Brush2D_StickerPainter.zip`,
  `assets/zip/area_locked.zip`, `draw_locked_box.zip`.
- UI: `com.vega.edit.mask` (104 class).

## Mask

`MaskParam` yang terbaca memuat: `feather`, `invert`, `center_x`, `center_y`,
`width`, `height`, `rotation`, `round_corner`, `aspect_ratio`, `maskId`,
`mask_effect_type`, `path`, `resource_id`, `is_keyframe`.

Tipe mask yang lazim (dari UI/asset): linear, mirror, circle, rectangle,
heart, star, brush, text mask. Brush mask memakai `GenerateBrushMaskFromVe`
dan `HandwriteGetBrushMask`.

```
mask = shape (geometri) + feather + invert + transform
render: fragment discarding di luar mask, dengan soft edge
```

```glsl
float maskAlpha(vec2 uv, vec2 center, vec2 size, float feather, bool invert) {
    vec2 d = abs(uv - center) - size * 0.5;
    float sdf = length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
    float a = 1.0 - smoothstep(-feather, feather, sdf);
    return invert ? 1.0 - a : a;
}
```

## Chroma key / green screen

- Effect package `Chroma_V320` (nama internal `sexiangkoutu`) berisi
  AmazingFeature keying.
- `GetPureGreenScreenDraft` membuat draft background hijau untuk compositing.
- `GetGreenScreenForegroundBoundingBoxPosition` memberi posisi foreground
  setelah keying (untuk transform lanjutan).

Parameter keying yang umum: key color (HSV), similarity/threshold, softness,
spill suppression, edge feather.

```kotlin
data class ChromaKey(
    val keyColor: Int = 0xFF00FF00.toInt(),
    val similarity: Float = 0.30f,
    val smoothness: Float = 0.10f,
    val spill: Float = 0.20f,
    val enabled: Boolean = true,
)
```

## Alur

```
pilih segmen → panel mask/chroma
  → pilih bentuk / brush / key color
  → set feather/invert/transform
  → preview
  → simpan MaskParam pada material segmen
```

Brush mask disimpan sebagai bitmap/path, bukan angka; karena itu ada
`SetMaskMaterialPath` dan `GenerateBrushMaskFromVe`.

## Implementasi referensi (clean-room)

```kotlin
class MaskRenderer {
    fun applyMask(shader: ShaderProgram, mask: MaskParam) {
        shader.setFloat("uFeather", mask.feather)
        shader.setInt("uInvert", if (mask.invert) 1 else 0)
        shader.setVec2("uCenter", mask.centerX, mask.centerY)
        shader.setVec2("uSize", mask.width, mask.height)
        shader.setFloat("uRotation", mask.rotation)
    }
}

fun chromaAlpha(px: FloatArray, key: FloatArray, sim: Float, smooth: Float): Float {
    val d = distanceRgb(px, key)
    return smoothstep(sim, sim + smooth, d)
}
```

## Catatan paritas

- **P0**: mask rectangle/linear + feather + invert.
- **P1**: circle/heart/star, brush mask, chroma key dengan spill.
- **P2**: AI matting mask, object tracking mask, animated mask keyframe.

## Unknown

- `mask_effect_type` nilai enum tidak terbaca.
- Apakah chroma memakai HSV atau YUV; hanya nama effect package yang terlihat.
