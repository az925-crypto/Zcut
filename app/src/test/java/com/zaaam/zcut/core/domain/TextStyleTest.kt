package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextStyleTest {

    private fun config(
        preset: TextStylePreset = TextStylePreset.NONE,
        strokeWidth: Float = 0f,
        shadow: Boolean = false,
        colorArgb: Long = 0xFFFFFFFF,
        gradientColorArgb: Long? = null
    ) = TextConfig(
        text = "Halo",
        stylePreset = preset,
        strokeWidth = strokeWidth,
        shadow = shadow,
        colorArgb = colorArgb,
        gradientColorArgb = gradientColorArgb
    )

    @Test
    fun noneKeepsLegacyStrokeAndShadow() {
        val resolved = TextStyleResolver.resolve(config(strokeWidth = 3f, shadow = true))

        assertEquals(3f, resolved.strokeWidth, 0.001f)
        assertTrue(resolved.shadow)
        assertNull(resolved.gradientEndArgb)
        assertEquals(false, resolved.bold)
    }

    @Test
    fun outlineForcesAVisibleStroke() {
        val resolved = TextStyleResolver.resolve(config(preset = TextStylePreset.OUTLINE))

        assertTrue(resolved.strokeWidth >= 2f)
    }

    @Test
    fun neonUsesTheFillAsStrokeAndAddsShadow() {
        val resolved = TextStyleResolver.resolve(
            config(preset = TextStylePreset.NEON, colorArgb = 0xFFC9FF4A)
        )

        assertEquals(0xFFC9FF4A, resolved.strokeColorArgb)
        assertTrue(resolved.shadow)
        assertTrue(resolved.strokeWidth >= 2f)
    }

    @Test
    fun gradientFallsBackToCyan() {
        val resolved = TextStyleResolver.resolve(config(preset = TextStylePreset.GRADIENT))

        assertEquals(0xFF59D9FF, resolved.gradientEndArgb)
    }

    @Test
    fun gradientKeepsExplicitEndColour() {
        val resolved = TextStyleResolver.resolve(
            config(preset = TextStylePreset.GRADIENT, gradientColorArgb = 0xFFFF6B4A)
        )

        assertEquals(0xFFFF6B4A, resolved.gradientEndArgb)
    }

    @Test
    fun boldPresetSetsBold() {
        assertTrue(TextStyleResolver.resolve(config(preset = TextStylePreset.BOLD)).bold)
    }

    @Test
    fun fontSizeScalesFromTheReferenceFrame() {
        val size = TextLayout.fontSizeDp(TextConfig(text = "x", fontSizeSp = 42f), 176f)

        assertEquals(28.43f, size, 0.1f)
    }

    @Test
    fun invalidFrameHeightIsSafe() {
        assertEquals(1f, TextLayout.scale(0f), 0.001f)
        assertEquals(1f, TextLayout.scale(Float.NaN), 0.001f)
        assertEquals(1f, TextLayout.scale(-10f), 0.001f)
    }

    @Test
    fun letterSpacingIsAFractionOfFontSize() {
        val config = TextConfig(text = "x", fontSizeSp = 42f, letterSpacingEm = 0.12f)

        assertEquals(
            TextLayout.fontSizeDp(config, 176f) * 0.12f,
            TextLayout.letterSpacingDp(config, 176f),
            0.01f
        )
    }

    @Test
    fun fontAliasesCanonicalise() {
        assertEquals(TextFonts.SANS, TextFonts.canonical("Display"))
        assertEquals(TextFonts.MONO, TextFonts.canonical("mono"))
        assertEquals(TextFonts.HAND, TextFonts.canonical("cursive"))
        assertEquals(TextFonts.SANS, TextFonts.canonical("unknown-font"))
        assertEquals("Display", TextFonts.label("sans"))
        assertEquals("Hand", TextFonts.label("hand"))
    }
}
