package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportDefaultsTest {

    @Test
    fun resolutionLabelsMatchTheSettingsCopy() {
        assertEquals("720p", ExportDefaults.resolutionLabel(Resolution.HD_720))
        assertEquals("1080p", ExportDefaults.resolutionLabel(Resolution.FHD_1080))
        assertEquals("1440p", ExportDefaults.resolutionLabel(Resolution.QHD_1440))
        assertEquals("4K", ExportDefaults.resolutionLabel(Resolution.UHD_2160))
    }

    @Test
    fun summaryCombinesResolutionAndFrameRate() {
        assertEquals("1080p · 30 fps", ExportDefaults.summary(Resolution.FHD_1080, 30))
        assertEquals("4K · 60 fps", ExportDefaults.summary(Resolution.UHD_2160, 60))
    }

    @Test
    fun audioLabelUsesTheStoredSampleRate() {
        assertEquals("AAC 48 kHz", ExportDefaults.audioLabel(48_000))
        assertEquals("AAC 44,1 kHz", ExportDefaults.audioLabel(44_100))
    }
}
