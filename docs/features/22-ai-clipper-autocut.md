# 22 — AI Clipper, AutoCut & Smart Vlog

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> AutoCut heuristic lokal; ASR/highlight kualitas penuh ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.edit.aiclipper` (1.764 class): `bean`, `clipflow`,
  `ui`, `viewmodel`, `service`.
- Class kunci: `AiClipperTask` (+ `MainTrackClipInfo`, `SegmentInfo`, `Param`),
  `AiClipperClipFlow`, `AiEditMaterialProcessor`,
  `AiEditMaterialProcessConfig`, `AiEditMaterialProcessConfigSettings`,
  `AiClipperSetting`.
- Node ClipFlow: `ASRNode`, `AEDNode` (audio event detection),
  `AIRecognitionASRNode`, `AIRecognitionHighlightNode`,
  `AIRecognitionFilterMediaNode`, `AIRecognitionMergeDataNode`,
  `AIRecognitionUpdateTrackNode`, `VideoContentUnderstandNode`,
  `AITemplateRemoteTaskParamNode`, `AITemplateRemoteTaskParseNode`,
  `AIApplyDraftNode`.
- Package terkait: `com.vega.edit.quickedit` (322), `aiedit` (127),
  `aishorts` (92), `script` (121), `broll` (88), `functionassistant` (311).
- Asset: `ai_clipper_loading.json`, `auto_cut_gen_loading.json`,
  `auto_video_cut_sug_word_loading_circle.json`,
  `autocut_tab_ai_vlog_flash/data.json`.
- `VideoHighlightcutClient`, `VideoPuzzlesClient`, `AiMomentClient`.

## Fitur

| Fitur | Input | Output |
|---|---|---|
| AI Clipper | video panjang | beberapa short (highlight) |
| AutoCut | video + musik | potongan sinkron beat |
| Smart Vlog | banyak klip | vlog terurut + transisi + musik |
| AI Story | foto/klip + cerita | video naratif |
| Script to Video | teks skrip | video + voiceover + b-roll |
| Video Highlight | video olahraga/acara | momen terbaik |
| AI Moment | momen | klip pendek |

## Pipeline AI Clipper

```
video panjang
  → ASR (kata + timestamp + confidence)
  → AED (applause, laugh, musik, silence, suara keras)
  → scene/shot detection + kualitas visual (blur, shake, face)
  → skor per kandidat window
  → pilih window dengan constraint (durasi, non-overlap, diversity)
  → arrange + caption + musik
  → apply ke draft (AIApplyDraftNode)
```

Node `AIRecognitionFilterMediaNode` menyaring media buruk;
`AIRecognitionMergeDataNode` menggabungkan hasil ASR/AED/visual;
`AIRecognitionUpdateTrackNode` menulis hasil ke track.

## Skoring highlight (contoh yang masuk akal)

```text
score = 0.35 * speechEnergy
      + 0.25 * keywordScore
      + 0.20 * audioEventScore (laugh/applause/cheer)
      + 0.10 * faceScore
      + 0.10 * motionScore
      - penalty(blur, shake, silence)
```

Bobot pastinya tidak terbaca; yang terbukti adalah adanya ASR, AED, dan
highlight node. Formula di atas adalah implementasi referensi.

## Pemilihan klip

Constraint: total durasi target, min/max per klip, tidak overlap, jarak
minimum, dan variasi. Ini basically **weighted interval scheduling** dengan
budget durasi.

```kotlin
data class Candidate(
    val startUs: Long, val endUs: Long, val score: Float,
)

fun selectHighlights(
    candidates: List<Candidate>,
    targetDurationUs: Long,
    minClipUs: Long = 2_000_000,
    maxClips: Int = 10,
): List<Candidate> {
    val sorted = candidates
        .filter { it.endUs - it.startUs >= minClipUs }
        .sortedByDescending { it.score }

    val chosen = mutableListOf<Candidate>()
    var total = 0L
    for (c in sorted) {
        if (chosen.size >= maxClips) break
        if (chosen.any { it.overlaps(c) }) continue
        if (total + (c.endUs - c.startUs) > targetDurationUs * 1.1) continue
        chosen += c
        total += c.endUs - c.startUs
    }
    return chosen.sortedBy { it.startUs }
}
```

## Implementasi referensi (clean-room)

```kotlin
interface HighlightPipeline {
    suspend fun analyze(video: MediaAsset): HighlightAnalysis
}

data class HighlightAnalysis(
    val words: List<AsrWord>,
    val audioEvents: List<AudioEvent>,
    val scenes: List<Scene>,
    val candidates: List<Candidate>,
)

class AiClipper(
    private val pipeline: HighlightPipeline,
    private val draftFactory: DraftFactory,
) {
    suspend fun run(
        video: MediaAsset,
        targetUs: Long,
        aspect: AspectRatio,
    ): Draft {
        val a = pipeline.analyze(video)
        val picks = selectHighlights(a.candidates, targetUs)
        val draft = draftFactory.newDraft(aspect)
        picks.forEachIndexed { i, c ->
            draft.addVideoClip(video, c.startUs, c.endUs, index = i)
        }
        draft.addCaptions(a.words.within(picks))
        draft.recomputeDuration()
        return draft
    }
}
```

## Catatan paritas

- **P0**: auto-cut berdasarkan silence/scene + urutkan.
- **P1**: ASR + highlight scoring, target durasi, caption otomatis.
- **P2**: AED, video understanding, multi-short, AI vlog, script-to-video,
  b-roll.

## Unknown

- Apakah analisis di server atau on-device. `AITemplateRemoteTaskParamNode`
  dan endpoint AI menunjukkan server untuk sebagian tugas.
- Bobot skor dan model highlight tidak terbaca.
- Batas durasi video input tidak terbaca.
