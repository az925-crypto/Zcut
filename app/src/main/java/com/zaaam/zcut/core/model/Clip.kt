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
    val sticker: StickerConfig? = null,
    val mask: MaskConfig? = null,
    val chroma: ChromaConfig? = null,
    val matting: MattingConfig? = null,
    val beauty: BeautyConfig? = null,
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
        get() = if (speed.isNaN()) 1f else speed.coerceIn(0.1f, 8f)
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

/** Single-select visual style from the ACC'd text mockup. */
@Serializable
enum class TextStylePreset {
    NONE,
    BOLD,
    OUTLINE,
    SHADOW,
    NEON,
    GRADIENT
}

/**
 * Text layer stored on a [ClipType.TEXT] clip.
 *
 * `fontSizeSp` keeps its historical name but is a design-point value scaled
 * against a 260 dp reference frame (see `TextLayout`), so the same draft looks
 * identical in the editor preview and the export. All M11 fields are additive
 * with defaults: drafts written before M11 decode unchanged.
 */
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
    val animation: TextAnimation = TextAnimation.NONE,
    val stylePreset: TextStylePreset = TextStylePreset.NONE,
    val gradientColorArgb: Long? = null,
    /** Letter spacing in em (fraction of the font size). */
    val letterSpacingEm: Float = 0f,
    val lineHeight: Float = 1.2f
)

/**
 * A sticker placed over the video, drawn from the app's own glyph catalogue
 * ([com.zaaam.zcut.core.domain.StickerCatalog]); no original asset is used.
 *
 * Position, scale, rotation and flip live on the clip's [Transform] and its
 * [Clip.opacity], so a sticker animates with the same keyframes as any other
 * clip. Additive: drafts written before M16 decode with `null`.
 */
@Serializable
data class StickerConfig(
    /**
     * Catalogue key, resolved by
     * [com.zaaam.zcut.core.domain.StickerCatalog.canonical]. An unknown key
     * degrades to the first entry instead of drawing nothing.
     */
    val stickerId: String = "trend-lol"
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

/** What replaces the background the on-device matte removes. */
@Serializable
enum class MattingBackground {
    /** Real transparency; the editor shows a checkerboard behind it. */
    TRANSPARENT,
    COLOR,
    BLUR,

    /** A still image the user picked from the device. */
    IMAGE,

    /** Needs a backend: the prompt is kept, nothing is generated locally. */
    AI
}

/**
 * Subject cutout for one clip. The matte itself is produced on-device by
 * [com.zaaam.zcut.core.render.SubjectSegmenter]; this only describes what to do
 * with it. Additive: drafts written before M14 decode with `null`.
 */
@Serializable
data class MattingConfig(
    val enabled: Boolean = true,
    val background: MattingBackground = MattingBackground.TRANSPARENT,
    val colorArgb: Long = 0xFF1D4ED8,
    /** Background blur strength, 0..1 (BLUR only). */
    val blur: Float = 0.5f,
    /** Raises the matte confidence threshold: 0 = soft edge, 1 = hard edge. */
    val edgeRefine: Float = 0.5f,
    /** Extra darkening of the background, 0..1. */
    val shadow: Float = 0.2f,
    /** Local image used by [MattingBackground.IMAGE]; null until one is picked. */
    val imageAssetId: String? = null,
    /** Prompt for [MattingBackground.AI]; only meaningful with a backend. */
    val aiPrompt: String = ""
)

/**
 * Retouch/beauty for one clip.
 *
 * The slider keys, their admissible ranges and the slider scale are taken from
 * the shipped local-beauty config of the analysed artifact
 * (`assets/local_beauty_all.json` → `beauty_all_info.category[].items[]`): every
 * item carries a `key`, `relative_min_value`, `relative_max_value` and a
 * `_ui_value` that is a 0–100 slider position. Only those names/ranges are
 * reused so a draft means the same thing the original meant; no original code
 * or asset is copied.
 *
 * Values here are the 0–100 slider positions, like `_ui_value`.
 * [com.zaaam.zcut.core.domain.BeautyEditor] converts them to the engine value
 * and holds the full shipped table (including the keys the approved mockup does
 * not have a slot for yet).
 */
@Serializable
data class BeautyConfig(
    /** `auto_buffing` — skin smoothing. */
    val smooth: Float = 0f,
    /** `auto_balance_skin` — whitening. */
    val whiten: Float = 0f,
    /** `thin_face` — face slimming. */
    val faceSlim: Float = 0f,
    /** `eye_size` — eye enlargement. */
    val eyes: Float = 0f,
    /** `nose_wing` — nose slimming. */
    val nose: Float = 0f,
    /** `type_lips` — lipstick. */
    val lips: Float = 0f,
    /** Keeps pore detail instead of a plastic look. */
    val preserveTexture: Boolean = true
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
