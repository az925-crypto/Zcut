package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MattingEditorTest {

    @Test
    fun normalizeClampsAnUntrustedConfig() {
        val hostile = MattingConfig(
            blur = 4f,
            edgeRefine = -2f,
            shadow = Float.NaN,
            imageAssetId = "   ",
            aiPrompt = "  halo  "
        )

        val safe = MattingEditor.normalize(hostile)

        assertEquals(1f, safe.blur, 0.0001f)
        assertEquals(0f, safe.edgeRefine, 0.0001f)
        assertEquals(0f, safe.shadow, 0.0001f)
        assertNull(safe.imageAssetId)
        assertEquals("halo", safe.aiPrompt)
    }

    @Test
    fun promptIsTruncatedSoADraftCannotCarryUnboundedText() {
        val long = "a".repeat(500)

        assertEquals(200, MattingEditor.normalize(MattingConfig(aiPrompt = long)).aiPrompt.length)
    }

    @Test
    fun matteAlphaDropsLowConfidenceAndStretchesTheRest() {
        // Soft edge: threshold 0, so alpha follows confidence.
        assertEquals(0f, MattingEditor.matteAlpha(0f, 0f), 0.0001f)
        assertEquals(0.5f, MattingEditor.matteAlpha(0.5f, 0f), 0.0001f)

        // Hard edge: threshold 0.6, everything below is gone.
        assertEquals(0f, MattingEditor.matteAlpha(0.6f, 1f), 0.0001f)
        assertEquals(1f, MattingEditor.matteAlpha(1f, 1f), 0.0001f)
        assertEquals(0.5f, MattingEditor.matteAlpha(0.8f, 1f), 0.0001f)
    }

    @Test
    fun subjectConfidenceIsNullWhenNothingIsForeground() {
        assertNull(MattingEditor.subjectConfidencePercent(floatArrayOf(0.1f, 0.2f, 0.4f)))
        assertNull(MattingEditor.subjectConfidencePercent(FloatArray(0)))

        assertEquals(
            "TIDAK ADA SUBYEK",
            MattingEditor.subjectLabel(MattingEditor.subjectConfidencePercent(floatArrayOf(0f)))
        )
    }

    @Test
    fun subjectConfidenceAveragesOnlyForegroundPixels() {
        // 0.8 and 1.0 are foreground; the two background values must not pull the
        // average down or a mostly-empty frame would look uncertain.
        val percent = MattingEditor.subjectConfidencePercent(
            floatArrayOf(0.8f, 1.0f, 0.1f, 0.0f)
        )

        assertEquals(90, percent)
        assertEquals("SUBYEK · 90%", MattingEditor.subjectLabel(percent))
    }

    @Test
    fun onlyTheGenerativeBackgroundNeedsABackend() {
        assertTrue(MattingEditor.requiresBackend(MattingBackground.AI))
        assertFalse(MattingEditor.requiresBackend(MattingBackground.BLUR))
        assertFalse(MattingEditor.requiresBackend(MattingBackground.COLOR))
        assertFalse(MattingEditor.requiresBackend(MattingBackground.TRANSPARENT))
        assertFalse(MattingEditor.requiresBackend(MattingBackground.IMAGE))
    }

    @Test
    fun summaryDescribesTheChosenBackdrop() {
        assertEquals(
            "Latar diburamkan 50%",
            MattingEditor.summary(MattingConfig(background = MattingBackground.BLUR, blur = 0.5f))
        )
        assertEquals(
            "Latar diganti warna #1D4ED8",
            MattingEditor.summary(
                MattingConfig(background = MattingBackground.COLOR, colorArgb = 0xFF1D4ED8)
            )
        )
        assertEquals(
            "Gambar latar belum dipilih",
            MattingEditor.summary(MattingConfig(background = MattingBackground.IMAGE))
        )
        assertEquals(
            "Latar AI · butuh backend",
            MattingEditor.summary(MattingConfig(background = MattingBackground.AI))
        )
    }
}
