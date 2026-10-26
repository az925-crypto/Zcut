package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.model.Resolution
import kotlinx.coroutines.flow.Flow

/**
 * Local preferences only. Implementations never talk to a server, so every
 * setting here is readable and writable offline.
 */
interface PreferencesRepository {
    val preferences: Flow<AppPreferences>

    suspend fun setOnboardingSeen(seen: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setExportResolution(resolution: Resolution)
    suspend fun setExportFrameRate(frameRate: Int)
    suspend fun setExportAudioSampleRate(sampleRate: Int)
    suspend fun setTemplateFavorite(templateId: String, favorite: Boolean)
}
