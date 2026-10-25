package com.zaaam.zcut.core.render

import android.content.Context
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.transformer.CompositionPlayer
import com.zaaam.zcut.core.model.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Media3-backed timeline preview.
 *
 * The player is created per editor screen (never shared): it owns a GL pipeline
 * and must be released when the draft closes. All methods run on the main
 * thread, which is also the thread CompositionPlayer requires.
 *
 * M3 previews the main visual track plus one audio track. Filters, effects,
 * text, overlays and multiple audio tracks are added by later milestones; until
 * then those layers simply do not appear in the preview.
 */
@OptIn(ExperimentalApi::class, UnstableApi::class)
class Media3PreviewEngine(
    private val context: Context
) : PreviewEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PreviewState())
    override val state: StateFlow<PreviewState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _playerFlow = MutableStateFlow<Player?>(null)
    override val playerFlow: StateFlow<Player?> = _playerFlow.asStateFlow()

    private var compositionPlayer: CompositionPlayer? = null
    private var plan: PreviewPlan? = null
    private var range: LongRange? = null
    private var muted = false
    private var tickerJob: Job? = null
    @Volatile
    private var released = false

    private val frameLock = Any()
    private val frameStamps = ArrayDeque<Long>()
    private var lastFpsUpdateMs = 0L

    override fun prepare(project: Project, startPositionMs: Long) {
        if (released) return
        // Only media inside the app's private workspace may reach the player.
        val newPlan = PreviewPlanBuilder.build(project, context.filesDir.absolutePath)
        val previous = plan
        // Text/style edits change the timeline but not the media sequences.
        // Rebuilding the player for those would tear down the GL pipeline and
        // flash black on every keystroke, so only refresh the duration/state.
        if (compositionPlayer != null && previous != null && previous.sameMediaAs(newPlan)) {
            plan = newPlan
            _state.update {
                it.copy(
                    durationMs = newPlan.durationMs,
                    hasVideo = newPlan.hasVideo,
                    hasAudio = newPlan.hasAudio
                )
            }
            return
        }
        plan = newPlan
        range = null
        releasePlayer()
        synchronized(frameLock) {
            frameStamps.clear()
            lastFpsUpdateMs = 0L
        }

        val start = PreviewSeek.clamp(startPositionMs, newPlan.durationMs)
        _positionMs.value = start
        _state.value = PreviewState(
            durationMs = newPlan.durationMs,
            hasVideo = newPlan.hasVideo,
            hasAudio = newPlan.hasAudio,
            isMuted = muted
        )
        if (!newPlan.isPlayable) return

        val composition = runCatching { Media3CompositionBuilder.build(newPlan) }
            .getOrElse { error ->
                Log.w(TAG, "Gagal membangun composition pratinjau: ${error.javaClass.simpleName}")
                _state.update { it.copy(error = PREVIEW_ERROR) }
                return
            }
        val newPlayer = runCatching {
            CompositionPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
                .build()
        }.getOrElse { error ->
            Log.w(TAG, "Gagal membuat CompositionPlayer: ${error.javaClass.simpleName}")
            _state.update { it.copy(error = PREVIEW_ERROR) }
            return
        }

        compositionPlayer = newPlayer
        _playerFlow.value = newPlayer
        newPlayer.addListener(playerListener)
        newPlayer.volume = if (muted) 0f else 1f
        // The frame listener must be registered before setComposition so the
        // primary sequence player receives it when it is created.
        newPlayer.setVideoFrameMetadataListener(frameListener)
        val setupError = runCatching {
            newPlayer.setComposition(composition, start)
            newPlayer.prepare()
        }.exceptionOrNull()
        if (setupError != null) {
            Log.w(TAG, "Gagal menyiapkan composition pratinjau: ${setupError.javaClass.simpleName}")
            releasePlayer()
            _state.update {
                it.copy(isPlaying = false, isReady = false, error = PREVIEW_ERROR)
            }
            return
        }
        startTicker()
    }

    override fun play() {
        val currentPlayer = compositionPlayer ?: return
        val duration = plan?.durationMs ?: return
        val target = PreviewSeek.playFrom(range, currentPlayer.currentPosition, duration)
        if (target != currentPlayer.currentPosition) {
            currentPlayer.seekTo(target)
        }
        _positionMs.value = target
        currentPlayer.play()
        startTicker()
    }

    override fun pause() {
        compositionPlayer?.pause()
        // Drop the FPS window so a long pause does not make the first frame
        // after resuming report a stale, meaningless rate.
        synchronized(frameLock) {
            frameStamps.clear()
            lastFpsUpdateMs = 0L
        }
        if (_state.value.renderedFps != 0) {
            _state.update { it.copy(renderedFps = 0) }
        }
    }

    override fun seekTo(positionMs: Long) {
        val target = PreviewSeek.clamp(positionMs, plan?.durationMs ?: 0L)
        range = PreviewSeek.rangeAfterSeek(range, target)
        _positionMs.value = target
        compositionPlayer?.seekTo(target)
        if (compositionPlayer != null) startTicker()
    }

    override fun setRange(startMs: Long?, endMs: Long?) {
        range = PreviewSeek.range(startMs, endMs, plan?.durationMs ?: 0L)
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        compositionPlayer?.volume = if (muted) 0f else 1f
        _state.update { it.copy(isMuted = muted) }
    }

    override fun release() {
        if (released) return
        released = true
        releasePlayer()
        scope.cancel()
        plan = null
        range = null
        synchronized(frameLock) {
            frameStamps.clear()
        }
        _positionMs.value = 0L
        _state.value = PreviewState()
    }

    private fun releasePlayer() {
        tickerJob?.cancel()
        tickerJob = null
        compositionPlayer?.release()
        compositionPlayer = null
        _playerFlow.value = null
    }

    /**
     * Publishes the playhead while playing and, for still images, waits until
     * the image renderer is ready. Media3's image renderer does not emit
     * frame-metadata callbacks, so `hasFrame` cannot rely on that listener
     * alone. The loop stops as soon as playback pauses and no image is pending.
     */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val currentPlayer = compositionPlayer ?: break
                val duration = plan?.durationMs ?: 0L
                val current = PreviewSeek.clamp(currentPlayer.currentPosition, duration)
                if (current != _positionMs.value) {
                    _positionMs.value = current
                }
                if (_state.value.error != null) break

                val nextIsImage = plan?.nextVisualItem(current)?.isImage == true
                if (!_state.value.hasFrame &&
                    currentPlayer.playbackState == Player.STATE_READY &&
                    nextIsImage
                ) {
                    _state.update { it.copy(hasFrame = true) }
                }

                if (currentPlayer.isPlaying && PreviewSeek.reachedEnd(range, current, duration)) {
                    val end = PreviewSeek.clamp(range?.last ?: duration, duration)
                    currentPlayer.pause()
                    currentPlayer.seekTo(end)
                    _positionMs.value = end
                    break
                }

                val waitingForImage = nextIsImage && !_state.value.hasFrame
                if (!currentPlayer.isPlaying && !waitingForImage) break
                delay(if (currentPlayer.isPlaying) PLAYING_TICK_MS else IDLE_TICK_MS)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val ready = playbackState == Player.STATE_READY ||
                playbackState == Player.STATE_ENDED
            _state.update { it.copy(isReady = ready) }
            if (ready) markImageFrameIfNeeded()
            if (playbackState == Player.STATE_ENDED) {
                val duration = plan?.durationMs ?: 0L
                _positionMs.value = PreviewSeek.clamp(
                    compositionPlayer?.currentPosition ?: _positionMs.value,
                    duration
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Do not log the throwable: Media3 messages embed media URIs/paths.
            Log.w(TAG, "Pemutaran pratinjau gagal (kode ${error.errorCode})")
            _state.update {
                it.copy(
                    isPlaying = false,
                    isReady = false,
                    error = PREVIEW_ERROR
                )
            }
        }
    }

    private fun markImageFrameIfNeeded() {
        val currentPlayer = compositionPlayer ?: return
        if (_state.value.hasFrame) return
        if (currentPlayer.playbackState != Player.STATE_READY) return
        val position = PreviewSeek.clamp(currentPlayer.currentPosition, plan?.durationMs ?: 0L)
        if (plan?.nextVisualItem(position)?.isImage == true) {
            _state.update { it.copy(hasFrame = true) }
        }
    }

    private val frameListener = object : VideoFrameMetadataListener {
        override fun onVideoFrameAboutToBeRendered(
            presentationTimeUs: Long,
            releaseTimeNs: Long,
            format: Format,
            mediaFormat: MediaFormat?
        ) {
            if (released) return
            val now = SystemClock.elapsedRealtime()
            // This callback runs on the playback thread, so the sample window
            // is guarded against prepare/release clearing it from the main one.
            val fpsUpdate = synchronized(frameLock) {
                frameStamps.addLast(now)
                while (frameStamps.size > MAX_FPS_SAMPLES) {
                    frameStamps.removeFirst()
                }
                while (frameStamps.isNotEmpty() && now - frameStamps.first() > FPS_WINDOW_MS) {
                    frameStamps.removeFirst()
                }
                if (lastFpsUpdateMs == 0L) {
                    // Start the window on the first frame so the overlay never
                    // reports a meaningless "1 fps" during the first second.
                    lastFpsUpdateMs = now
                    null
                } else if (now - lastFpsUpdateMs >= FPS_WINDOW_MS) {
                    lastFpsUpdateMs = now
                    frameStamps.size
                } else {
                    null
                }
            }

            if (!_state.value.hasFrame) {
                _state.update { it.copy(hasFrame = true) }
            }
            if (fpsUpdate != null) {
                _state.update { it.copy(renderedFps = fpsUpdate) }
            }
        }
    }

    private companion object {
        const val TAG = "ZcutPreview"
        const val PREVIEW_ERROR = "Pratinjau tidak tersedia"
        const val PLAYING_TICK_MS = 33L
        const val IDLE_TICK_MS = 120L
        const val FPS_WINDOW_MS = 1_000L
        const val MAX_FPS_SAMPLES = 120
    }
}
