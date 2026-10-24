package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.model.Resolution

/** App-wide appearance preference. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    companion object {
        /** Unknown or corrupt stored values fall back to the product default. */
        fun fromStored(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

/**
 * UI language. Only the preference is stored in M1; translated resources land in
 * M28 (see PROGRESS.md), so the app copy stays Indonesian until then.
 */
enum class AppLanguage(val tag: String, val label: String) {
    ID("id", "Bahasa Indonesia"),
    EN("en", "English");

    companion object {
        fun fromStored(raw: String?): AppLanguage =
            entries.firstOrNull { it.name == raw } ?: ID
    }
}

/**
 * Everything the user can configure locally. No account, no billing, no
 * telemetry: this is the whole preference surface.
 */
data class AppPreferences(
    val onboardingSeen: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ID,
    val exportResolution: Resolution = Resolution.FHD_1080,
    val exportFrameRate: Int = DEFAULT_FRAME_RATE,
    val exportAudioSampleRate: Int = DEFAULT_AUDIO_SAMPLE_RATE
) {
    companion object {
        const val DEFAULT_FRAME_RATE = 30
        const val DEFAULT_AUDIO_SAMPLE_RATE = 48_000
        val ALLOWED_FRAME_RATES = setOf(24, 30, 60)
        val ALLOWED_AUDIO_SAMPLE_RATES = setOf(44_100, 48_000)

        /**
         * Builds preferences from raw stored values, dropping anything that is
         * missing or out of range. DataStore contents are a trust boundary: a
         * corrupted file or a downgrade must not crash the app.
         */
        fun fromStored(
            onboardingSeen: Boolean?,
            themeMode: String?,
            language: String?,
            exportResolution: String?,
            exportFrameRate: Int?,
            exportAudioSampleRate: Int?
        ): AppPreferences = AppPreferences(
            onboardingSeen = onboardingSeen ?: false,
            themeMode = ThemeMode.fromStored(themeMode),
            language = AppLanguage.fromStored(language),
            exportResolution = resolutionFromStored(exportResolution),
            exportFrameRate = exportFrameRate
                ?.takeIf { it in ALLOWED_FRAME_RATES }
                ?: DEFAULT_FRAME_RATE,
            exportAudioSampleRate = exportAudioSampleRate
                ?.takeIf { it in ALLOWED_AUDIO_SAMPLE_RATES }
                ?: DEFAULT_AUDIO_SAMPLE_RATE
        )

        private fun resolutionFromStored(raw: String?): Resolution =
            Resolution.entries.firstOrNull { it.name == raw } ?: Resolution.FHD_1080
    }
}
