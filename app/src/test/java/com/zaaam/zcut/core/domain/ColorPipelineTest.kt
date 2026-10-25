package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.HslBand
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPipelineTest {

    private fun pixel(r: Float, g: Float, b: Float) = floatArrayOf(r, g, b, 1f)

    @Test
    fun neutralAdjustmentsAreIdentity() {
        assertArrayEquals(
            ColorPipeline.identity(),
            ColorPipeline.adjustMatrix(ColorAdjustments()),
            0.0001f
        )
    }

    @Test
    fun noFilterIsIdentity() {
        assertArrayEquals(
            ColorPipeline.identity(),
            ColorPipeline.filterMatrix(null),
            0.0001f
        )
        assertArrayEquals(
            ColorPipeline.identity(),
            ColorPipeline.filterMatrix(FilterConfig("unknown")),
            0.0001f
        )
    }

    @Test
    fun brightnessShiftsChannelsEqually() {
        val matrix = ColorPipeline.adjustMatrix(ColorAdjustments(brightness = 1f))
        val out = ColorPipeline.transform(matrix, pixel(0f, 0f, 0f))

        assertEquals(0.5f, out[0], 0.001f)
        assertEquals(0.5f, out[1], 0.001f)
        assertEquals(0.5f, out[2], 0.001f)
    }

    @Test
    fun contrastPivotsAroundMidGrey() {
        val matrix = ColorPipeline.adjustMatrix(ColorAdjustments(contrast = 1f))
        val mid = ColorPipeline.transform(matrix, pixel(0.5f, 0.5f, 0.5f))
        val dark = ColorPipeline.transform(matrix, pixel(0.25f, 0.25f, 0.25f))

        assertEquals(0.5f, mid[0], 0.001f)
        assertEquals(0f, dark[0], 0.001f)
    }

    @Test
    fun negativeSaturationCollapsesToLuma() {
        val matrix = ColorPipeline.adjustMatrix(ColorAdjustments(saturation = -1f))
        val out = ColorPipeline.transform(matrix, pixel(1f, 0f, 0f))

        assertEquals(0.2126f, out[0], 0.001f)
        assertEquals(0.2126f, out[1], 0.001f)
        assertEquals(0.2126f, out[2], 0.001f)
    }

    @Test
    fun intensityBlendsPresetTowardsIdentity() {
        val half = ColorPipeline.filterMatrix(FilterConfig("mono", intensity = 0.5f))
        val out = ColorPipeline.transform(half, pixel(1f, 0f, 0f))

        // Mono turns red into ~0.184 grey; half intensity sits halfway back.
        assertEquals(0.5919f, out[0], 0.001f)
        assertEquals(0.0919f, out[1], 0.001f)
    }

    @Test
    fun filterRunsBeforeAdjust() {
        val filter = ColorPipeline.filterMatrix(FilterConfig("mono", intensity = 1f))
        val adjust = ColorPipeline.adjustMatrix(ColorAdjustments(brightness = 1f))
        val combined = ColorPipeline.buildMatrix(
            FilterConfig("mono"),
            ColorAdjustments(brightness = 1f)
        )

        assertArrayEquals(ColorPipeline.pipe(filter, adjust), combined, 0.0001f)
        // Order matters: desaturate first, then add the +0.5 brightness offset.
        val out = ColorPipeline.transform(combined, pixel(1f, 0f, 0f))
        assertEquals(0.6839f, out[0], 0.001f)
    }

    @Test
    fun hslBandsDoNotChangeTheMatrix() {
        val withBands = ColorPipeline.adjustMatrix(
            ColorAdjustments(hslBands = listOf(HslBand(hue = 120f, saturation = 1f)))
        )

        assertArrayEquals(ColorPipeline.identity(), withBands, 0.0001f)
    }

    @Test
    fun presetsAreUniqueAndOriginalIsIdentity() {
        val ids = ColorPipeline.presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(8, ids.size)
        assertArrayEquals(
            ColorPipeline.identity(),
            ColorPipeline.preset("original")!!.matrix,
            0.0001f
        )
        assertNotNull(ColorPipeline.preset("tokyo"))
    }

    @Test
    fun monoPresetDesaturatesCompletely() {
        val matrix = ColorPipeline.filterMatrix(FilterConfig("mono", intensity = 1f))
        val out = ColorPipeline.transform(matrix, pixel(1f, 0f, 0f))

        assertEquals(out[0], out[1], 0.001f)
        assertEquals(out[1], out[2], 0.001f)
    }

    @Test
    fun temperatureWarmsRedAndCoolsBlue() {
        val warm = ColorPipeline.adjustMatrix(ColorAdjustments(temperature = 1f))
        val out = ColorPipeline.transform(warm, pixel(0.5f, 0.5f, 0.5f))

        assertTrue(out[0] > 0.5f)
        assertTrue(out[2] < 0.5f)
    }

    @Test
    fun multiplyConcatenatesOffsets() {
        val combined = ColorPipeline.multiply(
            ColorPipeline.brightnessOffset(0.1f),
            ColorPipeline.brightnessOffset(0.2f)
        )
        val out = ColorPipeline.transform(combined, pixel(0f, 0f, 0f))

        assertEquals(0.3f, out[0], 0.001f)
    }

    @Test
    fun upsertBandReplacesSameHueAndKeepsOrder() {
        val first = ColorPipeline.upsertBand(
            ColorAdjustments(),
            HslBand(hue = 120f, saturation = 0.3f)
        )
        val added = ColorPipeline.upsertBand(first, HslBand(hue = 0f, saturation = 0.2f))
        val replaced = ColorPipeline.upsertBand(
            added,
            HslBand(hue = 120f, saturation = -0.5f)
        )

        assertEquals(2, replaced.hslBands.size)
        assertEquals(0f, replaced.hslBands.first().hue, 0.001f)
        assertEquals(-0.5f, replaced.hslBands.last().saturation, 0.001f)
    }

    @Test
    fun bandAtFallsBackToNeutralBand() {
        val band = ColorPipeline.bandAt(ColorAdjustments(), 240f)

        assertEquals(240f, band.hue, 0.001f)
        assertEquals(0f, band.saturation, 0.001f)
        assertEquals(0f, band.luminance, 0.001f)
    }
}
