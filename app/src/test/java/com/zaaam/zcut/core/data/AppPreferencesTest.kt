package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppPreferencesTest {

    @Test
    fun defaultsApplyWhenNothingIsStored() {
        val preferences = AppPreferences.fromStored(
            onboardingSeen = null,
            themeMode = null,
            language = null,
            exportResolution = null,
            exportFrameRate = null,
            exportAudioSampleRate = null
        )

        assertFalse(preferences.onboardingSeen)
        assertEquals(ThemeMode.DARK, preferences.themeMode)
        assertEquals(AppLanguage.ID, preferences.language)
        assertEquals(Resolution.FHD_1080, preferences.exportResolution)
        assertEquals(30, preferences.exportFrameRate)
        assertEquals(48_000, preferences.exportAudioSampleRate)
    }

    @Test
    fun corruptStoredValuesFallBackInsteadOfCrashing() {
        val preferences = AppPreferences.fromStored(
            onboardingSeen = true,
            themeMode = "PURPLE",
            language = "xx",
            exportResolution = "8K",
            exportFrameRate = 12,
            exportAudioSampleRate = 96_000
        )

        assertEquals(ThemeMode.DARK, preferences.themeMode)
        assertEquals(AppLanguage.ID, preferences.language)
        assertEquals(Resolution.FHD_1080, preferences.exportResolution)
        assertEquals(30, preferences.exportFrameRate)
        assertEquals(48_000, preferences.exportAudioSampleRate)
    }

    @Test
    fun validStoredValuesAreKept() {
        val preferences = AppPreferences.fromStored(
            onboardingSeen = true,
            themeMode = ThemeMode.LIGHT.name,
            language = AppLanguage.EN.name,
            exportResolution = Resolution.UHD_2160.name,
            exportFrameRate = 60,
            exportAudioSampleRate = 44_100
        )

        assertEquals(true, preferences.onboardingSeen)
        assertEquals(ThemeMode.LIGHT, preferences.themeMode)
        assertEquals(AppLanguage.EN, preferences.language)
        assertEquals(Resolution.UHD_2160, preferences.exportResolution)
        assertEquals(60, preferences.exportFrameRate)
        assertEquals(44_100, preferences.exportAudioSampleRate)
    }

    @Test
    fun onlySupportedFrameRatesAreAccepted() {
        listOf(24, 30, 60).forEach { rate ->
            assertEquals(
                rate,
                AppPreferences.fromStored(null, null, null, null, rate, null).exportFrameRate
            )
        }
        listOf(1, 25, 120, -30).forEach { rate ->
            assertEquals(
                30,
                AppPreferences.fromStored(null, null, null, null, rate, null).exportFrameRate
            )
        }
    }
}
