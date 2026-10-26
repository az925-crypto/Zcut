package com.zaaam.zcut.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaaam.zcut.core.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zcut_preferences"
)

/**
 * DataStore-backed preferences. Kept separate from the project document so a
 * corrupt draft payload can never wipe the user's settings (and vice versa).
 */
class DataStorePreferencesRepository(
    private val context: Context
) : PreferencesRepository {

    private object Keys {
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val themeMode = stringPreferencesKey("theme_mode")
        val language = stringPreferencesKey("language")
        val exportResolution = stringPreferencesKey("export_resolution")
        val exportFrameRate = intPreferencesKey("export_frame_rate")
        val exportAudioSampleRate = intPreferencesKey("export_audio_sample_rate")
        val favoriteTemplates = stringSetPreferencesKey("favorite_templates")
    }

    override val preferences: Flow<AppPreferences> = context.appPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { stored ->
            AppPreferences.fromStored(
                onboardingSeen = stored[Keys.onboardingSeen],
                themeMode = stored[Keys.themeMode],
                language = stored[Keys.language],
                exportResolution = stored[Keys.exportResolution],
                exportFrameRate = stored[Keys.exportFrameRate],
                exportAudioSampleRate = stored[Keys.exportAudioSampleRate],
                favoriteTemplateIds = stored[Keys.favoriteTemplates]
            )
        }
        .flowOn(Dispatchers.IO)

    override suspend fun setOnboardingSeen(seen: Boolean) = edit { it[Keys.onboardingSeen] = seen }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    override suspend fun setLanguage(language: AppLanguage) = edit {
        it[Keys.language] = language.name
    }

    override suspend fun setExportResolution(resolution: Resolution) = edit {
        it[Keys.exportResolution] = resolution.name
    }

    override suspend fun setExportFrameRate(frameRate: Int) = edit {
        if (frameRate in AppPreferences.ALLOWED_FRAME_RATES) {
            it[Keys.exportFrameRate] = frameRate
        }
    }

    override suspend fun setExportAudioSampleRate(sampleRate: Int) = edit {
        if (sampleRate in AppPreferences.ALLOWED_AUDIO_SAMPLE_RATES) {
            it[Keys.exportAudioSampleRate] = sampleRate
        }
    }

    override suspend fun setTemplateFavorite(templateId: String, favorite: Boolean) = edit {
        if (templateId.isBlank()) return@edit
        val current = it[Keys.favoriteTemplates].orEmpty()
        it[Keys.favoriteTemplates] = when {
            favorite -> (current + templateId).take(AppPreferences.MAX_FAVORITES).toSet()
            else -> current - templateId
        }
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        withContext(Dispatchers.IO) {
            context.appPreferencesDataStore.edit { block(it) }
        }
    }
}
