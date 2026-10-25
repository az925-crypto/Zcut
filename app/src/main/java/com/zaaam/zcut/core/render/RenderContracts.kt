package com.zaaam.zcut.core.render

import androidx.media3.common.Player
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class RenderRequest(
    val project: Project,
    /** Absolute path of the output file; the engine never writes outside it. */
    val outputPath: String,
    val preset: ExportPreset = project.settings.exportPreset
)

sealed interface RenderProgress {
    data object Preparing : RenderProgress
    data class Rendering(
        val percent: Float,
        val etaMs: Long? = null
    ) : RenderProgress
    data object Finalizing : RenderProgress
    data class Completed(
        val outputPath: String,
        val durationMs: Long,
        val sizeBytes: Long
    ) : RenderProgress
    data class Failed(
        val message: String,
        val cause: Throwable? = null
    ) : RenderProgress
}

interface VideoRenderEngine {
    fun render(request: RenderRequest): Flow<RenderProgress>
    suspend fun cancel()
}

data class PreviewState(
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val hasVideo: Boolean = false,
    val hasAudio: Boolean = false,
    /** True once Media3 has rendered a frame; the thumbnail stays until then. */
    val hasFrame: Boolean = false,
    val isMuted: Boolean = false,
    val durationMs: Long = 0L,
    /** Measured frames per second over the last second while playing. */
    val renderedFps: Int = 0,
    val error: String? = null
) {
    val isPlayable: Boolean get() = hasVideo || hasAudio
}

interface PreviewEngine {
    val state: StateFlow<PreviewState>
    val positionMs: StateFlow<Long>

    /** Player for the attached PlayerView; emits null until [prepare]. */
    val playerFlow: StateFlow<Player?>

    fun prepare(project: Project, startPositionMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)

    /** Restricts playback to [startMs]..[endMs]; null clears the range. */
    fun setRange(startMs: Long?, endMs: Long?)
    fun setMuted(muted: Boolean)
    fun release()
}

interface MediaProbe {
    suspend fun probe(uri: String): MediaAsset?
}

interface ThumbnailGenerator {
    suspend fun generate(asset: MediaAsset, atMs: Long, outputPath: String): Boolean
}

interface AudioMixer {
    suspend fun mix(tracks: List<Track>, outputPath: String): Boolean
}

/** One voice exposed by the platform speech engine. */
data class SpeechVoice(
    /** Stable engine-side id; `null`/blank means the engine default voice. */
    val id: String,
    val label: String,
    val locale: String
)

interface SpeechEngine {
    suspend fun synthesize(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        outputPath: String
    ): Boolean

    /**
     * Voices the device reports. Empty means the engine has no usable voices,
     * which the UI must show honestly instead of offering dead choices.
     */
    suspend fun voices(): List<SpeechVoice> = emptyList()

    /** Speaks [text] out loud for a quick preview; no file is written. */
    suspend fun speak(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float
    ): Boolean = false

    /** Stops any in-flight preview speech. */
    fun stop() = Unit
}

interface SubjectSegmenter {
    suspend fun segment(inputUri: String, outputMaskPath: String): Boolean
}

interface EffectPipeline {
    fun supports(effectId: String): Boolean
}
