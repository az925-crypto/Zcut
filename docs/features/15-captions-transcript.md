# 15 — Captions, Transcript & Auto Subtitle

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> ASR dasar memakai SpeechRecognizer lokal; transkripsi akurat, terjemahan, dan overdub ditandai **BUTUH BACKEND** dengan fallback manual/SRT.

## Bukti

- Package: `com.vega.transcriptedit` (214 dock, 135 overdub, 105 edit),
  `com.vega.libsticker.subtitle`, `com.vega.libsticker.caption`,
  `com.vega.libsticker.keywords`, `com.vega.libsticker.aiemoji`.
- Client: `TranscriptEditClient`, `TextClient`, `StickerClient`.
- Module: `GenerateTranscriptInfo`, `CreateTranscriptEditSubtitle`,
  `CreateSubtitleFragmentInfos`, `AddTranscriptEditWord`,
  `AddTranscriptEditWordsSyncSubtitle`, `AddTranscriptEditTag`,
  `CorrectTranscriptEditWords`, `ApplyTranscriptEditOverdub`,
  `ApplyOverdubResult`, `RestoreOverdubCorrectedWord`,
  `AvoidBilingualCaptionOverlap`, `GetLyricsRecognizeInfo`,
  `AddLyricEffect`, `GenerateSubtitleData`.
- ClipFlow node: `AIRecognitionASRNode`, `ASRNode`, `VideoASRNode`,
  `AIRecognitionTranslateNode`, `AIRecognitionProcessSubtitleNode`,
  `AIRecognitionUpdateTranslateNode`, `AIRecognitionHighlightNode`.
- Native: `libspeechsdk`, `libspeechengine`, `libwhisper`.
- Callback: `IImportCaptionCallback`, `ISetNeedReTranslateCallback`.

## Fitur

| Fitur | Mekanisme |
|---|---|
| Auto caption | ASR word-level → segment teks per baris |
| Edit kata | koreksi kata, timing, delete, split/merge |
| Style caption | preset, font, warna, highlight kata |
| Bilingual | translate → dua track subtitle |
| Lyric | deteksi lirik + efek lirik |
| Overdub | re-record per kalimat, TTS, sinkron |
| AI emoji | emoji otomatis dari kata/kalimat |
| Keyword | highlight kata kunci |
| Import/export | SRT/VTT/ASS (callback import) |

## Pipeline ASR

```
audio track → extract/resample 16k mono
  → ASR (on-device Whisper atau cloud)
  → word timestamps
  → sentence/line segmentation
  → translate (opsional)
  → buat segment_text + timing
  → user koreksi
```

Line breaking memakai batas karakter/lebar, bukan hanya jeda. `AsrWordsPerLineConfig`
di package menunjukkan konfigurasi jumlah kata per baris.

## Model

```json
{
  "transcript": {
    "language": "en",
    "words": [
      {"text": "hello", "start": 0, "end": 420000, "confidence": 0.98},
      {"text": "world", "start": 440000, "end": 900000, "confidence": 0.95}
    ],
    "segments": [
      {"start": 0, "end": 900000, "text": "hello world",
       "words": [0, 1], "speaker": 0}
    ]
  }
}
```

## Implementasi referensi (clean-room)

```kotlin
data class AsrWord(val text: String, val startUs: Long, val endUs: Long,
                   val confidence: Float)
data class CaptionLine(val startUs: Long, val endUs: Long, val text: String)

fun buildCaptionLines(
    words: List<AsrWord>,
    maxChars: Int = 32,
    maxDurationUs: Long = 5_000_000,
): List<CaptionLine> {
    val lines = mutableListOf<CaptionLine>()
    var buf = mutableListOf<AsrWord>()
    fun flush() {
        if (buf.isEmpty()) return
        lines += CaptionLine(buf.first().startUs, buf.last().endUs,
            buf.joinToString(" ") { it.text })
        buf = mutableListOf()
    }
    for (w in words) {
        val nextLen = buf.joinToString(" ").length + 1 + w.text.length
        val tooLong = nextLen > maxChars ||
            (buf.isNotEmpty() && w.endUs - buf.first().startUs > maxDurationUs)
        if (tooLong) flush()
        buf += w
    }
    flush()
    return lines
}
```

## Overdub

`ApplyTranscriptEditOverdub` + `ReplaceOverdubSentence` + TTS:
pengguna memilih kalimat, merekam ulang atau memilih voice, lalu audio
pengganti dipasang pada rentang kalimat. `RestoreOverdubCorrectedWord`
mengembalikan kata yang dikoreksi.

## Catatan paritas

- **P0**: auto caption (ASR), edit teks, style dasar, export SRT.
- **P1**: word-level timing, translate, lyric, highlight kata, import SRT.
- **P2**: overdub, AI emoji, bilingual, speaker diarization, caption template.

## Unknown

- Bahasa yang didukung per build tidak terbaca dari manifest; UI memakai
  `subtitle_language_list`.
- Apakah ASR default on-device atau cloud; `libwhisper` + `ASRNode` menunjukkan
  keduanya tersedia.
- Format file subtitle internal tidak terverifikasi.
