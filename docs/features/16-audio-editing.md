# 16 — Audio Editing, Music & Dubbing

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> DSP dasar lokal; vocal separation dan dubbing kualitas penuh ditandai **BUTUH BACKEND**.

## Bukti

- Package: `com.vega.audio` (2.135 tone, 1.077 AI music, 726 library,
  448 dubbing, 366 lip sync, 286 voice change, 289 textstart/recording,
  173 music import, 141 audio service, 125 sound effect, 61 vocal separation).
- Module: `AddAudio`, `AddAudioBeat`, `AddVideoBeat`, `AdjustVolume`,
  `AudioFadeIn`, `AudioFadeOut`, `AudioLoudnessUnify`, `AudioSeparateSound`,
  `AudioChangeVoice`, `AudioChangeVoiceAdjust`, `AudioRealtimeDenoise`,
  `AudioWordInfo`, `AudioToBsInfoAlgorithmParam`, `MuxerExportAudio`,
  `GetAllBeats`, `GetAllAudioBeats`, `EnableVocalBeautify`.
- Native: `libaudioeffect`, `libfdk-aac`, `libopus`, `libaudio_fingerprint_sdk`,
  `libnativeaudio`.
- UI: `com.vega.audio.view`, `view/panel`, `view/dock`, `viewmodel`.

## Sub-fitur

1. **Music library** — kategori, search, preview, download, copyright check.
2. **Import audio** — dari file / ekstrak dari video.
3. **Voiceover** — rekam audio, monitor, retake.
4. **Volume** — per segmen, fade in/out, loudness normalize.
5. **Beat** — deteksi BPM/beat, auto-cut, beat marker.
6. **Voice change** — pitch/formant/robot/child/deep.
7. **Vocal separation** — pisah vokal & instrumental.
8. **Denoise** — audio realtime denoise.
9. **Dubbing** — ganti bahasa, TTS, lip sync.
10. **Sound effect** — library SFX.

## Mixer

```
audio segments (timeline)
  → resample ke project rate (48k)
  → gain / fade envelope
  → voice change / denoise / separation (opsional)
  → mix (sum + limiter)
  → AAC encode saat export
```

Rumus fade:

```
gain(t) = base * min(1, (t - start)/fadeIn) * min(1, (end - t)/fadeOut)
```

Loudness normalize biasanya target -14 LUFS (bukan bukti CapCut; nilai umum).

## Model

```json
{
  "type": "segment_audio",
  "material_id": "audio-1",
  "target_time_range": {"start": 0, "duration": 5000000},
  "source_time_range": {"start": 0, "duration": 5000000},
  "volume": 0.8,
  "fade": {"in": 500000, "out": 500000},
  "speed": 1.0,
  "beat": {"bpm": 120, "offset": 0},
  "voice_change": {"type": "none", "pitch": 0, "formant": 0}
}
```

## Implementasi referensi (clean-room)

```kotlin
data class AudioClip(
    val path: String,
    val timelineStartUs: Long,
    val durationUs: Long,
    val volume: Float = 1f,
    val fadeInUs: Long = 0,
    val fadeOutUs: Long = 0,
)

fun mix(clips: List<AudioClip>, totalUs: Long, sampleRate: Int = 48000): FloatArray {
    val out = FloatArray((totalUs * sampleRate / 1_000_000).toInt())
    for (c in clips) {
        val pcm = decodePcm(c.path, sampleRate)
        val offset = (c.timelineStartUs * sampleRate / 1_000_000).toInt()
        for (i in pcm.indices) {
            val t = i.toLong() * 1_000_000 / sampleRate
            val g = c.volume * fadeGain(t, c.durationUs, c.fadeInUs, c.fadeOutUs)
            val j = offset + i
            if (j in out.indices) out[j] += pcm[i] * g
        }
    }
    return softLimit(out)
}

fun fadeGain(t: Long, dur: Long, fin: Long, fout: Long): Float {
    val a = if (fin > 0) (t.toFloat() / fin).coerceIn(0f, 1f) else 1f
    val b = if (fout > 0) ((dur - t).toFloat() / fout).coerceIn(0f, 1f) else 1f
    return minOf(a, b)
}
```

## Catatan paritas

- **P0**: tambah audio, volume, fade, trim, mute, export mix.
- **P1**: music library, beat marker, voiceover, denoise, voice change.
- **P2**: vocal separation, dubbing + lip sync, loudness AI, SFX marketplace.

## Unknown

- Apakah `AudioSeparateSound` on-device atau cloud; `libaudio_fingerprint_sdk`
  menunjukkan ada model lokal untuk fingerprint, bukan pemisahan.
- Format metadata beat (ms vs frame) tidak terbaca.
