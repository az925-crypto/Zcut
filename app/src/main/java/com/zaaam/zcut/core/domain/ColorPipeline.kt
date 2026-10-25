package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.HslBand
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure colour math shared by the filter/adjust panels and (later) export.
 *
 * A transform is a 4×5 row-major matrix in the same layout Compose's
 * `ColorMatrix` expects, so the UI can hand it straight to a `ColorFilter`.
 * Steps compose left to right like CSS `filter:` chains: the first entry runs
 * first.
 *
 * ponytail: vignette and HSL bands are spatial/band-selective and cannot be a
 * 4×5 matrix, so they are stored for export and skipped in the live preview.
 */
object ColorPipeline {

    const val CHANNELS = 20

    data class Preset(val id: String, val label: String, val matrix: FloatArray)

    val presets: List<Preset> = listOf(
        Preset("original", "Original", identity()),
        Preset(
            "tokyo",
            "Tokyo",
            pipe(contrast(1.2f), saturation(1.25f), hueRotation(-8f))
        ),
        Preset(
            "film",
            "Film",
            pipe(sepia(0.28f), contrast(1.15f), saturation(0.85f))
        ),
        Preset("mono", "Mono", pipe(grayscale(1f), contrast(1.1f))),
        Preset(
            "warm",
            "Warm",
            pipe(sepia(0.18f), saturation(1.3f), brightnessScale(1.05f))
        ),
        Preset("cool", "Cool", pipe(hueRotation(18f), saturation(1.1f))),
        Preset(
            "neon",
            "Neon",
            pipe(saturation(1.8f), contrast(1.3f), hueRotation(-18f))
        ),
        Preset(
            "fade",
            "Fade",
            pipe(contrast(0.85f), brightnessScale(1.12f), saturation(0.8f))
        )
    )

    fun preset(id: String?): Preset? = presets.firstOrNull { it.id == id }

    /** Replaces the band with the same hue, otherwise appends, keeping hue order. */
    fun upsertBand(color: ColorAdjustments, band: HslBand): ColorAdjustments {
        val bands = color.hslBands
            .filterNot { kotlin.math.abs(it.hue - band.hue) < BAND_EPSILON }
            .plus(band)
            .sortedBy { it.hue }
        return color.copy(hslBands = bands)
    }

    fun bandAt(color: ColorAdjustments, hue: Float): HslBand =
        color.hslBands.firstOrNull { kotlin.math.abs(it.hue - hue) < BAND_EPSILON }
            ?: HslBand(hue = hue)

    /** Identity when the clip has no filter. */
    fun filterMatrix(filter: FilterConfig?): FloatArray {
        if (filter == null) return identity()
        val preset = preset(filter.id) ?: return identity()
        return blend(identity(), preset.matrix, filter.intensity.coerceIn(0f, 1f))
    }

    /** Filter first, then the numeric adjustments, matching the render order. */
    fun buildMatrix(filter: FilterConfig?, color: ColorAdjustments): FloatArray =
        pipe(filterMatrix(filter), adjustMatrix(color))

    fun adjustMatrix(color: ColorAdjustments): FloatArray = pipe(
        brightnessOffset(color.brightness * 0.5f),
        contrast(1f + color.contrast.coerceIn(-1f, 1f)),
        saturation(1f + color.saturation.coerceIn(-1f, 1f)),
        temperature(color.temperature.coerceIn(-1f, 1f)),
        tint(color.tint.coerceIn(-1f, 1f)),
        tone(color.highlights.coerceIn(-1f, 1f), color.shadows.coerceIn(-1f, 1f))
    )

    /** Applies the matrix to one RGBA pixel; useful for tests and preview math. */
    fun transform(matrix: FloatArray, rgba: FloatArray): FloatArray {
        val out = FloatArray(4)
        for (row in 0 until 4) {
            var value = matrix[row * 5 + 4]
            for (column in 0 until 4) {
                value += matrix[row * 5 + column] * rgba[column]
            }
            out[row] = value
        }
        return out
    }

    // --- building blocks ---------------------------------------------------

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** `steps[0]` runs first, `steps.last` runs last. */
    fun pipe(vararg steps: FloatArray): FloatArray {
        var acc = identity()
        steps.forEach { step -> acc = multiply(step, acc) }
        return acc
    }

    /** `outer ∘ inner`: inner runs first, then outer. */
    fun multiply(outer: FloatArray, inner: FloatArray): FloatArray {
        val result = FloatArray(CHANNELS)
        for (row in 0 until 4) {
            for (column in 0 until 4) {
                var value = 0f
                for (k in 0 until 4) {
                    value += outer[row * 5 + k] * inner[k * 5 + column]
                }
                result[row * 5 + column] = value
            }
            var offset = outer[row * 5 + 4]
            for (k in 0 until 4) {
                offset += outer[row * 5 + k] * inner[k * 5 + 4]
            }
            result[row * 5 + 4] = offset
        }
        return result
    }

    /** `from` when amount is 0, `to` when amount is 1. */
    fun blend(from: FloatArray, to: FloatArray, amount: Float): FloatArray {
        val t = amount.coerceIn(0f, 1f)
        return FloatArray(CHANNELS) { index -> from[index] + (to[index] - from[index]) * t }
    }

    fun brightnessScale(scale: Float): FloatArray = floatArrayOf(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun brightnessOffset(offset: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, offset,
        0f, 1f, 0f, 0f, offset,
        0f, 0f, 1f, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )

    /** Pivot at 0.5 so mid grey stays mid grey. */
    fun contrast(scale: Float): FloatArray = floatArrayOf(
        scale, 0f, 0f, 0f, 0.5f * (1f - scale),
        0f, scale, 0f, 0f, 0.5f * (1f - scale),
        0f, 0f, scale, 0f, 0.5f * (1f - scale),
        0f, 0f, 0f, 1f, 0f
    )

    fun saturation(scale: Float): FloatArray {
        val s = scale
        val inverse = 1f - s
        val r = LUM_R * inverse
        val g = LUM_G * inverse
        val b = LUM_B * inverse
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun grayscale(amount: Float): FloatArray = blend(identity(), saturation(0f), amount)

    fun sepia(amount: Float): FloatArray = blend(
        identity(),
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        amount
    )

    fun hueRotation(degrees: Float): FloatArray {
        val radians = Math.toRadians(degrees.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()
        return floatArrayOf(
            0.213f + cos * 0.787f - sin * 0.213f,
            0.715f - cos * 0.715f - sin * 0.715f,
            0.072f - cos * 0.072f + sin * 0.928f,
            0f, 0f,
            0.213f - cos * 0.213f + sin * 0.143f,
            0.715f + cos * 0.285f + sin * 0.140f,
            0.072f - cos * 0.072f - sin * 0.283f,
            0f, 0f,
            0.213f - cos * 0.213f - sin * 0.787f,
            0.715f - cos * 0.715f + sin * 0.715f,
            0.072f + cos * 0.928f + sin * 0.072f,
            0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Warm/cool split: red up and blue down for positive temperature. */
    fun temperature(value: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, value * 0.1f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, -value * 0.1f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Green→magenta axis. */
    fun tint(value: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, value * 0.05f,
        0f, 1f, 0f, 0f, -value * 0.1f,
        0f, 0f, 1f, 0f, value * 0.05f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Highlights scale the gain, shadows lift or crush the floor. */
    fun tone(highlights: Float, shadows: Float): FloatArray {
        val gain = 1f + highlights * 0.4f
        val floor = shadows * 0.15f
        return floatArrayOf(
            gain, 0f, 0f, 0f, floor,
            0f, gain, 0f, 0f, floor,
            0f, 0f, gain, 0f, floor,
            0f, 0f, 0f, 1f, 0f
        )
    }

    private const val LUM_R = 0.2126f
    private const val LUM_G = 0.7152f
    private const val LUM_B = 0.0722f
    private const val BAND_EPSILON = 0.001f
}
