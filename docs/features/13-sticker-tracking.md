# 13 — Sticker & Object Tracking

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Tracking objek lanjutan ditandai **BUTUH BACKEND**; sticker statis/Lottie tetap lokal.

## Bukti

- Package: `com.vega.libsticker` (view, aieffect, subtitle, texttemplate,
  brand, aiscript).
- Client: `StickerClient`; module: `AddSticker`, `AddImageSticker`,
  `AddVideoObjectLocked`, `StartVideoTracking`, `StartVideoTrackingV3`,
  `CancelVideoTracking`, `AddVideoTrackingListener`,
  `GetStickerBoundingBoxPosition/Rect/Rectangle/Size`,
  `CopyVideoTrackingMap`, `SetStickerAnimPreviewMode`.
- Segment type: `segment_sticker`, `segment_image_sticker`.
- Asset: `assets/Brush2D_StickerEraser.zip`, `Brush2D_StickerPainter.zip`,
  Lottie sticker (`assets/*.json`), brand sticker.

## Jenis sticker

1. **Image sticker** — PNG/WebP/GIF/Lottie.
2. **Video sticker** — video pendek dengan alpha/effect.
3. **Text sticker** — teks sebagai sticker (lihat dok 14).
4. **Brand sticker** — logo/brand kit.
5. **AI sticker** — generated / AI emoji.

## Transform

Sticker punya transform sendiri: position (x,y), scale, rotation, opacity,
flip, anchor. Bisa diberi keyframe. Tracking menambahkan offset per frame.

```json
{
  "type": "segment_sticker",
  "material_id": "sticker-1",
  "clip": {
    "transform": {"x": 0.5, "y": 0.5, "scale": 0.3,
                  "rotation": 0, "opacity": 1.0},
    "flip": {"horizontal": false, "vertical": false}
  },
  "tracking": {"enabled": true, "path": "/tracking/seg-1.json"}
}
```

## Object tracking

`StartVideoTracking` menganalisis frame dan menghasilkan map posisi objek.
Sticker kemudian di-offset mengikuti map. `CopyVideoTrackingMap` memungkinkan
tracking dipakai ulang setelah segmen di-copy/trim.

Alur:

```
pilih sticker → aktifkan tracking
  → user tandai objek (rect/point)
  → StartVideoTracking (progress)
  → hasil map disimpan
  → render: sticker pos = base pos + map[frame]
```

## Implementasi referensi (clean-room)

```kotlin
data class StickerTransform(
    var x: Float = 0.5f, var y: Float = 0.5f,
    var scale: Float = 1f, var rotation: Float = 0f,
    var opacity: Float = 1f,
)

data class TrackingMap(val points: List<Pair<Long, Offset>>)

fun trackedTransform(
    base: StickerTransform,
    map: TrackingMap?,
    timeUs: Long,
): StickerTransform {
    val p = map?.points?.nearest(timeUs) ?: return base
    return base.copy(x = base.x + p.x, y = base.y + p.y)
}
```

Render sticker:

```glsl
vec4 sticker = texture2D(uSticker, vUv);
gl_FragColor = vec4(sticker.rgb, sticker.a * uOpacity);
```

## Catatan paritas

- **P0**: image sticker, transform, opacity, layer.
- **P1**: Lottie/GIF, video sticker, keyframe, brand kit.
- **P2**: object tracking, AI sticker/emoji, sticker marketplace.

## Unknown

- Format tracking map (JSON per frame?) tidak terbaca; hanya path API.
- Algoritma tracking (optical flow, correlation filter, atau ML) tidak
  terverifikasi.
