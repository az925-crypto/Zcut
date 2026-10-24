package com.zaaam.zcut.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.AppLanguage
import com.zaaam.zcut.core.data.AppStorage
import com.zaaam.zcut.core.data.PreferencesRepository
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.data.ThemeMode
import com.zaaam.zcut.core.model.Resolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    repository: ProjectRepository,
    private val storage: AppStorage
) : ViewModel() {

    private val storageBytes = MutableStateFlow(0L)
    private val clearingCache = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        repository.projects,
        storageBytes,
        clearingCache
    ) { preferences, projects, bytes, isClearing ->
        SettingsUiState(
            isLoading = false,
            themeMode = preferences.themeMode,
            language = preferences.language,
            resolution = preferences.exportResolution,
            frameRate = preferences.exportFrameRate,
            audioSampleRate = preferences.exportAudioSampleRate,
            storageBytes = bytes,
            draftCount = projects.size,
            isClearingCache = isClearing
        )
    }
        .catch { throwable ->
            emit(
                SettingsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Gagal memuat pengaturan"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun refreshStorage() {
        viewModelScope.launch {
            storageBytes.value = runCatching { storage.usedBytes() }.getOrDefault(0L)
        }
    }

    fun setThemeMode(mode: ThemeMode) = write { preferencesRepository.setThemeMode(mode) }

    fun setLanguage(language: AppLanguage) = write { preferencesRepository.setLanguage(language) }

    fun setResolution(resolution: Resolution) = write {
        preferencesRepository.setExportResolution(resolution)
    }

    fun setFrameRate(frameRate: Int) = write {
        preferencesRepository.setExportFrameRate(frameRate)
    }

    fun setAudioSampleRate(sampleRate: Int) = write {
        preferencesRepository.setExportAudioSampleRate(sampleRate)
    }

    fun clearCache() {
        if (clearingCache.value) return
        clearingCache.value = true
        viewModelScope.launch {
            try {
                storageBytes.value = runCatching { storage.clearCache() }.getOrDefault(0L)
            } finally {
                clearingCache.value = false
            }
        }
    }

    /**
     * A failed write (full disk, I/O error) leaves the stored value untouched, so
     * the UI simply keeps showing what is actually persisted. Crashing the
     * settings screen would be worse; surfacing the error is on the backlog.
     */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}
