package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ExportPreset
import com.zaaam.zcut.core.model.ExportQuality
import com.zaaam.zcut.core.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPlanTest {

    @Test
    fun safeNameStripsPathSeparators() {
        val name = ExportPlan.safeName("../../etc/passwd")

        assertFalse("path separator survived: $name", name.contains('/'))
        assertFalse(name.contains(".."))
        assertTrue(name.isNotBlank())
    }

    @Test
    fun safeNameFallsBackWhenEverythingIsStripped() {
        assertEquals("zcut-export", ExportPlan.safeName("   ...   "))
        assertEquals("zcut-export", ExportPlan.safeName(""))
    }

    @Test
    fun fileNameKeepsTheDraftNameAndAddsTheStamp() {
        assertEquals(
            "Midnight-in-Tokyo-1234.mp4",
            ExportPlan.fileName("Midnight in Tokyo", 1234L)
        )
        assertEquals(
            "Midnight-in-Tokyo-1234-cover.jpg",
            ExportPlan.coverFileName("Midnight in Tokyo", 1234L)
        )
    }

    @Test
    fun aNegativeTimestampDoesNotEscapeTheName() {
        assertEquals("Draft-0.mp4", ExportPlan.fileName("Draft", -5L))
    }

    @Test
    fun estimatedSizeUsesVideoPlusAudioBitrate() {
        val preset = ExportPreset(
            resolution = Resolution.FHD_1080,
            frameRate = 30,
            videoBitrate = 12_000_000,
            audioBitrate = 192_000,
            quality = ExportQuality.HIGH
        )

        // 24 s of 12.192 Mbps is roughly 36.6 MB.
        val size = ExportPlan.estimatedSizeBytes(preset, 24_000L)
        assertEquals(36_576_000L, size)
        assertEquals("36.6 MB", ExportPlan.sizeLabel(size))
    }

    @Test
    fun sizeLabelSwitchesUnits() {
        assertEquals("512 B", ExportPlan.sizeLabel(512L))
        assertEquals("2 KB", ExportPlan.sizeLabel(2_048L))
        assertEquals("1.5 MB", ExportPlan.sizeLabel(1_500_000L))
        assertEquals("2.0 GB", ExportPlan.sizeLabel(2_000_000_000L))
    }

    @Test
    fun durationLabelRoundsToTheNearestSecond() {
        assertEquals("00:00", ExportPlan.durationLabel(400L))
        assertEquals("00:24", ExportPlan.durationLabel(24_000L))
        assertEquals("00:25", ExportPlan.durationLabel(24_600L))
        assertEquals("01:30", ExportPlan.durationLabel(90_000L))
    }
}
