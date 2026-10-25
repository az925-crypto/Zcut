package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.SpeedPoint
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure speed-curve math. A curve point's `atFraction` is normalised against the
 * *source* range, so a curve survives trims and splits: the editor remaps the
 * sub-range instead of guessing from the presentation length.
 *
 * Speed is linearly interpolated between points. Presentation duration is the
 * integral of `1 / speed` over the source range, solved in closed form per
 * segment (`S / (b - a) * ln(b / a)`), which is exactly what a frame-by-frame
 * retimer integrates. `sourceDurationForPresentationMs` inverts that integral;
 * it is the piece split/trim needs when a clip is retimed with a curve.
 */
object SpeedCurve {

    const val MIN_SPEED = 0.1f
    const val MAX_SPEED = 8f

    fun clampSpeed(value: Float): Float =
        if (value.isNaN()) 1f else value.coerceIn(MIN_SPEED, MAX_SPEED)

    /**
     * Sorts, clamps, de-duplicates and closes the curve at 0 and 1. A curve
     * without endpoints would otherwise make `speedAt` fall back to the scalar
     * speed and silently disagree with the rendered duration.
     */
    fun normalized(points: List<SpeedPoint>, fallbackSpeed: Float = 1f): List<SpeedPoint> {
        if (points.isEmpty()) return emptyList()
        val cleaned = points
            .map { SpeedPoint(it.atFraction.coerceIn(0f, 1f), clampSpeed(it.speed)) }
            .sortedBy { it.atFraction }
        val deduped = ArrayList<SpeedPoint>(cleaned.size + 2)
        for (point in cleaned) {
            if (deduped.isEmpty() ||
                point.atFraction > deduped.last().atFraction + FRACTION_EPSILON
            ) {
                deduped += point
            } else {
                deduped[deduped.lastIndex] = point
            }
        }
        if (deduped.isEmpty()) return emptyList()
        val first = deduped.first()
        if (first.atFraction > 0f) deduped.add(0, first.copy(atFraction = 0f))
        val last = deduped.last()
        if (last.atFraction < 1f) deduped.add(last.copy(atFraction = 1f))
        return deduped
    }

    fun speedAt(
        curve: List<SpeedPoint>,
        fraction: Float,
        fallbackSpeed: Float = 1f
    ): Float {
        val points = normalized(curve, fallbackSpeed)
        if (points.isEmpty()) return clampSpeed(fallbackSpeed)
        val progress = fraction.coerceIn(0f, 1f)
        if (progress <= points.first().atFraction) return points.first().speed
        if (progress >= points.last().atFraction) return points.last().speed
        val index = points.indexOfLast { it.atFraction <= progress }.coerceAtLeast(0)
        val left = points[index]
        val right = points.getOrNull(index + 1) ?: return left.speed
        val span = right.atFraction - left.atFraction
        if (span <= 0f) return right.speed
        val t = (progress - left.atFraction) / span
        return clampSpeed(left.speed + (right.speed - left.speed) * t)
    }

    /** Presentation length for a source range at [speed] / [curve]. */
    fun presentationDurationMs(
        sourceDurationMs: Long,
        speed: Float,
        curve: List<SpeedPoint>
    ): Long {
        val source = sourceDurationMs.coerceAtLeast(0L)
        if (source == 0L) return 0L
        val points = normalized(curve, speed)
        if (points.isEmpty()) {
            return (source / clampSpeed(speed).toDouble()).roundToLong().coerceAtLeast(1L)
        }
        var total = 0.0
        for (index in 0 until points.size - 1) {
            val left = points[index]
            val right = points[index + 1]
            val sourceSpan = (right.atFraction - left.atFraction).toDouble() * source
            if (sourceSpan <= 0.0) continue
            total += segmentPresentationMs(sourceSpan, left.speed, right.speed)
        }
        return total.roundToLong().coerceAtLeast(1L)
    }

    /**
     * How much source time is consumed by [presentationMs] of presentation.
     * Constant speed is linear; a curve is integrated in the same segment order
     * as [presentationDurationMs], so the two always agree at the endpoints.
     */
    fun sourceDurationForPresentationMs(
        sourceDurationMs: Long,
        speed: Float,
        curve: List<SpeedPoint>,
        presentationMs: Long
    ): Long {
        val source = sourceDurationMs.coerceAtLeast(0L)
        if (source == 0L || presentationMs <= 0L) return 0L
        val points = normalized(curve, speed)
        if (points.isEmpty()) {
            val scaled = presentationMs.toDouble() * clampSpeed(speed)
            return scaled.roundToLong().coerceIn(0L, source)
        }
        val totalPresentation = presentationDurationMs(source, speed, points)
        if (presentationMs >= totalPresentation) return source

        var elapsed = 0.0
        for (index in 0 until points.size - 1) {
            val left = points[index]
            val right = points[index + 1]
            val sourceSpan = (right.atFraction - left.atFraction).toDouble() * source
            if (sourceSpan <= 0.0) continue
            val segmentPresentation = segmentPresentationMs(
                sourceSpan,
                left.speed,
                right.speed
            )
            if (elapsed + segmentPresentation >= presentationMs) {
                val remaining = (presentationMs - elapsed).coerceAtLeast(0.0)
                val progress = if (segmentPresentation <= 0.0) {
                    0.0
                } else {
                    (remaining / segmentPresentation).coerceIn(0.0, 1.0)
                }
                val sourceProgress = sourceProgressFor(progress, left.speed, right.speed)
                val sourceFraction = left.atFraction +
                    (right.atFraction - left.atFraction) * sourceProgress
                return (sourceFraction * source).roundToLong().coerceIn(0L, source)
            }
            elapsed += segmentPresentation
        }
        return source
    }

    /**
     * Blends each interior point towards the average of its neighbours. The
     * endpoints stay fixed so the curve still spans the whole clip.
     */
    fun smoothed(points: List<SpeedPoint>, amount: Float): List<SpeedPoint> {
        val normalized = normalized(points)
        val strength = amount.coerceIn(0f, 1f)
        if (normalized.size < 3 || strength <= 0f) return normalized
        return normalized.mapIndexed { index, point ->
            if (index == 0 || index == normalized.lastIndex) {
                point
            } else {
                val previous = normalized[index - 1].speed
                val next = normalized[index + 1].speed
                val average = (previous + next) / 2f
                point.copy(
                    speed = clampSpeed(point.speed + (average - point.speed) * strength)
                )
            }
        }
    }

    /**
     * Cuts the part of [curve] covering `[fromFraction, toFraction]` and remaps
     * it to 0..1. Used by trim and split so each half keeps the correct shape.
     */
    fun subCurve(
        curve: List<SpeedPoint>,
        fromFraction: Float,
        toFraction: Float,
        fallbackSpeed: Float = 1f
    ): List<SpeedPoint> {
        val points = normalized(curve, fallbackSpeed)
        if (points.isEmpty()) return emptyList()
        val from = fromFraction.coerceIn(0f, 1f)
        val to = toFraction.coerceIn(0f, 1f)
        if (to - from <= FRACTION_EPSILON) return emptyList()
        val span = to - from
        val result = ArrayList<SpeedPoint>(points.size + 2)
        result += SpeedPoint(0f, speedAt(points, from, fallbackSpeed))
        points.forEach { point ->
            if (point.atFraction > from + FRACTION_EPSILON &&
                point.atFraction < to - FRACTION_EPSILON
            ) {
                result += point.copy(atFraction = (point.atFraction - from) / span)
            }
        }
        result += SpeedPoint(1f, speedAt(points, to, fallbackSpeed))
        return normalized(result, fallbackSpeed)
    }

    private fun segmentPresentationMs(
        sourceSpanMs: Double,
        fromSpeed: Float,
        toSpeed: Float
    ): Double {
        val a = fromSpeed.toDouble().coerceAtLeast(MIN_SPEED.toDouble())
        val b = toSpeed.toDouble().coerceAtLeast(MIN_SPEED.toDouble())
        val delta = b - a
        return if (abs(delta) <= SPEED_EPSILON) {
            sourceSpanMs / a
        } else {
            sourceSpanMs * ln(b / a) / delta
        }
    }

    /**
     * Fraction of the source span consumed by [progress] (0..1) of the segment's
     * presentation. Inverts `p = S / (b - a) * ln(1 + (b - a) f / a)`.
     */
    private fun sourceProgressFor(progress: Double, fromSpeed: Float, toSpeed: Float): Double {
        val a = fromSpeed.toDouble().coerceAtLeast(MIN_SPEED.toDouble())
        val b = toSpeed.toDouble().coerceAtLeast(MIN_SPEED.toDouble())
        val delta = b - a
        return if (abs(delta) <= SPEED_EPSILON) {
            progress
        } else {
            val ratio = b / a
            ((ratio.pow(progress) - 1.0) / (ratio - 1.0)).coerceIn(0.0, 1.0)
        }
    }

    private const val FRACTION_EPSILON = 0.0001f
    private const val SPEED_EPSILON = 0.000001
}
