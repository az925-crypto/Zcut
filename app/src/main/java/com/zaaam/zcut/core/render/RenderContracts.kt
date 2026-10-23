package com.zaaam.zcut.core.render

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Track
import kotlinx.coroutines.flow.Flow

data class RenderRequest(
    val project: Project,
    val outputUri: String,
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
        val outputUri: String,
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
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16
)

interface PreviewEngine {
    fun prepare(project: Project): Flow<PreviewState>
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
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

interface SpeechEngine {
    suspend fun synthesize(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        outputPath: String
    ): Boolean
}

interface SubjectSegmenter {
    suspend fun segment(inputUri: String, outputMaskPath: String): Boolean
}

interface EffectPipeline {
    fun supports(effectId: String): Boolean
}
