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

/** UI language; the in-app copy table lives in `core/i18n/ZcutStrings`. */
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
    /** Local display name; the avatar is just its initial, nothing is uploaded. */
    val displayName: String = DEFAULT_DISPLAY_NAME,
    val exportResolution: Resolution = Resolution.FHD_1080,
    val exportFrameRate: Int = DEFAULT_FRAME_RATE,
    val exportAudioSampleRate: Int = DEFAULT_AUDIO_SAMPLE_RATE,
    /**
     * Template ids the user hearted on the template detail screen. Local only:
     * nothing is published, uploaded or synced.
     */
    val favoriteTemplateIds: Set<String> = emptySet()
) {
    companion object {
        const val DEFAULT_FRAME_RATE = 30
        const val DEFAULT_AUDIO_SAMPLE_RATE = 48_000
        val ALLOWED_FRAME_RATES = setOf(24, 30, 60)
        val ALLOWED_AUDIO_SAMPLE_RATES = setOf(44_100, 48_000)

        /** Keeps a corrupted favourites list from growing without bound. */
        const val MAX_FAVORITES = 200

        const val DEFAULT_DISPLAY_NAME = "Kreator lokal"
        const val MAX_DISPLAY_NAME = 40

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
            exportAudioSampleRate: Int?,
            favoriteTemplateIds: Set<String>? = null,
            displayName: String? = null
        ): AppPreferences = AppPreferences(
            onboardingSeen = onboardingSeen ?: false,
            themeMode = ThemeMode.fromStored(themeMode),
            language = AppLanguage.fromStored(language),
            displayName = sanitizeDisplayName(displayName),
            exportResolution = resolutionFromStored(exportResolution),
            exportFrameRate = exportFrameRate
                ?.takeIf { it in ALLOWED_FRAME_RATES }
                ?: DEFAULT_FRAME_RATE,
            exportAudioSampleRate = exportAudioSampleRate
                ?.takeIf { it in ALLOWED_AUDIO_SAMPLE_RATES }
                ?: DEFAULT_AUDIO_SAMPLE_RATE,
            favoriteTemplateIds = sanitizeFavorites(favoriteTemplateIds)
        )

        /** Trimmed, bounded and never blank, so the profile always has a label. */
        fun sanitizeDisplayName(raw: String?): String =
            raw?.trim()
                ?.take(MAX_DISPLAY_NAME)
                ?.ifBlank { DEFAULT_DISPLAY_NAME }
                ?: DEFAULT_DISPLAY_NAME

        private fun sanitizeFavorites(ids: Set<String>?): Set<String> =
            ids.orEmpty()
                .filter { it.isNotBlank() }
                .take(MAX_FAVORITES)
                .toSet()

        private fun resolutionFromStored(raw: String?): Resolution =
            Resolution.entries.firstOrNull { it.name == raw } ?: Resolution.FHD_1080
    }
}
