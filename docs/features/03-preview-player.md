# 03 — Preview Player

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Implementasi inti on-device; lihat `docs/feature-index.md` untuk status backend.

## Bukti

- `com.vega.middlebridge.client.PlayerClient` dengan callback:
  `ISetSurfaceCallback`, `IPlayCallback`, `IPauseCallback`, `ISeekTimeCallback`,
  `ISeekWithSpeedCallback`, `ISetPreviewTimeRangeCallback`,
  `IRegisterPlayerProgressCallback`, `IRegisterPlayerFrameRenderedCallback`,
  `IGetPerformanceInfoCallback`, `ISetFpsCallback`, `ISetDisplayStateCallback`.
- `com.vega.middlebridge.client.SingleSegmentPlayClient`,
  `SingleResourcePlayerClient`, `NetworkPlayerClient`, `PcmPlayerClient`.
- Native: `libttmplayer.so`, `libvcn`, `libttdemux`, VESDK player.
- `com.vega.ve.api.VEService`, `VESDKHelper`, `VEState`, `SeekFlag`.

## Perilaku

- Preview bukan memutar file utuh, tapi **compositing timeline**: decode per
  segmen, apply effect/filter/text, render ke Surface.
- Seek harus **frame-accurate**; ada mode preview range dan speed.
- Player mengirim progress + frame PTS; UI timeline memakai itu untuk playhead.
- Ada jalur terpisah untuk network preview (template/cloud) dan PCM audio.
- Saat scrub, aplikasi memakai preview quality lebih rendah; saat pause,
  frame final dirender.

## Arsitektur player

```
PlayerClient (domain)
  ├── DraftPlayer      : timeline composite
  ├── SingleSegment    : preview satu klip
  ├── NetworkPlayer    : URL/stream (template preview)
  └── PcmPlayer        : audio-only / TTS preview

Surface → VESDK → decode graph → effect graph → GL output
                              ↘ audio track mixer → AudioTrack
```

## State machine

```
IDLE → PREPARING → READY → PLAYING ⇄ PAUSED
                       ↘ SEEKING → READY
                       ↘ ERROR
```

`prepare` membangun render graph dari draft; `seek` membatalkan frame lama
(`flush`) lalu decode ke PTS target.

## Implementasi referensi (clean-room)

```kotlin
interface PreviewPlayer {
    val state: StateFlow<PlayerState>
    val positionUs: StateFlow<Long>
    fun attach(surface: Surface)
    fun prepare(draft: Draft)
    fun play()
    fun pause()
    fun seekTo(us: Long, precise: Boolean = true)
    fun setSpeed(speed: Float)
    fun setRange(range: TimeRange?)
    fun release()
}

class VesdkPreviewPlayer(
    private val engine: VesdkEngine,
    private val scope: CoroutineScope,
) : PreviewPlayer {
    private val _state = MutableStateFlow(PlayerState.Idle)
    override val state = _state.asStateFlow()
    private val _position = MutableStateFlow(0L)
    override val positionUs = _position.asStateFlow()

    override fun prepare(draft: Draft) = scope.launch {
        _state.value = PlayerState.Preparing
        engine.setDraft(draft.toEngineModel())
        engine.prepare()
        _state.value = PlayerState.Ready
    }

    override fun seekTo(us: Long, precise: Boolean) = scope.launch {
        _state.value = PlayerState.Seeking
        engine.flush()
        engine.seek(us, precise)
        _position.value = engine.currentPts()
        _state.value = PlayerState.Ready
    }
}
```

## Timing & performa

- Gunakan `Choreographer`/`FrameCallback` untuk sinkron playhead, bukan timer
  wall-clock.
- Decode hardware untuk H.264/HEVC; fallback software untuk profile aneh.
- Cache frame hasil effect berat; invalidasi per node, bukan seluruh draft.
- Saat scrub cepat: drop frame, jangan antre seek (coalesce ke target terbaru).

## Catatan paritas

- **P0**: surface, prepare, play/pause, seek akurat, progress.
- **P1**: preview range, speed preview, network preview, low-res scrub.
- **P2**: frame callback + performance metrics, multi-surface, HDR preview.

## Unknown

- Apakah compositing memakai GL texture graph penuh atau layer per segmen.
- Strategi cache frame tidak terlihat dari API publik.
