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

/**
 * A per-pixel foreground matte. Values are 0 (background) to 1 (subject), row
 * major, `width * height` long. Plain data: no Android types, so the matte maths
 * can be exercised off-device.
 */
class SubjectMask(
    val confidence: FloatArray,
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0 && height > 0) { "mask must not be empty" }
        require(confidence.size >= width * height) {
            "mask has ${confidence.size} values for ${width}x$height"
        }
    }

    /** Confidence at a pixel; out-of-range coordinates are clamped. */
    fun at(x: Int, y: Int): Float {
        val px = x.coerceIn(0, width - 1)
        val py = y.coerceIn(0, height - 1)
        return confidence[py * width + px]
    }
}

/**
 * On-device subject segmentation for a single still frame. Null means the
 * device could not segment (unsupported hardware or a failed model load) — the
 * caller must say so instead of drawing a guess.
 */
interface SubjectSegmenter {
    suspend fun segment(frame: android.graphics.Bitmap): SubjectMask?
}

interface EffectPipeline {
    fun supports(effectId: String): Boolean
}

/** Normalised (0..1) point inside an analysed frame. */
data class FacePoint(val x: Float, val y: Float) {
    companion object {
        fun of(px: Float, py: Float, width: Int, height: Int): FacePoint = FacePoint(
            x = (px / width.coerceAtLeast(1)).coerceIn(0f, 1f),
            y = (py / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        )
    }
}

/** Normalised (0..1) bounding box of one face. */
data class FaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    companion object {
        /**
         * Builds a box from pixel coordinates, clamped into the frame. A box
         * that ends up empty is still returned as-is; the caller decides what an
         * unusable detection means.
         */
        fun of(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            width: Int,
            height: Int
        ): FaceBox {
            val w = width.coerceAtLeast(1)
            val h = height.coerceAtLeast(1)
            return FaceBox(
                left = (left / w).coerceIn(0f, 1f),
                top = (top / h).coerceIn(0f, 1f),
                right = (right / w).coerceIn(0f, 1f),
                bottom = (bottom / h).coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * One detected face: geometry only, in normalised frame coordinates so the same
 * detection works for the preview (any size) and the full-resolution frame.
 *
 * Landmarks mirror what the shipped reshape package drives its distortion mesh
 * with (`assets/zip/FaceReshape_V2/distortion.json`: one entry per landmark
 * `pos`, with `offsetX/offsetY`, `radiusX/radiusY`, `scale`). Anything the model
 * did not return is `null` and the caller must degrade instead of guessing.
 */
data class FaceGeometry(
    val bounds: FaceBox,
    val leftEye: FacePoint? = null,
    val rightEye: FacePoint? = null,
    val noseBase: FacePoint? = null,
    val mouthLeft: FacePoint? = null,
    val mouthRight: FacePoint? = null,
    val mouthBottom: FacePoint? = null
) {
    val hasEyeLandmarks: Boolean get() = leftEye != null && rightEye != null
    val hasMouthLandmarks: Boolean
        get() = mouthLeft != null && mouthRight != null && mouthBottom != null

    /** Normalised distance between the eyes; 0 when the landmarks are missing. */
    fun eyeDistance(): Float {
        val left = leftEye ?: return 0f
        val right = rightEye ?: return 0f
        val dx = right.x - left.x
        val dy = right.y - left.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Same geometry rebuilt so `leftEye.x <= rightEye.x`. The model's
     * left/right naming follows the subject, not the image, and the local
     * operations must not mirror the result on a flipped frame.
     */
    fun withCanonicalEyes(): FaceGeometry {
        val left = leftEye ?: return this
        val right = rightEye ?: return this
        return if (left.x <= right.x) this
        else copy(leftEye = right, rightEye = left)
    }
}

/**
 * On-device face detection for a single still frame. An empty list means "no
 * face found" (or the model could not load) — never a fabricated face.
 */
interface FaceDetector {
    suspend fun detect(frame: android.graphics.Bitmap): List<FaceGeometry>
}
