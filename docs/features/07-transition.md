# 07 — Transition

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- `com.vega.middlebridge.client` module: `AddVideoTransition`,
  `AddAITransitionHistories`, `AddAITransitionHistories` +
  `AIGCEffectStartVETaskMixNode*`.
- Package: `com.vega.edit.transition` (413 class), `com.vega.edit.aigceffect`.
- Class: `AITransitionAlgorithmInfoBuilder`,
  `AITransitionClipflowFramesExtractingNode`,
  `AITransitionApplyNode`, `AITransitionEffectInfo`.
- Transisi standar adalah effect package terpisah (download dari Artist),
  transisi AI adalah hasil generate (frame interpolation / generative).

## Model

Transisi bukan segmen mandiri; ia menempel pada **batas dua segmen**:

```json
{
  "segment_id": "seg-2",
  "transition": {
    "id": "tr-1",
    "name": "Dissolve",
    "type": "video_transition",
    "duration": 500000,
    "effect_id": "artist-effect-id",
    "resource_id": "resource-id",
    "path": "/workspace/effect/...",
    "params": {"intensity": 1.0}
  }
}
```

Durasi transisi biasanya dipotong dari overlap kedua segmen; render graph
menggabungkan frame A dan B dengan shader transisi.

## Jenis

1. **Standar** — dissolve, slide, zoom, glitch, light leak, dsb. Effect package
   berisi shader/Lua (format AmazingFeature).
2. **AI transition** — model menghasilkan frame antara (frame interpolation
   atau generative), dipakai saat materi sulit (gerakan besar, objek berbeda).
3. **Audio transition** — crossfade audio pada batas.

## Alur standar

```
pilih batas segmen
  → buka panel transisi
  → pilih item (fetch Artist effect list)
  → download effect package (jika belum)
  → apply: set transition pada segmen kanan
  → preview: render overlap A/B
```

## Alur AI transition (terlihat dari nama node)

```
frames extracting (A/B)
  → preprocess
  → upload / remote task
  → network state check
  → apply result
```

Artinya frame dikirim ke server, hasil interpolasi diunduh, lalu di-apply
sebagai transisi. Ini bukan on-device murni.

## Implementasi referensi (clean-room)

```kotlin
data class Transition(
    val id: String,
    val name: String,
    val durationUs: Long,
    val effectPath: String,
    val params: Map<String, Float> = emptyMap(),
)

fun applyTransition(draft: MutableDraft, boundaryUs: Long, t: Transition) {
    val (left, right) = draft.segmentsAt(boundaryUs) ?: return
    val maxOverlap = minOf(left.targetTimeRange.duration,
                           right.targetTimeRange.duration)
    val d = t.durationUs.coerceIn(0, maxOverlap)
    right.transition = t.copy(durationUs = d)
    right.overlapUs = d            // render graph: A dan B overlap d
}
```

Shader dissolve sederhana:

```glsl
vec4 mixTransition(vec4 a, vec4 b, float p) {
    return mix(a, b, smoothstep(0.0, 1.0, p));
}
```

## Catatan paritas

- **P0**: cut, dissolve, fade; durasi + overlap benar.
- **P1**: 10–20 transisi populer, preview real-time, audio crossfade.
- **P2**: AI transition, transisi 3D/light leak, marketplace.

## Unknown

- Format parameter effect transisi per item (nama field) tidak terbaca.
- Apakah AI transition memakai server CapCut atau model on-device; node upload
  menunjukkan server.
