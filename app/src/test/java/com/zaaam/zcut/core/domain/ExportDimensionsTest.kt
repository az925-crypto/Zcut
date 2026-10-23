package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportDimensionsTest {

    @Test
    fun portraitUsesShortEdgeAsWidth() {
        val size = ExportDimensions.forAspectRatio(
            AspectRatio.PORTRAIT_9_16,
            ExportPreset(resolution = Resolution.FHD_1080)
        )
        assertEquals(1080 to 1920, size)
    }

    @Test
    fun landscapeUsesShortEdgeAsHeight() {
        val size = ExportDimensions.forAspectRatio(
            AspectRatio.LANDSCAPE_16_9,
            ExportPreset(resolution = Resolution.FHD_1080)
        )
        assertEquals(1920 to 1080, size)
    }

    @Test
    fun squareIsSquare() {
        val size = ExportDimensions.forAspectRatio(
            AspectRatio.SQUARE_1_1,
            ExportPreset(resolution = Resolution.HD_720)
        )
        assertEquals(720 to 720, size)
    }
}
