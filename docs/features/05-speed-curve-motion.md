# 05 — Speed, Curve, Slow Motion & Motion

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.vega.edit.speed`, `com.vega.edit.keyframe`,
  `com.vega.middlebridge.client.VelocityClient`, `InterpolationClient`,
  `MotionBlurClient`, `VideoStableClient`, `DeflickerClient`, `ReverseClient`.
- Module: `ChangeSpeed`, `CurveSpeedUtils`, `Velocity`, `StartConvertSlowMotion`,
  `PreviewSlowMotionOpticalFlow`, `PreviewSlowMotionFrameBlending`,
  `StartConvertMotionBlur`, `PreviewMotionBlur`, `ConvertReverseVideo`,
  `StartVideoStabProcess`, `StartConvertDeflicker`, `StartConvertNoiseReduction`.
- Native: `libttffmpeg`, VESDK filter, optical-flow/interpolation algorithm.

## Fitur

| Fitur | Implementasi |
|---|---|
| Speed konstan | ubah `speed`, durasi target = source / speed |
| Speed curve | grafik kecepatan per waktu; integrasi untuk durasi |
| Slow motion | retiming + interpolasi frame (optical flow / blend) |
| Reverse | decode terbalik + audio reverse |
| Stabilisasi | estimasi motion global → crop/warp |
| Deflicker | ratakan luminance antar frame |
| Motion blur | blur temporal sesuai kecepatan |
| Noise reduction | denoise spasial/temporal audio/video |

## Model speed curve

```json
{
  "speed": 1.0,
  "curve_speed": {
    "mode": "curve",
    "points": [{"x": 0.0, "y": 1.0}, {"x": 0.5, "y": 0.4},
               {"x": 1.0, "y": 2.0}]
  }
}
```

Durasi target dihitung dengan integrasi numerik:

```
targetDuration = ∫ sourceDuration / speed(t) dt
```

Poin `x` dinormalisasi 0..1 terhadap source. Saat render, decoder harus
mengambil frame pada PTS hasil retiming; ini alasan slow motion butuh
interpolasi, bukan sekadar mengulang frame.

## Alur

```
pilih segmen
  → set speed / edit curve
  → hitung ulang target range
  → (opsional) jalankan algorithm task:
       StartConvertSlowMotion / MotionBlur / Stabilize / Deflicker
  → simpan hasil sebagai material turunan (path baru)
  → update segment.video_algorithm.path
```

Hasil algorithm disimpan sebagai file turunan, bukan menghapus sumber. Ini
memungkinkan toggle on/off dan re-edit.

## Implementasi referensi (clean-room)

```kotlin
data class SpeedPoint(val x: Float, val y: Float) // x normalized, y = speed

fun targetDurationUs(sourceUs: Long, curve: List<SpeedPoint>): Long {
    if (curve.size < 2) return sourceUs
    var acc = 0.0
    for (i in 0 until curve.size - 1) {
        val a = curve[i]; val b = curve[i + 1]
        val dx = ((b.x - a.x) * sourceUs).toDouble()
        val avgSpeed = ((a.y + b.y) / 2.0).coerceAtLeast(0.01)
        acc += dx / avgSpeed
    }
    return acc.toLong()
}

fun speedAt(curve: List<SpeedPoint>, progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val i = curve.indexOfLast { it.x <= p }.coerceAtLeast(0)
    val a = curve[i]
    val b = curve.getOrNull(i + 1) ?: return a.y
    val t = if (b.x == a.x) 0f else (p - a.x) / (b.x - a.x)
    return a.y + (b.y - a.y) * t
}
```

## Catatan paritas

- **P0**: speed konstan, reverse, durasi akurat.
- **P1**: slow motion interpolation, curve editor, audio retiming/pitch.
- **P2**: stabilisasi, deflicker, motion blur, denoise, optical flow.

## Unknown

- Algoritma interpolasi mana yang dipakai per device (GPU/CPU/cloud).
- Apakah audio memakai pitch correction saat speed berubah; UI ada
  `is_tone_modify` tetapi semantiknya tidak terverifikasi.
