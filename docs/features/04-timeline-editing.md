# 04 — Timeline Editing

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- `com.vega.middlebridge.client` + module: `MoveSegment`, `SplitSegment`,
  `CopyVideoSegment`, `RemoveSegment`, `AddTrack`, `RemoveEmptyTrack`,
  `SetFixClashInSameTrack`, `AdjustVolume`, `ReverseVideo`,
  `CanvasUpdateTransform`.
- Asset contoh: `assets/splitscreen/split_screen_2.zip → template.json`
  (track + segment nyata).
- UI: `com.vega.edit.editpage.fragment`, `com.vega.edit.base.dock`.

## Operasi inti

| Operasi | Semantik |
|---|---|
| Trim kiri | `source.start += delta`, `target.start += delta`, durasi turun |
| Trim kanan | `source.duration` & `target.duration` turun |
| Split | pada `t`: segmen kiri `[0,t)`, kanan `[t,end)`; source ikut dibagi |
| Move | ubah `target.start`; boleh pindah track |
| Ripple delete | hapus segmen + geser segmen setelahnya ke kiri |
| Duplicate | clone segmen/material ref + offset |
| Snap | magnet ke boundary, playhead, marker, track lain |
| Collision | `SetFixClashInSameTrack` → dorong/trim otomatis |

## Matematika

```
target.duration = source.duration / speed
target.end      = target.start + target.duration

split(t):
  left.target  = [target.start, t)
  left.source  = [source.start, source.start + (t - target.start) * speed)
  right.target = [t, target.end)
  right.source = [left.source.end, source.end)

trimLeft(d):
  target.start += d ; target.duration -= d
  source.start += d * speed ; source.duration -= d * speed
```

`source.start` harus dijepit ke `[0, material.duration]`. Untuk speed curve,
durasi tidak linear; gunakan integrasi tabel kecepatan.

## Alur edit

```
gesture (drag/trim/split)
  → TimelineCommand
  → validasi (batas material, overlap, track lock)
  → DraftBeginTransaction
  → native op (Move/Split/…)
  → recompute duration + render index
  → DraftCommit → UI diff
```

Command pattern penting: setiap gesture harus bisa dibatalkan dan di-undo
sebagai satu unit.

## Implementasi referensi (clean-room)

```kotlin
sealed interface TimelineCommand { fun apply(draft: MutableDraft) }

data class SplitCommand(val segmentId: String, val atUs: Long) : TimelineCommand {
    override fun apply(draft: MutableDraft) {
        val track = draft.trackOf(segmentId) ?: return
        val seg = track.segments.first { it.id == segmentId }
        val local = atUs - seg.targetTimeRange.start
        if (local <= 0 || local >= seg.targetTimeRange.duration) return

        val rightSourceStart = seg.sourceTimeRange.start +
            (local * seg.speed).toLong()

        val right = seg.copy(
            id = UUID.randomUUID().toString(),
            targetTimeRange = TimeRange(atUs,
                seg.targetTimeRange.end - atUs),
            sourceTimeRange = TimeRange(rightSourceStart,
                seg.sourceTimeRange.end - rightSourceStart),
        )
        seg.targetTimeRange = TimeRange(seg.targetTimeRange.start, local)
        seg.sourceTimeRange = TimeRange(seg.sourceTimeRange.start,
            rightSourceStart - seg.sourceTimeRange.start)

        track.segments.insertAfter(seg.id, right)
    }
}

fun snap(valueUs: Long, candidates: List<Long>, thresholdUs: Long): Long =
    candidates.minByOrNull { abs(it - valueUs) }
        ?.takeIf { abs(it - valueUs) <= thresholdUs } ?: valueUs
```

## Catatan paritas

- **P0**: trim, split, delete, move, snap, ripple.
- **P1**: multi-track, collision resolve, duplicate, group move.
- **P2**: magnetic main track kompleks, auto-arrange, linked audio/video.

## Unknown

- Aturan persis `SetFixClashInSameTrack` (dorong vs trim vs tolak).
- Threshold snap dalam piksel/µs tidak terlihat.
