package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Easing
import com.zaaam.zcut.core.model.Keyframe
import kotlin.math.abs

/**
 * Keyframes are stored per property and timed relative to the clip start, so
 * moving or ripple-deleting a clip never needs to rewrite them. Sampling is
 * pure JVM: the preview pipeline and the UI both read the same value at a given
 * playhead.
 *
 * A keyframe's [Easing] shapes the segment that starts at it, matching the
 * reference behaviour (the left frame owns the curve).
 */
object KeyframeEditor {

    /** Two taps inside this window count as the same keyframe. */
    const val MATCH_TOLERANCE_MS = 40L

    fun upsert(
        clip: Clip,
        property: AnimatableProperty,
        timeMs: Long,
        value: Float,
        easing: Easing = Easing.LINEAR
    ): Clip {
        if (clip.durationMs <= 0L) return clip
        val time = timeMs.coerceIn(0L, clip.durationMs)
        val existing = keyframeAt(clip, property, time)
        val frame = Keyframe(
            timeMs = time,
            property = property,
            value = coerce(property, value),
            easing = existing?.easing ?: easing
        )
        val frames = clip.keyframes
            .filterNot { it.property == property && abs(it.timeMs - time) <= MATCH_TOLERANCE_MS }
            .plus(frame)
            .sortedWith(compareBy({ it.timeMs }, { it.property.ordinal }))
        return clip.copy(keyframes = frames)
    }

    fun remove(
        clip: Clip,
        property: AnimatableProperty,
        timeMs: Long,
        toleranceMs: Long = MATCH_TOLERANCE_MS
    ): Clip {
        val remaining = clip.keyframes.filterNot {
            it.property == property && abs(it.timeMs - timeMs) <= toleranceMs
        }
        return if (remaining.size == clip.keyframes.size) clip else {
            clip.copy(keyframes = remaining)
        }
    }

    fun setEasing(
        clip: Clip,
        property: AnimatableProperty,
        timeMs: Long,
        easing: Easing,
        toleranceMs: Long = MATCH_TOLERANCE_MS
    ): Clip {
        val target = keyframeAt(clip, property, timeMs, toleranceMs) ?: return clip
        return clip.copy(
            keyframes = clip.keyframes.map { frame ->
                if (frame.property == property && frame.timeMs == target.timeMs) {
                    frame.copy(easing = easing)
                } else {
                    frame
                }
            }
        )
    }

    fun keyframeAt(
        clip: Clip,
        property: AnimatableProperty,
        timeMs: Long,
        toleranceMs: Long = MATCH_TOLERANCE_MS
    ): Keyframe? = clip.keyframes
        .asSequence()
        .filter { it.property == property }
        .filter { abs(it.timeMs - timeMs) <= toleranceMs }
        .minByOrNull { abs(it.timeMs - timeMs) }

    /** Static value used when a property has no keyframes at all. */
    fun valueOf(clip: Clip, property: AnimatableProperty): Float = when (property) {
        AnimatableProperty.POSITION_X -> clip.transform.positionX
        AnimatableProperty.POSITION_Y -> clip.transform.positionY
        AnimatableProperty.SCALE -> clip.transform.scale
        AnimatableProperty.ROTATION -> clip.transform.rotationDegrees
        AnimatableProperty.OPACITY -> clip.opacity
        AnimatableProperty.VOLUME -> clip.audio.volume
    }

    fun setProperty(clip: Clip, property: AnimatableProperty, value: Float): Clip {
        val safe = coerce(property, value)
        return when (property) {
            AnimatableProperty.POSITION_X ->
                clip.copy(transform = clip.transform.copy(positionX = safe))
            AnimatableProperty.POSITION_Y ->
                clip.copy(transform = clip.transform.copy(positionY = safe))
            AnimatableProperty.SCALE ->
                clip.copy(transform = clip.transform.copy(scale = safe))
            AnimatableProperty.ROTATION ->
                clip.copy(transform = clip.transform.copy(rotationDegrees = safe))
            AnimatableProperty.OPACITY -> clip.copy(opacity = safe)
            AnimatableProperty.VOLUME ->
                clip.copy(audio = clip.audio.copy(volume = safe))
        }
    }

    /**
     * Slider edits change the static property. Once a property is animated they
     * also write the value at the playhead, so dragging on a keyframed property
     * behaves like CapCut's auto-key.
     */
    fun setPropertyAndAutoKey(
        clip: Clip,
        property: AnimatableProperty,
        value: Float,
        timeMs: Long
    ): Clip {
        val updated = setProperty(clip, property, value)
        return if (clip.keyframes.any { it.property == property }) {
            upsert(updated, property, timeMs, value)
        } else {
            updated
        }
    }

    fun sample(clip: Clip, property: AnimatableProperty, timeMs: Long): Float =
        sample(clip.keyframes, property, timeMs, valueOf(clip, property))

    /**
     * Holds the first keyframe's value before it and the last keyframe's value
     * after it (standard editor hold semantics); interpolates between them.
     * Falls back to [fallback] only when the property has no keyframes.
     */
    fun sample(
        keyframes: List<Keyframe>,
        property: AnimatableProperty,
        timeMs: Long,
        fallback: Float
    ): Float {
        val frames = keyframes
            .asSequence()
            .filter { it.property == property }
            .sortedBy { it.timeMs }
            .toList()
        if (frames.isEmpty()) return fallback
        if (timeMs <= frames.first().timeMs) return frames.first().value
        if (timeMs >= frames.last().timeMs) return frames.last().value
        val index = frames.indexOfLast { it.timeMs <= timeMs }.coerceAtLeast(0)
        val left = frames[index]
        val right = frames.getOrNull(index + 1) ?: return left.value
        val span = right.timeMs - left.timeMs
        if (span <= 0L) return right.value
        val progress = (timeMs - left.timeMs).toFloat() / span
        val eased = easingTransform(left.easing, progress)
        return left.value + (right.value - left.value) * eased
    }

    fun easingTransform(easing: Easing, progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return when (easing) {
            Easing.LINEAR -> t
            Easing.EASE_IN -> t * t
            Easing.EASE_OUT -> 1f - (1f - t) * (1f - t)
            // ponytail: BEZIER uses a smoothstep approximation of (0.42,0,0.58,1)
            // until a real bezier solver is needed by the export pipeline.
            Easing.EASE_IN_OUT, Easing.BEZIER -> t * t * (3f - 2f * t)
        }
    }

    fun rangeOf(property: AnimatableProperty): ClosedFloatingPointRange<Float> =
        when (property) {
            AnimatableProperty.POSITION_X,
            AnimatableProperty.POSITION_Y -> -100f..100f
            AnimatableProperty.SCALE -> 0.1f..4f
            AnimatableProperty.ROTATION -> -180f..180f
            AnimatableProperty.OPACITY -> 0f..1f
            AnimatableProperty.VOLUME -> 0f..2f
        }

    fun labelOf(property: AnimatableProperty): String = when (property) {
        AnimatableProperty.POSITION_X -> "Posisi X"
        AnimatableProperty.POSITION_Y -> "Posisi Y"
        AnimatableProperty.SCALE -> "Skala"
        AnimatableProperty.ROTATION -> "Rotasi"
        AnimatableProperty.OPACITY -> "Opacity"
        AnimatableProperty.VOLUME -> "Volume"
    }

    fun formatValue(property: AnimatableProperty, value: Float): String = when (property) {
        AnimatableProperty.POSITION_X,
        AnimatableProperty.POSITION_Y -> String.format(java.util.Locale.US, "%.0f", value)
        AnimatableProperty.SCALE -> String.format(java.util.Locale.US, "%.0f%%", value * 100f)
        AnimatableProperty.ROTATION -> String.format(java.util.Locale.US, "%.1f°", value)
        AnimatableProperty.OPACITY,
        AnimatableProperty.VOLUME -> String.format(java.util.Locale.US, "%.0f%%", value * 100f)
    }

    private fun coerce(property: AnimatableProperty, value: Float): Float {
        if (value.isNaN()) return valueOfDefault(property)
        val range = rangeOf(property)
        return value.coerceIn(range.start, range.endInclusive)
    }

    private fun valueOfDefault(property: AnimatableProperty): Float = when (property) {
        AnimatableProperty.POSITION_X,
        AnimatableProperty.POSITION_Y,
        AnimatableProperty.ROTATION -> 0f
        AnimatableProperty.SCALE,
        AnimatableProperty.OPACITY,
        AnimatableProperty.VOLUME -> 1f
    }
}
