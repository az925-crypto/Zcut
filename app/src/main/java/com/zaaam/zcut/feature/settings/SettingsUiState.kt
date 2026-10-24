package com.zaaam.zcut.feature.settings

import com.zaaam.zcut.core.data.AppLanguage
import com.zaaam.zcut.core.data.ThemeMode
import com.zaaam.zcut.core.model.Resolution

data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ID,
    val resolution: Resolution = Resolution.FHD_1080,
    val frameRate: Int = 30,
    val audioSampleRate: Int = 48_000,
    val storageBytes: Long = 0L,
    val draftCount: Int = 0,
    val isClearingCache: Boolean = false,
    val error: String? = null
)
