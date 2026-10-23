# 25 — Digital Human & Lip Sync

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Render digital human dan lip sync ditandai **BUTUH BACKEND**; template preview tetap lokal.

## Bukti

- Package: `com.vega.edit.digitalhuman` (1.114 class): `digital` (566),
  `render` (154), `script` (39), `tone` (37), `background` (42), `mask` (23),
  `model` (16), `service` (23).
- `com.vega.audio.lipsync` (366): `viewlifecycle` (176), `task` (53),
  `model` (14), `repository`, `config`, `cache`.
- Module: `AddDigitalHuman`, `AddDigitalHumanParam`,
  `AddDigitalHumanTemplate`, `AddDigitalHumanTemplateSubtitles`,
  `AddDigitalHumanModelDressing`, `RunAudioToBsInfoAlgorithm`.
- Activity: `DigitalHumanTemplateLoadingActivity`,
  `DigitalHumanTemplateConstants`, `IAdMakerDigitalCustomizeUtils`.
- String: `Please select a valid digital human`, `confirmOnReplace step1.
  batch add digital human`, `onClipflowTaskSucceed. but digital human not
  found`, `Lip sync task failed, error code`.
- Client: `DigitalHumanClient`, `AILipSyncFunction` / `IAILipSyncFunction`
  (audio/lip sync), ClipFlow task.

## Dua mode

1. **Template digital human** — avatar + background + script siap pakai.
   `AddDigitalHumanTemplate` + subtitle.
2. **Custom digital human** — foto/avatar pengguna, model dressing (baju,
   aksesori), lalu dianimasikan.

## Pipeline

```
pilih avatar/template
  → input script (teks) atau audio
  → TTS (jika teks) → audio
  → lip sync task (audio + avatar)
  → render frame avatar
  → background + subtitle + mask
  → composite ke draft (segmen video)
```

Lip sync bisa berarti:
- **viseme/lip-sync rendering** — mulut avatar digerakkan sesuai fonem;
- **face reenactment** — wajah pada video asli digerakkan mengikuti audio
  (lihat juga face cover).

String `Lip sync task failed, error code` menunjukkan task asinkron dengan
kode error, bukan proses instan.

## Model

```json
{
  "digital_human": {
    "template_id": "dh-1",
    "avatar_id": "avatar-1",
    "dressing": {"outfit": "suit", "background": "office"},
    "script": "Hello, welcome!",
    "tone": {"voice_id": "v1", "speed": 1.0},
    "lip_sync": {"task_id": "ls-1", "status": "success"},
    "subtitle": {"enabled": true, "style": "default"}
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class DigitalHumanRequest(
    val avatarId: String,
    val audio: File,
    val background: File? = null,
    val subtitle: Boolean = true,
)

interface DigitalHumanEngine {
    suspend fun render(req: DigitalHumanRequest): DigitalHumanResult
}

data class DigitalHumanResult(
    val video: File,          // avatar dengan alpha atau green screen
    val durationUs: Long,
    val mask: File? = null,
)

class DigitalHumanController(
    private val engine: DigitalHumanEngine,
    private val draft: DraftSession,
) {
    suspend fun add(req: DigitalHumanRequest): Segment {
        val result = engine.render(req)
        val material = draft.addVideoMaterial(result.video, result.durationUs)
        return draft.addSegment(
            type = "segment_video",
            materialId = material.id,
            target = TimeRange(0, result.durationUs),
        )
    }
}
```

Jika hasil berupa green screen, apply chroma key (dok 10) sebelum composite.

## Catatan paritas

- **P0**: avatar template + TTS + lip sync + background.
- **P1**: subtitle, voice, dressing, preview, durasi fleksibel.
- **P2**: custom avatar dari foto, real-time lip sync, multi-avatar,
  face reenactment.

## Unknown

- Apakah lip sync dijalankan di server atau on-device. `task` + error code
  menunjukkan server; tidak ada model lip sync besar di daftar `.so`.
- Jumlah template/avatar dan lisensi tidak terbaca.
- Format output (MP4 alpha vs green screen) tidak terverifikasi.
