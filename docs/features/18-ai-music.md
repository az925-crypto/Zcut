# 18 — AI Music

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Generator prosedural lokal tersedia; model musik generatif ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.audio.aimusic` (1.077 class): `videotomusic` (250),
  `home` (161), `preview` (154), `model` (111), `panel` (100), `mv` (77),
  `lyric` (73), `task` (14), `v3` (10).
- Package terkait: `com.vega.audio.aiaudio` (586), `com.vega.audio.speechToSong`
  (34), `com.vega.audio.aisound` (94), `com.vega.audio.songclone` (37).
- ClipFlow: `AiAudioBuildRequestNode`, `AiAudioBranchStartNode`,
  `AiAudioCreatePlaceholderNode`, `AiAudioParseResultsNode`,
  `AiAudioApplyResultsNode`.
- Module: `GenerateAiMusicMv`, `ChangeAiMusicMvTemplate`,
  `AddAiMusicMvTemplateSegmentParams`, `AddAiMusicMvTemplateCombinationSegment`,
  `ReplaceAiMusicMvTemplateMaterial`, `AudioToBsInfoAlgorithmParam`,
  `RunAudioToBsInfoAlgorithm`.
- Asset: `ai_music_home_sug_state_anim.zip`, `audio_play.json`,
  `audio_downloading.json`.
- Endpoint AI video: `aivideo-normal.capcutapi.com` (bukan bukti khusus musik,
  tetapi satu-satunya domain AI yang terlihat).

## Sub-fitur

| Fitur | Input | Output |
|---|---|---|
| Video to music | video / mood / genre | lagu utuh + durasi |
| Text to music | prompt | lagu |
| AI MV | musik + template | draft video dengan potongan sinkron beat |
| Lyric video | lirik + lagu | teks animasi sinkron |
| Speech to song | rekaman suara | versi bernyanyi |
| Song clone | sample lagu | gaya/voice lagu |
| AI sound | prompt | SFX pendek |

`mv/` dan `lyric/` menunjukkan AI Music bukan hanya generator audio, tetapi
sekaligus pembuat draft video (MV template + sinkron lirik).

## Pipeline video-to-music

```
analisis video (durasi, scene, mood/energi)
  → bangun request (genre, mood, bpm, durasi)
  → task server (AiAudioBuildRequest → generate)
  → polling status
  → unduh audio + metadata (bpm, beat, lirik)
  → buat segment_audio
  → (opsional) MV: potong video per beat, apply template
```

`AudioToBsInfoAlgorithm` = audio-to-beat-sync info; dipakai untuk menempelkan
potongan video ke beat.

## Model

```json
{
  "ai_music": {
    "task_id": "task-1",
    "mode": "video_to_music",
    "prompt": "upbeat cinematic",
    "duration_us": 30000000,
    "bpm": 120,
    "beats": [0, 500000, 1000000],
    "audio_path": "/workspace/ai_music/task-1.mp3",
    "lyrics": [],
    "status": "success"
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class MusicRequest(
    val prompt: String,
    val durationUs: Long,
    val genre: String? = null,
    val mood: String? = null,
    val bpm: Int? = null,
)

data class MusicResult(
    val audio: File,
    val bpm: Int,
    val beatsUs: List<Long>,
    val lyrics: List<LyricLine> = emptyList(),
)

class AiMusicController(
    private val api: AiMusicApi,
    private val downloader: FileDownloader,
) {
    suspend fun generate(req: MusicRequest): MusicResult {
        val task = api.createTask(req)
        val done = pollUntilDone(task.id)          // exponential backoff
        val file = downloader.download(done.audioUrl, "ai_music/${task.id}.mp3")
        return MusicResult(file, done.bpm, done.beatsUs, done.lyrics)
    }
}
```

Sinkron ke beat (greedy):

```kotlin
fun snapCutsToBeats(cutsUs: List<Long>, beatsUs: List<Long>): List<Long> =
    cutsUs.map { cut -> beatsUs.minByOrNull { abs(it - cut) } ?: cut }
```

## Catatan paritas

- **P0**: text-to-music sederhana + insert ke timeline.
- **P1**: video-to-music, durasi tepat, beat metadata, preview/regenerate.
- **P2**: AI MV, lyric video, speech-to-song, song clone, marketplace.

## Unknown

- Model musik mana yang dipakai (nama model tidak muncul; hanya task API).
- Apakah audio dihasilkan utuh atau chunk; `task` + polling menunjukkan utuh.
- Batas durasi, lisensi komersial, dan watermark audio tidak terbaca.
