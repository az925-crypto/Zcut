package com.zaaam.zcut.core.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.roundToLong

@Serializable
enum class TrackType {
    VIDEO,
    OVERLAY,
    TEXT,
    AUDIO,
    CAPTION,
    EFFECT,
    STICKER
}

@Serializable
enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    STICKER,
    CAPTION,
    EFFECT,
    SHAPE,
    ADJUSTMENT
}

@Serializable
data class Track(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackType,
    val clips: List<Clip> = emptyList(),
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    /** Final gain is track.volume * clip.audio.volume. */
    val volume: Float = 1f
)

fun TrackType.accepts(clipType: ClipType): Boolean = when (this) {
    TrackType.VIDEO -> when (clipType) {
        ClipType.VIDEO,
        ClipType.IMAGE,
        ClipType.ADJUSTMENT -> true
        else -> false
    }
    TrackType.OVERLAY -> when (clipType) {
        ClipType.VIDEO,
        ClipType.IMAGE,
        ClipType.STICKER,
        ClipType.SHAPE,
        ClipType.EFFECT,
        ClipType.ADJUSTMENT -> true
        else -> false
    }
    TrackType.TEXT -> clipType == ClipType.TEXT
    TrackType.AUDIO -> clipType == ClipType.AUDIO
    TrackType.CAPTION -> clipType == ClipType.CAPTION
    TrackType.EFFECT -> clipType == ClipType.EFFECT
    TrackType.STICKER -> clipType == ClipType.STICKER
}

@Serializable
data class Timeline(
    val tracks: List<Track> = emptyList(),
    val durationMs: Long = 0L,
    val playheadMs: Long = 0L
) {
    fun track(trackId: String): Track? = tracks.firstOrNull { it.id == trackId }

    fun clip(clipId: String): Clip? =
        tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.id == clipId }
}

@Serializable
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val assetId: String? = null,
    val type: ClipType = ClipType.VIDEO,
    val startMs: Long,
    val durationMs: Long,
    val sourceStartMs: Long = 0L,
    val sourceDurationMs: Long = 0L,
    val speed: SpeedConfig = SpeedConfig(),
    val transform: Transform = Transform(),
    val opacity: Float = 1f,
    val color: ColorAdjustments = ColorAdjustments(),
    val filters: List<FilterConfig> = emptyList(),
    val effects: List<EffectConfig> = emptyList(),
    val transitionIn: TransitionConfig? = null,
    val transitionOut: TransitionConfig? = null,
    val keyframes: List<Keyframe> = emptyList(),
    val text: TextConfig? = null,
    val mask: MaskConfig? = null,
    val chroma: ChromaConfig? = null,
    val audio: AudioConfig = AudioConfig(),
    val isReversed: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    val endMs: Long
        get() = when {
            durationMs <= 0L -> startMs
            startMs > Long.MAX_VALUE - durationMs -> Long.MAX_VALUE
            else -> startMs + durationMs
        }

    val effectiveSourceDurationMs: Long
        get() = if (sourceDurationMs > 0L) {
            sourceDurationMs
        } else {
            (durationMs.coerceAtLeast(0L).toDouble() * speed.safeSpeed).roundToLong()
        }
}

@Serializable
data class SpeedConfig(
    val speed: Float = 1f,
    val preservePitch: Boolean = true,
    val curve: List<SpeedPoint> = emptyList()
) {
    val safeSpeed: Float
        get() = speed.coerceIn(0.1f, 8f)
}

@Serializable
data class SpeedPoint(
    val atFraction: Float,
    val speed: Float
)

@Serializable
data class Transform(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

@Serializable
enum class AnimatableProperty {
    POSITION_X,
    POSITION_Y,
    SCALE,
    ROTATION,
    OPACITY,
    VOLUME
}

@Serializable
enum class Easing {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BEZIER
}

@Serializable
data class Keyframe(
    val timeMs: Long,
    val property: AnimatableProperty,
    val value: Float,
    val easing: Easing = Easing.LINEAR
)

@Serializable
enum class TransitionType {
    CUT,
    DISSOLVE,
    FADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    ZOOM,
    WHIP,
    FLASH
}

@Serializable
data class TransitionConfig(
    val type: TransitionType = TransitionType.DISSOLVE,
    val durationMs: Long = 500L
)

@Serializable
data class FilterConfig(
    val id: String,
    val intensity: Float = 1f,
    val lutAssetPath: String? = null
)

@Serializable
data class EffectConfig(
    val id: String,
    val intensity: Float = 1f,
    val params: Map<String, Float> = emptyMap()
)

@Serializable
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

@Serializable
enum class TextAnimation {
    NONE,
    RISE,
    TYPEWRITER,
    POP,
    GLITCH
}

@Serializable
data class TextConfig(
    val text: String,
    val fontFamily: String = "sans",
    val fontSizeSp: Float = 42f,
    val colorArgb: Long = 0xFFFFFFFF,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val strokeWidth: Float = 0f,
    val strokeColorArgb: Long = 0xFF000000,
    val shadow: Boolean = false,
    val backgroundColorArgb: Long? = null,
    val animation: TextAnimation = TextAnimation.NONE
)

@Serializable
enum class MaskShape {
    NONE,
    LINEAR,
    MIRROR,
    CIRCLE,
    RECTANGLE,
    HEART,
    STAR
}

@Serializable
data class MaskConfig(
    val shape: MaskShape = MaskShape.NONE,
    val feather: Float = 0f,
    val invert: Boolean = false,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 1f,
    val height: Float = 1f,
    val rotationDegrees: Float = 0f
)

@Serializable
data class ChromaConfig(
    val keyColorArgb: Long = 0xFF25D366,
    val similarity: Float = 0.4f,
    val smoothness: Float = 0.1f,
    val spillSuppression: Float = 0.5f
)

@Serializable
data class AudioConfig(
    val volume: Float = 1f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val pitch: Float = 1f,
    val muted: Boolean = false,
    val looping: Boolean = false
)

@Serializable
data class HslBand(
    val hue: Float,
    val saturation: Float = 0f,
    val luminance: Float = 0f
)

@Serializable
data class ColorAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val hslBands: List<HslBand> = emptyList()
) {
    val isNeutral: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f &&
            temperature == 0f && tint == 0f && highlights == 0f &&
            shadows == 0f && vignette == 0f && hslBands.isEmpty()
}
