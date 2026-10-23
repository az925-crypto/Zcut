# 06 — Keyframe

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- `com.vega.middlebridge.client.InterpolationClient`.
- Module: `AddCommonKeyframe`, `UpdateCommonKeyframe`, `RemoveCommonKeyframe`,
  `AddSingleLockedKeyFrame`, `AddSingleSmartMotionKeyframes`,
  `AddTempSmartMotionKeyframes`, `KeyframeTracking`.
- Struct native `TimeKeyframe` dengan field: `play_head`, `position`,
  `rotation`, `scale`.
- Varian: `KeyframeVideo`, `KeyframeText`, `KeyframeAudio`, `KeyframeEffect`,
  `KeyframeFilter`, `KeyframeSticker`, `KeyframeAdjust`, `KeyframeHandwrite`.
- UI: `com.vega.edit.keyframe`.

## Model

Keyframe disimpan per properti, bukan satu objek gabungan. Contoh:

```json
{
  "keyframes": {
    "seg-1": {
      "position": [
        {"time": 0,      "value": [0.5, 0.5]},
        {"time": 1000000,"value": [0.3, 0.7]}
      ],
      "scale": [
        {"time": 0, "value": 1.0},
        {"time": 1000000, "value": 1.4}
      ],
      "rotation": [
        {"time": 0, "value": 0},
        {"time": 1000000, "value": 15}
      ],
      "opacity": [
        {"time": 0, "value": 1.0},
        {"time": 1000000, "value": 0.0}
      ]
    }
  }
}
```

`play_head` adalah waktu relatif terhadap segmen (µs), bukan waktu absolut
timeline. Saat segmen digeser, keyframe ikut karena relatif.

## Interpolasi

- Default linear: `v = a + (b-a) * t`.
- Easing: cubic bezier / smoothstep.
- Untuk posisi/scale/rotation, interpolasi numerik per komponen.
- Untuk properti diskrit (string, path), pakai step/hold.

```kotlin
fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

fun sample(points: List<Pair<Long, Float>>, timeUs: Long): Float {
    if (points.isEmpty()) return 0f
    if (timeUs <= points.first().first) return points.first().second
    if (timeUs >= points.last().first) return points.last().second
    val i = points.indexOfLast { it.first <= timeUs }
    val (t0, v0) = points[i]
    val (t1, v1) = points[i + 1]
    val t = (timeUs - t0).toFloat() / (t1 - t0).toFloat()
    return lerp(v0, v1, t)
}
```

## Alur edit

```
pilih segmen → posisikan playhead → ubah properti
  → auto-create keyframe di playhead (jika belum ada)
  → update nilai
  → render preview
  → tombol "keyframe" untuk navigasi/ hapus
```

CapCut menandai keyframe aktif dengan diamond di timeline; `KeyframeTracking`
menunjukkan ada keyframe yang mengikuti objek (tracking), bukan hanya properti
statis.

## Implementasi referensi (clean-room)

```kotlin
data class Keyframe<T>(val timeUs: Long, val value: T, val easing: Easing = Easing.Linear)
data class AnimatedProperty<T>(val frames: MutableList<Keyframe<T>> = mutableListOf())

fun AnimatedProperty<Float>.valueAt(timeUs: Long): Float {
    if (frames.isEmpty()) return 0f
    val sorted = frames.sortedBy { it.timeUs }
    if (timeUs <= sorted.first().timeUs) return sorted.first().value
    if (timeUs >= sorted.last().timeUs) return sorted.last().value
    val i = sorted.indexOfLast { it.timeUs <= timeUs }
    val a = sorted[i]; val b = sorted[i + 1]
    val t = (timeUs - a.timeUs).toFloat() / (b.timeUs - a.timeUs)
    return lerp(a.value, b.value, a.easing.transform(t))
}
```

## Catatan paritas

- **P0**: keyframe posisi/scale/opacity, interpolasi linear, UI diamond.
- **P1**: rotation, easing, copy/paste keyframe, curve editor.
- **P2**: tracking keyframe, smart motion, keyframe untuk effect/filter/text.

## Unknown

- Format easing persis (bezier control point) tidak terbaca.
- `is_auto_fill_keyframe` dan `keyframe_flags` belum jelas semantiknya.
