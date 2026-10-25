# 17 — Text-to-Speech & Voice Clone

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> TTS memakai engine sistem lokal; voice clone ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.audio.tone.tts` (2.135 class), subpackage engine:
  `elevenlabs` (20), `microsoft` (17), `nonstreaming` (73), `sami` (7),
  `server` (31), `ssml` (40), `streaming` (34).
- `TextToSpeechTaskManager` + `Operation.{Init, StartReading,
  StartReadingStreaming, StartReadingNonStreaming, StartSaving,
  StartSavingStreaming, StartSavingNonStreaming, StartSavingParallel, StopAll}`.
- `IStreamingTextToSpeechService`, `IStreamingToneManager`,
  `ToneDebugActivity`, `CloneToneActivity`.
- Enum/konfigurasi yang terlihat: `ttsMigrationConfig`, `ttsSavingStreamingConfig`,
  `ttsQwenSavingStreamingConfig`, `ttsStreamingSceneConfig`, `ttsV3ModelConfig`.
- `ElevenLabsApiService`, `ElevenLabsAudioResource`,
  `ElevenLabsAudioTaskRequestData`, `ElevenLabsToneManager`,
  `ElevenLabsErrorCode`; `MicrosoftApiService` + `AudioTaskRequestData`.
- `AudioChangeVoice*` (voice change, bukan clone), `voice clone` string,
  `ToneCloneArticleEditActivity`.

## Empat engine

| Engine | Peran | Bukti |
|---|---|---|
| ElevenLabs | TTS + kemungkinan voice clone premium | paket `engine/elevenlabs` |
| Microsoft Azure | TTS neural, SSML, bahasa luas | `MicrosoftApiService` |
| Qwen | TTS alternatif (streaming saving) | `ttsQwenSavingStreamingConfig` |
| Internal V3 / server | TTS ByteDance, streaming | `engine/server`, `engine/streaming`, `ttsV3ModelConfig` |

Kata “nonstreaming” bukan engine, melainkan **mode**: hasil baru diputar
setelah seluruh audio selesai. Streaming memutar sambil mengunduh; jika gagal,
`retryTTSWithNonStreaming` dipakai.

## Alur TTS

```
pilih teks / segmen teks
  → pilih voice + bahasa + speed/pitch
  → bangun request (SSML opsional)
  → engine (streaming atau nonstreaming)
  → audio resource (URL/file) → download → cache
  → buat segment_audio di timeline
  → preview / replace
```

Task manager memakai scheduler agar beberapa kalimat bisa disintesis paralel
(`StartSavingParallel`) tanpa menunggu satu per satu.

## Model

```json
{
  "tts": {
    "engine": "elevenlabs",
    "voice_id": "voice-123",
    "language": "en",
    "text": "Hello world",
    "speed": 1.0,
    "pitch": 0.0,
    "streaming": true,
    "ssml": "<speak>Hello <break time=\"200ms\"/>world</speak>",
    "resource_id": "tts-abc",
    "audio_path": "/cache/tts/abc.mp3"
  }
}
```

Cache key harus mencakup: engine + voice + teks + speed/pitch + format. Jika
tidak, mengganti voice akan memutar audio lama.

## Voice clone

`CloneToneActivity` menunjukkan alur consent + rekaman sample + training +
audition + subscribe. Secara umum:

```
rekam 30–120 detik sample bersih
  → upload (consent disimpan)
  → training job (server)
  → status polling
  → voice_id baru
  → TTS memakai voice_id tersebut
```

Jangan mengklaim clone on-device: class `ElevenLabsApiService` dan
`engine/server` menunjukkan proses server. Clone juga terikat consent dan
kebijakan penyedia.

## Implementasi referensi (clean-room)

```kotlin
interface TtsEngine {
    suspend fun synthesize(req: TtsRequest): TtsResult
    fun supportsStreaming(): Boolean
}

data class TtsRequest(
    val text: String,
    val voiceId: String,
    val language: String,
    val speed: Float = 1f,
    val pitch: Float = 0f,
    val ssml: String? = null,
)

data class TtsResult(val audio: File, val durationUs: Long, val cached: Boolean)

class TtsManager(
    private val engines: Map<String, TtsEngine>,
    private val cache: TtsCache,
) {
    suspend fun speak(engine: String, req: TtsRequest): TtsResult {
        val key = cache.key(engine, req)
        cache.get(key)?.let { return TtsResult(it, it.durationUs(), true) }

        val primary = engines.getValue(engine)
        return try {
            primary.synthesize(req).also { cache.put(key, it.audio) }
        } catch (e: StreamingUnavailable) {
            engines.getValue("nonstreaming").synthesize(req)
                .also { cache.put(key, it.audio) }
        }
    }
}
```

Menambahkan hasil ke draft:

```kotlin
suspend fun addTtsToTimeline(draft: MutableDraft, textSegment: Segment, result: TtsResult) {
    val audio = draft.addMaterial(AudioMaterial(path = result.audio.absolutePath,
        duration = result.durationUs))
    val track = draft.ensureTrack("audio")
    track.segments += Segment(
        type = "segment_audio",
        materialId = audio.id,
        targetTimeRange = textSegment.targetTimeRange.copy(duration = result.durationUs),
        sourceTimeRange = TimeRange(0, result.durationUs),
    )
}
```

## Catatan paritas

- **P0**: satu engine TTS, pilih voice, preview, insert ke timeline, cache.
- **P1**: streaming, SSML, multi-bahasa, speed/pitch, fallback nonstreaming.
- **P2**: voice clone, multi-engine routing, quota/credit, parallel synthesis.

## Unknown

- Voice clone dilatih di provider mana per region; kode hanya menunjukkan
  layanan pihak ketiga + server internal.
- Batas karakter per request, format audio hasil, dan biaya tidak terbaca.
- Apakah `sami` adalah engine TTS atau adaptor playback.

## Status implementasi Zcut (M12)

Yang sudah berjalan di Android:

| Bagian | Implementasi |
|---|---|
| Kontrak | `SpeechEngine.synthesize/voices/speak/stop` + `SpeechVoice` di `core/render/RenderContracts.kt` |
| Engine perangkat | `core/media/AndroidSpeechEngine.kt` — `android.speech.tts.TextToSpeech`, inisialisasi async, satu mutex, sintesis ke file + listener progres |
| Aturan skrip | `core/domain/TtsScript.kt` — sanitize (maks 1.000 char), estimasi durasi, cache key SHA-256 atas teks+suara+rate+pitch |
| Cache | `AppContainer.ttsCacheDir` (`filesDir/tts`); berkas dipakai ulang bila sudah ada |
| Layar & state | `feature/tools/TtsScreen.kt` + `TtsViewModel.kt`, rute `tts?projectId=&atMs=` |
| Penyisipan | Hasil disintesis masuk timeline lewat `AudioEditor.insertAudio` di posisi playhead |

Batas yang disengaja (bukan paywall):

- **Voice clone suara tertentu ditandai BUTUH BACKEND**; butuh model suara yang
  tidak ada di perangkat. TTS sistem tetap gratis dan lokal.
- Suara yang tersedia hanya yang dilaporkan engine perangkat; bila engine tidak
  memberi daftar, ditampilkan satu entri "Bawaan perangkat" — bukan daftar kosong.
- Hanya kualitas default engine (rate 0.5×–2×, pitch 0.5×–2×); SSML dan
  streaming belum dipakai.
- Tidak ada permintaan jaringan: seluruh sintesis lewat service lokal Android.

Unit test: `TtsScriptTest` (sanitize, estimasi, cache key deterministik);
paritas rute `tts` di `ZcutDestinationsTest`.
