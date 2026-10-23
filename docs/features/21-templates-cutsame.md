# 21 — Template, CutSame & Smart Vlog

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Template bawaan lokal; marketplace dan AI template ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.cutsameedit` (5.930 class), `com.vega.libcutsame`
  (4.392), `com.vega.edward` (1.370).
- Sub-package `cutsameedit/biz`: `edit/text` (493), `edit/music` (332),
  `edit/video` (270), `template/compose` (239), `edit/editpanel` (175),
  `aitemplate/clipflow` (162), `aitemplate/service` (138), `aitemplate/task`
  (91), `edit/aimoment` (76), `edit/adscript` (71), `edit/sticker` (66),
  `edit/templateselect` (62).
- `edward/templatecore`: `editor` (725), `material` (208), `utils` (62),
  `compat` (46), `videodraft`, `theme`, `agent/*`.
- Client/module: `TemplateClient`, `TemplateIntelligentClient`,
  `VideoHighlightcutClient`, `SubDraftClient`, `StdraftClient`,
  `AddAiMusicMvTemplateSegmentParams`, `TemplateSetGameplayPath`,
  `SplitScreenPreLoadTemplate`.
- Activity: `LoadingActivity` (template compose), `SmartVlogLoadingActivity`,
  `TextStartLoadingActivity`, `BusinessVideoLoadingActivity`,
  `DigitalHumanTemplateLoadingActivity`, `MinimalEditorActivity`.
- Asset: `assets/splitscreen/split_screen_{2..9}.zip` (template.json +
  config.json), `assets/zip/aigc_text_template.zip`,
  `assets/ai_writer_template/ai_writer_template.json`.

## Anatomi template

Template CapCut pada dasarnya **draft setengah jadi**. Contoh
`split_screen_2/template.json`:

```json
{
  "canvas_config": {"ratio": "1:1", "width": 1440, "height": 1440},
  "fps": 30, "duration": 2240000,
  "config": {
    "maintrack_adsorb": true, "video_mute": false,
    "subtitle_sync": true, "lyrics_sync": true,
    "adjust_max_index": 1, "sticker_max_index": 1,
    "extract_audio_last_index": 1
  },
  "materials": {
    "videos": [
      {"id": "...", "path": "video/1.mov", "duration": 2267000,
       "width": 1440, "height": 720, "crop": {...}, "has_audio": true,
       "type": "video"}
    ]
  },
  "tracks": [
    {"id": "...", "type": "video", "attribute": 0, "flag": 0,
     "segments": [{"id": "...", "material_id": "...",
       "source_timerange": {"start": 0, "duration": 2240000},
       "target_timerange": {"start": 0, "duration": 2240000},
       "speed": 1.0, "volume": 1.0, "visible": true,
       "render_index": 0, "track_render_index": 0}]}
  ],
  "extra_info": {"track_info": {"template_id": "", "transfer_paths": ["edit"]}}
}
```

Package template dapat pula berisi `config.json` yang menautkan effect
(`{"effect":{"Link":[{"type":"SplitScreenTemplate"}]}}`). Template AI memakai
ClipFlow untuk mengisi slot dan menjalankan algoritma.

## Dua kelas template

1. **CutSame / classic** — user memilih N media, engine memetakan ke slot,
   lalu compose draft. Cocok untuk split screen, MV, text start,
   marketing script.
2. **AI template** — slot diisi hasil algoritma (matting, face recognize,
   gameplay, reorder). Node: `AITemplateDAGParseNode`,
   `AITemplateMultiMediaDownloadNode`, `AITemplateAlgorithmReorderNode`,
   `AITemplateDreaminaMattingNode`, `AITemplateDreaminaFaceRecognizeNode`,
   `AITemplateDrivenResultApplyNode`, `AITemplateRemoteTaskParamNode`,
   `AITemplateRemoteTaskParseNode`.

## Alur apply template

```
pilih template
  → download package (template.json + asset)
  → parse slot: tipe media, jumlah, durasi min/max, rasio
  → user pilih media
  → validasi & trim otomatis ke durasi slot
  → clone draft dari template
  → ganti material slot + sesuaikan segment
  → jalankan AI node (jika template AI)
  → buka editor / langsung export
```

## Implementasi referensi (clean-room)

```kotlin
data class TemplateSlot(
    val id: String,
    val mediaType: MediaType,
    val minDurationUs: Long,
    val maxDurationUs: Long,
    val aspect: Float?,
)

data class TemplatePackage(
    val id: String,
    val draft: Draft,
    val slots: List<TemplateSlot>,
    val aiPipeline: String? = null,
)

class TemplateInstantiator(private val media: MediaRepository) {
    suspend fun instantiate(
        template: TemplatePackage,
        selected: List<String>,          // media id, urut sesuai slot
    ): Draft {
        require(selected.size == template.slots.size)
        val draft = template.draft.deepCopy()

        template.slots.forEachIndexed { i, slot ->
            val asset = media.get(selected[i])
            require(asset.type == slot.mediaType)
            val segs = draft.segmentsForSlot(slot.id)
            val trimmed = asset.copy(
                durationUs = asset.durationUs.coerceIn(
                    slot.minDurationUs, slot.maxDurationUs)
            )
            segs.forEach { it.replaceMaterial(trimmed, slot.id) }
        }
        draft.recomputeDuration()
        return draft
    }
}
```

Trim otomatis ke slot:

```kotlin
fun fitToSlot(assetDuration: Long, slot: TemplateSlot): TimeRange {
    val d = assetDuration.coerceIn(slot.minDurationUs, slot.maxDurationUs)
    val start = ((assetDuration - d) / 2).coerceAtLeast(0)
    return TimeRange(start, d)
}
```

## Catatan paritas

- **P0**: template sederhana (split screen / text start) + slot mapping.
- **P1**: package download, auto-trim, variabel teks/musik, preview.
- **P2**: AI template, smart vlog, marketplace/publish, template authoring.

## Unknown

- Format slot persis (apakah di `extra_info`, `config`, atau file terpisah)
  tidak terbaca; contoh split screen tidak memuat definisi slot eksplisit.
- Protokol marketplace dan lisensi template tidak terbaca.
- Nama `edward` dan `dreamina` adalah nama internal; fungsinya disimpulkan dari
  nama node.
