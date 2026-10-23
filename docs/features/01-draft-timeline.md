# 01 — Draft & Timeline Core

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- Package: `com.vega.draft`, `com.vega.middlebridge.swig`, `com.vega.ve.db`
- Modul native: `DraftDefine`, `DraftBeginTransaction`, `DraftCommit`,
  `DraftAmend`, `DraftCanUndo`, `DraftCanRedo`, `DraftDeepCopy`,
  `AddTrack`, `RemoveEmptyTrack`, `MoveSegment`, `SplitSegment`,
  `SetFixClashInSameTrack`.
- File skema contoh di `classes2.dex` (mini-draft) dan
  `assets/splitscreen/split_screen_2.zip → template.json`.
- Entity Room: `StableEntity`, `MediaDataTransEntity`, `DraftDatabase`
  (`Migration1To2`).

## Model data

Draft adalah **satu dokumen JSON** (bukan tabel per segmen). Versi skema yang
terlihat: `142.0.0`. Struktur inti:

```json
{
  "id": "UUID",
  "version": "142.0.0",
  "new_version": "142.0.0",
  "name": "draft",
  "create_time": 0, "update_time": 0,
  "duration": 0, "fps": 30,
  "canvas_scale": {"x": 100, "y": 100},
  "canvas_config": {
    "width": 1080, "height": 1920, "ratio": "9:16",
    "background": {"type": 0, "color": "#000000", "blur": "0.0"}
  },
  "config": {
    "video_mute": false, "maintrack_adsorb": true,
    "export_range": {"start": 0, "duration": 0},
    "zoom_info_params": {"zoom_ratio": 1, "offset_x": 0, "offset_y": 0}
  },
  "tracks": [
    {"id": "track-1", "type": "video", "attribute": 0, "flag": 0,
     "segments": [{"id": "seg-1"}]}
  ],
  "materials": {
    "videos": [{"id": "mat-1", "path": "...", "duration": 1000000,
                "width": 1920, "height": 1080, "crop": {...},
                "has_audio": true, "type": "video"}],
    "audios": [], "texts": [], "stickers": []
  },
  "keyframes": {},
  "extra_info": {}
}
```

Segmen penuh (contoh nyata dari `template.json`) memuat:

`cartoon, clip, enable_adjust, enable_color_curves, enable_color_wheels,
enable_lut, group_id, hdr_settings, id, intensifies_audio, is_tone_modify,
last_nonzero_volume, material_id, render_index, reverse, source_timerange,
speed, target_timerange, track_attribute, track_render_index, visible, volume`.

Tipe segmen yang didukung:
`video, audio, text, sticker, image_sticker, text_template, video_effect,
composition, tail_leader, handwrite, filter, shape, picture_adjust, adcube`.

## Operasi & semantik

| Operasi | Modul SWIG | Catatan |
|---|---|---|
| Tambah track | `AddTrack` | tipe + attribute |
| Hapus track kosong | `RemoveEmptyTrack` | housekeeping |
| Pindah segmen | `MoveSegment` | lintas track + waktu |
| Split | `SplitSegment` | dua segmen, source range dibagi |
| Duplikat | `Copy*Segment` | offset + new id |
| Transaction | `DraftBeginTransaction` / `DraftCommit` | batch atomik |
| Amend | `DraftAmend` | patch granular |
| Undo/Redo | `DraftCanUndo` / `DraftCanRedo` | history native |
| Deep copy | `DraftDeepCopy` | duplicate project |

**Aturan penting yang terlihat dari nama API:**

- `target_time_range` = posisi di timeline; `source_time_range` = potongan
  media asli. `speed` menghubungkan keduanya:
  `target.duration ≈ source.duration / speed`.
- `render_index` menentukan urutan layer; `track_attribute` menandai main
  track vs overlay.
- `maintrack_adsorb` = magnet/snap pada main track.
- `SetFixClashInSameTrack` menunjukkan ada mode auto-resolve tabrakan segmen.

## Alur simpan

```
edit → BeginTransaction → N operasi → Commit
      → draft JSON ditulis → Room update metadata → history push
```

`IDraftUpdateCallback` + `IDraftNodeUpdateCallback` memberi notifikasi granular
(node mana yang berubah), dipakai UI untuk invalidasi partial.

## Implementasi referensi (clean-room)

```kotlin
@Serializable
data class TimeRange(val start: Long, val duration: Long) {
    val end get() = start + duration
}

@Serializable
data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val type: String,                 // segment_video, segment_text, ...
    val materialId: String = "",
    val targetTimeRange: TimeRange,
    val sourceTimeRange: TimeRange = TimeRange(0, targetTimeRange.duration),
    val speed: Double = 1.0,
    val volume: Double = 1.0,
    val visible: Boolean = true,
    val renderIndex: Int = 0,
    val trackAttribute: Int = 0,
)

@Serializable
data class Track(
    val id: String = UUID.randomUUID().toString(),
    val type: String,                 // video, audio, text, sticker, effect
    val attribute: Int = 0,
    val flag: Int = 0,
    val segments: MutableList<Segment> = mutableListOf(),
)

class DraftRepository(private val store: DraftStore) {
    suspend fun <T> transaction(block: (MutableDraft) -> T): T {
        val draft = store.load()
        val snapshot = draft.deepCopy()
        return try {
            val result = block(draft)
            draft.recomputeDuration()
            store.saveAtomically(draft)     // write temp + rename
            store.pushHistory(snapshot)
            result
        } catch (t: Throwable) {
            store.restore(snapshot)
            throw t
        }
    }
}
```

## Catatan paritas

- **P0**: model track/segmen, target vs source range, split/move/trim,
  transaction + undo, autosave.
- **P1**: multi-track overlay, render index, group/composition, deep copy.
- **P2**: mini-draft incremental sync, draft repair, cross-platform version
  migration.

## Unknown

- Semantik persis `flag`, `attribute`, dan `state` per tipe segmen tidak
  terbaca dari nama saja.
- Threshold snapping dan aturan auto-resolve tabrakan tidak terverifikasi.
